from .conftest import verifier_for


def test_keyword_arguments():
    def helper(a: int, b: int) -> int:
        return a - b

    def my_function(a: int, b: int) -> int:
        return helper(b=b, a=a)

    verifier = verifier_for(my_function)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)


def test_default_arguments():
    def helper(a: int, b: int = 1, c: str = '') -> int:
        return a - b - len(c)

    def my_function(a: int) -> int:
        return helper(a)

    verifier = verifier_for(my_function)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)


def test_vargs():
    def helper(*items: int) -> int:
        total = 0
        for i in items:
            total += i
        return total

    def my_function(a: int, b: int, c: int) -> int:
        return helper(a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)


def test_kwargs():
    def helper(**kwargs: int) -> frozenset:
        return frozenset(kwargs.items())

    def my_function(a: int, b: int) -> frozenset:
        return helper(first=a, second=b)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))


def test_unpack_iterable():
    def helper(*items: int) -> int:
        total = 0
        for i in items:
            total += i
        return total

    def my_function(iterable: tuple) -> int:
        return helper(*iterable)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)


def test_unpack_keywords():
    def helper(**kwargs: int) -> set:
        return set(kwargs.items())

    def my_function(items: dict) -> set:
        return helper(**items)

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


def test_unpack_iterable_and_keywords():
    def helper(first: int, *positional: int, key: str, **keywords: str):
        return first, positional, key, keywords

    def my_function(items, keywords):
        return helper(*items, **keywords)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))


def test_default_with_vargs():
    def helper(*items: int, start: int = 10) -> int:
        total = start
        for item in items:
            total += item
        return total

    def my_function(items):
        return helper(*items)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)


def test_vargs_with_manatory_args():
    def helper(start: int, *items: int) -> int:
        total = start
        for item in items:
            total += item
        return total

    def my_function(a, b, c):
        return helper(10, a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)


def test_recursion():
    def fib(x: int) -> int:
        if x <= 1:
            return 1
        return fib(x - 1) + fib(x - 2)

    fib_verifier = verifier_for(fib)

    fib_verifier.verify(0, expected_result=1)
    fib_verifier.verify(1, expected_result=1)
    fib_verifier.verify(2, expected_result=2)
    fib_verifier.verify(3, expected_result=3)
    fib_verifier.verify(4, expected_result=5)
    fib_verifier.verify(5, expected_result=8)


def test_alternative_recursion():
    def is_even(x: int) -> bool:
        if x == 0:
            return True
        return is_odd(x - 1)

    def is_odd(x: int) -> bool:
        if x == 0:
            return False
        return is_even(x - 1)

    is_even_verifier = verifier_for(is_even)

    is_even_verifier.verify(0, expected_result=True)
    is_even_verifier.verify(1, expected_result=False)
    is_even_verifier.verify(2, expected_result=True)
    is_even_verifier.verify(3, expected_result=False)
    is_even_verifier.verify(4, expected_result=True)
    is_even_verifier.verify(5, expected_result=False)


def test_inner_function():
    def my_function(x: int) -> int:
        def inner_function(y: int) -> int:
            return y * y

        return inner_function(x) + inner_function(x)

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=2)
    verifier.verify(2, expected_result=8)
    verifier.verify(3, expected_result=18)


def test_read_cell_variable():
    def my_function(x: int) -> int:
        def inner_function(y: int) -> int:
            nonlocal x
            z = y

            def get_const():
                nonlocal z
                return z

            return x * get_const()

        a = inner_function(2)
        x += 1
        b = inner_function(3)

        return a + b

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=8)
    verifier.verify(2, expected_result=13)
    verifier.verify(3, expected_result=18)


def test_modify_cell_variable():
    def my_function(x: int) -> int:
        def inner_function(y: int) -> None:
            nonlocal x
            x += y

        inner_function(1)
        return x

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=2)
    verifier.verify(2, expected_result=3)
    verifier.verify(3, expected_result=4)


def test_nested_cell_variable():
    def my_function(x: int) -> tuple:
        def inner_function_1() -> int:
            nonlocal x
            y = x * x

            def inner_function_2() -> int:
                nonlocal x
                nonlocal y
                out = x + y
                x = y
                return out

            return inner_function_2()

        return inner_function_1(), x

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=(2, 1))
    verifier.verify(2, expected_result=(6, 4))
    verifier.verify(3, expected_result=(12, 9))


def test_code_works_if_compilation_failed():
    def my_function(x: int) -> tuple:
        def inner_function(y: int) -> type:
            nonlocal x

            class MyClass:  # TODO: Replace this with something else that fails when class creation is supported
                def __init__(self):
                    self.outer_arg = x
                    self.inner_arg = y

                def get_args(self):
                    return self.outer_arg, self.inner_arg

            return MyClass

        return inner_function(2 * x)().get_args()

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=(1, 2))
    verifier.verify(2, expected_result=(2, 4))
    verifier.verify(3, expected_result=(3, 6))
