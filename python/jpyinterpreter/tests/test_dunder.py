from .conftest import verifier_for


def test_same_operand():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return self.value - other.value

    def function(a: A, b: A) -> int:
        return a - b

    verifier = verifier_for(function)
    verifier.verify(A(3), A(2), expected_result=1)


def test_only_left_defined():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return self.value - other.value

    class B:
        def __init__(self, value):
            self.value = value

    def function(a: A, b: B) -> int:
        return a - b

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_result=1)


def test_only_right_defined():
    class A:
        def __init__(self, value):
            self.value = value

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return other.value - self.value

    def function(a: A, b: B) -> int:
        return a - b

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_result=1)


def test_both_defined():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return self.value - other.value

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return self.value - other.value

    def function(a: A, b: B) -> int:
        return a - b

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_result=1)


def test_neither_defined():
    class A:
        def __init__(self, value):
            self.value = value

    class B:
        def __init__(self, value):
            self.value = value

    # We are testing raising TypeError here, so we should ignore the IDE warnings this function gives
    def function(a: A, b: B) -> object:  # noqa
        return a - b  # noqa

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_error=TypeError)


def test_left_return_not_implemented():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return NotImplemented

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return other.value - self.value

    def function(a: A, b: B) -> int:
        return a - b

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_result=1)


def test_both_return_not_implemented():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return NotImplemented

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return NotImplemented

    def function(a: A, b: B) -> object:
        return a - b

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_error=TypeError)


def test_inplace_same_operand():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return self.value - other.value

    def function(a: A, b: A) -> int:
        a -= b
        return a  # noqa

    verifier = verifier_for(function)
    verifier.verify(A(3), A(2), expected_result=1)


def test_inplace_defined():
    class A:
        def __init__(self, value):
            self.value = value

        def __isub__(self, other):
            return self.value - other.value

        def __sub__(self, other):
            return self.value + other.value

    class B:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return self.value + other.value

        def __rsub__(self, other):
            return self.value + other.value

    def function(a: A, b: B) -> int:
        a -= b
        return a  # noqa

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_result=1)


def test_inplace_only_left_defined():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return self.value - other.value

    class B:
        def __init__(self, value):
            self.value = value

    def function(a: A, b: B) -> int:
        a -= b
        return a  # noqa

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_result=1)


def test_inplace_only_right_defined():
    class A:
        def __init__(self, value):
            self.value = value

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return other.value - self.value

    def function(a: A, b: B) -> int:
        a -= b
        return a  # noqa

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_result=1)


def test_inplace_both_defined():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return self.value - other.value

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return self.value - other.value

    def function(a: A, b: B) -> int:
        a -= b
        return a  # noqa

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_result=1)


def test_inplace_neither_defined():
    class A:
        def __init__(self, value):
            self.value = value

    class B:
        def __init__(self, value):
            self.value = value

    # We are testing raising TypeError here, so we should ignore the IDE warnings this function gives
    def function(a: A, b: B) -> object:  # noqa
        a -= b  # noqa
        return a

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_error=TypeError)


def test_inplace_left_inplace_return_not_implemented():
    class A:
        def __init__(self, value):
            self.value = value

        def __isub__(self, other):
            return NotImplemented

        def __sub__(self, other):
            return self.value - other.value

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return other.value + self.value

    def function(a: A, b: B) -> int:
        a -= b
        return a  # noqa

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_result=1)


def test_inplace_left_return_not_implemented():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return NotImplemented

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return other.value - self.value

    def function(a: A, b: B) -> int:
        a -= b
        return a  # noqa

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_result=1)


def test_inplace_both_return_not_implemented():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return NotImplemented

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return NotImplemented

    def function(a: A, b: B) -> object:
        a -= b
        return a

    verifier = verifier_for(function)
    verifier.verify(A(3), B(2), expected_error=TypeError)


def test_inverted_comparisons():
    class A:
        def __init__(self, value):
            self.value = value

    class B:
        def __init__(self, value):
            self.value = value

        def __lt__(self, other):
            return self.value < other.value

        def __gt__(self, other):
            return self.value > other.value

        def __le__(self, other):
            return self.value <= other.value

        def __ge__(self, other):
            return self.value >= other.value

        def __eq__(self, other):
            return self.value == other.value

        def __ne__(self, other):
            return self.value != other.value

    def less_than(a: A, b: B) -> bool:
        return a < b

    verifier = verifier_for(less_than)
    verifier.verify(A(1), B(2), expected_result=True)
    verifier.verify(A(1), B(1), expected_result=False)
    verifier.verify(A(2), B(1), expected_result=False)

    def greater_than(a: A, b: B) -> bool:
        return a > b

    verifier = verifier_for(greater_than)
    verifier.verify(A(1), B(2), expected_result=False)
    verifier.verify(A(1), B(1), expected_result=False)
    verifier.verify(A(2), B(1), expected_result=True)

    def less_than_or_equal(a: A, b: B) -> bool:
        return a <= b

    verifier = verifier_for(less_than_or_equal)
    verifier.verify(A(1), B(2), expected_result=True)
    verifier.verify(A(1), B(1), expected_result=True)
    verifier.verify(A(2), B(1), expected_result=False)

    def greater_than_or_equal(a: A, b: B) -> bool:
        return a >= b

    verifier = verifier_for(greater_than_or_equal)
    verifier.verify(A(1), B(2), expected_result=False)
    verifier.verify(A(1), B(1), expected_result=True)
    verifier.verify(A(2), B(1), expected_result=True)

    def equal(a: A, b: B) -> bool:
        return a == b

    verifier = verifier_for(equal)
    verifier.verify(A(1), B(2), expected_result=False)
    verifier.verify(A(1), B(1), expected_result=True)
    verifier.verify(A(2), B(1), expected_result=False)

    def not_equal(a: A, b: B) -> bool:
        return a != b

    verifier = verifier_for(not_equal)
    verifier.verify(A(1), B(2), expected_result=True)
    verifier.verify(A(1), B(1), expected_result=False)
    verifier.verify(A(2), B(1), expected_result=True)
