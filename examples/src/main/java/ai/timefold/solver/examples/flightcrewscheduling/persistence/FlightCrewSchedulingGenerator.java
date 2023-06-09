package ai.timefold.solver.examples.flightcrewscheduling.persistence;

import static java.time.temporal.ChronoUnit.DAYS;

import java.io.File;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.LoggingMain;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.common.persistence.generator.LocationDataGenerator;
import ai.timefold.solver.examples.common.persistence.generator.StringDataGenerator;
import ai.timefold.solver.examples.flightcrewscheduling.app.FlightCrewSchedulingApp;
import ai.timefold.solver.examples.flightcrewscheduling.domain.Airport;
import ai.timefold.solver.examples.flightcrewscheduling.domain.Employee;
import ai.timefold.solver.examples.flightcrewscheduling.domain.Flight;
import ai.timefold.solver.examples.flightcrewscheduling.domain.FlightAssignment;
import ai.timefold.solver.examples.flightcrewscheduling.domain.FlightCrewParametrization;
import ai.timefold.solver.examples.flightcrewscheduling.domain.FlightCrewSolution;
import ai.timefold.solver.examples.flightcrewscheduling.domain.Skill;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class FlightCrewSchedulingGenerator extends LoggingMain {

    private static final double TAXI_KM_THRESHOLD = 500.0;
    private static final double TAXI_SPEED_IN_KM_PER_MINUTE = 90.0 / 60.0;
    private static final double PLANE_SPEED_IN_KM_PER_MINUTE = 1000.0 / 60.0;
    private static final double PLANE_TAKE_OFF_AND_LANDING_MINUTES = 90.0;

    private static final int START_MINUTE_OF_DAY = 6 * 60;
    private static final int END_MINUTE_OF_DAY = 22 * 60;

    private static final int PILOT_COUNT_PER_FLIGHT = 2;
    private static final int FLIGHT_ATTENDANT_COUNT_PER_FLIGHT = 3;
    private static final int EMPLOYEE_COUNT_PER_FLIGHT = PILOT_COUNT_PER_FLIGHT + FLIGHT_ATTENDANT_COUNT_PER_FLIGHT;

    public static void main(String[] args) {
        FlightCrewSchedulingGenerator generator = new FlightCrewSchedulingGenerator();
        generator.writeFlightCrewSolution("Europe", LocationDataGenerator.EUROPE_BUSIEST_AIRPORTS, 10, 7);
        generator.writeFlightCrewSolution("Europe", LocationDataGenerator.EUROPE_BUSIEST_AIRPORTS, 10, 4 * 7);
        generator.writeFlightCrewSolution("Europe", LocationDataGenerator.EUROPE_BUSIEST_AIRPORTS, 50, 7);
        generator.writeFlightCrewSolution("US", LocationDataGenerator.US_MAINLAND_STATE_CAPITALS, 10, 7);
    }

    private final StringDataGenerator employeeNameGenerator = StringDataGenerator.buildFullNames();

    protected final SolutionFileIO<FlightCrewSolution> solutionFileIO;
    protected final File outputDir;

    protected Skill pilotSkill;
    protected Skill flightAttendantSkill;
    protected List<Airport> homeAirportList;

    protected Random random;

    public FlightCrewSchedulingGenerator() {
        solutionFileIO = new FlightCrewSchedulingXlsxFileIO();
        outputDir = new File(CommonApp.determineDataDir(FlightCrewSchedulingApp.DATA_DIR_NAME), "unsolved");
    }

    private void writeFlightCrewSolution(String locationDataName,
            List<LocationDataGenerator.LocationData> locationDataArray, int flightRoundTripsPerDay, int dayCount) {
        int flightListSize = (flightRoundTripsPerDay * 5 / 2) * dayCount;
        String fileName = flightListSize + "flights-" + dayCount + "days-" + locationDataName;
        File outputFile = new File(outputDir, fileName + "." + solutionFileIO.getOutputFileExtension());
        FlightCrewSolution solution = createFlightCrewSolution(
                fileName, locationDataArray, flightRoundTripsPerDay, dayCount);
        solutionFileIO.write(solution, outputFile);
    }

    public FlightCrewSolution createFlightCrewSolution(String fileName,
            List<LocationDataGenerator.LocationData> locationDataList, int flightRoundTripsPerDay, int dayCount) {
        random = new Random(37);
        FlightCrewSolution solution = new FlightCrewSolution(0L);
        LocalDate firstDate = LocalDate.of(2018, 1, 1);
        solution.setScheduleFirstUTCDate(firstDate);
        solution.setScheduleLastUTCDate(firstDate.plusDays(dayCount - 1));
        FlightCrewParametrization parametrization = new FlightCrewParametrization(0L);
        solution.setParametrization(parametrization);

        createSkillList(solution);
        createAirportList(solution, locationDataList);
        createFlightList(solution, flightRoundTripsPerDay, dayCount);
        createFlightAssignmentList(solution);
        createEmployeeList(solution, flightRoundTripsPerDay, dayCount);

        int employeeListSize = solution.getEmployeeList().size();
        int flightAssignmentListSize = solution.getFlightAssignmentList().size();
        BigInteger possibleSolutionSize = BigInteger.valueOf(employeeListSize).pow(flightAssignmentListSize);
        logger.info(
                "FlightCrew {} has {} skills, {} airports, {} employees, {} flights and {} flight assignments with a search space of {}.",
                fileName,
                solution.getSkillList().size(),
                solution.getAirportList().size(),
                solution.getEmployeeList().size(),
                solution.getFlightList().size(),
                flightAssignmentListSize,
                AbstractSolutionImporter.getFlooredPossibleSolutionSize(possibleSolutionSize));
        return solution;
    }

    private void createSkillList(FlightCrewSolution solution) {
        List<Skill> skillList = new ArrayList<>(2);
        pilotSkill = new Skill(0L, "Pilot");
        skillList.add(pilotSkill);
        flightAttendantSkill = new Skill(1L, "Flight attendant");
        skillList.add(flightAttendantSkill);
        solution.setSkillList(skillList);
    }

    private void createAirportList(FlightCrewSolution solution,
            List<LocationDataGenerator.LocationData> locationDataList) {
        List<Airport> airportList = new ArrayList<>(locationDataList.size());
        long id = 0L;
        for (LocationDataGenerator.LocationData locationData : locationDataList) {
            Airport airport = new Airport(id, locationData.getName().replaceAll("\\,.*", ""),
                    locationData.getName(), locationData.getLatitude(), locationData.getLongitude());
            logger.trace("Created airport ({}).", airport);
            airportList.add(airport);
            id++;
        }
        for (Airport a : airportList) {
            Map<Airport, Long> taxiTimeInMinutesMap = new LinkedHashMap<>(airportList.size());
            for (Airport b : airportList) {
                double distanceInKm = a.getHaversineDistanceInKmTo(b);
                if (distanceInKm < TAXI_KM_THRESHOLD) {
                    taxiTimeInMinutesMap.put(b, (long) (distanceInKm / TAXI_SPEED_IN_KM_PER_MINUTE));
                }
            }
            a.setTaxiTimeInMinutesMap(taxiTimeInMinutesMap);
        }
        solution.setAirportList(airportList);
    }

    private void createFlightList(FlightCrewSolution solution, int flightRoundTripsPerDay, int dayCount) {
        int flightListSize = flightRoundTripsPerDay * dayCount;
        List<Flight> flightList = new ArrayList<>(flightListSize);
        List<Airport> airportList = solution.getAirportList();
        Airport centerAirport = airportList.get(0);
        int homeAirportListSize = Math.min(airportList.size() / 10, flightRoundTripsPerDay / 5);
        homeAirportList = airportList.stream()
                .sorted(Comparator.comparingDouble(centerAirport::getHaversineDistanceInKmTo))
                .limit(homeAirportListSize)
                .collect(Collectors.toList());
        LocalDate firstDate = solution.getScheduleFirstUTCDate();
        LocalDate lastDate = solution.getScheduleLastUTCDate();
        int flightNumberSuffix = 1;
        long flightId = 0L;
        for (int i = 0; i < flightRoundTripsPerDay; i++) {
            int flightCount = i % 2 == 0 ? 2 : 3;
            ArrayList<Airport> selectedAirports = new ArrayList<>(flightCount);
            Airport firstAirport = homeAirportList.get(random.nextInt(homeAirportList.size()));
            selectedAirports.add(firstAirport);
            ArrayList<Airport> nonFirstAirports = new ArrayList<>(airportList);
            nonFirstAirports.remove(firstAirport);
            Collections.shuffle(nonFirstAirports, random);
            selectedAirports.addAll(nonFirstAirports.subList(0, flightCount - 1));
            for (int j = 0; j < flightCount; j++) {
                String flightNumber = "AB" + String.format("%03d", flightNumberSuffix);
                flightNumberSuffix++;
                Airport departureAirport = selectedAirports.get(j);
                Airport arrivalAirport = selectedAirports.get((j + 1) % flightCount);
                int flyingTime = (int) ((departureAirport.getHaversineDistanceInKmTo(arrivalAirport)
                        / PLANE_SPEED_IN_KM_PER_MINUTE) + PLANE_TAKE_OFF_AND_LANDING_MINUTES);
                int departureMinute = START_MINUTE_OF_DAY + random.nextInt(
                        END_MINUTE_OF_DAY - flyingTime - START_MINUTE_OF_DAY + 1);
                int arrivalMinute = departureMinute + flyingTime;
                for (LocalDate date = firstDate; date.compareTo(lastDate) <= 0; date = date.plusDays(1)) {
                    Flight flight = new Flight(flightId, flightNumber, departureAirport,
                            date.atTime(departureMinute / 60, departureMinute % 60), arrivalAirport,
                            date.atTime(arrivalMinute / 60, arrivalMinute % 60));
                    logger.trace("Created flight ({}).", flight);
                    flightList.add(flight);
                    flightId++;
                }
            }
        }
        flightList.sort(Flight::compareTo);
        solution.setFlightList(flightList);
    }

    private void createFlightAssignmentList(FlightCrewSolution solution) {
        List<Flight> flightList = solution.getFlightList();
        List<FlightAssignment> flightAssignmentList = new ArrayList<>(flightList.size() * EMPLOYEE_COUNT_PER_FLIGHT);
        long flightAssignmentId = 0L;
        for (Flight flight : flightList) {
            for (int indexInFlight = 0; indexInFlight < EMPLOYEE_COUNT_PER_FLIGHT; indexInFlight++) {
                Skill requiredSkill = indexInFlight < PILOT_COUNT_PER_FLIGHT ? pilotSkill : flightAttendantSkill;
                FlightAssignment flightAssignment =
                        new FlightAssignment(flightAssignmentId, flight, indexInFlight, requiredSkill);
                flightAssignmentList.add(flightAssignment);
                flightAssignmentId++;
            }
        }
        solution.setFlightAssignmentList(flightAssignmentList);
    }

    private void createEmployeeList(FlightCrewSolution solution, int flightRoundTripsPerDay, int dayCount) {
        int employeeListSize = flightRoundTripsPerDay * EMPLOYEE_COUNT_PER_FLIGHT * 3;
        List<Employee> employeeList = new ArrayList<>(employeeListSize);
        employeeNameGenerator.predictMaximumSizeAndReset(employeeListSize);
        LocalDate firstDate = solution.getScheduleFirstUTCDate();
        LocalDate lastDate = solution.getScheduleLastUTCDate();
        List<LocalDate> allDateList = new ArrayList<>((int) DAYS.between(firstDate, lastDate) + 1);
        for (LocalDate date = firstDate; date.compareTo(lastDate) <= 0; date = date.plusDays(1)) {
            allDateList.add(date);
        }
        List<LocalDate> unavailableDayPool = new ArrayList<>(allDateList);
        Collections.shuffle(unavailableDayPool, random);
        long id = 0L;
        for (int i = 0; i < employeeListSize; i++) {
            Employee employee =
                    new Employee(id, employeeNameGenerator.generateNextValue(),
                            homeAirportList.get(random.nextInt(homeAirportList.size())));
            employee.setSkillSet(Collections.singleton((i % 5) < 2 ? pilotSkill : flightAttendantSkill));
            int unavailableDayCount = 0;
            for (int j = 0; j < dayCount && unavailableDayCount < dayCount; j++) {
                if (random.nextDouble() < (20.0 / 365.0)) {
                    unavailableDayCount++;
                }
            }
            employee.setUnavailableDaySet(generateUnavailableDaySet(unavailableDayCount, allDateList, unavailableDayPool));
            employee.setFlightAssignmentSet(new TreeSet<>());
            logger.trace("Created employee ({}).", employee);
            employeeList.add(employee);
            id++;
        }
        solution.setEmployeeList(employeeList);

    }

    private Set<LocalDate> generateUnavailableDaySet(int size, List<LocalDate> allDateList, List<LocalDate> pool) {
        Set<LocalDate> unavailableDaySet = new LinkedHashSet<>(size);
        if (pool.size() < size) {
            // There are not enough left in the pool, so provision more
            unavailableDaySet.addAll(pool);
            pool.clear();
            pool.addAll(allDateList);
            Collections.shuffle(pool, random);
            // Add those that haven't been added yet
            for (Iterator<LocalDate> it = pool.iterator(); it.hasNext() && unavailableDaySet.size() < size;) {
                LocalDate date = it.next();
                if (unavailableDaySet.add(date)) {
                    it.remove();
                }
            }
        } else {
            // There are enough left in the pool
            List<LocalDate> selection = pool.subList(0, size);
            unavailableDaySet.addAll(selection);
            selection.clear();
        }
        return unavailableDaySet;
    }
}
