from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.config import *
from timefold.solver.score import *

from dataclasses import dataclass, field
from typing import Annotated, Optional, List


def test_solve_partial():
    @dataclass
    class Code:
        value: str

    @dataclass
    class Value:
        code: Code

    @planning_entity
    @dataclass
    class Entity:
        code: Code
        value: Annotated[Value, PlanningVariable] = field(default=None)

    def is_value_one(constraint_factory: ConstraintFactory):
        return (constraint_factory.for_each(Entity)
                .filter(lambda e: e.value.code.value == 'v1')
                .reward(SimpleScore.ONE)
                .as_constraint('Value 1')
                )

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
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
        ),
        termination_config=TerminationConfig(
            best_score_limit='3'
        )
    )

    e1 = Entity(Code('e1'))
    e2 = Entity(Code('e2'))
    e3 = Entity(Code('e3'))

    v1 = Value(Code('v1'))
    v2 = Value(Code('v2'))
    v3 = Value(Code('v3'))

    e1.value = v1
    e2.value = v2
    e3.value = v3

    problem = Solution([e1, e2, e3], [v1, v2, v3])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)

    assert solution.score.score == 3
    assert solution.entities[0].value == v1
    assert solution.entities[1].value == v1
    assert solution.entities[2].value == v1


def test_solve_nullable():
    @dataclass
    class Code:
        value: str

    @dataclass
    class Value:
        code: Code

    @planning_entity
    @dataclass
    class Entity:
        code: Code
        value: Annotated[Optional[Value], PlanningVariable(allows_unassigned=True,
                                                           value_range_provider_refs=['value_range'])] = (
            field(default=None))

    def at_least_one_null(constraint_factory: ConstraintFactory):
        return (constraint_factory.for_each_including_unassigned(Entity)
                .filter(lambda e: e.value is None)
                .group_by(ConstraintCollectors.count())
                .filter(lambda count: count >= 1)
                .reward(HardSoftScore.ONE_SOFT)
                .as_constraint('At least one null variable')
                )

    def assign_to_v1(constraint_factory: ConstraintFactory):
        return (constraint_factory.for_each_including_unassigned(Entity)
                .filter(lambda e: e.value is not None and e.value.code.value == 'v1')
                .group_by(ConstraintCollectors.count())
                .filter(lambda count: count >= 1)
                .reward(HardSoftScore.ONE_HARD)
                .as_constraint('At least one v1')
                )

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            at_least_one_null(constraint_factory),
            assign_to_v1(constraint_factory)
        ]

    @planning_solution
    @dataclass
    class Solution:
        entities: Annotated[List[Entity], PlanningEntityCollectionProperty]
        values: Annotated[List[Value], ProblemFactCollectionProperty, ValueRangeProvider(id='value_range')]
        score: Annotated[HardSoftScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='1hard/1soft'
        )
    )

    e1 = Entity(Code('e1'))
    e2 = Entity(Code('e2'))

    v1 = Value(Code('v1'))
    v2 = Value(Code('v2'))

    problem = Solution([e1, e2], [v1, v2])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)

    assert solution.score.is_feasible
    assert solution.score.hard_score == 1
    assert solution.score.soft_score == 1
    assert solution.entities[0].value == v1 or solution.entities[0].value is None
    assert solution.entities[1].value == v1 or solution.entities[1].value is None


def test_solve_typed():
    @dataclass
    class Code:
        value: str

    @dataclass
    class Value:
        code: Code

    @planning_entity
    @dataclass
    class Entity:
        code: Code
        value: Annotated[Value, PlanningVariable] = field(default=None)

    def assign_to_v1(constraint_factory: ConstraintFactory):
        return (constraint_factory.for_each(Entity)
                .filter(lambda e: e.value.code.value == 'v1')
                .reward(SimpleScore.ONE)
                .as_constraint('assign to v1')
                )

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            assign_to_v1(constraint_factory)
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
        ),
        termination_config=TerminationConfig(
            best_score_limit='2'
        )
    )

    e1 = Entity(Code('e1'))
    e2 = Entity(Code('e2'))

    v1 = Value(Code('v1'))
    v2 = Value(Code('v2'))

    problem = Solution([e1, e2], [v1, v2])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)

    assert solution.score.score == 2
    assert solution.entities[0].value == v1
    assert solution.entities[1].value == v1


