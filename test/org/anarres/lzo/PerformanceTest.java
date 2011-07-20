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

/**
 *
 * @author shevek
 */
public class PerformanceTest {

	private static final Log LOG = LogFactory.getLog(BlockCompressorStreamTest.class);
	private static final String PATH = "/home/shevek/vm/karmasphere-aws-vm/Karmasphere AWS VM-s003.vmdk";

	@Test
	public void testBlockCompress() throws Exception {
		try {
			LOG.info("Total memory is " + Runtime.getRuntime().totalMemory());
			LOG.info("Max memory is " + Runtime.getRuntime().maxMemory());

			FileInputStream fi = new FileInputStream(new File(PATH));
			DataInputStream di = new DataInputStream(fi);
			byte[] data = new byte[512 * 1024 * 1024];
			di.readFully(data);
			LOG.info("Original data is " + data.length + " bytes.");
			byte[] compressed = new byte[data.length];

			for (int i = 0; i < 4; i++) {
				lzo_uintp compressed_length = new lzo_uintp(compressed.length);
				LOG.info("Starting.");
				long start = System.currentTimeMillis();
				LzoCompressor1x_1.compress(data, 0, data.length , compressed, 0, compressed_length, new int[1<<14]);
				long end = System.currentTimeMillis();
				LOG.info("Compression took " + ((end - start) / 1000d) + " ms");

				lzo_uintp uncompressed_length = new lzo_uintp(data.length);
				start = System.currentTimeMillis();
				LzoDecompressor1x.decompress(compressed, 0, compressed_length.value, data, 0, uncompressed_length, null);
				end = System.currentTimeMillis();
				LOG.info("Uncompression took " + ((end - start) / 1000d) + " ms");
			}
		} finally {
			System.out.flush();
			System.err.flush();
			Thread.sleep(100);
		}
	}
}
