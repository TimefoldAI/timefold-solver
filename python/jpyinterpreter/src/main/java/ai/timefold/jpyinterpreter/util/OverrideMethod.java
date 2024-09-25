package ai.timefold.jpyinterpreter.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a generated method as an override implementation.
 * Needed since {@link Override} is not retained at runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OverrideMethod {
}
