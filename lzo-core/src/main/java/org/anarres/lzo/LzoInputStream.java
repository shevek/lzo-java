/*
 * This file is part of lzo-java, an implementation of LZO in Java.
 * https://github.com/shevek/lzo-java
 *
 * The Java portion of this library is:
 * Copyright (C) 2011 Shevek <shevek@anarres.org>
 * All Rights Reserved.
 *
 * The preprocessed C portion of this library is:
 * Copyright (C) 2006-2011 Markus Franz Xaver Johannes Oberhumer
 * All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation; either version 
 * 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with the LZO library; see the file COPYING.
 * If not, see <http://www.gnu.org/licenses/> or write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.

 * As a special exception, the copyright holders of this file
 * give you permission to link this file with independent
 * modules to produce an executable, regardless of the license 
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, 
 * the terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or 
 * based on this library or file. If you modify this file, you may 
 * extend this exception to your version of the file, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 */
package org.anarres.lzo;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author shevek
 */
public class LzoInputStream extends InputStream {

    private static final Log LOG = LogFactory.getLog(LzoInputStream.class.getName());
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    protected final InputStream in;
    private final LzoDecompressor decompressor;
    protected byte[] inputBuffer = EMPTY_BYTE_ARRAY;
    protected byte[] outputBuffer = EMPTY_BYTE_ARRAY;
    protected int outputBufferPos;
    protected final lzo_uintp outputBufferLen = new lzo_uintp();	// Also, end, since we base outputBuffer at 0.

    public LzoInputStream(@Nonnull InputStream in, @Nonnull LzoDecompressor decompressor) {
        this.in = in;
        this.decompressor = decompressor;
    }

    public void setInputBufferSize(@Nonnegative int inputBufferSize) {
        if (inputBufferSize > inputBuffer.length)
            inputBuffer = new byte[inputBufferSize];
    }

    public void setOutputBufferSize(@Nonnegative int outputBufferSize) {
        if (outputBufferSize > outputBuffer.length)
            outputBuffer = new byte[outputBufferSize];
    }

    @Override
    public int available() throws IOException {
        return outputBufferLen.value - outputBufferPos;
    }

    @Override
    public int read() throws IOException {
        if (!fill())
            return -1;
        return outputBuffer[outputBufferPos++] & 0xFF;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!fill())
            return -1;
        len = Math.min(len, available());
        System.arraycopy(outputBuffer, outputBufferPos, b, off, len);
        outputBufferPos += len;
        return len;
    }

    protected void logState(@Nonnull String when) {
        LOG.info("\n");
        LOG.info(when + " Input buffer size=" + inputBuffer.length);
        LOG.info(when + " Output buffer pos=" + outputBufferPos + "; length=" + outputBufferLen + "; size=" + outputBuffer.length);
        // testInvariants();
    }

    private boolean fill() throws IOException {
        while (available() == 0)
            if (!readBlock())  // Always consumes 8 bytes, so guaranteed to terminate.
                return false;
        return true;
    }

    protected boolean readBlock() throws IOException {
        // logState("Before readBlock");
        int outputBufferLength = readInt(true);
        if (outputBufferLength == -1)
            return false;
        setOutputBufferSize(outputBufferLength);
        int inputBufferLength = readInt(false);
        setInputBufferSize(inputBufferLength);
        readBytes(inputBuffer, 0, inputBufferLength);
        decompress(outputBufferLength, inputBufferLength);
        return true;
    }

    protected void decompress(@Nonnegative int outputBufferLength, @Nonnegative int inputBufferLength) throws IOException {
        // logState("Before decompress");
        try {
            outputBufferPos = 0;
            outputBufferLen.value = outputBuffer.length;
            int code = decompressor.decompress(inputBuffer, 0, inputBufferLength, outputBuffer, 0, outputBufferLen);
            if (code != LzoTransformer.LZO_E_OK) {
                logState("LZO error: " + code);
                // FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(inputBuffer, 0, inputBufferLength));
                throw new IllegalArgumentException(decompressor.toErrorString(code));
            }
            if (outputBufferLen.value != outputBufferLength) {
                logState("Output underrun: ");
                // FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(inputBuffer, 0, inputBufferLength));
                throw new IllegalStateException("Expected " + outputBufferLength + " bytes, but got only " + outputBufferLen);
            }
        } catch (IndexOutOfBoundsException e) {
            logState("IndexOutOfBoundsException: " + e);
            // FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(inputBuffer, 0, inputBufferLength));
            throw new IOException(e);
        }
        // LOG.info(inputBufferLength + " -> " + outputBufferLen);
        // logState("After decompress");
    }

    @CheckForSigned
    protected int readInt(boolean start_of_frame) throws IOException {
        int b1 = in.read();
        if (b1 == -1) {
            if (start_of_frame)
                return -1;
            else
                throw new EOFException("EOF before reading 4-byte integer.");
        }
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        if ((b1 | b2 | b3 | b4) < 0)
            throw new EOFException("EOF while reading 4-byte integer.");
        return ((b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
    }

    protected void readBytes(@Nonnull byte[] buf, @Nonnegative int off, @Nonnegative int length) throws IOException {
        while (length > 0) {
            int count = in.read(buf, off, length);
            if (count < 0)
                throw new EOFException();
            off += count;
            length -= count;
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
