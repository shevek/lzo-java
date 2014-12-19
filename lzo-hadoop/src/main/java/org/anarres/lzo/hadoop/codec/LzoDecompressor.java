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
import org.anarres.lzo.lzo_uintp;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.compress.Decompressor;

/**
 * A {@link Decompressor} based on the lzo algorithm.
 * http://www.oberhumer.com/opensource/lzo/
 *
 */
public class LzoDecompressor implements Decompressor {

    private static final Log LOG = LogFactory.getLog(LzoDecompressor.class);

    public static enum CompressionStrategy {

        /**
         * lzo1 algorithms.
         */
        LZO1(LzoAlgorithm.LZO1),
        /**
         * lzo1a algorithms.
         */
        LZO1A(LzoAlgorithm.LZO1A),
        /**
         * lzo1b algorithms.
         */
        LZO1B(LzoAlgorithm.LZO1B),
        LZO1B_SAFE(LzoAlgorithm.LZO1B, LzoConstraint.SAFETY),
        /**
         * lzo1c algorithms.
         */
        LZO1C(LzoAlgorithm.LZO1C),
        LZO1C_SAFE(LzoAlgorithm.LZO1C, LzoConstraint.SAFETY),
        LZO1C_ASM(LzoAlgorithm.LZO1C),
        LZO1C_ASM_SAFE(LzoAlgorithm.LZO1C, LzoConstraint.SAFETY),
        /**
         * lzo1f algorithms.
         */
        LZO1F(LzoAlgorithm.LZO1F),
        LZO1F_SAFE(LzoAlgorithm.LZO1F, LzoConstraint.SAFETY),
        LZO1F_ASM_FAST(LzoAlgorithm.LZO1F),
        LZO1F_ASM_FAST_SAFE(LzoAlgorithm.LZO1F, LzoConstraint.SAFETY),
        /**
         * lzo1x algorithms.
         */
        LZO1X(LzoAlgorithm.LZO1X),
        LZO1X_SAFE(LzoAlgorithm.LZO1X, LzoConstraint.SAFETY),
        LZO1X_ASM(LzoAlgorithm.LZO1X),
        LZO1X_ASM_SAFE(LzoAlgorithm.LZO1X, LzoConstraint.SAFETY),
        LZO1X_ASM_FAST(LzoAlgorithm.LZO1X, LzoConstraint.SPEED),
        LZO1X_ASM_FAST_SAFE(LzoAlgorithm.LZO1X, LzoConstraint.SAFETY),
        /**
         * lzo1y algorithms.
         */
        LZO1Y(LzoAlgorithm.LZO1Y),
        LZO1Y_SAFE(LzoAlgorithm.LZO1Y, LzoConstraint.SAFETY),
        LZO1Y_ASM(LzoAlgorithm.LZO1Y),
        LZO1Y_ASM_SAFE(LzoAlgorithm.LZO1Y, LzoConstraint.SAFETY),
        LZO1Y_ASM_FAST(LzoAlgorithm.LZO1Y, LzoConstraint.SPEED),
        LZO1Y_ASM_FAST_SAFE(LzoAlgorithm.LZO1Y, LzoConstraint.SAFETY),
        /**
         * lzo1z algorithms.
         */
        LZO1Z(LzoAlgorithm.LZO1Z),
        LZO1Z_SAFE(LzoAlgorithm.LZO1Z, LzoConstraint.SAFETY),
        /**
         * lzo2a algorithms.
         */
        LZO2A(LzoAlgorithm.LZO2A),
        LZO2A_SAFE(LzoAlgorithm.LZO2A, LzoConstraint.SAFETY);
        private final LzoAlgorithm algorithm;
        private final LzoConstraint constraint;

        private CompressionStrategy(LzoAlgorithm algorithm, LzoConstraint constraint) {
            this.algorithm = algorithm;
            this.constraint = constraint;
        }

        private CompressionStrategy(LzoAlgorithm algorithm) {
            this(algorithm, null);
        }

