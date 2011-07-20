/* lzo1x_c.ch -- implementation of the LZO1[XY]-1 compression algorithm

   This file is part of the LZO real-time data compression library.

   Copyright (C) 2011 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2010 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2009 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2008 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2007 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2006 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2005 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2004 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2003 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2002 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2001 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 2000 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1999 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1998 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1997 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1996 Markus Franz Xaver Johannes Oberhumer
   All Rights Reserved.

   The LZO library is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation; either version 2 of
   the License, or (at your option) any later version.

   The LZO library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with the LZO library; see the file COPYING.
   If not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

   Markus F.X.J. Oberhumer
   <markus@oberhumer.com>
   http://www.oberhumer.com/opensource/lzo/
 */



#if 1 && defined(DO_COMPRESS) && !defined(do_compress)
   /* choose a unique name to better help PGO optimizations */
#  define do_compress       LZO_CPP_ECONCAT2(DO_COMPRESS,_core)
#endif

#if defined(UA_GET64)
#  define WANT_lzo_bitops_ctz64 1
#elif defined(UA_GET32)
#  define WANT_lzo_bitops_ctz32 1
#endif
#include "lzo_func.ch"

// Java addition: {
#define lzo_uintptr_t int
private static final int init = 0;
private static final int next = 1;
private static final int try_match = 2;
private static final int literal = 3;
private static final int m_len_done = 4;
// End Java addition: }

/***********************************************************************
// compress a block of data.
************************************************************************/

