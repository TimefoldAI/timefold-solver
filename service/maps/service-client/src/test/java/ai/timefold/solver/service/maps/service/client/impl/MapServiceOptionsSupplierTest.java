package ai.timefold.solver.service.maps.service.client.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import ai.timefold.solver.service.maps.api.model.TransportType;

import org.junit.jupiter.api.Test;

class MapServiceOptionsSupplierTest {

    @Test
    void noConfigurationDefaultsToCar() {
        assertThat(supplier(null, null).getOptions()).isEqualTo("transportType:car");
        assertThat(supplier(null, null).options().build()).isEqualTo("transportType:car");
    }

    @Test
    void defaultTransportTypeComesFromConfiguration() {
        assertThat(supplier("bicycle", null).options().build()).contains("bicycle");
        assertThat(supplier("bicycle", null).getOptions()).contains("bicycle");
    }

    @Test
    void autoSelectResolvesToTheDefaultTransportType() {
        // car is allowed, and therefore the default
        assertThat(supplier("auto-select", "bicycle,car").options().build()).contains("car");
        // car is not allowed, so the first allowed one is the default
        assertThat(supplier("auto-select", "bicycle,foot").options().build()).contains("bicycle");
    }

    @Test
    void overridesTakePrecedenceOverConfiguration() {
        assertThat(supplier("bicycle", null).options().transportType(TransportType.FOOT).build())
                .contains("foot")
                .doesNotContain("bicycle");
    }

    @Test
    void autoSelectOrNullTransportTypeOmitsTheOption() {
        assertThat(supplier("bicycle", null).options().transportType(TransportType.AUTO_SELECT).build())
                .doesNotContain("bicycle")
                .doesNotContain("auto-select");
        assertThat(supplier("bicycle", null).options().transportType(null).build()).doesNotContain("bicycle");
    }

    @Test
    void locationSetNameIsOnlyAddedWhenSet() {
        var supplier = supplier(null, null);
        assertThat(supplier.options().build()).doesNotContain("my-set");
        assertThat(supplier.options().locationSetName("my-set").build()).contains("my-set");
        assertThat(supplier.options().locationSetName("my-set").locationSetName(null).build()).doesNotContain("my-set");
    }

    private static MapServiceOptionsSupplier supplier(String transportType, String allowedTransportTypes) {
        return new MapServiceOptionsSupplier(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(transportType),
                Optional.ofNullable(allowedTransportTypes));
    }
}
