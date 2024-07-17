from typing import Union
from .conftest import verifier_for

MAX_LONG = 0xFFFF_FFFF_FFFF_FFFF
MIN_LONG = -MAX_LONG


def test_add():
    def int_add(a: int, b: int) -> int:
        return a + b

    def float_add(a: int, b: float) -> float:
        return a + b

    int_add_verifier = verifier_for(int_add)
    float_add_verifier = verifier_for(float_add)

    int_add_verifier.verify(1, 1, expected_result=2)
    int_add_verifier.verify(1, -1, expected_result=0)
    int_add_verifier.verify(-1, 1, expected_result=0)
    int_add_verifier.verify(0, 1, expected_result=1)
    int_add_verifier.verify(MAX_LONG, 1, expected_result=(MAX_LONG + 1))
    int_add_verifier.verify(MIN_LONG, -1, expected_result=(MIN_LONG - 1))

    float_add_verifier.verify(1, 1.0, expected_result=2.0)
    float_add_verifier.verify(1, -1.0, expected_result=0.0)
    float_add_verifier.verify(-1, 1.0, expected_result=0.0)
    float_add_verifier.verify(0, 1.0, expected_result=1.0)
    float_add_verifier.verify(MAX_LONG, 1.0, expected_result=(MAX_LONG + 1.0))
    float_add_verifier.verify(MIN_LONG, -1.0, expected_result=(MIN_LONG - 1.0))


def test_iadd():
    def int_iadd(x: int, y: int) -> int:
        old = x
        x += y
        if y != 0:
            assert old is not x
        return x

    def float_iadd(x: int, y: float) -> float:
        old = x
        x += y
        if y != 0:
            assert old is not x
        return x

    int_iadd_verifier = verifier_for(int_iadd)
    float_iadd_verifier = verifier_for(float_iadd)

    int_iadd_verifier.verify(1, 1, expected_result=2)
    int_iadd_verifier.verify(1, -1, expected_result=0)
    int_iadd_verifier.verify(-1, 1, expected_result=0)
    int_iadd_verifier.verify(0, 1, expected_result=1)
    int_iadd_verifier.verify(MAX_LONG, 1, expected_result=(MAX_LONG + 1))
    int_iadd_verifier.verify(MIN_LONG, -1, expected_result=(MIN_LONG - 1))

    float_iadd_verifier.verify(1, 1.0, expected_result=2.0)
    float_iadd_verifier.verify(1, -1.0, expected_result=0.0)
    float_iadd_verifier.verify(-1, 1.0, expected_result=0.0)
    float_iadd_verifier.verify(0, 1.0, expected_result=1.0)
    float_iadd_verifier.verify(MAX_LONG, 1.0, expected_result=(MAX_LONG + 1.0))
    float_iadd_verifier.verify(MIN_LONG, -1.0, expected_result=(MIN_LONG - 1.0))


def test_sub():
    def int_sub(a: int, b: int) -> int:
        return a - b

    def float_sub(a: int, b: float) -> float:
        return a - b

    int_sub_verifier = verifier_for(int_sub)
    float_sub_verifier = verifier_for(float_sub)

    int_sub_verifier.verify(1, 1, expected_result=0)
    int_sub_verifier.verify(1, -1, expected_result=2)
    int_sub_verifier.verify(-1, 1, expected_result=-2)
    int_sub_verifier.verify(0, 1, expected_result=-1)
    int_sub_verifier.verify(MAX_LONG, -1, expected_result=(MAX_LONG + 1))
    int_sub_verifier.verify(MIN_LONG, 1, expected_result=(MIN_LONG - 1))

    float_sub_verifier.verify(1, 1.0, expected_result=0.0)
    float_sub_verifier.verify(1, -1.0, expected_result=2.0)
    float_sub_verifier.verify(-1, 1.0, expected_result=-2.0)
    float_sub_verifier.verify(0, 1.0, expected_result=-1.0)
    float_sub_verifier.verify(MAX_LONG, -1.0, expected_result=(MAX_LONG + 1.0))
    float_sub_verifier.verify(MIN_LONG, 1.0, expected_result=(MIN_LONG - 1.0))