def test_solve_complex_problem_facts():
    from abc import abstractmethod

    class BaseValue:
        @abstractmethod
        def get_id(self) -> str:
            raise NotImplementedError('Calling function on abstract base class')

        @abstractmethod
        def __str__(self) -> str:
            raise NotImplementedError('Calling function on abstract base class')

    @dataclass
    class ExpiringValue(BaseValue):
        name: str
        id: Annotated[str, PlanningId]
        expiration_date: float

        def get_id(self) -> str:
            return self.id

        def __str__(self) -> str:
            return f'ExpiringValue(id={self.id}, name={self.name})'

    @dataclass
    class SimpleValue(BaseValue):
        name: str
        id: Annotated[str, PlanningId]

        def get_id(self) -> str:
            return self.id

        def __str__(self) -> str:
            return f'SimpleValue(id={str(self.id)}, name={str(self.name)})'

    class NullValue(BaseValue):
        name: str
        id: Annotated[str, PlanningId]

        def get_id(self) -> str:
            return self.id

        def __str__(self) -> str:
            return f'NullValue(id={str(self.id)}, name={str(self.name)})'

    @planning_entity
    @dataclass
    class Entity:
        id: Annotated[str, PlanningId]
        list_of_suitable_values: List[BaseValue]
        start_time: int
        end_time: int
        value: Annotated[Optional[BaseValue], PlanningVariable(value_range_provider_refs=['value_range'])] = (
            field(default=None))

        def get_allowable_values(self) -> Annotated[List[BaseValue], ValueRangeProvider(id='value_range')]:
            return self.list_of_suitable_values

    @planning_solution
    @dataclass
    class Solution:
        entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
        score: Annotated[HardSoftScore, PlanningScore] = field(default=None)

    def is_present(r: Optional[BaseValue]) -> bool:
        if isinstance(r, NullValue):
            return False
        elif isinstance(r, BaseValue):
            return True
        else:
            return False

    def simultaneous_values(constraint_factory):
        return (
            constraint_factory.for_each_unique_pair(
                Entity,
                # ... if they support overlapping times
                (Joiners.overlapping(lambda entity: entity.start_time,
                                     lambda entity: entity.end_time)),
            )
            .filter(
                lambda entity_1, entity_2: (is_present(entity_1.value)
                                            and is_present(entity_2.value)
                                            and entity_1.value.get_id() == entity_2.value.get_id())
            )
            # Then penalize it!
            .penalize(HardSoftScore.ONE_HARD)
            .as_constraint("Simultaneous values")
        )

    def empty_value(constraint_factory):
        return (
            constraint_factory.for_each(Entity)
            .filter(lambda entity: not is_present(entity.value))
            .penalize(HardSoftScore.ONE_SOFT)
            .as_constraint("Prefer present value")
        )

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            simultaneous_values(constraint_factory),
            empty_value(constraint_factory)
        ]

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='0hard/0soft'
        )
    )

    v1 = ExpiringValue('expiring', '0', 2.0)
    v2 = SimpleValue('simple', '1')
    v3 = NullValue()

    e1 = Entity('e1', [v1, v2], 0, 2)
    e2 = Entity('e2', [v1, v3], 1, 3)

    problem = Solution([e1, e2])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)

    assert solution.score.hard_score == 0
    assert solution.score.soft_score == 0
    assert solution.entity_list[0].value == v2
    assert solution.entity_list[1].value == v1


def test_single_property():
    @dataclass
    class Value:
        code: str

    @planning_entity
    @dataclass
    class Entity:
        code: str
        value: Annotated[str, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .join(Value,
                  Joiners.equal(lambda entity: entity.value,
                                lambda value: value.code))
            .reward(SimpleScore.ONE)
            .as_constraint('Same as value'),
        ]

    @planning_solution
    @dataclass
    class Solution:
        entity: Annotated[Entity, PlanningEntityProperty]
        value: Annotated[Value, ProblemFactProperty]
        value_range: Annotated[List[str], ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='1'
        )
    )

    problem: Solution = Solution(Entity('A'), Value('1'), ['1', '2', '3'])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == 1
    assert solution.entity.value == '1'


