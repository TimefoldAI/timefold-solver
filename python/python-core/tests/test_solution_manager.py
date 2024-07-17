import inspect
import re
from ai.timefold.solver.core.api.score import ScoreExplanation as JavaScoreExplanation
from ai.timefold.solver.core.api.score.analysis import (
    ConstraintAnalysis as JavaConstraintAnalysis,
    ScoreAnalysis as JavaScoreAnalysis)
from ai.timefold.solver.core.api.score.constraint import (ConstraintRef as JavaConstraintRef,
                                                          ConstraintMatch as JavaConstraintMatch,
                                                          ConstraintMatchTotal as JavaConstraintMatchTotal)
from ai.timefold.solver.core.api.score.constraint import Indictment as JavaIndictment
from dataclasses import dataclass, field
from timefold.solver import *
from timefold.solver.config import *
from timefold.solver.domain import *
from timefold.solver.score import *
from typing import Annotated, List


@planning_entity
@dataclass(eq=True, unsafe_hash=True)
class Entity:
    code: Annotated[str, PlanningId] = field(hash=True)
    value: Annotated[int, PlanningVariable] = field(default=None, hash=False, compare=False)


@constraint_provider
def my_constraints(constraint_factory: ConstraintFactory):
    return [
        constraint_factory.for_each(Entity)
        .reward(SimpleScore.ONE, lambda entity: entity.value)
        .as_constraint('Maximize Value'),
    ]


@planning_solution
@dataclass
class Solution:
    entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
    value_range: Annotated[List[int], ValueRangeProvider]
    score: Annotated[SimpleScore, PlanningScore] = field(default=None)


solver_config = SolverConfig(
    solution_class=Solution,
    entity_class_list=[Entity],
    score_director_factory_config=ScoreDirectorFactoryConfig(
        constraint_provider_function=my_constraints
    )
)


def assert_score_explanation(problem: Solution,
                             score_explanation: ScoreExplanation[Solution]):
    assert score_explanation.solution is problem
    assert score_explanation.score.score == 3

    constraint_ref = ConstraintRef(package_name='tests.test_solution_manager',
                                   constraint_name='Maximize Value')
    constraint_match_total_map = score_explanation.constraint_match_total_map
    assert constraint_match_total_map == {
        constraint_ref.constraint_id: ConstraintMatchTotal(
            constraint_ref=constraint_ref,
            constraint_match_count=3,
            constraint_weight=SimpleScore.ONE,
            score=SimpleScore.of(3),
            constraint_match_set={
                ConstraintMatch(
                    constraint_ref=constraint_ref,
                    justification=DefaultConstraintJustification(
                        facts=(entity,),
                        impact=SimpleScore.ONE
                    ),
                    indicted_objects=(entity,),
                    score=SimpleScore.ONE
                ) for entity in problem.entity_list
            }
        )
    }

    indictment_map = score_explanation.indictment_map
    for entity in problem.entity_list:
        indictment = indictment_map[entity]
        assert indictment.indicted_object is entity
        assert indictment.score == SimpleScore.ONE
        assert indictment.constraint_match_count == 1
        assert indictment.constraint_match_set == {
            ConstraintMatch(
                constraint_ref=constraint_ref,
                justification=DefaultConstraintJustification(
                    facts=(entity,),
                    impact=SimpleScore.ONE
                ),
                indicted_objects=(entity,),
                score=SimpleScore.ONE
            )
        }

    assert constraint_ref.constraint_name in score_explanation.summary
    assert 'Entity' in score_explanation.summary


def assert_constraint_analysis(problem: Solution, constraint_analysis: ConstraintAnalysis):
    constraint_ref = ConstraintRef(package_name='tests.test_solution_manager',
                                   constraint_name='Maximize Value')
    assert constraint_analysis.score.score == 3
    assert constraint_analysis.weight.score == 1
    assert constraint_analysis.constraint_name == constraint_ref.constraint_name
    assert constraint_analysis.constraint_package == constraint_ref.package_name
    assert constraint_analysis.constraint_ref == constraint_ref

    matches = constraint_analysis.matches
    assert len(matches) == 3
    for entity in problem.entity_list:
        for match in constraint_analysis.matches:
            if match.justification.facts[0] is entity:
                assert match.score == SimpleScore.ONE
                assert match.constraint_ref == constraint_ref
                assert match.justification.facts == (entity,)
                assert match.justification.impact == SimpleScore.ONE
                break
        else:
            raise AssertionError(f'Entity {entity} does not have a match')