def test_isub():
    def int_isub(x: int, y: int) -> int:
        old = x
        x -= y
        if y != 0:
            assert old is not x
        return x

    def float_isub(x: int, y: float) -> float:
        old = x
        x -= y
        if y != 0:
            assert old is not x
        return x

    int_isub_verifier = verifier_for(int_isub)
    float_isub_verifier = verifier_for(float_isub)

    int_isub_verifier.verify(1, 1, expected_result=0)
    int_isub_verifier.verify(1, -1, expected_result=2)
    int_isub_verifier.verify(-1, 1, expected_result=-2)
    int_isub_verifier.verify(0, 1, expected_result=-1)
    int_isub_verifier.verify(MAX_LONG, -1, expected_result=(MAX_LONG + 1))
    int_isub_verifier.verify(MIN_LONG, 1, expected_result=(MIN_LONG - 1))

    float_isub_verifier.verify(1, 1.0, expected_result=0.0)
    float_isub_verifier.verify(1, -1.0, expected_result=2.0)
    float_isub_verifier.verify(-1, 1.0, expected_result=-2.0)
    float_isub_verifier.verify(0, 1.0, expected_result=-1.0)
    float_isub_verifier.verify(MAX_LONG, -1.0, expected_result=(MAX_LONG + 1.0))
    float_isub_verifier.verify(MIN_LONG, 1.0, expected_result=(MIN_LONG - 1.0))


def test_multiply():
    def int_multiply(a: int, b: int) -> int:
        return a * b

    def float_multiply(a: int, b: float) -> float:
        return a * b

    int_multiply_verifier = verifier_for(int_multiply)
    float_multiply_verifier = verifier_for(float_multiply)

    int_multiply_verifier.verify(1, 1, expected_result=1)
    int_multiply_verifier.verify(1, -1, expected_result=-1)
    int_multiply_verifier.verify(-1, 1, expected_result=-1)
    int_multiply_verifier.verify(0, 1, expected_result=0)
    int_multiply_verifier.verify(2, 3, expected_result=6)
    int_multiply_verifier.verify(MAX_LONG, 2, expected_result=(2 * MAX_LONG))
    int_multiply_verifier.verify(MIN_LONG, 2, expected_result=(2 * MIN_LONG))

    float_multiply_verifier.verify(1, 1.0, expected_result=1.0)
    float_multiply_verifier.verify(1, -1.0, expected_result=-1.0)
    float_multiply_verifier.verify(-1, 1.0, expected_result=-1.0)
    float_multiply_verifier.verify(0, 1.0, expected_result=0.0)
    float_multiply_verifier.verify(2, 3.0, expected_result=6.0)
    float_multiply_verifier.verify(MAX_LONG, 2.0, expected_result=(2.0 * MAX_LONG))
    float_multiply_verifier.verify(MIN_LONG, 2.0, expected_result=(2.0 * MIN_LONG))


def test_imultiply():
    def int_imultiply(x: int, y: int) -> int:
        old = x
        x *= y
        if y != 1:
            assert old is not x
        return x

    def float_imultiply(x: int, y: float) -> float:
        old = x
        x *= y
        if y != 1:
            assert old is not x
        return x

    int_imultiply_verifier = verifier_for(int_imultiply)
    float_imultiply_verifier = verifier_for(float_imultiply)

    int_imultiply_verifier.verify(1, 1, expected_result=1)
    int_imultiply_verifier.verify(1, -1, expected_result=-1)
    int_imultiply_verifier.verify(-1, 1, expected_result=-1)
    int_imultiply_verifier.verify(0, 1, expected_result=0)
    int_imultiply_verifier.verify(2, 3, expected_result=6)
    int_imultiply_verifier.verify(MAX_LONG, 2, expected_result=(2 * MAX_LONG))
    int_imultiply_verifier.verify(MIN_LONG, 2, expected_result=(2 * MIN_LONG))

    float_imultiply_verifier.verify(1, 1.0, expected_result=1.0)
    float_imultiply_verifier.verify(1, -1.0, expected_result=-1.0)
    float_imultiply_verifier.verify(-1, 1.0, expected_result=-1.0)
    float_imultiply_verifier.verify(0, 1.0, expected_result=0.0)
    float_imultiply_verifier.verify(2, 3.0, expected_result=6.0)
    float_imultiply_verifier.verify(MAX_LONG, 2.0, expected_result=(2.0 * MAX_LONG))
    float_imultiply_verifier.verify(MIN_LONG, 2.0, expected_result=(2.0 * MIN_LONG))


