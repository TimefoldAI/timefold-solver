package ai.timefold.solver.core.api.score.analysis;

import org.jspecify.annotations.NullMarked;

/**
 * An optional interface a class can implement to customize how it appears
 * when indicted in a {@link ScoreAnalysis}.
 * <p>
 * When not implemented, the class' canonical name is used for {@link IndictmentAnalysis#type()}
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
     * An identifier that uniquely identifies the type returned by
     * {@link #getIndictedObject()}.
     * @return a string that can be used to identify the class of {@link #getIndictedObject()}
     */
    String getIndictedTypeId();

    /**
     * Optional method. It is used to calculate the {@link IndictmentAnalysis#indictee()}
     * when this object is indicted. If not implemented, it will return this object.
     * @return the object that {@link IndictmentAnalysis#indictee()} should return
     */
    default Object getIndictedObject() {
        return this;
    }
}
