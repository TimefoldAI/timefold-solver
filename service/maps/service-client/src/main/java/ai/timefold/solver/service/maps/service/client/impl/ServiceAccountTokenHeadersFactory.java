package ai.timefold.solver.service.maps.service.client.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;

import ai.timefold.solver.service.definition.internal.platform.ServiceAccountToken;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Sends the service account token of the pod along with every call to the map service, so that a deployment which
 * puts the access service in front of the map service can tell which solver worker is calling.
 * <p>
 * A model that does not run in a cluster has no token mounted and calls the map service without one, the same way it
 * did before.
 */
@ApplicationScoped
public class ServiceAccountTokenHeadersFactory implements ClientHeadersFactory {

    private final ServiceAccountToken token;

    @Inject
    public ServiceAccountTokenHeadersFactory(
            @ConfigProperty(name = ServiceAccountToken.TOKEN_FILE_PROPERTY,
                    defaultValue = ServiceAccountToken.DEFAULT_TOKEN_FILE) String tokenFile) {
        this.token = new ServiceAccountToken(tokenFile);
    }

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> outgoingHeaders) {
        token.authorization()
                .ifPresent(authorization -> outgoingHeaders.putSingle(ServiceAccountToken.AUTHORIZATION_HEADER,
                        authorization));
        return outgoingHeaders;
    }
}
