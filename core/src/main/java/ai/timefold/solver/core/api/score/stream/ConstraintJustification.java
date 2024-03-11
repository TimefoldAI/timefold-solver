package ai.timefold.solver.core.api.score.stream;

import java.util.UUID;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.MatchAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.api.solver.SolutionManager;

/**
 * Marker interface for constraint justifications.
 * All classes used as constraint justifications must implement this interface.
 *
 * <p>
 * Implementing classes ("implementations") may decide to implement {@link Comparable}
 * to preserve order of instances when displayed in user interfaces, logs etc.
 * This is entirely optional.
 *
 * <p>
 * If two instances of this class are {@link Object#equals(Object) equal},
 * they are considered to be the same justification.
 * This matters in case of {@link SolutionManager#analyze(Object)} score analysis
 * where such justifications are grouped together.
 * This situation is likely to occur in case a {@link ConstraintStream} produces duplicate tuples,
 * which can be avoided by using {@link UniConstraintStream#distinct()} or its bi, tri and quad counterparts.
 * Alternatively, some unique ID (such as {@link UUID#randomUUID()}) can be used to distinguish between instances.
 *
 * <p>
 * Score analysis does not {@link ScoreAnalysis#diff(ScoreAnalysis) diff} contents of the implementations;
 * instead it uses equality of the implementations (as defined above) to tell them apart from the outside.
 * For this reason, it is recommended that:
 * <ul>
 * <li>The implementations must not use {@link Score} for {@link Object#equals(Object) equal} and hash codes,
 * as that would prevent diffing from working entirely.</li>
 * <li>The implementations should not store any {@link Score} instances,
 * as they would not be diffed, leading to confusion with {@link MatchAnalysis#score()}, which does get diffed.</li>
 * </ul>
 *
 * <p>
 * If the user wishes to use score analysis, they are required to ensure
 * that the class(es) implementing this interface can be serialized into any format
 * which is supported by the {@link SolutionManager} implementation, typically JSON.
 *
 * @see ConstraintMatch#getJustification()
 */
public interface ConstraintJustification {

}
