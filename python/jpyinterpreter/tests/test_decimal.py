from .conftest import verifier_for
from decimal import Decimal
from typing import Callable


def around(a: Decimal) -> Callable[[Decimal], bool]:
    def predicate(b: Decimal) -> bool:
        return abs(a - b) < 0.00001
    return predicate


def test_add():
    def decimal_add(a: Decimal, b: Decimal) -> Decimal:
        return a + b

    def int_add(a: Decimal, b: int) -> Decimal:
        return a + b

    decimal_add_verifier = verifier_for(decimal_add)
    int_add_verifier = verifier_for(int_add)

    decimal_add_verifier.verify(Decimal(1), Decimal(1), expected_result=Decimal(2))
    decimal_add_verifier.verify(Decimal(1), Decimal(-1), expected_result=Decimal(0))
    decimal_add_verifier.verify(Decimal(-1), Decimal(1), expected_result=Decimal(0))
    decimal_add_verifier.verify(Decimal(0), Decimal(1), expected_result=Decimal(1))
    decimal_add_verifier.verify(Decimal('1.5'), Decimal('1.5'), expected_result=Decimal('3.0'))

    int_add_verifier.verify(Decimal(1), 1, expected_result=Decimal(2))
    int_add_verifier.verify(Decimal(1), -1, expected_result=Decimal(0))
    int_add_verifier.verify(Decimal(-1), 1, expected_result=Decimal(0))
    int_add_verifier.verify(Decimal(0), 1, expected_result=Decimal(1))
    int_add_verifier.verify(Decimal('1.5'), 1, expected_result=Decimal('2.5'))


def test_sub():
    def decimal_sub(a: Decimal, b: Decimal) -> Decimal:
        return a - b

    def int_sub(a: Decimal, b: int) -> Decimal:
        return a - b

    decimal_sub_verifier = verifier_for(decimal_sub)
    int_sub_verifier = verifier_for(int_sub)

    decimal_sub_verifier.verify(Decimal(1), Decimal(1), expected_result=Decimal(0))
    decimal_sub_verifier.verify(Decimal(1), Decimal(-1), expected_result=Decimal(2))
    decimal_sub_verifier.verify(Decimal(-1), Decimal(1), expected_result=Decimal(-2))
    decimal_sub_verifier.verify(Decimal(0), Decimal(1), expected_result=Decimal(-1))
    decimal_sub_verifier.verify(Decimal('1.5'), Decimal('1.5'), expected_result=Decimal(0))

    int_sub_verifier.verify(Decimal(1), 1, expected_result=Decimal(0))
    int_sub_verifier.verify(Decimal(1), -1, expected_result=Decimal(2))
    int_sub_verifier.verify(Decimal(-1), 1, expected_result=Decimal(-2))
    int_sub_verifier.verify(Decimal(0), 1, expected_result=Decimal(-1))
    int_sub_verifier.verify(Decimal('1.5'), 1, expected_result=Decimal('0.5'))


def test_multiply():
    def decimal_multiply(a: Decimal, b: Decimal) -> Decimal:
        return a * b

    def int_multiply(a: Decimal, b: int) -> Decimal:
        return a * b

    decimal_multiply_verifier = verifier_for(decimal_multiply)
    int_multiply_verifier = verifier_for(int_multiply)

    decimal_multiply_verifier.verify(Decimal(1), Decimal(1), expected_result=Decimal(1))
    decimal_multiply_verifier.verify(Decimal(1), Decimal(-1), expected_result=Decimal(-1))
    decimal_multiply_verifier.verify(Decimal(-1), Decimal(1), expected_result=Decimal(-1))
    decimal_multiply_verifier.verify(Decimal(0), Decimal(1), expected_result=Decimal(0))
    decimal_multiply_verifier.verify(Decimal('1.5'), Decimal('1.5'), expected_result=Decimal('2.25'))

    int_multiply_verifier.verify(Decimal(1), 1, expected_result=Decimal(1))
    int_multiply_verifier.verify(Decimal(1), -1, expected_result=Decimal(-1))
    int_multiply_verifier.verify(Decimal(-1), 1, expected_result=Decimal(-1))
    int_multiply_verifier.verify(Decimal(0), 1, expected_result=Decimal(0))
    int_multiply_verifier.verify(Decimal('1.5'), 2, expected_result=Decimal('3.0'))


