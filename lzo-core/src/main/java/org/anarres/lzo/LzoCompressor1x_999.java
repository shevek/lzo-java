package org.anarres.lzo;

import java.util.Arrays;

class Lzo1x999T {
    int bp;
    int ip;
    int in_end;
    int out_base;

    int look;
    int m_len;
    int m_off;
    int r1_lit;
}

class Lzo1x999SWD {

    final byte[] b = new byte[0xCFFF];

    final int[] best_off = new int[34];
    final int[] best_pos = new int[34];
    final int[] head3 = new int[0x4000];
    final int[] succ3 = new int[0xC7FF];
    final int[] best3 = new int[0xC7FF];
    final int[] llen3 = new int[0x4000];
    final int[] head2 = new int[0x10000];

    boolean use_best_off;
    boolean b_char;

    static final int b_size = 0xC7FF;

    int max_chain;
    int m_len;
    int look;
    int m_off;
    int m_pos;
    int ip;
    int bp;
    int rp;
    int node_count;
}

public class LzoCompressor1x_999 extends AbstractLzo1Compressor {

    private int compression_level;

    public LzoCompressor1x_999(int level) {
        super(LzoAlgorithm.LZO1X, LzoConstraint.COMPRESSION);
        if (level > 0 && level < 10)
            compression_level = level - 1;
        else
            throw new IllegalArgumentException("compression level must be between 1 and 9");
    }

    private static int code_match(Lzo1x999T c, byte[] out, int op, int m_len, int m_off) {

        if (m_len == 2) {
            m_off -= 1;
            out[op++] = (byte) ((m_off & 3) << 2);
            out[op++] = (byte) (m_off >> 2);
        } else if (m_len <= 8 && m_off <= 0x0800) {
            m_off -= 1;
            out[op++] = (byte) (((m_len - 1) << 5) | ((m_off & 7) << 2));
            out[op++] = (byte) (m_off >> 3);
        } else if (m_len == 3 && m_off <= 0x0C00 && c.r1_lit >= 4) {
            m_off -= 0x0801;
            out[op++] = (byte) ((m_off & 3) << 2);
            out[op++] = (byte) (m_off >> 2);
        } else if (m_off <= 0x4000) {
            m_off -= 1;
            if (m_len <= 33)
                out[op++] = (byte) (32 | (m_len - 2));
            else {
                m_len -= 33;
                out[op++] = 32;
                while (m_len > 255) {
                    m_len -= 255;
                    out[op++] = 0;
                }

                out[op++] = (byte) m_len;
            }
            out[op++] = (byte) (m_off << 2);
            out[op++] = (byte) (m_off >> 6);
        } else {
            m_off -= 0x4000;
            int k = (m_off & 0x4000) >> 11;
            if (m_len <= 9)
                out[op++] = (byte) (16 | k | (m_len - 2));
            else {
                m_len -= 9;
                out[op++] = (byte) (16 | k);
                while (m_len > 255) {
                    m_len -= 255;
                    out[op++] = 0;
                }
                out[op++] = (byte) m_len;
            }
            out[op++] = (byte) (m_off << 2);
            out[op++] = (byte) (m_off >> 6);
        }

        return op;
    }

    private static int store_run(Lzo1x999T c, byte[] in, byte[] out, int op, lzo_uintp ii, int t) {

        if (op == c.out_base && t <= 238)
            out[op++] = (byte) (17 + t);
        else if (t <= 3)
            out[op - 2] |= (byte) t;
        else if (t <= 18)
            out[op++] = (byte) (t - 3);
        else {
            int tt = t - 18;
            out[op++] = 0;
            while (tt > 255) {
                tt -= 255;
                out[op++] = 0;
            }
            out[op++] = (byte) tt;
        }
        do out[op++] = in[ii.value++]; while (--t > 0);

        return op;
    }

    private static int code_run(Lzo1x999T c, byte[] in, byte[] out, int op, lzo_uintp ii, int lit) {
        if (lit > 0) {
            op = store_run(c, in, out, op, ii, lit);
            c.r1_lit = lit;
        } else
            c.r1_lit = 0;
        return op;
    }

    private static int min_gain(int ahead, int lit1, int lit2, int l1, int l2, int l3) {
        int lazy_match_min_gain = ahead;

        if (lit1 <= 3)
            lazy_match_min_gain += (lit2 <= 3) ? 0 : 2;
        else if (lit1 <= 18)
            lazy_match_min_gain += (lit2 <= 18) ? 0 : 1;

        lazy_match_min_gain += (l2 - l1) * 2;

        if (l3 != 0)
            lazy_match_min_gain -= (ahead - l3) * 2;

        if (lazy_match_min_gain < 0)
            lazy_match_min_gain = 0;

        return lazy_match_min_gain;
    }

