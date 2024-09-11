import dataclasses
from typing import Callable, Any, Sequence, TypeVar, List, Set, Dict, TYPE_CHECKING, overload

from jpype import JClass

if TYPE_CHECKING:
    from ai.timefold.solver.core.api.score.stream.common import SequenceChain
    from ai.timefold.solver.core.api.score.stream.common import LoadBalance
    from ai.timefold.solver.core.api.score.stream.uni import UniConstraintCollector
    from ai.timefold.solver.core.api.score.stream.bi import BiConstraintCollector
    from ai.timefold.solver.core.api.score.stream.tri import TriConstraintCollector
    from ai.timefold.solver.core.api.score.stream.quad import QuadConstraintCollector


class _ConstraintCollectorData:
    def __hash__(self):
        raise TypeError('A ConstraintCollector should not be used as a key. '
                        'Maybe you accidentally used a constraint collector in a lambda? '
                        '(i.e. instead of doing `lambda shift: ConstraintCollectors.sum(shift.duration)`, '
                        'do `ConstraintCollectors.sum(lambda shift: shift.duration)` instead).')


@dataclasses.dataclass
class NoArgsConstraintCollector(_ConstraintCollectorData):
    collector_creator: Callable


@dataclasses.dataclass
class GroupMappingSingleArgConstraintCollector(_ConstraintCollectorData):
    collector_creator: Callable
    group_mapping: Callable


@dataclasses.dataclass
class KeyValueMappingConstraintCollector(_ConstraintCollectorData):
    collector_creator: Callable
    key_mapping: Callable
    value_mapping: Callable


@dataclasses.dataclass
class GroupIntMappingSingleArgConstraintCollector(_ConstraintCollectorData):
    collector_creator: Callable
    group_mapping: Callable


@dataclasses.dataclass
class GroupMappingIntMappingTwoArgConstraintCollector(_ConstraintCollectorData):
    collector_creator: Callable
    group_mapping: Callable
    index_mapping: Callable


@dataclasses.dataclass
class ComposeConstraintCollector(_ConstraintCollectorData):
    collector_creator: Callable
    subcollectors: Sequence[Any]
    compose_function: Callable


@dataclasses.dataclass
class ConditionalConstraintCollector(_ConstraintCollectorData):
    collector_creator: Callable
    predicate: Callable
    delegate: Any


@dataclasses.dataclass
class CollectAndThenCollector(_ConstraintCollectorData):
    collector_creator: Callable
    delegate_collector: Any
    mapping_function: Callable


@dataclasses.dataclass
class LoadBalanceCollector(_ConstraintCollectorData):
    collector_creator: Callable
    balanced_item_function: Callable
    load_function: Callable | None
    initial_load_function: Callable | None


