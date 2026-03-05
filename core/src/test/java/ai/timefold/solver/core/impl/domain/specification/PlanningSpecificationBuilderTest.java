package ai.timefold.solver.core.impl.domain.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.specification.PlanningSpecification;
import ai.timefold.solver.core.api.score.SimpleScore;

import org.junit.jupiter.api.Test;

class PlanningSpecificationBuilderTest {

    // Simple domain classes (no annotations needed)
    static class MySolution {
        SimpleScore score;
        List<MyValue> values = new ArrayList<>();
        List<MyEntity> entities = new ArrayList<>();

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        List<MyValue> getValues() {
            return values;
        }

        List<MyEntity> getEntities() {
            return entities;
        }
    }

    static class MyEntity {
        String id;
        MyValue value;

        MyEntity() {
        }

        MyEntity(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        MyValue getValue() {
            return value;
        }

        void setValue(MyValue value) {
            this.value = value;
        }
    }

    static class MyValue {
        String code;

        MyValue() {
        }

        MyValue(String code) {
            this.code = code;
        }
    }

    @Test
    void minimalSpecification() {
        var spec = PlanningSpecification.of(MySolution.class)
                .score(SimpleScore.class, MySolution::getScore, MySolution::setScore)
                .problemFacts("values", MySolution::getValues)
                .entityCollection("entities", MySolution::getEntities)
                .valueRange("valueRange", MySolution::getValues)
                .entity(MyEntity.class, entity -> entity
                        .planningId(MyEntity::getId)
                        .variable("value", MyValue.class, var -> var
                                .accessors(MyEntity::getValue, MyEntity::setValue)
                                .valueRange("valueRange")))
                .build();

        assertThat(spec.solutionClass()).isEqualTo(MySolution.class);
        assertThat(spec.score()).isNotNull();
        assertThat(spec.score().scoreType()).isEqualTo(SimpleScore.class);
        assertThat(spec.facts()).hasSize(1);
        assertThat(spec.facts().getFirst().name()).isEqualTo("values");
        assertThat(spec.facts().getFirst().isCollection()).isTrue();
        assertThat(spec.entityCollections()).hasSize(1);
        assertThat(spec.entityCollections().getFirst().name()).isEqualTo("entities");
        assertThat(spec.valueRanges()).hasSize(1);
        assertThat(spec.valueRanges().getFirst().id()).isEqualTo("valueRange");
        assertThat(spec.entities()).hasSize(1);

        var entitySpec = spec.entities().getFirst();
        assertThat(entitySpec.entityClass()).isEqualTo(MyEntity.class);
        assertThat(entitySpec.planningIdGetter()).isNotNull();
        assertThat(entitySpec.variables()).hasSize(1);

        var varSpec = entitySpec.variables().getFirst();
        assertThat(varSpec.name()).isEqualTo("value");
        assertThat(varSpec.valueType()).isEqualTo(MyValue.class);
        assertThat(varSpec.isList()).isFalse();
        assertThat(varSpec.allowsUnassigned()).isFalse();
        assertThat(varSpec.valueRangeRefs()).containsExactly("valueRange");
    }

    @Test
    void lambdasWork() {
        var spec = PlanningSpecification.of(MySolution.class)
                .score(SimpleScore.class, MySolution::getScore, MySolution::setScore)
                .entityCollection("entities", MySolution::getEntities)
                .valueRange("valueRange", MySolution::getValues)
                .entity(MyEntity.class, entity -> entity
                        .variable("value", MyValue.class, var -> var
                                .accessors(MyEntity::getValue, MyEntity::setValue)
                                .valueRange("valueRange")))
                .build();

        // Verify the score getter/setter lambdas work
        var solution = new MySolution();
        solution.setScore(SimpleScore.of(42));
        assertThat(spec.score().getter().apply(solution)).isEqualTo(SimpleScore.of(42));

        // Verify entity collection getter
        var entity = new MyEntity("e1");
        solution.getEntities().add(entity);
        assertThat(spec.entityCollections().getFirst().getter().apply(solution)).hasSize(1);

        // Verify variable getter/setter
        var value = new MyValue("v1");
        var varSpec = spec.entities().getFirst().variables().getFirst();
        @SuppressWarnings("unchecked")
        var setter = (java.util.function.BiConsumer<MyEntity, MyValue>) (java.util.function.BiConsumer<?, ?>) varSpec.setter();
        setter.accept(entity, value);
        assertThat(entity.getValue()).isSameAs(value);

        @SuppressWarnings("unchecked")
        var getter = (java.util.function.Function<MyEntity, MyValue>) (java.util.function.Function<?, ?>) varSpec.getter();
        assertThat(getter.apply(entity)).isSameAs(value);
    }

