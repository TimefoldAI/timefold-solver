package ai.timefold.solver.examples.tennis.domain;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.common.swingui.components.Labeled;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(scope = Day.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Day extends AbstractPersistable implements Labeled {

    private int dateIndex;

    public Day() {
    }

    public Day(long id, int dateIndex) {
        super(id);
        this.dateIndex = dateIndex;
    }

    public int getDateIndex() {
        return dateIndex;
    }

    public void setDateIndex(int dateIndex) {
        this.dateIndex = dateIndex;
    }

    @Override
    public String getLabel() {
        return "day " + dateIndex;
    }

}
