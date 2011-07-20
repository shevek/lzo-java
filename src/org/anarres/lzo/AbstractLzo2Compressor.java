/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

/**
 *
 * @author shevek
 */
public abstract class AbstractLzo2Compressor extends AbstractLzoCompressor {

    @Override
    public int getCompressionOverhead(int inputBufferSize) {
        return (inputBufferSize >> 3) + 128 + 3;
    }
}