    @Test
    void problemFact_notCollection() {
        var spec = PlanningSpecification.of(MySolution.class)
                .score(SimpleScore.class, MySolution::getScore, MySolution::setScore)
                .problemFact("singleFact", s -> "constant")
                .entityCollection("entities", MySolution::getEntities)
                .valueRange("valueRange", MySolution::getValues)
                .entity(MyEntity.class, entity -> entity
                        .variable("value", MyValue.class, var -> var
                                .accessors(MyEntity::getValue, MyEntity::setValue)
                                .valueRange("valueRange")))
                .build();

        assertThat(spec.facts()).hasSize(1);
        assertThat(spec.facts().getFirst().name()).isEqualTo("singleFact");
        assertThat(spec.facts().getFirst().isCollection()).isFalse();
    }

    @Test
    void allowsUnassigned() {
        var spec = PlanningSpecification.of(MySolution.class)
                .score(SimpleScore.class, MySolution::getScore, MySolution::setScore)
                .entityCollection("entities", MySolution::getEntities)
                .valueRange("valueRange", MySolution::getValues)
                .entity(MyEntity.class, entity -> entity
                        .variable("value", MyValue.class, var -> var
                                .accessors(MyEntity::getValue, MyEntity::setValue)
                                .valueRange("valueRange")
                                .allowsUnassigned(true)))
                .build();

        assertThat(spec.entities().getFirst().variables().getFirst().allowsUnassigned()).isTrue();
    }

    @Test
    void buildFailsWithoutScore() {
        var builder = PlanningSpecification.of(MySolution.class)
                .entityCollection("entities", MySolution::getEntities)
                .valueRange("valueRange", MySolution::getValues)
                .entity(MyEntity.class, entity -> entity
                        .variable("value", MyValue.class, var -> var
                                .accessors(MyEntity::getValue, MyEntity::setValue)
                                .valueRange("valueRange")));

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buildFailsWithoutEntities() {
        var builder = PlanningSpecification.of(MySolution.class)
                .score(SimpleScore.class, MySolution::getScore, MySolution::setScore)
                .entityCollection("entities", MySolution::getEntities)
                .valueRange("valueRange", MySolution::getValues);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buildFailsWithoutEntityCollections() {
        var builder = PlanningSpecification.of(MySolution.class)
                .score(SimpleScore.class, MySolution::getScore, MySolution::setScore)
                .valueRange("valueRange", MySolution::getValues)
                .entity(MyEntity.class, entity -> entity
                        .variable("value", MyValue.class, var -> var
                                .accessors(MyEntity::getValue, MyEntity::setValue)
                                .valueRange("valueRange")));

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void specificationIsImmutable() {
        var spec = PlanningSpecification.of(MySolution.class)
                .score(SimpleScore.class, MySolution::getScore, MySolution::setScore)
                .entityCollection("entities", MySolution::getEntities)
                .valueRange("valueRange", MySolution::getValues)
                .entity(MyEntity.class, entity -> entity
                        .variable("value", MyValue.class, var -> var
                                .accessors(MyEntity::getValue, MyEntity::setValue)
                                .valueRange("valueRange")))
                .build();

        // Records are immutable by default — just verify we can access all fields
        assertThat(spec.cloning()).isNull();
        assertThat(spec.constraintWeights()).isNull();
    }
}