static __lzo_noinline lzo_uint
do_compress ( const lzo_bytep in, lzo_uint in_base , lzo_uint  in_len,
                    lzo_bytep out, lzo_uint out_base, lzo_uintp out_len,
                    //Java lzo_uint  ti,  lzo_voidp wrkmem)
                    lzo_uint  ti,  lzo_dict_p dict)
{
	PRINT("Call with in_base=" + in_base + ", in_len=" + in_len + "; out_base=" + out_base + ", out_len=" + out_len + "; ti=" + ti);
    //java register const lzo_bytep ip;
    lzo_uint in_ptr = in_base;
    //Java lzo_bytep op;
    lzo_uint out_ptr = out_base;
    //Java const lzo_bytep const in_end = in + in_len;
    lzo_uint in_end = in_base + in_len;
    //Java const lzo_bytep const ip_end = in + in_len - 20;
    lzo_uint ip_end = in_base + in_len - 20;
    //Java const lzo_bytep ii;
    lzo_uint ii;
    //Java lzo_dict_p const dict = (lzo_dict_p) wrkmem;

    //Java op = out;
    //Java ip = in;
    //Java ii = ip - ti;
    ii = in_ptr - ti;

GOTO_INIT()
#define GOTO(x) do { state = x; continue GOTO_LOOP; } while(false)

    //Java ip += ti < 4 ? 4 - ti : 0;
	//Java-note Make sure we have at least 4 bytes after ii.
    in_ptr += ti < 4 ? 4 - ti : 0;
	lzo_uint m_pos = Integer.MIN_VALUE;
	lzo_uint m_len = Integer.MIN_VALUE;
#if !(LZO_DETERMINISTIC)
#else
	lzo_uint m_off = Integer.MIN_VALUE;
#endif
GOTO_LOOP:
    for (;;)
    {
switch (state) { case init: // Java-GOTO
        //Java const lzo_bytep m_pos;
		//Java-moved lzo_uint m_pos;
#if !(LZO_DETERMINISTIC)
        LZO_DEFINE_UNINITIALIZED_VAR(lzo_uint, m_off, 0);
        //Java-moved lzo_uint m_len;
        lzo_uint dindex;
case next:
        //Java if __lzo_unlikely(ip >= ip_end)
        if __lzo_unlikely(in_ptr >= ip_end)
            break GOTO_LOOP;
        DINDEX1(dindex,ip);
        GINDEX(m_pos,m_off,dict,dindex,in);
        if (LZO_CHECK_MPOS_NON_DET(m_pos,m_off,in,ip,M4_MAX_OFFSET))
            GOTO(literal);
#if 1
        if (m_off <= M2_MAX_OFFSET || m_pos[3] == ip[3])
            GOTO(try_match);
        DINDEX2(dindex,ip);
#endif
        GINDEX(m_pos,m_off,dict,dindex,in);
        if (LZO_CHECK_MPOS_NON_DET(m_pos,m_off,in,ip,M4_MAX_OFFSET))
            GOTO(literal);
        if (m_off <= M2_MAX_OFFSET || m_pos[3] == ip[3])
            GOTO(try_match);
        GOTO(literal);

case try_match:
#if defined(UA_GET32)
        if (UA_GET32(m_pos) != UA_GET32(ip))
#else
        //Java if (m_pos[0] != ip[0] || m_pos[1] != ip[1] || m_pos[2] != ip[2] || m_pos[3] != ip[3])
        if (in[m_pos] != in[in_ptr] || in[m_pos + 1] != in[in_ptr + 1] in[m_pos + 2] != in[in_ptr + 2] in[m_pos + 3] != in[in_ptr + 3])
#endif
        {
            /* a literal */
case literal:
            //Java UPDATE_I(dict,0,dindex,ip,in);
			UPDATE_I(dict,0,dindex,in_ptr,in_base);
            //Java ip += 1 + ((ip - ii) >> 5);
			in_ptr += 1 + ((in_ptr - ii) >> 5);
            continue GOTO_LOOP;
        }
/*match:*/
        //Java UPDATE_I(dict,0,dindex,ip,in);
		UPDATE_I(dict,0,dindex,in_ptr,in_base);
#else	// LZO_DETERMINISTIC
        //Java-moved lzo_uint m_off;
        //Java-moved lzo_uint m_len;
        //Java {
        lzo_uint dv;
        lzo_uint dindex;
case literal:
		PRINT("skip a bit");
        //Java ip += 1 + ((ip - ii) >> 5);
		in_ptr += 1 + ((in_ptr - ii) >> 5);
case next:
		PRINT("case next");
        //Java if __lzo_unlikely(ip >= ip_end)
        if __lzo_unlikely(in_ptr >= ip_end)
            break GOTO_LOOP;
        //Java dv = UA_GET32(ip);
        dv = UA_GET32(in, in_ptr);
		PRINT("Data value at in_ptr=" + in_ptr + " is dv=" + Integer.toHexString(dv));
        dindex = DINDEX(dv,ip);
		PRINT("Dictionary index at dindex=" + Integer.toHexString(dindex) + " is dict[dindex]=" + dict[dindex]);
        //Java GINDEX(m_off,m_pos,in+dict,dindex,in);
        GINDEX(m_off,m_pos,in_base+dict,dindex,in_ptr);
        //Java UPDATE_I(dict,0,dindex,ip,in);
        UPDATE_I(dict,0,dindex,in_ptr,in_base);
        //Java if __lzo_unlikely(dv != UA_GET32(m_pos))
		PRINT("Previous data at m_pos=" + m_pos + "; in[m_pos]=" + Integer.toHexString(UA_GET32(in, m_pos)));
        if __lzo_unlikely(dv != UA_GET32(in, m_pos))
            GOTO(literal);
        //Java }
#endif	// LZO_DETERMINISTIC

    /* a match */

        {
        //Java register lzo_uint t = pd(ip,ii);
        register lzo_uint t = pd(in_ptr,ii);
		PRINT("Match at in_ptr = " + in_ptr + " with literal length " + t);
        if (t != 0)
        {
            if (t <= 3)
            {
                //Java op[-2] |= LZO_BYTE(t);
                out[out_ptr -2] |= LZO_BYTE(t);
#if defined(UA_COPY32)
                UA_COPY32(op, ii);
                op += t;
#else
                //Java { do *op++ = *ii++; while (--t > 0); }
                { do out[out_ptr++] = in[ii++]; while (--t > 0); }
#endif
            }
#if defined(UA_COPY32) || defined(UA_COPY64)
            else if (t <= 16)
            {
                //Java *op++ = LZO_BYTE(t - 3);
                out[out_ptr++] = LZO_BYTE(t - 3);
#if defined(UA_COPY64)
                UA_COPY64(op, ii);
                UA_COPY64(op+8, ii+8);
#else
                UA_COPY32(op, ii);
                UA_COPY32(op+4, ii+4);
                UA_COPY32(op+8, ii+8);
                UA_COPY32(op+12, ii+12);
#endif
                op += t;
            }
#endif
            else
            {
                if (t <= 18)
                    //Java *op++ = LZO_BYTE(t - 3);
                    out[out_ptr++] = LZO_BYTE(t - 3);
                else
                {
                    register lzo_uint tt = t - 18;
                    //Java *op++ = 0;
                    out[out_ptr++] = 0;
                    while __lzo_unlikely(tt > 255)
                    {
                        tt -= 255;
#if 1 && (LZO_CC_MSC && (_MSC_VER >= 1400))
                        * (volatile unsigned char *) op++ = 0;
#else
                        //Java *op++ = 0;
                        out[out_ptr++] = 0;
#endif
                    }
                    assert(tt > 0);
                    //Java *op++ = LZO_BYTE(tt);
                    out[out_ptr++] = LZO_BYTE(tt);
                }
#if defined(UA_COPY32) || defined(UA_COPY64)
                do {
#if defined(UA_COPY64)
                    UA_COPY64(op, ii);
                    UA_COPY64(op+8, ii+8);
#else
                    UA_COPY32(op, ii);
                    UA_COPY32(op+4, ii+4);
                    UA_COPY32(op+8, ii+8);
                    UA_COPY32(op+12, ii+12);
#endif
                    op += 16; ii += 16; t -= 16;
                } while (t >= 16); if (t > 0)
#endif
                //Java { do *op++ = *ii++; while (--t > 0); }
                { do out[out_ptr++] = in[ii++]; while (--t > 0); }
            }
        }
        }
        m_len = 4;
        {
#if defined(UA_GET64)
        lzo_uint64 v;
        v = UA_GET64(ip + m_len) ^ UA_GET64(m_pos + m_len);
        if __lzo_unlikely(v == 0) {
            do {
                m_len += 8;
                v = UA_GET64(ip + m_len) ^ UA_GET64(m_pos + m_len);
                if __lzo_unlikely(ip + m_len >= ip_end)
                    GOTO(m_len_done);
            } while (v == 0);
        }
#if (LZO_ABI_LITTLE_ENDIAN) && defined(lzo_bitops_ctz64)
        m_len += lzo_bitops_ctz64(v) / CHAR_BIT;
#elif (LZO_ABI_LITTLE_ENDIAN)
        if ((v & UCHAR_MAX) == 0) do {
            v >>= CHAR_BIT;
            m_len += 1;
        } while ((v & UCHAR_MAX) == 0);
#else
		//Java if (ip[m_len] == m_pos[m_len]) do {
        if (in[in_ptr + m_len] == in[m_pos + m_len]) do {
            m_len += 1;
		//Java } while (ip[m_len] == m_pos[m_len]);
        } while (in[in_ptr + m_len] == in[m_pos + m_len]);
#endif
#elif defined(UA_GET32)
        lzo_uint32 v;
        v = UA_GET32(ip + m_len) ^ UA_GET32(m_pos + m_len);
        if __lzo_unlikely(v == 0) {
            do {
                m_len += 4;
                v = UA_GET32(ip + m_len) ^ UA_GET32(m_pos + m_len);
                if __lzo_unlikely(ip + m_len >= ip_end)
                    GOTO(m_len_done);
            } while (v == 0);
        }
#if (LZO_ABI_LITTLE_ENDIAN) && defined(lzo_bitops_ctz32)
        m_len += lzo_bitops_ctz32(v) / CHAR_BIT;
#elif (LZO_ABI_LITTLE_ENDIAN)
        if ((v & UCHAR_MAX) == 0) do {
            v >>= CHAR_BIT;
            m_len += 1;
        } while ((v & UCHAR_MAX) == 0);
#else
        //Java if (ip[m_len] == m_pos[m_len]) do {
        if (in[in_ptr + m_len] == in[m_pos + m_len]) do {
            m_len += 1;
        //Java } while (ip[m_len] == m_pos[m_len]);
        } while (ip[in_ptr + m_len] == in[m_pos + m_len]);
#endif
#else
        //Java if __lzo_unlikely(ip[m_len] == m_pos[m_len]) {
		PRINT("Matching in_ptr=" + in_ptr + " against m_pos=" + m_pos + " until ip_end=" + ip_end);
        if __lzo_unlikely(in[in_ptr + m_len] == in[m_pos + m_len]) {
            do {
                m_len += 1;
                //Java if __lzo_unlikely(ip + m_len >= ip_end)
                if __lzo_unlikely(in_ptr + m_len >= ip_end) {
					PRINT("Match ran off end of input at " + m_len);
                    GOTO(m_len_done);
				}
            //Java } while (ip[m_len] == m_pos[m_len]);
            } while (in[in_ptr + m_len] == in[m_pos + m_len]);
        }
#endif
        }
case m_len_done:
		PRINT("Building match; in_ptr=" + in_ptr + "; m_pos=" + m_pos + "; m_len=" + m_len);
        //Java m_off = pd(ip,m_pos);
        m_off = pd(in_ptr,m_pos);
		PRINT("Offset from ip_ptr m_off=" + m_off);
        //Java ip += m_len;
		in_ptr += m_len;
        //Java ii = ip;
		ii = in_ptr;
        if (m_len <= M2_MAX_LEN && m_off <= M2_MAX_OFFSET)
        {
			PRINT("Emitting M2 match.");
            m_off -= 1;
#if defined(LZO1X)
            //Java *op++ = LZO_BYTE(((m_len - 1) << 5) | ((m_off & 7) << 2));
            out[out_ptr++] = LZO_BYTE(((m_len - 1) << 5) | ((m_off & 7) << 2));
            //Java *op++ = LZO_BYTE(m_off >> 3);
            out[out_ptr++] = LZO_BYTE(m_off >> 3);
#elif defined(LZO1Y)
            //Java *op++ = LZO_BYTE(((m_len + 1) << 4) | ((m_off & 3) << 2));
            out[out_ptr++] = LZO_BYTE(((m_len + 1) << 4) | ((m_off & 3) << 2));
            //Java *op++ = LZO_BYTE(m_off >> 2);
            out[out_ptr++] = LZO_BYTE(m_off >> 2);
#endif
        }
        else if (m_off <= M3_MAX_OFFSET)
        {
			PRINT("Emitting M3 match.");
            m_off -= 1;
            if (m_len <= M3_MAX_LEN)
                //Java *op++ = LZO_BYTE(M3_MARKER | (m_len - 2));
                out[out_ptr++] = LZO_BYTE(M3_MARKER | (m_len - 2));
            else
            {
                m_len -= M3_MAX_LEN;
                //Java *op++ = M3_MARKER | 0;
                out[out_ptr++] = M3_MARKER | 0;
                while __lzo_unlikely(m_len > 255)
                {
                    m_len -= 255;
#if 1 && (LZO_CC_MSC && (_MSC_VER >= 1400))
                    * (volatile unsigned char *) op++ = 0;
#else
                    //Java *op++ = 0;
                    out[out_ptr++] = 0;
#endif
                }
                //Java *op++ = LZO_BYTE(m_len);
                out[out_ptr++] = LZO_BYTE(m_len);
            }
            //Java *op++ = LZO_BYTE(m_off << 2);
            out[out_ptr++] = LZO_BYTE(m_off << 2);
            //Java *op++ = LZO_BYTE(m_off >> 6);
            out[out_ptr++] = LZO_BYTE(m_off >> 6);
        }
        else
        {
			PRINT("Emitting M4 match.");
            m_off -= 0x4000;
            if (m_len <= M4_MAX_LEN)
                //Java *op++ = LZO_BYTE(M4_MARKER | ((m_off >> 11) & 8) | (m_len - 2));
                out[out_ptr++] = LZO_BYTE(M4_MARKER | ((m_off >> 11) & 8) | (m_len - 2));
            else
            {
                m_len -= M4_MAX_LEN;
                //Java *op++ = LZO_BYTE(M4_MARKER | ((m_off >> 11) & 8));
                out[out_ptr++] = LZO_BYTE(M4_MARKER | ((m_off >> 11) & 8));
                while __lzo_unlikely(m_len > 255)
                {
                    m_len -= 255;
#if 1 && (LZO_CC_MSC && (_MSC_VER >= 1400))
                    * (volatile unsigned char *) op++ = 0;
#else
                    //Java *op++ = 0;
                    out[out_ptr++] = 0;
#endif
                }
                //Java *op++ = LZO_BYTE(m_len);
                out[out_ptr++] = LZO_BYTE(m_len);
            }
            //Java *op++ = LZO_BYTE(m_off << 2);
            out[out_ptr++] = LZO_BYTE(m_off << 2);
            //Java *op++ = LZO_BYTE(m_off >> 6);
            out[out_ptr++] = LZO_BYTE(m_off >> 6);
        }
        GOTO(next);
default: throw new IllegalStateException("Unknown state " + state);
}   // JAVA_GOTO
    }

    //Java *out_len = pd(op, out);
    out_len.value = out_ptr - out_base;
    return pd(in_end,ii);
}


