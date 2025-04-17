package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

/**
 * Specifies the paths to variables that a method referenced by {@link ShadowVariable#supplierName()}
 * uses to compute the value of a {@link ShadowVariable}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ShadowSources {
    /**
     * The paths to variables the method uses to compute the value of a {@link ShadowVariable#supplierName() supplier variable}.
     * <p>
     * Each path is a {@link String} that is one of the following three forms:
     *
     * <ul>
     * <li>
     * "variableName", for referring any variable on the same planning entity.
     * </li>
     * <li>
     * A list of names seperated by ".", such as "variableOrFact.fact.entity.supplierVariable",
     * for referencing a supplier variable accessible from the planning entity.
     * The first property may be a fact or any non-supplier variable; the remaining properties before the end
     * must be facts, and the final property must be a supplier variable.
     * For the path "a.b", it refers to the supplier variable "b"
     * on the property/variable "a" of the planning entity.
     * In general, if you access a variable in your method using a chain like {@code a.b.c}, that
     * chain should be included as a source.
     * </li>
     * <li>
     * A list of names seperated by ".", followed by a name suffix by "[].",
     * followed by either of the forms above.
     * For example, "group[].previous".
     * In this case, "group" is a {@link java.util.Collection} on the planning entity,
     * and the annotated method uses the "previous" variable of each element in the
     * collection.
     * The collection must not change during solving and may be null.
     * </li>
     * </ul>
     *
     * For example, for this method
     * 
     * <pre>
     * {@code
     * &#64;InverseRelationShadowVariable
     * Entity entity;
     *
     * &#64;PreviousElementShadowVariable
     * Value previous;
     *
     * &#64;ShadowVariable(supplierName="startTimeSupplier")
     * LocalDateTime startTime;
     *
     * &#64;ShadowVariable(supplierName="endTimeSupplier")
     * LocalDateTime endTime;
     *
     * Collection<Value> dependencies;
     *
     * &#64;ShadowSources("previous.endTime", "entity", "dependencies[].endTime")
     * public LocalDateTime startTimeSupplier() {
     *     LocalDateTime readyTime = null;
     *     if (previous != null) {
     *         readyTime = previous.endTime;
     *     } else if (entity != null) {
     *         readyTime = entity.startTime;
     *     } else {
     *         return null;
     *     }
     *     if (dependencies != null) {
     *         for (var dependency : dependencies) {
     *             if (dependency.endTime == null) {
     *                 return null;
     *             }
     *             readyTime = (readyTime.isBefore(dependency.endTime)? dependency.endTime: readyTime;
     *         }
     *     }
     *     return readyTime;
     * }
     * }
     * </pre>
     * 
     * The value {@code { "previous.endTime", "entity", "dependencies[].endTime") }} is used
     * for {@link ShadowSources} since it accesses
     * the end time supplier variable of its previous element variable ("previous.endTime"),
     * a fact on its inverse relation variable ("entity"),
     * and the end time supplier variable on each element in its dependencies ("dependencies[].endTime").
     * <p>
     * 
     * @return A non-empty list of variables the supplier method accesses.
     */
    String[] value();
}
