package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.SolutionExtractor;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;

import org.jspecify.annotations.NonNull;

public final class DefaultDataStreamFactory<Solution_> implements DataStreamFactory<Solution_> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final Map<AbstractDataStream<Solution_>, AbstractDataStream<Solution_>> sharingStreamMap = new HashMap<>(256);

    public DefaultDataStreamFactory(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = solutionDescriptor;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <A> @NonNull UniDataStream<Solution_, A> forEach(@NonNull Class<A> sourceClass) {
        assertValidForEachType(sourceClass);
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(sourceClass);
        if (entityDescriptor == null) {
            // Not genuine or shadow entity; no need for filtering.
            return share(new ForEachDataStream<>(this, sourceClass));
        }
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor == null || !listVariableDescriptor.acceptsValueType(sourceClass)) {
            // No applicable list variable; don't need to check inverse relationships.
            return share(new ForEachDataStream<>(this, sourceClass, entityDescriptor.getHasNoNullVariablesPredicateBasicVar()));
        }
        var entityClass = listVariableDescriptor.getEntityDescriptor().getEntityClass();
        if (entityClass == sourceClass) {
            throw new IllegalStateException("Impossible state: entityClass (%s) and sourceClass (%s) are the same."
                    .formatted(entityClass.getCanonicalName(), sourceClass.getCanonicalName()));
        }
        var shadowDescriptor = listVariableDescriptor.getInverseRelationShadowVariableDescriptor();
        if (shadowDescriptor == null) {
            // The list variable element doesn't have the @InverseRelationShadowVariable annotation.
            // We don't want the users to be forced to implement it in quickstarts,
            // so we'll do this expensive thing instead.
            return forEachIncludingUnassigned(sourceClass)
                    .ifExists((Class) entityClass,
                            Joiners.filtering(listVariableDescriptor.getInListPredicate()));
        } else { // We have the inverse relation variable, so we can read its value directly.
            return share(new ForEachDataStream<>(this, sourceClass, entityDescriptor.getHasNoNullVariablesPredicateListVar()));
        }
    }

    public <A> UniDataStream<Solution_, A> forEachIncludingUnassigned(@NonNull Class<A> sourceClass) {
        assertValidForEachType(sourceClass);
        return share(new ForEachDataStream<>(this, sourceClass));
    }

    public <A> void assertValidForEachType(Class<A> fromType) {
        SolutionDescriptor<Solution_> solutionDescriptor = getSolutionDescriptor();
        Set<Class<?>> problemFactOrEntityClassSet = solutionDescriptor.getProblemFactOrEntityClassSet();
        /*
         * Need to support the following situations:
         * 1/ FactType == FromType; querying for the declared type.
         * 2/ FromType extends/implements FactType; querying for impl type where declared type is its interface.
         * 3/ FromType super FactType; querying for interface where declared type is its implementation.
         */
        boolean hasMatchingType = problemFactOrEntityClassSet.stream()
                .anyMatch(factType -> fromType.isAssignableFrom(factType) || factType.isAssignableFrom(fromType));
        if (!hasMatchingType) {
            List<String> canonicalClassNameList = problemFactOrEntityClassSet.stream()
                    .map(Class::getCanonicalName)
                    .sorted()
                    .toList();
            throw new IllegalArgumentException("Cannot use class (" + fromType.getCanonicalName()
                    + ") in a data stream as it is neither the same as, nor a superclass or superinterface of "
                    + "one of planning entities or problem facts.\n"
                    + "Ensure that all forEach(), join(), ifExists() and ifNotExists() building blocks only reference "
                    + "classes assignable from planning entities or problem facts (" + canonicalClassNameList + ") "
                    + "annotated on the planning solution (" + solutionDescriptor.getSolutionClass().getCanonicalName()
                    + ").");
        }
    }

    @Override
    public @NonNull <A> UniDataStream<Solution_, A> forEach(@NonNull Class<A> sourceClass,
            @NonNull SolutionExtractor<Solution_, A> extractor) {
        assertValidForEachType(sourceClass);
        return share(new ForEachDataStream<>(this, sourceClass, extractor));
    }

    @Override
    public @NonNull <A> UniDataStream<Solution_, A> forEach(@NonNull Class<A> sourceClass, @NonNull Collection<A> collection) {
        // Not checking for valid from type because the collection is designed to inject arbitrary data into the stream.
        return share(new ForEachDataStream<>(this, sourceClass, collection));
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

    public List<AbstractDataset<Solution_, ?>> getDatasets() {
        return sharingStreamMap.values().stream()
                .flatMap(s -> {
                    if (s instanceof TerminalUniDataStream<Solution_, ?> terminalStream) {
                        return Stream.of(terminalStream.getDataset());
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toList());
    }

}