def test_constraint_stream_in_join():
    @dataclass
    class Value:
        code: int

    @planning_entity
    @dataclass
    class Entity:
        code: str
        value: Annotated[Value, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .filter(lambda e: e.code == 'A')
            .join(constraint_factory.for_each(Entity).filter(lambda e: e.code == 'B'))
            .join(constraint_factory.for_each(Entity).filter(lambda e: e.code == 'C'))
            .join(constraint_factory.for_each(Entity).filter(lambda e: e.code == 'D'))
            .group_by(ConstraintCollectors.sum(lambda a, b, c, d: a.value.code + b.value.code +
                                               c.value.code + d.value.code))
            .reward(SimpleScore.ONE, lambda the_sum: the_sum)
            .as_constraint('First Four Entities'),
        ]

    @planning_solution
    @dataclass
    class Solution:
        entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
        value_list: Annotated[List[Value], ProblemFactCollectionProperty, ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        )
    )

    entity_1, entity_2, entity_3, entity_4, entity_5 = Entity('A'), Entity('B'), Entity('C'), Entity('D'), Entity('E')
    value_1, value_2, value_3 = Value(1), Value(2), Value(3)
    problem = Solution([entity_1, entity_2, entity_3, entity_4, entity_5], [value_1, value_2, value_3])
    score_manager = SolutionManager.create(SolverFactory.create(solver_config))

    entity_1.value = value_1
    entity_2.value = value_1
    entity_3.value = value_1
    entity_4.value = value_1
    entity_5.value = value_1

    assert score_manager.update(problem).score == 4

    entity_5.value = value_2

    assert score_manager.update(problem).score == 4

    entity_1.value = value_2
    assert score_manager.update(problem).score == 5

    entity_2.value = value_2
    assert score_manager.update(problem).score == 6

    entity_3.value = value_2
    assert score_manager.update(problem).score == 7

    entity_4.value = value_2
    assert score_manager.update(problem).score == 8

    entity_1.value = value_3
    assert score_manager.update(problem).score == 9


def test_tuple_group_by_key():
    @dataclass(eq=False)
    class Value:
        code: str

    @planning_entity
    @dataclass
    class Entity:
        code: str
        value: Annotated[str, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .join(Value,
                  Joiners.equal(lambda entity: entity.value,
                                lambda value: value.code))
            .group_by(lambda entity, value: (0, value), ConstraintCollectors.count_bi())
            .reward(SimpleScore.ONE, lambda _, count: count)
            .as_constraint('Same as value'),
        ]

    @planning_solution
    @dataclass
    class Solution:
        entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
        value_list: Annotated[List[Value], ProblemFactCollectionProperty]
        value_range: Annotated[List[str], ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    entity_list = [Entity('A0'), Entity('B0'), Entity('C0'),
                   Entity('A1'), Entity('B1'), Entity('C1'),
                   Entity('A2'), Entity('B2'), Entity('C2'),
                   Entity('A3'), Entity('B3'), Entity('C3'),
                   Entity('A4'), Entity('B4'), Entity('C4'),
                   Entity('A5'), Entity('B5'), Entity('C5'),
                   Entity('A6'), Entity('B6'), Entity('C6'),
                   Entity('A7'), Entity('B7'), Entity('C7'),
                   Entity('A8'), Entity('B8'), Entity('C8'),
                   Entity('A9'), Entity('B9'), Entity('C9')]

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit=str(len(entity_list))
        )
    )

    problem: Solution = Solution(entity_list,
                                 [Value('1')],
                                 ['1', '2', '3'])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == len(entity_list)
    for entity in solution.entity_list:
        assert entity.value == '1'


def test_python_object():
    import ctypes
    pointer1 = ctypes.c_void_p(1)
    pointer2 = ctypes.c_void_p(2)
    pointer3 = ctypes.c_void_p(3)

    @planning_entity
    @dataclass
    class Entity:
        code: str
        value: Annotated[ctypes.c_void_p, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .filter(lambda entity: entity.value == pointer1)
            .reward(SimpleScore.ONE)
            .as_constraint('Same as value'),
            constraint_factory.for_each(Entity)
            .group_by(lambda entity: entity.value.value, ConstraintCollectors.count())
            .reward(SimpleScore.ONE, lambda value, count: count * count)
            .as_constraint('Entity have same value'),
            constraint_factory.for_each(Entity)
            .group_by(lambda entity: (entity.code, entity.value.value))
            .join(Entity,
                  Joiners.equal(lambda pair: pair[0], lambda entity: entity.code),
                  Joiners.equal(lambda pair: pair[1], lambda entity: entity.value.value))
            .reward(SimpleScore.ONE)
            .as_constraint('Entity for pair'),
        ]

    @planning_solution
    @dataclass
    class Solution:
        entity: Annotated[Entity, PlanningEntityProperty]
        value_range: Annotated[List[ctypes.c_void_p], ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='3'
        )
    )
    problem: Solution = Solution(Entity('A'), [pointer1, pointer2, pointer3])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == 3
    assert solution.entity.value is pointer1


