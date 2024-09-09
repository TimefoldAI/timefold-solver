import jpyinterpreter
import pytest
import sys
from jpype import JImplementationFor
from typing import SupportsAbs, Iterable, Callable, Sequence, Union, Iterator, Sized, Reversible, SupportsIndex

from .conftest import verifier_for


# Workaround for https://github.com/jpype-project/jpype/issues/1178
@JImplementationFor('java.lang.Throwable')
class _JavaException:
    @staticmethod
    def _get_exception_with_cause(exception):
        if exception is None:
            return None
        try:
            raise Exception(f'{exception.getClass().getSimpleName()}: {exception.getMessage()}\n'
                            f'{exception.stacktrace()}')
        except Exception as e:
            cause = _JavaException._get_exception_with_cause(exception.getCause())
            if cause is not None:
                try:
                    raise e from cause
                except Exception as return_value:
                    return return_value
            else:
                return e
    @property
    def __cause__(self):
        if self.getCause() is not None:
            return _JavaException._get_exception_with_cause(self.getCause())
        else:
            return None


def test_abs():
    def my_function(x: SupportsAbs) -> object:
        return abs(x)

    class MyClassWithAbs:
        def __abs__(self) -> int:
            return 10

    verifier = verifier_for(my_function)

    verifier.verify(1, expected_result=1)
    verifier.verify(-1, expected_result=1)
    verifier.verify(1.0, expected_result=1.0)
    verifier.verify(-1.0, expected_result=1.0)
    verifier.verify(MyClassWithAbs(), expected_result=10)


def test_any():
    def my_function(x: Iterable) -> bool:
        return any(x)

    class MyClassWithoutBool:
        pass

    class MyClassWithBool:
        is_true: bool

        def __init__(self, is_true: bool):
            self.is_true = is_true

        def __bool__(self) -> bool:
            return self.is_true

    verifier = verifier_for(my_function)
    verifier.verify([], expected_result=False)
    verifier.verify([False, 0, 0.0, MyClassWithBool(False), None], expected_result=False)
    verifier.verify([False, MyClassWithBool(True)], expected_result=True)
    verifier.verify([MyClassWithoutBool()], expected_result=True)
    verifier.verify([1], expected_result=True)
    verifier.verify([1.0], expected_result=True)
    verifier.verify([True], expected_result=True)


def test_all():
    def my_function(x: Iterable) -> bool:
        return all(x)

    class MyClassWithoutBool:
        pass

    class MyClassWithBool:
        is_true: bool

        def __init__(self, is_true: bool):
            self.is_true = is_true

        def __bool__(self) -> bool:
            return self.is_true

    verifier = verifier_for(my_function)
    verifier.verify([], expected_result=True)
    verifier.verify([True, 1, 1.0, MyClassWithBool(True)], expected_result=True)
    verifier.verify([False, MyClassWithBool(True)], expected_result=False)
    verifier.verify([MyClassWithoutBool()], expected_result=True)
    verifier.verify([0], expected_result=False)
    verifier.verify([0.0], expected_result=False)
    verifier.verify([False], expected_result=False)
    verifier.verify([None], expected_result=False)


def test_ascii():
    def my_function(x: object) -> str:
        return ascii(x)

    verifier = verifier_for(my_function)
    verifier.verify(10, expected_result='10')
    verifier.verify('text', expected_result="'text'")
    verifier.verify('text\nwith\nlines', expected_result="'text\\nwith\\nlines'")


def test_bin():
    def my_function(x: int) -> str:
        return bin(x)

    verifier = verifier_for(my_function)
    verifier.verify(10, expected_result=bin(10))
    verifier.verify(-10, expected_result=bin(-10))


def test_bool():
    def my_function(x: object) -> bool:
        return bool(x)

    verifier = verifier_for(my_function)
    verifier.verify(0, expected_result=False)
    verifier.verify(1, expected_result=True)
    verifier.verify(-1, expected_result=True)
    verifier.verify('', expected_result=False)
    verifier.verify('test', expected_result=True)
    verifier.verify(True, expected_result=True)
    verifier.verify(False, expected_result=False)


