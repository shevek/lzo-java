/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

/**
 *
 * @author shevek
 */
public abstract class AbstractLzo1Compressor extends AbstractLzoCompressor {

    @Override
    public int getCompressionOverhead(int inputBufferSize) {
        return (inputBufferSize >> 4) + 64 + 3;
    }
}
