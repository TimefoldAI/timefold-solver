from ._solver_factory import SolverFactory
from ._solver_manager import SolverManager
from ._jpype_type_conversions import to_python_score
from .score import ScoreAnalysis, ScoreExplanation

from typing import TypeVar, Generic, TYPE_CHECKING, Any

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from .score import Score
    from ai.timefold.solver.core.api.solver import SolutionManager as _JavaSolutionManager

Solution_ = TypeVar('Solution_')
ProblemId_ = TypeVar('ProblemId_')
Score_ = TypeVar('Score_', bound='Score')
Justification_ = TypeVar('Justification_', bound='ConstraintJustification')


class SolutionManager(Generic[Solution_]):
    """
    A stateless service to help calculate `Score`, `ConstraintMatchTotal`, `Indictment`, etc.
    To create a `SolutionManager` instance, use `create`.
    """
    _delegate: '_JavaSolutionManager'

    def __init__(self, delegate: '_JavaSolutionManager'):
        self._delegate = delegate

    @staticmethod
    def create(solver_factory: SolverFactory[Solution_] | SolverManager[Solution_, Any]) -> \
            'SolutionManager[Solution_]':
        """
        Uses a `SolverFactory` or `SolverManager` to build a SolutionManager.

        Parameters
        ----------
        solver_factory : SolverFactory | SolverManager

        Returns
        -------
        SolutionManager
            A `SolutionManager` instance.
        """
        from ai.timefold.solver.core.api.solver import SolutionManager as JavaSolutionManager
        return SolutionManager(JavaSolutionManager.create(solver_factory._delegate))

    def update(self, solution: Solution_, solution_update_policy=None) -> 'Score':
        """
        Updates the given solution according to the `SolutionUpdatePolicy`.

        Parameters
        ----------
        solution : Solution_
            The solution to explain
        solution_update_policy

        Returns
        -------
        Score
            The score of the updated solution.
        """
        #  TODO handle solution_update_policy
        from _jpyinterpreter import convert_to_java_python_like_object, update_python_object_from_java
        java_solution = convert_to_java_python_like_object(solution)
        out = self._delegate.update(java_solution)
        update_python_object_from_java(java_solution)
        return to_python_score(out)

    def analyze(self, solution: Solution_, score_analysis_fetch_policy=None, solution_update_policy=None) \
            -> 'ScoreAnalysis':
        """
        Calculates and retrieves information about which constraints contributed to the solution's score.
        This is a faster, JSON-friendly version of `explain`.

        Parameters
        ----------
        solution : Solution_
            A fully initialized solution
        score_analysis_fetch_policy
        solution_update_policy

        Returns
        -------
        ScoreAnalysis
            The `ScoreAnalysis` corresponding to the given solution.
        """
        #  TODO handle policies
        from _jpyinterpreter import convert_to_java_python_like_object
        return ScoreAnalysis(self._delegate.analyze(convert_to_java_python_like_object(solution)))

    def explain(self, solution: Solution_, solution_update_policy=None) -> 'ScoreExplanation':
        """
        Calculates and retrieves ConstraintMatchTotals and Indictments necessary for
        describing the quality of a particular solution.
        For a simplified, faster and JSON-friendly alternative, see `analyze`.

        Parameters
        ----------
        solution
        solution_update_policy

        Returns
        -------
        ScoreExplanation
            The `ScoreExplanation` corresponding to the given solution.
        """
        #  TODO handle policies
        from _jpyinterpreter import convert_to_java_python_like_object
        return ScoreExplanation(self._delegate.explain(convert_to_java_python_like_object(solution)))

    def recommend_fit(self, solution: Solution_, entity_or_element, proposition_function,
                      score_analysis_fetch_policy=None):
        #  TODO
        raise NotImplementedError


__all__ = ['SolutionManager']
