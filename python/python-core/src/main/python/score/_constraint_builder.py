import timefold.solver.score as score_api
from .._jpype_type_conversions import to_python_score
from ._function_translator import function_cast
from ..score._score import Score
from typing import TypeVar, Callable, Generic, Collection, TYPE_CHECKING, Type

if TYPE_CHECKING:
    import jpype.imports
    from ai.timefold.solver.core.api.score.stream.uni import UniConstraintBuilder as _JavaUniConstraintBuilder
    from ai.timefold.solver.core.api.score.stream.bi import BiConstraintBuilder as _JavaBiConstraintBuilder
    from ai.timefold.solver.core.api.score.stream.tri import TriConstraintBuilder as _JavaTriConstraintBuilder
    from ai.timefold.solver.core.api.score.stream.quad import QuadConstraintBuilder as _JavaQuadConstraintBuilder


A = TypeVar('A')
B = TypeVar('B')
C = TypeVar('C')
D = TypeVar('D')
ScoreType = TypeVar('ScoreType', bound=Score)


class Constraint:
    """
    This represents a single constraint in the ConstraintStream API that impacts the Score.
    It is defined in a function decorated by `constraint_provider` by calling `ConstraintFactory.for_each`.
    """
    ...


class UniConstraintBuilder(Generic[A, ScoreType]):
    """
    Used to build a `Constraint` out of a `UniConstraintStream`, applying optional configuration.
    To build the constraint, use one of the terminal operations, such as `as_constraint`.

    Unless `justify_with` is called, the default justification mapping will be used.
    The function takes the input arguments and score and converts them to a `DefaultConstraintJustification`.

    Unless `indict_with` is called, the default indicted objects' mapping will be used.
    The function takes the input arguments and converts them into a `list`.
    """
    delegate: '_JavaUniConstraintBuilder[A, ScoreType]'
    a_type: Type[A]

    def __init__(self, delegate: '_JavaUniConstraintBuilder[A, ScoreType]',
                 a_type: Type[A]) -> None:
        self.delegate = delegate
        self.a_type = a_type

    def indict_with(self, indictment_function: Callable[[A], Collection]) -> 'UniConstraintBuilder[A, ScoreType]':
        """
        Sets a custom function to mark any object returned by it as responsible for causing the constraint to match.
        Each object in the collection
        returned by this function will become an `Indictment` and be available as a key in
        `ScoreExplanation.indictment_map`.

        Parameters
        ----------
        indictment_function : Callable[[A], Collection]
            the function that returns the indicted objects.

        Returns
        -------
        UniConstraintBuilder
            this `UniConstraintBuilder`.
        """
        return UniConstraintBuilder(self.delegate.indictWith(
            function_cast(indictment_function, self.a_type)), self.a_type)

    def justify_with(self, justification_function: Callable[[A, ScoreType], 'score_api.ConstraintJustification']) -> \
            'UniConstraintBuilder[A, ScoreType]':
        """
        Sets a custom function to apply on a constraint match to justify it.
        That function must not return a `Collection`,
        else a RuntimeError will be raised during score calculation.

        Parameters
        ----------
        justification_function : Callable[[A, ScoreType], ConstraintJustification]
            the function that returns the justification.

        Returns
        -------
        UniConstraintBuilder
            this `UniConstraintBuilder`.
        """
        from ai.timefold.solver.core.api.score import Score

        def wrapped(a, score):
            return justification_function(a, to_python_score(score))

        return UniConstraintBuilder(self.delegate.justifyWith(
            function_cast(wrapped, self.a_type, Score)), self.a_type)

    def as_constraint(self, constraint_name: str, constraint_description: str = None) \
            -> Constraint:
        """
        Builds a Constraint from the constraint stream.

        Parameters
        ----------
        constraint_name : str
            This is the constraint name.
            The constraint package defaults to the module of the `planning_solution` class.

        constraint_description : str, optional
            The constraint description.
            Empty string if not present.

        Returns
        -------
        Constraint
            A `Constraint`.
        """
        if constraint_description is None:
            return self.delegate.asConstraint(constraint_name)
        else:
            return self.delegate.asConstraintDescribed(constraint_name, constraint_description)


