package ai.timefold.solver.core.impl.heuristic.selector.entity.mimic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.testdomain.TestdataEntity;

import org.junit.jupiter.api.Test;

class MimicReplayingEntitySelectorContractTest {

    @Test
    void listIteratorReplaysRecordedEntity() {
        var e1 = new TestdataEntity("e1");
        var child = SelectorTestUtils.mockEntitySelector(TestdataEntity.class, e1);
        var recorder = new MimicRecordingEntitySelector<>(child);
        var replaying = new MimicReplayingEntitySelector<>(recorder);

        var recordingIterator = recorder.iterator();
        assertThat(recordingIterator.hasNext()).isTrue();
        assertThat(recordingIterator.next()).isSameAs(e1);

        var iterator = replaying.listIterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isSameAs(e1);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void listIteratorWithIndexRejectsInvalidIndex() {
        var e1 = new TestdataEntity("e1");
        var child = SelectorTestUtils.mockEntitySelector(TestdataEntity.class, e1);
        var recorder = new MimicRecordingEntitySelector<>(child);
        var replaying = new MimicReplayingEntitySelector<>(recorder);

        var recordingIterator = recorder.iterator();
        assertThat(recordingIterator.hasNext()).isTrue();
        assertThat(recordingIterator.next()).isSameAs(e1);

        assertThat(replaying.listIterator(0).next()).isSameAs(e1);
        assertThatThrownBy(() -> replaying.listIterator(2))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void isNeverEndingIsFalseForFiniteReplay() {
        EntityMimicRecorder<Object> recorder = mock(EntityMimicRecorder.class);
        when(recorder.isNeverEnding()).thenReturn(true);
        var replaying = new MimicReplayingEntitySelector<>(recorder);

        assertThat(replaying.isNeverEnding()).isFalse();
    }
}
