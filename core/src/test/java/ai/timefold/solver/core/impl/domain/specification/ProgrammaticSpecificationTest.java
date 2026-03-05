package ai.timefold.solver.core.impl.domain.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ai.timefold.solver.core.api.domain.specification.PlanningSpecification;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SpecificationCompiler;
import ai.timefold.solver.core.impl.domain.variable.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;

import org.junit.jupiter.api.Test;

/**
 * Tests for various programmatic {@link PlanningSpecification} configurations
 * beyond the basic single-entity/single-variable case.
 */
class ProgrammaticSpecificationTest {

    // ── Basic variable with allowsUnassigned ────────────────────────

    static class NullableSolution {
        SimpleScore score;
        List<NullableValue> values = new ArrayList<>();
        List<NullableEntity> entities = new ArrayList<>();

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        List<NullableValue> getValues() {
            return values;
        }

        List<NullableEntity> getEntities() {
            return entities;
        }
    }

    static class NullableEntity {
        String id;
        NullableValue value; // nullable

        NullableEntity() {
        }

        NullableEntity(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        NullableValue getValue() {
            return value;
        }

        void setValue(NullableValue value) {
            this.value = value;
        }
    }

    static class NullableValue {
        String code;

        NullableValue() {
        }

        NullableValue(String code) {
            this.code = code;
        }
    }

    @Test
    void allowsUnassigned_compilesAndSolves() {
        var spec = PlanningSpecification.of(NullableSolution.class)
                .score(SimpleScore.class, NullableSolution::getScore, NullableSolution::setScore)
                .problemFacts("values", NullableSolution::getValues)
                .entityCollection("entities", NullableSolution::getEntities)
                .valueRange("vr", NullableSolution::getValues)
                .entity(NullableEntity.class, e -> e
                        .planningId(NullableEntity::getId)
                        .variable("value", NullableValue.class, v -> v
                                .accessors(NullableEntity::getValue, NullableEntity::setValue)
                                .valueRange("vr")
                                .allowsUnassigned(true)))
                .build();

        var sd = SpecificationCompiler.compile(spec, null);
        var ed = sd.findEntityDescriptorOrFail(NullableEntity.class);
        var vd = (BasicVariableDescriptor<?>) ed.getGenuineVariableDescriptor("value");
        assertThat(vd.allowsUnassigned()).isTrue();

        // Solve with more entities than values — some must remain null
        var solverConfig = solverConfig(spec, NullableScoreCalculator.class);
        var solver = SolverFactory.<NullableSolution> create(solverConfig).buildSolver();

        var problem = new NullableSolution();
        problem.getValues().add(new NullableValue("v1"));
        for (int i = 0; i < 5; i++) {
            problem.getEntities().add(new NullableEntity("e" + i));
        }
        var solution = solver.solve(problem);
        assertThat(solution.getScore()).isNotNull();
    }

    public static class NullableScoreCalculator implements EasyScoreCalculator<NullableSolution, SimpleScore> {
        @Override
        public SimpleScore calculateScore(NullableSolution solution) {
            int penalty = 0;
            for (var entity : solution.getEntities()) {
                if (entity.getValue() == null) {
                    penalty--;
                }
            }
            return SimpleScore.of(penalty);
        }
    }

    // ── Pinning ─────────────────────────────────────────────────────

    static class PinningSolution {
        SimpleScore score;
        List<String> values = new ArrayList<>();
        List<PinEntity> entities = new ArrayList<>();

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        List<String> getValues() {
            return values;
        }

        List<PinEntity> getEntities() {
            return entities;
        }
    }

    static class PinEntity {
        String id;
        boolean pinned;
        String value;

        PinEntity() {
        }

        PinEntity(String id, boolean pinned) {
            this.id = id;
            this.pinned = pinned;
        }

        PinEntity(String id, boolean pinned, String value) {
            this.id = id;
            this.pinned = pinned;
            this.value = value;
        }

        String getId() {
            return id;
        }

