import pytest
from .conftest import verifier_for
from typing import Iterable, Type


def assert_yield_values(expected):
    def predicate(generator):
        for item in expected:
            if next(generator) != item:
                return False

        with pytest.raises(StopIteration):
            next(generator)

        return True

    return predicate


def test_loop_generator():
    def my_function(x: int):
        i = 0
        while i < x:
            yield i
            i += 1

    generator_verifier = verifier_for(my_function)
    generator_verifier.verify_property(1, predicate=assert_yield_values(tuple(range(1))))
    generator_verifier.verify_property(2, predicate=assert_yield_values(tuple(range(2))))
    generator_verifier.verify_property(3, predicate=assert_yield_values(tuple(range(3))))


def test_iterator_loop_generator():
    def my_function(x: int):
        for i in range(x):
            yield i

    generator_verifier = verifier_for(my_function)
    generator_verifier.verify_property(1, predicate=assert_yield_values(tuple(range(1))))
    generator_verifier.verify_property(2, predicate=assert_yield_values(tuple(range(2))))
    generator_verifier.verify_property(3, predicate=assert_yield_values(tuple(range(3))))


def test_exception_in_generator():
    def my_function(x: int):
        i = 0
        while i < x:
            yield i
            if i == 3:
                raise ValueError('x > 3')
            i += 1
        yield x

    def my_function_property(expected, throws_value):
        def predicate(generator):
            for item in expected:
                if next(generator) != item:
                    return False

            if not throws_value:
                with pytest.raises(StopIteration):
                    next(generator)
            else:
                with pytest.raises(ValueError):
                    next(generator)

            return True

        return predicate

    generator_verifier = verifier_for(my_function)
    generator_verifier.verify_property(0, predicate=my_function_property((0,), False))
    generator_verifier.verify_property(1, predicate=my_function_property((0, 1), False))
    generator_verifier.verify_property(2, predicate=my_function_property((0, 1, 2), False))
    generator_verifier.verify_property(3, predicate=my_function_property((0, 1, 2, 3), False))
    generator_verifier.verify_property(4, predicate=my_function_property((0, 1, 2, 3), True))
    generator_verifier.verify_property(5, predicate=my_function_property((0, 1, 2, 3), True))


def test_try_except_in_generator():
    def my_function(x: str):
        try:
            if x == 'ValueError':
                raise ValueError
            else:
                yield 'Try'
                raise KeyError

        except ValueError:
            yield 'Value1'
            yield 'Value2'

        except KeyError:
            yield 'Key'
        finally:
            yield 'End'

    verifier = verifier_for(my_function)

    verifier.verify_property('ValueError', predicate=assert_yield_values(('Value1', 'Value2', 'End')))
    verifier.verify_property('KeyError', predicate=assert_yield_values(('Try', 'Key', 'End')))


def test_yield_from_iterable_generator():
    def my_function(iterable1: Iterable, iterable2: Iterable):
        yield from iterable1
        yield 0
        yield from iterable2

        verifier = verifier_for(my_function)
        verifier.verify_property((1, 2), (3, 4, 5), predicate=assert_yield_values((1, 2, 0, 3, 4, 5)))
        verifier.verify_property((1, 2, 3), (4, 5), predicate=assert_yield_values((1, 2, 3, 0, 4, 5)))
        verifier.verify_property([], (4, 5), predicate=assert_yield_values((0, 4, 5)))
        verifier.verify_property((1, 2, 3), [], predicate=assert_yield_values((1, 2, 3, 0)))


def test_yield_from_generator_generator():
    def my_function(x: int):
        def inner_generator(y: int):
            for i in range(y):
                yield 2 * i

        yield from inner_generator(x)

        verifier = verifier_for(my_function)
        verifier.verify_property(1, predicate=assert_yield_values((2,)))
        verifier.verify_property(2, predicate=assert_yield_values((2, 4)))
        verifier.verify_property(3, predicate=assert_yield_values((2, 4, 6)))


def test_send_generator():
    def my_function(x: int):
        a = yield x
        yield a + x

    def send_predicate(x, sent):
        def predicate(generator):
            if next(generator) != x:
                return False

            if generator.send(sent) != x + sent:
                return False

            with pytest.raises(StopIteration):
                next(generator)

            return True

        return predicate

    verifier = verifier_for(my_function)
    verifier.verify_property(1, predicate=send_predicate(1, 1))
    verifier.verify_property(1, predicate=send_predicate(1, 2))

    verifier.verify_property(2, predicate=send_predicate(2, 1))
    verifier.verify_property(2, predicate=send_predicate(2, 2))

    verifier.verify_property(3, predicate=send_predicate(3, 1))
    verifier.verify_property(3, predicate=send_predicate(3, 2))


def test_throw_generator():
    def my_function():
        try:
            yield 'Try'
        except ValueError:
            yield 'Value'
        finally:
            yield 'Finally'

    def throw_predicate(thrown, expected, error: Type[BaseException] = StopIteration):
        def predicate(generator):
            if next(generator) != expected[0]:
                return False

            if generator.throw(thrown) != expected[1]:
                return False

            for item in expected[2:]:
                if next(generator) != item:
                    return False

            with pytest.raises(error):
                next(generator)

            return True

        return predicate

    verifier = verifier_for(my_function)
    verifier.verify_property(predicate=throw_predicate(ValueError(), ('Try', 'Value', 'Finally')))
    verifier.verify_property(predicate=throw_predicate(KeyError(), ('Try', 'Finally'), error=KeyError))


def test_send_inner_generator():
    def my_function(x: int):
        def inner_generator(y: int):
            a = yield y
            yield a + y

        yield from inner_generator(x)

    def send_predicate(x, sent):
        def predicate(generator):
            if next(generator) != x:
                return False

            if generator.send(sent) != x + sent:
                return False

            with pytest.raises(StopIteration):
                next(generator)

            return True

        return predicate

    verifier = verifier_for(my_function)
    verifier.verify_property(1, predicate=send_predicate(1, 1))
    verifier.verify_property(1, predicate=send_predicate(1, 2))

    verifier.verify_property(2, predicate=send_predicate(2, 1))
    verifier.verify_property(2, predicate=send_predicate(2, 2))

    verifier.verify_property(3, predicate=send_predicate(3, 1))
    verifier.verify_property(3, predicate=send_predicate(3, 2))


def test_throw_inner_generator():
    def my_function():
        def inner_generator():
            try:
                yield 'Try'
            except ValueError:
                yield 'Value'
            finally:
                yield 'Finally'

        try:
            yield from inner_generator()
        except KeyError:
            yield 'Key'

    def throw_predicate(thrown, expected, error: Type[BaseException] = StopIteration):
        def predicate(generator):
            if next(generator) != expected[0]:
                return False

            if generator.throw(thrown) != expected[1]:
                return False

            for item in expected[2:]:
                if next(generator) != item:
                    return False

            with pytest.raises(error):
                next(generator)

            return True

        return predicate

    verifier = verifier_for(my_function)
    verifier.verify_property(predicate=throw_predicate(ValueError(), ('Try', 'Value', 'Finally')))
    verifier.verify_property(predicate=throw_predicate(KeyError(), ('Try', 'Finally', 'Key')))
    verifier.verify_property(predicate=throw_predicate(AssertionError(), ('Try', 'Finally'), error=AssertionError))
