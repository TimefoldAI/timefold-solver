package ai.timefold.solver.service.definition.api.usage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Points to an identifier of a trackable resource for the purpose of tracking the complexity of the dataset.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface ResourceId {

    /**
     * Identifies the resource type to scope the uniqueness of the resource IDs.
     */
    String type();
}