def assert_score_analysis(problem: Solution, score_analysis: ScoreAnalysis):
    constraint_ref = ConstraintRef(package_name='tests.test_solution_manager',
                                   constraint_name='Maximize Value')
    assert score_analysis.score.score == 3

    constraint_map = score_analysis.constraint_map
    assert len(constraint_map) == 1

    constraint_analysis = score_analysis.constraint_map[constraint_ref]
    assert_constraint_analysis(problem, constraint_analysis)

    constraint_analyses = score_analysis.constraint_analyses
    assert len(constraint_analyses) == 1
    constraint_analysis = constraint_analyses[0]
    assert_constraint_analysis(problem, constraint_analysis)


def assert_score_analysis_summary(score_analysis: ScoreAnalysis):
    summary = score_analysis.summary
    assert "Explanation of score (3):" in summary
    assert "Constraint matches:" in summary
    assert "3: constraint (Maximize Value) has 3 matches:" in summary
    assert "1: justified with" in summary

    summary_str = str(score_analysis)
    assert summary == summary_str

    match = score_analysis.constraint_analyses[0]
    match_summary = match.summary
    assert "Explanation of score (3):" in match_summary
    assert "Constraint matches:" in match_summary
    assert "3: constraint (Maximize Value) has 3 matches:" in match_summary
    assert "1: justified with" in match_summary

    match_summary_str = str(match)
    assert match_summary == match_summary_str


def assert_solution_manager(solution_manager: SolutionManager[Solution]):
    problem: Solution = Solution([Entity('A', 1), Entity('B', 1), Entity('C', 1)], [1, 2, 3])
    assert problem.score is None
    score = solution_manager.update(problem)
    assert score.score == 3
    assert problem.score.score == 3

    score_explanation = solution_manager.explain(problem)
    assert_score_explanation(problem, score_explanation)

    score_analysis = solution_manager.analyze(problem)
    assert_score_analysis(problem, score_analysis)

    score_analysis = solution_manager.analyze(problem)
    assert_score_analysis_summary(score_analysis)


def test_solver_manager_score_manager():
    with SolverManager.create(SolverFactory.create(solver_config)) as solver_manager:
        assert_solution_manager(SolutionManager.create(solver_manager))


def test_solver_factory_score_manager():
    assert_solution_manager(SolutionManager.create(SolverFactory.create(solver_config)))


def test_score_manager_solution_initialization():
    solution_manager = SolutionManager.create(SolverFactory.create(solver_config))
    problem: Solution = Solution([Entity('A', 1), Entity('B', 1), Entity('C', 1)], [1, 2, 3])
    score_analysis = solution_manager.analyze(problem)
    assert score_analysis.is_solution_initialized

    second_problem: Solution = Solution([Entity('A', None), Entity('B', None), Entity('C', None)], [1, 2, 3])
    second_score_analysis = solution_manager.analyze(second_problem)
    assert not second_score_analysis.is_solution_initialized


def test_score_manager_diff():
    solution_manager = SolutionManager.create(SolverFactory.create(solver_config))
    problem: Solution = Solution([Entity('A', 1), Entity('B', 1), Entity('C', 1)], [1, 2, 3])
    score_analysis = solution_manager.analyze(problem)
    second_problem: Solution = Solution([Entity('A', 1), Entity('B', 1), Entity('C', 1), Entity('D', 1)], [1, 2, 3])
    second_score_analysis = solution_manager.analyze(second_problem)
    diff = score_analysis.diff(second_score_analysis)
    assert diff.score.score == -1

    diff_operation = score_analysis - second_score_analysis
    assert diff_operation.score.score == -1

    constraint_analyses = score_analysis.constraint_analyses
    assert len(constraint_analyses) == 1


