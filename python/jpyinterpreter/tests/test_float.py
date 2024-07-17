from .conftest import verifier_for

MAX_LONG = 0xFFFF_FFFF_FFFF_FFFF
MIN_LONG = -MAX_LONG


def test_use_64_bits():
    def identity(x: float) -> float:
        return x

    identity_verifier = verifier_for(identity)

    identity_verifier.verify(2345678.2345678, expected_result=2345678.2345678)


def test_add():
    def int_add(a: float, b: int) -> float:
        return a + b

    def float_add(a: float, b: float) -> float:
        return a + b

    int_add_verifier = verifier_for(int_add)
    float_add_verifier = verifier_for(float_add)

    int_add_verifier.verify(1.0, 1, expected_result=2.0)
    int_add_verifier.verify(1.0, -1, expected_result=0.0)
    int_add_verifier.verify(-1.0, 1, expected_result=0.0)
    int_add_verifier.verify(0.0, 1, expected_result=1.0)
    int_add_verifier.verify(MAX_LONG + 0.0, 1, expected_result=(MAX_LONG + 1.0))
    int_add_verifier.verify(MIN_LONG + 0.0, -1, expected_result=(MIN_LONG - 1.0))

    float_add_verifier.verify(1.0, 1.0, expected_result=2.0)
    float_add_verifier.verify(1.0, -1.0, expected_result=0.0)
    float_add_verifier.verify(-1.0, 1.0, expected_result=0.0)
    float_add_verifier.verify(0.0, 1.0, expected_result=1.0)
    float_add_verifier.verify(MAX_LONG + 0.0, 1.0, expected_result=(MAX_LONG + 1.0))
    float_add_verifier.verify(MIN_LONG + 0.0, -1.0, expected_result=(MIN_LONG - 1.0))


def test_iadd():
    def int_iadd(x: float, y: int) -> float:
        old = x
        x += y
        if y != 0:
            assert old is not x
        return x

    def float_iadd(x: float, y: float) -> float:
        old = x
        x += y
        if y != 0:
            assert old is not x
        return x

    int_iadd_verifier = verifier_for(int_iadd)
    float_iadd_verifier = verifier_for(float_iadd)

    int_iadd_verifier.verify(1.0, 1, expected_result=2.0)
    int_iadd_verifier.verify(1.0, -1, expected_result=0.0)
    int_iadd_verifier.verify(-1.0, 1, expected_result=0.0)
    int_iadd_verifier.verify(0.0, 1, expected_result=1.0)
    int_iadd_verifier.verify(MAX_LONG + 0.0, 1, expected_result=(MAX_LONG + 1.0))
    int_iadd_verifier.verify(MIN_LONG + 0.0, -1, expected_result=(MIN_LONG - 1.0))

    float_iadd_verifier.verify(1.0, 1.0, expected_result=2.0)
    float_iadd_verifier.verify(1.0, -1.0, expected_result=0.0)
    float_iadd_verifier.verify(-1.0, 1.0, expected_result=0.0)
    float_iadd_verifier.verify(0.0, 1.0, expected_result=1.0)
    float_iadd_verifier.verify(MAX_LONG + 0.0, 1.0, expected_result=(MAX_LONG + 1.0))
    float_iadd_verifier.verify(MIN_LONG + 0.0, -1.0, expected_result=(MIN_LONG - 1.0))


