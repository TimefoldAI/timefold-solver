from .._timefold_java_interop import get_class
from typing import TYPE_CHECKING, Type, TypeVar, cast
if TYPE_CHECKING:
    import jpype.imports  # noqa
    from ai.timefold.solver.core.api.score.stream import ConstraintFactory as _JavaConstraintFactory
    from ai.timefold.solver.core.api.score.stream.bi import BiJoiner


class ConstraintFactory:
    """
    The factory to create every ConstraintStream (for example with `for_each`)
    which ends in a `Constraint` returned by a function decorated with `constraint_provider`.
    """
    delegate: '_JavaConstraintFactory'
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    def __init__(self, delegate: '_JavaConstraintFactory'):
        self.delegate = delegate

    def get_default_constraint_package(self) -> str:
        """
        This is `constraint_configuration(constraint_package=...)` if available,
        otherwise the module of the `constraint_provider` function.
        """
        return self.delegate.getDefaultConstraintPackage()

    def for_each(self, source_class: Type[A_]) -> 'UniConstraintStream[A_]':
        """
        Start a ConstraintStream of all instances of the `source_class`
        that are known as problem facts or planning entities.
        If the `source_class` is a `planning_entity`,
        then it is automatically filtered to only contain entities for which each genuine `PlanningVariable`
        (of the `source_class` or a superclass thereof) has a non-None value.

        If the `source_class` is a shadow entity (an entity without any genuine planning variables),
        and if there exists a genuine `planning_entity` with a `PlanningListVariable` which accepts instances of this
        shadow entity as values in that list, and if that list variable allows unassigned values, then this stream will
        filter out all `source_class` instances which are not present in any instances of that list variable.
        This is achieved in one of two ways:

        - If the `source_class`
          has `InverseRelationShadowVariable` field referencing instance of an entity with the list variable,
          the value of that field will be used to determine if the value is assigned.
          ``None`` in that field means the instance of `source_class` is unassigned.

        - As fallback,
          the value is considered assigned if there exists an instance of the entity
          where its list variable contains the value.
          This will perform significantly worse
          and only exists so that using the `InverseRelationShadowVariable` can remain optional.
          Adding the field is strongly recommended.
        """
        source_class = get_class(source_class)
        return UniConstraintStream(self.delegate.forEach(source_class), self.get_default_constraint_package(),
                                   cast(Type['A_'], source_class))

    def for_each_including_unassigned(self, source_class: Type[A_]) -> 'UniConstraintStream[A_]':
        """
        As defined by `for_each`,
        but without any filtering of unassigned planning entities
        (for ``PlanningVariable(allows_unassigned=True)``)
        or shadow entities not assigned to any applicable list variable
        (for ``PlanningListVariable(allows_unassigned_values=True)``).
        """
        source_class = get_class(source_class)
        return UniConstraintStream(self.delegate.forEachIncludingUnassigned(source_class),
                                   self.get_default_constraint_package(),
                                   cast(Type['A_'], source_class))

    def for_each_unique_pair(self, source_class: Type[A_], *joiners: 'BiJoiner[A_, A_]') -> \
            'BiConstraintStream[A_, A_]':
        """
        Create a new `BiConstraintStream` for every unique combination of A and another A with a higher `PlanningId`
        for which every `BiJoiner` is true (for the properties it extracts from both facts).
        """
        source_class = get_class(source_class)
        return BiConstraintStream(self.delegate.forEachUniquePair(source_class,
                                                                  extract_joiners(joiners, source_class, source_class)),
                                  self.get_default_constraint_package(),
                                  cast(Type['A_'], source_class),
                                  cast(Type['A_'], source_class))


from ._constraint_stream import *
from ._joiners import extract_joiners

__all__ = [
    'ConstraintFactory'
]
