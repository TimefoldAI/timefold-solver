package ai.timefold.solver.core.api.score.stream.test;

public interface ShadowVariableAwareMultiConstraintAssertion extends MultiConstraintAssertion {

    /**
     * The method allows the code under test that uses any type of shadow variables
     * to execute all related listeners and update the planning entities.
     * As a result, all shadow variables associated with the given solution will be updated.
     */
    MultiConstraintAssertion settingAllShadowVariables();
}
