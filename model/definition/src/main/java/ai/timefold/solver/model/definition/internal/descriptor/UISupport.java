package ai.timefold.solver.model.definition.internal.descriptor;

/**
 * Represents the type of UI support available.
 */
public enum UISupport {

    /**
     * No UI support is provided.
     */
    NONE,

    /**
     * UI support is provided via an app.js embedded in the model descriptor.
     */
    APP_JS
}