def test_sub():
    def int_sub(a: float, b: int) -> float:
        return a - b

    def float_sub(a: float, b: float) -> float:
        return a - b

    int_sub_verifier = verifier_for(int_sub)
    float_sub_verifier = verifier_for(float_sub)

    int_sub_verifier.verify(1.0, 1, expected_result=0.0)
    int_sub_verifier.verify(1.0, -1, expected_result=2.0)
    int_sub_verifier.verify(-1.0, 1, expected_result=-2.0)
    int_sub_verifier.verify(0.0, 1, expected_result=-1.0)
    int_sub_verifier.verify(MAX_LONG + 0.0, -1, expected_result=(MAX_LONG + 1.0))
    int_sub_verifier.verify(MIN_LONG + 0.0, 1, expected_result=(MIN_LONG - 1.0))

    float_sub_verifier.verify(1.0, 1.0, expected_result=0.0)
    float_sub_verifier.verify(1.0, -1.0, expected_result=2.0)
    float_sub_verifier.verify(-1.0, 1.0, expected_result=-2.0)
    float_sub_verifier.verify(0.0, 1.0, expected_result=-1.0)
    float_sub_verifier.verify(MAX_LONG + 0.0, -1.0, expected_result=(MAX_LONG + 1.0))
    float_sub_verifier.verify(MIN_LONG + 0.0, 1.0, expected_result=(MIN_LONG - 1.0))


def test_isub():
    def int_isub(x: float, y: int) -> float:
        old = x
        x -= y
        if y != 0:
            assert old is not x
        return x

    def float_isub(x: float, y: float) -> float:
        old = x
        x -= y
        if y != 0:
            assert old is not x
        return x

    int_isub_verifier = verifier_for(int_isub)
    float_isub_verifier = verifier_for(float_isub)

    int_isub_verifier.verify(1.0, 1, expected_result=0.0)
    int_isub_verifier.verify(1.0, -1, expected_result=2.0)
    int_isub_verifier.verify(-1.0, 1, expected_result=-2.0)
    int_isub_verifier.verify(0.0, 1, expected_result=-1.0)
    int_isub_verifier.verify(MAX_LONG + 0.0, -1, expected_result=(MAX_LONG + 1.0))
    int_isub_verifier.verify(MIN_LONG + 0.0, 1, expected_result=(MIN_LONG - 1.0))

    float_isub_verifier.verify(1.0, 1.0, expected_result=0.0)
    float_isub_verifier.verify(1.0, -1.0, expected_result=2.0)
    float_isub_verifier.verify(-1.0, 1.0, expected_result=-2.0)
    float_isub_verifier.verify(0.0, 1.0, expected_result=-1.0)
    float_isub_verifier.verify(MAX_LONG + 0.0, -1.0, expected_result=(MAX_LONG + 1.0))
    float_isub_verifier.verify(MIN_LONG + 0.0, 1.0, expected_result=(MIN_LONG - 1.0))


def test_multiply():
    def int_multiply(a: float, b: int) -> float:
        return a * b

    def float_multiply(a: float, b: float) -> float:
        return a * b

    int_multiply_verifier = verifier_for(int_multiply)
    float_multiply_verifier = verifier_for(float_multiply)

    int_multiply_verifier.verify(1.0, 1, expected_result=1.0)
    int_multiply_verifier.verify(1.0, -1, expected_result=-1.0)
    int_multiply_verifier.verify(-1.0, 1, expected_result=-1.0)
    int_multiply_verifier.verify(0.0, 1, expected_result=0.0)
    int_multiply_verifier.verify(2.0, 3, expected_result=6.0)
    int_multiply_verifier.verify(MAX_LONG + 0.0, 2, expected_result=(2.0 * MAX_LONG))
    int_multiply_verifier.verify(MIN_LONG + 0.0, 2, expected_result=(2.0 * MIN_LONG))

    float_multiply_verifier.verify(1.0, 1.0, expected_result=1.0)
    float_multiply_verifier.verify(1.0, -1.0, expected_result=-1.0)
    float_multiply_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    float_multiply_verifier.verify(0.0, 1.0, expected_result=0.0)
    float_multiply_verifier.verify(2.0, 3.0, expected_result=6.0)
    float_multiply_verifier.verify(MAX_LONG + 0.0, 2.0, expected_result=(2.0 * MAX_LONG))
    float_multiply_verifier.verify(MIN_LONG + 0.0, 2.0, expected_result=(2.0 * MIN_LONG))