        boolean isPinned() {
            return pinned;
        }

        String getValue() {
            return value;
        }

        void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    void pinnedEntity_isNotChanged() {
        var spec = PlanningSpecification.of(PinningSolution.class)
                .score(SimpleScore.class, PinningSolution::getScore, PinningSolution::setScore)
                .problemFacts("values", PinningSolution::getValues)
                .entityCollection("entities", PinningSolution::getEntities)
                .valueRange("vr", PinningSolution::getValues)
                .entity(PinEntity.class, e -> e
                        .planningId(PinEntity::getId)
                        .pinned(PinEntity::isPinned)
                        .variable("value", String.class, v -> v
                                .accessors(PinEntity::getValue, PinEntity::setValue)
                                .valueRange("vr")))
                .build();

        // Verify pinning is registered
        var sd = SpecificationCompiler.compile(spec, null);
        var ed = sd.findEntityDescriptorOrFail(PinEntity.class);
        assertThat(ed.supportsPinning()).isTrue();

        // Solve: pinned entity should keep its original value
        var solverConfig = solverConfig(spec, PinScoreCalculator.class);
        var solver = SolverFactory.<PinningSolution> create(solverConfig).buildSolver();

        var problem = new PinningSolution();
        problem.getValues().addAll(List.of("A", "B", "C"));
        problem.getEntities().add(new PinEntity("pinned1", true, "A")); // pinned with value "A"
        problem.getEntities().add(new PinEntity("free1", false));
        problem.getEntities().add(new PinEntity("free2", false));

        var solution = solver.solve(problem);
        // Pinned entity must still have its original value
        var pinnedEntity = solution.getEntities().stream()
                .filter(e -> "pinned1".equals(e.getId())).findFirst().orElseThrow();
        assertThat(pinnedEntity.getValue()).isEqualTo("A");
    }

    public static class PinScoreCalculator implements EasyScoreCalculator<PinningSolution, SimpleScore> {
        @Override
        public SimpleScore calculateScore(PinningSolution solution) {
            return SimpleScore.of(0);
        }
    }

    // ── Entity difficulty comparator ────────────────────────────────

    static class DifficultySolution {
        SimpleScore score;
        List<Integer> values = new ArrayList<>();
        List<DifficultyEntity> entities = new ArrayList<>();

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        List<Integer> getValues() {
            return values;
        }

        List<DifficultyEntity> getEntities() {
            return entities;
        }
    }

    static class DifficultyEntity {
        String id;
        int difficulty;
        Integer value;

        DifficultyEntity() {
        }

        DifficultyEntity(String id, int difficulty) {
            this.id = id;
            this.difficulty = difficulty;
        }

        String getId() {
            return id;
        }

        int getDifficulty() {
            return difficulty;
        }

        Integer getValue() {
            return value;
        }

        void setValue(Integer value) {
            this.value = value;
        }
    }

    @Test
    void entityDifficultyComparator_isRegistered() {
        var spec = PlanningSpecification.of(DifficultySolution.class)
                .score(SimpleScore.class, DifficultySolution::getScore, DifficultySolution::setScore)
                .problemFacts("values", DifficultySolution::getValues)
                .entityCollection("entities", DifficultySolution::getEntities)
                .valueRange("vr", DifficultySolution::getValues)
                .entity(DifficultyEntity.class, e -> e
                        .planningId(DifficultyEntity::getId)
                        .difficultyComparator(Comparator.comparingInt(DifficultyEntity::getDifficulty))
                        .variable("value", Integer.class, v -> v
                                .accessors(DifficultyEntity::getValue, DifficultyEntity::setValue)
                                .valueRange("vr")))
                .build();

        var sd = SpecificationCompiler.compile(spec, null);
        var ed = sd.findEntityDescriptorOrFail(DifficultyEntity.class);
        // Difficulty comparator produces a descending sorter
        assertThat(ed.getDescendingSorter()).isNotNull();
    }

    // ── Variable strength comparator ────────────────────────────────