    private static int len_of_coded_match(int m_len, int m_off, int lit) {
        int n = 4;

        if (m_len < 2)
            return 0;
        if (m_len == 2)
            return (m_off <= 0x0400 && lit > 0 && lit < 4) ? 2 : 0;
        if (m_len <= 8 && m_off <= 0x0800)
            return 2;
        if (m_len == 3 && m_off <= (0x0400 + 0x0800) && lit >= 4)
            return 2;
        if (m_off <= 0x4000) {
            if (m_len <= 33)
                return 3;
            m_len -= 33;
            while (m_len > 255) {
                m_len -= 255;
                n++;
            }
            return n;
        }
        if (m_off <= 0xBFFF) {
            if (m_len <= 9)
                return 3;
            m_len -= 9;
            while (m_len > 255) {
                m_len -= 255;
                n++;
            }
            return n;
        }
        return 0;
    }

    private static void better_match(Lzo1x999SWD swd, lzo_uintp m_len, lzo_uintp m_off) {

        if (m_len.value <= 3)
            return;
        if (m_off.value <= 0x0800)
            return;

        if (m_off.value > 0x0800 &&
                m_len.value >= 4 && m_len.value <= 9 &&
                swd.best_off[m_len.value - 1] != 0 && swd.best_off[m_len.value - 1] <= 0x0800) {
            m_len.value = m_len.value - 1;
            m_off.value = swd.best_off[m_len.value];
            return;
        }

        if (m_off.value > 0x4000 &&
                m_len.value == 10 &&
                swd.best_off[m_len.value - 2] != 0 && swd.best_off[m_len.value - 2] <= 0x0800) {
            m_len.value = m_len.value - 2;
            m_off.value = swd.best_off[m_len.value];
            return;
        }

        if (m_off.value > 0x4000 &&
                m_len.value >= 10 && m_len.value <= 34 &&
                swd.best_off[m_len.value - 1] != 0 && swd.best_off[m_len.value - 1] <= 0x4000) {
            m_len.value = m_len.value - 1;
            m_off.value = swd.best_off[m_len.value];
        }
    }

    private static void swd_search(Lzo1x999SWD s, int node, int cnt) {
        int p1;
        int p2;
        int m_len = s.m_len;
        int bp = s.bp;
        int bx = s.bp + s.look;
        byte scan_end1 = s.b[s.bp + m_len - 1];

        for (; cnt-- > 0; node = s.succ3[node]) {
            p1 = bp;
            p2 = node;

            if (s.b[p2 + m_len - 1] == scan_end1 &&
                    s.b[p2 + m_len] == s.b[p1 + m_len] &&
                    s.b[p2] == s.b[p1] &&
                    s.b[p2 + 1] == s.b[p1 + 1]) {

                p1 += 2;
                p2 += 2;
                do {
                } while (++p1 < bx && s.b[p1] == s.b[++p2]);

                int i = p1 - bp;

                if (i < 34) {
                    if (s.best_pos[i] == 0)
                        s.best_pos[i] = node + 1;
                }

                if (i > m_len) {
                    s.m_len = m_len = i;
                    s.m_pos = node;

                    if (m_len == s.look || m_len >= 0x800 || m_len > s.best3[node])
                        return;
                    scan_end1 = s.b[bp + m_len - 1];
                }
            }
        }
    }

    private static boolean swd_search2(Lzo1x999SWD s) {

        int key = s.head2[(s.b[s.bp] & 0xFF) + ((s.b[s.bp + 1] & 0xFF) << 8)];
        if (key == 0xFFFF)
            return false;

        if (s.best_pos[2] == 0)
            s.best_pos[2] = key + 1;

        if (s.m_len < 2) {
            s.m_len = 2;
            s.m_pos = key;
        }
        return true;
    }

    private static void swd_findbest(Lzo1x999SWD s) {

        int key = ((0x9F5F * ((((s.b[s.bp] << 5) ^ s.b[s.bp + 1]) << 5) ^ s.b[s.bp + 2])) >> 5) & 0x3FFF;

        int node = s.succ3[s.bp] = s.head3[key];
        int cnt = s.llen3[key]++;

        if (cnt > s.max_chain && s.max_chain > 0)
            cnt = s.max_chain;
        s.head3[key] = s.bp;

        s.b_char = true;
        int len = s.m_len;
        if (s.m_len >= s.look) {
            if (s.look == 0)
                s.b_char = false;
            s.best3[s.bp] = 0x801;
        } else {
            if (swd_search2(s) && s.look >= 3)
                swd_search(s, node, cnt);

            if (s.m_len > len)
                s.m_off = (s.bp > s.m_pos ? s.bp - s.m_pos : Lzo1x999SWD.b_size - (s.m_pos - s.bp));
            s.best3[s.bp] = s.m_len;

            if (s.use_best_off) {
                for (int i = 2; i < 34; i++)
                    if (s.best_pos[i] > 0)
                        s.best_off[i] = (s.bp > (s.best_pos[i] - 1) ? s.bp - (s.best_pos[i] - 1) : Lzo1x999SWD.b_size - ((s.best_pos[i] - 1) - s.bp));
                    else
                        s.best_off[i] = 0;
            }
        }
        swd_remove_node(s, s.rp);

        s.head2[(s.b[s.bp] & 0xFF) + ((s.b[s.bp + 1] & 0xFF) << 8)] = s.bp;
    }