def test_imultiply():
    def int_imultiply(x: float, y: int) -> float:
        old = x
        x *= y
        if y != 1:
            assert old is not x
        return x

    def float_imultiply(x: float, y: float) -> float:
        old = x
        x *= y
        if y != 1:
            assert old is not x
        return x

    int_imultiply_verifier = verifier_for(int_imultiply)
    float_imultiply_verifier = verifier_for(float_imultiply)

    int_imultiply_verifier.verify(1.0, 1, expected_result=1.0)
    int_imultiply_verifier.verify(1.0, -1, expected_result=-1.0)
    int_imultiply_verifier.verify(-1.0, 1, expected_result=-1.0)
    int_imultiply_verifier.verify(0.0, 1, expected_result=0.0)
    int_imultiply_verifier.verify(2.0, 3, expected_result=6.0)
    int_imultiply_verifier.verify(MAX_LONG + 0.0, 2, expected_result=(2.0 * MAX_LONG))
    int_imultiply_verifier.verify(MIN_LONG + 0.0, 2, expected_result=(2.0 * MIN_LONG))

    float_imultiply_verifier.verify(1.0, 1.0, expected_result=1.0)
    float_imultiply_verifier.verify(1.0, -1.0, expected_result=-1.0)
    float_imultiply_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    float_imultiply_verifier.verify(0.0, 1.0, expected_result=0.0)
    float_imultiply_verifier.verify(2.0, 3.0, expected_result=6.0)
    float_imultiply_verifier.verify(MAX_LONG + 0.0, 2.0, expected_result=(2.0 * MAX_LONG))
    float_imultiply_verifier.verify(MIN_LONG + 0.0, 2.0, expected_result=(2.0 * MIN_LONG))


def test_truediv():
    def int_truediv(a: float, b: int) -> float:
        return a / b

    def float_truediv(a: float, b: float) -> float:
        return a / b

    int_truediv_verifier = verifier_for(int_truediv)
    float_truediv_verifier = verifier_for(float_truediv)

    int_truediv_verifier.verify(1.0, 1, expected_result=1.0)
    int_truediv_verifier.verify(1.0, -1, expected_result=-1.0)
    int_truediv_verifier.verify(-1.0, 1, expected_result=-1.0)
    int_truediv_verifier.verify(0.0, 1, expected_result=0.0)
    int_truediv_verifier.verify(3.0, 2, expected_result=1.5)
    int_truediv_verifier.verify(2.0 * MAX_LONG, 2, expected_result=1.8446744073709552e+19)
    int_truediv_verifier.verify(2.0 * MIN_LONG, 2, expected_result=-1.8446744073709552e+19)
    int_truediv_verifier.verify(1.0, 0, expected_error=ZeroDivisionError)

    float_truediv_verifier.verify(1.0, 1.0, expected_result=1.0)
    float_truediv_verifier.verify(1.0, -1.0, expected_result=-1.0)
    float_truediv_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    float_truediv_verifier.verify(0.0, 1.0, expected_result=0.0)
    float_truediv_verifier.verify(3.0, 2.0, expected_result=1.5)
    float_truediv_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    float_truediv_verifier.verify(2.0 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)
    float_truediv_verifier.verify(1.0, 0.0, expected_error=ZeroDivisionError)


def test_itruediv():
    def int_itruediv(x: float, y: int) -> float:
        old = x
        x /= y
        if y != 1:
            assert old is not x
        return x

    def float_itruediv(x: float, y: float) -> float:
        old = x
        x /= y
        if y != 1:
            assert old is not x
        return x

    int_itruediv_verifier = verifier_for(int_itruediv)
    float_itruediv_verifier = verifier_for(float_itruediv)

    int_itruediv_verifier.verify(1.0, 1, expected_result=1.0)
    int_itruediv_verifier.verify(1.0, -1, expected_result=-1.0)
    int_itruediv_verifier.verify(-1.0, 1, expected_result=-1.0)
    int_itruediv_verifier.verify(0.0, 1, expected_result=0.0)
    int_itruediv_verifier.verify(3.0, 2, expected_result=1.5)
    int_itruediv_verifier.verify(2.0 * MAX_LONG, 2, expected_result=1.8446744073709552e+19)
    int_itruediv_verifier.verify(2.0 * MIN_LONG, 2, expected_result=-1.8446744073709552e+19)
    int_itruediv_verifier.verify(1.0, 0, expected_error=ZeroDivisionError)

    float_itruediv_verifier.verify(1.0, 1.0, expected_result=1.0)
    float_itruediv_verifier.verify(1.0, -1.0, expected_result=-1.0)
    float_itruediv_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    float_itruediv_verifier.verify(0.0, 1.0, expected_result=0.0)
    float_itruediv_verifier.verify(3.0, 2.0, expected_result=1.5)
    float_itruediv_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    float_itruediv_verifier.verify(2.0 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)
    float_itruediv_verifier.verify(1.0, 0.0, expected_error=ZeroDivisionError)


