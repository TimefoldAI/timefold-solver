package ai.timefold.solver.core.preview.api.variable.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ProvidedShadowVariableTest {
    @Test
    public void providedShadowVariable() {
        var sessionFactory = ShadowVariableSessionFactory.create(
                SolutionDescriptor.buildSolutionDescriptor(RoutePlan.class,
                        Vehicle.class, Visit.class),
                new TestShadowVariableProvider());

        var vehicle = spy(new Vehicle("v1"));
        var visit1 = spy(new Visit("c1"));
        var visit2 = spy(new Visit("c2"));
        var visit3 = spy(new Visit("c3"));

        var session = sessionFactory.forEntities(vehicle, visit1, visit2, visit3);
        session.setInverse(visit1, vehicle);
        session.setPrevious(visit2, visit1);
        session.setPrevious(visit3, visit2);

        Mockito.reset(vehicle, visit1, visit2, visit3);
        session.updateVariables();

        assertThat(visit1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visit1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visit1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));

        assertThat(visit2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visit2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visit2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));

        assertThat(visit3.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visit3.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visit3.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(150L));

        session.setPrevious(visit2, visit3);
        session.setPrevious(visit3, visit1);

        Mockito.reset(vehicle, visit1, visit2, visit3);
        session.updateVariables();

        verifyNoInteractions(visit1);

        assertThat(visit1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visit1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visit1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));

        assertThat(visit3.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visit3.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visit3.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));

        assertThat(visit2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visit2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visit2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(150L));
    }
}