def test_score_manager_constraint_analysis_map():
    solution_manager = SolutionManager.create(SolverFactory.create(solver_config))
    problem: Solution = Solution([Entity('A', 1), Entity('B', 1), Entity('C', 1)], [1, 2, 3])
    score_analysis = solution_manager.analyze(problem)
    constraints = score_analysis.constraint_analyses
    assert len(constraints) == 1

    constraint_analysis = (
        score_analysis.constraint_analysis('tests.test_solution_manager', 'Maximize Value'))
    assert constraint_analysis.constraint_name == 'Maximize Value'

    constraint_ref = ConstraintRef('tests.test_solution_manager', 'Maximize Value')
    constraint_analysis = score_analysis.constraint_analysis(constraint_ref)
    assert constraint_analysis.constraint_name == 'Maximize Value'
    assert constraint_analysis.match_count == 3


def test_score_manager_constraint_ref():
    constraint_ref = ConstraintRef.parse_id('tests.test_solution_manager/Maximize Value')

    assert constraint_ref.package_name == 'tests.test_solution_manager'
    assert constraint_ref.constraint_name == 'Maximize Value'


ignored_java_functions = {
    'equals',
    'getClass',
    'hashCode',
    'notify',
    'notifyAll',
    'toString',
    'wait',
    'compareTo',
}

ignored_java_functions_per_class = {
    'Indictment': {'getJustification'},  # deprecated
    'ConstraintRef': {'of', 'packageName', 'constraintName'},  # built-in constructor and properties with @dataclass
    'ConstraintAnalysis': {'summarize'},  # using summary instead
    'ScoreAnalysis': {'summarize'},  # using summary instead
    'ConstraintMatch': {
        'getConstraintRef',  # built-in constructor and properties with @dataclass
        'getConstraintPackage',  # deprecated
        'getConstraintName',  # deprecated
        'getConstraintId',  # deprecated
        'getJustificationList',  # deprecated
        'getJustification',  # built-in constructor and properties with @dataclass
        'getScore',  # built-in constructor and properties with @dataclass
        'getIndictedObjectList',  # built-in constructor and properties with @dataclass
    },
    'ConstraintMatchTotal': {
        'getConstraintRef',  # built-in constructor and properties with @dataclass
        'composeConstraintId',  # deprecated
        'getConstraintPackage',  # deprecated
        'getConstraintName',  # deprecated
        'getConstraintId',  # deprecated
        'getConstraintMatchCount',  # built-in constructor and properties with @dataclass
        'getConstraintMatchSet',  # built-in constructor and properties with @dataclass
        'getConstraintWeight',  # built-in constructor and properties with @dataclass
        'getScore',  # built-in constructor and properties with @dataclass
    },
}


def test_has_all_methods():
    missing = []
    for python_type, java_type in ((ScoreExplanation, JavaScoreExplanation),
                                   (ScoreAnalysis, JavaScoreAnalysis),
                                   (ConstraintAnalysis, JavaConstraintAnalysis),
                                   (ScoreExplanation, JavaScoreExplanation),
                                   (ConstraintMatch, JavaConstraintMatch),
                                   (ConstraintMatchTotal, JavaConstraintMatchTotal),
                                   (ConstraintRef, JavaConstraintRef),
                                   (Indictment, JavaIndictment)):
        type_name = python_type.__name__
        ignored_java_functions_type = ignored_java_functions_per_class[
            type_name] if type_name in ignored_java_functions_per_class else {}

        for function_name, function_impl in inspect.getmembers(java_type, inspect.isfunction):
            if function_name in ignored_java_functions or function_name in ignored_java_functions_type:
                continue

            snake_case_name = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', function_name)
            snake_case_name = re.sub('([a-z0-9])([A-Z])', r'\1_\2', snake_case_name).lower()
            snake_case_name_without_prefix = re.sub('(.)([A-Z][a-z]+)', r'\1_\2',
                                                    function_name[3:] if function_name.startswith(
                                                        "get") else function_name)
            snake_case_name_without_prefix = re.sub('([a-z0-9])([A-Z])', r'\1_\2',
                                                    snake_case_name_without_prefix).lower()
            if not hasattr(python_type, snake_case_name) and not hasattr(python_type, snake_case_name_without_prefix):
                missing.append((java_type, python_type, snake_case_name))

    if missing:
        assertion_msg = ''
        for java_type, python_type, snake_case_name in missing:
            assertion_msg += f'{python_type} is missing a method ({snake_case_name}) from java_type ({java_type}).)\n'
        raise AssertionError(assertion_msg)
