package ai.timefold.solver.core.impl.testdata.util;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.heuristic.move.CompositeMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.NoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.heuristic.selector.list.UnassignedLocation;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListAssignMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListUnassignMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListUnassignMove;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;

public interface CodeAssertable {

    String getCode();

    static CodeAssertable convert(Object o) {
        Objects.requireNonNull(o);
        if (o instanceof CodeAssertable assertable) {
            return assertable;
        } else if (o instanceof NoChangeMove<?>) {
            return () -> "No change";
        } else if (o instanceof ChangeMove<?> changeMove) {
            final String code = convert(changeMove.getEntity()).getCode()
                    + "->" + convert(changeMove.getToPlanningValue()).getCode();
            return () -> code;
        } else if (o instanceof SwapMove<?> swapMove) {
            final String code = convert(swapMove.getLeftEntity()).getCode()
                    + "<->" + convert(swapMove.getRightEntity()).getCode();
            return () -> code;
        } else if (o instanceof CompositeMove<?> compositeMove) {
            StringBuilder codeBuilder = new StringBuilder(compositeMove.getMoves().length * 80);
            for (Move<?> move : compositeMove.getMoves()) {
                codeBuilder.append("+").append(convert(move).getCode());
            }
            final String code = codeBuilder.substring(1);
            return () -> code;
        } else if (o instanceof ListAssignMove<?> listAssignMove) {
            return () -> convert(listAssignMove.getMovedValue())
                    + " {null->"
                    + convert(listAssignMove.getDestinationEntity())
                    + "[" + listAssignMove.getDestinationIndex() + "]}";
        } else if (o instanceof ListUnassignMove<?> listUnassignMove) {
            return () -> convert(listUnassignMove.getMovedValue())
                    + " {" + convert(listUnassignMove.getSourceEntity())
                    + "[" + listUnassignMove.getSourceIndex() + "]->null}";
        } else if (o instanceof ListChangeMove<?> listChangeMove) {
            return () -> convert(listChangeMove.getMovedValue())
                    + " {" + convert(listChangeMove.getSourceEntity())
                    + "[" + listChangeMove.getSourceIndex() + "]->"
                    + convert(listChangeMove.getDestinationEntity())
                    + "[" + listChangeMove.getDestinationIndex() + "]}";
        } else if (o instanceof ListSwapMove<?> listSwapMove) {
            return () -> convert(listSwapMove.getLeftValue())
                    + " {" + convert(listSwapMove.getLeftEntity())
                    + "[" + listSwapMove.getLeftIndex() + "]} <-> "
                    + convert(listSwapMove.getRightValue())
                    + " {" + convert(listSwapMove.getRightEntity())
                    + "[" + listSwapMove.getRightIndex() + "]}";
        } else if (o instanceof SubListChangeMove<?> subListChangeMove) {
            return () -> "|" + subListChangeMove.getSubListSize()
                    + "| {" + convert(subListChangeMove.getSourceEntity())
                    + "[" + subListChangeMove.getFromIndex()
                    + ".." + subListChangeMove.getToIndex()
                    + "]-" + (subListChangeMove.isReversing() ? "reversing->" : ">")
                    + convert(subListChangeMove.getDestinationEntity())
                    + "[" + subListChangeMove.getDestinationIndex() + "]}";
        } else if (o instanceof SubListUnassignMove<?> subListUnassignMove) {
            return () -> "|" + subListUnassignMove.getSubListSize()
                    + "| {" + convert(subListUnassignMove.getSourceEntity())
                    + "[" + subListUnassignMove.getFromIndex()
                    + ".." + subListUnassignMove.getToIndex()
                    + "]->null}";
        } else if (o instanceof SubListSwapMove<?> subListSwapMove) {
            return () -> "{" + convert(subListSwapMove.getLeftSubList()).getCode()
                    + "} <-" + (subListSwapMove.isReversing() ? "reversing-" : "")
                    + "> {" + convert(subListSwapMove.getRightSubList()).getCode() + "}";
        } else if (o instanceof List<?> list) {
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
        } else if (o instanceof SubList subList) {
            return () -> convert(subList.entity()) + "[" + subList.fromIndex() + "+" + subList.length() + "]";
        } else if (o instanceof UnassignedLocation unassignedLocation) {
            return unassignedLocation::toString;
        } else if (o instanceof LocationInList locationInList) {
            return () -> convert(locationInList.entity()) + "[" + locationInList.index() + "]";
        } else if (o instanceof SubChain subChain) {
            final String code = convert(subChain.getEntityList()).getCode();
            return () -> code;
        }
        throw new AssertionError(("o's class (" + o.getClass() + ") cannot be converted to CodeAssertable."));
    }
}
