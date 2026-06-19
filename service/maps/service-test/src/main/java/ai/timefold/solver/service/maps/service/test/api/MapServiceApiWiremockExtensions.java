package ai.timefold.solver.service.maps.service.test.api;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Map;

import ai.timefold.solver.service.maps.service.test.impl.DistanceGetUpdateResponseTransformer;
import ai.timefold.solver.service.maps.service.test.impl.GetLocationSetCompletedStatusTransformer;
import ai.timefold.solver.service.maps.service.test.impl.GetLocationSetNotFoundStatusTransformer;
import ai.timefold.solver.service.maps.service.test.impl.GetLocationSetProcessingStatusTransformer;
import ai.timefold.solver.service.maps.service.test.impl.HaversineDistanceResponseTransformer;
import ai.timefold.solver.service.maps.service.test.impl.HaversineWaypointsResponseTransformer;
import ai.timefold.solver.service.maps.service.test.impl.SaveLocationSetResponseTransformer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MapServiceApiWiremockExtensions implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options().bindAddress(getAddress()).dynamicPort().extensions(
                        HaversineDistanceResponseTransformer.class,
                        HaversineWaypointsResponseTransformer.class,
                        SaveLocationSetResponseTransformer.class,
                        GetLocationSetProcessingStatusTransformer.class,
                        GetLocationSetCompletedStatusTransformer.class,
                        GetLocationSetNotFoundStatusTransformer.class,
                        DistanceGetUpdateResponseTransformer.class));

        wireMockServer.start();

        wireMockServer.stubFor(WireMock.post(WireMock.urlPathMatching("/v1/distances"))
                .willReturn(WireMock.aResponse()
                        .withTransformers(HaversineDistanceResponseTransformer.TRANSFORMER_NAME)));

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathMatching("/v1/distances"))
                .willReturn(WireMock.aResponse()
                        .withTransformers(DistanceGetUpdateResponseTransformer.TRANSFORMER_NAME)));

        wireMockServer.stubFor(WireMock.post(WireMock.urlPathMatching("/v1/waypoints"))
                .willReturn(WireMock.aResponse()
                        .withTransformers(HaversineWaypointsResponseTransformer.TRANSFORMER_NAME)));

        wireMockServer.stubFor(WireMock.post(WireMock.urlPathMatching("/v1/management/location-sets"))
                .willReturn(WireMock.aResponse()
                        .withTransformers(SaveLocationSetResponseTransformer.TRANSFORMER_NAME)));

        wireMockServer.stubFor(WireMock.patch(WireMock.urlPathMatching("/v1/management/location-sets"))
                .willReturn(WireMock.aResponse().withStatus(204)));

        wireMockServer
                .stubFor(WireMock.get(WireMock.urlPathMatching("/v1/management/location-sets/[^/]+/[^/]+/[^/]+/status"))
                        .willReturn(WireMock.aResponse()
                                .withTransformers(GetLocationSetProcessingStatusTransformer.TRANSFORMER_NAME)));

        wireMockServer
                .stubFor(WireMock.get(WireMock.urlPathMatching("/v1/management/location-sets/[^/]+/[^/]+/notfound/status"))
                        .willReturn(WireMock.aResponse()
                                .withTransformers(GetLocationSetNotFoundStatusTransformer.TRANSFORMER_NAME)));

        wireMockServer
                .stubFor(WireMock.get(WireMock.urlPathMatching("/v1/management/location-sets/[^/]+/[^/]+/completed/status"))
                        .willReturn(WireMock.aResponse()
                                .withTransformers(GetLocationSetCompletedStatusTransformer.TRANSFORMER_NAME)));

        wireMockServer
                .stubFor(WireMock.delete(WireMock.urlPathMatching("/v1/management/location-sets/.*"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(204)));

        wireMockServer.stubFor(WireMock.get("/q/health").willReturn(WireMock.aResponse()));

        return Map.of("ai.timefold.platform.map-service.url",
                "http://" + getAddress() + ":" + wireMockServer.port());
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }

    private String getAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaceEnumeration.hasMoreElements()) {
                for (InterfaceAddress interfaceAddress : networkInterfaceEnumeration.nextElement().getInterfaceAddresses()) {
                    if (interfaceAddress.getAddress().isSiteLocalAddress()) {
                        return interfaceAddress.getAddress().getHostAddress();
                    }
                }
            }
            return "localhost";
        } catch (Exception e) {
            return "localhost";
        }
    }

}