def test_custom_planning_id():
    from uuid import UUID, uuid4
    id_1 = uuid4()
    id_2 = uuid4()
    id_3 = uuid4()

    @dataclass(unsafe_hash=True)
    class Value:
        code: str

    @planning_entity
    @dataclass
    class Entity:
        code: Annotated[UUID, PlanningId]
        value: Annotated[Value, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each_unique_pair(Entity,
                                                    Joiners.equal(lambda entity: entity.value))
            .penalize(SimpleScore.ONE)
            .as_constraint('Same value'),
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
        ),
        termination_config=TerminationConfig(
            best_score_limit='0'
        )
    )

    entity_1 = Entity(id_1)
    entity_2 = Entity(id_2)
    entity_3 = Entity(id_3)

    value_1 = Value('A')
    value_2 = Value('B')
    value_3 = Value('C')
    problem: Solution = Solution([
        entity_1,
        entity_2,
        entity_3
    ], [
        value_1,
        value_2,
        value_3
    ])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == 0

    encountered = set()
    for entity in solution.entities:
        assert entity.value not in encountered
        encountered.add(entity.value)


def test_custom_comparator():
    @dataclass(order=True, unsafe_hash=True)
    class Value:
        code: str

    @planning_entity
    @dataclass
    class Entity:
        code: Annotated[int, PlanningId]
        value: Annotated[Value, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            # use less_than_or_equal and greater_than_or_equal since they require Comparable instances
            .if_exists_other(Entity, Joiners.less_than_or_equal(lambda entity: entity.value),
                             Joiners.greater_than_or_equal(lambda entity: entity.value))
            .penalize(SimpleScore.ONE)
            .as_constraint('Same value'),
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
        ),
        termination_config=TerminationConfig(
            best_score_limit='0'
        )
    )

    entity_1 = Entity(0)
    entity_2 = Entity(1)
    entity_3 = Entity(2)

    value_1 = Value('A')
    value_2 = Value('B')
    value_3 = Value('C')
    problem: Solution = Solution([
        entity_1,
        entity_2,
        entity_3
    ], [
        value_1,
        value_2,
        value_3
    ])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == 0

    encountered = set()
    for entity in solution.entities:
        assert entity.value not in encountered
        encountered.add(entity.value)


def test_custom_equals():
    @dataclass(eq=True, unsafe_hash=True)
    class Code:
        code: str

    @dataclass
    class Value:
        code: Code

    @planning_entity
    @dataclass
    class Entity:
        code: Annotated[int, PlanningId]
        value: Annotated[Value, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each_unique_pair(Entity,
                                                    Joiners.equal(lambda entity: entity.value.code))
            .penalize(SimpleScore.ONE)
            .as_constraint('Same value')
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
        ),
        termination_config=TerminationConfig(
            best_score_limit='-1'
        )
    )

    value_1a = Value(Code('A'))
    value_1b = Value(Code('A'))
    value_2a = Value(Code('B'))

    entity_1 = Entity(0, value_1a)
    entity_2 = Entity(1, value_1b)
    entity_3 = Entity(2, value_2a)
    problem: Solution = Solution([
        entity_1,
        entity_2,
        entity_3
    ], [
        value_1a,
        value_1b,
        value_2a,
    ])
    score_manager = SolutionManager.create(SolverFactory.create(solver_config))
    score = score_manager.update(problem)
    assert score.score == -1