def extract_collector(collector_info, *type_arguments):
    if isinstance(collector_info, NoArgsConstraintCollector):
        return collector_info.collector_creator()
    elif isinstance(collector_info, GroupMappingSingleArgConstraintCollector):
        return collector_info.collector_creator(function_cast(collector_info.group_mapping, *type_arguments))
    elif isinstance(collector_info, KeyValueMappingConstraintCollector):
        return collector_info.collector_creator(function_cast(collector_info.key_mapping, *type_arguments),
                                                function_cast(collector_info.value_mapping, *type_arguments))
    elif isinstance(collector_info, GroupIntMappingSingleArgConstraintCollector):
        return collector_info.collector_creator(to_int_function_cast(collector_info.group_mapping, *type_arguments))
    elif isinstance(collector_info, GroupMappingIntMappingTwoArgConstraintCollector):
        return collector_info.collector_creator(function_cast(collector_info.group_mapping, *type_arguments),
                                                to_int_function_cast(collector_info.index_mapping,
                                                                     JClass('java.lang.Object')))
    elif isinstance(collector_info, ComposeConstraintCollector):
        subcollectors = tuple(map(lambda subcollector_info: extract_collector(subcollector_info, *type_arguments),
                                  collector_info.subcollectors))
        compose_parameters = (JClass('java.lang.Object'),) * len(subcollectors)
        compose_function = function_cast(collector_info.compose_function, *compose_parameters)
        return collector_info.collector_creator(*subcollectors, compose_function)
    elif isinstance(collector_info, ConditionalConstraintCollector):
        delegate_collector = extract_collector(collector_info.delegate, *type_arguments)
        predicate = predicate_cast(collector_info.predicate, *type_arguments)
        return collector_info.collector_creator(predicate, delegate_collector)
    elif isinstance(collector_info, CollectAndThenCollector):
        delegate_collector = extract_collector(collector_info.delegate_collector, *type_arguments)
        mapping_function = function_cast(collector_info.mapping_function, JClass('java.lang.Object'))
        return collector_info.collector_creator(delegate_collector, mapping_function)
    elif isinstance(collector_info, LoadBalanceCollector):
        balanced_item_function = function_cast(collector_info.balanced_item_function,  *type_arguments)
        if collector_info.load_function is None:
            return collector_info.collector_creator(balanced_item_function)
        load_function = to_long_function_cast(collector_info.load_function,  *type_arguments)
        if collector_info.initial_load_function is None:
            return collector_info.collector_creator(balanced_item_function, load_function)
        initial_load_function = to_long_function_cast(collector_info.initial_load_function,  *type_arguments)
        return collector_info.collector_creator(balanced_item_function, load_function, initial_load_function)
    else:
        raise ValueError(f'Invalid Collector: {collector_info}. '
                         f'Create Collectors via timefold.solver.constraint.ConstraintCollectors.')


def perform_group_by(constraint_stream, package, group_by_args, *type_arguments):
    actual_group_by_args = []
    for i in range(len(group_by_args)):
        if callable(group_by_args[i]):
            actual_group_by_args.append(function_cast(group_by_args[i], *type_arguments))
        else:
            collector_info = group_by_args[i]
            created_collector = extract_collector(collector_info, *type_arguments)
            actual_group_by_args.append(created_collector)

    if len(group_by_args) is 1:
        return UniConstraintStream(constraint_stream.groupBy(*actual_group_by_args), package,
                                   JClass('java.lang.Object'))
    elif len(group_by_args) is 2:
        return BiConstraintStream(constraint_stream.groupBy(*actual_group_by_args), package,
                                  JClass('java.lang.Object'),
                                  JClass('java.lang.Object'))
    elif len(group_by_args) is 3:
        return TriConstraintStream(constraint_stream.groupBy(*actual_group_by_args), package,
                                   JClass('java.lang.Object'),
                                   JClass('java.lang.Object'), JClass('java.lang.Object'))
    elif len(group_by_args) is 4:
        return QuadConstraintStream(constraint_stream.groupBy(*actual_group_by_args), package,
                                    JClass('java.lang.Object'),
                                    JClass('java.lang.Object'), JClass('java.lang.Object'),
                                    JClass('java.lang.Object'))
    else:
        raise ValueError