def test_truediv():
    def decimal_truediv(a: Decimal, b: Decimal) -> Decimal:
        return a / b

    def int_truediv(a: Decimal, b: int) -> Decimal:
        return a / b

    decimal_truediv_verifier = verifier_for(decimal_truediv)
    int_truediv_verifier = verifier_for(int_truediv)

    decimal_truediv_verifier.verify(Decimal(1), Decimal(1), expected_result=Decimal(1))
    decimal_truediv_verifier.verify(Decimal(1), Decimal(-1), expected_result=Decimal(-1))
    decimal_truediv_verifier.verify(Decimal(-1), Decimal(1), expected_result=Decimal(-1))
    decimal_truediv_verifier.verify(Decimal(0), Decimal(1), expected_result=Decimal(0))
    decimal_truediv_verifier.verify(Decimal(3), Decimal(2), expected_result=Decimal('1.5'))

    int_truediv_verifier.verify(Decimal(1), 1, expected_result=Decimal(1))
    int_truediv_verifier.verify(Decimal(1), -1, expected_result=Decimal(-1))
    int_truediv_verifier.verify(Decimal(-1), 1, expected_result=Decimal(-1))
    int_truediv_verifier.verify(Decimal(0), 1, expected_result=Decimal(0))
    int_truediv_verifier.verify(Decimal(3), 2, expected_result=Decimal('1.5'))


def test_floordiv():
    def decimal_floordiv(a: Decimal, b: Decimal) -> Decimal:
        return a // b

    def int_floordiv(a: Decimal, b: int) -> Decimal:
        return a // b

    decimal_floordiv_verifier = verifier_for(decimal_floordiv)
    int_floordiv_verifier = verifier_for(int_floordiv)

    decimal_floordiv_verifier.verify(Decimal(1), Decimal(1), expected_result=Decimal(1))
    decimal_floordiv_verifier.verify(Decimal(1), Decimal(-1), expected_result=Decimal(-1))
    decimal_floordiv_verifier.verify(Decimal(-1), Decimal(1), expected_result=Decimal(-1))
    decimal_floordiv_verifier.verify(Decimal(0), Decimal(1), expected_result=Decimal(0))
    decimal_floordiv_verifier.verify(Decimal(-7), Decimal(4), expected_result=Decimal('-1'))

    int_floordiv_verifier.verify(Decimal(1), 1, expected_result=Decimal(1))
    int_floordiv_verifier.verify(Decimal(1), -1, expected_result=Decimal(-1))
    int_floordiv_verifier.verify(Decimal(-1), 1, expected_result=Decimal(-1))
    int_floordiv_verifier.verify(Decimal(0), 1, expected_result=Decimal(0))
    int_floordiv_verifier.verify(Decimal(3), 2, expected_result=Decimal(1))


def test_mod():
    def decimal_mod(a: Decimal, b: Decimal) -> Decimal:
        return a % b

    def int_mod(a: Decimal, b: int) -> Decimal:
        return a % b

    decimal_mod_verifier = verifier_for(decimal_mod)
    int_mod_verifier = verifier_for(int_mod)

    decimal_mod_verifier.verify(Decimal(-7), Decimal(4), expected_result=Decimal(-3))
    decimal_mod_verifier.verify(Decimal(0), Decimal(1), expected_result=Decimal(0))
    decimal_mod_verifier.verify(Decimal(3), Decimal(2), expected_result=Decimal('1'))
    decimal_mod_verifier.verify(Decimal('3.5'), Decimal(2), expected_result=Decimal('1.5'))

    int_mod_verifier.verify(Decimal(1), 1, expected_result=Decimal(0))
    int_mod_verifier.verify(Decimal('3.5'), 2, expected_result=Decimal('1.5'))
    int_mod_verifier.verify(Decimal(3), 2, expected_result=Decimal(1))


def test_negate():
    def negate(x: Decimal) -> Decimal:
        return -x

    negate_verifier = verifier_for(negate)

    negate_verifier.verify(Decimal(1), expected_result=Decimal(-1))
    negate_verifier.verify(Decimal(-1), expected_result=Decimal(1))


def test_pos():
    def pos(x: Decimal) -> Decimal:
        return +x

    pos_verifier = verifier_for(pos)

    pos_verifier.verify(Decimal(1), expected_result=Decimal(1))
    pos_verifier.verify(Decimal(-1), expected_result=Decimal(-1))


def test_abs():
    def decimal_abs(x: Decimal) -> Decimal:
        return abs(x)

    abs_verifier = verifier_for(decimal_abs)

    abs_verifier.verify(Decimal(1), expected_result=Decimal(1))
    abs_verifier.verify(Decimal(-1), expected_result=Decimal(1))


