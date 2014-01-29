/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

import java.io.File;
import java.io.FileInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class LzopFileTest {

    public static File getDataDirectory() {
        String path = System.getProperty("test.data.dir");
        File file = new File(path == null ? "src/test/data" : path);
        assertTrue(file.getAbsolutePath() + " is a directory.", file.isDirectory());
        return file;
    }

    private void testLzopFile(String name, long flags) throws Exception {
        try {
            File dir = getDataDirectory();
            File lzo = new File(dir, name);
            File raw = new File(dir, "compressed.txt");

            FileInputStream lzo_fin = new FileInputStream(lzo);
            LzopInputStream lzo_zin = new LzopInputStream(lzo_fin);
            byte[] lzo_data = IOUtils.toByteArray(lzo_zin);

            FileInputStream raw_fin = new FileInputStream(raw);
            byte[] raw_data = IOUtils.toByteArray(raw_fin);

            long tflags = lzo_zin.getFlags() & flags;
            assertEquals("Missing flags in file", flags, tflags);

            assertArrayEquals("Failed to decompress properly.", raw_data, lzo_data);
        } finally {
            System.out.flush();
            System.err.flush();
            Thread.sleep(100);
        }
    }

    @Test
    public void testLzopFile() throws Exception {
        testLzopFile("compressed.txt.lzo.adler32d", LzopConstants.F_ADLER32_D);
        testLzopFile("compressed.txt.lzo.crc32d", LzopConstants.F_CRC32_D);
        testLzopFile("compressed.txt.lzo.adler32", LzopConstants.F_ADLER32_D | LzopConstants.F_ADLER32_C);
        testLzopFile("compressed.txt.lzo.crc32", LzopConstants.F_CRC32_D | LzopConstants.F_CRC32_C);
    }
}
