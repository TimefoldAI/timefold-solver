from .conftest import verifier_for
from typing import Union


def test_membership():
    def membership(tested: tuple, x: object):
        return x in tested

    def not_membership(tested: tuple, x: object):
        return x not in tested

    membership_verifier = verifier_for(membership)
    not_membership_verifier = verifier_for(not_membership)

    membership_verifier.verify((1, 2, 3), 1, expected_result=True)
    not_membership_verifier.verify((1, 2, 3), 1, expected_result=False)

    membership_verifier.verify((1, 2, 3), 4, expected_result=False)
    not_membership_verifier.verify((1, 2, 3), 4, expected_result=True)

    membership_verifier.verify((1, 2, 3), 3, expected_result=True)
    not_membership_verifier.verify((1, 2, 3), 3, expected_result=False)


def test_concat():
    def concat(x: tuple, y: tuple):
        out = x + y
        return out, out is x, out is y

    concat_verifier = verifier_for(concat)

    concat_verifier.verify((1, 2), (3, 4), expected_result=((1, 2, 3, 4), False, False))
    concat_verifier.verify((), (1, 2, 3), expected_result=((1, 2, 3), False, True))
    concat_verifier.verify((1, 2, 3), (), expected_result=((1, 2, 3), True, False))
    concat_verifier.verify((3,), (2, 1), expected_result=((3, 2, 1), False, False))


def test_repeat():
    def repeat_left(x: tuple, y: int):
        out = x * y
        return out, out is x, out is y

    def repeat_right(x: int, y: tuple):
        out = x * y
        return out, out is x, out is y

    repeat_left_verifier = verifier_for(repeat_left)
    repeat_right_verifier = verifier_for(repeat_right)

    repeat_left_verifier.verify((1, 2, 3), 1, expected_result=((1, 2, 3), True, False))
    repeat_left_verifier.verify((1, 2, 3), 2, expected_result=((1, 2, 3, 1, 2, 3), False, False))
    repeat_left_verifier.verify((1, 2), 4, expected_result=((1, 2, 1, 2, 1, 2, 1, 2), False, False))
    repeat_left_verifier.verify((1, 2, 3), 0, expected_result=((), False, False))
    repeat_left_verifier.verify((1, 2, 3), -1, expected_result=((), False, False))
    repeat_left_verifier.verify((1, 2, 3), -2, expected_result=((), False, False))

    repeat_right_verifier.verify(1, (1, 2, 3), expected_result=((1, 2, 3), False, True))
    repeat_right_verifier.verify(2, (1, 2, 3), expected_result=((1, 2, 3, 1, 2, 3), False, False))
    repeat_right_verifier.verify(4, (1, 2), expected_result=((1, 2, 1, 2, 1, 2, 1, 2), False, False))
    repeat_right_verifier.verify(0, (1, 2, 3), expected_result=((), False, False))
    repeat_right_verifier.verify(-1, (1, 2, 3), expected_result=((), False, False))
    repeat_right_verifier.verify(-2, (1, 2, 3), expected_result=((), False, False))


def test_get_item():
    def get_item(tested: tuple, index: int):
        return tested[index]

    get_item_verifier = verifier_for(get_item)

    get_item_verifier.verify((1, 2, 3), 1, expected_result=2)
    get_item_verifier.verify((1, 2, 3), -1, expected_result=3)
    get_item_verifier.verify((1, 2, 3, 4), -1, expected_result=4)
    get_item_verifier.verify((1, 2, 3, 4), -2, expected_result=3)
    get_item_verifier.verify((1, 2, 3, 4), 0, expected_result=1)
    get_item_verifier.verify((1, 2, 3), 3, expected_error=IndexError)
    get_item_verifier.verify((1, 2, 3), -4, expected_error=IndexError)


