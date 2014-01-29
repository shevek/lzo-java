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
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import org.anarres.lzo.hadoop.codec.LzoCompressor;
import org.anarres.lzo.hadoop.codec.LzoDecompressor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.compress.BlockCompressorStream;
import org.apache.hadoop.io.compress.BlockDecompressorStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author shevek
 */
public class BlockCompressorStreamTest {

    private static final Log LOG = LogFactory.getLog(BlockCompressorStreamTest.class);
    private File file;

    @Before
    public void setUp() throws IOException {
        file = File.createTempFile("BlockCompressorStreamTest", ".data");
        file.deleteOnExit();
        final Random r = new Random();
        InputStream is = new NullInputStream(100 * 1024 * 1024) {

            @Override
            protected int processByte() {
                return r.nextInt();
            }

            @Override
            protected void processBytes(byte[] bytes, int offset, int length) {
                if (offset == 0 && length == bytes.length) {
                    r.nextBytes(bytes);
                } else {
                    byte[] b = new byte[length];
                    r.nextBytes(b);
                    System.arraycopy(b, 0, bytes, offset, length);
                }
            }
        };
        FileUtils.copyInputStreamToFile(is, file);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteQuietly(file);
    }

    @Test
    public void testBlockCompressorStream() throws Throwable {
        try {
            LOG.info("Total memory is " + Runtime.getRuntime().totalMemory());
            LOG.info("Max memory is " + Runtime.getRuntime().maxMemory());

            FileInputStream fi = new FileInputStream(file);
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
                FileUtils.forceMkdir(new File("build/tmp"));
                FileUtils.writeByteArrayToFile(new File("build/tmp/compressed.out"), cb);

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
