"""
Classes used to configure the `Solver`.

Examples
--------
>>> from timefold.solver.config import (SolverConfig, ScoreDirectorFactoryConfig,
...                                     TerminationConfig, Duration)
>>> from domain import Timetable, Lesson
>>> from constraints import my_constraints
>>>
>>> solver_config = SolverConfig(solution_class=Timetable, entity_class_list=[Lesson],
...                              score_director_factory_config=ScoreDirectorFactoryConfig(
...                                  constraint_provider_function=my_constraints
...                                  ),
...                              termination_config=TerminationConfig(
...                                  spent_limit=Duration(seconds=30))
...                              )
"""
from ._config import *
