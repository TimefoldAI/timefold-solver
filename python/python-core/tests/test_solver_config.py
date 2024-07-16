import pathlib
import pytest
import re
from dataclasses import dataclass, field
from timefold.solver.config import *
from timefold.solver.domain import *
from timefold.solver.score import *
from typing import Annotated, List

test_directory = pathlib.Path(__file__).resolve().parent

class Value:
    def __init__(self, code):
        self.code = code


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
class Solution:
    entity: Annotated[Entity, PlanningEntityProperty]
    value: Annotated[Value, ProblemFactProperty]
    value_range: Annotated[List[str], ValueRangeProvider]
    score: Annotated[SimpleScore, PlanningScore]


def get_java_solver_config(path: pathlib.Path):
    return SolverConfig.create_from_xml_resource(path)._to_java_solver_config()


def test_load_from_solver_config_file():
    from _jpyinterpreter import get_java_type_for_python_type
    solver_config = get_java_solver_config(test_directory / 'solverConfig-simple.xml')
    assert solver_config.getSolutionClass() == get_java_type_for_python_type(Solution).getJavaClass()
    entity_class_list = solver_config.getEntityClassList()
    assert entity_class_list.size() == 1
    assert entity_class_list.get(0) == get_java_type_for_python_type(Entity).getJavaClass()
    assert solver_config.getScoreDirectorFactoryConfig().getConstraintProviderClass() == \
           my_constraints._timefold_java_class  # noqa
    assert solver_config.getTerminationConfig().getBestScoreLimit() == '0hard/0soft'


def test_reload_from_solver_config_file():
    from _jpyinterpreter import get_java_type_for_python_type

    @planning_solution
    class RedefinedSolution:
        ...

    RedefinedSolution1 = RedefinedSolution
    solver_config_1 = get_java_solver_config(test_directory / 'solverConfig-redefined.xml')

    @planning_solution
    class RedefinedSolution:
        ...

    RedefinedSolution2 = RedefinedSolution
    solver_config_2 = get_java_solver_config(test_directory / 'solverConfig-redefined.xml')

    assert solver_config_1.getSolutionClass() == get_java_type_for_python_type(RedefinedSolution1).getJavaClass()
    assert solver_config_2.getSolutionClass() == get_java_type_for_python_type(RedefinedSolution2).getJavaClass()


def test_cannot_find_solver_config_file():
    from java.lang import Thread
    current_thread = Thread.currentThread()
    thread_class_loader = current_thread.getContextClassLoader()
    with pytest.raises(FileNotFoundError, match=re.escape("The solverConfigFile (does-not-exist.xml) was not found.")):
        get_java_solver_config(pathlib.Path('does-not-exist.xml'))
    assert current_thread.getContextClassLoader() == thread_class_loader