def test_callable():
    def my_function(x: Callable) -> bool:
        return callable(x)

    verifier = verifier_for(my_function)
    verifier.verify(10, expected_result=False)
    verifier.verify(my_function, expected_result=True)
    verifier.verify(int, expected_result=True)


def test_chr():
    def my_function(x: int) -> str:
        return chr(x)

    verifier = verifier_for(my_function)
    verifier.verify(30, expected_result=chr(30))
    verifier.verify(2400, expected_result=chr(2400))


def test_delattr():
    def my_function(x: object) -> bool:
        delattr(x, 'my_attr')
        return hasattr(x, 'my_attr')

    class TestObject:
        pass

    a = TestObject()
    a.my_attr = 'test'

    verifier = verifier_for(my_function)
    verifier.verify(a, expected_result=False)
    verifier.verify(TestObject(), expected_error=AttributeError)


def test_divmod():
    def my_function(x: any, y: any) -> tuple:
        return divmod(x, y)

    verifier = verifier_for(my_function)
    verifier.verify(16, 5)
    verifier.verify(-16, 5)
    verifier.verify(16, -5)
    verifier.verify(-16, -5)

    verifier.verify(16, 5.0)
    verifier.verify(-16, 5.0)
    verifier.verify(16, -5.0)
    verifier.verify(-16, -5.0)

    verifier.verify(16.0, 5.0)
    verifier.verify(-16.0, 5.0)
    verifier.verify(16.0, -5.0)
    verifier.verify(-16.0, -5.0)


def test_dict():
    def my_function(x: object) -> dict:
        out = dict()
        out['key'] = x
        return out

    verifier = verifier_for(my_function)
    verifier.verify('value', expected_result={'key': 'value'})
    verifier.verify(10, expected_result={'key': 10})


def test_enumerate():
    def my_function_without_start(x: Sequence) -> tuple:
        return tuple(enumerate(x))

    def my_function_with_start(x: Sequence, start: int) -> tuple:
        return tuple(enumerate(x, start))

    verifier = verifier_for(my_function_without_start)
    with_start_verifier = verifier_for(my_function_with_start)
    verifier.verify(['a', 'b', 'c'], expected_result=((0, 'a'), (1, 'b'), (2, 'c')))
    with_start_verifier.verify(['a', 'b', 'c'], 5, expected_result=((5, 'a'), (6, 'b'), (7, 'c')))


def test_filter():
    def my_function(function: Callable[[any], bool], iterable: Iterable) -> tuple:
        return tuple(filter(function, iterable))

    verifier = verifier_for(my_function)

    verifier.verify(None, [0, 1, False, 2], expected_result=(1, 2))
    verifier.verify(lambda x: x == 2 or x is False, [0, 1, False, 2], expected_result=(False, 2))


def test_float():
    import math

    def my_function(x: any) -> float:
        return float(x)

    verifier = verifier_for(my_function)
    verifier.verify(10, expected_result=10.0)
    verifier.verify('1.0', expected_result=1.0)
    verifier.verify_property('nan', predicate=math.isnan)
    verifier.verify_property('NaN', predicate=math.isnan)
    verifier.verify_property('-nan', predicate=math.isnan)
    verifier.verify_property('-NaN', predicate=math.isnan)
    verifier.verify('inf', expected_result=float('inf'))
    verifier.verify('INF', expected_result=float('inf'))
    verifier.verify('-inf', expected_result=float('-inf'))
    verifier.verify('infinity', expected_result=float('inf'))
    verifier.verify('-infinity', expected_result=float('-inf'))


def test_format():
    def my_function(x: object) -> str:
        return format(x)

    def my_function_with_spec(x: object, spec: str) -> str:
        return format(x, spec)

    verifier = verifier_for(my_function)
    with_spec_verifier = verifier_for(my_function_with_spec)

    verifier.verify(10, expected_result='10')
    with_spec_verifier.verify(10, '', expected_result='10')


def test_getattr():
    def my_function(x: any, name: str) -> any:
        return getattr(x, name)

    def my_function_with_default(x: any, name: str, default: any) -> any:
        return getattr(x, name, default)

    class TestObject:
        pass

    verifier = verifier_for(my_function)
    with_default_verifier = verifier_for(my_function_with_default)

    a = TestObject()
    a.test = 'value'

    verifier.verify(a, 'test', expected_result='value')
    with_default_verifier.verify(a, 'missing', 10, expected_result=10)


