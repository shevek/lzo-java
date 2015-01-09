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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public class LzoLibrary {

    private static class Inner {

        private static final LzoLibrary INSTANCE = new LzoLibrary();
    }

    @Nonnull
    public static LzoLibrary getInstance() {
        return Inner.INSTANCE;
    }

    /**
     * Returns a new compressor for the given algorithm. 
     *
     * Currently the only available constraint is {@link LzoConstraint#COMPRESSION}. 
     * Applied to {@link LzoAlgorithm#LZO1X} algorithm, it yields an LZO1X999 compressor with a compression level of 7.
     */
    @Nonnull
    public LzoCompressor newCompressor(@CheckForNull LzoAlgorithm algorithm, @CheckForNull LzoConstraint constraint) {
        if (algorithm == null)
            return new LzoCompressor1x_1();
        switch (algorithm) {

            case LZO1X:
                if (constraint == null)
                   return new LzoCompressor1x_1();                
                else if (constraint == LzoConstraint.COMPRESSION)
                   return new LzoCompressor1x_999(7);
                else 
                   throw new UnsupportedOperationException("Unsupported combination " + algorithm + "/" + constraint);

            case LZO1Y:
                if (constraint == null)
                   return new LzoCompressor1y_1();
                else 
                   throw new UnsupportedOperationException("Unsupported combination " + algorithm + "/" + constraint);

            default:
                throw new UnsupportedOperationException("Unsupported algorithm " + algorithm);

        }
    }

    /**
     * Returns a new decompressor for the given algorithm.
     * The only constraint which makes sense is {@link LzoConstraint#SAFETY}.
     */
    @Nonnull
    public LzoDecompressor newDecompressor(@Nonnull LzoAlgorithm algorithm, @CheckForNull LzoConstraint constraint) {
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
