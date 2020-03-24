package com.github.glhez.jtools.text;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

/**
 * Mark options that affect the output, and how.
 *
 * @author gael.lhez
 */
@Documented
@Target({ FIELD })
public @interface TabulizeOutput {
  /**
   * Some explanation of the output on the whole.
   */
  String value() default "";
}
