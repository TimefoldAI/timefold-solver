package ai.timefold.solver.service.maps.api.model;

import java.time.OffsetDateTime;
import java.util.function.ToIntFunction;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.IndexableDistanceMatrix;
import ai.timefold.solver.service.maps.api.model.travel.TravelDistance;
import ai.timefold.solver.service.maps.api.model.travel.TravelTime;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(implementation = Double.class, minItems = 2, maxItems = 2, example = "[40.5044403760272, -76.37894009358867]",
        description = "Array of two elements: latitude and longitude, in that order.", type = SchemaType.ARRAY)
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class Location {

    private double latitude;

    private double longitude;

    @JsonIgnore
    private DistanceMatrix travelTimeMatrix;

    // Performance optimization for the LongArrayDistanceMatrix indexing.
    @JsonIgnore
    private short travelTimeMatrixIndex = IndexableDistanceMatrix.EMPTY_INDEX;

    @JsonIgnore
    private DistanceMatrix distanceMatrix;

    // Performance optimization for the LongArrayDistanceMatrix indexing.
    @JsonIgnore
    private short distanceMatrixIndex = IndexableDistanceMatrix.EMPTY_INDEX;

    @JsonIgnore
    private DistanceMatrix[] travelTimesByTimeframe;

    @JsonIgnore
    private DistanceMatrix[] distancesByTimeframe;

    @JsonIgnore
    private ToIntFunction<OffsetDateTime> timeframeIndexResolver;

    public Location() {
    }

    public Location(@JsonProperty("latitude") @Min(-90) @Max(90) double latitude,
            @JsonProperty("longitude") @Min(-180) @Max(180) double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;

        validateLatitude();
        validateLongitude();
    }

    public static Location of(double latitude, double longitude) {
        return new Location(latitude, longitude);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
        validateLatitude();
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
        validateLongitude();
    }

    public void setTravelTimeMatrix(DistanceMatrix travelTimeMatrix) {
        this.travelTimeMatrix = travelTimeMatrix;
        // if applicable, cache the index when distance matrix is set to avoid updating the cached value during multi-thread solving
        updateIndex(this.travelTimeMatrix);
    }

    public void setDistanceMatrix(DistanceMatrix distanceMatrix) {
        this.distanceMatrix = distanceMatrix;
        // if applicable, cache the index when distance matrix is set to avoid updating the cached value during multi-thread solving
        updateIndex(this.distanceMatrix);
    }

    public void setTravelTimeMatrices(DistanceMatrix[] travelTimesByTimeframe,
            ToIntFunction<OffsetDateTime> indexResolver) {
        this.travelTimesByTimeframe = travelTimesByTimeframe;
        this.timeframeIndexResolver = indexResolver;
    }

    public void setDistanceMatrices(DistanceMatrix[] distancesByTimeframe,
            ToIntFunction<OffsetDateTime> indexResolver) {
        this.distancesByTimeframe = distancesByTimeframe;
        this.timeframeIndexResolver = indexResolver;
    }

    /**
     * Returns the travel time for a route between this location and the given location.
     *
     * @param location the location representing the route destination
     * @return {@link TravelTime} instance representing the travel time in seconds and indicating if the destination is
     *         unreachable from this location.
     * @throws IllegalArgumentException When both locations are not included in the travel time matrix (either missing from the
     *         map or from the pre-configured location set).
     * @throws IllegalStateException When there is neither a single nor a per-timeframe travel time matrix configured for
     *         this location.
     */
    public TravelTime getTravelTimeTo(Location location) {
        DistanceMatrix matrix = travelTimeMatrix != null ? travelTimeMatrix : firstAvailableMatrix(travelTimesByTimeframe);
        if (matrix == null) {
            throw new IllegalStateException("No travel time matrix configured for a location (%s).".formatted(this));
        }
        long travelTimeFromMatrix = matrix.get(this, location);
        if (travelTimeFromMatrix == -1) {
            throw new IllegalArgumentException(("No travel time information found for a route from (%s) to (%s). " +
                    "Are both locations in the configured map and in the location set (if used)?").formatted(this, location));
        }
        return TravelTime.of(travelTimeFromMatrix);
    }

    /**
     * Returns the travel time for a route between this location and the given location at the given departure time.
     *
     * @param location the location representing the route destination
     * @param departureTime the instant used to select the traffic timeframe matrix
     * @return {@link TravelTime} instance representing the travel time in seconds and indicating if the destination is
     *         unreachable from this location.
     * @throws IllegalArgumentException When the resolved matrix does not include both locations, or the resolver returns
     *         an out-of-bounds index.
     * @throws IllegalStateException When there is neither a per-timeframe nor a single travel time matrix configured for
     *         this location.
     */
    public TravelTime getTravelTimeTo(Location location, OffsetDateTime departureTime) {
        DistanceMatrix matrix = hasTimeframeMatrices(travelTimesByTimeframe)
                ? resolveTimeframeMatrix(travelTimesByTimeframe, departureTime, "travel time")
                : travelTimeMatrix;
        if (matrix == null) {
            throw new IllegalStateException("No travel time matrix configured for a location (%s).".formatted(this));
        }
        long travelTime = matrix.get(this, location);
        if (travelTime == -1) {
            throw new IllegalArgumentException(
                    ("No travel time information found for a route from (%s) to (%s) at (%s).")
                            .formatted(this, location, departureTime));
        }
        return TravelTime.of(travelTime);
    }

    @Deprecated
    public TravelTime getDrivingTimeTo(Location location) {
        return getTravelTimeTo(location);
    }

    /**
     * Returns the travel distance for a route between this location and the given location.
     *
     * @param location the location representing the route destination
     * @return {@link TravelDistance} instance representing the travel distance in meters and indicating if the destination is
     *         unreachable from this location.
     * @throws IllegalArgumentException When both locations are not included in the travel distance matrix (either missing from
     *         the map or from the pre-configured location set).
     * @throws IllegalStateException When there is neither a single nor a per-timeframe distance matrix configured for this
     *         location.
     */
    public TravelDistance getDistanceTo(Location location) {
        DistanceMatrix matrix = distanceMatrix != null ? distanceMatrix : firstAvailableMatrix(distancesByTimeframe);
        if (matrix == null) {
            throw new IllegalStateException("No distance matrix configured for a location (%s).".formatted(this));
        }
        long travelDistanceFromMatrix = matrix.get(this, location);
        if (travelDistanceFromMatrix == -1) {
            throw new IllegalArgumentException(("No travel distance information found for a route from (%s) to (%s). " +
                    "Are both locations in the configured map and in the location set (if used)?").formatted(this, location));
        }
        return TravelDistance.of(travelDistanceFromMatrix);
    }

    /**
     * Returns the travel distance for a route between this location and the given location at the given departure time.
     *
     * @param location the location representing the route destination
     * @param departureTime the instant used to select the traffic timeframe matrix
     * @return {@link TravelDistance} instance representing the travel distance in meters and indicating if the destination is
     *         unreachable from this location.
     * @throws IllegalArgumentException When the resolved matrix does not include both locations, or the resolver returns
     *         an out-of-bounds index.
     * @throws IllegalStateException When there is neither a per-timeframe nor a single distance matrix configured for this
     *         location.
     */
    public TravelDistance getDistanceTo(Location location, OffsetDateTime departureTime) {
        DistanceMatrix matrix = hasTimeframeMatrices(distancesByTimeframe)
                ? resolveTimeframeMatrix(distancesByTimeframe, departureTime, "distance")
                : distanceMatrix;
        if (matrix == null) {
            throw new IllegalStateException("No distance matrix configured for a location (%s).".formatted(this));
        }
        long distance = matrix.get(this, location);
        if (distance == -1) {
            throw new IllegalArgumentException(("No distance information found for a route from (%s) to (%s) at (%s).")
                    .formatted(this, location, departureTime));
        }
        return TravelDistance.of(distance);
    }

    public short getIndex(DistanceMatrix matrix) {
        if (matrix == travelTimeMatrix) {
            return travelTimeMatrixIndex;
        } else if (matrix == distanceMatrix) {
            return distanceMatrixIndex;
        } else {
            return IndexableDistanceMatrix.EMPTY_INDEX;
        }
    }

    public void setIndex(DistanceMatrix matrix, short index) {
        if (matrix == travelTimeMatrix) {
            travelTimeMatrixIndex = index;
        } else if (matrix == distanceMatrix) {
            distanceMatrixIndex = index;
        }
    }

    private boolean hasTimeframeMatrices(DistanceMatrix[] matrices) {
        return matrices != null && timeframeIndexResolver != null;
    }

    private static DistanceMatrix firstAvailableMatrix(DistanceMatrix[] matrices) {
        if (matrices == null) {
            return null;
        }
        for (DistanceMatrix matrix : matrices) {
            if (matrix != null) {
                return matrix;
            }
        }
        return null;
    }

    private DistanceMatrix resolveTimeframeMatrix(DistanceMatrix[] matrices, OffsetDateTime departureTime, String what) {
        if (matrices == null || timeframeIndexResolver == null) {
            throw new IllegalStateException(
                    ("No traffic-aware %s matrices configured for a location (%s).").formatted(what, this));
        }
        int index = timeframeIndexResolver.applyAsInt(departureTime);
        if (index < 0 || index >= matrices.length) {
            throw new IllegalArgumentException(
                    ("Resolved timeframe index %d is out of bounds for %d %s matrix/matrices on location (%s) at (%s).")
                            .formatted(index, matrices.length, what, this, departureTime));
        }
        DistanceMatrix matrix = matrices[index];
        if (matrix == null) {
            throw new IllegalArgumentException(
                    ("No %s matrix fetched for timeframe index %d on location (%s) at (%s).")
                            .formatted(what, index, this, departureTime));
        }
        return matrix;
    }

    private void updateIndex(DistanceMatrix distanceMatrix) {
        if (distanceMatrix instanceof IndexableDistanceMatrix indexableDistanceMatrix) {
            indexableDistanceMatrix.updateCachedIndex(this);
        }
    }

    private void validateLatitude() {
        if (this.latitude < -90 || this.latitude > 90) {
            throw new IllegalStateException("Latitude must be between -90 and 90, found " + this.latitude);
        }
    }

    private void validateLongitude() {
        if (this.longitude < -180 || this.longitude > 180) {
            throw new IllegalStateException("Longitude must be between -180 and 180, found " + this.longitude);
        }
    }

    @Override
    public String toString() {
        return "(latitude=" + latitude + ", longitude=" + longitude + ")";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        Location location = (Location) object;
        return Double.compare(latitude, location.latitude) == 0 && Double.compare(longitude,
                location.longitude) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