def test_pow():
    def decimal_pow(a: Decimal, b: Decimal) -> Decimal:
        return a ** b

    def int_pow(a: Decimal, b: int) -> Decimal:
        return a ** b

    decimal_pow_verifier = verifier_for(decimal_pow)
    int_pow_verifier = verifier_for(int_pow)

    decimal_pow_verifier.verify(Decimal(1), Decimal(2), expected_result=Decimal(1))
    decimal_pow_verifier.verify(Decimal(2), Decimal(2), expected_result=Decimal(4))
    decimal_pow_verifier.verify(Decimal(3), Decimal(2), expected_result=Decimal(9))
    decimal_pow_verifier.verify(Decimal(2), Decimal(3), expected_result=Decimal(8))
    decimal_pow_verifier.verify(Decimal(2), Decimal(-1), expected_result=Decimal(0.5))
    decimal_pow_verifier.verify(Decimal(4), Decimal('0.5'), expected_result=Decimal(2))

    int_pow_verifier.verify(Decimal(1), 2, expected_result=Decimal(1))
    int_pow_verifier.verify(Decimal(2), 2, expected_result=Decimal(4))
    int_pow_verifier.verify(Decimal(3), 2, expected_result=Decimal(9))
    int_pow_verifier.verify(Decimal(2), 3, expected_result=Decimal(8))
    int_pow_verifier.verify(Decimal(2), -1, expected_result=Decimal(0.5))


def test_comparisons():
    def lt(a: Decimal, b: Decimal) -> bool:
        return a < b

    def gt(a: Decimal, b: Decimal) -> bool:
        return a > b

    def le(a: Decimal, b: Decimal) -> bool:
        return a <= b

    def ge(a: Decimal, b: Decimal) -> bool:
        return a >= b

    def eq(a: Decimal, b: Decimal) -> bool:
        return a == b

    def ne(a: Decimal, b: Decimal) -> bool:
        return a != b

    lt_verifier = verifier_for(lt)
    gt_verifier = verifier_for(gt)
    le_verifier = verifier_for(le)
    ge_verifier = verifier_for(ge)
    eq_verifier = verifier_for(eq)
    ne_verifier = verifier_for(ne)

    lt_verifier.verify(Decimal(1), Decimal(1), expected_result=False)
    gt_verifier.verify(Decimal(1), Decimal(1), expected_result=False)
    le_verifier.verify(Decimal(1), Decimal(1), expected_result=True)
    ge_verifier.verify(Decimal(1), Decimal(1), expected_result=True)
    eq_verifier.verify(Decimal(1), Decimal(1), expected_result=True)
    ne_verifier.verify(Decimal(1), Decimal(1), expected_result=False)

    lt_verifier.verify(Decimal(1), Decimal('1.0'), expected_result=False)
    gt_verifier.verify(Decimal(1), Decimal('1.0'), expected_result=False)
    le_verifier.verify(Decimal(1), Decimal('1.0'), expected_result=True)
    ge_verifier.verify(Decimal(1), Decimal('1.0'), expected_result=True)
    eq_verifier.verify(Decimal(1), Decimal('1.0'), expected_result=True)
    ne_verifier.verify(Decimal(1), Decimal('1.0'), expected_result=False)

    lt_verifier.verify(Decimal(1), Decimal(2), expected_result=True)
    gt_verifier.verify(Decimal(1), Decimal(2), expected_result=False)
    le_verifier.verify(Decimal(1), Decimal(2), expected_result=True)
    ge_verifier.verify(Decimal(1), Decimal(2), expected_result=False)
    eq_verifier.verify(Decimal(1), Decimal(2), expected_result=False)
    ne_verifier.verify(Decimal(1), Decimal(2), expected_result=True)

    lt_verifier.verify(Decimal(2), Decimal(1), expected_result=False)
    gt_verifier.verify(Decimal(2), Decimal(1), expected_result=True)
    le_verifier.verify(Decimal(2), Decimal(1), expected_result=False)
    ge_verifier.verify(Decimal(2), Decimal(1), expected_result=True)
    eq_verifier.verify(Decimal(2), Decimal(1), expected_result=False)
    ne_verifier.verify(Decimal(2), Decimal(1), expected_result=True)


def test_hash():
    def decimal_hash(a: Decimal) -> int:
        return hash(a)

    hash_verifier = verifier_for(decimal_hash)
    hash_verifier.verify(Decimal(1), expected_result=hash(Decimal(1)))
    hash_verifier.verify(Decimal('1.5'), expected_result=hash(Decimal('1.5')))


