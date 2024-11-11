import pytest
from typing import Type

from .conftest import verifier_for


def test_create_instance():
    class A:
        value: int

        def __init__(self, value):
            self.value = value

        def __eq__(self, other):
            if not isinstance(other, A):
                return False
            return self.value == other.value

    def create_instance(x: int) -> A:
        return A(x)

    verifier = verifier_for(create_instance)

    verifier.verify(1, expected_result=A(1))
    verifier.verify(2, expected_result=A(2))
    verifier.verify(3, expected_result=A(3))


def test_type_coercing():
    class A:
        value: float

        def __init__(self, value):
            self.value = value

        def __eq__(self, other):
            if not isinstance(other, A):
                return False
            return self.value == other.value

    def create_instance(x: int) -> A:
        return A(x)

    verifier = verifier_for(create_instance)

    verifier.verify(1, expected_result=A(1))
    verifier.verify(2, expected_result=A(2))
    verifier.verify(3, expected_result=A(3))


def test_deleted_field():
    class A:
        value: int

        def __init__(self, value):
            self.value = value

        def my_method(self, param):
            return self.value + param

    def my_method(x: A, y: int) -> int:
        return x.my_method(y)

    verifier = verifier_for(my_method)

    a = A(1)
    verifier.verify(a, 1, expected_result=2)

    del a.value

    verifier.verify(a, 1, expected_error=AttributeError)


def test_virtual_method():
    class A:
        value: int

        def __init__(self, value):
            self.value = value

        def my_method(self, param):
            return self.value + param

    def my_method(x: A, y: int) -> int:
        return x.my_method(y)

    verifier = verifier_for(my_method)

    verifier.verify(A(1), 1, expected_result=2)
    verifier.verify(A(1), 2, expected_result=3)
    verifier.verify(A(2), 1, expected_result=3)
    verifier.verify(A(2), 2, expected_result=4)


def test_static_method():
    class A:
        @staticmethod
        def my_method(param: int):
            return 1 + param

    def instance_my_method(x: A, y: int) -> int:
        return x.my_method(y)

    def static_my_method(x: Type[A], y: int) -> int:
        return x.my_method(y)

    instance_verifier = verifier_for(instance_my_method)
    static_verifier = verifier_for(static_my_method)

    static_verifier.verify(A, 1, expected_result=2)
    static_verifier.verify(A, 2, expected_result=3)
    instance_verifier.verify(A(), 3, expected_result=4)
    instance_verifier.verify(A(), 4, expected_result=5)


def test_class_method():
    class A:
        @classmethod
        def my_method(cls: type, parameter: str):
            return cls.__name__ + parameter

    class B(A):
        pass

    def instance_my_method(x: A, y: str) -> str:
        return x.my_method(y)

    def static_my_method(x: Type[A], y: str) -> str:
        return x.my_method(y)

    instance_verifier = verifier_for(instance_my_method)
    static_verifier = verifier_for(static_my_method)

    instance_verifier.verify(A(), '1', expected_result='A1')
    instance_verifier.verify(B(), '2', expected_result='B2')
    static_verifier.verify(A, '3', expected_result='A3')
    static_verifier.verify(B, '4', expected_result='B4')


def test_override_method():
    class A:
        def __init__(self, value):
            self.value = value

        def my_method(self):
            return self.value

    class B(A):
        def my_method(self):
            return self.value + 1

    def my_method(x: A) -> int:
        return x.my_method()

    verifier = verifier_for(my_method)

    verifier.verify(A(1), expected_result=1)
    verifier.verify(B(1), expected_result=2)

    verifier.verify(A(2), expected_result=2)
    verifier.verify(B(2), expected_result=3)


def test_simple_super_method():
    class A:
        def __init__(self, start):
            self.start = start

        def my_method(self, text):
            return self.start + text

    class B(A):
        def __init__(self, start, end):
            super().__init__(start)
            self.end = end

        def my_method(self, text):
            return super().my_method(text) + self.end

    def my_function(start: str, end: str, text: str) -> str:
        return B(start, end).my_method(text)

    verifier = verifier_for(my_function)

    verifier.verify('start', 'end', ' middle ', expected_result='start middle end')


