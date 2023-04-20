package ai.timefold.solver.examples.cloudbalancing.app;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudBalance;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudComputer;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudProcess;
import ai.timefold.solver.examples.cloudbalancing.optional.benchmark.CloudBalancingBenchmarkHelloWorld;
import ai.timefold.solver.examples.cloudbalancing.persistence.CloudBalancingGenerator;
import ai.timefold.solver.examples.cloudbalancing.score.CloudBalancingConstraintProvider;

/**
 * To benchmark this solver config, run {@link CloudBalancingBenchmarkHelloWorld} instead.
 */
public class CloudBalancingHelloWorld {

    public static void main(String[] args) {
        // Build the Solver
        SolverFactory<CloudBalance> solverFactory = SolverFactory.create(new SolverConfig()
                .withSolutionClass(CloudBalance.class)
                .withEntityClasses(CloudProcess.class)
                .withConstraintProviderClass(CloudBalancingConstraintProvider.class)
                .withTerminationSpentLimit(Duration.ofMinutes(2)));
        Solver<CloudBalance> solver = solverFactory.buildSolver();

        // Load a problem with 400 computers and 1200 processes
        CloudBalance unsolvedCloudBalance = new CloudBalancingGenerator().createCloudBalance(400, 1200);

        // Solve the problem
        CloudBalance solvedCloudBalance = solver.solve(unsolvedCloudBalance);

        // Display the result
        System.out.println("\nSolved cloudBalance with 400 computers and 1200 processes:\n"
                + toDisplayString(solvedCloudBalance));
    }

    public static String toDisplayString(CloudBalance cloudBalance) {
        StringBuilder displayString = new StringBuilder();
        for (CloudProcess process : cloudBalance.getProcessList()) {
            CloudComputer computer = process.getComputer();
            displayString.append("  ").append(process.getLabel()).append(" -> ")
                    .append(computer == null ? null : computer.getLabel()).append("\n");
        }
        return displayString.toString();
    }

}
