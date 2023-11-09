package ai.timefold.solver.quarkus.bean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;

import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class TimefoldSolverBannerBean {

    private static final Logger LOGGER = Logger.getLogger(TimefoldSolverBannerBean.class);

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Using Timefold Solver " + TimefoldSolverEnterpriseService.identifySolverVersion() + ".");
    }

}
