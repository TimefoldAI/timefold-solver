package ai.timefold.solver.spring.boot.autoconfigure;

import java.io.StringReader;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.spring.boot.autoconfigure.config.SolverManagerProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class TimefoldSolverAotFactory implements EnvironmentAware {
    private TimefoldProperties timefoldProperties;

    @Override
    public void setEnvironment(Environment environment) {
        // We need the environment to set run time properties of SolverFactory and SolverManager
        BindResult<TimefoldProperties> result = Binder.get(environment).bind("timefold", TimefoldProperties.class);
        this.timefoldProperties = result.orElseGet(TimefoldProperties::new);
    }

    public <Solution_, ProblemId_> SolverManager<Solution_> solverManagerSupplier(String solverConfigXml) {
        SolverFactory<Solution_> solverFactory = SolverFactory.create(solverConfigSupplier(solverConfigXml));
        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
        SolverManagerProperties solverManagerProperties = timefoldProperties.getSolverManager();
        if (solverManagerProperties != null && solverManagerProperties.getParallelSolverCount() != null) {
            solverManagerConfig.setParallelSolverCount(solverManagerProperties.getParallelSolverCount());
        }
        return SolverManager.create(solverFactory, solverManagerConfig);
    }

    public SolverConfig solverConfigSupplier(String solverConfigXml) {
        SolverConfigIO solverConfigIO = new SolverConfigIO();
        return solverConfigIO.read(new StringReader(solverConfigXml));
    }
}
