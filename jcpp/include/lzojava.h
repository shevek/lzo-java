#define LZO_CFG_FREESTANDING 1
#define LZO_PUBLIC(x) public static x
#define LZO_UNUSED(x)
#define LZO_DEFINE_UNINITIALIZED_VAR(t,n,v) t n = v

#define const
#define register
#define unsigned
#define char byte
#define lzo_bytep byte[]
#define lzo_uint int
#define lzo_uint32 long
#define lzo_xint long	// ?
#define lzo_voidp Object

#define GOTO_INIT() int state = init;	// Java-goto
#define GOTO_BEGIN(label) label: for (;;) { switch (state) { case init:    // Java-goto
#define GOTO_END() break; default: throw new IllegalStateException("Illegal state " + state); } break; }
#define GOTO_END_UNREACHABLE() default: throw new IllegalStateException("Illegal state " + state); } }

#define __lzo_unlikely(x) (x)

#define LZO_CPP_STRINGIZE(x)            #x
#define LZO_CPP_MACRO_EXPAND(x)         LZO_CPP_STRINGIZE(x)
#define LZO_CPP_CONCAT2(a,b)            a ## b
#define LZO_CPP_CONCAT3(a,b,c)          a ## b ## c
#define LZO_CPP_CONCAT4(a,b,c,d)        a ## b ## c ## d
#define LZO_CPP_CONCAT5(a,b,c,d,e)      a ## b ## c ## d ## e
#define LZO_CPP_ECONCAT2(a,b)           LZO_CPP_CONCAT2(a,b)
#define LZO_CPP_ECONCAT3(a,b,c)         LZO_CPP_CONCAT3(a,b,c)
#define LZO_CPP_ECONCAT4(a,b,c,d)       LZO_CPP_CONCAT4(a,b,c,d)
#define LZO_CPP_ECONCAT5(a,b,c,d,e)     LZO_CPP_CONCAT5(a,b,c,d,e)

// In Java, all of these are array indices.
#define PTR(x)              (x)
#define PTR_LT(a,b)         (PTR(a) < PTR(b))
#define PTR_GE(a,b)         (PTR(a) >= PTR(b))
#define PTR_DIFF(a, b)      (PTR(a) - PTR(b))

#define pd(a,b) PTR_DIFF(a,b)

#if 1 //Java && defined(UA_GET32)
#undef  LZO_DICT_USE_PTR
#define LZO_DICT_USE_PTR 0
#endif

// for lzo1y.h and lzo1z.h
#define lzo_sizeof_dict_t 4

// Unfortunately clobbered by config1x.h etc
// #define LZO_DETERMINISTIC (1)

#define U(x) ((x) & 0xff)
#define PRINT(x) // System.out.println(x)

// NOT a macro because liblzo2 assumes that if UA_GET32 is a macro,
// then it is faster than byte-array accesses, which it is not -
// or, if it is, hotspot will deal with it.
private static int UA_GET32(byte[] in, int in_ptr) {
	return (U(in[in_ptr]) << 24) | (U(in[in_ptr + 1]) << 16) | (U(in[in_ptr + 2]) << 8) | U(in[in_ptr + 3]);
}

