package ai.timefold.solver.core.api.solver.event;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.solver.phase.PhaseType;
import ai.timefold.solver.core.impl.phase.event.DefaultPhaseEventProducerId;

import org.junit.jupiter.api.Test;

class EventProducerIdTest {

    @Test
    void localSearchIsPhaseEventProducerId() {
        var producerId = DefaultPhaseEventProducerId.of(PhaseType.LOCAL_SEARCH, 1);

        assertThat(producerId).isInstanceOf(PhaseEventProducerId.class);
        var phaseId = producerId.phaseId();
        assertThat(phaseId.type()).isEqualTo(PhaseType.LOCAL_SEARCH);
        assertThat(phaseId.phaseIndex()).isEqualTo(1);
        assertThat(phaseId).hasToString("Local Search (1)");
        assertThat(producerId.producerId()).isEqualTo("Local Search (1)");
        assertThat(producerId.simpleProducerName()).isEqualTo("Local Search");
        assertThat(producerId.phaseIndex()).hasValue(1);
        assertThat(producerId).isEqualTo(DefaultPhaseEventProducerId.of(PhaseType.LOCAL_SEARCH, 1));
    }

}
