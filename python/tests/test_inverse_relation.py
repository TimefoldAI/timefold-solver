from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.config import *
from timefold.solver.score import *

from dataclasses import dataclass, field
from typing import Annotated, List


class BaseClass:
    pass


@planning_entity
@dataclass
class InverseRelationEntity:
    code: str
    value: Annotated[BaseClass, PlanningVariable(value_range_provider_refs=['value_range'])] = \
        field(default=None)

    def __hash__(self):
        return hash(self.code)


@planning_entity
@dataclass
class InverseRelationValue(BaseClass):
    code: str
    entities: Annotated[List[InverseRelationEntity],
                        InverseRelationShadowVariable(source_variable_name='value')] = \
        field(default_factory=list)


@planning_solution
@dataclass
class InverseRelationSolution:
    values: Annotated[List[InverseRelationValue],
                      PlanningEntityCollectionProperty,
                      ValueRangeProvider(id='value_range')]
    entities: Annotated[List[InverseRelationEntity],
                        PlanningEntityCollectionProperty]
    score: Annotated[SimpleScore, PlanningScore] = field(default=None)


@constraint_provider
def inverse_relation_constraints(constraint_factory: ConstraintFactory):
    return [
        constraint_factory.for_each(InverseRelationValue)
                          .filter(lambda value: len(value.entities) > 1)
                          .penalize(SimpleScore.ONE)
                          .as_constraint('Only one entity per value')
    ]


def test_inverse_relation():
    solver_config = SolverConfig(
        solution_class=InverseRelationSolution,
        entity_class_list=[InverseRelationEntity, InverseRelationValue],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=inverse_relation_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='0'
        )
    )
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(InverseRelationSolution(
        [
            InverseRelationValue('A'),
            InverseRelationValue('B'),
            InverseRelationValue('C')
        ],
        [
            InverseRelationEntity('1'),
            InverseRelationEntity('2'),
            InverseRelationEntity('3'),
        ]
    ))
    assert solution.score.score == 0
    visited_set = set()
    for value in solution.values:
        assert len(value.entities) == 1
        assert value.entities[0] is not None
        assert value.entities[0] not in visited_set
        visited_set.add(value.entities[0])