    private static void swd_getbyte(Lzo1x999T c, Lzo1x999SWD s, byte[] in) {
        int c1 = c.ip < c.in_end ? (in[c.ip++] & 0xFF) : -1;

        if (c1 < 0) {
            if (s.look > 0)
                --s.look;
        } else
            s.b[s.ip] = (byte) c1;

        if (++s.ip == Lzo1x999SWD.b_size)
            s.ip = 0;
        if (++s.bp == Lzo1x999SWD.b_size)
            s.bp = 0;
        if (++s.rp == Lzo1x999SWD.b_size)
            s.rp = 0;
    }

    private static void swd_remove_node(Lzo1x999SWD s, int node) {
        if (s.node_count == 0) {
            int key = ((0x9F5F * ((((s.b[node] << 5) ^ s.b[node + 1]) << 5) ^ s.b[node + 2])) >> 5) & 0x3FFF;
            --s.llen3[key];

            key = (s.b[node] & 0xFF) + ((s.b[node + 1] & 0xFF) << 8);
            if (s.head2[key] == node)
                s.head2[key] = 0xFFFF;
        } else
            s.node_count--;
    }

    private static void swd_accept(Lzo1x999T c, Lzo1x999SWD s, byte[] in, int n) {
        int key;
        if (n > 0)
            do {
                swd_remove_node(s, s.rp);

                key = ((0x9F5F * (((((s.b[s.bp] << 5) ^ s.b[s.bp + 1]) << 5) ^ s.b[s.bp + 2]))) >> 5) & 0x3FFF;

                s.succ3[s.bp] = s.head3[key];
                s.head3[key] = s.bp;
                s.best3[s.bp] = 0x801;
                s.llen3[key]++;
                s.head2[(s.b[s.bp] & 0xFF) + ((s.b[s.bp + 1] & 0xFF) << 8)] = s.bp;

                swd_getbyte(c, s, in);
            } while (--n != 0);
    }

    private static void find_match(Lzo1x999T c, Lzo1x999SWD s, byte[] in, int this_len, int skip) {

        if (skip > 0)
            swd_accept(c, s, in, this_len - skip);

        s.m_len = 1;
        s.m_off = 0;

        if (s.use_best_off)
            Arrays.fill(s.best_pos, 0);

        swd_findbest(s);

        c.m_len = s.m_len;
        c.m_off = s.m_off;

        swd_getbyte(c, s, in);

        if (!s.b_char) {
            c.look = 0;
            c.m_len = 0;
        } else
            c.look = s.look + 1;

        c.bp = c.ip - c.look;
    }

    private static void swd_init(Lzo1x999T c, Lzo1x999SWD s, byte[] in) {

        s.m_len = 0;
        s.m_off = 0;

        s.max_chain = 0x800;

        s.use_best_off = false;

        s.node_count = 0xBFFF;

        Arrays.fill(s.head2, 0xFFFF);

        s.rp = s.bp = s.ip = 0;

        s.look = c.in_end - c.ip;
        if (s.look > 0) {
            if (s.look > 0x800)
                s.look = 0x800;
            System.arraycopy(in, c.ip, s.b, s.ip, s.look);
            c.ip += s.look;
            s.ip += s.look;
        }
        if (s.ip == Lzo1x999SWD.b_size)
            s.ip = 0;

        if (s.rp >= s.node_count)
            s.rp -= s.node_count;
        else
            s.rp += Lzo1x999SWD.b_size - s.node_count;

        if (s.look < 3) {
            s.b[s.bp + s.look] = 0;
            s.b[s.bp + s.look + 1] = 0;
            s.b[s.bp + s.look + 2] = 0;
        }
    }