/***********************************************************************
// public entry point
************************************************************************/

LZO_PUBLIC(int)
DO_COMPRESS      ( const lzo_bytep in , lzo_uint in_base, lzo_uint  in_len,
                         lzo_bytep out, lzo_uint out_base, lzo_uintp out_len,
                         lzo_voidp wrkmem )
{
    //Java const lzo_bytep ip = in;
    lzo_uint in_ptr = in_base;
    // lzo_bytep op = out;
    lzo_uint out_ptr = out_base;
    lzo_uint l = in_len;
    lzo_uint t = 0;

    while (l > 20)
    {
        lzo_uint ll = l;
        lzo_uintptr_t ll_end;
#if 0 || (LZO_DETERMINISTIC)
        ll = LZO_MIN(ll, 49152);
#endif
        //Java ll_end = (lzo_uintptr_t)ip + ll;
        ll_end = (lzo_uintptr_t)in_ptr + ll;
        //Java if ((ll_end + ((t + ll) >> 5)) <= ll_end || (const lzo_bytep)(ll_end + ((t + ll) >> 5)) <= ip + ll)
        if ((ll_end + ((t + ll) >> 5)) <= ll_end /*|| (const lzo_bytep)(ll_end + ((t + ll) >> 5)) <= ip + ll*/)
            break;
        lzo_dict_p const dict = (lzo_dict_p) wrkmem;    //Java only
#if (LZO_DETERMINISTIC)
        //Java lzo_memset(wrkmem, 0, ((lzo_uint)1 << D_BITS) * sizeof(lzo_dict_t));
        Arrays.fill(dict, 0);
#endif
        //Java t = do_compress(ip,ll,op,out_len,t,wrkmem);
        t = do_compress(in,in_ptr,ll,out,out_ptr,out_len,t,dict);
        //Java ip += ll;
        in_ptr += ll;
        //Java op += *out_len;
        out_ptr += out_len.value;
        l  -= ll;
    }
    t += l;
    PRINT("do_compress done; trailer now t=" + t + " bytes at out_ptr=" + out_ptr);

    if (t > 0)
    {
        //Java const lzo_bytep ii = in + in_len - t;
        lzo_uint ii = in_base + in_len - t;

        //Java if (op == out && t <= 238)
        if (out_ptr == out_base && t <= 238)
            //Java *op++ = LZO_BYTE(17 + t);
            out[out_ptr++] = LZO_BYTE(17 + t);
        else if (t <= 3)
            //Java op[-2] |= LZO_BYTE(t);
            out[out_ptr -2] |= LZO_BYTE(t);
        else if (t <= 18)
            //Java *op++ = LZO_BYTE(t - 3);
            out[out_ptr++] = LZO_BYTE(t - 3);
        else
        {
            lzo_uint tt = t - 18;

            //Java *op++ = 0;
            out[out_ptr++] = 0;
            while (tt > 255)
            {
                tt -= 255;
#if 1 && (LZO_CC_MSC && (_MSC_VER >= 1400))
                /* prevent the compiler from transforming this loop
                 * into a memset() call */
                * (volatile unsigned char *) op++ = 0;
#else
                //Java *op++ = 0;
                out[out_ptr++] = 0;
#endif
            }
            assert(tt > 0);
            //Java *op++ = LZO_BYTE(tt);
            out[out_ptr++] = LZO_BYTE(tt);
        }
        //Java do *op++ = *ii++; while (--t > 0);
        do out[out_ptr++] = in[ii++]; while (--t > 0);
    }

    //Java *op++ = M4_MARKER | 1;
    out[out_ptr++] = M4_MARKER | 1;
    //Java *op++ = 0;
    out[out_ptr++] = 0;
    //Java *op++ = 0;
    out[out_ptr++] = 0;

    //Java *out_len = pd(op, out);
    out_len.value = out_ptr - out_base;
    return LZO_E_OK;
}


/*
vi:ts=4:et
*/