def test_chained_super_method():
    class A:
        def __init__(self, start):
            self.start = start

        def my_method(self, text):
            return self.start + text

    class B(A):
        def __init__(self, start, end):
            super().__init__(start)
            self.end = end

        def my_method(self, text):
            return super().my_method(text) + self.end

    class C(B):
        def __init__(self, start, end):
            super().__init__(start, end)

        def my_method(self, text):
            return '[' + super().my_method(text) + ']'

    def my_function(start: str, end: str, text: str) -> str:
        return C(start, end).my_method(text)

    verifier = verifier_for(my_function)

    verifier.verify('start', 'end', ' middle ', expected_result='[start middle end]')


def test_virtual_keyword_arguments():
    class A:
        def helper(self, a: int, b: int) -> int:
            return a - b

    def my_function(a: int, b: int) -> int:
        return A().helper(b=b, a=a)

    verifier = verifier_for(my_function)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)


def test_virtual_default_arguments():
    class A:
        def helper(self, a: int, b: int = 1, c: str = '') -> int:
            return a - b - len(c)

    def my_function(a: int) -> int:
        return A().helper(a)

    verifier = verifier_for(my_function)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)


def test_virtual_vargs():
    class A:
        def helper(self, *items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(a: int, b: int, c: int) -> int:
        return A().helper(a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)


def test_virtual_kwargs():
    class A:
        def helper(self, **kwargs: int) -> frozenset:
            return frozenset(kwargs.items())

    def my_function(a: int, b: int) -> frozenset:
        return A().helper(first=a, second=b)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))


def test_virtual_unpack_iterable():
    class A:
        def helper(self, *items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(iterable: tuple) -> int:
        return A().helper(*iterable)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)


def test_virtual_unpack_keywords():
    class A:
        def helper(self, **kwargs: int) -> set:
            return set(kwargs.items())

    def my_function(items: dict) -> set:
        return A().helper(**items)

    verifier = verifier_for(my_function)

    verifier.verify({
        'first': 1,
        'second': 2
    }, expected_result={('first', 1), ('second', 2)})

    verifier.verify({
        'third': 3,
        'fourth': 3
    }, expected_result={('third', 3), ('fourth', 3)})

    verifier.verify({
        'alone': 0,
    }, expected_result={('alone', 0)})

    verifier.verify(dict(), expected_result=set())


def test_virtual_unpack_iterable_and_keywords():
    class A:
        def helper(self, first: int, *positional: int, key: str, **keywords: str):
            return first, positional, key, keywords

    def my_function(items, keywords):
        return A().helper(*items, **keywords)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))


def test_virtual_default_with_vargs():
    class A:
        def helper(self, *items: int, start: int = 10) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(items):
        return A().helper(*items)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)


def test_virtual_vargs_with_manatory_args():
    class A:
        def helper(self, start: int, *items: int) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(a, b, c):
        return A().helper(10, a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)


def test_static_keyword_arguments():
    class A:
        @staticmethod
        def helper(a: int, b: int) -> int:
            return a - b

    def my_function(a: int, b: int) -> int:
        return A.helper(b=b, a=a)

    def my_function_instance(a: int, b: int) -> int:
        return A().helper(b=b, a=a)

    verifier = verifier_for(my_function)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)


def test_static_default_arguments():
    class A:
        @staticmethod
        def helper(a: int, b: int = 1, c: str = '') -> int:
            return a - b - len(c)

    def my_function(a: int) -> int:
        return A.helper(a)

    def my_function_instance(a: int) -> int:
        return A().helper(a)

    verifier = verifier_for(my_function)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)


def test_static_vargs():
    class A:
        @staticmethod
        def helper(*items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(a: int, b: int, c: int) -> int:
        return A.helper(a, b, c)

    def my_function_instance(a: int, b: int, c: int) -> int:
        return A().helper(a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)


def test_static_kwargs():
    class A:
        @staticmethod
        def helper(**kwargs: int) -> frozenset:
            return frozenset(kwargs.items())

    def my_function(a: int, b: int) -> frozenset:
        return A.helper(first=a, second=b)

    def my_function_instance(a: int, b: int) -> frozenset:
        return A().helper(first=a, second=b)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))


def test_static_unpack_iterable():
    class A:
        @staticmethod
        def helper(*items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(iterable: tuple) -> int:
        return A.helper(*iterable)

    def my_function_instance(iterable: tuple) -> int:
        return A().helper(*iterable)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)