global_variable = 10


def test_globals():
    def my_function() -> any:
        global global_variable
        x = global_variable
        return globals()

    verifier = verifier_for(my_function)
    # The globals directory in Java only stores used globals
    verifier.verify_property(predicate=lambda out: out['global_variable'] == 10)


def test_hasattr():
    def my_function(x: any, name: str) -> bool:
        return hasattr(x, name)

    class TestObject:
        pass

    verifier = verifier_for(my_function)
    a = TestObject()
    a.test = 'value'
    verifier.verify(a, 'test', expected_result=True)
    verifier.verify(a, 'other', expected_result=False)


def test_hash():
    def my_function(x: any) -> int:
        return hash(x)

    verifier = verifier_for(my_function)
    verifier.verify(1)
    verifier.verify(1.0)
    verifier.verify(True)


def test_id():
    def my_function(x: object) -> int:
        return id(x)

    a = object()
    verifier = verifier_for(my_function)
    verifier.verify(a, clone_arguments=False)


def test_int():
    def my_function(x: any) -> int:
        return int(x)

    verifier = verifier_for(my_function)
    verifier.verify(1.5, expected_result=1)
    verifier.verify(1.0, expected_result=1)
    verifier.verify('2', expected_result=2)


def test_isinstance():
    def my_function(x: object, y: Union[type, tuple]) -> bool:
        return isinstance(x, y)

    verifier = verifier_for(my_function)
    verifier.verify(1, int, expected_result=True)
    verifier.verify(True, int, expected_result=True)
    verifier.verify(1.0, int, expected_result=False)
    verifier.verify(int, int, expected_result=False)
    verifier.verify(1.0, (int, float), expected_result=True)


def test_issubclass():
    def my_function(x: any, y: Union[type, tuple]) -> bool:
        return issubclass(x, y)

    verifier = verifier_for(my_function)
    verifier.verify(1, int, expected_error=TypeError)
    verifier.verify(int, int, expected_result=True)
    verifier.verify(bool, int, expected_result=True)
    verifier.verify(float, int, expected_result=False)
    verifier.verify(float, (int, float), expected_result=True)


def test_iter():
    def my_function(x: Iterable) -> Iterator:
        return iter(x)

    def predicate(iterator):
        for i in range(4):
            assert next(iterator) == i + 1

        try:
            next(iterator)
            return False
        except StopIteration:
            return True

    verifier = verifier_for(my_function)
    verifier.verify_property([1, 2, 3, 4], predicate=predicate)


def test_len():
    def my_function(x: Sized) -> int:
        return len(x)

    verifier = verifier_for(my_function)
    verifier.verify([1, 2, 3], expected_result=3)
    verifier.verify((4, 5), expected_result=2)
    verifier.verify({6}, expected_result=1)
    verifier.verify({
        'a': 1,
        'b': 2,
        'c': 3
    }, expected_result=3)


def test_list():
    def my_function(x: Iterable) -> list:
        return list(x)

    def my_function_no_args() -> list:
        return list()

    verifier = verifier_for(my_function)
    no_args_verifier = verifier_for(my_function_no_args)

    verifier.verify((1, 2, 3), expected_result=[1, 2, 3])
    no_args_verifier.verify(expected_result=[])


def test_locals():
    def my_function():
        return locals()

    # Not using verifier since locals is not implemented in Java since it would involve reading frame variable slots
    # to get the current value of local variables
    java_function = jpyinterpreter.as_java(my_function)
    with pytest.raises(ValueError) as excinfo:
        java_function()

    assert 'builtin locals() is not supported when executed in Java bytecode' in str(excinfo.value)


def test_map():
    def my_function(function: Callable, iterable: Iterable) -> tuple:
        return tuple(map(function, iterable))

    def my_function_two_args(function: Callable, iterable1: Iterable, iterable2: Iterable) -> tuple:
        return tuple(map(function, iterable1, iterable2))

    verifier = verifier_for(my_function)
    two_args_verifier = verifier_for(my_function_two_args)

    verifier.verify(lambda x: x + 1, [1, 2, 3], expected_result=(2, 3, 4))
    two_args_verifier.verify(lambda x, y: x + y, [1, 2, 3], [3, 4, 5], expected_result=(4, 6, 8))
    two_args_verifier.verify(lambda x, y: x + y, [1, 2], [3, 4, 5], expected_result=(4, 6))
    two_args_verifier.verify(lambda x, y: x + y, [1, 2, 3], [3, 4], expected_result=(4, 6))


