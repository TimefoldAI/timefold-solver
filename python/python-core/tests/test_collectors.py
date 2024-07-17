from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.score import *
from timefold.solver.config import *

from dataclasses import dataclass, field
from typing import Annotated, List


@dataclass
class Value:
    number: int


@planning_entity
@dataclass
class Entity:
    code: str
    value: Annotated[Value, PlanningVariable] = field(default=None)


@planning_solution
@dataclass
class Solution:
    entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
    value_list: Annotated[List[Value], ProblemFactCollectionProperty, ValueRangeProvider]
    score: Annotated[SimpleScore,
    PlanningScore] = field(default=None)


def create_score_manager(constraint_provider) -> SolutionManager:
    return SolutionManager.create(SolverFactory.create(
        SolverConfig(solution_class=Solution,
                     entity_class_list=[Entity],
                     score_director_factory_config=ScoreDirectorFactoryConfig(
                         constraint_provider_function=constraint_provider
                     ))))


def test_min():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.min(lambda entity: entity.value.number))
            .reward(SimpleScore.ONE, lambda min_value: min_value)
            .as_constraint('Min value')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(1)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(1)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(2)


def test_max():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.max(lambda entity: entity.value.number))
            .reward(SimpleScore.ONE, lambda max_value: max_value)
            .as_constraint('Max value')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(1)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(2)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(2)


def test_sum():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.sum(lambda entity: entity.value.number))
            .reward(SimpleScore.ONE, lambda sum_value: sum_value)
            .as_constraint('Sum value')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(2)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(3)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(4)


def test_average():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.average(lambda entity: entity.value.number))
            .reward(SimpleScore.ONE, lambda average_value: int(10 * average_value))
            .as_constraint('Average value')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(10)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(15)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(20)


def test_count():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .filter(lambda entity: entity.code[0] == 'A')
            .group_by(ConstraintCollectors.count())
            .reward(SimpleScore.ONE, lambda count: count)
            .as_constraint('Count value')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a1: Entity = Entity('A1')
    entity_a2: Entity = Entity('A2')
    entity_b: Entity = Entity('B1')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a1, entity_a2, entity_b], [value_1, value_2])
    entity_a1.value = value_1
    entity_a2.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(2)


def test_count_distinct():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.count_distinct(lambda entity: entity.value))
            .reward(SimpleScore.ONE, lambda count: count)
            .as_constraint('Count distinct value')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(1)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(2)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(1)


def test_to_consecutive_sequences():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.to_consecutive_sequences(
                lambda entity: entity.value.number))
            .flatten_last(lambda sequences: sequences.getConsecutiveSequences())
            .reward(SimpleScore.ONE, lambda sequence: sequence.getCount() ** 2)
            .as_constraint('squared sequence length')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')
    entity_c: Entity = Entity('C')
    entity_d: Entity = Entity('D')
    entity_e: Entity = Entity('E')

    value_1 = Value(1)
    value_2 = Value(2)
    value_3 = Value(3)
    value_4 = Value(4)
    value_5 = Value(5)
    value_6 = Value(6)
    value_7 = Value(7)
    value_8 = Value(8)
    value_9 = Value(9)

    problem = Solution([entity_a, entity_b, entity_c, entity_d, entity_e],
                       [value_1, value_2, value_3, value_4, value_5,
                        value_6, value_7, value_8, value_9])

    entity_a.value = value_1
    entity_b.value = value_3
    entity_c.value = value_5
    entity_d.value = value_7
    entity_e.value = value_9

    assert score_manager.explain(problem).score.score == 5

    entity_a.value = value_1
    entity_b.value = value_2
    entity_c.value = value_3
    entity_d.value = value_4
    entity_e.value = value_5

    assert score_manager.explain(problem).score.score == 25

    entity_a.value = value_1
    entity_b.value = value_2
    entity_c.value = value_3
    entity_d.value = value_5
    entity_e.value = value_6

    assert score_manager.explain(problem).score.score == 13


def test_to_list():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.to_list(lambda entity: entity.value))
            .reward(SimpleScore.ONE, lambda values: len(values))
            .as_constraint('list size')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(2)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(2)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(2)


def test_to_set():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.to_set(lambda entity: entity.value))
            .reward(SimpleScore.ONE, lambda values: len(values))
            .as_constraint('set size')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(1)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(2)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(1)


def test_to_map():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.to_map(lambda entity: entity.code, lambda entity: entity.value.number))
            .filter(lambda entity_map: next(iter(entity_map['A'])) == 1)
            .reward(SimpleScore.ONE, lambda entity_map: next(iter(entity_map['B'])))
            .as_constraint('map at B')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(1)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(2)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(0)


def test_to_sorted_set():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.to_sorted_set(lambda entity: entity.value.number))
            .reward(SimpleScore.ONE, lambda values: next(iter(values)))
            .as_constraint('min')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(1)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(1)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(2)


def test_to_sorted_map():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(
                ConstraintCollectors.to_sorted_map(lambda entity: entity.code, lambda entity: entity.value.number))
            .filter(lambda entity_map: next(iter(entity_map['B'])) == 1)
            .reward(SimpleScore.ONE, lambda entity_map: next(iter(entity_map['A'])))
            .as_constraint('map at A')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(1)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(0)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(0)

    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(2)


def test_conditionally():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.conditionally(lambda entity: entity.code[0] == 'A',
                                                         ConstraintCollectors.count()))
            .reward(SimpleScore.ONE, lambda count: count)
            .as_constraint('Conditionally count value')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a1: Entity = Entity('A1')
    entity_a2: Entity = Entity('A2')
    entity_b: Entity = Entity('B1')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a1, entity_a2, entity_b], [value_1, value_2])
    entity_a1.value = value_1
    entity_a2.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(2)


def test_compose():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.compose(
                ConstraintCollectors.min(lambda entity: entity.value.number),
                ConstraintCollectors.max(lambda entity: entity.value.number),
                lambda a, b: (a, b)
            ))
            .reward(SimpleScore.ONE, lambda min_max: min_max[0] + min_max[1] * 10)
            .as_constraint('Max value')
            # min is in lower digit; max in upper digit
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(11)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(21)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(22)


def test_collect_and_then():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.collect_and_then(
                ConstraintCollectors.min(lambda entity: entity.value.number),
                lambda a: 2 * a
            ))
            .reward(SimpleScore.ONE, lambda twice_min: twice_min)
            .as_constraint('Double min value')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.value = value_1
    entity_b.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(2)

    entity_a.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(2)

    entity_b.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(4)


def test_load_balance():
    @constraint_provider
    def define_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .group_by(ConstraintCollectors.load_balance(
                lambda entity: entity.value
            ))
            .reward(SimpleScore.ONE,
                    lambda balance: round(balance.unfairness() * 1000))
            .as_constraint('Balanced value')
        ]

    score_manager = create_score_manager(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')
    entity_c: Entity = Entity('C')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1])
    entity_a.value = value_1
    entity_b.value = value_1
    entity_c.value = value_1

    assert score_manager.explain(problem).score == SimpleScore.of(0)

    problem = Solution([entity_a, entity_b, entity_c], [value_1, value_2])

    assert score_manager.explain(problem).score == SimpleScore.of(0)

    entity_c.value = value_2

    assert score_manager.explain(problem).score == SimpleScore.of(707)