def test_round():
    def decimal_round(a: Decimal) -> int:
        return round(a)

    def decimal_round_with_digits(a: Decimal, digits: int) -> Decimal:
        return round(a, digits)

    decimal_round_verifier = verifier_for(decimal_round)
    decimal_round_with_digits_verifier = verifier_for(decimal_round_with_digits)

    decimal_round_verifier.verify(Decimal('1.2'), expected_result=1)
    decimal_round_verifier.verify(Decimal('1.5'), expected_result=2)
    decimal_round_verifier.verify(Decimal('1.7'), expected_result=2)
    decimal_round_verifier.verify(Decimal('2.5'), expected_result=2)

    decimal_round_with_digits_verifier.verify(Decimal('13.22'), 1, expected_result=Decimal('13.2'))
    decimal_round_with_digits_verifier.verify(Decimal('13.22'), 2, expected_result=Decimal('13.22'))
    decimal_round_with_digits_verifier.verify(Decimal('13.27'), 1, expected_result=Decimal('13.3'))
    decimal_round_with_digits_verifier.verify(Decimal('13.25'), 1, expected_result=Decimal('13.2'))


def test_adjusted():
    def adjusted(a: Decimal) -> int:
        return a.adjusted()

    adjusted_verifier = verifier_for(adjusted)
    adjusted_verifier.verify(Decimal(100), expected_result=2)
    adjusted_verifier.verify(Decimal('0.001'), expected_result=-3)


def test_as_integer_ratio():
    def as_integer_ratio(a: Decimal) -> tuple[int, int]:
        return a.as_integer_ratio()

    adjusted_verifier = verifier_for(as_integer_ratio)
    adjusted_verifier.verify(Decimal(100), expected_result=(100, 1))
    adjusted_verifier.verify(Decimal('-3.14'), expected_result=(-157, 50))


# TODO: Make as_tuple use NamedTuple
def test_as_tuple():
    def as_tuple(a: Decimal) -> tuple[int, tuple[int,...], int]:
        return a.as_tuple()

    def matches_tuple(t: tuple[int, tuple[int,...], int]) -> Callable[[tuple[int, tuple[int,...], int]], bool]:
        def predicate(tested: tuple[int, tuple[int,...], int]) -> bool:
            return t == tested

        return predicate

    as_tuple_verifier = verifier_for(as_tuple)
    as_tuple_verifier.verify_property(Decimal(100), predicate=matches_tuple((0, (1, 0, 0), 0)))
    as_tuple_verifier.verify_property(Decimal(-100), predicate=matches_tuple((1, (1, 0, 0), 0)))
    as_tuple_verifier.verify_property(Decimal('123.45'), predicate=matches_tuple((0, (1, 2, 3, 4, 5), -2)))


def test_canonical():
    def canonical(a: Decimal) -> Decimal:
        return a.canonical()

    canonical_verifier = verifier_for(canonical)
    canonical_verifier.verify(Decimal(100), expected_result=Decimal(100))


def test_compare():
    def compare(a: Decimal, b: Decimal) -> Decimal:
        return a.compare(b)

    compare_verifier = verifier_for(compare)
    compare_verifier.verify(Decimal(-5), Decimal(5), expected_result=Decimal(-1))
    compare_verifier.verify(Decimal(5), Decimal(-5), expected_result=Decimal(1))
    compare_verifier.verify(Decimal(5), Decimal(5), expected_result=Decimal(0))


def test_compare_signal():
    def compare_signal(a: Decimal, b: Decimal) -> Decimal:
        return a.compare_signal(b)

    compare_signal_verifier = verifier_for(compare_signal)
    compare_signal_verifier.verify(Decimal(-5), Decimal(5), expected_result=Decimal(-1))
    compare_signal_verifier.verify(Decimal(5), Decimal(-5), expected_result=Decimal(1))
    compare_signal_verifier.verify(Decimal(5), Decimal(5), expected_result=Decimal(0))


def test_compare_total():
    def compare_total(a: Decimal, b: Decimal) -> Decimal:
        return a.compare_total(b)

    compare_total_verifier = verifier_for(compare_total)
    compare_total_verifier.verify(Decimal(-5), Decimal(5), expected_result=Decimal(-1))
    compare_total_verifier.verify(Decimal(5), Decimal(-5), expected_result=Decimal(1))
    compare_total_verifier.verify(Decimal(5), Decimal(5), expected_result=Decimal(0))
    compare_total_verifier.verify(Decimal('12.0'), Decimal('12'), expected_result=Decimal(-1))
    compare_total_verifier.verify(Decimal('12'), Decimal('12.0'), expected_result=Decimal(1))


