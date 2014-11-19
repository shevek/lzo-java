/*
 * This file is part of lzo-java, an implementation of LZO in Java.
 * https://github.com/shevek/lzo-java
 *
 * The Java portion of this library is:
 * Copyright (C) 2011 Shevek <shevek@anarres.org>
 * All Rights Reserved.
 *
 * The preprocessed C portion of this library is:
 * Copyright (C) 2006-2011 Markus Franz Xaver Johannes Oberhumer
 * All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version
 * 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with the LZO library; see the file COPYING.
 * If not, see <http://www.gnu.org/licenses/> or write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.

 * As a special exception, the copyright holders of this file
 * give you permission to link this file with independent
 * modules to produce an executable, regardless of the license
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or
 * based on this library or file. If you modify this file, you may
 * extend this exception to your version of the file, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 */
package org.anarres.lzo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author shevek
 */
public class LzopInputStream extends LzoInputStream {

    private static final Log LOG = LogFactory.getLog(LzopInputStream.class);
    private final int flags;
    private final CRC32 c_crc32_c;
    private final CRC32 c_crc32_d;
    private final Adler32 c_adler32_c;
    private final Adler32 c_adler32_d;
    private boolean eof;

    public LzopInputStream(@Nonnull InputStream in) throws IOException {
        super(in, new LzoDecompressor1x());
        this.flags = readHeader();
        this.c_crc32_c = ((flags & LzopConstants.F_CRC32_C) == 0) ? null : new CRC32();
        this.c_crc32_d = ((flags & LzopConstants.F_CRC32_D) == 0) ? null : new CRC32();
        this.c_adler32_c = ((flags & LzopConstants.F_ADLER32_C) == 0) ? null : new Adler32();
        this.c_adler32_d = ((flags & LzopConstants.F_ADLER32_D) == 0) ? null : new Adler32();
        this.eof = false;
        // logState();
    }

    public int getFlags() {
        return flags;
    }

    @Nonnegative
    public int getCompressedChecksumCount() {
        int out = 0;
        if (c_crc32_c != null)
            out++;
        if (c_adler32_c != null)
            out++;
        return out;
    }

    @Nonnegative
    public int getUncompressedChecksumCount() {
        int out = 0;
        if (c_crc32_d != null)
            out++;
        if (c_adler32_d != null)
            out++;
        return out;
    }

    @Override
    protected void logState(@Nonnull String when) {
        super.logState(when);
        LOG.info(when + " Flags = " + Integer.toHexString(flags));
        // LOG.info(when + " CRC32C = " + c_crc32_c);
        // LOG.info(when + " CRC32D = " + c_crc32_d);
        // LOG.info(when + " Adler32C = " + c_adler32_c);
        // LOG.info(when + " Adler32D = " + c_adler32_d);
    }

    /**
     * Read len bytes into buf, st LSB of int returned is the last byte of the
     * first word read.
     */
    // @Nonnegative ?
    private int readInt(@Nonnull byte[] buf, @Nonnegative int len)
            throws IOException {
        readBytes(buf, 0, len);
        int ret = (0xFF & buf[0]) << 24;
        ret |= (0xFF & buf[1]) << 16;
        ret |= (0xFF & buf[2]) << 8;
        ret |= (0xFF & buf[3]);
        return (len > 3) ? ret : (ret >>> (8 * (4 - len)));
    }

    /**
     * Read bytes, update checksums, return first four bytes as an int, first
     * byte read in the MSB.
     */
    // @Nonnegative ?
    private int readHeaderItem(@Nonnull byte[] buf, @Nonnegative int len, @Nonnull Adler32 adler, @Nonnull CRC32 crc32) throws IOException {
        int ret = readInt(buf, len);
        adler.update(buf, 0, len);
        crc32.update(buf, 0, len);
        Arrays.fill(buf, (byte) 0);
        return ret;
    }

