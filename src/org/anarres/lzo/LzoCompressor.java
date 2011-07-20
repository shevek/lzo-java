/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

/**
 *
 * @author shevek
 */
public interface LzoCompressor extends LzoTransformer {

    public int getCompressionOverhead(int inputBufferSize);

    public int compress(byte[] in, int in_base, int in_len,
            byte[] out, int out_base, lzo_uintp out_len);
}
