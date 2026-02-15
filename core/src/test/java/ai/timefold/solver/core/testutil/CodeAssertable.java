package ai.timefold.solver.core.testutil;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedCompositeMove;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedNoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedPillarChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListAssignMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListUnassignMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedSubListChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedSubListSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedSubListUnassignMove;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;
import ai.timefold.solver.core.preview.api.move.Move;

public interface CodeAssertable {

    String getCode();

    static CodeAssertable convert(Object o) {
        Objects.requireNonNull(o);
        switch (o) {
            case CodeAssertable assertable -> {
                return assertable;
            }
            case SelectorBasedNoChangeMove<?> selectorBasedNoChangeMove -> {
                return () -> "No change";
            }
            case SelectorBasedChangeMove<?> changeMove -> {
                final String code = convert(changeMove.getEntity()).getCode()
                        + "->" + convert(changeMove.getToPlanningValue()).getCode();
                return () -> code;
            }
            case SelectorBasedSwapMove<?> swapMove -> {
                final String code = convert(swapMove.getLeftEntity()).getCode()
                        + "<->" + convert(swapMove.getRightEntity()).getCode();
                return () -> code;
            }
            case SelectorBasedPillarChangeMove<?> pillarChangeMove -> {
                final String code = pillarChangeMove.getPillar() +
                        "->" + convert(pillarChangeMove.getToPlanningValue()).getCode();
                return () -> code;
            }
            case SelectorBasedCompositeMove<?> compositeMove -> {
                StringBuilder codeBuilder = new StringBuilder(compositeMove.getMoves().length * 80);
                for (Move<?> move : compositeMove.getMoves()) {
                    codeBuilder.append("+").append(convert(move).getCode());
                }
                final String code = codeBuilder.substring(1);
                return () -> code;
            }
            case SelectorBasedListAssignMove<?> listAssignMove -> {
                return () -> convert(listAssignMove.getMovedValue())
                        + " {null->"
                        + convert(listAssignMove.getDestinationEntity())
                        + "[" + listAssignMove.getDestinationIndex() + "]}";
            }
            case SelectorBasedListUnassignMove<?> listUnassignMove -> {
                return () -> convert(listUnassignMove.getMovedValue())
                        + " {" + convert(listUnassignMove.getSourceEntity())
                        + "[" + listUnassignMove.getSourceIndex() + "]->null}";
            }
            case SelectorBasedListChangeMove<?> listChangeMove -> {
                return () -> convert(listChangeMove.getMovedValue())
                        + " {" + convert(listChangeMove.getSourceEntity())
                        + "[" + listChangeMove.getSourceIndex() + "]->"
                        + convert(listChangeMove.getDestinationEntity())
                        + "[" + listChangeMove.getDestinationIndex() + "]}";
            }
            case SelectorBasedListSwapMove<?> listSwapMove -> {
                return () -> convert(listSwapMove.getLeftValue())
                        + " {" + convert(listSwapMove.getLeftEntity())
                        + "[" + listSwapMove.getLeftIndex() + "]} <-> "
                        + convert(listSwapMove.getRightValue())
                        + " {" + convert(listSwapMove.getRightEntity())
                        + "[" + listSwapMove.getRightIndex() + "]}";
            }
            case SelectorBasedSubListChangeMove<?> subListChangeMove -> {
                return () -> "|" + subListChangeMove.getSubListSize()
                        + "| {" + convert(subListChangeMove.getSourceEntity())
                        + "[" + subListChangeMove.getFromIndex()
                        + ".." + subListChangeMove.getToIndex()
                        + "]-" + (subListChangeMove.isReversing() ? "reversing->" : ">")
                        + convert(subListChangeMove.getDestinationEntity())
                        + "[" + subListChangeMove.getDestinationIndex() + "]}";
            }
            case SelectorBasedSubListUnassignMove<?> subListUnassignMove -> {
                return () -> "|" + subListUnassignMove.getSubListSize()
                        + "| {" + convert(subListUnassignMove.getSourceEntity())
                        + "[" + subListUnassignMove.getFromIndex()
                        + ".." + subListUnassignMove.getToIndex()
                        + "]->null}";
            }
            case SelectorBasedSubListSwapMove<?> subListSwapMove -> {
                return () -> "{" + convert(subListSwapMove.getLeftSubList()).getCode()
                        + "} <-" + (subListSwapMove.isReversing() ? "reversing-" : "")
                        + "> {" + convert(subListSwapMove.getRightSubList()).getCode() + "}";
            }
            case List<?> list -> {
                StringBuilder codeBuilder = new StringBuilder("[");
                boolean firstElement = true;
                for (Object element : list) {
                    if (firstElement) {
                        firstElement = false;
                    } else {
                        codeBuilder.append(", ");
                    }
                    codeBuilder.append(convert(element).getCode());
                }
                codeBuilder.append("]");
                final String code = codeBuilder.toString();
                return () -> code;
            }
            case SubList subList -> {
                return () -> convert(subList.entity()) + "[" + subList.fromIndex() + "+" + subList.length() + "]";
            }
            case UnassignedElement unassignedLocation -> {
                return unassignedLocation::toString;
            }
            case PositionInList locationInList -> {
                return () -> convert(locationInList.entity()) + "[" + locationInList.index() + "]";
            }
            default -> {
            }
        }
        throw new AssertionError(("o's class (" + o.getClass() + ") cannot be converted to CodeAssertable."));
    }
}