def test_compare_total_mag():
    def compare_total_mag(a: Decimal, b: Decimal) -> Decimal:
        return a.compare_total_mag(b)

    compare_total_mag_verifier = verifier_for(compare_total_mag)
    compare_total_mag_verifier.verify(Decimal(3), Decimal(5), expected_result=Decimal(-1))
    compare_total_mag_verifier.verify(Decimal(-7), Decimal(5), expected_result=Decimal(1))
    compare_total_mag_verifier.verify(Decimal(-5), Decimal(5), expected_result=Decimal(0))
    compare_total_mag_verifier.verify(Decimal(5), Decimal(-5), expected_result=Decimal(0))
    compare_total_mag_verifier.verify(Decimal(5), Decimal(5), expected_result=Decimal(0))
    compare_total_mag_verifier.verify(Decimal('12.0'), Decimal('12'), expected_result=Decimal(-1))
    compare_total_mag_verifier.verify(Decimal('12'), Decimal('12.0'), expected_result=Decimal(1))
    compare_total_mag_verifier.verify(Decimal('12.0'), Decimal('-12'), expected_result=Decimal(-1))
    compare_total_mag_verifier.verify(Decimal('-12'), Decimal('12.0'), expected_result=Decimal(1))


def test_conjugate():
    def conjugate(a: Decimal) -> Decimal:
        return a.conjugate()

    conjugate_verifier = verifier_for(conjugate)
    conjugate_verifier.verify(Decimal(10), expected_result=Decimal(10))


def test_copy_abs():
    def copy_abs(a: Decimal) -> Decimal:
        return a.copy_abs()

    copy_abs_verifier = verifier_for(copy_abs)
    copy_abs_verifier.verify(Decimal(10), expected_result=Decimal(10))
    copy_abs_verifier.verify(Decimal(-10), expected_result=Decimal(10))


def test_copy_negate():
    def copy_negate(a: Decimal) -> Decimal:
        return a.copy_negate()

    copy_negate_verifier = verifier_for(copy_negate)
    copy_negate_verifier.verify(Decimal(10), expected_result=Decimal(-10))
    copy_negate_verifier.verify(Decimal(-10), expected_result=Decimal(10))


def test_copy_sign():
    def copy_sign(a: Decimal, b: Decimal) -> Decimal:
        return a.copy_sign(b)

    copy_sign_verifier = verifier_for(copy_sign)
    copy_sign_verifier.verify(Decimal(1), Decimal(2), expected_result=Decimal(1))
    copy_sign_verifier.verify(Decimal('2.3'), Decimal('-1.5'), expected_result=Decimal('-2.3'))
    copy_sign_verifier.verify(Decimal('-1.5'), Decimal('2.3'), expected_result=Decimal('1.5'))


def test_exp():
    def exp(a: Decimal) -> Decimal:
        return a.exp()

    exp_verifier = verifier_for(exp)
    exp_verifier.verify(Decimal(1), expected_result=Decimal('2.718281828459045235360287471'))
    exp_verifier.verify(Decimal(321), expected_result=Decimal('2.561702493119680037517373933E+139'))


def test_fma():
    def decimal_decimal_fma(a: Decimal, b: Decimal, c: Decimal) -> Decimal:
        return a.fma(b, c)

    def int_decimal_fma(a: Decimal, b: int, c: Decimal) -> Decimal:
        return a.fma(b, c)

    def decimal_int_fma(a: Decimal, b: Decimal, c: int) -> Decimal:
        return a.fma(b, c)

    def int_int_fma(a: Decimal, b: int, c: int) -> Decimal:
        return a.fma(b, c)

    fma_decimal_decimal_verifier = verifier_for(decimal_decimal_fma)
    fma_int_decimal_verifier = verifier_for(int_decimal_fma)
    fma_decimal_int_decimal_verifier = verifier_for(decimal_int_fma)
    fma_int_int_decimal_verifier = verifier_for(int_int_fma)

    fma_decimal_decimal_verifier.verify(Decimal(2), Decimal(3), Decimal(5), expected_result=Decimal(11))
    fma_int_decimal_verifier.verify(Decimal(2), 3, Decimal(5), expected_result=Decimal(11))
    fma_decimal_int_decimal_verifier.verify(Decimal(2), Decimal(3), 5, expected_result=Decimal(11))
    fma_int_int_decimal_verifier.verify(Decimal(2), 3, 5, expected_result=Decimal(11))


