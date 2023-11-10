package ai.timefold.solver.examples.tsp.persistence;

import ai.timefold.solver.examples.common.business.SolutionBusiness;
import ai.timefold.solver.examples.common.persistence.AbstractTxtSolutionImporter;
import ai.timefold.solver.examples.common.persistence.SolutionConverter;
import ai.timefold.solver.examples.tsp.app.TspApp;
import ai.timefold.solver.examples.tsp.domain.Domicile;
import ai.timefold.solver.examples.tsp.domain.Standstill;
import ai.timefold.solver.examples.tsp.domain.TspSolution;
import ai.timefold.solver.examples.tsp.domain.Visit;
import ai.timefold.solver.examples.tsp.domain.location.AirLocation;
import ai.timefold.solver.examples.tsp.domain.location.DistanceType;
import ai.timefold.solver.examples.tsp.domain.location.Location;
import ai.timefold.solver.examples.tsp.domain.location.RoadLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TspImporter extends AbstractTxtSolutionImporter<TspSolution> {

    public static final String INPUT_FILE_SUFFIX = "tsp";

    public static void main(String[] args) {
        SolutionConverter<TspSolution> converter =
                SolutionConverter.createImportConverter(TspApp.DATA_DIR_NAME, new TspImporter(), new TspSolutionFileIO());
        converter.convert("other/air/europe40.tsp", "europe40.json");
        converter.convert("other/road-km/americanRoadTrip-road-km-n50.tsp", "americanRoadTrip-road-km-n50.json");
        converter.convert("cook/air/lu980.tsp", "lu980.json");
    }

    @Override
    public String getInputFileSuffix() {
        return INPUT_FILE_SUFFIX;
    }

    @Override
    public TxtInputBuilder<TspSolution> createTxtInputBuilder() {
        return new TspInputBuilder();
    }

    public static class TspInputBuilder extends TxtInputBuilder<TspSolution> {

        private TspSolution tspSolution;

        private int locationListSize;
        private boolean isMatrix;
        private CoordinateReader coordinateReader;

        @Override
        public TspSolution readSolution() throws IOException {
            tspSolution = new TspSolution(0);
            String firstLine = readStringValue();
            if (firstLine.matches("\\s*NAME\\s*:.*")) {
                tspSolution.setName(removePrefixSuffixFromLine(firstLine, "\\s*NAME\\s*:", ""));
                readTspLibFormat();
            } else {
                tspSolution.setName(SolutionBusiness.getBaseFileName(inputFile));
                locationListSize = Integer.parseInt(firstLine.trim());
                readCourseraFormat();
            }
            BigInteger possibleSolutionSize = factorial(tspSolution.getLocationList().size() - 1);
            logger.info("TspSolution {} has {} locations with a search space of {}.",
                    getInputId(),
                    tspSolution.getLocationList().size(),
                    getFlooredPossibleSolutionSize(possibleSolutionSize));
            return tspSolution;
        }

        // ************************************************************************
        // TSP TSPLIB format. See http://www.math.uwaterloo.ca/tsp/
        // ************************************************************************

        private void readTspLibFormat() throws IOException {
            readTspLibHeaders();
            if (isMatrix) {
                readTspLibMatrix();
            } else {
                readTspLibCityList();
            }
            createVisitList();
            readTspLibSolution();
            readOptionalConstantLine("EOF");
        }

        private void readTspLibHeaders() throws IOException {
            // Data format described here: http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/tsp95.pdf
            readUntilConstantLine("TYPE *: A?TSP.*");
            readOptionalConstantLine("COMMENT.*");
            locationListSize = readIntegerValue("DIMENSION *:");
            String edgeWeightType = readStringValue("EDGE_WEIGHT_TYPE *:").toUpperCase();
            switch (edgeWeightType) {
                case "ATT":
                    tspSolution.setDistanceType(DistanceType.ATT);
                    isMatrix = false;
                    coordinateReader = this::readTwoCoordinateLocations;
                    break;
                case "CEIL_2D":
                case "EUC_2D":
                    tspSolution.setDistanceType(DistanceType.AIR_DISTANCE);
                    isMatrix = false;
                    coordinateReader = this::readTwoCoordinateLocations;
                    break;
                case "GEO":
                    tspSolution.setDistanceType(DistanceType.GEO);
                    isMatrix = false;
                    coordinateReader = this::readTwoCoordinateLocations;
                    break;
                case "EXPLICIT":
                    tspSolution.setDistanceType(DistanceType.ROAD_DISTANCE);
                    isMatrix = true;
                    String edgeWeightFormat = readStringValue("EDGE_WEIGHT_FORMAT *:").toUpperCase();
                    coordinateReader = switch (edgeWeightFormat) {
                        case "FULL_MATRIX" -> this::readFullMatrixLocations;
                        case "UPPER_ROW" -> this::readUpperRowMatrixLocations;
                        case "UPPER_DIAG_ROW" -> this::readUpperDiagRowMatrixLocations;
                        default ->
                                throw new IllegalArgumentException("The edgeWeightFormat (" + edgeWeightFormat + ") is not supported.");
                    };
                    break;
                default:
                    throw new IllegalArgumentException("The edgeWeightType (" + edgeWeightType + ") is not supported.");
            }
            readOptionalConstantLine("DISPLAY_DATA_TYPE.*");
            tspSolution.setDistanceUnitOfMeasurement(readOptionalStringValue("EDGE_WEIGHT_UNIT_OF_MEASUREMENT *:", "distance"));
        }

        private void readTspLibCityList() throws IOException {
            readOptionalConstantLine("EDGE_WEIGHT_FORMAT.*");
            readOptionalConstantLine("DISPLAY_DATA_TYPE.*");
            readConstantLine("NODE_COORD_SECTION");
            if (coordinateReader == null) {
                throw new IllegalStateException("Read the headers first.");
            }
            DistanceType distanceType = tspSolution.getDistanceType();
            List<Location> locationList = coordinateReader.apply(bufferedReader, locationListSize);
            tspSolution.setLocationList(locationList);
            if (distanceType == DistanceType.ROAD_DISTANCE) {
                readConstantLine("EDGE_WEIGHT_SECTION");
                for (int i = 0; i < locationListSize; i++) {
                    RoadLocation location = (RoadLocation) locationList.get(i);
                    Map<RoadLocation, Double> travelDistanceMap = new LinkedHashMap<>(locationListSize);
                    String line = bufferedReader.readLine();
                    String[] lineTokens = splitBySpacesOrTabs(line.trim(), locationListSize);
                    for (int j = 0; j < locationListSize; j++) {
                        double travelDistance = Double.parseDouble(lineTokens[j]);
                        if (i == j) {
                            if (travelDistance != 0.0) {
                                throw new IllegalStateException("The travelDistance (" + travelDistance
                                        + ") should be zero.");
                            }
                        } else {
                            RoadLocation otherLocation = (RoadLocation) locationList.get(j);
                            travelDistanceMap.put(otherLocation, travelDistance);
                        }
                    }
                    location.setTravelDistanceMap(travelDistanceMap);
                }
            }
        }

        private void readTspLibMatrix() throws IOException {
            if (coordinateReader == null) {
                throw new IllegalStateException("Read the headers first.");
            }
            readConstantLine("EDGE_WEIGHT_SECTION");
            List<Location> locationList = coordinateReader.apply(bufferedReader, locationListSize);
            tspSolution.setLocationList(locationList);
        }

        private void createVisitList() {
            List<Location> locationList = tspSolution.getLocationList();
            List<Visit> visitList = new ArrayList<>(locationList.size() - 1);
            int count = 0;
            for (Location location : locationList) {
                if (count < 1) {
                    Domicile domicile = new Domicile(location.getId(), location);
                    tspSolution.setDomicile(domicile);
                } else {
                    Visit visit = new Visit(location.getId(), location);
                    // Notice that we leave the PlanningVariable properties on null
                    visitList.add(visit);
                }
                count++;
            }
            tspSolution.setVisitList(visitList);
        }

        private void readTspLibSolution() throws IOException {
            boolean enabled = readOptionalConstantLine("TOUR_SECTION");
            if (!enabled) {
                return;
            }
            long domicileId = readLongValue();
            Domicile domicile = tspSolution.getDomicile();
            if (domicile.getId() != domicileId) {
                throw new IllegalStateException("The domicileId (" + domicileId
                        + ") is not the domicile's id (" + domicile.getId() + ").");
            }
            int visitListSize = tspSolution.getVisitList().size();
            Map<Long, Visit> idToVisitMap = new HashMap<>(visitListSize);
            for (Visit visit : tspSolution.getVisitList()) {
                idToVisitMap.put(visit.getId(), visit);
            }
            Standstill previousStandstill = domicile;
            for (int i = 0; i < visitListSize; i++) {
                long visitId = readLongValue();
                Visit visit = idToVisitMap.get(visitId);
                if (visit == null) {
                    throw new IllegalStateException("The visitId (" + visitId
                            + ") is does not exist.");
                }
                visit.setPreviousStandstill(previousStandstill);
                previousStandstill = visit;
            }
        }

        // ************************************************************************
        // TSP coursera format. See https://class.coursera.org/optimization-001/
        // ************************************************************************

        private void readCourseraFormat() throws IOException {
            List<Location> locationList = new ArrayList<>(locationListSize);
            long id = 0;
            for (int i = 0; i < locationListSize; i++) {
                String line = bufferedReader.readLine();
                String[] lineTokens = splitBySpace(line, 2);
                Location location = new AirLocation(id, Double.parseDouble(lineTokens[0]), Double.parseDouble(lineTokens[1]));
                locationList.add(location);
            }
            tspSolution.setLocationList(locationList);
            createVisitList();
        }

        private List<Location> readTwoCoordinateLocations(BufferedReader bufferedReader, int locationListSize) throws IOException {
            List<Location> locationList = new ArrayList<>(locationListSize);
            for (int i = 0; i < locationListSize; i++) {
                String line = bufferedReader.readLine().trim();
                String[] lineTokens = splitBySpace(line, 3, 3, true, true);
                long id = Long.parseLong(lineTokens[0]);
                double x = Double.parseDouble(lineTokens[1]);
                double y = Double.parseDouble(lineTokens[2]);
                Location location = tspSolution.getDistanceType().createLocation(id, x, y);
                locationList.add(location);
            }
            return locationList;
        }

        private List<Location> readFullMatrixLocations(BufferedReader bufferedReader, int locationListSize) throws IOException {
            Map<LocationPair, Double> distanceMap = new HashMap<>();
            String[][] lineTokens = readFullMatrix(bufferedReader, locationListSize);
            for (int locationA = 0; locationA < locationListSize; locationA++) {
                for (int locationB = 0; locationB < locationListSize; locationB++) {
                    distanceMap.put(new LocationPair(locationA, locationB), Double.parseDouble(lineTokens[locationA][locationB]));
                }
            }
            List<RoadLocation> locationList = new ArrayList<>(locationListSize);
            for (int i = 0; i < locationListSize; i++) {
                RoadLocation roadLocation = new RoadLocation(i);
                locationList.add(roadLocation);
            }
            for (int i = 0; i < locationListSize; i++) {
                Map<RoadLocation, Double> distanceMatrix = new LinkedHashMap<>();
                RoadLocation roadLocation = locationList.get(i);
                distanceMap.forEach((locationPair, distance) -> {
                    if (locationPair.locationA == roadLocation.getId()) {
                        RoadLocation otherLocation = locationList.get((int) locationPair.locationB);
                        distanceMatrix.put(otherLocation, distance);
                    }
                });
                roadLocation.setTravelDistanceMap(distanceMatrix);
            }
            return (List) locationList;
        }

        private String[][] readFullMatrix(BufferedReader bufferedReader, int expectedLocations) throws IOException {
            List<String> tokens = new ArrayList<>(expectedLocations * expectedLocations);
            int unreadTokens = expectedLocations * expectedLocations;
            while (unreadTokens > 0) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    throw new IllegalStateException("Unprocessed tokens: " + unreadTokens);
                } else if (line.trim().isEmpty()) {
                    continue;
                }
                String[] lineTokens = splitBySpace(line, 1, unreadTokens, true, true);
                tokens.addAll(Arrays.asList(lineTokens));
                unreadTokens -= lineTokens.length;
            }
            // Split array into chunks of expectedLocations
            String[][] tokenArray = new String[expectedLocations][expectedLocations];
            for (int i = 0; i < expectedLocations; i++) {
                tokenArray[i] = tokens.subList(i * expectedLocations, (i + 1) * expectedLocations).toArray(new String[0]);
            }
            return tokenArray;
        }

        private List<Location> readUpperRowMatrixLocations(BufferedReader bufferedReader, int locationListSize) throws IOException {
            Map<LocationPair, Double> distanceMap = new HashMap<>();
            String[][] lineTokens = readUpperRowMatrix(bufferedReader, locationListSize);
            for (int locationA = 0; locationA < locationListSize - 1; locationA++) {
                int processedLocationsAlready = locationA + 1;
                int expectedTokenCount = locationListSize - processedLocationsAlready;
                for (int locationB = 0; locationB < expectedTokenCount; locationB++) {
                    int actualLocationB = processedLocationsAlready + locationB;
                    distanceMap.put(new LocationPair(locationA, actualLocationB), Double.parseDouble(lineTokens[locationA][locationB]));
                }
            }
            List<RoadLocation> locationList = new ArrayList<>(locationListSize);
            for (int i = 0; i < locationListSize; i++) {
                RoadLocation roadLocation = new RoadLocation(i);
                roadLocation.setTravelDistanceMap(new LinkedHashMap<>());
                locationList.add(roadLocation);
            }
            for (int i = 0; i < locationListSize; i++) {
                RoadLocation roadLocation = locationList.get(i);
                distanceMap.forEach((locationPair, distance) -> {
                    if (locationPair.locationA == roadLocation.getId()) {
                        RoadLocation otherLocation = locationList.get((int) locationPair.locationB);
                        roadLocation.getTravelDistanceMap().put(otherLocation, distance);
                        otherLocation.getTravelDistanceMap().put(roadLocation, distance);
                    }
                });
            }
            return (List) locationList;
        }

        private String[][] readUpperRowMatrix(BufferedReader bufferedReader, int expectedLocations) throws IOException {
            List<String> tokens = new ArrayList<>(expectedLocations * expectedLocations);
            int n = expectedLocations - 1;
            int unreadTokens = (n * (n + 1)) / 2;
            while (unreadTokens > 0) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    throw new IllegalStateException("Unprocessed tokens: " + unreadTokens);
                } else if (line.trim().isEmpty()) {
                    continue;
                }
                String[] lineTokens = splitBySpace(line, 1, unreadTokens, true, true);
                tokens.addAll(Arrays.asList(lineTokens));
                unreadTokens -= lineTokens.length;
            }
            // Split array into chunks of expectedLocations
            String[][] tokenArray = new String[expectedLocations][];
            List<String> unprocessedTokens = tokens;
            for (int i = 0; i < expectedLocations; i++) {
                int expectedTokenCount = expectedLocations - i - 1;
                tokenArray[i] = unprocessedTokens.subList(0, expectedTokenCount).toArray(new String[0]);
                unprocessedTokens = unprocessedTokens.subList(expectedTokenCount, unprocessedTokens.size());
            }
            if (!unprocessedTokens.isEmpty()) {
                throw new IllegalStateException("Not all tokens processed: " + unprocessedTokens);
            }
            return tokenArray;
        }

        private List<Location> readUpperDiagRowMatrixLocations(BufferedReader bufferedReader, int locationListSize) throws IOException {
            Map<LocationPair, Double> distanceMap = new HashMap<>();
            String[][] lineTokens = readUpperDiagRowMatrix(bufferedReader, locationListSize);
            for (int locationA = 0; locationA < locationListSize - 1; locationA++) {
                int processedLocationsAlready = locationA + 1;
                int expectedTokenCount = locationListSize - processedLocationsAlready;
                for (int locationB = 0; locationB < expectedTokenCount; locationB++) {
                    int actualLocationB = processedLocationsAlready + locationB;
                    distanceMap.put(new LocationPair(locationA, actualLocationB), Double.parseDouble(lineTokens[locationA][locationB]));
                }
            }
            List<RoadLocation> locationList = new ArrayList<>(locationListSize);
            for (int i = 0; i < locationListSize; i++) {
                RoadLocation roadLocation = new RoadLocation(i);
                roadLocation.setTravelDistanceMap(new LinkedHashMap<>());
                locationList.add(roadLocation);
            }
            for (int i = 0; i < locationListSize; i++) {
                RoadLocation roadLocation = locationList.get(i);
                distanceMap.forEach((locationPair, distance) -> {
                    if (locationPair.locationA == roadLocation.getId()) {
                        RoadLocation otherLocation = locationList.get((int) locationPair.locationB);
                        roadLocation.getTravelDistanceMap().put(otherLocation, distance);
                        otherLocation.getTravelDistanceMap().put(roadLocation, distance);
                    }
                });
            }
            return (List) locationList;
        }

        private String[][] readUpperDiagRowMatrix(BufferedReader bufferedReader, int expectedLocations) throws IOException {
            List<String> tokens = new ArrayList<>(expectedLocations * expectedLocations);
            int unreadTokens = (int) Math.round(expectedLocations * ((expectedLocations + 1)/2.0));
            while (unreadTokens > 0) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    throw new IllegalStateException("Unprocessed tokens: " + unreadTokens);
                } else if (line.trim().isEmpty()) {
                    continue;
                }
                String[] lineTokens = splitBySpace(line, 1, unreadTokens, true, true);
                tokens.addAll(Arrays.asList(lineTokens));
                unreadTokens -= lineTokens.length;
            }
            // Split array into chunks of expectedLocations
            String[][] tokenArray = new String[expectedLocations][];
            List<String> unprocessedTokens = tokens;
            int expectedTokenCount = expectedLocations;
            for (int i = 0; i < expectedLocations; i++) {
                tokenArray[i] = unprocessedTokens.subList(0, expectedTokenCount).toArray(new String[0]);
                unprocessedTokens = unprocessedTokens.subList(expectedTokenCount, unprocessedTokens.size());
                expectedTokenCount = expectedTokenCount - 1;
            }
            if (!unprocessedTokens.isEmpty()) {
                throw new IllegalStateException("Not all tokens processed: " + unprocessedTokens);
            }
            return tokenArray;
        }

        private record LocationPair(long locationA, long locationB) {

        }
    }

    @FunctionalInterface
    private interface CoordinateReader {

        List<Location> apply(BufferedReader bufferedReader, int locationListSize) throws IOException;

    }

}
