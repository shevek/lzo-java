/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author shevek
 */
public class LzoAlgorithmTest {

    private static final Log LOG = LogFactory.getLog(LzoAlgorithmTest.class);

    public void testAlgorithm(LzoAlgorithm algorithm, LzoConstraint constraint, byte[] orig, String desc) {
        LzoCompressor compressor = LzoLibrary.getInstance().newCompressor(algorithm, constraint);
        LOG.info("\nCompressing " + orig.length + " " + desc + " bytes using " + algorithm + "/" + constraint);

        // LOG.info("Original:   " + Arrays.toString(orig));
        byte[] compressed = new byte[orig.length * 2];
        lzo_uintp compressed_length = new lzo_uintp(compressed.length);
        int compressed_code = compressor.compress(orig, 0, orig.length, compressed, 0, compressed_length);

        LOG.info("Compressed: " + compressor.toErrorString(compressed_code) + "; length=" + compressed_length);
        // LOG.info("Compressed: " + Arrays.toString(Arrays.copyOf(compressed, compressed_length.value)));
        assertEquals(LzoTransformer.LZO_E_OK, compressed_code);

        LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(algorithm, null);
        byte[] uncompressed = new byte[orig.length];
        lzo_uintp uncompressed_length = new lzo_uintp(uncompressed.length);
        int uncompressed_code = decompressor.decompress(compressed, 0, compressed_length.value, uncompressed, 0, uncompressed_length);

        LOG.info("Output:     " + decompressor.toErrorString(uncompressed_code));
        // LOG.info("Output:     " + Arrays.toString(uncompressed));

        assertEquals(LzoTransformer.LZO_E_OK, uncompressed_code);
        assertArrayEquals(orig, uncompressed);
    }

    LzoConstraint[] supportedConstraints = { null, LzoConstraint.COMPRESSION };     

    // Totally RLE.
    @Test
    public void testBlank() throws Exception {
        byte[] orig = new byte[512 * 1024];
        Arrays.fill(orig, (byte) 0);
        for (LzoAlgorithm algorithm : LzoAlgorithm.values()) {
            for (LzoConstraint constraint : supportedConstraints) {
                try {
                    testAlgorithm(algorithm, constraint, orig, "blank");
                } catch (UnsupportedOperationException e) {
                    // LOG.info("Unsupported algorithm " + algorithm);
                }
            }
        }
    }

    // Highly cyclic.
    @Test
    public void testSequence() throws Exception {
        byte[] orig = new byte[512 * 1024];
        for (int i = 0; i < orig.length; i++)
            orig[i] = (byte) (i & 0xf);
        for (LzoAlgorithm algorithm : LzoAlgorithm.values()) {
            for (LzoConstraint constraint : supportedConstraints) {
                try {
                    testAlgorithm(algorithm, constraint, orig, "sequential");
                } catch (UnsupportedOperationException e) {
                    // LOG.info("Unsupported algorithm " + algorithm);
                }
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
            for (LzoAlgorithm algorithm : LzoAlgorithm.values()) {
                for (LzoConstraint constraint : supportedConstraints) {
                    try {
                        testAlgorithm(algorithm, constraint, orig, "random");
                    } catch (UnsupportedOperationException e) {
                        // LOG.info("Unsupported algorithm " + algorithm);
                    }
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
        for (LzoAlgorithm algorithm : LzoAlgorithm.values()) {
            for (LzoConstraint constraint : supportedConstraints) {
                try {
                    testAlgorithm(algorithm, constraint, orig, "class-file");
                } catch (UnsupportedOperationException e) {
                    // LOG.info("Unsupported algorithm " + algorithm);
                }
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
