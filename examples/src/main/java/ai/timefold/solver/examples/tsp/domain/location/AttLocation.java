package ai.timefold.solver.examples.tsp.domain.location;

public class AttLocation extends Location {

    private static final double PI = 3.141592;
    private static final double RRR = 6378.388;

    public AttLocation() {
    }

    public AttLocation(long id, double x, double y) {
        super(id, x, y);
    }

    @Override
    public long getDistanceTo(Location location) {
        double xd = latitude - location.latitude;
        double yd = longitude - location.longitude;
        double rij = Math.sqrt((xd * xd + yd * yd) / 10.0);
        double tij = nint(rij);
        return nint(tij < rij ? tij + 1 : tij);
    }

    private static int nint(double x) {
        return (int) (x + 0.5);
    }

}
