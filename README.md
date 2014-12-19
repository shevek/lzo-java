LZO for Java
============

Introduction
------------

There is no version of LZO in pure Java. The obvious solution is to
take the C source code, and feed it to the Java compiler, modifying
the Java compiler as necessary to make it compile.

This package is an implementation of that obvious solution, for which
I can only apologise to the world.

It turns out, however, that the compression performance on a single
2.4GHz laptop CPU is in excess of 500Mb/sec, and decompression runs
at 815Mb/sec, which seems to be more than adequate. Run
PerformanceTest on an appropriate file to reproduce these figures.

Example
-------

Compression:

```
	OutputStream out = ...;
	LzoAlgorithm algorithm = LzoAlgorithm.LZO1X;
	LzoCompressor compressor = LzoLibrary.getInstance().newCompressor(algorithm, null);
	LzoOutputStream stream = new LzoOutputStream(out, compressor, 256);
	stream.write(...);
```

Decompression:

```
	InputStream in = ...;
	LzoAlgorithm algorithm = LzoAlgorithm.LZO1X;
	LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(algorithm, null);
	LzoInputStream stream = new LzoInputStream(in, decompressor);
	stream.read(...);
```

Documentation
-------------

The [JavaDoc API](http://shevek.github.io/lzo-java/docs/javadoc/)
is available.

Hadoop Notes
------------

Notes on BlockCompressionStream, as of Hadoop 0.21.x:

* If you write 1 byte, then a large block, BlockCompressorStream will
flush the single-byte block before compressing the large block. This
is inefficient.

* If you write a large block to a fresh stream, BlockCompressorStream
will flush existing data, which will write a zero uncompressed
length to the file, but follow it with no blocks, thus breaking the
ulen-clen-data format. This is wrong. There is no contract for the
finished() method to avoid this, since it must return false at the
top of write(), then must (with no other mutator calls) return true
in BlockCompressorStream.finish() in order to avoid the empty block;
having returned true there, compress() must be able to return a
nonempty block, even though we have no data. This is wrong.

* Large blocks are written (ulen (clen data)*) not (ulen clen data)*
due to the loop in compress(). This is not the same as the format for
lzop, thus a data file written using LzopCodec cannot be read by lzop.
See lzop-1.03/src/p_lzo.c method lzo_compress, which contains a
single very simple loop, which is how Hadoop's BlockCompressorStream
should be written. This is both inefficient and wrong.

* If the LZO compressor needs to use its holdover field (or,
equivalently in other people's code, setInputFromSavedData()),
then the ulen-clen-data format is broken because getBytesRead()
MUST return the full number of bytes passed to setInput(), not
just the number of bytes actually compressed so far; then if there
is holdover data, there is nowhere for it to go but into the
returned data from a second call to compress(), at which point the
API has forced us to break ulen-clen-data, as per lzop's file
format. This is wrong, and badly designed.

* The number of uncompressed bytes is written to the stream in lzop.
There is therefore no excuse for a "Buffer too small" error in
decompression. However, this value is NOT used to resize the
decompressor's output buffer, and so the error occurs. One cannot,
as a rule, know the size of output buffer required to decompress a
given file, so Hadoop must be configured by trial and error. This
is badly designed, and harder to use.

