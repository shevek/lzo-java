/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.anarres.lzo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
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
 *
 * @author shevek
 */
public class LzoStreamTest {

    private static final Log LOG = LogFactory.getLog(LzoStreamTest.class);

    public void testAlgorithm(LzoAlgorithm algorithm, byte[] orig) throws IOException {
        LzoCompressor compressor = LzoLibrary.getInstance().newCompressor(algorithm, null);
        LOG.info("\nCompressing " + orig.length + " bytes using " + algorithm);

        // LOG.info("Original:   " + Arrays.toString(orig));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        LzoOutputStream cs = new LzoOutputStream(os, compressor, 256);
        cs.write(orig);
        cs.close();

        LOG.info("Compressed: OK.");

        LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(algorithm, null);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        LzoInputStream us = new LzoInputStream(is, decompressor);
        DataInputStream ds = new DataInputStream(us);
        byte[] uncompressed = new byte[orig.length];
        ds.readFully(uncompressed);

        LOG.info("Output:     OK.");
        // LOG.info("Output:     " + Arrays.toString(uncompressed));

        assertArrayEquals(orig, uncompressed);
    }

    // Totally RLE.
    @Test
    public void testBlank() throws Exception {
        byte[] orig = new byte[512 * 1024];
        Arrays.fill(orig, (byte) 0);
        for (LzoAlgorithm algorithm : LzoAlgorithm.values()) {
            try {
                testAlgorithm(algorithm, orig);
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
        for (LzoAlgorithm algorithm : LzoAlgorithm.values()) {
            try {
                testAlgorithm(algorithm, orig);
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
            for (LzoAlgorithm algorithm : LzoAlgorithm.values()) {
                try {
                    testAlgorithm(algorithm, orig);
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
        for (LzoAlgorithm algorithm : LzoAlgorithm.values()) {
            try {
                testAlgorithm(algorithm, orig);
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
