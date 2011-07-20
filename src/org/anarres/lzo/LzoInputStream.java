/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author shevek
 */
public class LzoInputStream extends InputStream {

    private static final Log LOG = LogFactory.getLog(LzoInputStream.class.getName());
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private final InputStream in;
    private final LzoDecompressor decompressor;
    private byte[] inputBuffer = EMPTY_BYTE_ARRAY;
    private byte[] outputBuffer = EMPTY_BYTE_ARRAY;
    private int outputBufferPos;
    private final lzo_uintp outputBufferLen = new lzo_uintp();	// Also, end, since we base outputBuffer at 0.
    private boolean eof = false;

    public LzoInputStream(InputStream in, LzoDecompressor decompressor) {
        this.in = in;
        this.decompressor = decompressor;
    }

    private void setInputBufferSize(int inputBufferSize) {
        if (inputBufferSize > inputBuffer.length)
            inputBuffer = new byte[inputBufferSize];
    }

    private void setOutputBufferSize(int outputBufferSize) {
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
        return outputBuffer[outputBufferPos++];
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

    private void logState(String when) {
        LOG.info("\n");
        LOG.info(when + " Output buffer pos=" + outputBufferPos + "; length=" + outputBufferLen);
        // testInvariants();
    }

    private boolean fill() throws IOException {
        while (available() == 0)
            if (!decompress())  // Always consumes 8 bytes.
                return false;
        return true;
    }

    private boolean decompress() throws IOException {
        int outputBufferLength = readInt(true);
        if (outputBufferLength == -1)
            return false;
        setOutputBufferSize(outputBufferLength);
        int inputBufferLength = readInt(false);
        setInputBufferSize(inputBufferLength);
        readBytes(inputBuffer, inputBufferLength);

        // logState("Before setInput");
        try {
            outputBufferPos = 0;
            outputBufferLen.value = outputBuffer.length;
            int code = decompressor.decompress(inputBuffer, 0, inputBufferLength, outputBuffer, 0, outputBufferLen);
            if (code != LzoTransformer.LZO_E_OK) {
                logState("LZO error: " + code);
                FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(inputBuffer, 0, inputBufferLength));
                throw new IllegalArgumentException(decompressor.toErrorString(code));
            }
            if (outputBufferLen.value != outputBufferLength) {
                logState("Output underrun: ");
                FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(inputBuffer, 0, inputBufferLength));
                throw new IllegalStateException("Expected " + outputBufferLength + " bytes, but got only " + outputBufferLen);
            }
        } catch (IndexOutOfBoundsException e) {
            logState("IndexOutOfBoundsException: " + e);
            FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(inputBuffer, 0, inputBufferLength));
            throw new IOException(e);
        }
        // LOG.info(inputBufferLength + " -> " + outputBufferLen);
        // logState("After setInput");

        return true;
    }

    private int readInt(boolean start_of_frame) throws IOException {
        int b1 = in.read();
        if (b1 == -1)
            return -1;
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        if ((b1 | b2 | b3 | b4) < 0)
            throw new EOFException();
        return ((b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
    }

    private void readBytes(byte[] buf, int length) throws IOException {
        int off = 0;
        while (length > 0) {
            int count = in.read(buf, off, length);
            if (count < 0)
                throw new EOFException();
            off += count;
            length -= count;
        }
    }
}
