package org.anarres.lzo.commons;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoCompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.LzoOutputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;

/**
 *
 * @author shevek
 */
public class LzoCompressorOutputStream extends CompressorOutputStream {

    private final LzoOutputStream out;

    public LzoCompressorOutputStream(@Nonnull OutputStream out) {
        this.out = new LzoOutputStream(out);
    }

    public LzoCompressorOutputStream(@Nonnull OutputStream out, @Nonnull LzoCompressor compressor) {
        this.out = new LzoOutputStream(out, compressor);
    }

    public LzoCompressorOutputStream(@Nonnull OutputStream out, @Nonnull LzoAlgorithm algorithm) {
        this.out = new LzoOutputStream(out, LzoLibrary.getInstance().newCompressor(algorithm, null));
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }
}