    private static void compress_internal(byte[] in, int in_base, int in_len,
                                          byte[] out, int out_base, lzo_uintp out_len,
                                          int try_lazy_parm,
                                          int good_length,
                                          int max_lazy,
                                          int max_chain,
                                          int flags) {
        int try_lazy = try_lazy_parm;
        if (try_lazy_parm < 0)
            try_lazy = 1;

        if (good_length == 0)
            good_length = 32;

        if (max_lazy == 0)
            max_lazy = 32;

        if (max_chain == 0)
            max_chain = 2048;

        Lzo1x999T c = new Lzo1x999T();
        Lzo1x999SWD swd = new Lzo1x999SWD();

        c.ip = in_base;
        c.in_end = in_base + in_len;

        int op = c.out_base = out_base;

        lzo_uintp ii = new lzo_uintp(c.ip);

        int lit = c.r1_lit = 0;

        swd_init(c, swd, in);
        swd.use_best_off = (flags == 1);

        if (max_chain > 0)
            swd.max_chain = max_chain;

        find_match(c, swd, in, 0, 0);

        int m_len, m_off;

        while (c.look > 0) {

            int ahead;
            int max_ahead;
            int l1, l2, l3;

            m_len = c.m_len;
            m_off = c.m_off;

            if (lit == 0)
                ii.value = c.bp;

            if (m_len < 2 ||
                    (m_len == 2 && (m_off > 0x0400 || lit == 0 || lit >= 4)) ||
                    (m_len == 2 && op == out_base) ||
                    (op == out_base && lit == 0)) {
                m_len = 0;
            } else if (m_len == 3) {
                if (m_off > (0x0400 + 0x0800) && lit >= 4)
                    m_len = 0;
            }

            if (m_len == 0) {
                lit++;
                swd.max_chain = max_chain;
                find_match(c, swd, in, 1, 0);
                continue;
            }

            if (swd.use_best_off) {
                lzo_uintp m_len_p = new lzo_uintp(m_len);
                lzo_uintp m_off_p = new lzo_uintp(m_off);
                better_match(swd, m_len_p, m_off_p);
                m_len = m_len_p.value;
                m_off = m_off_p.value;
            }

            ahead = 0;
            if (try_lazy == 0 || m_len >= max_lazy) {
                l1 = 0;
                max_ahead = 0;
            } else {
                l1 = len_of_coded_match(m_len, m_off, lit);
                max_ahead = (try_lazy <= (l1 - 1) ? try_lazy : (l1 - 1));
            }

            boolean lazy_match_done = false;

            while (!lazy_match_done && ahead < max_ahead && c.look > m_len) {
                if (m_len >= good_length)
                    swd.max_chain = max_chain >> 2;
                else
                    swd.max_chain = max_chain;
                find_match(c, swd, in, 1, 0);
                ahead++;

                if (c.m_len < m_len)
                    continue;

                if (c.m_len == m_len && c.m_off >= m_off)
                    continue;

                if (swd.use_best_off) {
                    lzo_uintp m_len_p = new lzo_uintp(c.m_len);
                    lzo_uintp m_off_p = new lzo_uintp(c.m_off);
                    better_match(swd, m_len_p, m_off_p);
                    c.m_len = m_len_p.value;
                    c.m_off = m_off_p.value;
                }

                l2 = len_of_coded_match(c.m_len, c.m_off, lit + ahead);
                if (l2 == 0)
                    continue;
                l3 = (op == out_base) ? 0 : len_of_coded_match(ahead, m_off, lit);

                int lazy_match_min_gain = min_gain(ahead, lit, lit + ahead, l1, l2, l3);
                if (c.m_len >= m_len + lazy_match_min_gain) {
                    if (l3 != 0) {
                        op = code_run(c, in, out, op, ii, lit);
                        lit = 0;
                        op = code_match(c, out, op, ahead, m_off);
                    } else
                        lit += ahead;
                    lazy_match_done = true;
                }
            }
            if (!lazy_match_done) {
                op = code_run(c, in, out, op, ii, lit);
                lit = 0;
                op = code_match(c, out, op, m_len, m_off);
                swd.max_chain = max_chain;
                find_match(c, swd, in, m_len, 1 + ahead);
            }
        }

        if (lit > 0)
            op = store_run(c, in, out, op, ii, lit);

        out[op++] = 17;
        out[op++] = 0;
        out[op++] = 0;

        out_len.value = op - out_base;
    }

    public int getCompressionLevel() {
        return compression_level + 1;
    }

    @Override 
    public String toString() {
        return "LZO1X999-" + getCompressionLevel();
    }

    public int compress(byte[] in, int in_base, int in_len,
                        byte[] out, int out_base, lzo_uintp out_len) {
        int[] try_lazy_parm = {0, 0, 0, 1, 1, 1, 2, 2, 2};
        int[] good_length = {0, 0, 0, 4, 8, 8, 8, 32, 2048};
        int[] max_lazy = {0, 0, 0, 4, 16, 16, 32, 128, 2048};
        int[] max_chain = {4, 8, 16, 16, 32, 128, 256, 2048, 4096};
        int[] flags = {0, 0, 0, 0, 0, 0, 0, 1, 1};

        compress_internal(in, in_base, in_len,
                out, out_base, out_len,
                try_lazy_parm[compression_level],
                good_length[compression_level],
                max_lazy[compression_level],
                max_chain[compression_level],
                flags[compression_level]
        );
        return 0;
    }
}