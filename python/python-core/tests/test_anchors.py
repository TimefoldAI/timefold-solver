from timefold.solver import *
from timefold.solver.config import *
from timefold.solver.domain import *
from timefold.solver.score import *
from typing import Annotated


class ChainedObject:
    pass


class ChainedAnchor(ChainedObject):
    def __init__(self, code):
        self.code = code


@planning_entity
class ChainedEntity(ChainedObject):
    value: Annotated[ChainedObject, PlanningVariable(graph_type=PlanningVariableGraphType.CHAINED,
                                                     value_range_provider_refs=['chained_anchor_range',
                                                                                'chained_entity_range'])]
    anchor: Annotated[ChainedAnchor, AnchorShadowVariable(source_variable_name='value')]

    def __init__(self, code, value=None, anchor=None):
        self.code = code
        self.value = value
        self.anchor = anchor

    def __str__(self):
        return f'ChainedEntity(code={self.code}, value={self.value}, anchor={self.anchor})'


@planning_solution
class ChainedSolution:
    anchors: Annotated[
        list[ChainedAnchor], ProblemFactCollectionProperty, ValueRangeProvider(id='chained_anchor_range')]
    entities: Annotated[
        list[ChainedEntity], PlanningEntityCollectionProperty, ValueRangeProvider(id='chained_entity_range')]
    score: Annotated[SimpleScore, PlanningScore]

    def __init__(self, anchors, entities, score=None):
        self.anchors = anchors
        self.entities = entities
        self.score = score


@constraint_provider
def chained_constraints(constraint_factory: ConstraintFactory):
    return [
        constraint_factory.for_each(ChainedEntity)
        .group_by(lambda entity: entity.anchor, ConstraintCollectors.count())
        .reward(SimpleScore.ONE, lambda anchor, count: count * count)
        .as_constraint('Maximize chain length')
    ]


def test_chained():
    termination = TerminationConfig(best_score_limit='9')
    solver_config = SolverConfig(
        solution_class=ChainedSolution,
        entity_class_list=[ChainedEntity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=chained_constraints
        ),
        termination_config=termination
    )
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(ChainedSolution(
        [
            ChainedAnchor('A'),
            ChainedAnchor('B'),
            ChainedAnchor('C')
        ],
        [
            ChainedEntity('1'),
            ChainedEntity('2'),
            ChainedEntity('3'),
        ]
    ))
    assert solution.score.score == 9
    anchor = solution.entities[0].anchor
    assert anchor is not None
    anchor_value_count = 0
    for entity in solution.entities:
        if entity.value == anchor:
            anchor_value_count += 1
    assert anchor_value_count == 1
    for entity in solution.entities:
        assert entity.anchor == anchor
