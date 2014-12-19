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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author shevek
 */
public class LzoOutputStream extends OutputStream {

    private static final Log LOG = LogFactory.getLog(LzoOutputStream.class.getName());
    protected final OutputStream out;
    private final LzoCompressor compressor; // Replace with BlockCompressor.
    private final byte[] inputBuffer;
    private int inputBufferLen;
    private byte[] inputHoldoverBuffer;
    private int inputHoldoverBufferPos;
    private int inputHoldoverBufferLen;
    private final byte[] outputBuffer;
    private final lzo_uintp outputBufferLen = new lzo_uintp();	// Also, end, since we base outputBuffer at 0.

    /**
     * Creates a new compressor using the specified {@link LzoCompressor}.
     *
     * @param compressor lzo compression algorithm to use
     * @param inputBufferSize size of the buffers to be used. If &lt;0, uses the default 64K.
     */
    public LzoOutputStream(@Nonnull OutputStream out, @Nonnull LzoCompressor compressor, @CheckForSigned int inputBufferSize) {
        if (inputBufferSize <= 0)
            inputBufferSize = 64 * 1024;
        this.out = out;
        this.compressor = compressor;
        this.inputBuffer = new byte[inputBufferSize];
        this.outputBuffer = new byte[inputBufferSize + compressor.getCompressionOverhead(inputBufferSize)];
        reset();
    }

    /**
     * Creates a new compressor with the specified compression.
     */
    public LzoOutputStream(@Nonnull OutputStream out, @Nonnull LzoCompressor compressor) {
        this(out, compressor, 0);
    }

    /**
     * Creates a new compressor with the default lzo1x_1 compression.
     */
    public LzoOutputStream(@Nonnull OutputStream out) {
        this(out, LzoLibrary.getInstance().newCompressor(null, null));
    }

    @Nonnull
    public LzoCompressor getCompressor() {
        return compressor;
    }

    @Nonnull
    public LzoAlgorithm getAlgorithm() {
        return getCompressor().getAlgorithm();
    }

    @Nonnull
    public LzoConstraint[] getConstraints() {
        return getCompressor().getConstraints();
    }

    private void reset() {
        inputBufferLen = 0;
        inputHoldoverBuffer = null;
        inputHoldoverBufferPos = -1;
        inputHoldoverBufferLen = -1;
        outputBufferLen.value = 0;
    }

    private void logState(@Nonnull String when) {
        LOG.info("\n");
        LOG.info(when + " Input buffer length=" + inputBufferLen + "/" + inputBuffer.length);
        if (inputHoldoverBuffer == null) {
            LOG.info(when + " Input holdover = null");
        } else {
            LOG.info(when + " Input holdover pos=" + inputHoldoverBufferPos + "; length=" + inputHoldoverBufferLen);
        }
        LOG.info(when + " Output buffer length=" + outputBufferLen + "/" + outputBuffer.length);
        // LOG.info(when + " Read=" + inputByteCount + "; Written=" + outputByteCount + "; Finished = " + finished);
        testInvariants();
    }

    private boolean testInvariants() {
        if (inputHoldoverBuffer != null) {
            if (inputBufferLen != 0 && inputBufferLen != inputBuffer.length)
                throw new IllegalStateException("Funny input buffer length " + inputBufferLen + " with array size " + inputBuffer.length + " and holdover.");
            if (inputHoldoverBufferPos < 0)
                throw new IllegalStateException("Using holdover buffer, but invalid holdover position " + inputHoldoverBufferPos);
            if (inputHoldoverBufferLen < 0)
                throw new IllegalStateException("Using holdover buffer, but invalid holdover length " + inputHoldoverBufferLen);
        } else {
            if (inputHoldoverBufferPos != -1)
                throw new IllegalStateException("No holdover buffer, but valid holdover position " + inputHoldoverBufferPos);
            if (inputHoldoverBufferLen != -1)
                throw new IllegalStateException("No holdover buffer, but valid holdover length " + inputHoldoverBufferLen);
        }

        if (outputBufferLen.value < 0)
            throw new IllegalStateException("Output buffer overrun length=" + outputBufferLen);

        return true;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        // logState("Before setInput");
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || off > b.length - len)
            throw new ArrayIndexOutOfBoundsException("Illegal range in buffer: Buffer length=" + b.length + ", offset=" + off + ", length=" + len);
        if (inputHoldoverBuffer != null)
            throw new IllegalStateException("Cannot accept input while holdover is present.");