def test_min():
    def my_function(x: Iterable) -> any:
        return min(x)

    def my_function_two_args(x: any, y: any) -> any:
        return min(x, y)

    verifier = verifier_for(my_function)
    two_args_verifier = verifier_for(my_function_two_args)

    verifier.verify([1, 2, 3], expected_result=1)
    verifier.verify([3, 2, 1], expected_result=1)
    two_args_verifier.verify(1, 2, expected_result=1)
    two_args_verifier.verify(2, 1, expected_result=1)


def test_max():
    def my_function(x: Iterable) -> any:
        return max(x)

    def my_function_two_args(x: any, y: any) -> any:
        return max(x, y)

    verifier = verifier_for(my_function)
    two_args_verifier = verifier_for(my_function_two_args)

    verifier.verify([1, 2, 3], expected_result=3)
    verifier.verify([3, 2, 1], expected_result=3)
    two_args_verifier.verify(1, 2, expected_result=2)
    two_args_verifier.verify(2, 1, expected_result=2)


def test_next():
    def my_function(x: Iterable) -> any:
        i = iter(x)
        return next(i)

    verifier = verifier_for(my_function)
    verifier.verify([1, 2, 3], expected_result=1)


def test_object():
    def my_function(x: any) -> bool:
        # cannot really do anything with a plain object
        return isinstance(x, object)

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=True)
    verifier.verify('a', expected_result=True)
    verifier.verify(int, expected_result=True)


def test_oct():
    def my_function(x: int) -> str:
        return oct(x)

    verifier = verifier_for(my_function)
    verifier.verify(15)
    verifier.verify(-15)


def test_ord():
    def my_function(x: str) -> int:
        return ord(x)

    verifier = verifier_for(my_function)
    verifier.verify('a')
    verifier.verify('\n')


def test_pow():
    def my_function(x: int, y: int) -> any:
        return pow(x, y)

    def my_function_with_mod(x: int, y: int, z: int) -> any:
        return pow(x, y, z)

    verifier = verifier_for(my_function)
    with_mod_verifier = verifier_for(my_function_with_mod)

    verifier.verify(2, 3, expected_result=8)
    verifier.verify(2, -3, expected_result=0.125)

    with_mod_verifier.verify(2, 3, 3, expected_result=2)
    with_mod_verifier.verify(2, -1, 3, expected_result=2)


def test_range():
    def my_function(x: int) -> tuple:
        return tuple(range(x))

    def my_function_with_start(start: int, stop: int) -> tuple:
        return tuple(range(start, stop))

    def my_function_with_start_and_step(start: int, stop: int, step: int) -> tuple:
        return tuple(range(start, stop, step))

    verifier = verifier_for(my_function)
    with_start_verifier = verifier_for(my_function_with_start)
    with_start_and_step_verifier = verifier_for(my_function_with_start_and_step)

    verifier.verify(5, expected_result=(0, 1, 2, 3, 4))

    with_start_verifier.verify(5, 10, expected_result=(5, 6, 7, 8, 9))

    with_start_and_step_verifier.verify(5, 10, 2, expected_result=(5, 7, 9))
    with_start_and_step_verifier.verify(10, 5, -2, expected_result=(10, 8, 6))


def test_repr():
    def my_function(x: any) -> str:
        return repr(x)

    verifier = verifier_for(my_function)

    verifier.verify(10, expected_result='10')
    verifier.verify('a\nstring\nwith\nnew lines', expected_result="'a\\nstring\\nwith\\nnew lines'")
    verifier.verify([1, '2', 3], expected_result="[1, '2', 3]")


def test_reversed():
    def my_function(x: Reversible) -> tuple:
        return tuple(reversed(x))

    class TestClass:
        def __reversed__(self):
            yield from range(10, 1, -1)

    verifier = verifier_for(my_function)

    verifier.verify([1, 2, 3], expected_result=(3, 2, 1))
    verifier.verify((1, 2, 3), expected_result=(3, 2, 1))
    verifier.verify(TestClass(), expected_result=(10, 9, 8, 7, 6, 5, 4, 3, 2))


