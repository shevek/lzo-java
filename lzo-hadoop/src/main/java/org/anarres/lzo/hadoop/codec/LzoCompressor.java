/*
 * This file is part of lzo-java, an implementation of LZO in Java.
 * https://github.com/shevek/lzo-java
 *
 * The Java portion of this library is:
 * Copyright (C) 2011 Shevek <shevek@anarres.org>
 * All Rights Reserved.
 *
 * This file is based on a file from hadoop-gpl-compression.
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
 */
package org.anarres.lzo.hadoop.codec;

import java.io.IOException;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoConstraint;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.LzoTransformer;
import org.anarres.lzo.SuppressWarnings;
import org.anarres.lzo.lzo_uintp;
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
        LZO1(LzoAlgorithm.LZO1),
        LZO1_99(LzoAlgorithm.LZO1, LzoConstraint.COMPRESSION),
        /**
         * lzo1a algorithms.
         */
        LZO1A(LzoAlgorithm.LZO1),
        LZO1A_99(LzoAlgorithm.LZO1, LzoConstraint.COMPRESSION),
        /**
         * lzo1b algorithms.
         */
        LZO1B(LzoAlgorithm.LZO1),
        LZO1B_BEST_COMPRESSION(LzoAlgorithm.LZO1, LzoConstraint.COMPRESSION),
        LZO1B_BEST_SPEED(LzoAlgorithm.LZO1, LzoConstraint.SPEED),
        LZO1B_1(LzoAlgorithm.LZO1B),
        LZO1B_2(LzoAlgorithm.LZO1B),
        LZO1B_3(LzoAlgorithm.LZO1B),
        LZO1B_4(LzoAlgorithm.LZO1B),
        LZO1B_5(LzoAlgorithm.LZO1B),
        LZO1B_6(LzoAlgorithm.LZO1B),
        LZO1B_7(LzoAlgorithm.LZO1B),
        LZO1B_8(LzoAlgorithm.LZO1B),
        LZO1B_9(LzoAlgorithm.LZO1B),
        LZO1B_99(LzoAlgorithm.LZO1B, LzoConstraint.COMPRESSION),
        LZO1B_999(LzoAlgorithm.LZO1B, LzoConstraint.COMPRESSION),
        /**
         * lzo1c algorithms.
         */
        LZO1C(LzoAlgorithm.LZO1C),
        LZO1C_BEST_COMPRESSION(LzoAlgorithm.LZO1C, LzoConstraint.COMPRESSION),
        LZO1C_BEST_SPEED(LzoAlgorithm.LZO1C, LzoConstraint.SPEED),
        LZO1C_1(LzoAlgorithm.LZO1C),
        LZO1C_2(LzoAlgorithm.LZO1C),
        LZO1C_3(LzoAlgorithm.LZO1C),
        LZO1C_4(LzoAlgorithm.LZO1C),
        LZO1C_5(LzoAlgorithm.LZO1C),
        LZO1C_6(LzoAlgorithm.LZO1C),
        LZO1C_7(LzoAlgorithm.LZO1C),
        LZO1C_8(LzoAlgorithm.LZO1C),
        LZO1C_9(LzoAlgorithm.LZO1C),
        LZO1C_99(LzoAlgorithm.LZO1C, LzoConstraint.COMPRESSION),
        LZO1C_999(LzoAlgorithm.LZO1C, LzoConstraint.COMPRESSION),
        /**
         * lzo1f algorithms.
         */
        LZO1F_1(LzoAlgorithm.LZO1F),
        LZO1F_999(LzoAlgorithm.LZO1F, LzoConstraint.COMPRESSION),
        /**
         * lzo1x algorithms.
         */
        LZO1X_1(LzoAlgorithm.LZO1X),
        LZO1X_11(LzoAlgorithm.LZO1X, LzoConstraint.MEMORY),
        LZO1X_12(LzoAlgorithm.LZO1X),
        LZO1X_15(LzoAlgorithm.LZO1X),
        LZO1X_999(LzoAlgorithm.LZO1X, LzoConstraint.COMPRESSION),
        /**
         * lzo1y algorithms.
         */
        LZO1Y_1(LzoAlgorithm.LZO1Y),
        LZO1Y_999(LzoAlgorithm.LZO1Y, LzoConstraint.COMPRESSION),
        /**
         * lzo1z algorithms.
         */
        LZO1Z_999(LzoAlgorithm.LZO1Z, LzoConstraint.COMPRESSION),
        /**
         * lzo2a algorithms.
         */
        LZO2A_999(LzoAlgorithm.LZO2A, LzoConstraint.COMPRESSION);
        private final LzoAlgorithm algorithm;
        private final LzoConstraint constraint;

        private CompressionStrategy(LzoAlgorithm algorithm, LzoConstraint constraint) {
            this.algorithm = algorithm;
            this.constraint = constraint;
        }

        private CompressionStrategy(LzoAlgorithm algorithm) {
            this(algorithm, null);
        }

        public org.anarres.lzo.LzoCompressor newCompressor() {
            return LzoLibrary.getInstance().newCompressor(algorithm, constraint);
        }
    }; // CompressionStrategy
    private final org.anarres.lzo.LzoCompressor compressor; // The lzo compression algorithm.
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
    private boolean finished;	// Interaction with a brain-damaged contract from BlockCompressorStream.

    /**
     * Creates a new compressor using the specified {@link CompressionStrategy}.
     *
     * @param strategy lzo compression algorithm to use
     * @param outputBufferSize size of the output buffer to be used.
     */
    public LzoCompressor(CompressionStrategy strategy, int outputBufferSize) {
        this.compressor = strategy.newCompressor();
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

    /**
     * {@inheritDoc}
     *
     * WARNING: This method retains a pointer to the user's buffer.
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
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
                int code = compressor.compress(compressBuffer, compressBufferPos, compressBufferLen, outputBuffer, outputBufferPos, outputBufferLen);
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
        }

        len = Math.min(len, outputBufferLen.value);
        System.arraycopy(outputBuffer, outputBufferPos, b, off, len);
        outputBufferPos += len;
        outputBufferLen.value -= len;

        outputByteCount += len;

        // logState("After compress; len=" + len);
        return len;
    }

    // This method is called from the constructor, and must not be overridden.
    private void _reset() {
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
    public void reset() {
        _reset();
    }

    @Override
    public synchronized void reinit(Configuration conf) {
        _reset();
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
