from .conftest import verifier_for


def test_while_loops():
    def my_function(x: int) -> int:
        total = 0
        while x > 0:
            total += x
            x = x - 1
        return total

    function_verifier = verifier_for(my_function)

    function_verifier.verify(0, expected_result=0)
    function_verifier.verify(1, expected_result=1)
    function_verifier.verify(2, expected_result=3)
    function_verifier.verify(3, expected_result=6)
    function_verifier.verify(4, expected_result=10)
    function_verifier.verify(5, expected_result=15)


def test_inner_loops():
    def my_function(x: int, y: int) -> int:
        total = 0
        while x > 0:
            remaining = y
            while remaining > 0:
                total += remaining * x
                remaining = remaining - 1
            x = x - 1
        return total

    function_verifier = verifier_for(my_function)

    function_verifier.verify(0, 0, expected_result=0)
    function_verifier.verify(1, 0, expected_result=0)
    function_verifier.verify(1, 1, expected_result=1)
    function_verifier.verify(1, 2, expected_result=3)
    function_verifier.verify(1, 3, expected_result=6)
    function_verifier.verify(2, 1, expected_result=3)
    function_verifier.verify(2, 2, expected_result=9)
    function_verifier.verify(2, 3, expected_result=18)


def test_iterable_loops():
    def my_function(x: list) -> int:
        total = 0
        for item in x:
            total += item
        return total

    function_verifier = verifier_for(my_function)

    function_verifier.verify([], expected_result=0)
    function_verifier.verify([1, 2, 3], expected_result=6)
    function_verifier.verify([10, 20, 30], expected_result=60)


def test_inner_iterable_loops():
    def my_function(x: list, y: list) -> int:
        total = 0
        for x_item in x:
            for y_item in y:
                total += x_item * y_item
        return total

    function_verifier = verifier_for(my_function)

    function_verifier.verify([], [], expected_result=0)
    function_verifier.verify([], [1, 2, 3], expected_result=0)
    function_verifier.verify([1, 2, 3], [], expected_result=0)

    function_verifier.verify([1, 2, 3], [1, 2], expected_result=18)
    function_verifier.verify([1, 2], [-1, 3], expected_result=6)


def test_breaks_in_iterable_loop():
    def my_function(x: list) -> int:
        total = 0
        for item in x:
            if item == 0:
                break
            total += item
        return total

    function_verifier = verifier_for(my_function)

    function_verifier.verify([], expected_result=0)
    function_verifier.verify([1, 0, 3], expected_result=1)
    function_verifier.verify([1, 2, 3], expected_result=6)
    function_verifier.verify([1, 2, 3, 0, 4], expected_result=6)


def test_continues_in_iterable_loop():
    def my_function(x: list) -> int:
        total = 0
        for item in x:
            if item == 5:
                continue
            total += item
        return total

    function_verifier = verifier_for(my_function)

    function_verifier.verify([], expected_result=0)
    function_verifier.verify([1, 5, 3], expected_result=4)
    function_verifier.verify([1, 2, 3], expected_result=6)
    function_verifier.verify([1, 2, 3, 5, 4], expected_result=10)


def test_iterating_generator():
    def my_generator(x: int):
        for i in range(x + 1):
            yield i

    def my_function(x: int) -> int:
        total = 0
        for item in my_generator(x):
            total += item
        return total

    function_verifier = verifier_for(my_function)

    function_verifier.verify(0, expected_result=0)
    function_verifier.verify(1, expected_result=1)
    function_verifier.verify(2, expected_result=3)
    function_verifier.verify(3, expected_result=6)


def test_try_except_in_loop():
    def my_function(x: int) -> int:
        total = 0
        for item in range(x + 1):
            try:
                if item % 2 == 0:
                    raise ValueError
                total += item
            except ValueError:
                total -= item
        return total

    function_verifier = verifier_for(my_function)

    function_verifier.verify(0, expected_result=0)
    function_verifier.verify(1, expected_result=1)
    function_verifier.verify(2, expected_result=-1)
    function_verifier.verify(3, expected_result=2)
    function_verifier.verify(4, expected_result=-2)
    function_verifier.verify(5, expected_result=3)
    function_verifier.verify(6, expected_result=-3)


def test_try_except_outside_loop():
    def my_function(x: list) -> int:
        total = 0
        try:
            for item in x:
                if item == 0:
                    raise ValueError
                total += item
        except ValueError:
            return 0
        return total

    function_verifier = verifier_for(my_function)

    function_verifier.verify([], expected_result=0)
    function_verifier.verify([1], expected_result=1)
    function_verifier.verify([1, 2], expected_result=3)
    function_verifier.verify([1, 0, 2], expected_result=0)
    function_verifier.verify([1, 2, 3, 0], expected_result=0)