def test_floordiv():
    def int_floordiv(a: float, b: int) -> float:
        return a // b

    def float_floordiv(a: float, b: float) -> float:
        return a // b

    int_floordiv_verifier = verifier_for(int_floordiv)
    float_floordiv_verifier = verifier_for(float_floordiv)

    int_floordiv_verifier.verify(1.0, 1, expected_result=1.0)
    int_floordiv_verifier.verify(1.0, -1, expected_result=-1.0)
    int_floordiv_verifier.verify(-1.0, 1, expected_result=-1.0)
    int_floordiv_verifier.verify(0.0, 1, expected_result=0.0)
    int_floordiv_verifier.verify(3.0, 2, expected_result=1.0)
    int_floordiv_verifier.verify(2.0 * MAX_LONG, 2, expected_result=(MAX_LONG + 0.0))
    int_floordiv_verifier.verify(2.0 * MIN_LONG, 2, expected_result=(MIN_LONG + 0.0))
    int_floordiv_verifier.verify(1.0, 0, expected_error=ZeroDivisionError)

    float_floordiv_verifier.verify(1.0, 1.0, expected_result=1.0)
    float_floordiv_verifier.verify(1.0, -1.0, expected_result=-1.0)
    float_floordiv_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    float_floordiv_verifier.verify(0.0, 1.0, expected_result=0.0)
    float_floordiv_verifier.verify(3.0, 2.0, expected_result=1.0)
    float_floordiv_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    float_floordiv_verifier.verify(2.0 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)
    float_floordiv_verifier.verify(1.0, 0.0, expected_error=ZeroDivisionError)


def test_ifloordiv():
    def int_ifloordiv(x: float, y: int) -> float:
        old = x
        x //= y
        if y != 1:
            assert old is not x
        return x

    def float_ifloordiv(x: float, y: float) -> float:
        old = x
        x //= y
        if y != 1:
            assert old is not x
        return x

    int_ifloordiv_verifier = verifier_for(int_ifloordiv)
    float_ifloordiv_verifier = verifier_for(float_ifloordiv)

    int_ifloordiv_verifier.verify(1.0, 1, expected_result=1.0)
    int_ifloordiv_verifier.verify(1.0, -1, expected_result=-1.0)
    int_ifloordiv_verifier.verify(-1.0, 1, expected_result=-1.0)
    int_ifloordiv_verifier.verify(0.0, 1, expected_result=0.0)
    int_ifloordiv_verifier.verify(3.0, 2, expected_result=1.0)
    int_ifloordiv_verifier.verify(2.0 * MAX_LONG, 2, expected_result=(MAX_LONG + 0.0))
    int_ifloordiv_verifier.verify(2.0 * MIN_LONG, 2, expected_result=(MIN_LONG + 0.0))
    int_ifloordiv_verifier.verify(1.0, 0, expected_error=ZeroDivisionError)

    float_ifloordiv_verifier.verify(1.0, 1.0, expected_result=1.0)
    float_ifloordiv_verifier.verify(1.0, -1.0, expected_result=-1.0)
    float_ifloordiv_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    float_ifloordiv_verifier.verify(0.0, 1.0, expected_result=0.0)
    float_ifloordiv_verifier.verify(3.0, 2.0, expected_result=1.0)
    float_ifloordiv_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    float_ifloordiv_verifier.verify(2.0 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)
    float_ifloordiv_verifier.verify(1.0, 0.0, expected_error=ZeroDivisionError)


