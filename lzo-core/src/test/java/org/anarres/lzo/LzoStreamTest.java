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
 * @author shevek
 */
public class LzoStreamTest {

    private static final Log LOG = LogFactory.getLog(LzoStreamTest.class);

    public void testAlgorithm(LzoAlgorithm algorithm, LzoConstraint constraint, byte[] orig) throws IOException {
        LzoCompressor compressor = LzoLibrary.getInstance().newCompressor(algorithm, constraint);
        LOG.info("\nCompressing " + orig.length + " bytes using " + algorithm + "/" + constraint);

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

    LzoConstraint[] supportedConstraints = { null, LzoConstraint.COMPRESSION };     

    @Test
    public void testHoldover() throws IOException {
        LzoCompressor compressor = LzoLibrary.getInstance().newCompressor(null, null);
        LOG.info("\nRunning holdover test");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ByteArrayOutputStream ts = new ByteArrayOutputStream();
        LzoOutputStream cs = new LzoOutputStream(os, compressor, 256);

        byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++)
            data[i] = (byte) (i & 0xf);

        for (int i = 0; i < data.length - 10; i++) {
            byte[] chunk = Arrays.copyOfRange(data, i, i + 10);
            cs.write(chunk);
            ts.write(chunk);
        }

        cs.close();
        LOG.info("Compressed: OK.");

        byte[] test = ts.toByteArray();

        LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(LzoAlgorithm.LZO1X, null);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        LzoInputStream us = new LzoInputStream(is, decompressor);
        DataInputStream ds = new DataInputStream(us);
        byte[] uncompressed = new byte[test.length];
        ds.readFully(uncompressed);

        LOG.info("Output:     OK.");
        assertArrayEquals(test, uncompressed);

    }

    // Totally RLE.
    @Test
    public void testBlank() throws Exception {
        byte[] orig = new byte[512 * 1024];
        Arrays.fill(orig, (byte) 0);
        for (LzoAlgorithm algorithm : LzoAlgorithm.values()) {
            for (LzoConstraint constraint : supportedConstraints) {
                 try {
                    testAlgorithm(algorithm, constraint, orig);
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
                    testAlgorithm(algorithm, constraint, orig);
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
                        testAlgorithm(algorithm, constraint, orig);
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
                    testAlgorithm(algorithm, constraint, orig);
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
