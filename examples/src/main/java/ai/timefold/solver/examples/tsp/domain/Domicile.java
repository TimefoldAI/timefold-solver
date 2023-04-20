package ai.timefold.solver.examples.tsp.domain;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.tsp.domain.location.Location;

public class Domicile extends AbstractPersistable implements Standstill {

    private Location location;

    public Domicile() {
    }

    public Domicile(long id) {
        super(id);
    }

    public Domicile(long id, Location location) {
        this(id);
        this.location = location;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    /**
     * @param standstill never null
     * @return a positive number, the distance multiplied by 1000 to avoid floating point arithmetic rounding errors
     */
    @Override
    public long getDistanceTo(Standstill standstill) {
        return location.getDistanceTo(standstill.getLocation());
    }

    @Override
    public String toString() {
        if (location.getName() == null) {
            return super.toString();
        }
        return location.getName();
    }

}
