/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import org.anarres.lzo.hadoop.BlockCompressorStream;
import org.anarres.lzo.hadoop.codec.LzoCompressor;
import org.anarres.lzo.hadoop.codec.LzoDecompressor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.compress.BlockDecompressorStream;
import org.junit.Test;

/**
 *
 * @author shevek
 */
public class BlockCompressorStreamTest {

    private static final Log LOG = LogFactory.getLog(BlockCompressorStreamTest.class);
    private static final String PATH = "/home/shevek/vm/karmasphere-aws-vm/Karmasphere AWS VM-s003.vmdk";

    @Test
    public void testBlockCompressorStream() throws Throwable {
        try {
            LOG.info("Total memory is " + Runtime.getRuntime().totalMemory());
            LOG.info("Max memory is " + Runtime.getRuntime().maxMemory());

            FileInputStream fi = new FileInputStream(new File(PATH));
            DataInputStream di = new DataInputStream(fi);
            byte[] data = new byte[512 * 1024 /* * 1024 */];
            di.readFully(data);
            LOG.info("Original data is " + data.length + " bytes.");

            for (int i = 0; i < 1; i++) {
                ByteArrayInputStream bi = new ByteArrayInputStream(data);
                ByteArrayOutputStream bo = new ByteArrayOutputStream(data.length);
                BlockCompressorStream co = new BlockCompressorStream(bo, new LzoCompressor(), 64 * 1024, 18);
                LOG.info("Starting.");
                long start = System.currentTimeMillis();
                IOUtils.copy(bi, co);
                co.close();
                long end = System.currentTimeMillis();
                LOG.info("Compression took " + ((end - start) / 1000d) + " ms");
                LOG.info("Compressed data is " + bo.size() + " bytes.");

                byte[] cb = bo.toByteArray();
                FileUtils.writeByteArrayToFile(new File("compressed.out"), cb);

                bi = new ByteArrayInputStream(cb);
                BlockDecompressorStream ci = new BlockDecompressorStream(bi, new LzoDecompressor());
                bo.reset();
                start = System.currentTimeMillis();
                IOUtils.copy(ci, bo);
                end = System.currentTimeMillis();
                LOG.info("Uncompression took " + ((end - start) / 1000d) + " ms");
                LOG.info("Uncompressed data is " + bo.size() + " bytes.");
            }
        } catch (Throwable t) {
            LOG.error(t, t);
            throw t;
        } finally {
            System.out.flush();
            System.err.flush();
            Thread.sleep(100);
        }
    }
}