def test_entity_value_range_provider():
    @dataclass(unsafe_hash=True)
    class Value:
        code: str

    @planning_entity
    @dataclass
    class Entity:
        code: Annotated[str, PlanningId]
        possible_values: List[Value]
        value: Annotated[Value, PlanningVariable(value_range_provider_refs=['value_range'])] = (
            field(default=None))

        def get_possible_values(self) -> Annotated[List[Value], ValueRangeProvider(id='value_range')]:
            return self.possible_values

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each_unique_pair(Entity,
                                                    Joiners.equal(lambda entity: entity.value))
            .reward(SimpleScore.ONE)
            .as_constraint('Same value'),
        ]

    @planning_solution
    @dataclass
    class Solution:
        entities: Annotated[List[Entity], PlanningEntityCollectionProperty]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='0'
        )
    )

    value_1 = Value('A')
    value_2 = Value('B')
    value_3 = Value('C')

    entity_1 = Entity('1', [value_1])
    entity_2 = Entity('2', [value_2])
    entity_3 = Entity('3', [value_3])

    problem: Solution = Solution([
        entity_1,
        entity_2,
        entity_3
    ])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == 0

    encountered = set()
    for entity in solution.entities:
        assert entity.value not in encountered
        encountered.add(entity.value)


def test_int_value_range_provider():
    @planning_entity
    @dataclass
    class Entity:
        code: Annotated[str, PlanningId]
        actual_value: int
        value: Annotated[int, PlanningVariable(value_range_provider_refs=['value_range'])] \
            = field(default=None)
        possible_values: Annotated[CountableValueRange, ValueRangeProvider(id='value_range')] = field(init=False)

        def __post_init__(self):
            self.possible_values = ValueRangeFactory.create_int_value_range(self.actual_value,
                                                                            self.actual_value + 1)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each_unique_pair(Entity,
                                                    Joiners.equal(lambda entity: entity.value))
            .reward(SimpleScore.ONE)
            .as_constraint('Same value'),
        ]

    @planning_solution
    @dataclass
    class Solution:
        entities: Annotated[List[Entity], PlanningEntityCollectionProperty]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='0'
        )
    )

    entity_1 = Entity('1', 1)
    entity_2 = Entity('2', 2)
    entity_3 = Entity('3', 3)

    problem: Solution = Solution([
        entity_1,
        entity_2,
        entity_3
    ])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == 0

    encountered = set()
    for entity in solution.entities:
        assert entity.value not in encountered
        encountered.add(entity.value)


def test_list_variable():
    @planning_entity
    @dataclass
    class Entity:
        code: str
        value: Annotated[List[int], PlanningListVariable] = field(default_factory=list)

    def count_mismatches(entity):
        mismatches = 0
        for index in range(len(entity.value)):
            if entity.value[index] != index + 1:
                mismatches += 1
        return mismatches

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .filter(lambda entity: any(entity.value[index] != index + 1 for index in range(len(entity.value))))
            .penalize(SimpleScore.ONE, count_mismatches)
            .as_constraint('Value is not the same as index'),
        ]

    @planning_solution
    @dataclass
    class Solution:
        entity: Annotated[Entity, PlanningEntityProperty]
        value_range: Annotated[List[int], ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='0'
        )
    )
    problem: Solution = Solution(Entity('A'), [1, 2, 3])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == 0
    assert solution.entity.value == [1, 2, 3]

def test_deep_clone_class():
    @deep_planning_clone
    @dataclass
    class Code:
        value: str
        parent_entity: 'Entity' = field(default=None)

    @dataclass
    class Value:
        code: Code

    @planning_entity
    @dataclass
    class Entity:
        code: Code
        value: Annotated[Value, PlanningVariable] = field(default=None)

    def assign_to_v1(constraint_factory: ConstraintFactory):
        return (constraint_factory.for_each(Entity)
                .filter(lambda e: e.value.code.value == 'v1')
                .reward(SimpleScore.ONE)
                .as_constraint('assign to v1')
                )

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            assign_to_v1(constraint_factory)
        ]

    @planning_solution
    @dataclass
    class Solution:
        entities: Annotated[List[Entity], PlanningEntityCollectionProperty]
        values: Annotated[List[Value], ProblemFactCollectionProperty, ValueRangeProvider]
        codes: Annotated[List[Code], ProblemFactCollectionProperty]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='2'
        )
    )

    e1 = Entity(Code('e1'))
    e1.code.parent_entity = e1
    e2 = Entity(Code('e2'))
    e2.code.parent_entity = e2

    v1 = Value(Code('v1'))
    v2 = Value(Code('v2'))

    problem = Solution([e1, e2], [v1, v2], [e1.code, e2.code, v1.code, v2.code])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)

    assert solution.score.score == 2
    assert solution.entities[0].value == v1
    assert solution.codes[0].parent_entity == solution.entities[0]
    assert solution.codes[0] is not e1.code