def test_static_unpack_keywords():
    class A:
        @staticmethod
        def helper(**kwargs: int) -> set:
            return set(kwargs.items())

    def my_function(items: dict) -> set:
        return A.helper(**items)

    def my_function_instance(items: dict) -> set:
        return A().helper(**items)

    verifier = verifier_for(my_function)

    verifier.verify({
        'first': 1,
        'second': 2
    }, expected_result={('first', 1), ('second', 2)})

    verifier.verify({
        'third': 3,
        'fourth': 3
    }, expected_result={('third', 3), ('fourth', 3)})

    verifier.verify({
        'alone': 0,
    }, expected_result={('alone', 0)})

    verifier.verify(dict(), expected_result=set())

    verifier = verifier_for(my_function_instance)

    verifier.verify({
        'first': 1,
        'second': 2
    }, expected_result={('first', 1), ('second', 2)})

    verifier.verify({
        'third': 3,
        'fourth': 3
    }, expected_result={('third', 3), ('fourth', 3)})

    verifier.verify({
        'alone': 0,
    }, expected_result={('alone', 0)})

    verifier.verify(dict(), expected_result=set())


def test_static_unpack_iterable_and_keywords():
    class A:
        @staticmethod
        def helper(first: int, *positional: int, key: str, **keywords: str):
            return first, positional, key, keywords

    def my_function(items, keywords):
        return A.helper(*items, **keywords)

    def my_function_instance(items, keywords):
        return A().helper(*items, **keywords)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))
    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))


def test_static_default_with_vargs():
    class A:
        @staticmethod
        def helper(*items: int, start: int = 10) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(items):
        return A.helper(*items)

    def my_function_instance(items):
        return A().helper(*items)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)

    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)


def test_static_vargs_with_manatory_args():
    class A:
        @staticmethod
        def helper(start: int, *items: int) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(a, b, c):
        return A.helper(10, a, b, c)

    def my_function_instance(a, b, c):
        return A().helper(10, a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)


def test_class_keyword_arguments():
    class A:
        @classmethod
        def helper(cls: type, a: int, b: int) -> int:
            return a - b

    def my_function(a: int, b: int) -> int:
        return A.helper(b=b, a=a)

    def my_function_instance(a: int, b: int) -> int:
        return A().helper(b=b, a=a)

    verifier = verifier_for(my_function)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)


def test_class_default_arguments():
    class A:
        @classmethod
        def helper(cls: type, a: int, b: int = 1, c: str = '') -> int:
            return a - b - len(c)

    def my_function(a: int) -> int:
        return A.helper(a)

    def my_function_instance(a: int) -> int:
        return A().helper(a)

    verifier = verifier_for(my_function)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)


def test_class_vargs():
    class A:
        @classmethod
        def helper(cls: type, *items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(a: int, b: int, c: int) -> int:
        return A.helper(a, b, c)

    def my_function_instance(a: int, b: int, c: int) -> int:
        return A().helper(a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)


def test_class_kwargs():
    class A:
        @classmethod
        def helper(cls: type, **kwargs: int) -> frozenset:
            return frozenset(kwargs.items())

    def my_function(a: int, b: int) -> frozenset:
        return A.helper(first=a, second=b)

    def my_function_instance(a: int, b: int) -> frozenset:
        return A().helper(first=a, second=b)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))


def test_class_unpack_iterable():
    class A:
        @classmethod
        def helper(cls: type, *items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(iterable: tuple) -> int:
        return A.helper(*iterable)

    def my_function_instance(iterable: tuple) -> int:
        return A().helper(*iterable)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)


def test_class_unpack_keywords():
    class A:
        @classmethod
        def helper(cls: type, **kwargs: int) -> set:
            return set(kwargs.items())

    def my_function(items: dict) -> set:
        return A.helper(**items)

    def my_function_instance(items: dict) -> set:
        return A().helper(**items)

    verifier = verifier_for(my_function)

    verifier.verify({
        'first': 1,
        'second': 2
    }, expected_result={('first', 1), ('second', 2)})

    verifier.verify({
        'third': 3,
        'fourth': 3
    }, expected_result={('third', 3), ('fourth', 3)})

    verifier.verify({
        'alone': 0,
    }, expected_result={('alone', 0)})

    verifier.verify(dict(), expected_result=set())

    verifier = verifier_for(my_function_instance)

    verifier.verify({
        'first': 1,
        'second': 2
    }, expected_result={('first', 1), ('second', 2)})

    verifier.verify({
        'third': 3,
        'fourth': 3
    }, expected_result={('third', 3), ('fourth', 3)})

    verifier.verify({
        'alone': 0,
    }, expected_result={('alone', 0)})

    verifier.verify(dict(), expected_result=set())


