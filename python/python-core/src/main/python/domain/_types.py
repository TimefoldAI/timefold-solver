from ..score import Score
from typing import Dict, Generic, Type, TypeVar, TYPE_CHECKING

if TYPE_CHECKING:
    from ai.timefold.solver.core.api.domain.solution import ConstraintWeightOverrides as _JavaConstraintWeightOverrides

Score_ = TypeVar('Score_', bound='Score')

class ConstraintWeightOverrides(Generic[Score_]):
    """
    Alows to override the weights of constraints in a planning problem.
    """
    delegate: '_JavaConstraintWeightOverrides[Score_]'
    package: str
    score_type: Type[Score_]

    def __init__(self, delegate: '_JavaConstraintWeightOverrides[Score_]'):
        self.delegate = delegate