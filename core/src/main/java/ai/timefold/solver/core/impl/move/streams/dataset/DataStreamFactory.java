package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.domain.solution.descriptor.InnerGenuineVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.move.streams.dataset.common.TerminalDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DataStreamFactory<Solution_> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final Map<AbstractDataStream<Solution_>, AbstractDataStream<Solution_>> sharingStreamMap = new HashMap<>(256);

    public DataStreamFactory(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
    }

    public <A> UniDataStream<Solution_, A> forEachNonDiscriminating(Class<A> sourceClass, boolean includeNull) {
        assertValidForEachType(sourceClass);
        return share(new ForEachIncludingPinnedDataStream<>(this, sourceClass, includeNull));
    }

    @SuppressWarnings("unchecked")
    public <A> UniDataStream<Solution_, A> forEachExcludingPinned(Class<A> sourceClass, boolean includeNull) {
        assertValidForEachType(sourceClass);
        if (!solutionDescriptor.getMetaModel().hasEntity(sourceClass)) {
            // The sourceClass is not a planning entity, therefore it cannot be pinned.
            return forEachNonDiscriminating(sourceClass, includeNull);
        }
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        // We have a basic variable, or the sourceClass is not a valid type for a list variable value.
        // In that case, we use the standard exclusion logic.
        if (listVariableDescriptor == null || !listVariableDescriptor.acceptsValueType(sourceClass)) {
            return share(new ForEachExcludingPinnedDataStream<>(this, solutionDescriptor.getMetaModel().entity(sourceClass),
                    includeNull));
        }
        // The sourceClass is a list variable value, therefore we need to specialize the exclusion logic.
        var parentEntityDescriptor = listVariableDescriptor.getEntityDescriptor();
        if (!parentEntityDescriptor.supportsPinning()) { // Should have been eliminated earlier.
            throw new UnsupportedOperationException("Impossible state: the list variable (%s) does not support pinning."
                    .formatted(listVariableDescriptor.getVariableName()));
        }
        var stream = forEachNonDiscriminating(sourceClass, includeNull)
                .ifNotExists(parentEntityDescriptor.getEntityClass(),
                        Joiners.filtering(listVariableDescriptor.getEntityContainsPinnedValuePredicate()));
        return share((AbstractUniDataStream<Solution_, A>) stream);
    }

    @SuppressWarnings("unchecked")
    public <A> UniDataStream<Solution_, A> forEachFromSolution(GenuineVariableMetaModel<Solution_, ?, A> variableMetaModel,
            boolean includeNull) {
        var variableDescriptor = ((InnerGenuineVariableMetaModel<Solution_>) variableMetaModel).variableDescriptor();
        return share(new ForEachFromSolutionDataStream<>(this, variableDescriptor.getValueRangeDescriptor(),
                includeNull));
    }

    public <A> void assertValidForEachType(Class<A> fromType) {
        var problemFactOrEntityClassSet = solutionDescriptor.getProblemFactOrEntityClassSet();
        /*
         * Need to support the following situations:
         * 1/ FactType == FromType; querying for the declared type.
         * 2/ FromType extends/implements FactType; querying for impl type where declared type is its interface.
         * 3/ FromType super FactType; querying for interface where declared type is its implementation.
         */
        var hasMatchingType = problemFactOrEntityClassSet.stream()
                .anyMatch(factType -> fromType.isAssignableFrom(factType) || factType.isAssignableFrom(fromType));
        if (!hasMatchingType) {
            var canonicalClassNameList = problemFactOrEntityClassSet.stream()
                    .map(Class::getCanonicalName)
                    .sorted()
                    .toList();
            throw new IllegalArgumentException("""
                    Cannot use class (%s) in a data stream as it is neither the same as, \
                    nor a superclass or superinterface of one of planning entities or problem facts.
                    Ensure that all forEach(), join(), ifExists() and ifNotExists() building blocks only reference classes \
                    assignable from planning entities or problem facts (%s) annotated on the planning solution (%s)."""
                    .formatted(fromType.getCanonicalName(), canonicalClassNameList,
                            solutionDescriptor.getSolutionClass().getCanonicalName()));
        }
    }

    public <Stream_ extends AbstractDataStream<Solution_>> Stream_ share(Stream_ stream) {
        return share(stream, t -> {
        });
    }

    /**
     * Enables node sharing.
     * If a stream already exists in this factory, it replaces it with the old copy.
     * {@link AbstractDataStream} implement equals/hashcode ignoring child streams.
     * <p>
     * {@link DatasetSessionFactory#buildSession()} needs this to happen for all streams.
     * <p>
     * This must be called before the stream receives child streams.
     *
     * @param stream never null
     * @param consumer never null
     * @param <Stream_> the {@link AbstractDataStream} subclass
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public <Stream_ extends AbstractDataStream<Solution_>> Stream_ share(Stream_ stream, Consumer<Stream_> consumer) {
        return (Stream_) sharingStreamMap.computeIfAbsent(stream, k -> {
            consumer.accept(stream);
            return stream;
        });
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    @SuppressWarnings("unchecked")
    public List<AbstractDataset<Solution_, ?>> getDatasets() {
        return sharingStreamMap.values().stream()
                .flatMap(s -> {
                    if (s instanceof TerminalDataStream<?, ?, ?> terminalStream) {
                        return Stream.of((AbstractDataset<Solution_, ?>) terminalStream.getDataset());
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toList());
    }

}
