package org.anarres.lzo.commons;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoDecompressor;
import org.anarres.lzo.LzoInputStream;
import org.anarres.lzo.LzoLibrary;
import org.apache.commons.compress.compressors.CompressorInputStream;

/**
 *
 * @author shevek
 */
public class LzoCompressorInputStream extends CompressorInputStream {

    private final LzoInputStream in;

    public LzoCompressorInputStream(@Nonnull InputStream in, @Nonnull LzoDecompressor decompressor) {
        this.in = new LzoInputStream(in, decompressor);
    }

    public LzoCompressorInputStream(@Nonnull InputStream in, @Nonnull LzoAlgorithm algorithm) {
        this.in = new LzoInputStream(in, LzoLibrary.getInstance().newDecompressor(algorithm, null));
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        count(b < 0 ? -1 : 1);
        return b;
    }

}
