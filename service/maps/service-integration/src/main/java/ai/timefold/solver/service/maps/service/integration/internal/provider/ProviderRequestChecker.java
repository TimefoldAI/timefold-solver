package ai.timefold.solver.service.maps.service.integration.internal.provider;

import java.util.Map;

public interface ProviderRequestChecker extends ProviderIdentifier {

    boolean acceptsRequest(Map<String, String> options);

}
