package ai.timefold.solver.model.maps.service.client.impl;

import ai.timefold.solver.model.maps.service.client.impl.error.MapServiceExceptionMapper;
import ai.timefold.solver.model.maps.service.integration.internal.MapManagementApi;
import ai.timefold.solver.model.maps.service.integration.internal.MapServiceApi;
import ai.timefold.solver.model.maps.service.integration.internal.MapServiceHealthCheckApi;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "map-service")
@RegisterProvider(MapServiceExceptionMapper.class)
public interface MapServiceClient extends MapServiceApi, MapServiceHealthCheckApi, MapManagementApi {

}
