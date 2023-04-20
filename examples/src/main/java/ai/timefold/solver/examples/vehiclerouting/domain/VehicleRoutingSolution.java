package ai.timefold.solver.examples.vehiclerouting.domain;

import java.text.NumberFormat;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.vehiclerouting.domain.location.DistanceType;
import ai.timefold.solver.examples.vehiclerouting.domain.location.Location;

@PlanningSolution
public class VehicleRoutingSolution extends AbstractPersistable {

    protected String name;
    protected DistanceType distanceType;
    protected String distanceUnitOfMeasurement;
    protected List<Location> locationList;
    protected List<Depot> depotList;
    protected List<Vehicle> vehicleList;

    protected List<Customer> customerList;

    protected HardSoftLongScore score;

    public VehicleRoutingSolution() {
        this(0);
    }

    public VehicleRoutingSolution(long id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DistanceType getDistanceType() {
        return distanceType;
    }

    public void setDistanceType(DistanceType distanceType) {
        this.distanceType = distanceType;
    }

    public String getDistanceUnitOfMeasurement() {
        return distanceUnitOfMeasurement;
    }

    public void setDistanceUnitOfMeasurement(String distanceUnitOfMeasurement) {
        this.distanceUnitOfMeasurement = distanceUnitOfMeasurement;
    }

    @ProblemFactCollectionProperty
    public List<Location> getLocationList() {
        return locationList;
    }

    public void setLocationList(List<Location> locationList) {
        this.locationList = locationList;
    }

    @ProblemFactCollectionProperty
    public List<Depot> getDepotList() {
        return depotList;
    }

    public void setDepotList(List<Depot> depotList) {
        this.depotList = depotList;
    }

    @PlanningEntityCollectionProperty
    public List<Vehicle> getVehicleList() {
        return vehicleList;
    }

    public void setVehicleList(List<Vehicle> vehicleList) {
        this.vehicleList = vehicleList;
    }

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    public List<Customer> getCustomerList() {
        return customerList;
    }

    public void setCustomerList(List<Customer> customerList) {
        this.customerList = customerList;
    }

    @PlanningScore
    public HardSoftLongScore getScore() {
        return score;
    }

    public void setScore(HardSoftLongScore score) {
        this.score = score;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    public String getDistanceString(NumberFormat numberFormat) {
        if (score == null) {
            return null;
        }
        long distance = -score.softScore();
        if (distanceUnitOfMeasurement == null) {
            return numberFormat.format(distance / 1000.0);
        }
        switch (distanceUnitOfMeasurement) {
            case "sec": // TODO why are the values 1000 larger?
                long hours = distance / 3600000L;
                long minutes = distance % 3600000L / 60000L;
                long seconds = distance % 60000L / 1000L;
                long milliseconds = distance % 1000L;
                return hours + "h " + minutes + "m " + seconds + "s " + milliseconds + "ms";
            case "km": { // TODO why are the values 1000 larger?
                long km = distance / 1000L;
                long meter = distance % 1000L;
                return km + "km " + meter + "m";
            }
            case "meter": {
                long km = distance / 1000L;
                long meter = distance % 1000L;
                return km + "km " + meter + "m";
            }
            default:
                return numberFormat.format(distance / 1000.0) + " " + distanceUnitOfMeasurement;
        }
    }

}
