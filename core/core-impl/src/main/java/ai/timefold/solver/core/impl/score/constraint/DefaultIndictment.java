package ai.timefold.solver.core.impl.score.constraint;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

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
            constraintJustificationList = constraintMatchSet.stream()
                    .map(s -> (ConstraintJustification) s.getJustification())
                    .distinct()
                    .collect(Collectors.toList());
        }
        return constraintJustificationList;
    }

    @Override
    public Score_ getScore() {
        return score;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public void addConstraintMatch(ConstraintMatch<Score_> constraintMatch) {
        score = score.add(constraintMatch.getScore());
        boolean added = constraintMatchSet.add(constraintMatch);
        if (!added) {
            throw new IllegalStateException("The indictment (" + this
                    + ") could not add constraintMatch (" + constraintMatch
                    + ") to its constraintMatchSet (" + constraintMatchSet + ").");
        }
        constraintJustificationList = null; // Rebuild later.
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
        if (this == o) {
            return true;
        } else if (o instanceof DefaultIndictment) {
            DefaultIndictment<Score_> other = (DefaultIndictment<Score_>) o;
            return indictedObject.equals(other.indictedObject);
        } else {
            return false;
        }
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
