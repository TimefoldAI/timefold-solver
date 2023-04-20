package ai.timefold.solver.examples.machinereassignment.domain;

import java.util.List;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.common.persistence.jackson.JacksonUniqueIdGenerator;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;

@JsonIdentityInfo(generator = JacksonUniqueIdGenerator.class)
public class MrService extends AbstractPersistable {

    private List<MrService> toDependencyServiceList;
    private List<MrService> fromDependencyServiceList;

    private int locationSpread;

    @SuppressWarnings("unused")
    MrService() {
    }

    public MrService(long id) {
        super(id);
    }

    public List<MrService> getToDependencyServiceList() {
        return toDependencyServiceList;
    }

    public void setToDependencyServiceList(List<MrService> toDependencyServiceList) {
        this.toDependencyServiceList = toDependencyServiceList;
    }

    public List<MrService> getFromDependencyServiceList() {
        return fromDependencyServiceList;
    }

    public void setFromDependencyServiceList(List<MrService> fromDependencyServiceList) {
        this.fromDependencyServiceList = fromDependencyServiceList;
    }

    public int getLocationSpread() {
        return locationSpread;
    }

    public void setLocationSpread(int locationSpread) {
        this.locationSpread = locationSpread;
    }

}
