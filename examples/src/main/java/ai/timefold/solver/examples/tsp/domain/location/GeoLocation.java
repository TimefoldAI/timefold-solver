package ai.timefold.solver.examples.tsp.domain.location;

/**
 * Used with {@link DistanceType#GEO}.
 */
public class GeoLocation extends Location {

    private static final double PI = 3.141592;
    private static final double RRR = 6378.388;

    public GeoLocation() {
    }

    public GeoLocation(long id, double x, double y) {
        super(id, toCoordinate(x), toCoordinate(y));
    }

    @Override
    public long getDistanceTo(Location location) {
        double q1 = Math.cos(longitude - location.longitude);
        double q2 = Math.cos(latitude - location.latitude);
        double q3 = Math.cos(latitude + location.latitude);
        return adjust(RRR * Math.acos(0.5 * ((1.0 + q1) * q2 - (1.0 - q1) * q3)) + 1.0);
    }

    private static double toCoordinate(double i) {
        int deg = nint(i);
        double min = i - deg;
        return PI * (deg + 5.0 * min / 3.0) / 180.0;
    }

    private static int nint(double x) {
        return (int) (x + 0.5);
    }

}
