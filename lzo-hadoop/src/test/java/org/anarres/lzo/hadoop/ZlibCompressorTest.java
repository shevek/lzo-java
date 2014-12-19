/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo.hadoop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.compress.BlockCompressorStream;
import org.apache.hadoop.io.compress.BlockDecompressorStream;
import org.apache.hadoop.io.compress.zlib.BuiltInZlibDeflater;
import org.apache.hadoop.io.compress.zlib.BuiltInZlibInflater;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author shevek
 */
public class ZlibCompressorTest {

    private static final Log LOG = LogFactory.getLog(ZlibCompressorTest.class);
    private static final String PATH = "/home/shevek/vm/karmasphere-aws-vm/Karmasphere AWS VM-s003.vmdk";

    @Test
    public void testEmpty() {
        LOG.info("Every test suite must have a case, or JUnit gets unhappy.");
    }

    @Ignore
    @Test
    public void testBlockCompressorStream() throws Exception {
        try {
            LOG.info("Total memory is " + Runtime.getRuntime().totalMemory());
            LOG.info("Max memory is " + Runtime.getRuntime().maxMemory());

            FileInputStream fi = new FileInputStream(new File(PATH));
            DataInputStream di = new DataInputStream(fi);
            byte[] data = new byte[512 * 1024 * 1024];
            di.readFully(data);
            LOG.info("Original data is " + data.length + " bytes.");

            ByteArrayInputStream bi = new ByteArrayInputStream(data);
            ByteArrayOutputStream bo = new ByteArrayOutputStream(data.length);
            BlockCompressorStream co = new BlockCompressorStream(bo, new BuiltInZlibDeflater());
            LOG.info("Starting.");
            long start = System.currentTimeMillis();
            IOUtils.copy(bi, co);
            co.close();
            long end = System.currentTimeMillis();
            LOG.info("Compression took " + ((end - start) / 1000d) + " ms");
            LOG.info("Compressed data is " + bo.size() + " bytes.");

            if (true)
                return;

            bi = new ByteArrayInputStream(bo.toByteArray());
            BlockDecompressorStream ci = new BlockDecompressorStream(bi, new BuiltInZlibInflater());
            bo.reset();
            start = System.currentTimeMillis();
            IOUtils.copy(ci, bo);
            end = System.currentTimeMillis();
            LOG.info("Uncompression took " + ((end - start) / 1000d) + " ms");
            LOG.info("Uncompressed data is " + bo.size() + " bytes.");
        } finally {
            System.out.flush();
            System.err.flush();
            Thread.sleep(100);
        }
    }
}