class BiConstraintBuilder(Generic[A, B, ScoreType]):
    """
    Used to build a `Constraint` out of a `UniConstraintStream`, applying optional configuration.
    To build the constraint, use one of the terminal operations, such as `as_constraint`.

    Unless `justify_with` is called, the default justification mapping will be used.
    The function takes the input arguments and score and converts them to a `DefaultConstraintJustification`.

    Unless `indict_with` is called, the default indicted objects' mapping will be used.
    The function takes the input arguments and converts them into a `list`.
    """
    delegate: '_JavaBiConstraintBuilder[A, B, ScoreType]'
    a_type: Type[A]
    b_type: Type[B]

    def __init__(self, delegate: '_JavaBiConstraintBuilder[A, B, ScoreType]',
                 a_type: Type[A], b_type: Type[B]) -> None:
        self.delegate = delegate
        self.a_type = a_type
        self.b_type = b_type

    def indict_with(self, indictment_function: Callable[[A, B], Collection]) -> 'BiConstraintBuilder[A, B, ScoreType]':
        """
        Sets a custom function to mark any object returned by it as responsible for causing the constraint to match.
        Each object in the collection
        returned by this function will become an `Indictment` and be available as a key in
        `ScoreExplanation.indictment_map`.

        Parameters
        ----------
        indictment_function : Callable[[A, B], Collection]
            the function that returns the indicted objects.

        Returns
        -------
        BiConstraintBuilder
            this `BiConstraintBuilder`.
        """
        return BiConstraintBuilder(self.delegate.indictWith(
            function_cast(indictment_function, self.a_type, self.b_type)), self.a_type, self.b_type)

    def justify_with(self, justification_function: Callable[[A, B, ScoreType],
                                                            'score_api.ConstraintJustification']) -> \
            'BiConstraintBuilder[A, B, ScoreType]':
        """
        Sets a custom function to apply on a constraint match to justify it.
        That function must not return a `Collection`,
        else a RuntimeError will be raised during score calculation.

        Parameters
        ----------
        justification_function : Callable[[A, B, ScoreType], ConstraintJustification]
            the function that returns the justification.

        Returns
        -------
        BiConstraintBuilder
            this `BiConstraintBuilder`.
        """
        from ai.timefold.solver.core.api.score import Score

        def wrapped(a, b, score):
            return justification_function(a, b, to_python_score(score))

        return BiConstraintBuilder(self.delegate.justifyWith(
            function_cast(wrapped, self.a_type, self.b_type, Score)), self.a_type, self.b_type)

    def as_constraint(self, constraint_name: str, constraint_description: str = None) \
            -> Constraint:
        """
        Builds a Constraint from the constraint stream.

        Parameters
        ----------
        constraint_name : str
            This is the constraint name.
            The constraint package defaults to the module of the `planning_solution` class.

        constraint_description : str, optional
            The constraint description.
            Empty string if not present.

        Returns
        -------
        Constraint
            A `Constraint`.
        """
        if constraint_description is None:
            return self.delegate.asConstraint(constraint_name)
        else:
            return self.delegate.asConstraintDescribed(constraint_name, constraint_description)


class TriConstraintBuilder(Generic[A, B, C, ScoreType]):
    """
    Used to build a `Constraint` out of a `UniConstraintStream`, applying optional configuration.
    To build the constraint, use one of the terminal operations, such as `as_constraint`.

    Unless `justify_with` is called, the default justification mapping will be used.
    The function takes the input arguments and score and converts them to a `DefaultConstraintJustification`.

    Unless `indict_with` is called, the default indicted objects' mapping will be used.
    The function takes the input arguments and converts them into a `list`.
    """
    delegate: '_JavaTriConstraintBuilder[A, B, C, ScoreType]'
    a_type: Type[A]
    b_type: Type[B]
    c_type: Type[C]

    def __init__(self, delegate: '_JavaTriConstraintBuilder[A, B, C, ScoreType]',
                 a_type: Type[A], b_type: Type[B], c_type: Type[C]) -> None:
        self.delegate = delegate
        self.a_type = a_type
        self.b_type = b_type
        self.c_type = c_type

    def indict_with(self, indictment_function: Callable[[A, B, C], Collection]) -> \
            'TriConstraintBuilder[A, B, C, ScoreType]':
        """
        Sets a custom function to mark any object returned by it as responsible for causing the constraint to match.
        Each object in the collection
        returned by this function will become an `Indictment` and be available as a key in
        `ScoreExplanation.indictment_map`.

        Parameters
        ----------
        indictment_function : Callable[[A, B, C], Collection]
            the function that returns the indicted objects.

        Returns
        -------
        TriConstraintBuilder
            this `TriConstraintBuilder`.
        """
        return TriConstraintBuilder(self.delegate.indictWith(
            function_cast(indictment_function, self.a_type, self.b_type, self.c_type)), self.a_type, self.b_type,
                                    self.c_type)

    def justify_with(self, justification_function: Callable[[A, B, C, ScoreType],
                                                            'score_api.ConstraintJustification']) -> \
            'TriConstraintBuilder[A, B, C, ScoreType]':
        """
        Sets a custom function to apply on a constraint match to justify it.
        That function must not return a `Collection`,
        else a RuntimeError will be raised during score calculation.

        Parameters
        ----------
        justification_function : Callable[[A, B, C, ScoreType], ConstraintJustification]
            the function that returns the justification.

        Returns
        -------
        TriConstraintBuilder
            this `TriConstraintBuilder`.
        """
        from ai.timefold.solver.core.api.score import Score

        def wrapped(a, b, c, score):
            return justification_function(a, b, c, to_python_score(score))

        return TriConstraintBuilder(self.delegate.justifyWith(
            function_cast(wrapped, self.a_type, self.b_type, self.c_type, Score)),
            self.a_type, self.b_type, self.c_type)

    def as_constraint(self, constraint_name: str, constraint_description: str = None) \
            -> Constraint:
        """
        Builds a Constraint from the constraint stream.

        Parameters
        ----------
        constraint_name : str
            This is the constraint name.
            The constraint package defaults to the module of the `planning_solution` class.

        constraint_description : str, optional
            The constraint description.
            Empty string if not present.

        Returns
        -------
        Constraint
            A `Constraint`.
        """
        if constraint_description is None:
            return self.delegate.asConstraint(constraint_name)
        else:
            return self.delegate.asConstraintDescribed(constraint_name, constraint_description)


