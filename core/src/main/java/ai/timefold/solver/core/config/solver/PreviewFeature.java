package ai.timefold.solver.core.config.solver;

/**
 * Lists features available in Timefold Solver on a preview basis.
 * These preview features are developed to the same standard as the rest of Timefold Solver.
 * However, their APIs are not yet considered stable, pending user feedback.
 * Any class, method, or field related to these features may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * We encourage you to try these preview features and give us feedback on your experience with them.
 * Please direct your feedback to our Github Discussions.
 * 
 * <p>
 * This list is not constant and is evolving over time,
 * with items being added and removed without warning.
 * It should not be treated as part of our public API,
 * just like the preview features themselves.
 *
 * @see <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github Discussions</a>
 */
public enum PreviewFeature {

    DECLARATIVE_SHADOW_VARIABLES,
    DIVERSIFIED_LATE_ACCEPTANCE,
    PLANNING_SOLUTION_DIFF,
    /**
     * Unlike other preview features, Move Streams are an active research project.
     * It is intended to simplify the creation of custom moves, eventually replacing move selectors.
     * The component is under heavy development, entirely undocumented, and many key features are yet to be delivered.
     * Neither the API nor the feature set are complete, and any part can change or be removed at any time.
     * 
     * Move Streams will eventually stabilize and be promoted from a research project to a true preview feature.
     * We only expose it now to be able to use it for experimentation and testing.
     * As such, it is an exception to the rule;
     * this preview feature is not finished, and it is not yet ready for feedback.
     */
    MOVE_STREAMS

}
