package fr.glhez.jtools.text;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

/**
 * Mark option of {@link TabulizeOptions} that affect the column finder.
 *
 * @author gael.lhez
 *
 */
@Documented
@Target({ FIELD })
public @interface TabulizeColumnFinder {
  /**
   * Some explanation of the finder impact on the whole.
   */
  String value() default "";
}
