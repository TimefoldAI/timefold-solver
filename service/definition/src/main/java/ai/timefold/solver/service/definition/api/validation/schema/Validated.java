package ai.timefold.solver.service.definition.api.validation.schema;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Validated {

    String operationId();

    boolean nullable() default false;
}
