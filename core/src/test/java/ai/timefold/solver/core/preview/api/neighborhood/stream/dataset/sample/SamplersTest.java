package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.neighborhood.stream.dataset.sample.SampleAssembler;
import ai.timefold.solver.core.impl.solver.random.RandomSource;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample.Decision;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class SamplersTest {

    private static List<Integer> sourceOf(int size) {
        return IntStream.range(0, size).boxed().toList();
    }

    private static RandomGenerator seededRandom() {
        return RandomSource.seeded(0L).moveIteratorUsage();
    }

    @Test
    void exactlyYieldsExactSizeOnALargerSource() {
        var sample = SampleAssembler.assemble(sourceOf(10).iterator(), seededRandom(), Samplers.exactly(4));
        assertThat(sample).isNotNull();
        assertThat(sample.size()).isEqualTo(4);
    }

    @Test
    void exactlyReturnsNullWhenSourceIsSmallerThanMinimum() {
        var sample = SampleAssembler.assemble(sourceOf(3).iterator(), seededRandom(), Samplers.<Integer> exactly(10));
        assertThat(sample).isNull();
    }

    @Test
    void betweenReturnsNullWhenSourceIsSmallerThanMinimum() {
        var sample = SampleAssembler.assemble(sourceOf(2).iterator(), seededRandom(), Samplers.<Integer> between(3, 7));
        assertThat(sample).isNull();
    }

    @Test
    void upToAndAllHaveNoFloorAndStayProductiveOnAShortSource() {
        var upToSample = SampleAssembler.assemble(sourceOf(1).iterator(), seededRandom(), Samplers.<Integer> upTo(8));
        assertThat(upToSample).isNotNull();
        assertThat(upToSample.size()).isEqualTo(1);

        var allSample = SampleAssembler.assemble(sourceOf(1).iterator(), seededRandom(), Samplers.<Integer> all());
        assertThat(allSample).isNotNull();
        assertThat(allSample.size()).isEqualTo(1);
    }

    @Test
    void exactlyOneStopsAtTheFirstCandidate() {
        var sample = SampleAssembler.assemble(sourceOf(10).iterator(), seededRandom(), Samplers.<Integer> exactly(1));
        assertThat(sample).isNotNull();
        assertThat(sample.size()).isEqualTo(1);
    }

    @Test
    void exactlyOneConsumesOnlyOneElementFromTheSource() {
        var delegate = sourceOf(10).iterator();
        var drawCount = new int[1];
        var countingIterator = new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Integer next() {
                drawCount[0]++;
                return delegate.next();
            }
        };
        var sample = SampleAssembler.assemble(countingIterator, seededRandom(), Samplers.<Integer> exactly(1));
        assertThat(sample).isNotNull();
        assertThat(sample.size()).isEqualTo(1);
        assertThat(drawCount[0]).isEqualTo(1);
    }

    @Test
    void upToStaysWithinRangeAndVaries() {
        var random = seededRandom();
        var sizes = new HashSet<Integer>();
        var sampler = Samplers.<Integer> upTo(8);
        for (var i = 0; i < 50; i++) {
            var sample = SampleAssembler.assemble(sourceOf(20).iterator(), random, sampler);
            assertThat(sample).isNotNull();
            assertThat(sample.size()).isBetween(1, 8);
            sizes.add(sample.size());
        }
        assertThat(sizes).hasSizeGreaterThan(1);
    }

    @Test
    void betweenStaysWithinRangeAndVaries() {
        var random = seededRandom();
        var sizes = new HashSet<Integer>();
        var sampler = Samplers.<Integer> between(3, 7);
        for (var i = 0; i < 50; i++) {
            var sample = SampleAssembler.assemble(sourceOf(20).iterator(), random, sampler);
            assertThat(sample).isNotNull();
            assertThat(sample.size()).isBetween(3, 7);
            sizes.add(sample.size());
        }
        assertThat(sizes).hasSizeGreaterThan(1);
    }

    @Test
    void betweenMinimumSizeIsTheDeclaredFloorNotTheDrawnTarget() {
        var random = seededRandom();
        var sampler = Samplers.<Integer> between(2, 5);
        for (var i = 0; i < 50; i++) {
            var sample = SampleAssembler.assemble(sourceOf(20).iterator(), random, sampler);
            assertThat(sample).isNotNull();
            // minimumSize() must stay 2 regardless of which target size reset() happened to draw.
            assertThat(sampler.minimumSize()).isEqualTo(2);
        }
    }

    @Test
    void allDrainsTheSource() {
        var sample = SampleAssembler.assemble(sourceOf(15).iterator(), seededRandom(), Samplers.all());
        assertThat(sample).isNotNull();
        assertThat(sample.size()).isEqualTo(15);
    }

    @Test
    void constructorGuardsRejectInvalidSizes() {
        assertThatIllegalArgumentException().isThrownBy(() -> Samplers.exactly(0));
        assertThatIllegalArgumentException().isThrownBy(() -> Samplers.upTo(0));
        assertThatIllegalArgumentException().isThrownBy(() -> Samplers.between(3, 2));
    }

    @Test
    void evaluateSeesTheFirstCandidateAtSizeZero() {
        var recordedSizes = new ArrayList<Integer>();
        Sampler<Integer> sampler = (sizeSoFar, candidate) -> {
            recordedSizes.add(sizeSoFar);
            return Decision.ACCEPT_AND_STOP;
        };
        var sample = SampleAssembler.assemble(sourceOf(5).iterator(), seededRandom(), sampler);
        assertThat(sample).isNotNull();
        assertThat(recordedSizes).containsExactly(0);
        assertThat(sample.size()).isEqualTo(1);
    }

    @Test
    void rejectAtSizeZeroKeepsDrawingForAFirstMember() {
        var callCount = new int[1];
        Sampler<Integer> sampler = (sizeSoFar, candidate) -> {
            callCount[0]++;
            return callCount[0] < 3 ? Decision.REJECT : Decision.ACCEPT_AND_STOP;
        };
        // sourceOf(5) offers 0, 1, 2, 3, 4 in order: the first two candidates are rejected,
        // the third (value 2) is accepted and stops the sample.
        var sample = SampleAssembler.assemble(sourceOf(5).iterator(), seededRandom(), sampler);
        assertThat(sample).isNotNull();
        assertThat(sample.size()).isEqualTo(1);
        assertThat(sample.representative()).isEqualTo(2);
    }

    @Test
    void resetRunsBeforeTheFirstEvaluateCall() {
        var callOrder = new ArrayList<String>();
        var sampler = new Sampler<Integer>() {
            @Override
            public void reset(@NonNull RandomGenerator random) {
                callOrder.add("reset");
            }

            @Override
            public Decision evaluate(int sizeSoFar, Integer candidate) {
                callOrder.add("evaluate");
                return Decision.ACCEPT_AND_STOP;
            }
        };
        SampleAssembler.assemble(sourceOf(3).iterator(), seededRandom(), sampler);
        assertThat(callOrder).containsExactly("reset", "evaluate");
    }

    @Test
    void minimumSizeBelowOneThrows() {
        var sampler = new Sampler<Integer>() {
            @Override
            public int minimumSize() {
                return 0;
            }

            @Override
            public Decision evaluate(int sizeSoFar, Integer candidate) {
                return Decision.ACCEPT_AND_STOP;
            }
        };
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SampleAssembler.assemble(sourceOf(3).iterator(), seededRandom(), sampler))
                .withMessageContaining("minimumSize");
    }

    @Test
    void samplerStoppingBelowItsOwnMinimumSizeThrows() {
        var sampler = new Sampler<Integer>() {
            @Override
            public int minimumSize() {
                return 5;
            }

            @Override
            public Decision evaluate(int sizeSoFar, Integer candidate) {
                return sizeSoFar == 0 ? Decision.ACCEPT_AND_STOP : Decision.ACCEPT;
            }
        };
        assertThatIllegalStateException()
                .isThrownBy(() -> SampleAssembler.assemble(sourceOf(10).iterator(), seededRandom(), sampler))
                .withMessageContaining("minimumSize");
    }

    @Test
    void dryStopWithoutSamplerStopReturnsNullNotAnException() {
        var sampler = new Sampler<Integer>() {
            @Override
            public int minimumSize() {
                return 5;
            }

            @Override
            public Decision evaluate(int sizeSoFar, Integer candidate) {
                return Decision.ACCEPT;
            }
        };
        // Only 2 candidates ever exist: the source runs dry before the sampler ever gets a chance to stop,
        // so this is an undersized world, not a contract violation.
        var sample = SampleAssembler.assemble(sourceOf(2).iterator(), seededRandom(), sampler);
        assertThat(sample).isNull();
    }

    @Test
    void pillarForwardsResetToTheWrappedSampler() {
        var resetCallCount = new int[1];
        var sampler = new Sampler<Integer>() {
            @Override
            public void reset(@NonNull RandomGenerator random) {
                resetCallCount[0]++;
            }

            @Override
            public Decision evaluate(int sizeSoFar, Integer candidate) {
                return Decision.ACCEPT_AND_STOP;
            }
        };
        var pillarSampler = Samplers.<String, Integer> pillar(sampler);
        SampleAssembler.assemble(sourceOf(3).iterator(), seededRandom(), "key", pillarSampler);
        assertThat(resetCallCount[0]).isEqualTo(1);
    }

    @Test
    void pillarForwardsEvaluateDecisionsUnchanged() {
        for (var decision : Decision.values()) {
            Sampler<Integer> sampler = (sizeSoFar, candidate) -> decision;
            var pillarSampler = Samplers.<String, Integer> pillar(sampler);
            assertThat(pillarSampler.evaluate(0, 1)).isEqualTo(decision);
        }
    }

    @Test
    void pillarForwardsMinimumSizeAndEnforcesItThroughTheKeyedAssemble() {
        var pillarSampler = Samplers.<String, Integer> pillar(Samplers.exactly(4));
        assertThat(pillarSampler.minimumSize()).isEqualTo(4);

        var sample = SampleAssembler.assemble(sourceOf(3).iterator(), seededRandom(), "key", pillarSampler);
        assertThat(sample).isNull();
    }

    @Test
    void allReportsAnExpectedSizeWhenGivenOne() {
        assertThat(Samplers.all().targetSize()).isEqualTo(1);
        assertThat(Samplers.all(12).targetSize()).isEqualTo(12);

        // The hint changes nothing about what the sampler accepts.
        var sample = SampleAssembler.assemble(sourceOf(5).iterator(), seededRandom(), Samplers.<Integer> all(12));
        assertThat(sample).isNotNull();
        assertThat(sample.size()).isEqualTo(5);

        assertThatIllegalArgumentException().isThrownBy(() -> Samplers.all(0))
                .withMessageContaining("The expectedSize (0)");
    }

    @Test
    void betweenReportsItsDrawnTargetSizeAfterReset() {
        var sampler = Samplers.<Integer> between(3, 7);
        sampler.reset(seededRandom());

        assertThat(sampler.targetSize()).isBetween(3, 7);
        assertThat(Samplers.<Integer> exactly(4).targetSize()).isEqualTo(4);
    }

    @Test
    void pillarForwardsTargetSize() {
        var sampler = Samplers.<Integer> exactly(6);
        var pillarSampler = Samplers.<String, Integer> pillar(sampler);
        pillarSampler.reset(seededRandom(), "key");

        assertThat(pillarSampler.minimumSize()).isEqualTo(6);
        assertThat(pillarSampler.targetSize()).isEqualTo(6);
    }

    @Test
    void assembleRejectsASamplerWhoseTargetSizeIsBelowItsMinimum() {
        var brokenSampler = new Sampler<Integer>() {

            @Override
            public int minimumSize() {
                return 3;
            }

            @Override
            public int targetSize() {
                return 1;
            }

            @Override
            public @NonNull Decision evaluate(int sizeSoFar, Integer candidate) {
                return Decision.ACCEPT;
            }

        };

        assertThatIllegalArgumentException()
                .isThrownBy(() -> SampleAssembler.assemble(sourceOf(5).iterator(), seededRandom(), brokenSampler))
                .withMessageContaining("The targetSize (1)")
                .withMessageContaining("must be at least the minimumSize (3)");
    }

}
