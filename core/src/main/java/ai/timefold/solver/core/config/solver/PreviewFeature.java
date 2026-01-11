package ai.timefold.solver.core.config.solver;

/**
 * Lists features available in Timefold Solver on a preview basis.
 * These preview features are developed to the same standard as the rest of Timefold Solver.
 * However, their APIs are not yet considered stable, pending user feedback.
 * Any class, method, or field related to these features may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * We encourage you to try these preview features and give us feedback on your experience with them.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
 * 
 * <p>
 * This list is not constant and is evolving over time,
 * with items being added and removed without warning.
 * It should not be treated as part of our public API,
 * just like the preview features themselves.
 */
public enum PreviewFeature {

    DIVERSIFIED_LATE_ACCEPTANCE,
    PLANNING_SOLUTION_DIFF,
    /**
     * Unlike other preview features, Neighborhoods are an active research project.
     * It is intended to simplify the creation of custom moves, eventually replacing move selectors.
     * The component is under development, and many key features are yet to be delivered.
     */
    NEIGHBORHOODS

}
