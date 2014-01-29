package org.anarres.lzo;

import java.util.Arrays;

public class LzoDecompressor1y_safe extends AbstractLzoDecompressor {

    public LzoDecompressor1y_safe() {
        super(LzoAlgorithm.LZO1Y, LzoConstraint.SAFETY);
    }

#include "lzojava.h"
#include "config1y.h"

#define LZO_TEST_OVERRUN
#define DO_DECOMPRESS       decompress

#include "lzo1x_d.ch"

	@Override
	public int decompress(byte[] in, int in_base, int in_len,
                         byte[] out, int out_base, lzo_uintp out_len) {
		return decompress(in, in_base, in_len,
						  out, out_base, out_len, null);
	}
}
