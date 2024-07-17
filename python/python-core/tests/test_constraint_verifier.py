import pytest

from timefold.solver.domain import *
from timefold.solver.score import *
from timefold.solver.config import *
from timefold.solver.test import *

import inspect
import re
from typing import Annotated, List
from dataclasses import dataclass, field

from ai.timefold.solver.test.api.score.stream import (ConstraintVerifier as JavaConstraintVerifier,
                                                      SingleConstraintAssertion as JavaSingleConstraintAssertion,
                                                      SingleConstraintVerification as JavaSingleConstraintVerification,
                                                      MultiConstraintAssertion as JavaMultiConstraintAssertion,
                                                      MultiConstraintVerification as JavaMultiConstraintVerification)

def verifier_suite(verifier: ConstraintVerifier, same_value, is_value_one,
                   EntityValueIndictment, EntityValueJustification, EntityValuePairJustification,
                   solution, e1, e2, e3, v1, v2, v3):
    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .penalizes(0)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .rewards()

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .penalizes()

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .penalizes(1)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .indicts_with_exactly(EntityValueIndictment(e1, v1))

    e1.value = v1
    e2.value = v1
    e3.value = v1

    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .penalizes(1)

    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .penalizes(1)

    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .indicts_with(EntityValueIndictment(e1, e1.value), EntityValueIndictment(e2, v1)) \
        .penalizes_by(1)

    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .indicts_with_exactly(EntityValueIndictment(e1, e1.value), EntityValueIndictment(e2, v1)) \
        .penalizes_by(1)

    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .indicts_with(EntityValueIndictment(e1, v1))

    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .justifies_with(EntityValuePairJustification((e1, e2), v1, SimpleScore(-1))) \
        .penalizes_by(1)

    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .justifies_with_exactly(EntityValuePairJustification((e1, e2), v1, SimpleScore(-1))) \
        .penalizes_by(1)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .indicts_with_exactly(EntityValueIndictment(e1, v1))


    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .justifies_with(EntityValuePairJustification((e1, e2), v1, SimpleScore(1)))

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2, e3) \
            .justifies_with_exactly(EntityValuePairJustification((e1, e2), v1, SimpleScore(1)))

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .rewards(1)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .penalizes(0)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .penalizes(2)

    verifier.verify_that(same_value) \
        .given(e1, e2, e3) \
        .penalizes(3)

    verifier.verify_that(same_value) \
        .given(e1, e2, e3) \
        .penalizes_more_than(2)

    verifier.verify_that(same_value) \
        .given(e1, e2, e3) \
        .penalizes_less_than(4)

    verifier.verify_that(same_value) \
        .given(e1, e2, e3) \
        .penalizes()

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2, e3) \
            .penalizes_more_than(3)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2, e3) \
            .penalizes_less_than(3)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2, e3) \
            .rewards(3)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2, e3) \
            .penalizes(2)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2, e3) \
            .penalizes(4)

    verifier.verify_that(same_value) \
        .given_solution(solution) \
        .penalizes(3)

    verifier.verify_that(same_value) \
        .given_solution(solution) \
        .penalizes()

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given_solution(solution) \
            .rewards(3)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given_solution(solution) \
            .penalizes(2)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given_solution(solution) \
            .penalizes(4)

    verifier.verify_that(is_value_one) \
        .given(e1, e2, e3) \
        .rewards(3)

    verifier.verify_that(is_value_one) \
        .given(e1, e2, e3) \
        .rewards()

    with pytest.raises(AssertionError):
        verifier.verify_that(is_value_one) \
            .given(e1, e2, e3) \
            .penalizes()

    with pytest.raises(AssertionError):
        verifier.verify_that(is_value_one) \
            .given(e1, e2, e3) \
            .penalizes(3)

    with pytest.raises(AssertionError):
        verifier.verify_that(is_value_one) \
            .given(e1, e2, e3) \
            .rewards(2)

    with pytest.raises(AssertionError):
        verifier.verify_that(is_value_one) \
            .given(e1, e2, e3) \
            .rewards(4)

    verifier.verify_that() \
        .given(e1, e2, e3) \
        .scores(SimpleScore.of(0))

    with pytest.raises(AssertionError):
        verifier.verify_that() \
            .given(e1, e2, e3) \
            .scores(SimpleScore.of(1))

    with pytest.raises(AssertionError):
        verifier.verify_that() \
            .given(e1, e2, e3) \
            .scores(SimpleScore.of(-1))

    verifier.verify_that() \
        .given_solution(solution) \
        .scores(SimpleScore.of(0))

    with pytest.raises(AssertionError):
        verifier.verify_that() \
            .given_solution(solution) \
            .scores(SimpleScore.of(1))

    with pytest.raises(AssertionError):
        verifier.verify_that() \
            .given_solution(solution) \
            .scores(SimpleScore.of(-1))

    e1.value = v1
    e2.value = v2
    e3.value = v3
    verifier.verify_that() \
        .given(e1, e2, e3) \
        .scores(SimpleScore.of(1))

    with pytest.raises(AssertionError):
        verifier.verify_that() \
            .given(e1, e2, e3) \
            .scores(SimpleScore.of(2))

    with pytest.raises(AssertionError):
        verifier.verify_that() \
            .given(e1, e2, e3) \
            .scores(SimpleScore.of(0))

    verifier.verify_that() \
        .given_solution(solution) \
        .scores(SimpleScore.of(1))

    with pytest.raises(AssertionError):
        verifier.verify_that() \
            .given_solution(solution) \
            .scores(SimpleScore.of(2))

    with pytest.raises(AssertionError):
        verifier.verify_that() \
            .given_solution(solution) \
            .scores(SimpleScore.of(0))


