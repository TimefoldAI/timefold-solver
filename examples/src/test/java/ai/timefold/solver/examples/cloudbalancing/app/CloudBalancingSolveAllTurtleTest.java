package ai.timefold.solver.examples.cloudbalancing.app;

import ai.timefold.solver.examples.cloudbalancing.domain.CloudBalance;
import ai.timefold.solver.examples.cloudbalancing.optional.score.CloudBalancingEasyScoreCalculator;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "ai.timefold.solver.examples.turtle", matches = "cloudbalancing")
class CloudBalancingSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<CloudBalance> {

    @Override
    protected CommonApp<CloudBalance> createCommonApp() {
        return new CloudBalancingApp();
    }

    @Override
    protected Class<CloudBalancingEasyScoreCalculator> overwritingEasyScoreCalculatorClass() {
        return CloudBalancingEasyScoreCalculator.class;
    }
}