        public org.anarres.lzo.LzoDecompressor newDecompressor() {
            return LzoLibrary.getInstance().newDecompressor(algorithm, constraint);
        }
    }; // CompressionStrategy
    private final org.anarres.lzo.LzoDecompressor decompressor;
    private byte[] outputBuffer;
    private int outputBufferPos;
    private final lzo_uintp outputBufferLen = new lzo_uintp();	// Also, end, since we base outputBuffer at 0.
    // private boolean finished;   // We need this because BlockCompressorStream's state machine doesn't distinguish between no-data, and all-data-decoded.

    /**
     * Creates a new lzo decompressor.
     *
     * @param strategy lzo decompression algorithm
     * @param outputBufferSize size of the output buffer
     */
    public LzoDecompressor(CompressionStrategy strategy, int outputBufferSize) {
        this.decompressor = strategy.newDecompressor();
        setOutputBufferSize(outputBufferSize);
    }

    public void setOutputBufferSize(int outputBufferSize) {
        if (outputBuffer == null || outputBufferSize > outputBuffer.length)
            outputBuffer = new byte[outputBufferSize];
    }

    /**
     * Creates a new lzo decompressor.
     */
    public LzoDecompressor() {
        this(CompressionStrategy.LZO1X, 64 * 1024);
    }

    private void logState(String when) {
        LOG.info("\n");
        LOG.info(when + " Output buffer pos=" + outputBufferPos + "; length=" + outputBufferLen);
        // testInvariants();
    }

    @Override
    public void setInput(byte[] b, int off, int len) {
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || off > b.length - len)
            throw new ArrayIndexOutOfBoundsException("Illegal range in buffer: Buffer length=" + b.length + ", offset=" + off + ", length=" + len);
        if (!needsInput())
            throw new IllegalStateException("I don't need input: pos=" + outputBufferPos + "; len=" + outputBufferLen);
        // logState("Before setInput");
        // LOG.info("Decompressing " + len + " bytes at " + off);
        outputBufferLen.value = outputBuffer.length;
        // try {
        try {
            outputBufferPos = 0;
            int code = decompressor.decompress(b, off, len, outputBuffer, outputBufferPos, outputBufferLen);
            if (code != LzoTransformer.LZO_E_OK) {
                logState("LZO error: " + code);
                // FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(b, off, off + len));
                throw new IllegalArgumentException(decompressor.toErrorString(code));
            }
        } catch (IndexOutOfBoundsException e) {
            logState("IndexOutOfBoundsException: " + e);
            // FileUtils.writeByteArrayToFile(new File("bytes.out"), Arrays.copyOfRange(b, off, off + len));
            throw e;
        }
        // } catch (IOException _e) {
        // throw new RuntimeException(_e);
        // }
        // LOG.info(len + " -> " + outputBufferLen);
        // logState("After setInput");
    }

    @Override
    public void setDictionary(byte[] b, int off, int len) {
        // nop
    }

    @Override
    public boolean needsInput() {
        // logState("Before needsInput");
        return outputBufferLen.value <= 0;
    }

    @Override
    public boolean needsDictionary() {
        return false;
    }

    @Override
    public boolean finished() {
        // logState("Before finished");
// https://github.com/hortonworks/hadoop-lzo/commit/729bcc3d0d86fefb5a9b0a76fbcdbc20bc497db8
//		if (outputBufferLen.value == 0 && outputBufferPos == 0)
//			return false;
        return outputBufferLen.value <= 0;
        // return false;
    }

    @Override
    public int decompress(byte[] b, int off, int len)
            throws IOException {
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || off > b.length - len)
            throw new ArrayIndexOutOfBoundsException("Illegal range in buffer: Buffer length=" + b.length + ", offset=" + off + ", length=" + len);

        // logState("Before decompress");
        len = Math.min(len, outputBufferLen.value);
        System.arraycopy(outputBuffer, outputBufferPos, b, off, len);
        outputBufferPos += len;
        outputBufferLen.value -= len;

        return len;
    }

    @Override
    public int getRemaining() {
        return outputBufferLen.value;
    }

    @Override
    public void reset() {
        outputBufferPos = 0;
        outputBufferLen.value = 0;
    }

    @Override
    public void end() {
    }
}
