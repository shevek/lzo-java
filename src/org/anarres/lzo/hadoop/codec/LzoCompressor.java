/*
 * This file is part of Hadoop-Gpl-Compression.
 *
 * Hadoop-Gpl-Compression is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Hadoop-Gpl-Compression is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hadoop-Gpl-Compression.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.anarres.lzo.hadoop.codec;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.anarres.lzo.LzoCompressor1x;
import org.anarres.lzo.LzoConstants;
import org.anarres.lzo.LzoErrors;
import org.anarres.lzo.lzo_uintp;
import org.apache.commons.io.FileUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.Compressor;

/**
 * A {@link Compressor} based on the lzo algorithm.
 * http://www.oberhumer.com/opensource/lzo/
 * 
 */
public class LzoCompressor implements Compressor {

    private static final Log LOG = LogFactory.getLog(LzoCompressor.class);

    /**
     * The compression algorithm for lzo library.
     */
    public static enum CompressionStrategy {

        /**
         * lzo1 algorithms.
         */
        LZO1(0),
        LZO1_99(1),
        /**
         * lzo1a algorithms.
         */
        LZO1A(2),
        LZO1A_99(3),
        /**
         * lzo1b algorithms.
         */
        LZO1B(4),
        LZO1B_BEST_COMPRESSION(5),
        LZO1B_BEST_SPEED(6),
        LZO1B_1(7),
        LZO1B_2(8),
        LZO1B_3(9),
        LZO1B_4(10),
        LZO1B_5(11),
        LZO1B_6(12),
        LZO1B_7(13),
        LZO1B_8(14),
        LZO1B_9(15),
        LZO1B_99(16),
        LZO1B_999(17),
        /**
         * lzo1c algorithms.
         */
        LZO1C(18),
        LZO1C_BEST_COMPRESSION(19),
        LZO1C_BEST_SPEED(20),
        LZO1C_1(21),
        LZO1C_2(22),
        LZO1C_3(23),
        LZO1C_4(24),
        LZO1C_5(25),
        LZO1C_6(26),
        LZO1C_7(27),
        LZO1C_8(28),
        LZO1C_9(29),
        LZO1C_99(30),
        LZO1C_999(31),
        /**
         * lzo1f algorithms.
         */
        LZO1F_1(32),
        LZO1F_999(33),
        /**
         * lzo1x algorithms.
         */
        LZO1X_1(34),
        LZO1X_11(35),
        LZO1X_12(36),
        LZO1X_15(37),
        LZO1X_999(38),
        /**
         * lzo1y algorithms.
         */
        LZO1Y_1(39),
        LZO1Y_999(40),
        /**
         * lzo1z algorithms.
         */
        LZO1Z_999(41),
        /**
         * lzo2a algorithms.
         */
        LZO2A_999(42);
        private final int compressor;

        private CompressionStrategy(int compressor) {
            this.compressor = compressor;
        }

        int getCompressor() {
            return compressor;
        }
    }; // CompressionStrategy
    private final CompressionStrategy strategy; // The lzo compression algorithm.
    private final byte[] inputBuffer;
    private int inputBufferLen;
    private byte[] inputHoldoverBuffer;
    private int inputHoldoverBufferPos;
    private int inputHoldoverBufferLen;
    private final byte[] outputBuffer;
    private int outputBufferPos;
    private final lzo_uintp outputBufferLen = new lzo_uintp();	// Also, end, since we base outputBuffer at 0.
    private int inputByteCount;
    private int outputByteCount;
    private int workingMemoryBufLen = 0;  // The length of 'working memory' buf.
    private boolean finished;	// Interaction with a brain-damaged contract from BlockCompressorStream.

    /**
     * Creates a new compressor using the specified {@link CompressionStrategy}.
     *
     * @param strategy lzo compression algorithm to use
     * @param outputBufferSize size of the output buffer to be used.
     */
    public LzoCompressor(CompressionStrategy strategy, int outputBufferSize) {
        this.strategy = strategy;
        this.inputBuffer = new byte[outputBufferSize];
        this.outputBuffer = new byte[outputBufferSize + (outputBufferSize >> 3) + 256];
        reset();
    }

    /**
     * Creates a new compressor with the default lzo1x_1 compression.
     */
    public LzoCompressor() {
        this(CompressionStrategy.LZO1X_1, 64 * 1024);
    }