def test_is_canonical():
    def is_canonical(a: Decimal) -> bool:
        return a.is_canonical()

    is_canonical_verifier = verifier_for(is_canonical)
    is_canonical_verifier.verify(Decimal(10), expected_result=True)


def test_is_finite():
    def is_finite(a: Decimal) -> bool:
        return a.is_finite()

    is_finite_verifier = verifier_for(is_finite)
    is_finite_verifier.verify(Decimal(10), expected_result=True)


def test_is_infinite():
    def is_infinite(a: Decimal) -> bool:
        return a.is_infinite()

    is_infinite_verifier = verifier_for(is_infinite)
    is_infinite_verifier.verify(Decimal(10), expected_result=False)


def test_is_nan():
    def is_nan(a: Decimal) -> bool:
        return a.is_nan()

    is_nan_verifier = verifier_for(is_nan)
    is_nan_verifier.verify(Decimal(10), expected_result=False)


def test_is_normal():
    def is_normal(a: Decimal) -> bool:
        return a.is_normal()

    is_normal_verifier = verifier_for(is_normal)
    is_normal_verifier.verify(Decimal(10), expected_result=True)


def test_is_qnan():
    def is_qnan(a: Decimal) -> bool:
        return a.is_qnan()

    is_qnan_verifier = verifier_for(is_qnan)
    is_qnan_verifier.verify(Decimal(10), expected_result=False)


def test_is_signed():
    def is_signed(a: Decimal) -> bool:
        return a.is_signed()

    is_signed_verifier = verifier_for(is_signed)
    is_signed_verifier.verify(Decimal(10), expected_result=False)
    is_signed_verifier.verify(Decimal(0), expected_result=False)
    is_signed_verifier.verify(Decimal(-10), expected_result=True)


def test_is_snan():
    def is_snan(a: Decimal) -> bool:
        return a.is_snan()

    is_snan_verifier = verifier_for(is_snan)
    is_snan_verifier.verify(Decimal(10), expected_result=False)


def test_is_subnormal():
    def is_subnormal(a: Decimal) -> bool:
        return a.is_subnormal()

    is_subnormal_verifier = verifier_for(is_subnormal)
    is_subnormal_verifier.verify(Decimal(10), expected_result=False)


def test_is_zero():
    def is_zero(a: Decimal) -> bool:
        return a.is_zero()

    is_zero_verifier = verifier_for(is_zero)
    is_zero_verifier.verify(Decimal(10), expected_result=False)
    is_zero_verifier.verify(Decimal(0), expected_result=True)


def test_ln():
    def ln(a: Decimal) -> Decimal:
        return a.ln()

    ln_verifier = verifier_for(ln)
    ln_verifier.verify_property(Decimal(1), predicate=around(Decimal(0)))
    ln_verifier.verify_property(Decimal(1).exp(), predicate=around(Decimal(1)))
    ln_verifier.verify_property(Decimal('2.5').exp(), predicate=around(Decimal('2.5')))


def test_log10():
    def log10(a: Decimal) -> Decimal:
        return a.log10()

    log10_verifier = verifier_for(log10)
    log10_verifier.verify_property(Decimal(1), predicate=around(Decimal(0)))
    log10_verifier.verify_property(Decimal(10), predicate=around(Decimal(1)))
    log10_verifier.verify_property(Decimal('0.1'), predicate=around(Decimal(-1)))
    log10_verifier.verify_property(Decimal('5'), predicate=around(Decimal('0.69897')))


def test_logb():
    def logb(a: Decimal) -> Decimal:
        return a.logb()

    logb_verifier = verifier_for(logb)
    logb_verifier.verify(Decimal(1), expected_result=Decimal(0))
    logb_verifier.verify(Decimal(100), expected_result=Decimal(2))
    logb_verifier.verify(Decimal(200), expected_result=Decimal(2))
    logb_verifier.verify(Decimal('0.1'), expected_result=Decimal(-1))
    logb_verifier.verify(Decimal('0.5'), expected_result=Decimal(-1))


def test_logical_and():
    def logical_and(a: Decimal, b: Decimal) -> Decimal:
        return a.logical_and(b)

    logical_and_verifier = verifier_for(logical_and)
    logical_and_verifier.verify(Decimal('1010'), Decimal('1100'), expected_result=Decimal('1000'))


def test_logical_invert():
    def logical_invert(a: Decimal) -> Decimal:
        return a.logical_invert()

    logical_invert_verifier = verifier_for(logical_invert)
    logical_invert_verifier.verify(Decimal('1010'), expected_result=Decimal('1111111111111111111111110101'))


