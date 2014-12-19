/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author shevek
 */
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
@Retention(RetentionPolicy.CLASS)
public @interface SuppressWarnings {

    public String[] value();
}
