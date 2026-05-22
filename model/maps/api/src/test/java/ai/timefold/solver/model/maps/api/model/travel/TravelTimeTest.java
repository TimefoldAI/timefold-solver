package ai.timefold.solver.model.maps.api.model.travel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.LongUnaryOperator;

import org.junit.jupiter.api.Test;

public class TravelTimeTest {

    @Test
    void adjust_identity_returnsSameInstance_reachable() {
        TravelTime original = TravelTime.of(123L);
        TravelTime adjusted = original.adjust(LongUnaryOperator.identity());

        assertThat(adjusted).isSameAs(original);
    }

    @Test
    void adjust_identity_returnsSameInstance_unreachable() {
        TravelTime original = TravelTime.UNREACHABLE;
        TravelTime adjusted = original.adjust(LongUnaryOperator.identity());

        assertThat(adjusted).isSameAs(original);
    }

    @Test
    void adjust_nullAdjuster_throwsException() {
        TravelTime original = TravelTime.of(123L);
        assertThatThrownBy(() -> original.adjust(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void adjust_nonIdentity_adjustsSeconds() {
        TravelTime original = TravelTime.of(10L);
        TravelTime adjusted = original.adjust(seconds -> seconds + 5L);

        assertThat(adjusted.seconds()).isEqualTo(15L);
    }

    @Test
    void adjust_nonIdentity_doesNotChangeUnreachableAndReturnsSameInstance() {
        TravelTime original = TravelTime.UNREACHABLE;
        TravelTime adjusted = original.adjust(seconds -> seconds + 5L);

        assertThat(adjusted).isSameAs(original);
    }
}