def test_round():
    def my_function(x: any) -> int:
        return round(x)

    def my_function_with_precision(x: any, y: int) -> any:
        return round(x, y)

    verifier = verifier_for(my_function)
    with_precision_verifier = verifier_for(my_function_with_precision)

    verifier.verify(15, expected_result=15)
    verifier.verify(1.5, expected_result=2)
    verifier.verify(1.2, expected_result=1)
    verifier.verify(1.7, expected_result=2)
    verifier.verify(2.5, expected_result=2)

    with_precision_verifier.verify(15, 0, expected_result=15)
    with_precision_verifier.verify(15, 2, expected_result=15)
    with_precision_verifier.verify(15, -1, expected_result=20)
    with_precision_verifier.verify(25, -1, expected_result=20)

    with_precision_verifier.verify(1.076, 2, expected_result=1.08)
    with_precision_verifier.verify(1.024, 2, expected_result=1.02)
    with_precision_verifier.verify(1.045, 2, expected_result=1.04)
    with_precision_verifier.verify(1.055, 2, expected_result=1.05)

    with_precision_verifier.verify(176.0, -1, expected_result=180.0)
    with_precision_verifier.verify(124.0, -1, expected_result=120.0)
    with_precision_verifier.verify(145.0, -1, expected_result=140.0)
    with_precision_verifier.verify(155.0, -1, expected_result=160.0)


def test_set():
    def my_function() -> set:
        return set()

    def my_function_with_arg(x: Iterable) -> set:
        return set(x)

    verifier = verifier_for(my_function)
    with_arg_verifier = verifier_for(my_function_with_arg)

    verifier.verify(expected_result=set())
    with_arg_verifier.verify([1, 2, 2, 3], expected_result={1, 2, 3})


def test_setattr():
    def my_function(x: any, name: str, value: any) -> any:
        setattr(x, name, value)
        return getattr(x, name)

    class TestObject:
        pass

    verifier = verifier_for(my_function)

    a = TestObject()
    a.test = 'value 1'

    verifier.verify(a, 'test', 'value 2', expected_result='value 2')


def test_slice():
    def my_function(start: Union[int, SupportsIndex], stop: Union[int, SupportsIndex]):
        return slice(start, stop)

    def my_function_with_step(start: Union[int, SupportsIndex], stop: Union[int, SupportsIndex], step: int):
        return slice(start, stop, step)

    class MyClassWithIndex:
        def __init__(self, index):
            self.index = index

        def __index__(self):
            return self.index

        def __eq__(self, other):
            return self.index == other.index

    verifier = verifier_for(my_function)
    with_step_verifier = verifier_for(my_function_with_step)

    verifier.verify(1, 5, expected_result=slice(1, 5))
    verifier.verify(1,  MyClassWithIndex(3), expected_result=slice(1,  MyClassWithIndex(3)))
    with_step_verifier.verify(1, 5, 2, expected_result=slice(1, 5, 2))


def test_sorted():
    def my_function(x: Sequence) -> Sequence:
        return sorted(x)

    def my_function_with_key(x: Sequence, y: Callable[[any], any]) -> Sequence:
        return sorted(x, key=y)

    def my_function_with_reverse(x: Sequence, y: bool) -> Sequence:
        return sorted(x, reverse=y)

    def my_function_with_key_and_reverse(x: Sequence, y: Callable[[any], any], z: bool) -> Sequence:
        return sorted(x, key=y, reverse=z)

    verifier = verifier_for(my_function)
    with_key_verifier = verifier_for(my_function_with_key)
    with_reverse_verifier = verifier_for(my_function_with_reverse)
    with_key_and_reverse_verifier = verifier_for(my_function_with_key_and_reverse)

    verifier.verify([2, 1, 3, 4], expected_result=[1, 2, 3, 4])
    with_key_verifier.verify([2, 1, 3, 4], lambda x: -x, expected_result=[4, 3, 2, 1])
    with_reverse_verifier.verify([2, 1, 3, 4], True, expected_result=[4, 3, 2, 1])
    with_key_and_reverse_verifier.verify([2, 1, 3, 4], lambda x: -x, True, expected_result=[1, 2, 3, 4])


