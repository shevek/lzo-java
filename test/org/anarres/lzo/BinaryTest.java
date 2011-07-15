/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class BinaryTest {

	@Test
	public void testShifts() {
		byte[] data = new byte[]{(byte) 0x81, 1, (byte) 0x82, 3};
		int value = LzoCompressor1x.UA_GET32(data, 0);
		System.out.println("Byte is " + Integer.toHexString((data[0] & 0xff) << 24));
		System.out.println("Byte is " + Integer.toHexString((data[1] & 0xff) << 16));
		System.out.println("Byte is " + Integer.toHexString((data[2] & 0xff) << 8));
		System.out.println("Byte is " + Integer.toHexString((data[3] & 0xff) << 0));
		System.out.println("Value is " + Integer.toHexString(value));
		assertEquals(0x81018203, value);
	}
}
