/*
 * This file is part of lzo-java, an implementation of LZO in Java.
 * https://github.com/shevek/lzo-java
 *
 * The Java portion of this library is:
 * Copyright (C) 2011 Shevek <shevek@anarres.org>
 * All Rights Reserved.
 *
 * This file is based on a file from hadoop-gpl-compression.
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
 */
package org.anarres.lzo.hadoop.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.apache.hadoop.io.compress.Compressor;
import org.apache.hadoop.io.compress.Decompressor;

/**
 * A {@link CompressionCodec} for a streaming
 * <b>lzo</b> compression/decompression pair compatible with lzop.
 * http://www.lzop.org/
 */
public class LzopCodec extends LzoCodec {

    @Override
    public CompressionOutputStream createOutputStream(OutputStream out, Compressor compressor) throws IOException {
        Configuration conf = getConf();
        LzoCompressor.CompressionStrategy strategy = LzoCodec.getCompressionStrategy(conf);
        int bufferSize = LzoCodec.getBufferSize(conf);
        return new LzopOutputStream(out, strategy, bufferSize);
    }

    @Override
    public CompressionInputStream createInputStream(InputStream in, Decompressor decompressor) throws IOException {
        // Ensure native-lzo library is loaded & initialized
        return new LzopInputStream(in);
    }

    @Override
    public Decompressor createDecompressor() {
        return null;
    }

    @Override
    public String getDefaultExtension() {
        return ".lzo";
    }
}
