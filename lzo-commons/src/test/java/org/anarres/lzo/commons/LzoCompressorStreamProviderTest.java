package org.anarres.lzo.commons;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class LzoCompressorStreamProviderTest {
    private void testAlgorithm(String algorithm, byte[] orig)
        throws IOException, CompressorException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        CompressorOutputStream cs = new CompressorStreamFactory()
            .createCompressorOutputStream(algorithm, os);
        assertNotNull("didn't get any output stream for algorithm " + algorithm, cs);
        assertTrue("stream is not an LzoCompressorOutputStream but " + cs.getClass()
                   + " for algorithm " + algorithm,
                   cs instanceof LzoCompressorOutputStream);
        cs.write(orig);
        cs.close();

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        CompressorInputStream us = new CompressorStreamFactory()
            .createCompressorInputStream(algorithm, is);
        assertNotNull("didn't get any input stream for algorithm " + algorithm, us);
        assertTrue("stream is not an LzoCompressorInputStream but " + us.getClass()
                   + " for algorithm " + algorithm,
                   us instanceof LzoCompressorInputStream);
        DataInputStream ds = new DataInputStream(us);
        byte[] uncompressed = new byte[orig.length];
        ds.readFully(uncompressed);

        assertArrayEquals(orig, uncompressed);
    }

    // Highly cyclic.
    @Test
    public void testSequence() throws Exception {
        byte[] orig = new byte[512 * 1024];
        for (int i = 0; i < orig.length; i++)
            orig[i] = (byte) (i & 0xf);
        for (String algorithm : new LzoCompressorStreamProvider()
                 .getOutputStreamCompressorNames()) {
            testAlgorithm(algorithm, orig);
        }
    }

}
