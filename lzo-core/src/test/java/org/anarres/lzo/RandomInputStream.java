/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

import java.util.Random;
import org.apache.commons.io.input.NullInputStream;

/**
 *
 * @author shevek
 */
public class RandomInputStream extends NullInputStream {

    private final Random r = new Random();

    public RandomInputStream(long size) {
        super(size);
    }

    @Override
    protected int processByte() {
        return r.nextInt() & 0xff;
    }

    @Override
    protected void processBytes(byte[] bytes, int offset, int length) {
        r.nextBytes(bytes);
    }
}