class QuadConstraintBuilder(Generic[A, B, C, D, ScoreType]):
    """
    Used to build a `Constraint` out of a `UniConstraintStream`, applying optional configuration.
    To build the constraint, use one of the terminal operations, such as `as_constraint`.

    Unless `justify_with` is called, the default justification mapping will be used.
    The function takes the input arguments and score and converts them to a `DefaultConstraintJustification`.

    Unless `indict_with` is called, the default indicted objects' mapping will be used.
    The function takes the input arguments and converts them into a `list`.
    """
    delegate: '_JavaQuadConstraintBuilder[A, B, C, D, ScoreType]'
    a_type: Type[A]
    b_type: Type[B]
    c_type: Type[C]
    d_type: Type[D]

    def __init__(self, delegate: '_JavaQuadConstraintBuilder[A, B, C, D, ScoreType]',
                 a_type: Type[A], b_type: Type[B], c_type: Type[C], d_type: Type[D]) -> None:
        self.delegate = delegate
        self.a_type = a_type
        self.b_type = b_type
        self.c_type = c_type
        self.d_type = d_type

    def indict_with(self, indictment_function: Callable[[A, B, C, D], Collection]) -> \
            'QuadConstraintBuilder[A, B, C, D, ScoreType]':
        """
        Sets a custom function to mark any object returned by it as responsible for causing the constraint to match.
        Each object in the collection
        returned by this function will become an `Indictment` and be available as a key in
        `ScoreExplanation.indictment_map`.

        Parameters
        ----------
        indictment_function : Callable[[A, B, C, D], Collection]
            the function that returns the indicted objects.

        Returns
        -------
        QuadConstraintBuilder
            this `QuadConstraintBuilder`.
        """
        return QuadConstraintBuilder(self.delegate.indictWith(
            function_cast(indictment_function, self.a_type, self.b_type, self.c_type, self.d_type)),
            self.a_type, self.b_type, self.c_type, self.d_type)

    def justify_with(self, justification_function: Callable[[A, B, C, D, ScoreType],
                                                            'score_api.ConstraintJustification']) \
            -> 'QuadConstraintBuilder[A, B, C, D, ScoreType]':
        """
        Sets a custom function to apply on a constraint match to justify it.
        That function must not return a `Collection`,
        else a RuntimeError will be raised during score calculation.

        Parameters
        ----------
        justification_function : Callable[[A, B, C, D, ScoreType], ConstraintJustification]
            the function that returns the justification.

        Returns
        -------
        QuadConstraintBuilder
            this `QuadConstraintBuilder`.
        """
        from ai.timefold.solver.core.api.score import Score

        def wrapped(a, b, c, d, score):
            return justification_function(a, b, c, d, to_python_score(score))

        return QuadConstraintBuilder(self.delegate.justifyWith(
            function_cast(wrapped, self.a_type, self.b_type, self.c_type, self.d_type, Score)),
            self.a_type, self.b_type, self.c_type, self.d_type)

    def as_constraint(self, constraint_name: str, constraint_description: str = None) \
            -> Constraint:
        """
        Builds a Constraint from the constraint stream.

        Parameters
        ----------
        constraint_name : str
            This is the constraint name.
            The constraint package defaults to the module of the `planning_solution` class.

        constraint_description : str, optional
            The constraint description.
            Empty string if not present.

        Returns
        -------
        Constraint
            A `Constraint`.
        """
        if constraint_description is None:
            return self.delegate.asConstraint(constraint_name)
        else:
            return self.delegate.asConstraintDescribed(constraint_name, constraint_description)


__all__ = ['Constraint',
           'UniConstraintBuilder', 'BiConstraintBuilder', 'TriConstraintBuilder', 'QuadConstraintBuilder']