def test_mod():
    def int_mod(a: float, b: int) -> float:
        return a % b

    def float_mod(a: float, b: float) -> float:
        return a % b

    int_mod_verifier = verifier_for(int_mod)
    float_mod_verifier = verifier_for(float_mod)

    int_mod_verifier.verify(3.0, 4, expected_result=3.0)
    int_mod_verifier.verify(4.0, 3, expected_result=1.0)
    int_mod_verifier.verify(-4.0, 3, expected_result=2.0)
    int_mod_verifier.verify(4.0, -3, expected_result=-2.0)
    int_mod_verifier.verify(-4.0, -3, expected_result=-1.0)
    int_mod_verifier.verify(2.0 * MAX_LONG, 2, expected_result=0.0)
    int_mod_verifier.verify(1.0, 0, expected_error=ZeroDivisionError)

    float_mod_verifier.verify(3.0, 4.0, expected_result=3.0)
    float_mod_verifier.verify(4.0, 3.0, expected_result=1.0)
    float_mod_verifier.verify(-4.0, 3.0, expected_result=2.0)
    float_mod_verifier.verify(4.0, -3.0, expected_result=-2.0)
    float_mod_verifier.verify(-4.0, -3.0, expected_result=-1.0)
    float_mod_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=0.0)
    float_mod_verifier.verify(1.0, 0.0, expected_error=ZeroDivisionError)


def test_imod():
    def int_imod(x: float, y: int) -> float:
        old = x
        x %= y
        if old > y:
            assert old is not x
        return x

    def float_imod(x: float, y: float) -> float:
        old = x
        x %= y
        if old > y:
            assert old is not x
        return x

    int_imod_verifier = verifier_for(int_imod)
    float_imod_verifier = verifier_for(float_imod)

    int_imod_verifier.verify(3.0, 4, expected_result=3.0)
    int_imod_verifier.verify(4.0, 3, expected_result=1.0)
    int_imod_verifier.verify(-4.0, 3, expected_result=2.0)
    int_imod_verifier.verify(4.0, -3, expected_result=-2.0)
    int_imod_verifier.verify(-4.0, -3, expected_result=-1.0)
    int_imod_verifier.verify(2.0 * MAX_LONG, 2, expected_result=0.0)
    int_imod_verifier.verify(1.0, 0, expected_error=ZeroDivisionError)

    float_imod_verifier.verify(3.0, 4.0, expected_result=3.0)
    float_imod_verifier.verify(4.0, 3.0, expected_result=1.0)
    float_imod_verifier.verify(-4.0, 3.0, expected_result=2.0)
    float_imod_verifier.verify(4.0, -3.0, expected_result=-2.0)
    float_imod_verifier.verify(-4.0, -3.0, expected_result=-1.0)
    float_imod_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=0.0)
    float_imod_verifier.verify(1.0, 0.0, expected_error=ZeroDivisionError)


def test_negate():
    def negate(x: float) -> float:
        return -x

    negate_verifier = verifier_for(negate)

    negate_verifier.verify(1.0, expected_result=-1.0)
    negate_verifier.verify(-1.0, expected_result=1.0)
    negate_verifier.verify(MAX_LONG + 0.0, expected_result=-(MAX_LONG + 0.0))
    negate_verifier.verify(MIN_LONG + 0.0, expected_result=-(MIN_LONG + 0.0))


def test_pos():
    def pos(x: float) -> float:
        return +x

    pos_verifier = verifier_for(pos)

    pos_verifier.verify(1.0, expected_result=1.0)
    pos_verifier.verify(-1.0, expected_result=-1.0)
    pos_verifier.verify(MAX_LONG + 0.0, expected_result=(MAX_LONG + 0.0))
    pos_verifier.verify(MIN_LONG + 0.0, expected_result=(MIN_LONG + 0.0))


