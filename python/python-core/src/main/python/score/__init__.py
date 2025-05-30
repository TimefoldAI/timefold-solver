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
from .standard_shift_constraints import define_standard_shift_constraints

__all__ = [
    # from ._annotations
    'constraint_provider',
    # from ._constraint_factory
    'ConstraintFactory',
    # from ._constraint_builder
    'ConstraintBuilder', # Note: Or specific builders if preferred
    # from ._constraint_stream
    'ConstraintStream', # Or specific stream types
    # from ._joiners
    'Joiners',
    # from ._score
    'HardSoftScore', 'SimpleScore', 'HardMediumSoftScore', # Add other score types as needed
    # from ._incremental_score_calculator
    'IncrementalScoreCalculator',
    # from current file
    'define_standard_shift_constraints'
    # Potentially others from _score_director, _score_analysis depending on typical public API
]
