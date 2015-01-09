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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author shevek
 */
public class LzopStreamTest {

    private static final Log LOG = LogFactory.getLog(LzopStreamTest.class);
    private static final long[] FLAGS = new long[]{
            0L,
            // Adler32
            LzopConstants.F_ADLER32_C,
            LzopConstants.F_ADLER32_D,
            LzopConstants.F_ADLER32_C | LzopConstants.F_ADLER32_D,
            // CRC32
            LzopConstants.F_CRC32_C,
            LzopConstants.F_CRC32_D,
            LzopConstants.F_CRC32_C | LzopConstants.F_CRC32_D,
            // Both
            LzopConstants.F_ADLER32_C | LzopConstants.F_CRC32_C,
            LzopConstants.F_ADLER32_D | LzopConstants.F_CRC32_D,
            LzopConstants.F_ADLER32_C | LzopConstants.F_ADLER32_D | LzopConstants.F_CRC32_C | LzopConstants.F_CRC32_D
    };

    public void testAlgorithm(LzoAlgorithm algorithm, LzoConstraint constraint, byte[] orig) throws IOException {
        for (long flags : FLAGS) {
            try {
                LzoCompressor compressor = LzoLibrary.getInstance().newCompressor(algorithm, constraint);
                LOG.info("Compressing " + orig.length + " bytes using " + algorithm + "/" + constraint);

                // LOG.info("Original:   " + Arrays.toString(orig));
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                LzopOutputStream cs = new LzopOutputStream(os, compressor, 256, flags);
                cs.write(orig);
                cs.close();

                // LOG.info("Compressed: OK.");
                FileUtils.forceMkdir(new File("build/tmp"));
                FileUtils.writeByteArrayToFile(new File("build/tmp/temp.lzo"), os.toByteArray());

                // LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(algorithm, null);
                ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
                LzopInputStream us = new LzopInputStream(is);
                DataInputStream ds = new DataInputStream(us);
                byte[] uncompressed = new byte[orig.length];
                ds.readFully(uncompressed);

                // LOG.info("Output:     OK.");
                // LOG.info("Output:     " + Arrays.toString(uncompressed));
                assertArrayEquals(orig, uncompressed);
            } finally {
                System.out.flush();
                System.err.flush();
            }
        }
    }

    LzoConstraint[] supportedConstraints = { null, LzoConstraint.COMPRESSION };     

    // Totally RLE.
    @Test
    public void testBlank() throws Exception {
        byte[] orig = new byte[512 * 1024];
        Arrays.fill(orig, (byte) 0);
        for (LzoConstraint constraint : supportedConstraints) {
            try {
                testAlgorithm(LzoAlgorithm.LZO1X, constraint, orig);
            } catch (UnsupportedOperationException e) {
                // LOG.info("Unsupported algorithm " + algorithm);
            }
        }
    }

    // Highly cyclic.
    @Test
    public void testSequence() throws Exception {
        byte[] orig = new byte[512 * 1024];
        for (int i = 0; i < orig.length; i++)
            orig[i] = (byte) (i & 0xf);
        for (LzoConstraint constraint : supportedConstraints) {
            try {
                testAlgorithm(LzoAlgorithm.LZO1X, constraint, orig);
            } catch (UnsupportedOperationException e) {
                // LOG.info("Unsupported algorithm " + algorithm);
            }
        }
    }

    // Essentially uncompressible.
    @Test
    public void testRandom() throws Exception {
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            byte[] orig = new byte[256 * 1024];
            r.nextBytes(orig);
            for (LzoConstraint constraint : supportedConstraints) {
                try {
                    testAlgorithm(LzoAlgorithm.LZO1X, constraint, orig);
                } catch (UnsupportedOperationException e) {
                    // LOG.info("Unsupported algorithm " + algorithm);
                }
            }
        }
    }

    public void testClass(Class<?> type) throws Exception {
        String name = type.getName();
        name = name.replace('.', '/') + ".class";
        LOG.info("Class is " + name);
        InputStream in = getClass().getClassLoader().getResourceAsStream(name);
        byte[] orig = IOUtils.toByteArray(in);
        for (LzoConstraint constraint : supportedConstraints) {
            try {
                testAlgorithm(LzoAlgorithm.LZO1X, constraint, orig);
            } catch (UnsupportedOperationException e) {
                // LOG.info("Unsupported algorithm " + algorithm);
            }
        }
    }

    @Test
    public void testClass() throws Exception {
        testClass(getClass());
        testClass(Integer.class);
        testClass(Formatter.class);
    }

}