def test_logical_or():
    def logical_or(a: Decimal, b: Decimal) -> Decimal:
        return a.logical_or(b)

    logical_or_verifier = verifier_for(logical_or)
    logical_or_verifier.verify(Decimal('1010'), Decimal('1100'), expected_result=Decimal('1110'))


def test_logical_xor():
    def logical_xor(a: Decimal, b: Decimal) -> Decimal:
        return a.logical_xor(b)

    logical_xor_verifier = verifier_for(logical_xor)
    logical_xor_verifier.verify(Decimal('1010'), Decimal('1100'), expected_result=Decimal('0110'))


def test_max():
    def decimal_max(a: Decimal, b: Decimal) -> Decimal:
        return a.max(b)

    decimal_max_verifier = verifier_for(decimal_max)
    decimal_max_verifier.verify(Decimal(1), Decimal(2), expected_result=Decimal(2))
    decimal_max_verifier.verify(Decimal(2), Decimal(1), expected_result=Decimal(2))
    decimal_max_verifier.verify(Decimal(1), Decimal(-2), expected_result=Decimal(1))


def test_max_mag():
    def decimal_max_mag(a: Decimal, b: Decimal) -> Decimal:
        return a.max_mag(b)

    decimal_max_mag_verifier = verifier_for(decimal_max_mag)
    decimal_max_mag_verifier.verify(Decimal(1), Decimal(2), expected_result=Decimal(2))
    decimal_max_mag_verifier.verify(Decimal(2), Decimal(1), expected_result=Decimal(2))
    decimal_max_mag_verifier.verify(Decimal(1), Decimal(-2), expected_result=Decimal(-2))


def test_min():
    def decimal_min(a: Decimal, b: Decimal) -> Decimal:
        return a.min(b)

    decimal_min_verifier = verifier_for(decimal_min)
    decimal_min_verifier.verify(Decimal(1), Decimal(2), expected_result=Decimal(1))
    decimal_min_verifier.verify(Decimal(2), Decimal(1), expected_result=Decimal(1))
    decimal_min_verifier.verify(Decimal(1), Decimal(-2), expected_result=Decimal(-2))


def test_min_mag():
    def decimal_min_mag(a: Decimal, b: Decimal) -> Decimal:
        return a.min_mag(b)

    decimal_min_mag_verifier = verifier_for(decimal_min_mag)
    decimal_min_mag_verifier.verify(Decimal(1), Decimal(2), expected_result=Decimal(1))
    decimal_min_mag_verifier.verify(Decimal(2), Decimal(1), expected_result=Decimal(1))
    decimal_min_mag_verifier.verify(Decimal(1), Decimal(-2), expected_result=Decimal(1))


def test_next_minus():
    def next_minus(a: Decimal) -> Decimal:
        return a.next_minus()

    next_minus_verifier = verifier_for(next_minus)
    next_minus_verifier.verify(Decimal(1), expected_result=Decimal('0.9999999999999999999999999999'))
    next_minus_verifier.verify(Decimal('0.9999999999999999999999999999'),
                               expected_result=Decimal('0.9999999999999999999999999998'))


def test_next_plus():
    def next_plus(a: Decimal) -> Decimal:
        return a.next_plus()

    next_plus_verifier = verifier_for(next_plus)
    next_plus_verifier.verify(Decimal(1), expected_result=Decimal('1.000000000000000000000000001'))
    next_plus_verifier.verify(Decimal('1.000000000000000000000000001'),
                              expected_result=Decimal('1.000000000000000000000000002'))


def test_next_toward():
    def next_toward(a: Decimal, b: Decimal) -> Decimal:
        return a.next_toward(b)

    next_toward_verifier = verifier_for(next_toward)
    next_toward_verifier.verify(Decimal(1), Decimal(0), expected_result=Decimal('0.9999999999999999999999999999'))
    next_toward_verifier.verify(Decimal(1), Decimal(2), expected_result=Decimal('1.000000000000000000000000001'))
    next_toward_verifier.verify(Decimal(1), Decimal(1), expected_result=Decimal(1))


def test_normalize():
    def normalize(a: Decimal) -> Decimal:
        return a.normalize()

    normalize_verifier = verifier_for(normalize)
    normalize_verifier.verify(Decimal(10), expected_result=Decimal(10))