def test_constraint_verifier_create():
    @dataclass(unsafe_hash=True)
    class Value:
        code: str

        def __str__(self):
            return f'Value({self.code})'

    @planning_entity
    @dataclass(unsafe_hash=True)
    class Entity:
        code: str
        value: Annotated[Value | None, PlanningVariable] = field(default=None)

        def __str__(self):
            return f'Entity({self.code}, {self.value})'

    @dataclass(unsafe_hash=True)
    class EntityValueIndictment:
        entity: Entity
        value: Value

        def __str__(self):
            return f'EntityValueIndictment({self.entity}, {self.value})'

    @dataclass(unsafe_hash=True)
    class EntityValueJustification(ConstraintJustification):
        entity: Entity
        value: Value
        score: SimpleScore

        def __str__(self):
            return f'EntityValueJustification({self.entity}, {self.value}, {self.score})'

    @dataclass(unsafe_hash=True)
    class EntityValuePairJustification(ConstraintJustification):
        entities: tuple[Entity]
        value: Value
        score: SimpleScore

        def __str__(self):
            return f'EntityValuePairJustification({self.entities}, {self.value}, {self.score})'

    def same_value(constraint_factory: ConstraintFactory):
        return (constraint_factory.for_each(Entity)
                .join(Entity, Joiners.less_than(lambda e: e.code),
                      Joiners.equal(lambda e: e.value))
                .penalize(SimpleScore.ONE)
                .indict_with(lambda e1, e2: [EntityValueIndictment(e1, e1.value), EntityValueIndictment(e2, e2.value)])
                .justify_with(lambda e1, e2, score: EntityValuePairJustification((e1, e2), e1.value, score))
                .as_constraint('Same Value')
                )

    def is_value_one(constraint_factory: ConstraintFactory):
        return (constraint_factory.for_each(Entity)
                .filter(lambda e: e.value.code == 'v1')
                .reward(SimpleScore.ONE)
                .indict_with(lambda e: [EntityValueIndictment(e, e.value)])
                .justify_with(lambda e, score: EntityValueJustification(e, e.value, score))
                .as_constraint('Value 1')
                )

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            same_value(constraint_factory),
            is_value_one(constraint_factory)
        ]

    @planning_solution
    @dataclass
    class Solution:
        entities: Annotated[List[Entity], PlanningEntityCollectionProperty]
        values: Annotated[List[Value], ProblemFactCollectionProperty, ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        )
    )

    verifier = ConstraintVerifier.create(solver_config)

    e1 = Entity('e1')
    e2 = Entity('e2')
    e3 = Entity('e3')

    v1 = Value('v1')
    v2 = Value('v2')
    v3 = Value('v3')

    solution = Solution([e1, e2, e3], [v1, v2, v3])

    verifier_suite(verifier, same_value, is_value_one,
                   EntityValueIndictment, EntityValueJustification, EntityValuePairJustification,
                   solution, e1, e2, e3, v1, v2, v3)

    verifier = ConstraintVerifier.build(my_constraints, Solution, Entity)

    e1 = Entity('e1')
    e2 = Entity('e2')
    e3 = Entity('e3')

    v1 = Value('v1')
    v2 = Value('v2')
    v3 = Value('v3')

    solution = Solution([e1, e2, e3], [v1, v2, v3])

    verifier_suite(verifier, same_value, is_value_one,
                   EntityValueIndictment, EntityValueJustification, EntityValuePairJustification,
                   solution, e1, e2, e3, v1, v2, v3)


ignored_java_functions = {
    'equals',
    'getClass',
    'hashCode',
    'notify',
    'notifyAll',
    'toString',
    'wait',
    'withConstraintStreamImplType'
}


def test_has_all_methods():
    missing = []
    for python_type, java_type in ((ConstraintVerifier, JavaConstraintVerifier),
                                   (SingleConstraintAssertion, JavaSingleConstraintAssertion),
                                   (SingleConstraintVerification, JavaSingleConstraintVerification),
                                   (MultiConstraintAssertion, JavaMultiConstraintAssertion),
                                   (MultiConstraintVerification, JavaMultiConstraintVerification)):
        for function_name, function_impl in inspect.getmembers(java_type, inspect.isfunction):
            if function_name in ignored_java_functions:
                continue
            snake_case_name = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', function_name)
            # change h_t_t_p -> http
            snake_case_name = re.sub('([a-z0-9])([A-Z])', r'\1_\2', snake_case_name).lower()
            if not hasattr(python_type, snake_case_name):
                missing.append((java_type, python_type, snake_case_name))

    if missing:
        assertion_msg = ''
        for java_type, python_type, snake_case_name in missing:
            assertion_msg += f'{python_type} is missing a method ({snake_case_name}) from java_type ({java_type}).)\n'
        raise AssertionError(assertion_msg)

