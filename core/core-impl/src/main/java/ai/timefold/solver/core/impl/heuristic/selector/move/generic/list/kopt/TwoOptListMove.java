package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;

/**
 * A 2-opt move for list variables, which takes two edges and swap their endpoints.
 * For instance, let [A, B, E, D, C, F, G, H] be the route assigned to an entity.
 * Select (B, E) and (C, F) as the edges to swap.
 * Then the resulting route after this operation would be [A, B, C, D, E, F, G, H].
 * The edge (B, E) became (B, C), and the edge (C, F) became (E, F)
 * (the first edge end point became the second edge start point and vice versa).
 * It is used to fix crossings; for instance, it can change:
 *
 * <pre>{@code
 * ... -> A B <- ...
 * ....... x .......
 * ... <- C D -> ...
 * }</pre>
 *
 * to
 *
 * <pre>{@code
 * ... -> A -> B -> ...
 * ... <- C <- D <- ...
 * }</pre>
 *
 * Note the sub-path D...B was reversed.
 * The 2-opt works by reversing the path between the two edges being removed.
 * <p>
 * When the edges are assigned to different entities,
 * it results in a tail swap.
 * For instance, let r1 = [A, B, C, D], and r2 = [E, F, G, H].
 * Doing a 2-opt on (B, C) + (F, G) will result in r1 = [A, B, G, H] and r2 = [E, F, C, D].
 *
 * @param <Solution_>
 */
public final class TwoOptListMove<Solution_> extends AbstractMove<Solution_> {
    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object firstEntity;
    private final Object secondEntity;
    private final int firstEdgeEndpoint;
    private final int secondEdgeEndpoint;

    private final int shift;
    private final int entityFirstUnpinnedIndex;

    public TwoOptListMove(ListVariableDescriptor<Solution_> variableDescriptor,
            Object firstEntity, Object secondEntity,
            int firstEdgeEndpoint,
            int secondEdgeEndpoint) {
        this.variableDescriptor = variableDescriptor;
        this.firstEntity = firstEntity;
        this.secondEntity = secondEntity;
        this.firstEdgeEndpoint = firstEdgeEndpoint;
        this.secondEdgeEndpoint = secondEdgeEndpoint;
        if (firstEntity == secondEntity) {
            entityFirstUnpinnedIndex = variableDescriptor.getFirstUnpinnedIndex(firstEntity);
            if (firstEdgeEndpoint == 0) {
                shift = -secondEdgeEndpoint;
            } else if (secondEdgeEndpoint < firstEdgeEndpoint) {
                int listSize = variableDescriptor.getListSize(firstEntity);
                int flippedSectionSize = listSize - firstEdgeEndpoint + secondEdgeEndpoint;
                int firstElementIndexInFlipped = listSize - firstEdgeEndpoint;
                int firstElementMirroredIndex = flippedSectionSize - firstElementIndexInFlipped;
                shift = -(firstEdgeEndpoint + firstElementMirroredIndex - 1);
            } else {
                shift = 0;
            }
        } else {
            // This is a tail swap move, so entityFirstUnpinnedIndex is unused as no
            // flipping will be done
            entityFirstUnpinnedIndex = 0;
            shift = 0;
        }
    }

    private TwoOptListMove(ListVariableDescriptor<Solution_> variableDescriptor,
            Object firstEntity, Object secondEntity,
            int firstEdgeEndpoint,
            int secondEdgeEndpoint,
            int entityFirstUnpinnedIndex,
            int shift) {
        this.variableDescriptor = variableDescriptor;
        this.firstEntity = firstEntity;
        this.secondEntity = secondEntity;
        this.firstEdgeEndpoint = firstEdgeEndpoint;
        this.secondEdgeEndpoint = secondEdgeEndpoint;
        this.entityFirstUnpinnedIndex = entityFirstUnpinnedIndex;
        this.shift = shift;
    }

