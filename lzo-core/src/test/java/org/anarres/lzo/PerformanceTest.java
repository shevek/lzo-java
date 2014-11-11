/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 *
 * @author shevek
 */
public class PerformanceTest {

    private static final Log LOG = LogFactory.getLog(PerformanceTest.class);
    // I am not going to commit a 512Mb file to git. You will have to choose your own.
    // This is a VMWare disk image, which counts as "real data".
    private static final String PATH = "/home/shevek/vm/karmasphere-aws-vm/Karmasphere AWS VM-s003.vmdk";
    // private static final String PATH = "/home/shevek/thirdparty/archive/fp.log";

    public void testBlockCompress(LzoCompressor compressor) throws Exception {
        try {
            LOG.info("Running performance test for " + compressor);
            LOG.info("Total memory is " + Runtime.getRuntime().totalMemory());
            LOG.info("Max memory is " + Runtime.getRuntime().maxMemory());

            File file = new File(PATH);
            assumeTrue(file.isFile());
            FileInputStream fi = new FileInputStream(new File(PATH));
            DataInputStream di = new DataInputStream(fi);
            int length = Math.min((int) file.length(), 512 * 1024 * 1024);
            byte[] data = new byte[length];
            di.readFully(data);
            LOG.info("Original data is " + data.length + " bytes.");
            byte[] compressed = new byte[data.length];

            LzoDecompressor decompressor = new LzoDecompressor1x();

            for (int i = 0; i < 8; i++) {
                lzo_uintp compressed_length = new lzo_uintp(compressed.length);
                LOG.info("Starting.");
                long start = System.currentTimeMillis();
                compressor.compress(data, 0, data.length, compressed, 0, compressed_length);
                long end = System.currentTimeMillis();
                LOG.info("Compression took " + ((end - start) / 1000d) + " ms "
                        + "and output " + compressed_length + " bytes, "
                        + "ratio=" + (data.length / (double) compressed_length.value));

                lzo_uintp uncompressed_length = new lzo_uintp(data.length);
                start = System.currentTimeMillis();
                decompressor.decompress(compressed, 0, compressed_length.value, data, 0, uncompressed_length);
                end = System.currentTimeMillis();
                LOG.info("Uncompression took " + ((end - start) / 1000d) + " ms and output " + uncompressed_length + " bytes");
                assertEquals(data.length, uncompressed_length.value);
            }
        } finally {
            System.out.flush();
            System.err.flush();
            Thread.sleep(100);
        }
    }

    @Test
    public void testBlockCompress() throws Exception {
        testBlockCompress(new LzoCompressor1x_1());
        testBlockCompress(new LzoCompressor1x_999(7));
        testBlockCompress(new LzoCompressor1x_999(8));
        testBlockCompress(new LzoCompressor1x_999(9));
    }
}
