/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

/**
 *
 * @author shevek
 */
public class LzoLibrary {

    private static class Inner {

        private static final LzoLibrary INSTANCE = new LzoLibrary();
    }

    public static LzoLibrary getInstance() {
        return Inner.INSTANCE;
    }

    /**
     * Returns a new compressor for the given algorithm.
     */
    public LzoCompressor newCompressor(LzoAlgorithm algorithm, LzoConstraint constraint) {
        if (algorithm == null)
            return new LzoCompressor1x_1();
        switch (algorithm) {
            case LZO1X:
                return new LzoCompressor1x_1();
            case LZO1Y:
                return new LzoCompressor1y_1();
            default:
                throw new UnsupportedOperationException("Unsupported algorithm " + algorithm);
        }
    }

    /**
     * Returns a new decompressor for the given algorithm.
     * The only constraint which makes sense is {@link LzoConstraint#SAFETY}.
     */
    public LzoDecompressor newDecompressor(LzoAlgorithm algorithm, LzoConstraint constraint) {
        if (algorithm == null)
            throw new NullPointerException("No algorithm specified.");
        switch (algorithm) {

            case LZO1X:
                if (constraint == LzoConstraint.SAFETY)
                    return new LzoDecompressor1x_safe();
                else
                    return new LzoDecompressor1x();

            case LZO1Y:
                if (constraint == LzoConstraint.SAFETY)
                    return new LzoDecompressor1y_safe();
                else
                    return new LzoDecompressor1y();

            case LZO1Z:
                if (constraint == LzoConstraint.SAFETY)
                    return new LzoDecompressor1z_safe();
                else
                    return new LzoDecompressor1z();

            default:
                throw new UnsupportedOperationException("Unsupported algorithm " + algorithm);

        }
    }
}
