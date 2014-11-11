package org.anarres.lzo;

import java.util.Arrays;

public class LzoCompressor1y_1 extends AbstractLzo1Compressor {

    public LzoCompressor1y_1() {
        super(LzoAlgorithm.LZO1Y);
    }

    public int getCompressionLevel() {
        return 5; // TODO: check LZO1Y compression level
    }

    @Override 
    public String toString() {
        return "LZO1Y1";
    }


#include "lzojava.h"

#define LZO_NEED_DICT_H 1
#ifndef D_BITS
#define D_BITS          14
#endif
#define D_INDEX1(d,p)       d = DX3(p,5,5,6); d += d >> 5; d = DM(d)
#define D_INDEX2(d,p)       d = (d & (D_MASK & 0x7ff)) ^ (D_HIGH | 0x1f)
#if 1
#define DINDEX(dv,p)        DM(((DMUL(0x1824429d,dv)) >> (32-D_BITS)))
#else
#define DINDEX(dv,p)        DM((dv) + ((dv) >> (32-D_BITS)))
#endif
#include "config1y.h"
#define LZO_DETERMINISTIC (1)

#ifndef DO_COMPRESS
#define DO_COMPRESS     compress
#endif

#include "lzo1x_c.ch"

	private final lzo_dict_p dictionary = new lzo_dict_t[16384];

	public int compress(byte[] in, int in_base, int in_len,
	                    byte[] out, int out_base, lzo_uintp out_len) {
		return compress(in, in_base, in_len,
						out, out_base, out_len,
						dictionary);
	}
}
