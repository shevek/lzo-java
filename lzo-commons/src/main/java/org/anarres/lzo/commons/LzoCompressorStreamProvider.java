package org.anarres.lzo.commons;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.anarres.lzo.LzoAlgorithm;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamProvider;

/**
 * Implements Apache Commons Compress' CompressorStreamProvider
 * interface to make LZO compression available to its
 * CompressorStreamFactory via ServiceLoader.
 */
public class LzoCompressorStreamProvider implements CompressorStreamProvider {
    private static final Set<String> COMPRESSION_ALGORITHMS =
        Collections.unmodifiableSet(new HashSet(Arrays.asList(
            LzoAlgorithm.LZO1X.name(),
            LzoAlgorithm.LZO1Y.name())));
    private static final Set<String> DECOMPRESSION_ALGORITHMS =
        Collections.unmodifiableSet(new HashSet(Arrays.asList(
            LzoAlgorithm.LZO1X.name(),
            LzoAlgorithm.LZO1Y.name(),
            LzoAlgorithm.LZO1Z.name())));

    @Override
    public  Set<String> getInputStreamCompressorNames() {
        return DECOMPRESSION_ALGORITHMS;
    }

    @Override
    public  Set<String> getOutputStreamCompressorNames() {
        return COMPRESSION_ALGORITHMS;
    }

    @Override
    public CompressorInputStream createCompressorInputStream(@Nonnull final String name,
        @Nonnull final InputStream in, final boolean ignoredDecompressUntilEOF) {
        return new LzoCompressorInputStream(in, LzoAlgorithm.valueOf(name));
    }

    @Override
    public CompressorOutputStream createCompressorOutputStream(@Nonnull final String name,
        @Nonnull final OutputStream out) {
        return new LzoCompressorOutputStream(out, LzoAlgorithm.valueOf(name));
    }
}
