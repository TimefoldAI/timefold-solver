package ai.timefold.solver.model.maps.haversine.impl;

import ai.timefold.solver.model.maps.service.integration.internal.provider.ProviderIdentifier;

public abstract class HaversineProviderIdentification implements ProviderIdentifier {

    public static final String PROVIDER = "haversine";

    @Override
    public String getProvider() {
        return PROVIDER;
    }
}
