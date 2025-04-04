package ai.timefold.solver.test.api.score.stream;

public interface ShadowVariableAwareSingleConstraintAssertion extends SingleConstraintAssertion {

    /**
     * The method allows the code under test that uses any type of shadow variables
     * to execute all related listeners and update the planning entities.
     * As a result, all shadow variables associated with the given solution will be updated.
     */
    SingleConstraintAssertion settingAllShadowVariables();
}
