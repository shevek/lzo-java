/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

/**
 *
 * @author shevek
 */
public interface LzoTransformer {

    public static final int LZO_E_OK = 0;
    public static final int LZO_E_ERROR = -1;
    public static final int LZO_E_OUT_OF_MEMORY = -2;
    public static final int LZO_E_NOT_COMPRESSIBLE = -3;
    public static final int LZO_E_INPUT_OVERRUN = -4;
    public static final int LZO_E_OUTPUT_OVERRUN = -5;
    public static final int LZO_E_LOOKBEHIND_OVERRUN = -6;
    public static final int LZO_E_EOF_NOT_FOUND = -7;
    public static final int LZO_E_INPUT_NOT_CONSUMED = -8;

    public String toErrorString(int code);
}
