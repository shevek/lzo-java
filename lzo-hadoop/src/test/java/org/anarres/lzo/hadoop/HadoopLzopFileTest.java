/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo.hadoop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import org.anarres.lzo.LzopFileTest;
import org.anarres.lzo.hadoop.codec.LzopInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.LineRecordReader;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author shevek
 */
@Ignore // I can't be bothered to make an lzop file right now. :-(
public class HadoopLzopFileTest {

    private static final Log LOG = LogFactory.getLog(BlockCompressorStreamTest.class);

    @Test
    public void testInputFile() throws Exception {
        try {
            File dir = LzopFileTest.getDataDirectory();
            File file = new File(dir, "input.txt.lzo");
            FileInputStream fi = new FileInputStream(file);
            LzopInputStream ci = new LzopInputStream(fi);
            ByteArrayOutputStream bo = new ByteArrayOutputStream((int) (file.length() * 2));
            bo.reset();
            long start = System.currentTimeMillis();
            IOUtils.copy(ci, bo);
            long end = System.currentTimeMillis();
            LOG.info("Uncompression took " + ((end - start) / 1000d) + " ms");
            LOG.info("Uncompressed data is " + bo.size() + " bytes.");
            LOG.info("Uncompressed data is\n" + bo);
        } finally {
            System.out.flush();
            System.err.flush();
            Thread.sleep(100);
        }
    }

    @Test
    public void testLineReader() throws Exception {
        try {
            File dir = LzopFileTest.getDataDirectory();
            File file = new File(dir, "input.txt.lzo");
            FileInputStream fi = new FileInputStream(file);
            LzopInputStream ci = new LzopInputStream(fi);
            LineRecordReader reader = new LineRecordReader(ci, 0L, 9999L, 9999);
            LongWritable key = new LongWritable();
            Text value = new Text();
            while (reader.next(key, value)) {
                LOG.info("key=" + key + "; value=" + value);
            }
        } catch (Exception e) {
            LOG.info("Test failed", e);
            throw e;
        } finally {
            System.out.flush();
            System.err.flush();
            Thread.sleep(100);
        }
    }
}