def test_class_unpack_iterable_and_keywords():
    class A:
        @classmethod
        def helper(cls: type, first: int, *positional: int, key: str, **keywords: str):
            return first, positional, key, keywords

    def my_function(items, keywords):
        return A.helper(*items, **keywords)

    def my_function_instance(items, keywords):
        return A().helper(*items, **keywords)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))
    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))


def test_class_default_with_vargs():
    class A:
        @classmethod
        def helper(cls: type, *items: int, start: int = 10) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(items):
        return A.helper(*items)

    def my_function_instance(items):
        return A().helper(*items)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)

    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)


def test_class_vargs_with_manatory_args():
    class A:
        @classmethod
        def helper(cls: type, start: int, *items: int) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(a, b, c):
        return A.helper(10, a, b, c)

    def my_function_instance(a, b, c):
        return A().helper(10, a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)


def test_enum_translate_to_class():
    from enum import Enum
    from jpyinterpreter import translate_python_class_to_java_class
    from ai.timefold.jpyinterpreter.types.wrappers import CPythonType

    class Color(Enum):
        RED = 'RED'
        GREEN = 'GREEN'
        BLUE = 'BLUE'

    def is_red(color: Color):
        return color is Color.RED

    translated_class = translate_python_class_to_java_class(Color)
    assert not isinstance(translated_class, CPythonType)

    verifier = verifier_for(is_red)
    verifier.verify(Color.RED, expected_result=True)
    verifier.verify(Color.GREEN, expected_result=False)
    verifier.verify(Color.BLUE, expected_result=False)


def test_enum_as_attribute_in_class():
    from enum import Enum
    from dataclasses import dataclass

    class Color(Enum):
        RED = 'RED'
        GREEN = 'GREEN'
        BLUE = 'BLUE'

    @dataclass
    class Order:
        color: Color

    def is_red(order: Order):
        return order.color is Color.RED

    verifier = verifier_for(is_red)
    verifier.verify(Order(Color.RED), expected_result=True)
    verifier.verify(Order(Color.GREEN), expected_result=False)
    verifier.verify(Order(Color.BLUE), expected_result=False)


def test_class_annotations():
    from typing import Annotated
    from jpype import JInt
    from java.lang import Deprecated, Integer
    from java.lang.annotation import Target, ElementType
    from ai.timefold.solver.core.api.domain.variable import PiggybackShadowVariable
    from jpyinterpreter import (add_class_annotation, JavaAnnotation, translate_python_class_to_java_class,
                                get_java_type_for_python_type, convert_to_java_python_like_object)
    from ai.timefold.jpyinterpreter.types.numeric import PythonInteger

    class B:
        some_field: int

    @add_class_annotation(Deprecated,
                          forRemoval=True,
                          since='0.0.0')
    class A:
        my_field: Annotated[int, JavaAnnotation(Deprecated, {
            'forRemoval': True,
            'since': '1.0.0'
        }), 'extra metadata',
        JavaAnnotation(Target, {
            'value': [ElementType.CONSTRUCTOR, ElementType.METHOD]
        }),
        JavaAnnotation(PiggybackShadowVariable, {
            'shadowVariableName': 'some_field',
            'shadowEntityClass': B
        })
        ]
        type_overridden: Annotated[int, JavaAnnotation(Deprecated, {
            'forRemoval': True,
            'since': '1.0.0'
        }, field_type_override=Integer)]
        primitive_type_overridden: Annotated[int, JavaAnnotation(Deprecated, {
            'forRemoval': True,
            'since': '1.0.0'
        }, field_type_override=JInt)]

        def my_method(self) -> Annotated[str, 'extra', JavaAnnotation(Deprecated, {
            'forRemoval': False,
            'since': '2.0.0'
        })]:
            return 'hello world'

    translated_class = translate_python_class_to_java_class(A).getJavaClass()
    annotations = translated_class.getAnnotations()
    assert len(annotations) == 1
    assert isinstance(annotations[0], Deprecated)
    assert annotations[0].forRemoval()
    assert annotations[0].since() == '0.0.0'

    my_field_getter = translated_class.getMethod('getMy_field')
    annotations = my_field_getter.getAnnotations()
    assert my_field_getter.getReturnType() == PythonInteger.class_
    assert len(annotations) == 3
    assert isinstance(annotations[0], Deprecated)
    assert annotations[0].forRemoval()
    assert annotations[0].since() == '1.0.0'
    assert isinstance(annotations[1], Target)
    assert list(annotations[1].value()) == [ElementType.CONSTRUCTOR, ElementType.METHOD]
    assert isinstance(annotations[2], PiggybackShadowVariable)
    assert annotations[2].shadowVariableName() == 'some_field'
    assert annotations[2].shadowEntityClass() == get_java_type_for_python_type(B).getJavaClass()

    type_overridden_getter = translated_class.getMethod('getType_overridden')
    assert type_overridden_getter.getReturnType() == Integer.class_

    type_overridden_getter = translated_class.getMethod('getPrimitive_type_overridden')
    assert type_overridden_getter.getReturnType() == JInt.class_

    annotations = translated_class.getMethod('$method$my_method').getAnnotations()
    assert len(annotations) == 1
    assert isinstance(annotations[0], Deprecated)
    assert annotations[0].forRemoval() is False
    assert annotations[0].since() == '2.0.0'

    a = A()
    a.my_field = 1
    a.type_overridden = 2
    a.primitive_type_overridden = 3

    converted_a = convert_to_java_python_like_object(a)
    assert isinstance(converted_a.getMy_field(), PythonInteger)
    assert converted_a.getMy_field().equals(PythonInteger.valueOf(1))
    assert converted_a.getType_overridden() == 2
    assert converted_a.getPrimitive_type_overridden() == 3