def test_get_slice():
    def get_slice(tested: tuple, start: Union[int, None], end: Union[int, None]):
        return tested[start:end]

    get_slice_verifier = verifier_for(get_slice)

    get_slice_verifier.verify((1, 2, 3, 4, 5), 1, 3, expected_result=(2, 3))
    get_slice_verifier.verify((1, 2, 3, 4, 5), -3, -1, expected_result=(3, 4))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, -2, expected_result=(1, 2, 3))
    get_slice_verifier.verify((1, 2, 3, 4, 5), -3, 4, expected_result=(3, 4))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 3, 1, expected_result=())
    get_slice_verifier.verify((1, 2, 3, 4, 5), -1, -3, expected_result=())

    get_slice_verifier.verify((1, 2, 3, 4, 5), 100, 1000, expected_result=())
    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, 1000, expected_result=(1, 2, 3, 4, 5))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 1, None, expected_result=(2, 3, 4, 5))
    get_slice_verifier.verify((1, 2, 3, 4, 5), None, 2, expected_result=(1, 2))
    get_slice_verifier.verify((1, 2, 3, 4, 5), None, None, expected_result=(1, 2, 3, 4, 5))


def test_get_slice_with_step():
    def get_slice_with_step(tested: tuple, start: Union[int, None], end: Union[int, None], step: Union[int, None]):
        return tested[start:end:step]

    get_slice_verifier = verifier_for(get_slice_with_step)

    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, None, 2, expected_result=(1, 3, 5))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 1, None, 2, expected_result=(2, 4))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, 5, 2, expected_result=(1, 3, 5))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 1, 5, 2, expected_result=(2, 4))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, -1, 2, expected_result=(1, 3))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 1, -1, 2, expected_result=(2, 4))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 4, None, -2, expected_result=(5, 3, 1))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 3, None, -2, expected_result=(4, 2))
    get_slice_verifier.verify((1, 2, 3, 4, 5), -1, -6, -2, expected_result=(5, 3, 1))
    get_slice_verifier.verify((1, 2, 3, 4, 5), -2, -6, -2, expected_result=(4, 2))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 4, 0, -2, expected_result=(5, 3))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 3, 0, -2, expected_result=(4, 2))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, None, None, expected_result=(1, 2, 3, 4, 5))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, 3, None, expected_result=(1, 2, 3))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 3, 1, -1, expected_result=(4, 3))
    get_slice_verifier.verify((1, 2, 3, 4, 5), -1, -3, -1, expected_result=(5, 4))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 3, 1, 1, expected_result=())
    get_slice_verifier.verify((1, 2, 3, 4, 5), -1, -3, 1, expected_result=())


def test_len():
    def length(tested: tuple):
        return len(tested)

    len_verifier = verifier_for(length)

    len_verifier.verify((), expected_result=0)
    len_verifier.verify((1,), expected_result=1)
    len_verifier.verify((1, 2), expected_result=2)
    len_verifier.verify((3, 2, 1), expected_result=3)


