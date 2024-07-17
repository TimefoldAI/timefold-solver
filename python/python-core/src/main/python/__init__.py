"""
`Timefold Solver <https://timefold.ai/>`_ is a lightweight,
embeddable constraint satisfaction engine which optimizes planning problems.

It solves use cases such as:

 - Employee shift rostering: timetabling nurses, repairmen, ...

 - Vehicle routing: planning vehicle routes for moving freight and/or passengers through
   multiple destinations using known mapping tools ...

 - Agenda scheduling: scheduling meetings, appointments, maintenance jobs, advertisements, ...


Planning problems are defined using Python classes and functions.

Examples
--------
>>> from timefold.solver import Solver, SolverFactory
>>> from timefold.solver.config import (SolverConfig, ScoreDirectorFactoryConfig,
...                                     TerminationConfig, Duration)
>>> from domain import Timetable, Lesson, generate_problem
>>> from constraints import my_constraints
...
>>> solver_config = SolverConfig(solution_class=Timetable, entity_class_list=[Lesson],
...                              score_director_factory_config=ScoreDirectorFactoryConfig(
...                                  constraint_provider_function=my_constraints
...                                  ),
...                              termination_config=TerminationConfig(
...                                  spent_limit=Duration(seconds=30))
...                              )
>>> solver = SolverFactory.create(solver_config).build_solver()
>>> problem = generate_problem()
>>> solution = solver.solve(problem)

See Also
--------
:mod:`timefold.solver.config`
:mod:`timefold.solver.domain`
:mod:`timefold.solver.score`
:mod:`timefold.solver.test`
"""
from ._problem_change import *
from ._solution_manager import *
from ._solver import *
from ._solver_factory import *
from ._solver_manager import *

import timefold.solver.config as config
import timefold.solver.domain as domain
import timefold.solver.heuristic as heuristic
import timefold.solver.score as score
import timefold.solver.test as test

from ._timefold_java_interop import init, set_class_output_directory
