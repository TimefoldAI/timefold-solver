from _jpyinterpreter import add_java_interface
from typing import TYPE_CHECKING

from ..score import Score

if TYPE_CHECKING:
    from ai.timefold.solver.core.api.domain.solution import ConstraintWeightOverrides as _JavaConstraintWeightOverrides


# We need to implement the interface so SolutionDescription detects this as an implementation.
# This is because ConstraintWeightOverrides do not use any annotations and rely on the type of
# fields.
#
# The delegate is still required despite implementing the interface,
# since `getKnownConstraintNames` returns a collection of Java Strings.
# Collection items are not converted back into Java types (since this would be
# an expensive operation that would need to be done on every Java operation and
# may break identity semantics).
@add_java_interface('ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides')
class ConstraintWeightOverrides:
    """
    Allows overriding the weights of constraints in a planning problem.
    """
    _delegate: '_JavaConstraintWeightOverrides'

    def __init__(self, overrides: dict[str, Score] = None):
        from java.util import HashMap
        from ai.timefold.solver.core.api.domain.solution import (
            ConstraintWeightOverrides as _JavaConstraintWeightOverrides)
        # Use an empty dict for overrides if empty
        overrides = overrides or {}
        java_overrides = HashMap()
        for name, override in overrides.items():
            java_overrides.put(name, override._to_java_score())
        self._delegate = _JavaConstraintWeightOverrides.of(java_overrides)

    def _to_java(self):
        return self._delegate

    if not TYPE_CHECKING:
        def getConstraintWeight(self, constraint_name: str):
            raise NotImplementedError

        def getKnownConstraintNames(self):
            raise NotImplementedError