def test_extra_attributes():
    from jpyinterpreter import convert_to_java_python_like_object, unwrap_python_like_object

    class A:
        pass

    a = A()
    a.name = 'Name'

    converted_a = convert_to_java_python_like_object(a)

    assert getattr(converted_a, '$getAttributeOrNull')('name').value == 'Name'

    unwrapped_a = unwrap_python_like_object(converted_a)

    assert unwrapped_a.name == 'Name'


def function_attribute_function():
    return 10


def test_function_attributes():
    from jpyinterpreter import convert_to_java_python_like_object, unwrap_python_like_object

    class A:
        dispatch = {
            'my_function': function_attribute_function,
        }

    def run():
        return A.dispatch['my_function']()

    a = A()

    converted_a = convert_to_java_python_like_object(a)
    unwrapped_a = unwrap_python_like_object(converted_a)

    assert unwrapped_a.dispatch['my_function'] is function_attribute_function

    verifier = verifier_for(run)
    verifier.verify(expected_result=10)


def test_java_class_as_field_type():
    from ai.timefold.jpyinterpreter import TypeHint
    from jpyinterpreter import translate_python_class_to_java_class

    class A:
        my_field: TypeHint

    translated_class = translate_python_class_to_java_class(A).getJavaClass()
    field_type = translated_class.getField('my_field').getType()
    assert field_type.getName() == TypeHint.class_.getName()


def test_generic_field_type():
    from typing import List
    from ai.timefold.jpyinterpreter.types import PythonString
    from jpyinterpreter import translate_python_class_to_java_class

    class A:
        my_field: List[str]

    translated_class = translate_python_class_to_java_class(A).getJavaClass()
    field_type = translated_class.getField('my_field').getGenericType()
    assert field_type.getActualTypeArguments()[0].getName() == PythonString.class_.getName()


def test_getter_type():
    from typing import Optional, Union
    from ai.timefold.jpyinterpreter.types import PythonString, PythonBytes
    from ai.timefold.jpyinterpreter.types.numeric import PythonInteger
    from jpyinterpreter import translate_python_class_to_java_class

    class A:
        str_field: Optional[str]
        int_field: Union[int, None]
        bytes_field: bytes | None

    translated_class = translate_python_class_to_java_class(A).getJavaClass()
    str_field_getter_type = translated_class.getMethod('getStr_field').getReturnType()
    assert str_field_getter_type == PythonString.class_

    int_field_getter_type = translated_class.getMethod('getInt_field').getReturnType()
    assert int_field_getter_type == PythonInteger.class_

    bytes_field_getter_type = translated_class.getMethod('getBytes_field').getReturnType()
    assert bytes_field_getter_type == PythonBytes.class_


def test_marker_interface():
    from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference
    from jpyinterpreter import translate_python_class_to_java_class, add_java_interface

    @add_java_interface(OpaquePythonReference)
    class A:
        pass

    translated_class = translate_python_class_to_java_class(A).getJavaClass()
    assert OpaquePythonReference.class_.isAssignableFrom(translated_class)


