package ai.timefold.solver.examples.tsp.domain.location;

public class AttLocation extends Location {

    public AttLocation() {
    }

    public AttLocation(long id, double x, double y) {
        super(id, x, y);
    }

    @Override
    public long getDistanceTo(Location location) {
        double xd = location.latitude - latitude;
        double yd = location.longitude - longitude;
        double rij = Math.sqrt((xd * xd + yd * yd) / 10.0);
        long tij = adjust(rij);
        return tij < rij ? tij + 1 : tij;
    }

}
