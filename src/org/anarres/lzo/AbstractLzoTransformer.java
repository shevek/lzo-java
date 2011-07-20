/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

/**
 *
 * @author shevek
 */
public class AbstractLzoTransformer implements LzoTransformer {

    @Override
    public String toErrorString(int code) {
        switch (code) {
            case LZO_E_OK:
                return "OK";
            case LZO_E_ERROR:
                return "Error";
            case LZO_E_OUT_OF_MEMORY:
                return "Out of memory";
            case LZO_E_NOT_COMPRESSIBLE:
                return "Not compressible";
            case LZO_E_INPUT_OVERRUN:
                return "Input overrun";
            case LZO_E_OUTPUT_OVERRUN:
                return "Output overrun";
            case LZO_E_LOOKBEHIND_OVERRUN:
                return "Lookbehind overrun";
            case LZO_E_EOF_NOT_FOUND:
                return "EOF not found";
            case LZO_E_INPUT_NOT_CONSUMED:
                return "Input not consumed";
            default:
                return "Unknown-" + code;
        }
    }
}