def test_str():
    def my_function(x: any) -> str:
        return str(x)

    class TestObject:
        def __str__(self):
            return 'A TestObject Instance'

    verifier = verifier_for(my_function)
    verifier.verify(10, expected_result='10')
    verifier.verify(1.0, expected_result='1.0')
    verifier.verify('text', expected_result='text')
    verifier.verify([1, '2', 3], expected_result="[1, '2', 3]")
    verifier.verify(TestObject(), expected_result='A TestObject Instance')


def test_sum():
    def my_function(x: Iterable) -> any:
        return sum(x)

    def my_function_with_start(x: Iterable, y: any) -> any:
        return sum(x, y)

    verifier = verifier_for(my_function)
    with_start_verifier = verifier_for(my_function_with_start)

    verifier.verify([], expected_result=0)
    verifier.verify([1, 3, 5], expected_result=9)
    verifier.verify([1.0, 3.0, 5.0], expected_result=9.0)
    with_start_verifier.verify([], 1.5, expected_result=1.5)
    with_start_verifier.verify([1, 3, 5], 1.5, expected_result=10.5)
    with_start_verifier.verify([1.0, 3.0, 5.0], 1.5, expected_result=10.5)


def test_tuple():
    def my_function() -> tuple:
        return tuple()

    def my_function_with_arg(x: Iterable) -> tuple:
        return tuple(x)

    verifier = verifier_for(my_function)
    with_arg_verifier = verifier_for(my_function_with_arg)

    verifier.verify(expected_result=tuple())
    with_arg_verifier.verify([1, 2, 2, 3], expected_result=(1, 2, 2, 3))


def test_type():
    def my_function(x: any) -> type:
        return type(x)

    class MyObject:
        pass

    verifier = verifier_for(my_function)

    verifier.verify(10, expected_result=int)
    verifier.verify('text', expected_result=str)
    verifier.verify([1, 2], expected_result=list)
    verifier.verify(MyObject(), expected_result=MyObject)
    verifier.verify(MyObject, expected_result=type)


def test_zip():
    def my_function(iterables: Iterable) -> tuple:
        return tuple(zip(*iterables))

    def my_function_strict(iterables: Iterable, is_strict: bool) -> tuple:
        return tuple(zip(*iterables, strict=is_strict))

    verifier = verifier_for(my_function)
    strict_verifier = verifier_for(my_function_strict)

    verifier.verify([], expected_result=tuple())
    verifier.verify(((1, 2, 3),), expected_result=((1,), (2,), (3,)))
    verifier.verify(((1, 2, 3), (4, 5, 6)), expected_result=((1, 4), (2, 5), (3, 6)))
    verifier.verify(((1, 2, 3), (4, 5)), expected_result=((1, 4), (2, 5)))
    verifier.verify(((1, 2), (4, 5, 6)), expected_result=((1, 4), (2, 5)))

    if sys.version_info >= (3, 10):
        strict_verifier.verify(((1, 2, 3),), True, expected_result=((1,), (2,), (3,)))
        strict_verifier.verify(((1, 2, 3), (4, 5, 6)), True, expected_result=((1, 4), (2, 5), (3, 6)))
        strict_verifier.verify(((1, 2, 3), (4, 5)), True, expected_error=ValueError)
        strict_verifier.verify(((1, 2), (4, 5, 6)), True, expected_error=ValueError)


def test_builtin_exceptions():
    import builtins

    for key in dir(builtins):
        value = getattr(builtins, key)
        if isinstance(value, type) and issubclass(value, BaseException):
            exception_class = value

            if issubclass(exception_class, OSError) or issubclass(exception_class, SyntaxError) or \
                    issubclass(exception_class, UnicodeError) or exception_class.__name__.endswith('Group'):
                # Exception groups are 3.11+, and take two arguments
                # these errors take a different number of arguments, and thus the below code will not work
                continue

            def my_function() -> exception_class:
                return exception_class("my argument")

            verifier = verifier_for(my_function)
            verifier.verify_property(predicate=
                                     lambda error: isinstance(error, exception_class) and len(error.args) == 1 and
                                                   'my argument' in error.args[0])
