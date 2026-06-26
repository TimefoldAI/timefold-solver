package ai.timefold.solver.spring.boot.autoconfigure;

import java.io.StringReader;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.spring.boot.autoconfigure.config.SolverManagerProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;

/**
 * Factory for creating SolverConfig and SolverManager from XML strings.
 * Used by TimefoldSolverAutoConfiguration to lazily create beans.
 */
public class SolverConfigFactory {

    private final TimefoldProperties timefoldProperties;

    public SolverConfigFactory(TimefoldProperties timefoldProperties) {
        this.timefoldProperties = timefoldProperties;
    }

    @SuppressWarnings("unused") // Referenced by TimefoldSolverAutoConfiguration as a String.
    public <Solution_> SolverManager<Solution_> solverManagerSupplier(String solverConfigXml) {
        SolverFactory<Solution_> solverFactory = SolverFactory.create(solverConfigSupplier(solverConfigXml));
        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
        SolverManagerProperties solverManagerProperties = timefoldProperties.getSolverManager();
        if (solverManagerProperties != null && solverManagerProperties.getParallelSolverCount() != null) {
            solverManagerConfig.setParallelSolverCount(solverManagerProperties.getParallelSolverCount());
        }
        return SolverManager.create(solverFactory, solverManagerConfig);
    }

    @SuppressWarnings("unused") // Referenced by TimefoldSolverAutoConfiguration as a String.
    public SolverConfig solverConfigSupplier(String solverConfigXml) {
        SolverConfigIO solverConfigIO = new SolverConfigIO();
        return solverConfigIO.read(new StringReader(solverConfigXml));
    }
}
