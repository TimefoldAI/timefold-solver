package ai.timefold.solver.spring.boot.autoconfigure;

import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class TimefoldSolverBannerBean implements InitializingBean {

    private static final Log LOG = LogFactory.getLog(TimefoldSolverBannerBean.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.info("Using Timefold Solver " + TimefoldSolverEnterpriseService.identifySolverVersion() + ".");
    }
}
