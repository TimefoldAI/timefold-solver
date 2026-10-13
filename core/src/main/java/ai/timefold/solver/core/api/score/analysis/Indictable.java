package ai.timefold.solver.core.api.score.analysis;

import org.jspecify.annotations.NullMarked;

/**
 * An optional interface a class can implement to customize how it appears
 * when indicted in a {@link ScoreAnalysis}.
 * <p>
 * When not implemented, the class' canonical name is used as the key in {@link ScoreAnalysis#indictmentMap()}}
 * and {@link IndictmentAnalysis#indictee()} is the same instance.
 * <p>
 * The primary use case for implementing {@link Indictable} is if you have a separate
 * solver and rest model, and don't want to expose your solver types in your REST API.
 * <p>
 * Note: {@link ScoreAnalysis} in general and indictments in particular
 * are exclusive to Timefold Solver Enterprise Edition.
 */
@NullMarked
public interface Indictable {

    /**
     * Returns the indictment class id used by {@link ScoreAnalysis#indictmentMap()}
     * when a class does not implement {@link Indictable}.
     *
     * @param clazz The type of class
     * @return the key into {@link ScoreAnalysis#indictmentMap()} to get indicted objects of that type
     */
    static String getIndictmentClassId(Class<?> clazz) {
        if (Indictable.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("""
                    The class (%s) implements %s so its indictment map key cannot be determined statically.
                    Maybe check (%s) implementation of getIndictmentClassId()?""".formatted(clazz.getCanonicalName(),
                    Indictable.class.getSimpleName(), clazz.getCanonicalName()));
        }
        return clazz.getCanonicalName();
    }

    /**
     * An identifier that uniquely identifies the type returned by
     * {@link #getIndictedObject()}.
     * 
     * @return a string that can be used to identify the class of {@link #getIndictedObject()}
     */
    String getIndictmentClassId();

    /**
     * Optional method. It is used to calculate the {@link IndictmentAnalysis#indictee()}
     * when this object is indicted. If not implemented, it will return this object.
     * 
     * @return the object that {@link IndictmentAnalysis#indictee()} should return
     */
    default Object getIndictedObject() {
        return this;
    }
}