class ConstraintCollectors:
    """
    Creates an UniConstraintCollector, BiConstraintCollector, ...
    instances for use in `UniConstraintStream.group_by`, ...
    """
    #  Method parameter type variables
    A = TypeVar('A')
    B = TypeVar('B')
    C = TypeVar('C')
    D = TypeVar('D')
    E = TypeVar('E')
    Balanced = TypeVar('Balanced')

    #  Method return type variables
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')
    Balanced_ = TypeVar('Balanced_')

    @staticmethod
    def _delegate():
        from .._timefold_java_interop import ensure_init
        ensure_init()
        from ai.timefold.solver.core.api.score.stream import ConstraintCollectors
        return ConstraintCollectors

    @overload  # noqa
    @staticmethod
    def average(group_value_mapping: Callable[[A], int]) -> 'UniConstraintCollector[A, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def average(group_value_mapping: Callable[[A, B], int]) -> 'BiConstraintCollector[A, B, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def average(group_value_mapping: Callable[[A, B, C], int]) -> 'TriConstraintCollector[A, B, C, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def average(group_value_mapping: Callable[[A, B, C, D], int]) -> 'QuadConstraintCollector[A, B, C, D, Any, int]':
        ...

    @staticmethod
    def average(group_value_mapping):
        """
        Returns a collector that calculates an average of an int property of the elements that are being grouped.
        """
        return GroupIntMappingSingleArgConstraintCollector(ConstraintCollectors._delegate().average,
                                                           group_value_mapping)

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'UniConstraintCollector[A, Any, A_]',
                sub_collector_2: 'UniConstraintCollector[A, Any, B_]',
                compose_function: Callable[[A_, B_], C_]) -> 'UniConstraintCollector[A, Any, C_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'UniConstraintCollector[A, Any, A_]',
                sub_collector_2: 'UniConstraintCollector[A, Any, B_]',
                sub_collector_3: 'UniConstraintCollector[A, Any, C_]',
                compose_function: Callable[[A_, B_, C_], D_]) -> 'UniConstraintCollector[A, Any, D_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'UniConstraintCollector[A, Any, A_]',
                sub_collector_2: 'UniConstraintCollector[A, Any, B_]',
                sub_collector_3: 'UniConstraintCollector[A, Any, C_]',
                sub_collector_4: 'UniConstraintCollector[A, Any, D_]',
                compose_function: Callable[[A_, B_, C_, D_], E_]) -> 'UniConstraintCollector[A, Any, E_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'BiConstraintCollector[A, B, Any, A_]',
                sub_collector_2: 'BiConstraintCollector[A, B, Any, B_]',
                compose_function: Callable[[A_, B_], C_]) -> 'BiConstraintCollector[A, B, Any, C_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'BiConstraintCollector[A, B, Any, A_]',
                sub_collector_2: 'BiConstraintCollector[A, B, Any, B_]',
                sub_collector_3: 'BiConstraintCollector[A, B, Any, C_]',
                compose_function: Callable[[A_, B_, C_], D_]) -> 'BiConstraintCollector[A, B, Any, D_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'BiConstraintCollector[A, B, Any, A_]',
                sub_collector_2: 'BiConstraintCollector[A, B, Any, B_]',
                sub_collector_3: 'BiConstraintCollector[A, B, Any, C_]',
                sub_collector_4: 'BiConstraintCollector[A, B, Any, D_]',
                compose_function: Callable[[A_, B_, C_, D_], E_]) -> 'BiConstraintCollector[A, B, Any, E_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'TriConstraintCollector[A, B, C, Any, A_]',
                sub_collector_2: 'TriConstraintCollector[A, B, C, Any, B_]',
                compose_function: Callable[[A_, B_], C_]) -> 'TriConstraintCollector[A, B, C, Any, C_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'TriConstraintCollector[A, B, C, Any, A_]',
                sub_collector_2: 'TriConstraintCollector[A, B, C, Any, B_]',
                sub_collector_3: 'TriConstraintCollector[A, B, C, Any, C_]',
                compose_function: Callable[[A_, B_, C_], D_]) -> 'TriConstraintCollector[A, B, C, Any, D_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'TriConstraintCollector[A, B, C, Any, A_]',
                sub_collector_2: 'TriConstraintCollector[A, B, C, Any, B_]',
                sub_collector_3: 'TriConstraintCollector[A, B, C, Any, C_]',
                sub_collector_4: 'TriConstraintCollector[A, B, C, Any, D_]',
                compose_function: Callable[[A_, B_, C_, D_], E_]) -> 'TriConstraintCollector[A, B, C, Any, E_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                sub_collector_2: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                compose_function: Callable[[A_, B_], C_]) -> 'QuadConstraintCollector[A, B, C, D, Any, C_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                sub_collector_2: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                sub_collector_3: 'QuadConstraintCollector[A, B, C, D, Any, C_]',
                compose_function: Callable[[A_, B_, C_], D_]) -> 'QuadConstraintCollector[A, B, C, D, Any, D_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                sub_collector_2: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                sub_collector_3: 'QuadConstraintCollector[A, B, C, D, Any, C_]',
                sub_collector_4: 'QuadConstraintCollector[A, B, C, D, Any, D_]',
                compose_function: Callable[[A_, B_, C_, D_], E_]) -> 'QuadConstraintCollector[A, B, C, D, Any, E_]':
        ...

    @staticmethod
    def compose(*args):
        """
        Returns a constraint collector the result of which is a composition of other constraint collectors.
        """
        if len(args) < 3:  # Need at least two collectors + 1 compose function
            raise ValueError
        collectors = args[:-1]
        compose_function = args[-1]
        return ComposeConstraintCollector(ConstraintCollectors._delegate().compose, collectors, compose_function)

    @overload  # noqa
    @staticmethod
    def conditionally(predicate: Callable[[A], bool], delegate: 'UniConstraintCollector[A, Any, A_]') -> \
            'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def conditionally(predicate: Callable[[A, B], bool],
                      delegate: 'BiConstraintCollector[A, B, Any, A_]') -> 'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def conditionally(predicate: Callable[[A, B, C], bool],
                      delegate: 'TriConstraintCollector[A, B, C, Any, A_]') -> \
            'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def conditionally(predicate: Callable[[A, B, C, D], bool],
                      delegate: 'QuadConstraintCollector[A, B, C, D, Any, A_]') -> \
            'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @staticmethod
    def conditionally(predicate, delegate):
        """
        Returns a collector that delegates to the underlying collector if and only if the input tuple meets the given
        condition.
        """
        return ConditionalConstraintCollector(ConstraintCollectors._delegate().conditionally,
                                              predicate,
                                              delegate)

    @overload  # noqa
    @staticmethod
    def collect_and_then(delegate: 'UniConstraintCollector[A, Any, A_]',
                         mapping_function: Callable[[A_], B_]) -> \
            'UniConstraintCollector[A, Any, B_]':
        ...

    @overload  # noqa
    @staticmethod
    def collect_and_then(delegate: 'BiConstraintCollector[A, B, Any, A_]',
                         mapping_function: Callable[[A_], B_]) -> 'BiConstraintCollector[A, B, Any, B_]':
        ...

    @overload  # noqa
    @staticmethod
    def collect_and_then(delegate: 'TriConstraintCollector[A, B, C, Any, A_]',
                         mapping_function: Callable[[A_], B_]) -> \
            'TriConstraintCollector[A, B, C, Any, B_]':
        ...

    @overload  # noqa
    @staticmethod
    def collect_and_then(delegate: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                         mapping_function: Callable[[A_], B_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, B_]':
        ...

    @staticmethod
    def collect_and_then(delegate, mapping_function):
        """
        Returns a collector that delegates to the underlying collector and maps its result to another value.
        """
        return CollectAndThenCollector(ConstraintCollectors._delegate().collectAndThen,
                                       delegate,
                                       mapping_function)

    @staticmethod
    def count() -> 'UniConstraintCollector[A, Any, int]':
        """
        Returns a collector that counts the number of elements that are being grouped.
        """
        return NoArgsConstraintCollector(ConstraintCollectors._delegate().count)  # noqa

    @staticmethod
    def count_bi() -> 'BiConstraintCollector[A, B, Any, int]':
        """
        Returns a collector that counts the number of elements that are being grouped.
        """
        return NoArgsConstraintCollector(ConstraintCollectors._delegate().countBi)  # noqa

    @staticmethod
    def count_tri() -> 'TriConstraintCollector[A, B, C, Any, int]':
        """
        Returns a collector that counts the number of elements that are being grouped.
        """
        return NoArgsConstraintCollector(ConstraintCollectors._delegate().countTri)  # noqa

    @staticmethod
    def count_quad() -> 'QuadConstraintCollector[A, B, C, D, Any, int]':
        """
        Returns a collector that counts the number of elements that are being grouped.
        """
        return NoArgsConstraintCollector(ConstraintCollectors._delegate().countQuad)  # noqa

    @overload  # noqa
    @staticmethod
    def count_distinct() -> 'UniConstraintCollector[A, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def count_distinct(group_value_mapping: Callable[[A], int]) -> 'UniConstraintCollector[A, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def count_distinct(group_value_mapping: Callable[[A, B], int]) -> 'BiConstraintCollector[A, B, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def count_distinct(group_value_mapping: Callable[[A, B, C], int]) -> 'TriConstraintCollector[A, B, C, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def count_distinct(group_value_mapping: Callable[[A, B, C, D], int]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, int]':
        ...

    @staticmethod
    def count_distinct(function=None):
        """
        Returns a collector that counts the number of unique elements that are being grouped.
        """
        if function is None:
            return NoArgsConstraintCollector(ConstraintCollectors._delegate().countDistinct)
        else:
            return GroupMappingSingleArgConstraintCollector(ConstraintCollectors._delegate().countDistinct, function)

    @overload  # noqa
    @staticmethod
    def max() -> 'UniConstraintCollector[A, Any, A]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A], A_]) -> 'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(comparator: Callable[[A, A], int]) -> 'UniConstraintCollector[A, Any, A]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A], A_], comparator: Callable[[A_, A_], int]) -> \
            'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B], A_]) -> 'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B], A_], comparator: Callable[[A_, A_], int]) -> \
            'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B, C], A_]) -> 'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B, C], A_], comparator: Callable[[A_, A_], int]) -> \
            'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B, C, D], A_]) -> 'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B, C, D], A_], comparator: Callable[[A_, A_], int]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @staticmethod
    def max(function=None, comparator=None):
        """
        Returns a collector that finds a maximum value in a group of comparable elements.
        """
        if function is None and comparator is None:
            return NoArgsConstraintCollector(ConstraintCollectors._delegate().max)
        elif function is not None and comparator is None:
            return GroupMappingSingleArgConstraintCollector(ConstraintCollectors._delegate().max, function)
        elif function is None and comparator is not None:
            raise NotImplementedError  # TODO
        else:
            raise NotImplementedError  # TODO

    @overload  # noqa
    @staticmethod
    def min() -> 'UniConstraintCollector[A, Any, A]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A], A_]) -> 'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(comparator: Callable[[A, A], int]) -> 'UniConstraintCollector[A, Any, A]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A], A_], comparator: Callable[[A_, A_], int]) -> \
            'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B], A_]) -> 'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B], A_], comparator: Callable[[A_, A_], int]) -> \
            'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B, C], A_]) -> 'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B, C], A_], comparator: Callable[[A_, A_], int]) -> \
            'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B, C, D], A_]) -> 'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B, C, D], A_], comparator: Callable[[A_, A_], int]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @staticmethod
    def min(function=None, comparator=None):
        """
        Returns a collector that finds a minimum value in a group of comparable elements.
        """
        if function is None and comparator is None:
            return NoArgsConstraintCollector(ConstraintCollectors._delegate().min)
        elif function is not None and comparator is None:
            return GroupMappingSingleArgConstraintCollector(ConstraintCollectors._delegate().min, function)
        elif function is None and comparator is not None:
            raise NotImplementedError  # TODO
        else:
            raise NotImplementedError  # TODO

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A], int]) -> 'UniConstraintCollector[A, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B], int]) -> 'BiConstraintCollector[A, B, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B, C], int]) -> 'TriConstraintCollector[A, B, C, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B, C, D], int]) -> 'QuadConstraintCollector[A, B, C, D, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A], A_], zero: A_, adder: Callable[[A_, A_], A_], subtractor: Callable[[A_, A_], A_]) \
            -> 'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B], A_], zero: A_, adder: Callable[[A_, A_], A_],
            subtractor: Callable[[A_, A_], A_]) \
            -> 'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B, C], A_], zero: A_, adder: Callable[[A_, A_], A_],
            subtractor: Callable[[A_, A_], A_]) \
            -> 'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B, C, D], A_], zero: A_, adder: Callable[[A_, A_], A_],
            subtractor: Callable[[A_, A_], A_]) \
            -> 'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @staticmethod
    def sum(function, zero=None, adder=None, subtractor=None):
        """
        Returns a collector that sums an int property of the elements that are being grouped.
        """
        if zero is None and adder is None and subtractor is None:
            return GroupIntMappingSingleArgConstraintCollector(ConstraintCollectors._delegate().sum, function)
        elif zero is not None and adder is not None and subtractor is not None:
            raise NotImplementedError  # TODO
        else:
            raise ValueError

    @overload  # noqa
    @staticmethod
    def to_consecutive_sequences(index_map: Callable[[A], int]) -> \
            'UniConstraintCollector[A, Any, SequenceChain[A, int]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_consecutive_sequences(result_map: Callable[[A, B], A_], index_map: Callable[[A_], int]) -> \
            'BiConstraintCollector[A, B, Any, SequenceChain[A_, int]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_consecutive_sequences(result_map: Callable[[A, B, C], A_], index_map: Callable[[A_], int]) -> \
            'TriConstraintCollector[A, B, C, Any, SequenceChain[A_, int]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_consecutive_sequences(result_map: Callable[[A, B, C, D], A_], index_map: Callable[[A_], int]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, SequenceChain[A_, int]]':
        ...

    @staticmethod
    def to_consecutive_sequences(result_or_index_map, index_map=None):
        """
        Creates a constraint collector that returns SequenceChain about the first fact.

        For instance, [Shift slot=1] [Shift slot=2] [Shift slot=4] [Shift slot=6] returns the following information:

        - Consecutive Lengths: 2, 1, 1
        - Break Lengths: 1, 2
        - Consecutive Items: [[Shift slot=1] [Shift slot=2]], [[Shift slot=4]], [[Shift slot=6]]
        """
        if index_map is None:
            return GroupIntMappingSingleArgConstraintCollector(ConstraintCollectors._delegate().toConsecutiveSequences,
                                                               result_or_index_map)
        else:
            return GroupMappingIntMappingTwoArgConstraintCollector(
                ConstraintCollectors._delegate().toConsecutiveSequences,
                result_or_index_map, index_map)

    @overload  # noqa
    @staticmethod
    def to_list() -> 'UniConstraintCollector[A, Any, List[A]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_list(group_value_mapping: Callable[[A], A_]) -> 'UniConstraintCollector[A, Any, List[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_list(group_value_mapping: Callable[[A, B], A_]) -> 'BiConstraintCollector[A, B, Any, List[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_list(group_value_mapping: Callable[[A, B, C], A_]) -> 'TriConstraintCollector[A, B, C, Any, List[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_list(group_value_mapping: Callable[[A, B, C, D], A_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, List[A_]]':
        ...

    @staticmethod
    def to_list(group_value_mapping=None):
        """
        Creates constraint collector that returns List of the given element type.
        """
        if group_value_mapping is None:
            return NoArgsConstraintCollector(ConstraintCollectors._delegate().toList)
        else:
            return GroupMappingSingleArgConstraintCollector(ConstraintCollectors._delegate().toList,
                                                            group_value_mapping)

    @overload  # noqa
    @staticmethod
    def to_set() -> 'UniConstraintCollector[A, Any, Set[A]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_set(group_value_mapping: Callable[[A], A_]) -> 'UniConstraintCollector[A, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_set(group_value_mapping: Callable[[A, B], A_]) -> 'BiConstraintCollector[A, B, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_set(group_value_mapping: Callable[[A, B, C], A_]) -> 'TriConstraintCollector[A, B, C, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_set(group_value_mapping: Callable[[A, B, C, D], A_]) -> 'QuadConstraintCollector[A, B, C, D, Any, Set[A_]]':
        ...

    @staticmethod
    def to_set(group_value_mapping=None):
        """
        Creates constraint collector that returns Set of the same element type as the ConstraintStream.
        """
        if group_value_mapping is None:
            return NoArgsConstraintCollector(ConstraintCollectors._delegate().toSet)
        else:
            return GroupMappingSingleArgConstraintCollector(ConstraintCollectors._delegate().toSet, group_value_mapping)

    @overload  # noqa
    @staticmethod
    def to_sorted_set() -> 'UniConstraintCollector[A, Any, Set[A]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A], A_]) -> 'UniConstraintCollector[A, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(comparator: Callable[[A, A], int]) -> 'UniConstraintCollector[A, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A], A_], comparator: Callable[[A_, A_], int]) -> \
            'UniConstraintCollector[A, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B], A_]) -> 'BiConstraintCollector[A, B, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B], A_], comparator: Callable[[A_, A_], int]) -> \
            'BiConstraintCollector[A, B, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B, C], A_]) -> 'TriConstraintCollector[A, B, C, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B, C], A_], comparator: Callable[[A_, A_], int]) -> \
            'TriConstraintCollector[A, B, C, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B, C, D], A_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B, C, D], A_], comparator: Callable[[A_, A_], int]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Set[A_]]':
        ...

    @staticmethod
    def to_sorted_set(group_value_mapping=None, comparator=None):
        """
        Creates constraint collector that returns SortedSet of the same element type as the ConstraintStream.
        """
        if group_value_mapping is None and comparator is None:
            return NoArgsConstraintCollector(ConstraintCollectors._delegate().toSortedSet)
        elif group_value_mapping is not None and comparator is None:
            return GroupMappingSingleArgConstraintCollector(ConstraintCollectors._delegate().toSortedSet,
                                                            group_value_mapping)

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_]) -> \
            'UniConstraintCollector[A, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_],
               merge_function: Callable[[B_, B_], B_]) -> \
            'UniConstraintCollector[A, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_],
               set_creator: Callable[[int], Set[B_]]) -> \
            'UniConstraintCollector[A, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_]) -> \
            'BiConstraintCollector[A, B, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_],
               merge_function: Callable[[B_, B_], B_]) -> \
            'BiConstraintCollector[A, B, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_],
               set_creator: Callable[[int], Set[B_]]) -> \
            'BiConstraintCollector[A, B, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_]) -> \
            'TriConstraintCollector[A, B, C, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_],
               merge_function: Callable[[B_, B_], B_]) -> \
            'TriConstraintCollector[A, B, C, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_],
               set_creator: Callable[[int], Set[B_]]) -> \
            'TriConstraintCollector[A, B, C, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B, C, D], B_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B, C], B_],
               merge_function: Callable[[B_, B_], B_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B], B_],
               set_creator: Callable[[int], Set[B_]]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, Set[B_]]]':
        ...

    @staticmethod
    def to_map(key_mapper, value_mapper, merge_function_or_set_creator=None):
        """
        Creates a constraint collector that returns a Map with given keys and values consisting of a Set of mappings.
        """
        import inspect
        if merge_function_or_set_creator is None:
            return KeyValueMappingConstraintCollector(ConstraintCollectors._delegate().toMap, key_mapper, value_mapper)

        arg_count = len(inspect.signature(merge_function_or_set_creator).parameters)
        if arg_count == 1:  # set_creator
            raise NotImplementedError
        elif arg_count == 2:  # merge_function
            raise NotImplementedError
        else:
            raise ValueError

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_]) -> \
            'UniConstraintCollector[A, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_],
                      merge_function: Callable[[B_, B_], B_]) -> \
            'UniConstraintCollector[A, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_],
                      set_creator: Callable[[int], Set[B_]]) -> \
            'UniConstraintCollector[A, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_]) -> \
            'BiConstraintCollector[A, B, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_],
                      merge_function: Callable[[B_, B_], B_]) -> \
            'BiConstraintCollector[A, B, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_],
                      set_creator: Callable[[int], Set[B_]]) -> \
            'BiConstraintCollector[A, B, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_]) -> \
            'TriConstraintCollector[A, B, C, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_],
                      merge_function: Callable[[B_, B_], B_]) -> \
            'TriConstraintCollector[A, B, C, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_],
                      set_creator: Callable[[int], Set[B_]]) -> \
            'TriConstraintCollector[A, B, C, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B, C, D], B_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B, C], B_],
                      merge_function: Callable[[B_, B_], B_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B], B_],
                      set_creator: Callable[[int], Set[B_]]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, Set[B_]]]':
        ...

    @staticmethod
    def to_sorted_map(key_mapper, value_mapper, merge_function_or_set_creator=None):
        """
        Creates a constraint collector that returns a SortedMap with given keys and values consisting of a Set of
        mappings.
        """
        import inspect
        if merge_function_or_set_creator is None:
            return KeyValueMappingConstraintCollector(ConstraintCollectors._delegate().toSortedMap, key_mapper,
                                                      value_mapper)

        arg_count = len(inspect.signature(merge_function_or_set_creator).parameters)
        if arg_count == 1:  # set_creator
            raise NotImplementedError
        elif arg_count == 2:  # merge_function
            raise NotImplementedError
        else:
            raise ValueError

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A], Balanced_]) -> \
            'UniConstraintCollector[A, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A], Balanced_], load_function: Callable[[A], int]) -> \
            'UniConstraintCollector[A, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A], Balanced_], load_function: Callable[[A], int],
                     initial_load_function: Callable[[A], int]) -> \
            'UniConstraintCollector[A, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A, B], Balanced_]) -> \
            'BiConstraintCollector[A, B, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A, B], Balanced_], load_function: Callable[[A, B], int]) -> \
            'BiConstraintCollector[A, B, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A, B], Balanced_], load_function: Callable[[A, B], int],
                     initial_load_function: Callable[[A, B], int]) -> \
            'BiConstraintCollector[A, B, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A, B, C], Balanced_]) -> \
            'TriConstraintCollector[A, B, C, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A, B, C], Balanced_],
                     load_function: Callable[[A, B, C], int]) -> \
            'TriConstraintCollector[A, B, C, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A, B, C], Balanced_], load_function: Callable[[A, B, C], int],
                     initial_load_function: Callable[[A, B, C], int]) -> \
            'TriConstraintCollector[A, B, C, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A, B, C, D], Balanced_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A, B, C, D], Balanced_],
                     load_function: Callable[[A, B, C, D], int]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, LoadBalance[Balanced_]]':
        ...

    @overload  # noqa
    @staticmethod
    def load_balance(balanced_item_function: Callable[[A, B, C, D], Balanced_],
                     load_function: Callable[[A, B, C, D], int],
                     initial_load_function: Callable[[A, B, C, D], int]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, LoadBalance[Balanced_]]':
        ...

    @staticmethod
    def load_balance(balanced_item_function, load_function=None, initial_load_function=None):
        """
        Returns a collector that takes a stream of items and calculates the unfairness measure from them.
        The load for every item is provided by the load_function,
        with the starting load provided by the initial_load_function.

        When this collector is used in a constraint stream,
        it is recommended to use a score type which supports real numbers.
        This is so that the unfairness measure keeps its precision
        without forcing the other constraints to be multiplied by a large constant,
        which would otherwise be required to implement fixed-point arithmetic.

        Parameters
        ----------
        balanced_item_function: Callable[[ParameterTypes, ...], Balanced_]
             The function that returns the item which should be load-balanced.
        load_function: Callable[[ParameterTypes, ...], int]
            How much the item should count for in the formula.
        initial_load_function: Callable[[ParameterTypes, ...], int]
            The initial value of the metric, allowing to provide initial state
            without requiring the entire previous planning windows in the working memory.
            If this function is provided, load_function must be provided as well.
        """
        if load_function is None and initial_load_function is None:
            return LoadBalanceCollector(ConstraintCollectors._delegate().loadBalance, balanced_item_function, None,
                                        None)
        elif initial_load_function is None:
            return LoadBalanceCollector(ConstraintCollectors._delegate().loadBalance, balanced_item_function,
                                        load_function, None)
        elif load_function is None:
            raise ValueError("load_function cannot be None if initial_load_function is not None")
        else:
            return LoadBalanceCollector(ConstraintCollectors._delegate().loadBalance, balanced_item_function,
                                        load_function, initial_load_function)


# Must be at the bottom, constraint_stream depends on this module
from ._constraint_stream import *
from ._function_translator import *
__all__ = [
    'NoArgsConstraintCollector',
    'GroupMappingSingleArgConstraintCollector',
    'KeyValueMappingConstraintCollector',
    'GroupIntMappingSingleArgConstraintCollector',
    'GroupMappingIntMappingTwoArgConstraintCollector',
    'ComposeConstraintCollector',
    'ConditionalConstraintCollector',
    'CollectAndThenCollector',
    'LoadBalanceCollector',
    'ConstraintCollectors',
    'perform_group_by'
]
