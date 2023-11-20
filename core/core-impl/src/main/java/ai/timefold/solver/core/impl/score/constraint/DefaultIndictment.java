package ai.timefold.solver.core.impl.score.constraint;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.impl.util.CollectionUtils;

public final class DefaultIndictment<Score_ extends Score<Score_>> implements Indictment<Score_> {

    private final Object indictedObject;
    private final Set<ConstraintMatch<Score_>> constraintMatchSet = new LinkedHashSet<>();
    private List<ConstraintJustification> constraintJustificationList;
    private Score_ score;

    public DefaultIndictment(Object indictedObject, Score_ zeroScore) {
        this.indictedObject = indictedObject;
        this.score = zeroScore;
    }

    @Override
    public <IndictedObject_> IndictedObject_ getIndictedObject() {
        return (IndictedObject_) indictedObject;
    }

    @Override
    public Set<ConstraintMatch<Score_>> getConstraintMatchSet() {
        return constraintMatchSet;
    }

    @Override
    public List<ConstraintJustification> getJustificationList() {
        if (constraintJustificationList == null) {
            constraintJustificationList = buildConstraintJustificationList();
        }
        return constraintJustificationList;
    }

    private List<ConstraintJustification> buildConstraintJustificationList() {
        var constraintMatchSetSize = constraintMatchSet.size();
        switch (constraintMatchSetSize) {
            case 0 -> {
                return Collections.emptyList();
            }
            case 1 -> {
                return Collections.singletonList(constraintMatchSet.iterator().next().getJustification());
            }
            default -> {
                Set<ConstraintJustification> justificationSet = CollectionUtils.newLinkedHashSet(constraintMatchSetSize);
                for (ConstraintMatch<Score_> constraintMatch : constraintMatchSet) {
                    justificationSet.add(constraintMatch.getJustification());
                }
                return CollectionUtils.toDistinctList(justificationSet);
            }
        }
    }

    @Override
    public Score_ getScore() {
        return score;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public void addConstraintMatch(ConstraintMatch<Score_> constraintMatch) {
        boolean added = addConstraintMatchWithoutFail(constraintMatch);
        if (!added) {
            throw new IllegalStateException("The indictment (" + this
                    + ") could not add constraintMatch (" + constraintMatch
                    + ") to its constraintMatchSet (" + constraintMatchSet + ").");
        }
    }

    public boolean addConstraintMatchWithoutFail(ConstraintMatch<Score_> constraintMatch) {
        boolean added = constraintMatchSet.add(constraintMatch);
        if (added) {
            score = score.add(constraintMatch.getScore());
            constraintJustificationList = null; // Rebuild later.
        }
        return added;
    }

    public void removeConstraintMatch(ConstraintMatch<Score_> constraintMatch) {
        score = score.subtract(constraintMatch.getScore());
        boolean removed = constraintMatchSet.remove(constraintMatch);
        if (!removed) {
            throw new IllegalStateException("The indictment (" + this
                    + ") could not remove constraintMatch (" + constraintMatch
                    + ") from its constraintMatchSet (" + constraintMatchSet + ").");
        }
        constraintJustificationList = null; // Rebuild later.
    }

    // ************************************************************************
    // Infrastructure methods
    // ************************************************************************

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultIndictment other) {
            return indictedObject.equals(other.indictedObject);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return indictedObject.hashCode();
    }

    @Override
    public String toString() {
        return indictedObject + "=" + score;
    }

}