def test_number_class():
    def number_class(a: Decimal) -> str:
        return a.number_class()

    number_class_verifier = verifier_for(number_class)
    number_class_verifier.verify(Decimal(1), expected_result='+Normal')
    number_class_verifier.verify(Decimal(-1), expected_result='-Normal')
    number_class_verifier.verify(Decimal(0), expected_result='+Zero')


def test_quantize():
    def quantize(a: Decimal, b: Decimal) -> Decimal:
        return a.quantize(b)

    quantize_verifier = verifier_for(quantize)
    quantize_verifier.verify(Decimal('1.41421356'), Decimal('1.000'),
                             expected_result=Decimal('1.414'))


def test_radix():
    def radix(a: Decimal) -> Decimal:
        return a.radix()

    radix_verifier = verifier_for(radix)
    radix_verifier.verify(Decimal(1), expected_result=Decimal(10))


def test_remainder_near():
    def remainder_near(a: Decimal, b: Decimal) -> Decimal:
        return a.remainder_near(b)

    remainder_near_verifier = verifier_for(remainder_near)
    remainder_near_verifier.verify(Decimal(18), Decimal(10), expected_result=Decimal(-2))
    remainder_near_verifier.verify(Decimal(25), Decimal(10), expected_result=Decimal(5))
    remainder_near_verifier.verify(Decimal(35), Decimal(10), expected_result=Decimal(-5))


def test_rotate():
    def rotate(a: Decimal, b: int) -> Decimal:
        return a.rotate(b)

    rotate_verifier = verifier_for(rotate)
    rotate_verifier.verify(Decimal('12.34'), 3, expected_result=Decimal('12340.00'))
    rotate_verifier.verify(Decimal('12.34'), -3, expected_result=Decimal('23400000000000000000000000.01'))


def test_same_quantum():
    def same_quantum(a: Decimal, b: Decimal) -> bool:
        return a.same_quantum(b)

    same_quantum_verifier = verifier_for(same_quantum)
    same_quantum_verifier.verify(Decimal(1), Decimal(2), expected_result=True)
    same_quantum_verifier.verify(Decimal(1), Decimal(10), expected_result=True)
    same_quantum_verifier.verify(Decimal('0.1'), Decimal('0.01'), expected_result=False)


def test_scaleb():
    def scaleb(a: Decimal, b: int) -> Decimal:
        return a.scaleb(b)

    scaleb_verifier = verifier_for(scaleb)
    scaleb_verifier.verify(Decimal(1), 2, expected_result=Decimal(100))
    scaleb_verifier.verify(Decimal(1), -2, expected_result=Decimal('0.01'))


def test_sqrt():
    def sqrt(a: Decimal) -> Decimal:
        return a.sqrt()

    sqrt_verifier = verifier_for(sqrt)
    sqrt_verifier.verify(Decimal(1), expected_result=Decimal(1))
    sqrt_verifier.verify(Decimal(2), expected_result=Decimal('1.414213562373095048801688724'))
    sqrt_verifier.verify(Decimal(9), expected_result=Decimal(3))


def test_to_eng_string():
    def to_eng_string(a: Decimal) -> str:
        return a.to_eng_string()

    to_eng_string_verifier = verifier_for(to_eng_string)
    to_eng_string_verifier.verify(Decimal('123E+1'), expected_result='1.23E+3')


def test_to_integral():
    def to_integral(a: Decimal) -> Decimal:
        return a.to_integral()

    to_integral_verifier = verifier_for(to_integral)
    to_integral_verifier.verify(Decimal('1.23'), Decimal('1'))
    to_integral_verifier.verify(Decimal('1.7'), Decimal('2'))
    to_integral_verifier.verify(Decimal('1.5'), Decimal('2'))


def test_to_integral_exact():
    def to_integral_exact(a: Decimal) -> Decimal:
        return a.to_integral_exact()

    to_integral_exact_verifier = verifier_for(to_integral_exact)
    to_integral_exact_verifier.verify(Decimal('1.23'), Decimal('1'))
    to_integral_exact_verifier.verify(Decimal('1.7'), Decimal('2'))
    to_integral_exact_verifier.verify(Decimal('1.5'), Decimal('2'))


def test_to_integral_value():
    def to_to_integral_value(a: Decimal) -> Decimal:
        return a.to_to_integral_value()

    to_to_integral_value_verifier = verifier_for(to_to_integral_value)
    to_to_integral_value_verifier.verify(Decimal('1.23'), Decimal('1'))
    to_to_integral_value_verifier.verify(Decimal('1.7'), Decimal('2'))
    to_to_integral_value_verifier.verify(Decimal('1.5'), Decimal('2'))