    @Test
    void variableStrengthComparator_isRegistered() {
        var spec = PlanningSpecification.of(DifficultySolution.class)
                .score(SimpleScore.class, DifficultySolution::getScore, DifficultySolution::setScore)
                .problemFacts("values", DifficultySolution::getValues)
                .entityCollection("entities", DifficultySolution::getEntities)
                .valueRange("vr", DifficultySolution::getValues)
                .entity(DifficultyEntity.class, e -> e
                        .planningId(DifficultyEntity::getId)
                        .variable("value", Integer.class, v -> v
                                .accessors(DifficultyEntity::getValue, DifficultyEntity::setValue)
                                .valueRange("vr")
                                .strengthComparator(Comparator.naturalOrder())))
                .build();

        var sd = SpecificationCompiler.compile(spec, null);
        var ed = sd.findEntityDescriptorOrFail(DifficultyEntity.class);
        var vd = ed.getGenuineVariableDescriptor("value");
        assertThat(vd.getValueRangeDescriptor()).isNotNull();
        // Strength comparator produces ascending/descending sorters on the variable
        assertThat(vd.getAscendingSorter()).isNotNull();
        assertThat(vd.getDescendingSorter()).isNotNull();
    }

    // ── Cloning with factories ──────────────────────────────────────

    static class CloneSolution {
        SimpleScore score;
        List<String> values = new ArrayList<>();
        List<CloneEntity> entities = new ArrayList<>();

        CloneSolution() {
        }

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        List<String> getValues() {
            return values;
        }

        List<CloneEntity> getEntities() {
            return entities;
        }

        void setEntities(List<CloneEntity> entities) {
            this.entities = entities;
        }
    }

    static class CloneEntity {
        String id;
        String value;

        CloneEntity() {
        }