def test_truediv():
    def int_truediv(a: int, b: int) -> float:
        return a / b

    def float_truediv(a: int, b: float) -> float:
        return a / b

    int_truediv_verifier = verifier_for(int_truediv)
    float_truediv_verifier = verifier_for(float_truediv)

    int_truediv_verifier.verify(1, 1, expected_result=1.0)
    int_truediv_verifier.verify(1, -1, expected_result=-1.0)
    int_truediv_verifier.verify(-1, 1, expected_result=-1.0)
    int_truediv_verifier.verify(0, 1, expected_result=0.0)
    int_truediv_verifier.verify(3, 2, expected_result=1.5)
    int_truediv_verifier.verify(2 * MAX_LONG, 2, expected_result=1.8446744073709552e+19)
    int_truediv_verifier.verify(2 * MIN_LONG, 2, expected_result=-1.8446744073709552e+19)
    int_truediv_verifier.verify(1, 0, expected_error=ZeroDivisionError)

    float_truediv_verifier.verify(1, 1.0, expected_result=1.0)
    float_truediv_verifier.verify(1, -1.0, expected_result=-1.0)
    float_truediv_verifier.verify(-1, 1.0, expected_result=-1.0)
    float_truediv_verifier.verify(0, 1.0, expected_result=0.0)
    float_truediv_verifier.verify(3, 2.0, expected_result=1.5)
    float_truediv_verifier.verify(2 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    float_truediv_verifier.verify(2 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)
    float_truediv_verifier.verify(1, 0.0, expected_error=ZeroDivisionError)


def test_itruediv():
    def int_itruediv(x: int, y: int) -> float:
        old = x
        x /= y
        if y != 1:
            assert old is not x
        return x

    def float_itruediv(x: int, y: float) -> float:
        old = x
        x /= y
        if y != 1:
            assert old is not x
        return x

    int_itruediv_verifier = verifier_for(int_itruediv)
    float_itruediv_verifier = verifier_for(float_itruediv)

    int_itruediv_verifier.verify(1, 1, expected_result=1.0)
    int_itruediv_verifier.verify(1, -1, expected_result=-1.0)
    int_itruediv_verifier.verify(-1, 1, expected_result=-1.0)
    int_itruediv_verifier.verify(0, 1, expected_result=0.0)
    int_itruediv_verifier.verify(3, 2, expected_result=1.5)
    int_itruediv_verifier.verify(2 * MAX_LONG, 2, expected_result=1.8446744073709552e+19)
    int_itruediv_verifier.verify(2 * MIN_LONG, 2, expected_result=-1.8446744073709552e+19)
    int_itruediv_verifier.verify(1, 0, expected_error=ZeroDivisionError)

    float_itruediv_verifier.verify(1, 1.0, expected_result=1.0)
    float_itruediv_verifier.verify(1, -1.0, expected_result=-1.0)
    float_itruediv_verifier.verify(-1, 1.0, expected_result=-1.0)
    float_itruediv_verifier.verify(0, 1.0, expected_result=0.0)
    float_itruediv_verifier.verify(3, 2.0, expected_result=1.5)
    float_itruediv_verifier.verify(2 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    float_itruediv_verifier.verify(2 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)
    float_itruediv_verifier.verify(1, 0.0, expected_error=ZeroDivisionError)


def test_floordiv():
    def int_floordiv(a: int, b: int) -> int:
        return a // b

    def float_floordiv(a: int, b: float) -> float:
        return a // b

    int_floordiv_verifier = verifier_for(int_floordiv)
    float_floordiv_verifier = verifier_for(float_floordiv)

    int_floordiv_verifier.verify(1, 1, expected_result=1)
    int_floordiv_verifier.verify(1, -1, expected_result=-1)
    int_floordiv_verifier.verify(-1, 1, expected_result=-1)
    int_floordiv_verifier.verify(0, 1, expected_result=0)
    int_floordiv_verifier.verify(3, 2, expected_result=1)
    int_floordiv_verifier.verify(2 * MAX_LONG, 2, expected_result=MAX_LONG)
    int_floordiv_verifier.verify(2 * MIN_LONG, 2, expected_result=MIN_LONG)
    int_floordiv_verifier.verify(1, 0, expected_error=ZeroDivisionError)

    float_floordiv_verifier.verify(1, 1.0, expected_result=1.0)
    float_floordiv_verifier.verify(1, -1.0, expected_result=-1.0)
    float_floordiv_verifier.verify(-1, 1.0, expected_result=-1.0)
    float_floordiv_verifier.verify(0, 1.0, expected_result=0.0)
    float_floordiv_verifier.verify(3, 2.0, expected_result=1.0)
    float_floordiv_verifier.verify(2 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    float_floordiv_verifier.verify(2 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)
    float_floordiv_verifier.verify(1, 0.0, expected_error=ZeroDivisionError)


def test_ifloordiv():
    def int_ifloordiv(x: int, y: int) -> int:
        old = x
        x //= y
        if y != 1:
            assert old is not x
        return x

    def float_ifloordiv(x: int, y: float) -> float:
        old = x
        x //= y
        if y != 1:
            assert old is not x
        return x

    int_ifloordiv_verifier = verifier_for(int_ifloordiv)
    float_ifloordiv_verifier = verifier_for(float_ifloordiv)

    int_ifloordiv_verifier.verify(1, 1, expected_result=1)
    int_ifloordiv_verifier.verify(1, -1, expected_result=-1)
    int_ifloordiv_verifier.verify(-1, 1, expected_result=-1)
    int_ifloordiv_verifier.verify(0, 1, expected_result=0)
    int_ifloordiv_verifier.verify(3, 2, expected_result=1)
    int_ifloordiv_verifier.verify(2 * MAX_LONG, 2, expected_result=MAX_LONG)
    int_ifloordiv_verifier.verify(2 * MIN_LONG, 2, expected_result=MIN_LONG)
    int_ifloordiv_verifier.verify(1, 0, expected_error=ZeroDivisionError)

    float_ifloordiv_verifier.verify(1, 1.0, expected_result=1.0)
    float_ifloordiv_verifier.verify(1, -1.0, expected_result=-1.0)
    float_ifloordiv_verifier.verify(-1, 1.0, expected_result=-1.0)
    float_ifloordiv_verifier.verify(0, 1.0, expected_result=0.0)
    float_ifloordiv_verifier.verify(3, 2.0, expected_result=1.0)
    float_ifloordiv_verifier.verify(2 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    float_ifloordiv_verifier.verify(2 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)
    float_ifloordiv_verifier.verify(1, 0.0, expected_error=ZeroDivisionError)


def test_mod():
    def int_mod(a: int, b: int) -> int:
        return a % b

    def float_mod(a: int, b: float) -> float:
        return a % b

    int_mod_verifier = verifier_for(int_mod)
    float_mod_verifier = verifier_for(float_mod)

    int_mod_verifier.verify(3, 4, expected_result=3)
    int_mod_verifier.verify(4, 3, expected_result=1)
    int_mod_verifier.verify(-4, 3, expected_result=2)
    int_mod_verifier.verify(4, -3, expected_result=-2)
    int_mod_verifier.verify(-4, -3, expected_result=-1)
    int_mod_verifier.verify(2 * MAX_LONG, 2, expected_result=0)
    int_mod_verifier.verify(1, 0, expected_error=ZeroDivisionError)

    float_mod_verifier.verify(3, 4.0, expected_result=3.0)
    float_mod_verifier.verify(4, 3.0, expected_result=1.0)
    float_mod_verifier.verify(-4, 3.0, expected_result=2.0)
    float_mod_verifier.verify(4, -3.0, expected_result=-2.0)
    float_mod_verifier.verify(-4, -3.0, expected_result=-1.0)
    float_mod_verifier.verify(2 * MAX_LONG, 2.0, expected_result=0.0)
    float_mod_verifier.verify(1, 0.0, expected_error=ZeroDivisionError)


def test_imod():
    def int_imod(x: int, y: int) -> int:
        old = x
        x %= y
        if old > y:
            assert old is not x
        return x

    def float_imod(x: int, y: float) -> float:
        old = x
        x %= y
        if old > y:
            assert old is not x
        return x

    int_imod_verifier = verifier_for(int_imod)
    float_imod_verifier = verifier_for(float_imod)

    int_imod_verifier.verify(3, 4, expected_result=3)
    int_imod_verifier.verify(4, 3, expected_result=1)
    int_imod_verifier.verify(-4, 3, expected_result=2)
    int_imod_verifier.verify(4, -3, expected_result=-2)
    int_imod_verifier.verify(-4, -3, expected_result=-1)
    int_imod_verifier.verify(2 * MAX_LONG, 2, expected_result=0)
    int_imod_verifier.verify(1, 0, expected_error=ZeroDivisionError)

    float_imod_verifier.verify(3, 4.0, expected_result=3.0)
    float_imod_verifier.verify(4, 3.0, expected_result=1.0)
    float_imod_verifier.verify(-4, 3.0, expected_result=2.0)
    float_imod_verifier.verify(4, -3.0, expected_result=-2.0)
    float_imod_verifier.verify(-4, -3.0, expected_result=-1.0)
    float_imod_verifier.verify(2 * MAX_LONG, 2.0, expected_result=0.0)
    float_imod_verifier.verify(1, 0.0, expected_error=ZeroDivisionError)


def test_negate():
    def negate(x: int) -> int:
        return -x

    negate_verifier = verifier_for(negate)

    negate_verifier.verify(1, expected_result=-1)
    negate_verifier.verify(-1, expected_result=1)
    negate_verifier.verify(MAX_LONG, expected_result=-MAX_LONG)
    negate_verifier.verify(MIN_LONG, expected_result=-MIN_LONG)


def test_pos():
    def pos(x: int) -> int:
        return +x

    pos_verifier = verifier_for(pos)

    pos_verifier.verify(1, expected_result=1)
    pos_verifier.verify(-1, expected_result=-1)
    pos_verifier.verify(MAX_LONG, expected_result=MAX_LONG)
    pos_verifier.verify(MIN_LONG, expected_result=MIN_LONG)


def test_abs():
    def int_abs(x: int) -> int:
        return abs(x)

    abs_verifier = verifier_for(int_abs)

    abs_verifier.verify(1, expected_result=1)
    abs_verifier.verify(-1, expected_result=1)
    abs_verifier.verify(MAX_LONG, expected_result=MAX_LONG)
    abs_verifier.verify(MIN_LONG, expected_result=-MIN_LONG)


def test_divmod():
    def int_divmod(a: int, b: int) -> tuple:
        return divmod(a, b)

    def float_divmod(a: int, b: float) -> tuple:
        return divmod(a, b)

    int_divmod_verifier = verifier_for(int_divmod)
    float_divmod_verifier = verifier_for(float_divmod)

    int_divmod_verifier.verify(1, 1, expected_result=(1, 0))
    int_divmod_verifier.verify(1, -1, expected_result=(-1, 0))
    int_divmod_verifier.verify(-1, 1, expected_result=(-1, 0))
    int_divmod_verifier.verify(0, 1, expected_result=(0, 0))
    int_divmod_verifier.verify(3, 2, expected_result=(1, 1))
    int_divmod_verifier.verify(-3, -2, expected_result=(1, -1))
    int_divmod_verifier.verify(2 * MAX_LONG, 2, expected_result=(MAX_LONG, 0))
    int_divmod_verifier.verify(2 * MIN_LONG, 2, expected_result=(MIN_LONG, 0))

    float_divmod_verifier.verify(1, 1.0, expected_result=(1, 0))
    float_divmod_verifier.verify(1, -1.0, expected_result=(-1, 0))
    float_divmod_verifier.verify(-1, 1.0, expected_result=(-1, 0))
    float_divmod_verifier.verify(0, 1.0, expected_result=(0, 0))
    float_divmod_verifier.verify(3, 2.0, expected_result=(1, 1))
    float_divmod_verifier.verify(-3, -2.0, expected_result=(1, -1))
    float_divmod_verifier.verify(2 * MAX_LONG, 2.0, expected_result=(1.8446744073709552e+19, 0))
    float_divmod_verifier.verify(2 * MIN_LONG, 2.0, expected_result=(-1.8446744073709552e+19, 0))


def test_pow():
    def int_pow(a: int, b: int) -> Union[int, float]:
        return a ** b

    def float_pow(a: int, b: float) -> float:
        return a ** b

    int_pow_verifier = verifier_for(int_pow)
    float_pow_verifier = verifier_for(float_pow)

    int_pow_verifier.verify(0, 0, expected_result=1)
    int_pow_verifier.verify(1, 2, expected_result=1)
    int_pow_verifier.verify(2, 2, expected_result=4)
    int_pow_verifier.verify(-2, 2, expected_result=4)
    int_pow_verifier.verify(-2, 3, expected_result=-8)
    int_pow_verifier.verify(3, 2, expected_result=9)
    int_pow_verifier.verify(2, 3, expected_result=8)
    int_pow_verifier.verify(2, -1, expected_result=0.5)
    int_pow_verifier.verify(2, -2, expected_result=0.25)

    float_pow_verifier.verify(0, 0.0, expected_result=1.0)
    float_pow_verifier.verify(1, 2.0, expected_result=1.0)
    float_pow_verifier.verify(2, 2.0, expected_result=4.0)
    float_pow_verifier.verify(-2, 2.0, expected_result=4.0)
    float_pow_verifier.verify(-2, 3.0, expected_result=-8.0)
    float_pow_verifier.verify(3, 2.0, expected_result=9.0)
    float_pow_verifier.verify(2, 3.0, expected_result=8.0)
    float_pow_verifier.verify(2, -1.0, expected_result=0.5)
    float_pow_verifier.verify(2, -2.0, expected_result=0.25)


def test_mod_pow():
    def mod_pow(x: int, y: int, z: int) -> int:
        return pow(x, y, z)

    mod_pow_verifier = verifier_for(mod_pow)

    mod_pow_verifier.verify(2, 3, 3, expected_result=2)
    mod_pow_verifier.verify(2, -1, 3, expected_result=2)