def test_abs():
    def float_abs(x: float) -> float:
        return abs(x)

    abs_verifier = verifier_for(float_abs)

    abs_verifier.verify(1.0, expected_result=1.0)
    abs_verifier.verify(-1.0, expected_result=1.0)
    abs_verifier.verify(MAX_LONG + 0.0, expected_result=(MAX_LONG + 0.0))
    abs_verifier.verify(MIN_LONG + 0.0, expected_result=-(MIN_LONG + 0.0))


def test_divmod():
    def int_divmod(a: float, b: int) -> tuple:
        return divmod(a, b)

    def float_divmod(a: float, b: float) -> tuple:
        return divmod(a, b)

    int_divmod_verifier = verifier_for(int_divmod)
    float_divmod_verifier = verifier_for(float_divmod)

    int_divmod_verifier.verify(1.0, 1, expected_result=(1.0, 0.0))
    int_divmod_verifier.verify(1.0, -1, expected_result=(-1.0, 0.0))
    int_divmod_verifier.verify(-1.0, 1, expected_result=(-1.0, 0.0))
    int_divmod_verifier.verify(0.0, 1, expected_result=(0.0, 0.0))
    int_divmod_verifier.verify(3.0, 2, expected_result=(1.0, 1.0))
    int_divmod_verifier.verify(-3.0, -2, expected_result=(1.0, -1.0))
    int_divmod_verifier.verify(2.0 * MAX_LONG, 2, expected_result=(MAX_LONG + 0.0, 0.0))
    int_divmod_verifier.verify(2.0 * MIN_LONG, 2, expected_result=(MIN_LONG + 0.0, 0.0))

    float_divmod_verifier.verify(1.0, 1.0, expected_result=(1.0, 0.0))
    float_divmod_verifier.verify(1.0, -1.0, expected_result=(-1.0, 0.0))
    float_divmod_verifier.verify(-1.0, 1.0, expected_result=(-1.0, 0.0))
    float_divmod_verifier.verify(0.0, 1.0, expected_result=(0.0, 0.0))
    float_divmod_verifier.verify(3.0, 2.0, expected_result=(1.0, 1.0))
    float_divmod_verifier.verify(-3.0, -2.0, expected_result=(1.0, -1.0))
    float_divmod_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=(1.8446744073709552e+19, 0.0))
    float_divmod_verifier.verify(2.0 * MIN_LONG, 2.0, expected_result=(-1.8446744073709552e+19, 0.0))


def test_pow():
    def int_pow(a: float, b: int) -> float:
        return a ** b

    def float_pow(a: float, b: float) -> float:
        return a ** b

    int_pow_verifier = verifier_for(int_pow)
    float_pow_verifier = verifier_for(float_pow)

    int_pow_verifier.verify(0.0, 0, expected_result=1.0)
    int_pow_verifier.verify(1.0, 2, expected_result=1.0)
    int_pow_verifier.verify(2.0, 2, expected_result=4.0)
    int_pow_verifier.verify(-2.0, 2, expected_result=4.0)
    int_pow_verifier.verify(-2.0, 3, expected_result=-8.0)
    int_pow_verifier.verify(3.0, 2, expected_result=9.0)
    int_pow_verifier.verify(2.0, 3, expected_result=8.0)
    int_pow_verifier.verify(2.0, -1, expected_result=0.5)
    int_pow_verifier.verify(2.0, -2, expected_result=0.25)

    float_pow_verifier.verify(0.0, 0.0, expected_result=1.0)
    float_pow_verifier.verify(1.0, 2.0, expected_result=1.0)
    float_pow_verifier.verify(2.0, 2.0, expected_result=4.0)
    float_pow_verifier.verify(-2.0, 2.0, expected_result=4.0)
    float_pow_verifier.verify(-2.0, 3.0, expected_result=-8.0)
    float_pow_verifier.verify(3.0, 2.0, expected_result=9.0)
    float_pow_verifier.verify(2.0, 3.0, expected_result=8.0)
    float_pow_verifier.verify(2.0, -1.0, expected_result=0.5)
    float_pow_verifier.verify(2.0, -2.0, expected_result=0.25)