def test_index():
    def index(tested: tuple, item: object):
        return tested.index(item)

    def index_start(tested: tuple, item: object, start: int):
        return tested.index(item, start)

    def index_start_end(tested: tuple, item: object, start: int, end: int):
        return tested.index(item, start, end)

    index_verifier = verifier_for(index)
    index_start_verifier = verifier_for(index_start)
    index_start_end_verifier = verifier_for(index_start_end)

    index_verifier.verify((1, 2, 3), 1, expected_result=0)
    index_verifier.verify((1, 2, 3), 2, expected_result=1)
    index_verifier.verify((1, 2, 3), 5, expected_error=ValueError)

    index_start_verifier.verify((1, 2, 3), 1, 1, expected_error=ValueError)
    index_start_verifier.verify((1, 2, 3), 2, 1, expected_result=1)
    index_start_verifier.verify((1, 2, 3), 3, 1, expected_result=2)
    index_start_verifier.verify((1, 2, 3), 5, 1, expected_error=ValueError)

    index_start_verifier.verify((1, 2, 3), 1, -2, expected_error=ValueError)
    index_start_verifier.verify((1, 2, 3), 2, -2, expected_result=1)
    index_start_verifier.verify((1, 2, 3), 3, -2, expected_result=2)
    index_start_verifier.verify((1, 2, 3), 5, -2, expected_error=ValueError)

    index_start_end_verifier.verify((1, 2, 3), 1, 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify((1, 2, 3), 2, 1, 2, expected_result=1)
    index_start_end_verifier.verify((1, 2, 3), 3, 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify((1, 2, 3), 5, 1, 2, expected_error=ValueError)

    index_start_end_verifier.verify((1, 2, 3), 1, -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify((1, 2, 3), 2, -2, -1, expected_result=1)
    index_start_end_verifier.verify((1, 2, 3), 3, -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify((1, 2, 3), 5, -2, -1, expected_error=ValueError)


def test_count():
    def count(tested: tuple, item: object):
        return tested.count(item)

    count_verifier = verifier_for(count)

    count_verifier.verify((1, 2, 3), 1, expected_result=1)
    count_verifier.verify((1, 2, 3), 2, expected_result=1)
    count_verifier.verify((1, 2, 3), 3, expected_result=1)
    count_verifier.verify((1, 2, 3), 4, expected_result=0)

    count_verifier.verify((1, 2, 3, 1), 1, expected_result=2)
    count_verifier.verify((1, 1, 3, 1), 1, expected_result=3)
    count_verifier.verify((), 1, expected_result=0)


def test_compare_methods():
    def less_than(a: tuple, b: tuple):
        return a < b

    def greater_than(a: tuple, b: tuple):
        return a > b

    def less_than_or_equal(a: tuple, b: tuple):
        return a <= b

    def greater_than_or_equal(a: tuple, b: tuple):
        return a >= b

    less_than_verifier = verifier_for(less_than)
    greater_than_verifier = verifier_for(greater_than)
    less_than_or_equal_verifier = verifier_for(less_than_or_equal)
    greater_than_or_equal_verifier = verifier_for(greater_than_or_equal)

    less_than_verifier.verify((1, 1), (1, 1), expected_result=False)
    less_than_verifier.verify((1, 1), (1, 2), expected_result=True)
    less_than_verifier.verify((1, 2), (2, 1), expected_result=True)
    less_than_verifier.verify((1, 1), (1, 1, 1), expected_result=True)
    less_than_verifier.verify((2, 1), (1, 2), expected_result=False)
    less_than_verifier.verify((1, 2), (1, 1), expected_result=False)
    less_than_verifier.verify((1, 1, 1), (1, 1), expected_result=False)

    greater_than_verifier.verify((1, 1), (1, 1), expected_result=False)
    greater_than_verifier.verify((1, 1), (1, 2), expected_result=False)
    greater_than_verifier.verify((1, 2), (2, 1), expected_result=False)
    greater_than_verifier.verify((1, 1), (1, 1, 1), expected_result=False)
    greater_than_verifier.verify((2, 1), (1, 2), expected_result=True)
    greater_than_verifier.verify((1, 2), (1, 1), expected_result=True)
    greater_than_verifier.verify((1, 1, 1), (1, 1), expected_result=True)

    less_than_or_equal_verifier.verify((1, 1), (1, 1), expected_result=True)
    less_than_or_equal_verifier.verify((1, 1), (1, 2), expected_result=True)
    less_than_or_equal_verifier.verify((1, 2), (2, 1), expected_result=True)
    less_than_or_equal_verifier.verify((1, 1), (1, 1, 1), expected_result=True)
    less_than_or_equal_verifier.verify((2, 1), (1, 2), expected_result=False)
    less_than_or_equal_verifier.verify((1, 2), (1, 1), expected_result=False)
    less_than_or_equal_verifier.verify((1, 1, 1), (1, 1), expected_result=False)

    greater_than_or_equal_verifier.verify((1, 1), (1, 1), expected_result=True)
    greater_than_or_equal_verifier.verify((1, 1), (1, 2), expected_result=False)
    greater_than_or_equal_verifier.verify((1, 2), (2, 1), expected_result=False)
    greater_than_or_equal_verifier.verify((1, 1), (1, 1, 1), expected_result=False)
    greater_than_or_equal_verifier.verify((2, 1), (1, 2), expected_result=True)
    greater_than_or_equal_verifier.verify((1, 2), (1, 1), expected_result=True)
    greater_than_or_equal_verifier.verify((1, 1, 1), (1, 1), expected_result=True)