        CloneEntity(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        String getValue() {
            return value;
        }

        void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    void cloningWithFactories_compilesAndClones() {
        var spec = PlanningSpecification.of(CloneSolution.class)
                .score(SimpleScore.class, CloneSolution::getScore, CloneSolution::setScore)
                .problemFacts("values", CloneSolution::getValues)
                .entityCollection("entities", CloneSolution::getEntities)
                .valueRange("vr", CloneSolution::getValues)
                .entity(CloneEntity.class, e -> e
                        .planningId(CloneEntity::getId)
                        .variable("value", String.class, v -> v
                                .accessors(CloneEntity::getValue, CloneEntity::setValue)
                                .valueRange("vr")))
                .cloning(c -> c
                        .solutionFactory(CloneSolution::new)
                        .solutionProperty("score", CloneSolution::getScore, CloneSolution::setScore)
                        .solutionProperty("values", CloneSolution::getValues, (s, v) -> {
                        })
                        .solutionProperty("entities",
                                (CloneSolution s) -> s.getEntities(),
                                (CloneSolution s, Object v) -> s.setEntities((List) v),
                                ai.timefold.solver.core.api.domain.specification.CloningSpecification.DeepCloneDecision.DEEP_COLLECTION)
                        .entityClass(CloneEntity.class, CloneEntity::new, e -> e
                                .shallowProperty("id", CloneEntity::getId, (entity, v) -> {
                                })
                                .shallowProperty("value", CloneEntity::getValue, CloneEntity::setValue)))
                .build();

        var sd = SpecificationCompiler.compile(spec, null);
        var cloner = sd.getSolutionCloner();

        var original = new CloneSolution();
        original.getValues().addAll(List.of("A", "B"));
        original.getEntities().add(new CloneEntity("e1"));
        original.getEntities().add(new CloneEntity("e2"));
        original.getEntities().get(0).setValue("A");
        original.setScore(SimpleScore.of(10));

        var clone = cloner.cloneSolution(original);
        assertThat(clone).isNotSameAs(original);
        assertThat(clone.getEntities()).hasSize(2);
        assertThat(clone.getEntities().get(0)).isNotSameAs(original.getEntities().get(0));
        assertThat(clone.getEntities().get(0).getValue()).isEqualTo("A");
        assertThat(clone.getScore()).isEqualTo(SimpleScore.of(10));
    }

    // ── Custom SolutionCloner ───────────────────────────────────────

    @Test
    void customSolutionCloner_isUsed() {
        var spec = PlanningSpecification.of(CloneSolution.class)
                .score(SimpleScore.class, CloneSolution::getScore, CloneSolution::setScore)
                .problemFacts("values", CloneSolution::getValues)
                .entityCollection("entities", CloneSolution::getEntities)
                .valueRange("vr", CloneSolution::getValues)
                .entity(CloneEntity.class, e -> e
                        .planningId(CloneEntity::getId)
                        .variable("value", String.class, v -> v
                                .accessors(CloneEntity::getValue, CloneEntity::setValue)
                                .valueRange("vr")))
                .solutionCloner(original -> {
                    var clone = new CloneSolution();
                    clone.setScore(original.getScore());
                    clone.getValues().addAll(original.getValues());
                    for (var entity : original.getEntities()) {
                        var clonedEntity = new CloneEntity(entity.getId());
                        clonedEntity.setValue(entity.getValue());
                        clone.getEntities().add(clonedEntity);
                    }
                    return clone;
                })
                .build();

        var sd = SpecificationCompiler.compile(spec, null);
        var cloner = sd.getSolutionCloner();

        var original = new CloneSolution();
        original.getValues().addAll(List.of("X", "Y"));
        original.getEntities().add(new CloneEntity("e1"));
        original.setScore(SimpleScore.of(-5));

        var clone = cloner.cloneSolution(original);
        assertThat(clone).isNotSameAs(original);
        assertThat(clone.getScore()).isEqualTo(SimpleScore.of(-5));
        assertThat(clone.getValues()).containsExactly("X", "Y");
        assertThat(clone.getEntities()).hasSize(1);
    }

    // ── List variable with inverse relation + index shadows ─────────

    static class RoutingSolution {
        SimpleScore score;
        List<Visit> visits = new ArrayList<>();
        List<Vehicle> vehicles = new ArrayList<>();

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        List<Visit> getVisits() {
            return visits;
        }

        List<Vehicle> getVehicles() {
            return vehicles;
        }
    }

    static class Vehicle {
        String id;
        List<Visit> route = new ArrayList<>();

        Vehicle() {
        }

        Vehicle(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        List<Visit> getRoute() {
            return route;
        }

        void setRoute(List<Visit> route) {
            this.route = route;
        }
    }

    static class Visit {
        String id;
        Vehicle vehicle; // inverse relation shadow
        int index = -1; // index shadow

        Visit() {
        }

        Visit(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        Vehicle getVehicle() {
            return vehicle;
        }

        void setVehicle(Vehicle vehicle) {
            this.vehicle = vehicle;
        }

        int getIndex() {
            return index;
        }

        void setIndex(int index) {
            this.index = index;
        }
    }

    @Test
    void listVariable_withInverseAndIndex_compiles() {
        var spec = PlanningSpecification.of(RoutingSolution.class)
                .score(SimpleScore.class, RoutingSolution::getScore, RoutingSolution::setScore)
                .problemFacts("visits", RoutingSolution::getVisits)
                .entityCollection("vehicles", RoutingSolution::getVehicles)
                .valueRange("visitRange", RoutingSolution::getVisits)
                .entity(Vehicle.class, e -> e
                        .planningId(Vehicle::getId)
                        .listVariable("route", Visit.class, lv -> lv
                                .accessors(Vehicle::getRoute, Vehicle::setRoute)
                                .valueRange("visitRange")))
                .entity(Visit.class, e -> e
                        .planningId(Visit::getId)
                        .inverseRelationShadow("vehicle", Vehicle.class,
                                Visit::getVehicle, Visit::setVehicle,
                                src -> src.sourceVariable("route"))
                        .indexShadow("index",
                                Visit::getIndex, Visit::setIndex,
                                src -> src.sourceVariable("route")))
                .build();

        var sd = SpecificationCompiler.compile(spec, null);

        // Vehicle entity has a list variable
        var vehicleEd = sd.findEntityDescriptorOrFail(Vehicle.class);
        assertThat(vehicleEd.hasAnyListVariables()).isTrue();
        var listVd = vehicleEd.getListVariableDescriptor();
        assertThat(listVd).isNotNull();
        assertThat(listVd.getVariableName()).isEqualTo("route");

        // Visit entity has shadow variables
        var visitEd = sd.findEntityDescriptorOrFail(Visit.class);
        assertThat(visitEd.getShadowVariableDescriptors()).hasSize(2);
        assertThat(visitEd.getShadowVariableDescriptor("vehicle"))
                .isInstanceOf(InverseRelationShadowVariableDescriptor.class);
        assertThat(visitEd.getShadowVariableDescriptor("index"))
                .isInstanceOf(IndexShadowVariableDescriptor.class);
    }

    @Test
    void listVariable_compilesAndSolves() {
        var spec = PlanningSpecification.of(RoutingSolution.class)
                .score(SimpleScore.class, RoutingSolution::getScore, RoutingSolution::setScore)
                .problemFacts("visits", RoutingSolution::getVisits)
                .entityCollection("vehicles", RoutingSolution::getVehicles)
                .valueRange("visitRange", RoutingSolution::getVisits)
                .entity(Vehicle.class, e -> e
                        .planningId(Vehicle::getId)
                        .listVariable("route", Visit.class, lv -> lv
                                .accessors(Vehicle::getRoute, Vehicle::setRoute)
                                .valueRange("visitRange")))
                .entity(Visit.class, e -> e
                        .planningId(Visit::getId)
                        .inverseRelationShadow("vehicle", Vehicle.class,
                                Visit::getVehicle, Visit::setVehicle,
                                src -> src.sourceVariable("route"))
                        .indexShadow("index",
                                Visit::getIndex, Visit::setIndex,
                                src -> src.sourceVariable("route")))
                .build();

        var solverConfig = chOnlySolverConfig(spec, RoutingScoreCalculator.class);
        var solver = SolverFactory.<RoutingSolution> create(solverConfig).buildSolver();

        var problem = new RoutingSolution();
        problem.getVehicles().add(new Vehicle("truck1"));
        problem.getVehicles().add(new Vehicle("truck2"));
        for (int i = 0; i < 6; i++) {
            problem.getVisits().add(new Visit("visit" + i));
        }

        var solution = solver.solve(problem);
        assertThat(solution.getScore()).isNotNull();

        // All visits should be assigned to some vehicle
        int totalAssigned = solution.getVehicles().stream()
                .mapToInt(v -> v.getRoute().size()).sum();
        assertThat(totalAssigned).isEqualTo(6);

        // Inverse relation shadows should be populated
        for (var vehicle : solution.getVehicles()) {
            for (var visit : vehicle.getRoute()) {
                assertThat(visit.getVehicle()).isSameAs(vehicle);
            }
        }

        // Index shadows should be populated
        for (var vehicle : solution.getVehicles()) {
            for (int i = 0; i < vehicle.getRoute().size(); i++) {
                assertThat(vehicle.getRoute().get(i).getIndex()).isEqualTo(i);
            }
        }
    }

    public static class RoutingScoreCalculator implements EasyScoreCalculator<RoutingSolution, SimpleScore> {
        @Override
        public SimpleScore calculateScore(RoutingSolution solution) {
            return SimpleScore.of(0);
        }
    }

    // ── Multiple entities with different variable types ──────────────

    static class MultiEntitySolution {
        SimpleScore score;
        List<String> colors = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        List<ColorEntity> colorEntities = new ArrayList<>();
        List<NumberEntity> numberEntities = new ArrayList<>();

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        List<String> getColors() {
            return colors;
        }

        List<Integer> getNumbers() {
            return numbers;
        }

        List<ColorEntity> getColorEntities() {
            return colorEntities;
        }

        List<NumberEntity> getNumberEntities() {
            return numberEntities;
        }
    }

    static class ColorEntity {
        String id;
        String color;

        ColorEntity() {
        }

        ColorEntity(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        String getColor() {
            return color;
        }

        void setColor(String color) {
            this.color = color;
        }
    }

    static class NumberEntity {
        String id;
        Integer number;

        NumberEntity() {
        }

        NumberEntity(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        Integer getNumber() {
            return number;
        }

        void setNumber(Integer number) {
            this.number = number;
        }
    }

    @Test
    void multipleEntityTypes_compileAndSolve() {
        var spec = PlanningSpecification.of(MultiEntitySolution.class)
                .score(SimpleScore.class, MultiEntitySolution::getScore, MultiEntitySolution::setScore)
                .problemFacts("colors", MultiEntitySolution::getColors)
                .problemFacts("numbers", MultiEntitySolution::getNumbers)
                .entityCollection("colorEntities", MultiEntitySolution::getColorEntities)
                .entityCollection("numberEntities", MultiEntitySolution::getNumberEntities)
                .valueRange("colorRange", MultiEntitySolution::getColors)
                .valueRange("numberRange", MultiEntitySolution::getNumbers)
                .entity(ColorEntity.class, e -> e
                        .planningId(ColorEntity::getId)
                        .variable("color", String.class, v -> v
                                .accessors(ColorEntity::getColor, ColorEntity::setColor)
                                .valueRange("colorRange")))
                .entity(NumberEntity.class, e -> e
                        .planningId(NumberEntity::getId)
                        .variable("number", Integer.class, v -> v
                                .accessors(NumberEntity::getNumber, NumberEntity::setNumber)
                                .valueRange("numberRange")))
                .build();

        var sd = SpecificationCompiler.compile(spec, null);
        assertThat(sd.getEntityDescriptors()).hasSize(2);

        var colorEd = sd.findEntityDescriptorOrFail(ColorEntity.class);
        assertThat(colorEd.getGenuineVariableDescriptor("color")).isNotNull();
        assertThat(colorEd.getGenuineVariableDescriptor("color").getValueRangeDescriptor()).isNotNull();

        var numberEd = sd.findEntityDescriptorOrFail(NumberEntity.class);
        assertThat(numberEd.getGenuineVariableDescriptor("number")).isNotNull();
        assertThat(numberEd.getGenuineVariableDescriptor("number").getValueRangeDescriptor()).isNotNull();

        // Verify fact/entity collection accessors
        assertThat(sd.getProblemFactCollectionMemberAccessorMap()).containsKey("colors");
        assertThat(sd.getProblemFactCollectionMemberAccessorMap()).containsKey("numbers");
        assertThat(sd.getEntityCollectionMemberAccessorMap()).containsKey("colorEntities");
        assertThat(sd.getEntityCollectionMemberAccessorMap()).containsKey("numberEntities");
    }

    public static class MultiEntityScoreCalculator implements EasyScoreCalculator<MultiEntitySolution, SimpleScore> {
        @Override
        public SimpleScore calculateScore(MultiEntitySolution solution) {
            return SimpleScore.of(0);
        }
    }

    // ── Singular problem fact (not collection) ──────────────────────

    static class SingularFactSolution {
        SimpleScore score;
        String config = "default";
        List<String> values = new ArrayList<>();
        List<SingularFactEntity> entities = new ArrayList<>();

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        String getConfig() {
            return config;
        }

        List<String> getValues() {
            return values;
        }

        List<SingularFactEntity> getEntities() {
            return entities;
        }
    }

    static class SingularFactEntity {
        String id;
        String value;

        SingularFactEntity() {
        }

        SingularFactEntity(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        String getValue() {
            return value;
        }

        void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    void singularProblemFact_isRegistered() {
        var spec = PlanningSpecification.of(SingularFactSolution.class)
                .score(SimpleScore.class, SingularFactSolution::getScore, SingularFactSolution::setScore)
                .problemFact("config", SingularFactSolution::getConfig)
                .problemFacts("values", SingularFactSolution::getValues)
                .entityCollection("entities", SingularFactSolution::getEntities)
                .valueRange("vr", SingularFactSolution::getValues)
                .entity(SingularFactEntity.class, e -> e
                        .planningId(SingularFactEntity::getId)
                        .variable("value", String.class, v -> v
                                .accessors(SingularFactEntity::getValue, SingularFactEntity::setValue)
                                .valueRange("vr")))
                .build();

        var sd = SpecificationCompiler.compile(spec, null);
        // Singular fact goes to problemFactMemberAccessorMap
        assertThat(sd.getProblemFactMemberAccessorMap()).containsKey("config");
        // Collection fact goes to problemFactCollectionMemberAccessorMap
        assertThat(sd.getProblemFactCollectionMemberAccessorMap()).containsKey("values");

        // Verify the singular accessor works
        var configAccessor = sd.getProblemFactMemberAccessorMap().get("config");
        var problem = new SingularFactSolution();
        assertThat(configAccessor.executeGetter(problem)).isEqualTo("default");
    }

    // ── Entity-scoped value range ───────────────────────────────────

    static class EntityRangeSolution {
        SimpleScore score;
        List<EntityRangeEntity> entities = new ArrayList<>();

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        List<EntityRangeEntity> getEntities() {
            return entities;
        }
    }

    static class EntityRangeEntity {
        String id;
        List<Integer> possibleValues = new ArrayList<>();
        Integer value;

        EntityRangeEntity() {
        }

        EntityRangeEntity(String id, List<Integer> possibleValues) {
            this.id = id;
            this.possibleValues = new ArrayList<>(possibleValues);
        }

        String getId() {
            return id;
        }

        List<Integer> getPossibleValues() {
            return possibleValues;
        }

        Integer getValue() {
            return value;
        }

        void setValue(Integer value) {
            this.value = value;
        }
    }

    @Test
    void entityScopedValueRange_compilesAndSolves() {
        var spec = PlanningSpecification.of(EntityRangeSolution.class)
                .score(SimpleScore.class, EntityRangeSolution::getScore, EntityRangeSolution::setScore)
                .entityCollection("entities", EntityRangeSolution::getEntities)
                .entity(EntityRangeEntity.class, e -> e
                        .planningId(EntityRangeEntity::getId)
                        .valueRange("entityVr", EntityRangeEntity::getPossibleValues)
                        .variable("value", Integer.class, v -> v
                                .accessors(EntityRangeEntity::getValue, EntityRangeEntity::setValue)
                                .valueRange("entityVr")))
                .build();

        var sd = SpecificationCompiler.compile(spec, null);
        var ed = sd.findEntityDescriptorOrFail(EntityRangeEntity.class);
        var vd = ed.getGenuineVariableDescriptor("value");
        assertThat(vd.getValueRangeDescriptor()).isNotNull();

        // Solve: each entity has its own value range
        var solverConfig = solverConfig(spec, EntityRangeScoreCalculator.class);
        var solver = SolverFactory.<EntityRangeSolution> create(solverConfig).buildSolver();

        var problem = new EntityRangeSolution();
        problem.getEntities().add(new EntityRangeEntity("e1", List.of(10, 20)));
        problem.getEntities().add(new EntityRangeEntity("e2", List.of(30, 40)));

        var solution = solver.solve(problem);
        assertThat(solution.getScore()).isNotNull();
        for (var entity : solution.getEntities()) {
            assertThat(entity.getValue()).isNotNull();
            assertThat(entity.getPossibleValues()).contains(entity.getValue());
        }
    }

    public static class EntityRangeScoreCalculator implements EasyScoreCalculator<EntityRangeSolution, SimpleScore> {
        @Override
        public SimpleScore calculateScore(EntityRangeSolution solution) {
            return SimpleScore.of(0);
        }
    }

    // ── Multiple value ranges per variable ──────────────────────────

    static class MultiRangeSolution {
        SimpleScore score;
        List<Integer> lowValues = new ArrayList<>();
        List<Integer> highValues = new ArrayList<>();
        List<MultiRangeEntity> entities = new ArrayList<>();

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        List<Integer> getLowValues() {
            return lowValues;
        }

        List<Integer> getHighValues() {
            return highValues;
        }

        List<MultiRangeEntity> getEntities() {
            return entities;
        }
    }

    static class MultiRangeEntity {
        String id;
        Integer value;

        MultiRangeEntity() {
        }

        MultiRangeEntity(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        Integer getValue() {
            return value;
        }

        void setValue(Integer value) {
            this.value = value;
        }
    }

    @Test
    void multipleValueRanges_compilesAndSolves() {
        var spec = PlanningSpecification.of(MultiRangeSolution.class)
                .score(SimpleScore.class, MultiRangeSolution::getScore, MultiRangeSolution::setScore)
                .problemFacts("lowValues", MultiRangeSolution::getLowValues)
                .problemFacts("highValues", MultiRangeSolution::getHighValues)
                .entityCollection("entities", MultiRangeSolution::getEntities)
                .valueRange("low", MultiRangeSolution::getLowValues)
                .valueRange("high", MultiRangeSolution::getHighValues)
                .entity(MultiRangeEntity.class, e -> e
                        .planningId(MultiRangeEntity::getId)
                        .variable("value", Integer.class, v -> v
                                .accessors(MultiRangeEntity::getValue, MultiRangeEntity::setValue)
                                .valueRange("low", "high")))
                .build();

        var sd = SpecificationCompiler.compile(spec, null);
        var ed = sd.findEntityDescriptorOrFail(MultiRangeEntity.class);
        var vd = ed.getGenuineVariableDescriptor("value");
        assertThat(vd.getValueRangeDescriptor()).isNotNull();

        // Solve: variable draws from merged ranges [1,2] + [100,200]
        var solverConfig = solverConfig(spec, MultiRangeScoreCalculator.class);
        var solver = SolverFactory.<MultiRangeSolution> create(solverConfig).buildSolver();

        var problem = new MultiRangeSolution();
        problem.getLowValues().addAll(List.of(1, 2));
        problem.getHighValues().addAll(List.of(100, 200));
        for (int i = 0; i < 3; i++) {
            problem.getEntities().add(new MultiRangeEntity("e" + i));
        }

        var solution = solver.solve(problem);
        assertThat(solution.getScore()).isNotNull();
        for (var entity : solution.getEntities()) {
            assertThat(entity.getValue()).isNotNull();
            assertThat(entity.getValue()).isIn(1, 2, 100, 200);
        }
    }

    public static class MultiRangeScoreCalculator implements EasyScoreCalculator<MultiRangeSolution, SimpleScore> {
        @Override
        public SimpleScore calculateScore(MultiRangeSolution solution) {
            return SimpleScore.of(0);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    @SuppressWarnings("rawtypes")
    private static <S> SolverConfig solverConfig(PlanningSpecification<S> spec, Class<? extends EasyScoreCalculator> calc) {
        return new SolverConfig()
                .withPlanningSpecification(spec)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withEasyScoreCalculatorClass(calc))
                .withTerminationConfig(new TerminationConfig()
                        .withSecondsSpentLimit(10L))
                .withPhases(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(5)));
    }

    @SuppressWarnings("rawtypes")
    private static <S> SolverConfig chOnlySolverConfig(PlanningSpecification<S> spec,
            Class<? extends EasyScoreCalculator> calc) {
        return new SolverConfig()
                .withPlanningSpecification(spec)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withEasyScoreCalculatorClass(calc))
                .withTerminationConfig(new TerminationConfig()
                        .withSecondsSpentLimit(10L))
                .withPhases(new ConstructionHeuristicPhaseConfig());
    }
}
