/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

import java.util.Formatter;
import org.junit.Ignore;
import java.io.InputStream;
import java.util.Random;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class Lzo1xTest {

	public void testLzo1x(byte[] orig) {
		System.out.println("\nCompressing " + orig.length + " bytes.");

		// System.out.println("Original:   " + Arrays.toString(orig));

		byte[] compressed = new byte[orig.length * 2];
		lzo_uintp compressed_length = new lzo_uintp(compressed.length);
		int compressed_code = LzoCompressor1x.lzo1x_1_compress(orig, 0, orig.length, compressed, 0, compressed_length, new int[1 << 14]);

		System.out.println("Compressed: " + LzoErrors.toString(compressed_code));
		// System.out.println("Compressed: " + Arrays.toString(Arrays.copyOf(compressed, compressed_length.value)));
		assertEquals(LzoConstants.LZO_E_OK, compressed_code);

		byte[] uncompressed = new byte[orig.length];
		lzo_uintp uncompressed_length = new lzo_uintp(uncompressed.length);
		int uncompressed_code = LzoDecompressor1x.lzo1x_decompress(compressed, 0, compressed_length.value, uncompressed, 0, uncompressed_length, null);

		System.out.println("Output:     " + LzoErrors.toString(uncompressed_code));
		// System.out.println("Output:     " + Arrays.toString(uncompressed));

		assertEquals(LzoConstants.LZO_E_OK, uncompressed_code);
		assertArrayEquals(orig, uncompressed);
	}

	// Totally RLE.
	@Test
	public void testBlank() throws Exception {
		byte[] orig = new byte[512];
		Arrays.fill(orig, (byte) 0);
		testLzo1x(orig);
	}

	// Highly cyclic.
	@Test
	public void testSequence() throws Exception {
		byte[] orig = new byte[512];
		for (int i = 0; i < orig.length; i++)
			orig[i] = (byte) (i & 0xf);
		testLzo1x(orig);
	}

	// Essentially uncompressible.
	@Test
	public void testRandom() throws Exception {
		Random r = new Random();
		for (int i = 0; i < 10; i++) {
			byte[] orig = new byte[256];
			r.nextBytes(orig);
			testLzo1x(orig);
		}
	}

	public void testClass(Class<?> type) throws Exception {
		String name = type.getName();
		name = name.replace('.', '/') + ".class";
		System.out.println("Class is " + name);
		InputStream in = getClass().getClassLoader().getResourceAsStream(name);
		byte[] data = IOUtils.toByteArray(in);
		testLzo1x(data);
	}

	@Test
	public void testClass() throws Exception {
		testClass(getClass());
		testClass(Integer.class);
		testClass(Formatter.class);
	}
}
