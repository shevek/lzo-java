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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import javax.annotation.CheckForNull;
import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author shevek
 */
public class LzopOutputStream extends LzoOutputStream {

    private static final Log LOG = LogFactory.getLog(LzopOutputStream.class);
    private final long flags;
    private final CRC32 c_crc32_c;
    private final CRC32 c_crc32_d;
    private final Adler32 c_adler32_c;
    private final Adler32 c_adler32_d;
    private boolean closed = false;

    /**
     * Constructs a new LzopOutputStream.
     * <p>
     * I recommend limiting flags to the following unless you REALLY know what
     * you are doing:
     * <ul>
     * <li>{@link LzopConstants#F_ADLER32_C}</li>
     * <li>{@link LzopConstants#F_ADLER32_D}</li>
     * <li>{@link LzopConstants#F_CRC32_C}</li>
     * <li>{@link LzopConstants#F_CRC32_D}</li>
     * </ul>
     */
    public LzopOutputStream(OutputStream out, LzoCompressor compressor, @CheckForSigned int inputBufferSize, long flags) throws IOException {
        super(out, compressor, inputBufferSize);
        this.flags = flags;
        this.c_crc32_c = ((flags & LzopConstants.F_CRC32_C) == 0) ? null : new CRC32();
        this.c_crc32_d = ((flags & LzopConstants.F_CRC32_D) == 0) ? null : new CRC32();
        this.c_adler32_c = ((flags & LzopConstants.F_ADLER32_C) == 0) ? null : new Adler32();
        this.c_adler32_d = ((flags & LzopConstants.F_ADLER32_D) == 0) ? null : new Adler32();
        writeLzopHeader();
    }

    public LzopOutputStream(@Nonnull OutputStream out, @Nonnull LzoCompressor compressor, @CheckForSigned int inputBufferSize) throws IOException {
        this(out, compressor, inputBufferSize, 0L);
    }

    public LzopOutputStream(@Nonnull OutputStream out, @Nonnull LzoCompressor compressor) throws IOException {
        this(out, compressor, 256 * 1024);
    }

    /**
     * Writes an lzop-compatible header to the OutputStream provided.
     */
    protected void writeLzopHeader() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dob = new DataOutputStream(bos);
        try {
            dob.writeShort(LzopConstants.LZOP_VERSION);
            dob.writeShort(LzoVersion.LZO_LIBRARY_VERSION);
            dob.writeShort(LzopConstants.LZOP_COMPAT_VERSION);
            switch (getAlgorithm()) {
                case LZO1X:
                    LzoConstraint[] constraints = getConstraints();

                    if (constraints.length == 0) {
                        dob.writeByte(LzopConstants.M_LZO1X_1);
                        dob.writeByte(5);
                    } else if (constraints.length == 1 && constraints[0] == LzoConstraint.COMPRESSION) {
                        dob.writeByte(LzopConstants.M_LZO1X_999);
                        dob.writeByte(getCompressor().getCompressionLevel());
                    } else
                        throw new IOException("Unsupported constraint combination for LZO1X: " + Arrays.toString(constraints));

                    break;
                /*
                 case LZO1X_15:
                 dob.writeByte(LzopConstants.M_LZO1X_1_15);
                 dob.writeByte(1);
                 break;
                 */
                default:
                    throw new IOException("Incompatible lzop algorithm " + getAlgorithm());
            }
            long mask = LzopConstants.F_ADLER32_C | LzopConstants.F_ADLER32_D;
            mask = mask | LzopConstants.F_CRC32_C | LzopConstants.F_CRC32_D;
            dob.writeInt((int) (flags & mask & 0xFFFFFFFF)); // all flags 0
            dob.writeInt(33188); // mode
            dob.writeInt((int) (System.currentTimeMillis() / 1000)); // mtime
            dob.writeInt(0); // gmtdiff ignored
            dob.writeByte(0); // no filename
            Adler32 headerChecksum = new Adler32();
            headerChecksum.update(bos.toByteArray());
            // headerChecksum.update(dob.getData(), 0, dob.getLength());
            int hc = (int) headerChecksum.getValue();
            dob.writeInt(hc);
            out.write(LzopConstants.LZOP_MAGIC);
            // out.write(dob.getData(), 0, dob.getLength());
            out.write(bos.toByteArray());
        } finally {
            dob.close();
        }
    }

    private void writeChecksum(@CheckForNull Checksum csum, @Nonnull byte[] data, @Nonnegative int off, @Nonnegative int len) throws IOException {
        if (csum == null)
            return;
        csum.reset();
        csum.update(data, off, len);
        long value = csum.getValue();
        // LOG.info("Writing checksum " + csum);
        writeInt((int) (value & 0xFFFFFFFF));
    }

    @Override
    protected void writeBlock(@Nonnull byte[] inputData, @Nonnegative int inputPos, @Nonnegative int inputLen, @Nonnull byte[] outputData, @Nonnegative int outputPos, @Nonnegative int outputLen) throws IOException {
        // LOG.info("inputLen=" + inputLen + "; outputLen=" + outputLen);
        writeInt(inputLen);
        if (outputLen < inputLen)
            writeInt(outputLen);
        else
            writeInt(inputLen);

        // This is where we put checksums, if any.
        writeChecksum(c_adler32_d, inputData, inputPos, inputLen);
        writeChecksum(c_crc32_d, inputData, inputPos, inputLen);
        if (outputLen < inputLen) {
            writeChecksum(c_adler32_c, outputData, outputPos, outputLen);
            writeChecksum(c_crc32_c, outputData, outputPos, outputLen);
        }

        if (outputLen < inputLen)
            out.write(outputData, outputPos, outputLen);
        else
            out.write(inputData, inputPos, inputLen);
    }

    /**
     * Writes a null word to the underlying output stream, then closes it.
     */
    @Override
    public void close() throws IOException {
        if (!closed) {
            flush();
            out.write(new byte[]{0, 0, 0, 0});
            super.close();
            closed = true;
        }
    }
}
