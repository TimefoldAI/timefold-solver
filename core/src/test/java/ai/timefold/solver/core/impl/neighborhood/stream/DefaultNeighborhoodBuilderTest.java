package ai.timefold.solver.core.impl.neighborhood.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Map;

import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class DefaultNeighborhoodBuilderTest {

    @Test
    void addUsesProviderIdInRegistrationOrder() {
        var first = new NamedMoveProvider();
        var second = new OtherMoveProvider();
        var neighborhood = build(builder().add(second).add(first));

        assertThat(neighborhood.moveProviderMap()).containsExactly(
                Map.entry("OtherMoveProvider", second), Map.entry("NamedMoveProvider", first));
    }

    @Test
    void customDefaultIdIsSimpleClassName() {
        assertThat(new NamedMoveProvider().getId()).isEqualTo("NamedMoveProvider");
    }

    @Test
    void lambdaDefaultIdIsCustom() {
        MoveProvider<TestdataSolution> lambda = moveStreamFactory -> {
            throw new UnsupportedOperationException();
        };
        assertThat(lambda.getId()).isEqualTo("custom");
    }

    @Test
    void sameProviderSameIdKeptOnce() {
        var provider = new NamedMoveProvider();
        var neighborhood = build(builder().add(provider).add("NamedMoveProvider", provider));

        assertThat(neighborhood.moveProviderMap()).containsExactly(Map.entry("NamedMoveProvider", provider));
    }

    @Test
    void sameProviderOtherIdFails() {
        var provider = new NamedMoveProvider();
        var builder = builder().add(provider).add("other", provider);

        assertThatIllegalStateException()
                .isThrownBy(builder::build)
                .withMessageContaining("added twice")
                .withMessageContaining("(NamedMoveProvider) and (other)");
    }

    @Test
    void duplicateIdFails() {
        var builder = builder().add(new NamedMoveProvider()).add(new NamedMoveProvider());

        assertThatIllegalStateException()
                .isThrownBy(builder::build)
                .withMessageContaining("id (NamedMoveProvider) is used by two move providers")
                .withMessageContaining("add(id, moveProvider)");
    }

    @Test
    void ownIdsResolveDuplicates() {
        var first = new NamedMoveProvider();
        var second = new NamedMoveProvider();
        var neighborhood = build(builder().add("first", first).add("second", second));

        assertThat(neighborhood.moveProviderMap()).containsExactly(Map.entry("first", first),
                Map.entry("second", second));
    }

    @Test
    void blankIdFails() {
        var builder = builder();
        var provider = new NamedMoveProvider();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> builder.add(" ", provider))
                .withMessageContaining("must not be blank");
    }

    @SuppressWarnings("unchecked")
    private static DefaultNeighborhood<TestdataSolution> build(NeighborhoodBuilder<TestdataSolution> builder) {
        return (DefaultNeighborhood<TestdataSolution>) builder.build();
    }

    private static DefaultNeighborhoodBuilder<TestdataSolution> builder() {
        return new DefaultNeighborhoodBuilder<>(TestdataSolution.buildMetaModel());
    }

    private static class NamedMoveProvider implements MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            throw new UnsupportedOperationException();
        }

    }

    private static final class OtherMoveProvider extends NamedMoveProvider {
    }

}