def test_marker_interface_string():
    from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference
    from jpyinterpreter import translate_python_class_to_java_class, add_java_interface

    @add_java_interface('ai.timefold.jpyinterpreter.types.wrappers.OpaquePythonReference')
    class A:
        pass

    translated_class = translate_python_class_to_java_class(A).getJavaClass()
    assert OpaquePythonReference.class_.isAssignableFrom(translated_class)


def test_functional_interface():
    from java.util.function import ToIntFunction
    from jpyinterpreter import translate_python_class_to_java_class, add_java_interface

    @add_java_interface(ToIntFunction)
    class A:
        def applyAsInt(self, argument: int):
            return argument + 1

    translated_class = translate_python_class_to_java_class(A).getJavaClass()
    assert ToIntFunction.class_.isAssignableFrom(translated_class)
    java_object = translated_class.getConstructor().newInstance()
    assert java_object.applyAsInt(1) == 2


def test_extend_interface_wrapper():
    from ai.timefold.jpyinterpreter.test import TestdataExtendedInterface
    from jpyinterpreter import translate_python_class_to_java_class, add_java_interface

    @add_java_interface(TestdataExtendedInterface)
    class A:
        def stringMethod(self, name):
            return self.string_method(name)

        def intMethod(self, value):
            return self.int_method(value)

        def string_method(self, name):
            raise NotImplementedError

        def int_method(self, value):
            raise NotImplementedError

    class B(A):
        def string_method(self, name: str) -> str:
            return f'Hello {name}!'

        def int_method(self, value: int) -> int:
            return value + 1

    translated_class = translate_python_class_to_java_class(B).getJavaClass()
    assert TestdataExtendedInterface.class_.isAssignableFrom(translated_class)
    java_object = translated_class.getConstructor().newInstance()
    assert TestdataExtendedInterface.getString(java_object, 'World') == 'Hello World!'
    assert TestdataExtendedInterface.getInt(java_object, 1) == 2


def test_python_java_type_mapping():
    from java.lang import String
    from jpyinterpreter import (translate_python_class_to_java_class,
                                add_python_java_type_mapping, unwrap_python_like_object)
    from jpype import JImplements, JOverride
    from dataclasses import dataclass

    @dataclass
    class PythonClass:
        data: str

    python_class_type = translate_python_class_to_java_class(PythonClass)

    @JImplements('ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping')
    class MyMapping:
        @JOverride
        def getPythonType(self):
            return python_class_type

        @JOverride
        def getJavaType(self):
            return String.class_

        @JOverride
        def toPythonObject(self, java_object):
            from ai.timefold.jpyinterpreter.types import PythonString
            instance = python_class_type.getJavaClass().getConstructor().newInstance()
            instance.data = PythonString.valueOf(java_object)
            return instance

        @JOverride
        def toJavaObject(self, python_object):
            return python_object.data.getValue()

    add_python_java_type_mapping(MyMapping())

    @dataclass
    class A:
        data: PythonClass | None

    translated_class = translate_python_class_to_java_class(A).getJavaClass()
    assert translated_class.getMethod('getData').getReturnType() == String.class_
    assert translated_class.getMethod('setData', String.class_) is not None

    java_object = translated_class.getConstructor().newInstance()
    java_object.setData('test')
    assert unwrap_python_like_object(translated_class.getField('data').get(java_object)) == PythonClass('test')
    assert java_object.getData() == 'test'

    java_object.setData(None)
    assert unwrap_python_like_object(translated_class.getField('data').get(java_object)) is None
    assert java_object.getData() is None


def test_class_properties():
    from jpyinterpreter import translate_python_class_to_java_class, unwrap_python_like_object
    from dataclasses import dataclass
    from java.lang import NoSuchFieldException
    from ai.timefold.jpyinterpreter.types import PythonString

    @dataclass
    class Car:
        name: str

        @property
        def speed(self):
            return 100

        def is_fast(self) -> bool:
            return self.speed > 50

    translated_class = translate_python_class_to_java_class(Car)
    java_class = translated_class.getJavaClass()

    with pytest.raises(NoSuchFieldException):
        java_class.getField('speed')

    instance = java_class.getConstructor().newInstance()
    instance.name = PythonString.valueOf('Car')
    assert java_class.getMethod('$method$is_fast').invoke(instance)
    assert unwrap_python_like_object(instance) == Car('Car')