        inputHoldoverBuffer = Arrays.copyOfRange(b, off, off + len);
        inputHoldoverBufferPos = off;
        inputHoldoverBufferLen = len;
        compact();

        while (inputHoldoverBuffer != null || inputBufferLen == inputBuffer.length)
            compress();

        // logState("After setInput");
    }

    @Override
    public void flush() throws IOException {
        while (inputHoldoverBuffer != null || inputBufferLen > 0)
            compress();
    }

    @Override
    public void close() throws IOException {
        flush();
        out.close();
    }

    private void compact() {
        if (inputHoldoverBuffer == null) {
            assert testInvariants();
            return;
        }

        int remaining = inputBuffer.length - inputBufferLen;
        if (inputHoldoverBufferLen <= remaining) {  // Possibly even 0.
            // We can put the entire holdover into the input buffer.
            System.arraycopy(inputHoldoverBuffer, inputHoldoverBufferPos, inputBuffer, inputBufferLen, inputHoldoverBufferLen);
            inputBufferLen += inputHoldoverBufferLen;
            inputHoldoverBuffer = null;
            inputHoldoverBufferPos = -1;
            inputHoldoverBufferLen = -1;
        } else if (inputBufferLen == 0) {
            // We have no input, and will run zero-copy from the holdover buffer.
        } else {
            // We need to complete the input buffer block using holdover.
            System.arraycopy(inputHoldoverBuffer, inputHoldoverBufferPos, inputBuffer, inputBufferLen, remaining);
            inputBufferLen += remaining;
            inputHoldoverBufferPos += remaining;
            inputHoldoverBufferLen -= remaining;
        }
        assert testInvariants();
    }

    private void compress() throws IOException {
        // logState("Before compress");

        byte[] compressBuffer;
        int compressBufferPos;
        int compressBufferLen;

        // Do compression.
        if (inputBufferLen > 0) {
            compressBuffer = inputBuffer.clone();
            compressBufferPos = 0;
            compressBufferLen = inputBufferLen;
            inputBufferLen = 0;
        } else if (inputHoldoverBuffer != null) {
            compressBuffer = inputHoldoverBuffer.clone();
            compressBufferPos = inputHoldoverBufferPos;
            // If this is ever less than inputBuffer.length, then we should have copied it into the input buffer.
            compressBufferLen = Math.min(inputBuffer.length, inputHoldoverBufferLen);
            assert compressBufferLen == inputBuffer.length : "Compressing less than one block of holdover.";
            inputHoldoverBufferPos += compressBufferLen;
            inputHoldoverBufferLen -= compressBufferLen;
        } else {
            throw new IllegalStateException("compress() called with no input.");
        }
        compact();

        // A sane implementation would do this here, but Hadoop breaks if we do.
        // inputByteCount += compressBufferLen;
        outputBufferLen.value = outputBuffer.length;
        try {
            int code = compressor.compress(compressBuffer, compressBufferPos, compressBufferLen, outputBuffer, 0, outputBufferLen);
            if (code != LzoTransformer.LZO_E_OK) {
                logState("LZO error: " + code);
                // FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(compressBuffer, compressBufferPos, compressBufferPos + compressBufferLen));
                throw new IllegalArgumentException(compressor.toErrorString(code));
            }
        } catch (IndexOutOfBoundsException e) {
            logState("IndexOutOfBoundsException: " + e);
            // FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(compressBuffer, compressBufferPos, compressBufferPos + compressBufferLen));
            throw new IOException(e);
        }
        // LOG.info(compressBufferLen + "(" + Integer.toHexString(compressBufferLen) + ") -> " + outputBufferLen + "(" + Integer.toHexString(outputBufferLen.value) + ")");

        writeBlock(compressBuffer, compressBufferPos, compressBufferLen, outputBuffer, 0, outputBufferLen.value);
    }

    protected void writeBlock(@Nonnull byte[] inputData, @Nonnegative int inputPos, @Nonnegative int inputLen, @Nonnull byte[] outputData, @Nonnegative int outputPos, @Nonnegative int outputLen)
            throws IOException {
        writeInt(inputLen);
        writeInt(outputLen);
        out.write(outputData, outputPos, outputLen);
    }

    protected void writeInt(int v) throws IOException {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write(v & 0xFF);
    }
}