    /**
     * Read and verify an lzo header, setting relevant block checksum options
     * and ignoring most everything else.
     */
    protected int readHeader() throws IOException {
        byte[] buf = new byte[9];
        readBytes(buf, 0, 9);
        if (!Arrays.equals(buf, LzopConstants.LZOP_MAGIC))
            throw new IOException("Invalid LZO header");
        Arrays.fill(buf, (byte) 0);
        Adler32 adler = new Adler32();
        CRC32 crc32 = new CRC32();
        int hitem = readHeaderItem(buf, 2, adler, crc32); // lzop version
        if (hitem > LzopConstants.LZOP_VERSION) {
            LOG.debug("Compressed with later version of lzop: "
                    + Integer.toHexString(hitem) + " (expected 0x"
                    + Integer.toHexString(LzopConstants.LZOP_VERSION) + ")");
        }
        hitem = readHeaderItem(buf, 2, adler, crc32); // lzo library version
        if (hitem > LzoVersion.LZO_LIBRARY_VERSION) {
            throw new IOException("Compressed with incompatible lzo version: 0x"
                    + Integer.toHexString(hitem) + " (expected 0x"
                    + Integer.toHexString(LzoVersion.LZO_LIBRARY_VERSION) + ")");
        }
        hitem = readHeaderItem(buf, 2, adler, crc32); // lzop extract version
        if (hitem > LzopConstants.LZOP_VERSION) {
            throw new IOException("Compressed with incompatible lzop version: 0x"
                    + Integer.toHexString(hitem) + " (expected 0x"
                    + Integer.toHexString(LzopConstants.LZOP_VERSION) + ")");
        }
        hitem = readHeaderItem(buf, 1, adler, crc32); // method
        switch (hitem) {
            case LzopConstants.M_LZO1X_1:
            case LzopConstants.M_LZO1X_1_15:
            case LzopConstants.M_LZO1X_999:
                break;
            default:
                throw new IOException("Invalid strategy " + Integer.toHexString(hitem));
        }
        readHeaderItem(buf, 1, adler, crc32); // ignore level

        // flags
        int flags = readHeaderItem(buf, 4, adler, crc32);
        boolean useCRC32 = (flags & LzopConstants.F_H_CRC32) != 0;
        boolean extraField = (flags & LzopConstants.F_H_EXTRA_FIELD) != 0;
        if ((flags & LzopConstants.F_MULTIPART) != 0)
            throw new IOException("Multipart lzop not supported");
        if ((flags & LzopConstants.F_H_FILTER) != 0)
            throw new IOException("lzop filter not supported");
        if ((flags & LzopConstants.F_RESERVED) != 0)
            throw new IOException("Unknown flags in header");
        // known !F_H_FILTER, so no optional block

        readHeaderItem(buf, 4, adler, crc32); // ignore mode
        readHeaderItem(buf, 4, adler, crc32); // ignore mtime
        readHeaderItem(buf, 4, adler, crc32); // ignore gmtdiff
        hitem = readHeaderItem(buf, 1, adler, crc32); // fn len
        if (hitem > 0) {
            byte[] tmp = (hitem > buf.length) ? new byte[hitem] : buf;
            readHeaderItem(tmp, hitem, adler, crc32); // skip filename
        }
        int checksum = (int) (useCRC32 ? crc32.getValue() : adler.getValue());
        hitem = readHeaderItem(buf, 4, adler, crc32); // read checksum
        if (hitem != checksum) {
            throw new IOException("Invalid header checksum: "
                    + Long.toHexString(checksum) + " (expected 0x"
                    + Integer.toHexString(hitem) + ")");
        }
        if (extraField) { // lzop 1.08 ultimately ignores this
            LOG.debug("Extra header field not processed");
            adler.reset();
            crc32.reset();
            hitem = readHeaderItem(buf, 4, adler, crc32);
            readHeaderItem(new byte[hitem], hitem, adler, crc32);
            checksum = (int) (useCRC32 ? crc32.getValue() : adler.getValue());
            if (checksum != readHeaderItem(buf, 4, adler, crc32)) {
                throw new IOException("Invalid checksum for extra header field");
            }
        }

        return flags;
    }

    private int readChecksum(@CheckForNull Checksum csum) throws IOException {
        if (csum == null)
            return 0;
        // LOG.info("Reading checksum " + csum);
        return readInt(false);
    }

    private void testChecksum(@CheckForNull Checksum csum, int value, @Nonnull byte[] data, @Nonnegative int off, @Nonnegative int len) throws IOException {
        if (csum == null)
            return;
        csum.reset();
        csum.update(data, off, len);
        if (value != (int) csum.getValue())
            throw new IOException("Checksum failure: "
                    + "Expected " + Integer.toHexString(value)
                    + "; got " + Long.toHexString(csum.getValue()));
    }

    @Override
    protected boolean readBlock() throws IOException {
        // logState("Before readBlock");
        if (eof)
            return false;
        int outputBufferLength = readInt(false);
        if (outputBufferLength == 0) {
            // logState("After empty readBlock");
            eof = true;
            return false;
        }
        setOutputBufferSize(outputBufferLength);
        int inputBufferLength = readInt(false);
        setInputBufferSize(inputBufferLength);
        int v_adler32_d = readChecksum(c_adler32_d);
        int v_crc32_d = readChecksum(c_crc32_d);
        // LOG.info("outputBufferLength=" + outputBufferLength + "; inputBufferLength=" + inputBufferLength);
        if (outputBufferLength == inputBufferLength) {
            outputBufferPos = 0;
            outputBufferLen.value = outputBufferLength;
            readBytes(outputBuffer, 0, outputBufferLength);
            testChecksum(c_adler32_d, v_adler32_d, outputBuffer, 0, outputBufferLength);
            testChecksum(c_crc32_d, v_crc32_d, outputBuffer, 0, outputBufferLength);
            // logState("After uncompressed readBlock");
            return true;
        }
        int v_adler32_c = readChecksum(c_adler32_c);
        int v_crc32_c = readChecksum(c_crc32_c);
        readBytes(inputBuffer, 0, inputBufferLength);
        testChecksum(c_adler32_c, v_adler32_c, inputBuffer, 0, inputBufferLength);
        testChecksum(c_crc32_c, v_crc32_c, inputBuffer, 0, inputBufferLength);
        decompress(outputBufferLength, inputBufferLength);
        testChecksum(c_adler32_d, v_adler32_d, outputBuffer, 0, outputBufferLength);
        testChecksum(c_crc32_d, v_crc32_d, outputBuffer, 0, outputBufferLength);
        // logState("After compressed readBlock");
        return true;
    }
}
