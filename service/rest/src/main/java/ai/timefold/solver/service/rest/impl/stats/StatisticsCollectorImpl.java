package ai.timefold.solver.service.rest.impl.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

import ai.timefold.solver.service.definition.internal.stats.StatisticsCollector;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StatisticsCollectorImpl implements StatisticsCollector {

    private static final String TF_REQUEST_ID_PROP = "tf_request_id";

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsCollectorImpl.class);
    private AtomicLong lastRequest = new AtomicLong(0);
    private List<UUID> inflightRequests = Collections.synchronizedList(new ArrayList<>());

    @ServerRequestFilter(preMatching = true)
    public void preMatchingFilter(ContainerRequestContext requestContext) {

        UUID requestId = UUID.randomUUID();
        LOGGER.debug("Request start {}", requestId);
        requestContext.setProperty(TF_REQUEST_ID_PROP, requestId);
        inflightRequests.add(requestId);
        // record request as negative number to indicate it is a in-flight request
        this.lastRequest.set(System.currentTimeMillis() * -1);
    }

    @ServerResponseFilter
    public void postMatchingFilter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

        UUID requestId = (UUID) requestContext.getProperty(TF_REQUEST_ID_PROP);
        LOGGER.debug("Request end {}", requestId);
        inflightRequests.remove(requestId);

        if (inflightRequests.isEmpty()) {
            LOGGER.debug("Recording as last request completed {}", requestId);
            // record current timestamp as last request completion time
            this.lastRequest.set(System.currentTimeMillis());
        }
    }

    @Override
    public long lastRequestTimestamp() {
        return lastRequest.get();
    }

}
