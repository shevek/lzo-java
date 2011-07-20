/*
 * This file is part of Hadoop-Gpl-Compression.
 *
 * Hadoop-Gpl-Compression is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Hadoop-Gpl-Compression is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hadoop-Gpl-Compression.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.anarres.lzo.hadoop.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.compress.BlockCompressorStream;
import org.apache.hadoop.io.compress.BlockDecompressorStream;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.apache.hadoop.io.compress.Compressor;
import org.apache.hadoop.io.compress.Decompressor;

/**
 * A {@link org.apache.hadoop.io.compress.CompressionCodec} for a streaming
 * <b>lzo</b> compression/decompression pair.
 * http://www.oberhumer.com/opensource/lzo/
 * 
 */
public class LzoCodec extends Configured implements CompressionCodec {

	private static final Log LOG = LogFactory.getLog(LzoCodec.class.getName());

	@Override
	public CompressionOutputStream createOutputStream(OutputStream out)
			throws IOException {
		return createOutputStream(out, createCompressor());
	}

	@Override
	public CompressionOutputStream createOutputStream(OutputStream out,
			Compressor compressor) throws IOException {
		/**
		 * <b>http://www.oberhumer.com/opensource/lzo/lzofaq.php</b>
		 *
		 * How much can my data expand during compression ?
		 * ================================================
		 * LZO will expand incompressible data by a little amount.
		 * I still haven't computed the exact values, but I suggest using
		 * these formulas for a worst-case expansion calculation:
		 *
		 * Algorithm LZO1, LZO1A, LZO1B, LZO1C, LZO1F, LZO1X, LZO1Y, LZO1Z:
		 * ----------------------------------------------------------------
		 * output_block_size = input_block_size + (input_block_size / 16) + 64 + 3
		 *
		 * This is about 106% for a large block size.
		 *
		 * Algorithm LZO2A:
		 * ----------------
		 * output_block_size = input_block_size + (input_block_size / 8) + 128 + 3
		 */
		// Create the lzo output-stream
		Configuration conf = getConf();
		LzoCompressor.CompressionStrategy strategy =
				LzoCompressor.CompressionStrategy.valueOf(
				conf.get("io.compression.codec.lzo.compressor",
				LzoCompressor.CompressionStrategy.LZO1X_1.name()));
		int bufferSize =
				conf.getInt("io.compression.codec.lzo.buffersize", 64 * 1024);
		int compressionOverhead = strategy.name().contains("LZO1")
				? (bufferSize >> 4) + 64 + 3
				: (bufferSize >> 3) + 128 + 3;

		return new BlockCompressorStream(out, compressor, bufferSize,
				compressionOverhead);
	}

	@Override
	public Class<? extends Compressor> getCompressorType() {
		return LzoCompressor.class;
	}

	@Override
	public Compressor createCompressor() {
		Configuration conf = getConf();
		LzoCompressor.CompressionStrategy strategy =
				LzoCompressor.CompressionStrategy.valueOf(
				conf.get("io.compression.codec.lzo.compressor",
				LzoCompressor.CompressionStrategy.LZO1X_1.name()));
		int bufferSize =
				conf.getInt("io.compression.codec.lzo.buffersize", 64 * 1024);

		return new LzoCompressor(strategy, bufferSize);
	}

	@Override
	public CompressionInputStream createInputStream(InputStream in)
			throws IOException {
		return createInputStream(in, createDecompressor());
	}

	@Override
	public CompressionInputStream createInputStream(InputStream in,
			Decompressor decompressor)
			throws IOException {
		Configuration conf = getConf();
		return new BlockDecompressorStream(in, decompressor,
				conf.getInt("io.compression.codec.lzo.buffersize", 64 * 1024));
	}

	@Override
	public Class<? extends Decompressor> getDecompressorType() {
		return LzoDecompressor.class;
	}

	@Override
	public Decompressor createDecompressor() {
		Configuration conf = getConf();
		LzoDecompressor.CompressionStrategy strategy =
				LzoDecompressor.CompressionStrategy.valueOf(
				conf.get("io.compression.codec.lzo.decompressor",
				LzoDecompressor.CompressionStrategy.LZO1X.name()));
		int bufferSize =
				conf.getInt("io.compression.codec.lzo.buffersize", 64 * 1024);

		return new LzoDecompressor(strategy, bufferSize);
	}

	/**
	 * Get the default filename extension for this kind of compression.
	 * @return the extension including the '.'
	 */
	@Override
	public String getDefaultExtension() {
		return ".lzo_deflate";
	}
}
