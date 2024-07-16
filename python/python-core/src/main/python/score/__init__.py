"""
Classes and decorators used to define constraints.

Examples
--------
>>> from timefold.solver.score import ConstraintFactory, Constraint, Joiners, HardSoftScore, constraint_provider
>>> from domain import Lesson
>>>
>>> @constraint_provider
... def timetabling_constraints(cf: ConstraintFactory) -> list[Constraint]:
...     return [
...         cf.for_each_unique_pair(Lesson,
...                                 Joiners.equal(lambda lesson: lesson.teacher),
...                                 Joiners.equal(lambda lesson: lesson.timeslot))
...           .penalize(HardSoftScore.ONE_HARD)
...           .as_constraint('Overlapping Timeslots')
...     ]
"""
from ._annotations import *
from ._constraint_builder import *
from ._constraint_factory import *
from ._constraint_stream import *
from ._function_translator import *
from ._group_by import *
from ._incremental_score_calculator import *
from ._joiners import *
from ._score import *
from ._score_analysis import *
from ._score_director import *