    private void logState(String when) {
        LOG.info("\n");
        LOG.info(when + " Input buffer length=" + inputBufferLen + "/" + inputBuffer.length);
        if (inputHoldoverBuffer == null) {
            LOG.info(when + " Input holdover = null");
        } else {
            LOG.info(when + " Input holdover pos=" + inputHoldoverBufferPos + "; length=" + inputHoldoverBufferLen);
        }
        LOG.info(when + " Output buffer pos=" + outputBufferPos + "; length=" + outputBufferLen + "/" + outputBuffer.length);
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
            throw new IllegalStateException("Output buffer overrun pos=" + outputBufferPos + "; len=" + outputBufferLen);

        return true;
    }

    @Override
    public void setInput(byte[] b, int off, int len) {
        // logState("Before setInput");
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || off > b.length - len)
            throw new ArrayIndexOutOfBoundsException("Illegal range in buffer: Buffer length=" + b.length + ", offset=" + off + ", length=" + len);
        if (inputHoldoverBuffer != null)
            throw new IllegalStateException("Cannot accept input while holdover is present.");

        inputHoldoverBuffer = b;
        inputHoldoverBufferPos = off;
        inputHoldoverBufferLen = len;
        compact();

        inputByteCount += len;  // Unfortunately, we have to do this here. This is so, so, so wrong.

        // logState("After setInput");
    }

    @Override
    public void setDictionary(byte[] b, int off, int len) {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsInput() {
        compact();
        if (inputHoldoverBuffer != null)
            return false;
        return inputBufferLen < inputBuffer.length;
    }

    @Override
    public void finish() {
        finished = true;
    }

    @Override
    public boolean finished() {
        assert testInvariants();
        return finished && outputBufferLen.value == 0 && inputBufferLen == 0 && inputHoldoverBuffer == null;
    }

    private void compact() {
        if (inputHoldoverBuffer == null) {
            assert testInvariants();
            return;
        }

        int remaining = inputBuffer.length - inputBufferLen;
        if (inputHoldoverBufferLen <= remaining) {
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

    @Override
    public int compress(byte[] b, int off, int len) throws IOException {
        // logState("Before compress");
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || off > b.length - len)
            throw new ArrayIndexOutOfBoundsException("Illegal range in buffer: Buffer length=" + b.length + ", offset=" + off + ", length=" + len);

        if (outputBufferLen.value == 0) {
            byte[] compressBuffer;
            int compressBufferPos;
            int compressBufferLen;

            // Do compression.
            if (inputBufferLen > 0) {
                compressBuffer = inputBuffer;
                compressBufferPos = 0;
                compressBufferLen = inputBufferLen;
                inputBufferLen = 0;
            } else if (inputHoldoverBuffer != null) {
                compressBuffer = inputHoldoverBuffer;
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
            outputBufferPos = 0;
            outputBufferLen.value = outputBuffer.length;
            try {
                int code = LzoCompressor1x.lzo1x_1_compress(compressBuffer, compressBufferPos, compressBufferLen, outputBuffer, outputBufferPos, outputBufferLen, new int[1 << 14]);
                if (code != LzoConstants.LZO_E_OK) {
                    logState("LZO error: " + code);
                    FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(compressBuffer, compressBufferPos, compressBufferPos + compressBufferLen));
                    throw new IllegalArgumentException(LzoErrors.toString(code));
                }
            } catch (IndexOutOfBoundsException e) {
                logState("IndexOutOfBoundsException: " + e);
                FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(compressBuffer, compressBufferPos, compressBufferPos + compressBufferLen));
                throw new IOException(e);
            }
            LOG.info(compressBufferLen + "(" + Integer.toHexString(compressBufferLen) + ") -> " + outputBufferLen + "(" + Integer.toHexString(outputBufferLen.value) + ")");
        }

        len = Math.min(len, outputBufferLen.value);
        System.arraycopy(outputBuffer, outputBufferPos, b, off, len);
        outputBufferPos += len;
        outputBufferLen.value -= len;

        outputByteCount += len;

        // logState("After compress; len=" + len);
        return len;
    }

    @Override
    public void reset() {
        inputByteCount = 0;
        outputByteCount = 0;
        inputBufferLen = 0;
        inputHoldoverBuffer = null;
        inputHoldoverBufferPos = -1;
        inputHoldoverBufferLen = -1;
        outputBufferPos = 0;
        outputBufferLen.value = 0;
        finished = false;
    }

    @Override
    public synchronized void reinit(Configuration conf) {
        reset();
    }

    /**
     * Return number of bytes given to this compressor since last reset.
     */
    @Override
    public synchronized long getBytesRead() {
        return inputByteCount;
    }

    /**
     * Return number of bytes consumed by callers of compress since last reset.
     */
    @Override
    public long getBytesWritten() {
        return outputByteCount;
    }

    /**
     * Noop.
     */
    @Override
    public void end() {
        // nop
    }
}
