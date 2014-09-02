/* lzo1x_d.ch -- implementation of the LZO1X decompression algorithm

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


// Java addition: {
private static final int init = 0;
private static final int copy_match = 1;
private static final int eof_found = 2;
private static final int first_literal_run = 3;
private static final int match = 4;
private static final int match_done = 5;
private static final int match_next = 6;
private static final int input_overrun = 7;
private static final int output_overrun = 8;
private static final int lookbehind_overrun = 9;
// End Java addition: }

#include "lzo1_d.ch"


/***********************************************************************
// decompress a block of data.
************************************************************************/

#if defined(DO_DECOMPRESS)
LZO_PUBLIC(int)
DO_DECOMPRESS  ( const byte[] in, lzo_uint in_base , lzo_uint  in_len,
                       byte[] out, lzo_uint out_base, lzo_uintp out_len,
                       lzo_voidp wrkmem )
#endif
{
    //Java register lzo_bytep op;
	lzo_uint in_ptr = in_base;
    //Java register const lzo_bytep ip;
	lzo_uint out_ptr = out_base;
    register lzo_uint t = Integer.MIN_VALUE;
#if defined(COPY_DICT)
    lzo_uint m_off;
    const lzo_bytep dict_end;
#else
    //Java: lzo_bytep const op_end = out + *out_len;
    //Java: m_pos is always a pointer into op.
    register const lzo_uint m_pos = Integer.MIN_VALUE;
#endif

    const lzo_uint const ip_end = in_ptr + in_len;
#if defined(HAVE_ANY_OP)
    lzo_uint const op_end = out_ptr + out_len.value;
#endif
#if defined(LZO1Z)
    lzo_uint last_m_off = 0;
#endif

    LZO_UNUSED(wrkmem);

	GOTO_INIT();

GOTO_0: {	// Java-goto

#if defined(COPY_DICT)
    if (dict)
    {
        if (dict_len > M4_MAX_OFFSET)
        {
            dict += dict_len - M4_MAX_OFFSET;
            dict_len = M4_MAX_OFFSET;
        }
        dict_end = dict + dict_len;
    }
    else
    {
        dict_len = 0;
        dict_end = NULL;
    }
#endif /* COPY_DICT */

	//Java *out_len = 0;
    out_len.value = 0;

    //Java op = out;
    //Java ip = in;

	//Java if (*ip > 17)
    if (U(in[in_ptr]) > 17)
    {
        //Java t = *ip++ - 17;
        t = U(in[in_ptr++]) - 17;
        if (t < 4)
            //Java goto match_next;
			{ state = match_next; break GOTO_0; }
        assert(t > 0); NEED_OP(t, break GOTO_0); NEED_IP(t+1, break GOTO_0);
        //Java do *op++ = *ip++ while (--t > 0);
        // System.arraycopy(in, in_ptr, out, out_ptr, t);
        // in_ptr += t;
        // out_ptr += t;
		do out[out_ptr++] = in[in_ptr++]; while (--t > 0);
		//Java goto first_literal_run;
        { state = first_literal_run; break GOTO_0; }
    }

}	// GOTO_0	// Java-goto

GOTO_LOOP_OUTER:
    while (TEST_IP && TEST_OP)
    {
	PRINT("Outer loop top");
GOTO_BEGIN(GOTO_PRE)

        //Java t = *ip++;
        t = U(in[in_ptr++]);
        if (t >= 16)
            //Java goto match;
			{ state = match; break GOTO_PRE; }
		PRINT("Literal run: t=" + t);
        /* a literal run */
        if (t == 0)
        {
            NEED_IP(1, break GOTO_LOOP_OUTER);
            //Java while (*ip == 0)
            while (in[in_ptr] == 0)
            {
                t += 255;
                //Java ip++;
                in_ptr++;
                NEED_IP(1, break GOTO_LOOP_OUTER);
            }
            //Java t += 15 + *ip++;
            t += 15 + U(in[in_ptr++]);
        }
        /* copy literals */
        assert(t > 0); NEED_OP(t+3, break GOTO_LOOP_OUTER); NEED_IP(t+4, break GOTO_LOOP_OUTER);
#if defined(LZO_UNALIGNED_OK_8) && defined(LZO_UNALIGNED_OK_4)
        t += 3;
        if (t >= 8) do
        {
            UA_COPY64(op,ip);
            op += 8; ip += 8; t -= 8;
        } while (t >= 8);
        if (t >= 4)
        {
            UA_COPY32(op,ip);
            op += 4; ip += 4; t -= 4;
        }
        if (t > 0)
        {
            *op++ = *ip++;
            if (t > 1) { *op++ = *ip++; if (t > 2) { *op++ = *ip++; } }
        }
#elif defined(LZO_UNALIGNED_OK_4) || defined(LZO_ALIGNED_OK_4)
#if !defined(LZO_UNALIGNED_OK_4)
        if (PTR_ALIGNED2_4(op,ip))
        {
#endif
        UA_COPY32(op,ip);
        op += 4; ip += 4;
        if (--t > 0)
        {
            if (t >= 4)
            {
                do {
                    UA_COPY32(op,ip);
                    op += 4; ip += 4; t -= 4;
                } while (t >= 4);
                if (t > 0) do *op++ = *ip++; while (--t > 0);
            }
            else
                do *op++ = *ip++; while (--t > 0);
        }
#if !defined(LZO_UNALIGNED_OK_4)
        }
        else
#endif
#endif
#if !defined(LZO_UNALIGNED_OK_4) && !defined(LZO_UNALIGNED_OK_8)
        {
            //Java *op++ = *ip++; *op++ = *ip++; *op++ = *ip++;
            //Java do *op++ = *ip++; while (--t > 0);
            t += 3;
            // System.arraycopy(in, in_ptr, out, out_ptr, t);
            // in_ptr += t;
            // out_ptr += t;
			do out[out_ptr++] = in[in_ptr++]; while (--t > 0);
        }
#endif


case first_literal_run:


        //Java t = *ip++;
        t = U(in[in_ptr++]);
		PRINT("First literal match: t=" + t);
        if (t >= 16)
			{ state = match; break GOTO_PRE; }
#if defined(COPY_DICT)
#if defined(LZO1Z)
        m_off = (1 + M2_MAX_OFFSET) + (t << 6) + (*ip++ >> 2);
        last_m_off = m_off;
#else
        m_off = (1 + M2_MAX_OFFSET) + (t >> 2) + (*ip++ << 2);
#endif
        NEED_OP(3);
        t = 3; COPY_DICT(t,m_off)
#else /* !COPY_DICT */
#if defined(LZO1Z)
        //Java t = (1 + M2_MAX_OFFSET) + (t << 6) + (*ip++ >> 2);
        t = (1 + M2_MAX_OFFSET) + (t << 6) + (U(in[in_ptr++]) >> 2);
        //Java m_pos = op - t;
		m_pos = out_ptr - t;
        last_m_off = t;
#else
        //Java m_pos = op - (1 + M2_MAX_OFFSET);
        m_pos = out_ptr - (1 + M2_MAX_OFFSET);
        m_pos -= t >> 2;
        //Java m_pos -= *ip++ << 2;
        m_pos -= U(in[in_ptr++]) << 2;
#endif
		PRINT("Going to copy first literal match; m_pos=" + m_pos + "; len=3(const)");
        TEST_LB(m_pos, break GOTO_LOOP_OUTER); NEED_OP(3, break GOTO_LOOP_OUTER);
        //Java *op++ = *m_pos++; *op++ = *m_pos++; *op++ = *m_pos;
        out[out_ptr++] = out[m_pos++];
        out[out_ptr++] = out[m_pos++];
        out[out_ptr++] = out[m_pos];
#endif /* COPY_DICT */
        //Java goto match_done;
		{ state = match_done; break GOTO_PRE; }

case match:
case match_next:
	break GOTO_PRE;
case input_overrun:
case output_overrun:
case lookbehind_overrun:
	break GOTO_LOOP_OUTER;
GOTO_END_UNREACHABLE()

// Enter the inner loop:

GOTO_LOOP_INNER:
        /* handle matches */
        do {
	PRINT("Inner loop top; t=" + t + ", state=" + state);
GOTO_BEGIN(GOTO_INNER)
case match:
            if (t >= 64)                /* a M2 match */
            {
#if defined(COPY_DICT)
#if defined(LZO1X)
                m_off = 1 + ((t >> 2) & 7) + (*ip++ << 3);
                t = (t >> 5) - 1;
#elif defined(LZO1Y)
                m_off = 1 + ((t >> 2) & 3) + (*ip++ << 2);
                t = (t >> 4) - 3;
#elif defined(LZO1Z)
                m_off = t & 0x1f;
                if (m_off >= 0x1c)
                    m_off = last_m_off;
                else
                {
                    m_off = 1 + (m_off << 6) + (*ip++ >> 2);
                    last_m_off = m_off;
                }
                t = (t >> 5) - 1;
#endif
#else /* !COPY_DICT */
#if defined(LZO1X)
                //Java m_pos = op - 1;
                m_pos = out_ptr - 1;
                m_pos -= (t >> 2) & 7;
                //Java m_pos -= *ip++ << 3;
                m_pos -= U(in[in_ptr++]) << 3;
                t = (t >> 5) - 1;
#elif defined(LZO1Y)
                //Java m_pos = op - 1;
                m_pos = out_ptr - 1;
                m_pos -= (t >> 2) & 3;
                //Java m_pos -= *ip++ << 2;
                m_pos -= U(in[in_ptr++]) << 2;
                t = (t >> 4) - 3;
#elif defined(LZO1Z)
                {
                    lzo_uint off = t & 0x1f;
                    //Java m_pos = op;
					m_pos = out_ptr;
                    if (off >= 0x1c)
                    {
                        assert(last_m_off > 0);
                        m_pos -= last_m_off;
                    }
                    else
                    {
                        //Java off = 1 + (off << 6) + (*ip++ >> 2);
                        off = 1 + (off << 6) + (U(in[in_ptr++]) >> 2);
                        m_pos -= off;
                        last_m_off = off;
                    }
                }
                t = (t >> 5) - 1;
#endif
				PRINT("Going to copy M4; m_pos=" + m_pos + "; t=" + t);
                TEST_LB(m_pos, break GOTO_LOOP_OUTER); assert(t > 0); NEED_OP(t+3-1, break GOTO_LOOP_OUTER);
                //Java goto copy_match;
                { state = copy_match; continue GOTO_INNER; }
#endif /* COPY_DICT */
            }
            else if (t >= 32)           /* a M3 match */
            {
                t &= 31;
                if (t == 0)
                {
                    NEED_IP(1, break GOTO_LOOP_OUTER);
                    //Java while (*ip == 0)
                    while (in[in_ptr] == 0)
                    {
                        t += 255;
                        //Java ip++;
                        in_ptr++;
                        NEED_IP(1, break GOTO_LOOP_OUTER);
                    }
                    //Java t += 31 + *ip++;
                    t += 31 + U(in[in_ptr++]);
                }
#if defined(COPY_DICT)
#if defined(LZO1Z)
                m_off = 1 + (ip[0] << 6) + (ip[1] >> 2);
                last_m_off = m_off;
#else
                m_off = 1 + (ip[0] >> 2) + (ip[1] << 6);
#endif
#else /* !COPY_DICT */
#if defined(LZO1Z)
                {
                    // lzo_uint off = 1 + (ip[0] << 6) + (ip[1] >> 2);
                    lzo_uint off = 1 + (U(in[in_ptr]) << 6) + (U(in[in_ptr + 1]) >> 2);
                    //Java m_pos = op - off;
                    m_pos = out_ptr - off;
                    last_m_off = off;
                }
#elif defined(LZO_UNALIGNED_OK_2) && defined(LZO_ABI_LITTLE_ENDIAN)
                //Java m_pos = op - 1;
                m_pos = out_ptr - 1;
                m_pos -= UA_GET16(ip) >> 2;
#else
                //Java m_pos = op - 1;
                m_pos = out_ptr - 1;
                //Java m_pos -= (ip[0] >> 2) + (ip[1] << 6);
                m_pos -= (U(in[in_ptr]) >> 2) + (U(in[in_ptr + 1]) << 6);
#endif
#endif /* COPY_DICT */
                //Java ip += 2;
                in_ptr += 2;
				PRINT("Going to copy M3; m_pos=" + m_pos + "; t=" + t);
            }
            else if (t >= 16)           /* a M4 match */
            {
#if defined(COPY_DICT)
                m_off = (t & 8) << 11;
#else /* !COPY_DICT */
                //Java m_pos = op;
                m_pos = out_ptr;
                m_pos -= (t & 8) << 11;
#endif /* COPY_DICT */
                t &= 7;
                if (t == 0)
                {
                    NEED_IP(1, break GOTO_LOOP_OUTER);
                    //Java while (*ip == 0)
                    while (in[in_ptr] == 0)
                    {
                        t += 255;
                        //Java ip++;
                        in_ptr++;
                        NEED_IP(1, break GOTO_LOOP_OUTER);
                    }
                    //Java t += 7 + *ip++;
                    t += 7 + U(in[in_ptr++]);
                }
#if defined(COPY_DICT)
#if defined(LZO1Z)
                //Java m_off += (ip[0] << 6) + (ip[1] >> 2);
                m_off += (U(in[in_ptr]) << 6) + (U(in[in_ptr + 1]) >> 2);
#else
                //Java m_off += (ip[0] >> 2) + (ip[1] << 6);
                m_off += (U(in[in_ptr]) >> 2) + (U(in[in_ptr + 1]) << 6);
#endif
                //Java ip += 2;
                in_ptr += 2;
                if (m_off == 0)
                    //Java goto eof_found;
					{ state = eof_found; break GOTO_LOOP_OUTER; }
                m_off += 0x4000;
#if defined(LZO1Z)
                last_m_off = m_off;
#endif
#else /* !COPY_DICT */
#if defined(LZO1Z)
                //Java m_pos -= (ip[0] << 6) + (ip[1] >> 2);
                m_pos -= (U(in[in_ptr]) << 6) + (U(in[in_ptr + 1]) >> 2);
#elif defined(LZO_UNALIGNED_OK_2) && defined(LZO_ABI_LITTLE_ENDIAN)
                m_pos -= UA_GET16(ip) >> 2;
#else
                //Java m_pos -= (ip[0] >> 2) + (ip[1] << 6);
                m_pos -= (U(in[in_ptr]) >> 2) + (U(in[in_ptr + 1]) << 6);
#endif
                //Java ip += 2;
                in_ptr += 2;
                //Java if (m_pos == op)
                if (m_pos == out_ptr)
                    //Java goto eof_found;
					{ state = eof_found; break GOTO_LOOP_OUTER; }
                m_pos -= 0x4000;
#if defined(LZO1Z)
                //Java last_m_off = pd((const lzo_bytep)op, m_pos);
                last_m_off = pd(out_ptr, m_pos);
#endif
#endif /* COPY_DICT */
            }
            else                            /* a M1 match */
            {
#if defined(COPY_DICT)
#if defined(LZO1Z)
                m_off = 1 + (t << 6) + (*ip++ >> 2);
                last_m_off = m_off;
#else
                m_off = 1 + (t >> 2) + (*ip++ << 2);
#endif
                NEED_OP(2);
                t = 2; COPY_DICT(t,m_off)
#else /* !COPY_DICT */
#if defined(LZO1Z)
                //Java t = 1 + (t << 6) + (*ip++ >> 2);
                t = 1 + (t << 6) + (U(in[in_ptr++]) >> 2);
                //Java m_pos = op - t;
                m_pos = out_ptr - t;
                last_m_off = t;
#else
                //Java m_pos = op - 1;
                m_pos = out_ptr - 1;
                m_pos -= t >> 2;
                //Java m_pos -= *ip++ << 2;
                m_pos -= U(in[in_ptr++]) << 2;
#endif
				PRINT("Going to copy M1; m_pos=" + m_pos + "; length=2");
                TEST_LB(m_pos, break GOTO_LOOP_OUTER); NEED_OP(2, break GOTO_LOOP_OUTER);
                //Java *op++ = *m_pos++; *op++ = *m_pos;
                out[out_ptr++] = out[m_pos++];
                out[out_ptr++] = out[m_pos];
#endif /* COPY_DICT */
                //Java goto match_done;
				{ state = match_done; continue GOTO_INNER; }
            }

            /* copy match */
#if defined(COPY_DICT)

            NEED_OP(t+3-1);
            t += 3-1; COPY_DICT(t,m_off)

#else /* !COPY_DICT */

			PRINT("Copy " + (t+2) + " bytes from " + m_pos + " to " + out_ptr);
            TEST_LB(m_pos, break GOTO_LOOP_OUTER); assert(t > 0); NEED_OP(t+3-1, break GOTO_LOOP_OUTER);
#if defined(LZO_UNALIGNED_OK_8) && defined(LZO_UNALIGNED_OK_4)
            if (op - m_pos >= 8)
            {
                t += (3 - 1);
                if (t >= 8) do
                {
                    UA_COPY64(op,m_pos);
                    op += 8; m_pos += 8; t -= 8;
                } while (t >= 8);
                if (t >= 4)
                {
                    UA_COPY32(op,m_pos);
                    op += 4; m_pos += 4; t -= 4;
                }
                if (t > 0)
                {
                    *op++ = m_pos[0];
                    if (t > 1) { *op++ = m_pos[1]; if (t > 2) { *op++ = m_pos[2]; } }
                }
            }
            else
#elif defined(LZO_UNALIGNED_OK_4) || defined(LZO_ALIGNED_OK_4)
#if !defined(LZO_UNALIGNED_OK_4)
            if (t >= 2 * 4 - (3 - 1) && PTR_ALIGNED2_4(op,m_pos))
            {
                assert((op - m_pos) >= 4);  /* both pointers are aligned */
#else
            if (t >= 2 * 4 - (3 - 1) && (op - m_pos) >= 4)
            {
#endif
                UA_COPY32(op,m_pos);
                op += 4; m_pos += 4; t -= 4 - (3 - 1);
                do {
                    UA_COPY32(op,m_pos);
                    op += 4; m_pos += 4; t -= 4;
                } while (t >= 4);
                if (t > 0) do *op++ = *m_pos++; while (--t > 0);
            }
            else
#endif
case copy_match:
            {
                //Java *op++ = *m_pos++; *op++ = *m_pos++;
                //Java do *op++ = *m_pos++; while (--t > 0);
                t += 2;
				do out[out_ptr++] = out[m_pos++]; while (--t > 0);
            }

#endif /* COPY_DICT */

case match_done:
#if defined(LZO1Z)
            //Java t = ip[-1] & 3;
            t = in[in_ptr -1] & 3;
#else
            //Java t = ip[-2] & 3;
            t = in[in_ptr -2] & 3;
#endif
            if (t == 0)
                break GOTO_LOOP_INNER;

            /* copy literals */
case match_next:
            assert(t > 0); assert(t < 4); NEED_OP(t, break GOTO_LOOP_OUTER); NEED_IP(t+1, break GOTO_LOOP_OUTER);
//Java #if 0
#if 1
            //Java do *op++ = *ip++; while (--t > 0);
            do out[out_ptr++] = in[in_ptr++]; while (--t > 0);
#else
            *op++ = *ip++;
            if (t > 1) { *op++ = *ip++; if (t > 2) { *op++ = *ip++; } }
#endif
            //Java t = *ip++;
			t = U(in[in_ptr++]);
GOTO_END()
		state = init;
        } while (TEST_IP && TEST_OP);
// GOTO_LOOP_INNER

		state = init;
    }
// GOTO_LOOP_OUTER

GOTO_BEGIN(GOTO_3)
#if defined(HAVE_TEST_IP) || defined(HAVE_TEST_OP)
    /* no EOF code was found */
    //Java *out_len = pd(op, out);
    out_len.value = out_ptr - out_base;
    return LZO_E_EOF_NOT_FOUND;
#endif

case eof_found:
    assert(t == 1);
    //Java *out_len = pd(op, out);
    out_len.value = out_ptr - out_base;
    //Java return (ip == ip_end ? LZO_E_OK :
    //Java (ip < ip_end  ? LZO_E_INPUT_NOT_CONSUMED : LZO_E_INPUT_OVERRUN));
    return (in_ptr == ip_end ? LZO_E_OK :
           (in_ptr < ip_end  ? LZO_E_INPUT_NOT_CONSUMED : LZO_E_INPUT_OVERRUN));


#if defined(HAVE_NEED_IP)
case input_overrun:
    //Java *out_len = pd(op, out);
    out_len.value = out_ptr - out_base;
    return LZO_E_INPUT_OVERRUN;
#endif

#if defined(HAVE_NEED_OP)
case output_overrun:
    //Java *out_len = pd(op, out);
    out_len.value = out_ptr - out_base;
    return LZO_E_OUTPUT_OVERRUN;
#endif

#if defined(LZO_TEST_OVERRUN_LOOKBEHIND)
case lookbehind_overrun:
    //Java *out_len = pd(op, out);
    out_len.value = out_ptr - out_base;
    return LZO_E_LOOKBEHIND_OVERRUN;
#endif

GOTO_END_UNREACHABLE()
}


/*
vi:ts=4:et
*/

