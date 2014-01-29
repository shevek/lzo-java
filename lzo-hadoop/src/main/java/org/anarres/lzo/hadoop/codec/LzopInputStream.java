/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo.hadoop.codec;

import java.io.IOException;
import java.io.InputStream;
import org.apache.hadoop.io.compress.CompressionInputStream;

/**
 *
 * @author shevek
 */
public class LzopInputStream extends CompressionInputStream {

    public LzopInputStream(InputStream in) throws IOException {
        super(new org.anarres.lzo.LzopInputStream(in));
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        return in.read(buf, off, len);
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public void resetState() throws IOException {
    }
}