    @Override
    protected TwoOptListMove<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        return new TwoOptListMove<>(variableDescriptor,
                firstEntity,
                secondEntity,
                firstEdgeEndpoint,
                secondEdgeEndpoint,
                entityFirstUnpinnedIndex,
                -shift);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        if (firstEntity == secondEntity) {
            doSublistReversal(scoreDirector);
        } else {
            doTailSwap(scoreDirector);
        }
    }

    private void doTailSwap(ScoreDirector<Solution_> scoreDirector) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        List<Object> firstListVariable = variableDescriptor.getValue(firstEntity);
        List<Object> secondListVariable = variableDescriptor.getValue(secondEntity);
        int firstOriginalSize = firstListVariable.size();
        int secondOriginalSize = secondListVariable.size();

        innerScoreDirector.beforeListVariableChanged(variableDescriptor, firstEntity,
                firstEdgeEndpoint,
                firstOriginalSize);
        innerScoreDirector.beforeListVariableChanged(variableDescriptor, secondEntity,
                secondEdgeEndpoint,
                secondOriginalSize);

        List<Object> firstListVariableTail = firstListVariable.subList(firstEdgeEndpoint, firstOriginalSize);
        List<Object> secondListVariableTail = secondListVariable.subList(secondEdgeEndpoint, secondOriginalSize);

        int tailSizeDifference = secondListVariableTail.size() - firstListVariableTail.size();

        List<Object> firstListVariableTailCopy = new ArrayList<>(firstListVariableTail);
        firstListVariableTail.clear();
        firstListVariable.addAll(secondListVariableTail);
        secondListVariableTail.clear();
        secondListVariable.addAll(firstListVariableTailCopy);

        innerScoreDirector.afterListVariableChanged(variableDescriptor, firstEntity,
                firstEdgeEndpoint,
                firstOriginalSize + tailSizeDifference);
        innerScoreDirector.afterListVariableChanged(variableDescriptor, secondEntity,
                secondEdgeEndpoint,
                secondOriginalSize - tailSizeDifference);
    }

    private void doSublistReversal(ScoreDirector<Solution_> scoreDirector) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        List<Object> listVariable = variableDescriptor.getValue(firstEntity);

        if (firstEdgeEndpoint < secondEdgeEndpoint) {
            if (firstEdgeEndpoint > 0) {
                innerScoreDirector.beforeListVariableChanged(variableDescriptor, firstEntity,
                        firstEdgeEndpoint,
                        secondEdgeEndpoint);
            } else {
                innerScoreDirector.beforeListVariableChanged(variableDescriptor, firstEntity,
                        entityFirstUnpinnedIndex,
                        listVariable.size());
            }

            if (firstEdgeEndpoint == 0 && shift > 0) {
                if (entityFirstUnpinnedIndex == 0) {
                    Collections.rotate(listVariable, shift);
                } else {
                    Collections.rotate(listVariable.subList(entityFirstUnpinnedIndex, listVariable.size()),
                            shift);
                }
            }

            FlipSublistAction.flipSublist(listVariable, entityFirstUnpinnedIndex, firstEdgeEndpoint, secondEdgeEndpoint);

            if (firstEdgeEndpoint == 0 && shift < 0) {
                if (entityFirstUnpinnedIndex == 0) {
                    Collections.rotate(listVariable, shift);
                } else {
                    Collections.rotate(listVariable.subList(entityFirstUnpinnedIndex, listVariable.size()),
                            shift);
                }
            }

            if (firstEdgeEndpoint > 0) {
                innerScoreDirector.afterListVariableChanged(variableDescriptor, firstEntity,
                        firstEdgeEndpoint,
                        secondEdgeEndpoint);
            } else {
                innerScoreDirector.afterListVariableChanged(variableDescriptor, firstEntity,
                        entityFirstUnpinnedIndex,
                        listVariable.size());
            }
        } else {
            innerScoreDirector.beforeListVariableChanged(variableDescriptor, firstEntity,
                    entityFirstUnpinnedIndex,
                    listVariable.size());

            if (shift > 0) {
                if (entityFirstUnpinnedIndex == 0) {
                    Collections.rotate(listVariable, shift);
                } else {
                    Collections.rotate(listVariable.subList(entityFirstUnpinnedIndex, listVariable.size()),
                            shift);
                }
            }

            FlipSublistAction.flipSublist(listVariable, entityFirstUnpinnedIndex, firstEdgeEndpoint, secondEdgeEndpoint);

            if (shift < 0) {
                if (entityFirstUnpinnedIndex == 0) {
                    Collections.rotate(listVariable, shift);
                } else {
                    Collections.rotate(listVariable.subList(entityFirstUnpinnedIndex, listVariable.size()),
                            shift);
                }
            }
            innerScoreDirector.afterListVariableChanged(variableDescriptor, firstEntity,
                    entityFirstUnpinnedIndex,
                    listVariable.size());
        }
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        if (firstEntity == secondEntity) {
            if (shift != 0) {
                // A shift will rotate the entire list, changing the visiting order
                return true;
            }
            int chainLength = Math.abs(secondEdgeEndpoint - firstEdgeEndpoint);

            // The chain flipped by a K-Opt only changes if there are at least 2 values
            // in the chain
            return chainLength >= 2;
        }
        // This is a tail-swap move otherwise, which always changes at least one element
        // (the element where the tail begins for each entity)
        return true;
    }

    @Override
    public TwoOptListMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new TwoOptListMove<>(variableDescriptor,
                destinationScoreDirector.lookUpWorkingObject(firstEntity),
                destinationScoreDirector.lookUpWorkingObject(secondEntity),
                firstEdgeEndpoint,
                secondEdgeEndpoint,
                entityFirstUnpinnedIndex,
                shift);
    }

    @Override
    public String getSimpleMoveTypeDescription() {
        return "2-Opt(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<?> getPlanningEntities() {
        if (firstEntity == secondEntity) {
            return Collections.singleton(firstEntity);
        }
        return Set.of(firstEntity, secondEntity);
    }

    @Override
    public Collection<?> getPlanningValues() {
        if (firstEntity == secondEntity) {
            List<Object> listVariable = variableDescriptor.getValue(firstEntity);
            if (firstEdgeEndpoint < secondEdgeEndpoint) {
                return new ArrayList<>(listVariable.subList(firstEdgeEndpoint, secondEdgeEndpoint));
            } else {
                List<Object> firstHalfReversedPath = listVariable.subList(firstEdgeEndpoint, listVariable.size());
                List<Object> secondHalfReversedPath = listVariable.subList(entityFirstUnpinnedIndex, secondEdgeEndpoint);
                return CollectionUtils.concat(firstHalfReversedPath, secondHalfReversedPath);
            }
        } else {
            List<Object> firstListVariable = variableDescriptor.getValue(firstEntity);
            List<Object> secondListVariable = variableDescriptor.getValue(secondEntity);
            List<Object> firstListVariableTail = firstListVariable.subList(firstEdgeEndpoint, firstListVariable.size());
            List<Object> secondListVariableTail = secondListVariable.subList(secondEdgeEndpoint, secondListVariable.size());
            List<Object> out = new ArrayList<>(firstListVariableTail.size() + secondListVariableTail.size());
            out.addAll(firstListVariableTail);
            out.addAll(secondListVariableTail);
            return out;
        }
    }

    public Object getFirstEntity() {
        return firstEntity;
    }

    public Object getSecondEntity() {
        return secondEntity;
    }

    public Object getFirstEdgeEndpoint() {
        return firstEdgeEndpoint;
    }

    public Object getSecondEdgeEndpoint() {
        return secondEdgeEndpoint;
    }

    @Override
    public String toString() {
        return "2-Opt(firstEntity=" +
                firstEntity +
                ", secondEntity=" + secondEntity +
                ", firstEndpointIndex=" + firstEdgeEndpoint +
                ", secondEndpointIndex=" + secondEdgeEndpoint +
                ")";
    }
}
