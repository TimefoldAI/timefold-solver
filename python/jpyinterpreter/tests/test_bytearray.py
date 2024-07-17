from typing import Union
from .conftest import verifier_for


########################################
# Sequence methods
########################################

def test_membership():
    def membership(tested: bytearray, x: bytearray) -> bool:
        return x in tested

    def not_membership(tested: bytearray, x: bytearray) -> bool:
        return x not in tested

    membership_verifier = verifier_for(membership)
    not_membership_verifier = verifier_for(not_membership)

    membership_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), expected_result=True)
    not_membership_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), expected_result=False)

    membership_verifier.verify(bytearray(b'hello world'), bytearray(b'test'), expected_result=False)
    not_membership_verifier.verify(bytearray(b'hello world'), bytearray(b'test'), expected_result=True)

    membership_verifier.verify(bytearray(b'hello world'), bytearray(b''), expected_result=True)
    not_membership_verifier.verify(bytearray(b'hello world'), bytearray(b''), expected_result=False)


def test_concat():
    def concat(x: bytearray, y: bytearray) -> tuple:
        out = x + y
        return out, out is x, out is y

    concat_verifier = verifier_for(concat)

    concat_verifier.verify(bytearray(b'hello '), bytearray(b'world'), expected_result=(bytearray(b'hello world'), False, False))
    concat_verifier.verify(bytearray(b''), bytearray(b'hello world'), expected_result=(bytearray(b'hello world'), False, False))
    concat_verifier.verify(bytearray(b'hello world'), bytearray(b''), expected_result=(bytearray(b'hello world'), False, False))
    concat_verifier.verify(bytearray(b'world '), bytearray(b'hello'), expected_result=(bytearray(b'world hello'), False, False))


def test_repeat():
    def left_repeat(x: bytearray, y: int) -> tuple:
        out = x * y
        return out, out is x, out is y

    def right_repeat(x: int, y: bytearray) -> tuple:
        out = x * y
        return out, out is x, out is y

    left_repeat_verifier = verifier_for(left_repeat)
    right_repeat_verifier = verifier_for(right_repeat)

    left_repeat_verifier.verify(bytearray(b'hi'), 1, expected_result=(bytearray(b'hi'), False, False))
    left_repeat_verifier.verify(bytearray(b'abc'), 2, expected_result=(bytearray(b'abcabc'), False, False))
    left_repeat_verifier.verify(bytearray(b'a'), 4, expected_result=(bytearray(b'aaaa'), False, False))
    left_repeat_verifier.verify(bytearray(b'test'), 0, expected_result=(bytearray(b''), False, False))
    left_repeat_verifier.verify(bytearray(b'test'), -1, expected_result=(bytearray(b''), False, False))
    left_repeat_verifier.verify(bytearray(b'test'), -2, expected_result=(bytearray(b''), False, False))

    right_repeat_verifier.verify(1, bytearray(b'hi'), expected_result=(bytearray(b'hi'), False, False))
    right_repeat_verifier.verify(2, bytearray(b'abc'), expected_result=(bytearray(b'abcabc'), False, False))
    right_repeat_verifier.verify(4, bytearray(b'a'), expected_result=(bytearray(b'aaaa'), False, False))
    right_repeat_verifier.verify(0, bytearray(b'test'), expected_result=(bytearray(b''), False, False))
    right_repeat_verifier.verify(-1, bytearray(b'test'), expected_result=(bytearray(b''), False, False))
    right_repeat_verifier.verify(-2, bytearray(b'test'), expected_result=(bytearray(b''), False, False))


def test_get_item():
    def get_item(tested: bytearray, index: int) -> int:
        return tested[index]

    get_item_verifier = verifier_for(get_item)

    get_item_verifier.verify(bytearray(b'abc'), 1, expected_result=ord(bytearray(b'b')))
    get_item_verifier.verify(bytearray(b'abc'), -1, expected_result=ord(bytearray(b'c')))
    get_item_verifier.verify(bytearray(b'abcd'), -1, expected_result=ord(bytearray(b'd')))
    get_item_verifier.verify(bytearray(b'abcd'), -2, expected_result=ord(bytearray(b'c')))
    get_item_verifier.verify(bytearray(b'abcd'), 0, expected_result=ord(bytearray(b'a')))
    get_item_verifier.verify(bytearray(b'abc'), 3, expected_error=IndexError)
    get_item_verifier.verify(bytearray(b'abc'), -4, expected_error=IndexError)


def test_get_slice():
    def get_slice(tested: bytearray, start: Union[int, None], end: Union[int, None]) -> bytearray:
        return tested[start:end]

    get_slice_verifier = verifier_for(get_slice)

    get_slice_verifier.verify(bytearray(b'abcde'), 1, 3, expected_result=bytearray(b'bc'))
    get_slice_verifier.verify(bytearray(b'abcde'), -3, -1, expected_result=bytearray(b'cd'))

    get_slice_verifier.verify(bytearray(b'abcde'), 0, -2, expected_result=bytearray(b'abc'))
    get_slice_verifier.verify(bytearray(b'abcde'), -3, 4, expected_result=bytearray(b'cd'))

    get_slice_verifier.verify(bytearray(b'abcde'), 3, 1, expected_result=bytearray(b''))
    get_slice_verifier.verify(bytearray(b'abcde'), -1, -3, expected_result=bytearray(b''))

    get_slice_verifier.verify(bytearray(b'abcde'), 100, 1000, expected_result=bytearray(b''))
    get_slice_verifier.verify(bytearray(b'abcde'), 0, 1000, expected_result=bytearray(b'abcde'))

    get_slice_verifier.verify(bytearray(b'abcde'), 1, None, expected_result=bytearray(b'bcde'))
    get_slice_verifier.verify(bytearray(b'abcde'), None, 2, expected_result=bytearray(b'ab'))
    get_slice_verifier.verify(bytearray(b'abcde'), None, None, expected_result=bytearray(b'abcde'))


def test_get_slice_with_step():
    def get_slice_with_step(tested: bytearray, start: Union[int, None], end: Union[int, None],
                            step: Union[int, None]) -> bytearray:
        return tested[start:end:step]

    get_slice_verifier = verifier_for(get_slice_with_step)

    get_slice_verifier.verify(bytearray(b'abcde'), 0, None, 2, expected_result=bytearray(b'ace'))
    get_slice_verifier.verify(bytearray(b'abcde'), 1, None, 2, expected_result=bytearray(b'bd'))
    get_slice_verifier.verify(bytearray(b'abcde'), 0, 5, 2, expected_result=bytearray(b'ace'))
    get_slice_verifier.verify(bytearray(b'abcde'), 1, 5, 2, expected_result=bytearray(b'bd'))
    get_slice_verifier.verify(bytearray(b'abcde'), 0, -1, 2, expected_result=bytearray(b'ac'))
    get_slice_verifier.verify(bytearray(b'abcde'), 1, -1, 2, expected_result=bytearray(b'bd'))

    get_slice_verifier.verify(bytearray(b'abcde'), 4, None, -2, expected_result=bytearray(b'eca'))
    get_slice_verifier.verify(bytearray(b'abcde'), 3, None, -2, expected_result=bytearray(b'db'))
    get_slice_verifier.verify(bytearray(b'abcde'), -1, -6, -2, expected_result=bytearray(b'eca'))
    get_slice_verifier.verify(bytearray(b'abcde'), -2, -6, -2, expected_result=bytearray(b'db'))
    get_slice_verifier.verify(bytearray(b'abcde'), 4, 0, -2, expected_result=bytearray(b'ec'))
    get_slice_verifier.verify(bytearray(b'abcde'), 3, 0, -2, expected_result=bytearray(b'db'))

    get_slice_verifier.verify(bytearray(b'abcde'), 0, None, None, expected_result=bytearray(b'abcde'))
    get_slice_verifier.verify(bytearray(b'abcde'), 0, 3, None, expected_result=bytearray(b'abc'))

    get_slice_verifier.verify(bytearray(b'abcde'), 3, 1, -1, expected_result=bytearray(b'dc'))
    get_slice_verifier.verify(bytearray(b'abcde'), -1, -3, -1, expected_result=bytearray(b'ed'))
    get_slice_verifier.verify(bytearray(b'abcde'), 3, 1, 1, expected_result=bytearray(b''))
    get_slice_verifier.verify(bytearray(b'abcde'), -1, -3, 1, expected_result=bytearray(b''))


def test_len():
    def length(tested: bytearray) -> int:
        return len(tested)

    len_verifier = verifier_for(length)

    len_verifier.verify(bytearray(b''), expected_result=0)
    len_verifier.verify(bytearray(b'a'), expected_result=1)
    len_verifier.verify(bytearray(b'ab'), expected_result=2)
    len_verifier.verify(bytearray(b'cba'), expected_result=3)


def test_index():
    def index(tested: bytearray, item: bytearray) -> int:
        return tested.index(item)

    def index_start(tested: bytearray, item: bytearray, start: int) -> int:
        return tested.index(item, start)

    def index_start_end(tested: bytearray, item: bytearray, start: int, end: int) -> int:
        return tested.index(item, start, end)

    index_verifier = verifier_for(index)
    index_start_verifier = verifier_for(index_start)
    index_start_end_verifier = verifier_for(index_start_end)

    index_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), expected_result=0)
    index_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), expected_result=1)
    index_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), expected_error=ValueError)

    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, expected_result=3)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 5, expected_error=ValueError)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, expected_result=1)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, expected_result=2)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, expected_error=ValueError)

    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -3, expected_result=3)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, expected_result=4)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, expected_result=5)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, expected_error=ValueError)

    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, 2, expected_result=1)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, 2, expected_error=ValueError)

    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, -1, expected_result=4)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, -1, expected_error=ValueError)


def test_count():
    def count(tested: bytearray, item: bytearray) -> int:
        return tested.count(item)

    count_verifier = verifier_for(count)

    count_verifier.verify(bytearray(b'abc'), bytearray(b'a'), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), bytearray(b'b'), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), bytearray(b'c'), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), bytearray(b'd'), expected_result=0)

    count_verifier.verify(bytearray(b'abca'), bytearray(b'a'), expected_result=2)
    count_verifier.verify(bytearray(b'aaca'), bytearray(b'a'), expected_result=3)
    count_verifier.verify(bytearray(b''), bytearray(b'a'), expected_result=0)


########################################
# Mutable Sequence operations
########################################


def test_set_item():
    def set_item(tested: bytearray, index: int, value: int) -> bytearray:
        tested[index] = value
        return tested

    set_item_verifier = verifier_for(set_item)
    set_item_verifier.verify(bytearray(b'abcde'), 0, ord('g'), expected_result=bytearray(b'gbcde'))
    set_item_verifier.verify(bytearray(b'abcde'), 2, ord('C'), expected_result=bytearray(b'abCde'))
    set_item_verifier.verify(bytearray(b'abcde'), -1, ord('F'), expected_result=bytearray(b'abcdF'))
    set_item_verifier.verify(bytearray(b'abcde'), -10, ord('a'), expected_error=IndexError)
    set_item_verifier.verify(bytearray(b'abcde'), 10, ord('a'), expected_error=IndexError)

def test_set_slice():
    def set_slice(tested: bytearray, start: Union[int, None], stop: Union[int, None], value: bytes) -> bytearray:
        tested[start:stop] = value
        return tested

    set_slice_verifier = verifier_for(set_slice)
    set_slice_verifier.verify(bytearray(b'abcde'), 1, 3, b'', expected_result=bytearray(b'ade'))
    set_slice_verifier.verify(bytearray(b'abcde'), 2, 2, b'H', expected_result=bytearray(b'abHcde'))
    set_slice_verifier.verify(bytearray(b'abcde'), 1, 3, b'H', expected_result=bytearray(b'aHde'))
    set_slice_verifier.verify(bytearray(b'abcde'), 1, 3, b'HI', expected_result=bytearray(b'aHIde'))
    set_slice_verifier.verify(bytearray(b'abcde'), 1, 3, b'HIJ', expected_result=bytearray(b'aHIJde'))

    set_slice_verifier.verify(bytearray(b'abcde'), -4, -2, b'HI', expected_result=bytearray(b'aHIde'))
    set_slice_verifier.verify(bytearray(b'abcde'), 1, -2, b'HI', expected_result=bytearray(b'aHIde'))

    set_slice_verifier.verify(bytearray(b'abcde'), 5, 5, b'F', expected_result=bytearray(b'abcdeF'))

    set_slice_verifier.verify(bytearray(b'abcde'), 1, None, b'', expected_result=bytearray(b'a'))
    set_slice_verifier.verify(bytearray(b'abcde'), 1, None, b'HI', expected_result=bytearray(b'aHI'))


def test_delete_slice():
    def delete_slice(tested: bytearray, start: Union[int, None], stop: Union[int, None]) -> bytearray:
        del tested[start:stop]
        return tested

    delete_slice_verifier = verifier_for(delete_slice)
    delete_slice_verifier.verify(bytearray(b'abcde'), 1, 3, expected_result=bytearray(b'ade'))
    delete_slice_verifier.verify(bytearray(b'abcde'), 3, 5, expected_result=bytearray(b'abc'))
    delete_slice_verifier.verify(bytearray(b'abcde'), 1, None, expected_result=bytearray(b'a'))
    delete_slice_verifier.verify(bytearray(b'abcde'), None, 3, expected_result=bytearray(b'de'))
    delete_slice_verifier.verify(bytearray(b'abcde'), None, None, expected_result=bytearray(b''))


def test_set_slice_with_step():
    def set_slice_with_step(tested: bytearray, start: Union[int, None], stop: Union[int, None], step: Union[int, None],
                            value: bytes) -> bytearray:
        tested[start:stop:step] = value
        return tested

    set_slice_with_step_verifier = verifier_for(set_slice_with_step)

    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 3, 0, -1, b'BCD', expected_result=bytearray(b'aDCBe'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 1, 4, 2, b'BD', expected_result=bytearray(b'aBcDe'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 1, -1, 2, b'BD', expected_result=bytearray(b'aBcDe'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 0, None, 2, b'ACE',
                                        expected_result=bytearray(b'AbCdE'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), None, 4, 2, b'AC',
                                        expected_result=bytearray(b'AbCde'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), None, None, 2, b'ACE',
                                        expected_result=bytearray(b'AbCdE'))

    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 3, 0, -1, b'', expected_result=bytearray(b'ae'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 3, 0, -1, b'BCDE', expected_error=ValueError)


def test_delete_slice_with_step():
    def delete_slice_with_step(tested: bytearray, start: Union[int, None], stop: Union[int, None],
                               step: Union[int, None]) -> bytearray:
        del tested[start:stop:step]
        return tested

    delete_slice_with_step_verifier = verifier_for(delete_slice_with_step)

    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), 3, 0, -1, expected_result=bytearray(b'ae'))
    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), 1, 4, 2, expected_result=bytearray(b'ace'))
    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), 1, -1, 2, expected_result=bytearray(b'ace'))
    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), 0, None, 2,
                                           expected_result=bytearray(b'bd'))
    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), None, 4, 2,
                                           expected_result=bytearray(b'bde'))
    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), None, None, 2,
                                           expected_result=bytearray(b'bd'))


def test_append():
    def append(tested: bytearray, item: int) -> bytearray:
        tested.append(item)
        return tested

    append_verifier = verifier_for(append)

    append_verifier.verify(bytearray(b''), ord('a'), expected_result=bytearray(b'a'))
    append_verifier.verify(bytearray(b'a'), ord('b'), expected_result=bytearray(b'ab'))
    append_verifier.verify(bytearray(b'ab'), ord('c'), expected_result=bytearray(b'abc'))
    append_verifier.verify(bytearray(b'abc'), ord('c'), expected_result=bytearray(b'abcc'))


def test_clear():
    def clear(tested: bytearray) -> bytearray:
        tested.clear()
        return tested

    clear_verifier = verifier_for(clear)

    clear_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    clear_verifier.verify(bytearray(b'a'), expected_result=bytearray(b''))
    clear_verifier.verify(bytearray(b'ab'), expected_result=bytearray(b''))
    clear_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b''))


def test_copy():
    def copy(tested: bytearray) -> tuple:
        out = tested.copy()
        return out, out is tested

    copy_verifier = verifier_for(copy)

    copy_verifier.verify(bytearray(b''), expected_result=(bytearray(b''), False))
    copy_verifier.verify(bytearray(b'a'), expected_result=(bytearray(b'a'), False))
    copy_verifier.verify(bytearray(b'ab'), expected_result=(bytearray(b'ab'), False))
    copy_verifier.verify(bytearray(b'abc'), expected_result=(bytearray(b'abc'), False))


def test_extend():
    def extend(tested: bytearray, item: bytearray) -> bytearray:
        tested.extend(item)
        return tested

    extend_verifier = verifier_for(extend)

    extend_verifier.verify(bytearray(b''), bytearray(b'a'), expected_result=bytearray(b'a'))
    extend_verifier.verify(bytearray(b'a'), bytearray(b'b'), expected_result=bytearray(b'ab'))
    extend_verifier.verify(bytearray(b'ab'), bytearray(b'c'), expected_result=bytearray(b'abc'))
    extend_verifier.verify(bytearray(b'abc'), bytearray(b''), expected_result=bytearray(b'abc'))
    extend_verifier.verify(bytearray(b'abc'), bytearray(b'de'), expected_result=bytearray(b'abcde'))


def test_inplace_add():
    def extend(tested: bytearray, item: bytearray) -> bytearray:
        tested += item
        return tested

    extend_verifier = verifier_for(extend)

    extend_verifier.verify(bytearray(b''), bytearray(b'a'), expected_result=bytearray(b'a'))
    extend_verifier.verify(bytearray(b'a'), bytearray(b'b'), expected_result=bytearray(b'ab'))
    extend_verifier.verify(bytearray(b'ab'), bytearray(b'c'), expected_result=bytearray(b'abc'))
    extend_verifier.verify(bytearray(b'abc'), bytearray(b''), expected_result=bytearray(b'abc'))
    extend_verifier.verify(bytearray(b'abc'), bytearray(b'de'), expected_result=bytearray(b'abcde'))


def test_inplace_multiply():
    def multiply(tested: bytearray, item: int) -> bytearray:
        tested *= item
        return tested

    multiply_verifier = verifier_for(multiply)

    multiply_verifier.verify(bytearray(b'abc'), 1, expected_result=bytearray(b'abc'))
    multiply_verifier.verify(bytearray(b'ab'), 2, expected_result=bytearray(b'abab'))
    multiply_verifier.verify(bytearray(b'ab'), 3, expected_result=bytearray(b'ababab'))
    multiply_verifier.verify(bytearray(b'abc'), 0, expected_result=bytearray(b''))
    multiply_verifier.verify(bytearray(b'abc'), -1, expected_result=bytearray(b''))



def test_insert():
    def insert(tested: bytearray, index: int, item: int) -> bytearray:
        tested.insert(index, item)
        return tested

    insert_verifier = verifier_for(insert)

    insert_verifier.verify(bytearray(b''), 0, ord('a'), expected_result=bytearray(b'a'))
    insert_verifier.verify(bytearray(b'a'), 0, ord('b'), expected_result=bytearray(b'ba'))
    insert_verifier.verify(bytearray(b'a'), 1, ord('b'), expected_result=bytearray(b'ab'))
    insert_verifier.verify(bytearray(b'ab'), 0, ord('c'), expected_result=bytearray(b'cab'))
    insert_verifier.verify(bytearray(b'ab'), 1, ord('c'), expected_result=bytearray(b'acb'))
    insert_verifier.verify(bytearray(b'ab'), 2, ord('c'), expected_result=bytearray(b'abc'))
    insert_verifier.verify(bytearray(b'abc'), -1, ord('d'), expected_result=bytearray(b'abdc'))
    insert_verifier.verify(bytearray(b'abc'), -2, ord('d'), expected_result=bytearray(b'adbc'))
    insert_verifier.verify(bytearray(b'abc'), 3, ord('d'), expected_result=bytearray(b'abcd'))
    insert_verifier.verify(bytearray(b'abc'), 4, ord('d'), expected_result=bytearray(b'abcd'))
    insert_verifier.verify(bytearray(b'abc'), -4, ord('d'), expected_result=bytearray(b'dabc'))
    insert_verifier.verify(bytearray(b'abc'), -5, ord('d'), expected_result=bytearray(b'dabc'))


def test_pop():
    def pop(tested: bytearray) -> tuple:
        item = tested.pop()
        return item, tested

    pop_verifier = verifier_for(pop)

    pop_verifier.verify(bytearray(b'abc'), expected_result=(ord('c'), bytearray(b'ab')))
    pop_verifier.verify(bytearray(b'ab'), expected_result=(ord('b'), bytearray(b'a')))
    pop_verifier.verify(bytearray(b'a'), expected_result=(ord('a'), bytearray(b'')))

    pop_verifier.verify(bytearray(b'abe'), expected_result=(ord('e'), bytearray(b'ab')))

    pop_verifier.verify(bytearray(b''), expected_error=IndexError)


def test_pop_at_index():
    def pop_at_index(tested: bytearray, index: int) -> tuple:
        item = tested.pop(index)
        return item, tested

    pop_at_index_verifier = verifier_for(pop_at_index)

    pop_at_index_verifier.verify(bytearray(b'abc'), -1, expected_result=(ord('c'), bytearray(b'ab')))
    pop_at_index_verifier.verify(bytearray(b'ab'), -1, expected_result=(ord('b'), bytearray(b'a')))
    pop_at_index_verifier.verify(bytearray(b'a'), -1, expected_result=(ord('a'), bytearray(b'')))

    pop_at_index_verifier.verify(bytearray(b'abc'), 1, expected_result=(ord('b'), bytearray(b'ac')))
    pop_at_index_verifier.verify(bytearray(b'abc'), 0, expected_result=(ord('a'), bytearray(b'bc')))
    pop_at_index_verifier.verify(bytearray(b'abc'), 2, expected_result=(ord('c'), bytearray(b'ab')))
    pop_at_index_verifier.verify(bytearray(b'abc'), -2, expected_result=(ord('b'), bytearray(b'ac')))

    pop_at_index_verifier.verify(bytearray(b'abc'), -4, expected_error=IndexError)
    pop_at_index_verifier.verify(bytearray(b'abc'), 4, expected_error=IndexError)
    pop_at_index_verifier.verify(bytearray(b''), 0, expected_error=IndexError)


def test_remove():
    def remove(tested: bytearray, item: int) -> bytearray:
        tested.remove(item)
        return tested

    remove_verifier = verifier_for(remove)

    remove_verifier.verify(bytearray(b'abc'), ord('a'), expected_result=bytearray(b'bc'))
    remove_verifier.verify(bytearray(b'abc'), ord('b'), expected_result=bytearray(b'ac'))
    remove_verifier.verify(bytearray(b'abc'), ord('c'), expected_result=bytearray(b'ab'))

    remove_verifier.verify(bytearray(b'abe'), ord('b'), expected_result=bytearray(b'ae'))

    remove_verifier.verify(bytearray(b'abc'), ord('d'), expected_error=ValueError)
    remove_verifier.verify(bytearray(b''), ord('a'), expected_error=ValueError)


def test_reverse():
    def reverse(tested: bytearray) -> bytearray:
        tested.reverse()
        return tested

    reverse_verifier = verifier_for(reverse)

    reverse_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b'cba'))
    reverse_verifier.verify(bytearray(b'cba'), expected_result=bytearray(b'abc'))
    reverse_verifier.verify(bytearray(b'ab'), expected_result=bytearray(b'ba'))
    reverse_verifier.verify(bytearray(b'ba'), expected_result=bytearray(b'ab'))
    reverse_verifier.verify(bytearray(b'a'), expected_result=bytearray(b'a'))
    reverse_verifier.verify(bytearray(b''), expected_result=bytearray(b''))


########################################
# Bytearray operations
########################################
def test_interpolation():
    def interpolation(tested: bytearray, values: object) -> bytearray:  # noqa
        # IDE thinks bytearray __mod__ returns bytes, but in reality it returns bytearray
        return tested % values  # noqa

    interpolation_verifier = verifier_for(interpolation)

    interpolation_verifier.verify(bytearray(b'%d'), 100, expected_result=bytearray(b'100'))
    interpolation_verifier.verify(bytearray(b'%d'), 0b1111, expected_result=bytearray(b'15'))
    interpolation_verifier.verify(bytearray(b'%s'), bytearray(b'foo'), expected_result=bytearray(b'foo'))
    interpolation_verifier.verify(bytearray(b'%s %s'), (bytearray(b'foo'), bytearray(b'bar')),
                                  expected_result=bytearray(b'foo bar'))
    interpolation_verifier.verify(bytearray(b'%(foo)s'), {b'foo': bytearray(b'10'), b'bar': bytearray(b'20')},
                                  expected_result=bytearray(b'10'))

    interpolation_verifier.verify(bytearray(b'%d'), 101, expected_result=bytearray(b'101'))
    interpolation_verifier.verify(bytearray(b'%i'), 101, expected_result=bytearray(b'101'))

    interpolation_verifier.verify(bytearray(b'%o'), 27, expected_result=bytearray(b'33'))
    interpolation_verifier.verify(bytearray(b'%#o'), 27, expected_result=bytearray(b'0o33'))

    interpolation_verifier.verify(bytearray(b'%x'), 27, expected_result=bytearray(b'1b'))
    interpolation_verifier.verify(bytearray(b'%X'), 27, expected_result=bytearray(b'1B'))
    interpolation_verifier.verify(bytearray(b'%#x'), 27, expected_result=bytearray(b'0x1b'))
    interpolation_verifier.verify(bytearray(b'%#X'), 27, expected_result=bytearray(b'0X1B'))

    interpolation_verifier.verify(bytearray(b'%03d'), 1, expected_result=bytearray(b'001'))
    interpolation_verifier.verify(bytearray(b'%-5d'), 1, expected_result=bytearray(b'1    '))
    interpolation_verifier.verify(bytearray(b'%0-5d'), 1, expected_result=bytearray(b'1    '))

    interpolation_verifier.verify(bytearray(b'%d'), 1, expected_result=bytearray(b'1'))
    interpolation_verifier.verify(bytearray(b'%d'), -1, expected_result=bytearray(b'-1'))
    interpolation_verifier.verify(bytearray(b'% d'), 1, expected_result=bytearray(b' 1'))
    interpolation_verifier.verify(bytearray(b'% d'), -1, expected_result=bytearray(b'-1'))
    interpolation_verifier.verify(bytearray(b'%+d'), 1, expected_result=bytearray(b'+1'))
    interpolation_verifier.verify(bytearray(b'%+d'), -1, expected_result=bytearray(b'-1'))

    interpolation_verifier.verify(bytearray(b'%f'), 3.14, expected_result=bytearray(b'3.140000'))
    interpolation_verifier.verify(bytearray(b'%F'), 3.14, expected_result=bytearray(b'3.140000'))
    interpolation_verifier.verify(bytearray(b'%.1f'), 3.14, expected_result=bytearray(b'3.1'))
    interpolation_verifier.verify(bytearray(b'%.2f'), 3.14, expected_result=bytearray(b'3.14'))
    interpolation_verifier.verify(bytearray(b'%.3f'), 3.14, expected_result=bytearray(b'3.140'))

    interpolation_verifier.verify(bytearray(b'%g'), 1234567890, expected_result=bytearray(b'1.23457e+09'))
    interpolation_verifier.verify(bytearray(b'%G'), 1234567890, expected_result=bytearray(b'1.23457E+09'))
    interpolation_verifier.verify(bytearray(b'%e'), 1234567890, expected_result=bytearray(b'1.234568e+09'))
    interpolation_verifier.verify(bytearray(b'%E'), 1234567890, expected_result=bytearray(b'1.234568E+09'))

    interpolation_verifier.verify(bytearray(b'ABC %c'), 10, expected_result=bytearray(b'ABC \n'))
    interpolation_verifier.verify(bytearray(b'ABC %c'), 67, expected_result=bytearray(b'ABC C'))
    interpolation_verifier.verify(bytearray(b'ABC %c'), 68, expected_result=bytearray(b'ABC D'))
    interpolation_verifier.verify(bytearray(b'ABC %c'), bytearray(b'D'), expected_result=bytearray(b'ABC D'))
    interpolation_verifier.verify(bytearray(b'ABC %s'), bytearray(b'test'), expected_result=bytearray(b'ABC test'))
    interpolation_verifier.verify(bytearray(b'ABC %r'), bytearray(b'test'),
                                  expected_result=bytearray(b'ABC bytearray(b\'test\')'))

    interpolation_verifier.verify(bytearray(b'Give it %d%%!'), 100, expected_result=bytearray(b'Give it 100%!'))
    interpolation_verifier.verify(bytearray(b'Give it %(all-you-got)d%%!'), {b'all-you-got': 100},
                                  expected_result=bytearray(b'Give it 100%!'))


########################################
# Bytearray methods
########################################


def test_capitalize():
    def capitalize(tested: bytearray) -> bytearray:
        return tested.capitalize()

    capitalize_verifier = verifier_for(capitalize)

    capitalize_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    capitalize_verifier.verify(bytearray(b'test'), expected_result=bytearray(b'Test'))
    capitalize_verifier.verify(bytearray(b'TEST'), expected_result=bytearray(b'Test'))
    capitalize_verifier.verify(bytearray(b'hello world'), expected_result=bytearray(b'Hello world'))
    capitalize_verifier.verify(bytearray(b'Hello World'), expected_result=bytearray(b'Hello world'))
    capitalize_verifier.verify(bytearray(b'HELLO WORLD'), expected_result=bytearray(b'Hello world'))
    capitalize_verifier.verify(bytearray('π'.encode()), expected_result=bytearray('π'.encode()))


def test_center():
    def center(tested: bytearray, width: int) -> bytearray:
        return tested.center(width)

    def center_with_fill(tested: bytearray, width: int, fill: bytearray) -> bytearray:
        return tested.center(width, fill)

    center_verifier = verifier_for(center)
    center_with_fill_verifier = verifier_for(center_with_fill)

    center_verifier.verify(bytearray(b'test'), 10, expected_result=bytearray(b'   test   '))
    center_verifier.verify(bytearray(b'test'), 9, expected_result=bytearray(b'   test  '))
    center_verifier.verify(bytearray(b'test'), 4, expected_result=bytearray(b'test'))
    center_verifier.verify(bytearray(b'test'), 2, expected_result=bytearray(b'test'))

    center_with_fill_verifier.verify(bytearray(b'test'), 10, bytearray(b'#'), expected_result=bytearray(b'###test###'))
    center_with_fill_verifier.verify(bytearray(b'test'), 9, bytearray(b'#'), expected_result=bytearray(b'###test##'))
    center_with_fill_verifier.verify(bytearray(b'test'), 4, bytearray(b'#'), expected_result=bytearray(b'test'))
    center_with_fill_verifier.verify(bytearray(b'test'), 2, bytearray(b'#'), expected_result=bytearray(b'test'))


def test_count_byte():
    def count(tested: bytearray, item: int) -> int:
        return tested.count(item)

    def count_start(tested: bytearray, item: int, start: int) -> int:
        return tested.count(item, start)

    def count_start_end(tested: bytearray, item: int, start: int, end: int) -> int:
        return tested.count(item, start, end)

    count_verifier = verifier_for(count)
    count_from_start_verifier = verifier_for(count_start)
    count_between_verifier = verifier_for(count_start_end)

    count_verifier.verify(bytearray(b'abc'), ord(bytearray(b'a')), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), ord(bytearray(b'b')), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), ord(bytearray(b'c')), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), ord(bytearray(b'd')), expected_result=0)

    count_verifier.verify(bytearray(b'abca'), ord(bytearray(b'a')), expected_result=2)
    count_verifier.verify(bytearray(b'aaca'), ord(bytearray(b'a')), expected_result=3)
    count_verifier.verify(bytearray(b''), ord(bytearray(b'a')), expected_result=0)

    count_from_start_verifier.verify(bytearray(b'abc'), ord(bytearray(b'a')), 1, expected_result=0)
    count_from_start_verifier.verify(bytearray(b'abc'), ord(bytearray(b'b')), 1, expected_result=1)
    count_from_start_verifier.verify(bytearray(b'abc'), ord(bytearray(b'c')), 1, expected_result=1)
    count_from_start_verifier.verify(bytearray(b'abc'), ord(bytearray(b'd')), 1, expected_result=0)

    count_from_start_verifier.verify(bytearray(b'abca'), ord(bytearray(b'a')), 1, expected_result=1)
    count_from_start_verifier.verify(bytearray(b'aaca'), ord(bytearray(b'a')), 1, expected_result=2)
    count_from_start_verifier.verify(bytearray(b''), ord(bytearray(b'a')), 1, expected_result=0)

    count_between_verifier.verify(bytearray(b'abc'), ord(bytearray(b'a')), 1, 2, expected_result=0)
    count_between_verifier.verify(bytearray(b'abc'), ord(bytearray(b'b')), 1, 2, expected_result=1)
    count_between_verifier.verify(bytearray(b'abc'), ord(bytearray(b'c')), 1, 2, expected_result=0)
    count_between_verifier.verify(bytearray(b'abc'), ord(bytearray(b'd')), 1, 2, expected_result=0)

    count_between_verifier.verify(bytearray(b'abca'), ord(bytearray(b'a')), 1, 2, expected_result=0)
    count_between_verifier.verify(bytearray(b'abca'), ord(bytearray(b'a')), 1, 4, expected_result=1)
    count_between_verifier.verify(bytearray(b'abca'), ord(bytearray(b'a')), 0, 2, expected_result=1)
    count_between_verifier.verify(bytearray(b'aaca'), ord(bytearray(b'a')), 1, 2, expected_result=1)
    count_between_verifier.verify(bytearray(b''), ord(bytearray(b'a')), 1, 2, expected_result=0)


def test_endswith():
    def endswith(tested: bytearray, suffix: bytearray) -> bool:
        return tested.endswith(suffix)

    def endswith_start(tested: bytearray, suffix: bytearray, start: int) -> bool:
        return tested.endswith(suffix, start)

    def endswith_between(tested: bytearray, suffix: bytearray, start: int, end: int) -> bool:
        return tested.endswith(suffix, start, end)

    endswith_verifier = verifier_for(endswith)
    endswith_start_verifier = verifier_for(endswith_start)
    endswith_between_verifier = verifier_for(endswith_between)

    endswith_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), expected_result=True)
    endswith_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), expected_result=False)
    endswith_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), expected_result=False)
    endswith_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), expected_result=True)

    endswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 6, expected_result=True)
    endswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), 6, expected_result=False)
    endswith_start_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), 6, expected_result=False)
    endswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), 6, expected_result=False)

    endswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 6, 11, expected_result=True)
    endswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 7, 11, expected_result=False)
    endswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), 0, 5, expected_result=True)
    endswith_between_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), 0, 5, expected_result=False)
    endswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), 5, 11, expected_result=False)


def test_expandtabs():
    def expandtabs(tested: bytearray) -> bytearray:
        return tested.expandtabs()

    def expandtabs_with_tabsize(tested: bytearray, tabsize: int) -> bytearray:
        return tested.expandtabs(tabsize)

    expandtabs_verifier = verifier_for(expandtabs)
    expandtabs_with_tabsize_verifier = verifier_for(expandtabs_with_tabsize)

    expandtabs_verifier.verify(bytearray(b'01\t012\t0123\t01234'), expected_result=bytearray(b'01      012     0123    01234'))
    expandtabs_with_tabsize_verifier.verify(bytearray(b'01\t012\t0123\t01234'), 8, expected_result=bytearray(b'01      012     0123    01234'))
    expandtabs_with_tabsize_verifier.verify(bytearray(b'01\t012\t0123\t01234'), 4, expected_result=bytearray(b'01  012 0123    01234'))


def test_find():
    def find(tested: bytearray, item: bytearray) -> int:
        return tested.find(item)

    def find_start_verifier(tested: bytearray, item: bytearray, start: int) -> int:
        return tested.find(item, start)

    def find_start_end_verifier(tested: bytearray, item: bytearray, start: int, end: int) -> int:
        return tested.find(item, start, end)

    find_verifier = verifier_for(find)
    find_start_verifier = verifier_for(find_start_verifier)
    find_start_end_verifier = verifier_for(find_start_end_verifier)

    find_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), expected_result=0)
    find_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), expected_result=1)
    find_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), expected_result=-1)

    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, expected_result=3)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 5, expected_result=-1)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, expected_result=1)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, expected_result=2)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, expected_result=-1)

    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -3, expected_result=3)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, expected_result=4)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, expected_result=5)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, expected_result=-1)

    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, 2, expected_result=-1)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, 2, expected_result=1)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, 2, expected_result=-1)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, 2, expected_result=-1)

    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -2, -1, expected_result=-1)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, -1, expected_result=4)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, -1, expected_result=-1)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, -1, expected_result=-1)


def test_isalnum():
    def isalnum(tested: bytearray) -> bool:
        return tested.isalnum()

    isalnum_verifier = verifier_for(isalnum)

    isalnum_verifier.verify(bytearray(b''), expected_result=False)
    isalnum_verifier.verify(bytearray(b'abc'), expected_result=True)
    isalnum_verifier.verify(bytearray(b'ABC'), expected_result=True)
    isalnum_verifier.verify(bytearray(b'123'), expected_result=True)
    isalnum_verifier.verify(bytearray(b'ABC123'), expected_result=True)
    isalnum_verifier.verify(bytearray(b'+'), expected_result=False)
    isalnum_verifier.verify(bytearray(b'[]'), expected_result=False)
    isalnum_verifier.verify(bytearray(b'-'), expected_result=False)
    isalnum_verifier.verify(bytearray(b'%'), expected_result=False)
    isalnum_verifier.verify(bytearray(b'\n'), expected_result=False)
    isalnum_verifier.verify(bytearray(b'\t'), expected_result=False)
    isalnum_verifier.verify(bytearray(b' '), expected_result=False)


def test_isalpha():
    def isalpha(tested: bytearray) -> bool:
        return tested.isalpha()

    isalpha_verifier = verifier_for(isalpha)

    isalpha_verifier.verify(bytearray(b''), expected_result=False)
    isalpha_verifier.verify(bytearray(b'abc'), expected_result=True)
    isalpha_verifier.verify(bytearray(b'ABC'), expected_result=True)
    isalpha_verifier.verify(bytearray(b'123'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'ABC123'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'+'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'[]'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'-'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'%'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'\n'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'\t'), expected_result=False)
    isalpha_verifier.verify(bytearray(b' '), expected_result=False)


def test_isascii():
    def isascii(tested: bytearray) -> bool:
        return tested.isascii()

    isascii_verifier = verifier_for(isascii)

    isascii_verifier.verify(bytearray(b''), expected_result=True)
    isascii_verifier.verify(bytearray(b'abc'), expected_result=True)
    isascii_verifier.verify(bytearray(b'ABC'), expected_result=True)
    isascii_verifier.verify(bytearray(b'123'), expected_result=True)
    isascii_verifier.verify(bytearray(b'ABC123'), expected_result=True)
    isascii_verifier.verify(bytearray(b'+'), expected_result=True)
    isascii_verifier.verify(bytearray(b'[]'), expected_result=True)
    isascii_verifier.verify(bytearray(b'-'), expected_result=True)
    isascii_verifier.verify(bytearray(b'%'), expected_result=True)
    isascii_verifier.verify(bytearray(b'\n'), expected_result=True)
    isascii_verifier.verify(bytearray(b'\t'), expected_result=True)
    isascii_verifier.verify(bytearray(b' '), expected_result=True)


def test_isdigit():
    def isdigit(tested: bytearray) -> bool:
        return tested.isdigit()

    isdigit_verifier = verifier_for(isdigit)

    isdigit_verifier.verify(bytearray(b''), expected_result=False)
    isdigit_verifier.verify(bytearray(b'abc'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'ABC'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'123'), expected_result=True)
    isdigit_verifier.verify(bytearray(b'ABC123'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'+'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'[]'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'-'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'%'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'\n'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'\t'), expected_result=False)
    isdigit_verifier.verify(bytearray(b' '), expected_result=False)


def test_islower():
    def islower(tested: bytearray) -> bool:
        return tested.islower()

    islower_verifier = verifier_for(islower)

    islower_verifier.verify(bytearray(b''), expected_result=False)
    islower_verifier.verify(bytearray(b'abc'), expected_result=True)
    islower_verifier.verify(bytearray(b'ABC'), expected_result=False)
    islower_verifier.verify(bytearray(b'123'), expected_result=False)
    islower_verifier.verify(bytearray(b'ABC123'), expected_result=False)
    islower_verifier.verify(bytearray(b'+'), expected_result=False)
    islower_verifier.verify(bytearray(b'[]'), expected_result=False)
    islower_verifier.verify(bytearray(b'-'), expected_result=False)
    islower_verifier.verify(bytearray(b'%'), expected_result=False)
    islower_verifier.verify(bytearray(b'\n'), expected_result=False)
    islower_verifier.verify(bytearray(b'\t'), expected_result=False)
    islower_verifier.verify(bytearray(b' '), expected_result=False)


def test_isspace():
    def isspace(tested: bytearray) -> bool:
        return tested.isspace()

    isspace_verifier = verifier_for(isspace)

    isspace_verifier.verify(bytearray(b''), expected_result=False)
    isspace_verifier.verify(bytearray(b'abc'), expected_result=False)
    isspace_verifier.verify(bytearray(b'ABC'), expected_result=False)
    isspace_verifier.verify(bytearray(b'123'), expected_result=False)
    isspace_verifier.verify(bytearray(b'ABC123'), expected_result=False)
    isspace_verifier.verify(bytearray(b'+'), expected_result=False)
    isspace_verifier.verify(bytearray(b'[]'), expected_result=False)
    isspace_verifier.verify(bytearray(b'-'), expected_result=False)
    isspace_verifier.verify(bytearray(b'%'), expected_result=False)
    isspace_verifier.verify(bytearray(b'\n'), expected_result=True)
    isspace_verifier.verify(bytearray(b'\t'), expected_result=True)
    isspace_verifier.verify(bytearray(b' '), expected_result=True)


def test_istitle():
    def istitle(tested: bytearray) -> bool:
        return tested.istitle()

    istitle_verifier = verifier_for(istitle)

    istitle_verifier.verify(bytearray(b''), expected_result=False)

    istitle_verifier.verify(bytearray(b'Abc'), expected_result=True)
    istitle_verifier.verify(bytearray(b'The Title'), expected_result=True)
    istitle_verifier.verify(bytearray(b'The title'), expected_result=False)

    istitle_verifier.verify(bytearray(b'abc'), expected_result=False)
    istitle_verifier.verify(bytearray(b'ABC'), expected_result=False)
    istitle_verifier.verify(bytearray(b'123'), expected_result=False)
    istitle_verifier.verify(bytearray(b'ABC123'), expected_result=False)
    istitle_verifier.verify(bytearray(b'+'), expected_result=False)
    istitle_verifier.verify(bytearray(b'[]'), expected_result=False)
    istitle_verifier.verify(bytearray(b'-'), expected_result=False)
    istitle_verifier.verify(bytearray(b'%'), expected_result=False)
    istitle_verifier.verify(bytearray(b'\n'), expected_result=False)
    istitle_verifier.verify(bytearray(b'\t'), expected_result=False)
    istitle_verifier.verify(bytearray(b' '), expected_result=False)


def test_isupper():
    def isupper(tested: bytearray) -> bool:
        return tested.isupper()

    isupper_verifier = verifier_for(isupper)

    isupper_verifier.verify(bytearray(b''), expected_result=False)
    isupper_verifier.verify(bytearray(b'abc'), expected_result=False)
    isupper_verifier.verify(bytearray(b'ABC'), expected_result=True)
    isupper_verifier.verify(bytearray(b'123'), expected_result=False)
    isupper_verifier.verify(bytearray(b'ABC123'), expected_result=True)
    isupper_verifier.verify(bytearray(b'+'), expected_result=False)
    isupper_verifier.verify(bytearray(b'[]'), expected_result=False)
    isupper_verifier.verify(bytearray(b'-'), expected_result=False)
    isupper_verifier.verify(bytearray(b'%'), expected_result=False)
    isupper_verifier.verify(bytearray(b'\n'), expected_result=False)
    isupper_verifier.verify(bytearray(b'\t'), expected_result=False)
    isupper_verifier.verify(bytearray(b' '), expected_result=False)


def test_join():
    def join(tested: bytearray, iterable: list) -> bytearray:
        return tested.join(iterable)

    join_verifier = verifier_for(join)

    join_verifier.verify(bytearray(b', '), [], expected_result=bytearray(b''))
    join_verifier.verify(bytearray(b', '), [bytearray(b'a')], expected_result=bytearray(b'a'))
    join_verifier.verify(bytearray(b', '), [bytearray(b'a'), bytearray(b'b')], expected_result=bytearray(b'a, b'))
    join_verifier.verify(bytearray(b' '), [bytearray(b'hello'), bytearray(b'world'), bytearray(b'again')], expected_result=bytearray(b'hello world again'))
    join_verifier.verify(bytearray(b'\n'), [bytearray(b'1'), bytearray(b'2'), bytearray(b'3')], expected_result=bytearray(b'1\n2\n3'))
    join_verifier.verify(bytearray(b', '), [1, 2], expected_error=TypeError)


def test_ljust():
    def ljust(tested: bytearray, width: int) -> bytearray:
        return tested.ljust(width)

    def ljust_with_fill(tested: bytearray, width: int, fill: bytearray) -> bytearray:
        return tested.ljust(width, fill)

    ljust_verifier = verifier_for(ljust)
    ljust_with_fill_verifier = verifier_for(ljust_with_fill)

    ljust_verifier.verify(bytearray(b'test'), 10, expected_result=bytearray(b'test      '))
    ljust_verifier.verify(bytearray(b'test'), 9, expected_result=bytearray(b'test     '))
    ljust_verifier.verify(bytearray(b'test'), 4, expected_result=bytearray(b'test'))
    ljust_verifier.verify(bytearray(b'test'), 2, expected_result=bytearray(b'test'))

    ljust_with_fill_verifier.verify(bytearray(b'test'), 10, bytearray(b'#'), expected_result=bytearray(b'test######'))
    ljust_with_fill_verifier.verify(bytearray(b'test'), 9, bytearray(b'#'), expected_result=bytearray(b'test#####'))
    ljust_with_fill_verifier.verify(bytearray(b'test'), 4, bytearray(b'#'), expected_result=bytearray(b'test'))
    ljust_with_fill_verifier.verify(bytearray(b'test'), 2, bytearray(b'#'), expected_result=bytearray(b'test'))


def test_lower():
    def lower(tested: bytearray) -> bytearray:
        return tested.lower()

    lower_verifier = verifier_for(lower)

    lower_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    lower_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b'abc'))
    lower_verifier.verify(bytearray(b'ABC'), expected_result=bytearray(b'abc'))
    lower_verifier.verify(bytearray(b'123'), expected_result=bytearray(b'123'))
    lower_verifier.verify(bytearray(b'ABC123'), expected_result=bytearray(b'abc123'))
    lower_verifier.verify(bytearray(b'+'), expected_result=bytearray(b'+'))
    lower_verifier.verify(bytearray(b'[]'), expected_result=bytearray(b'[]'))
    lower_verifier.verify(bytearray(b'-'), expected_result=bytearray(b'-'))
    lower_verifier.verify(bytearray(b'%'), expected_result=bytearray(b'%'))
    lower_verifier.verify(bytearray('π'.encode()), expected_result=bytearray('π'.encode()))
    lower_verifier.verify(bytearray(b'\n'), expected_result=bytearray(b'\n'))
    lower_verifier.verify(bytearray(b'\t'), expected_result=bytearray(b'\t'))
    lower_verifier.verify(bytearray(b' '), expected_result=bytearray(b' '))


def test_lstrip():
    def lstrip(tested: bytearray) -> bytearray:
        return tested.lstrip()

    def lstrip_with_chars(tested: bytearray, chars: bytearray) -> bytearray:
        return tested.lstrip(chars)

    lstrip_verifier = verifier_for(lstrip)
    lstrip_with_chars_verifier = verifier_for(lstrip_with_chars)

    lstrip_verifier.verify(bytearray(b'   spacious   '), expected_result=bytearray(b'spacious   '))
    lstrip_with_chars_verifier.verify(bytearray(b'www.example.com'), bytearray(b'cmowz.'), expected_result=bytearray(b'example.com'))


def test_partition():
    def partition(tested: bytearray, sep: bytearray) -> tuple:
        return tested.partition(sep)

    partition_verifier = verifier_for(partition)

    partition_verifier.verify(bytearray(b'before+after+extra'), bytearray(b'+'), expected_result=(bytearray(b'before'), bytearray(b'+'), bytearray(b'after+extra')))
    partition_verifier.verify(bytearray(b'before and after and extra'), bytearray(b'+'), expected_result=(bytearray(b'before and after and extra'),
                                                                                    bytearray(b''), bytearray(b'')))
    partition_verifier.verify(bytearray(b'before and after and extra'), bytearray(b' and '),
                              expected_result=(bytearray(b'before'), bytearray(b' and '), bytearray(b'after and extra')))
    partition_verifier.verify(bytearray(b'before+after+extra'), bytearray(b' and '), expected_result=(bytearray(b'before+after+extra'), bytearray(b''), bytearray(b'')))


def test_removeprefix():
    def removeprefix(tested: bytearray, prefix: bytearray) -> bytearray:
        return tested.removeprefix(prefix)

    removeprefix_verifier = verifier_for(removeprefix)

    removeprefix_verifier.verify(bytearray(b'TestHook'), bytearray(b'Test'), expected_result=bytearray(b'Hook'))
    removeprefix_verifier.verify(bytearray(b'BaseTestCase'), bytearray(b'Test'), expected_result=bytearray(b'BaseTestCase'))
    removeprefix_verifier.verify(bytearray(b'BaseCaseTest'), bytearray(b'Test'), expected_result=bytearray(b'BaseCaseTest'))
    removeprefix_verifier.verify(bytearray(b'BaseCase'), bytearray(b'Test'), expected_result=bytearray(b'BaseCase'))


def test_removesuffix():
    def removesuffix(tested: bytearray, prefix: bytearray) -> bytearray:
        return tested.removesuffix(prefix)

    removesuffix_verifier = verifier_for(removesuffix)

    removesuffix_verifier.verify(bytearray(b'MiscTests'), bytearray(b'Tests'), expected_result=bytearray(b'Misc'))
    removesuffix_verifier.verify(bytearray(b'TmpTestsDirMixin'), bytearray(b'Tests'), expected_result=bytearray(b'TmpTestsDirMixin'))
    removesuffix_verifier.verify(bytearray(b'TestsTmpDirMixin'), bytearray(b'Tests'), expected_result=bytearray(b'TestsTmpDirMixin'))
    removesuffix_verifier.verify(bytearray(b'TmpDirMixin'), bytearray(b'Tests'), expected_result=bytearray(b'TmpDirMixin'))


def test_replace():
    def replace(tested: bytearray, substring: bytearray, replacement: bytearray) -> bytearray:
        return tested.replace(substring, replacement)

    def replace_with_count(tested: bytearray, substring: bytearray, replacement: bytearray, count: int) -> bytearray:
        return tested.replace(substring, replacement, count)


    replace_verifier = verifier_for(replace)
    replace_with_count_verifier = verifier_for(replace_with_count)

    replace_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'),
                            expected_result=bytearray(b'all dogs, including the dog Aldogo, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), 0,
                                        expected_result=bytearray(b'all cats, including the cat Alcato, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), 1,
                                       expected_result=bytearray(b'all dogs, including the cat Alcato, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), 2,
                                       expected_result=bytearray(b'all dogs, including the dog Alcato, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), 3,
                                       expected_result=bytearray(b'all dogs, including the dog Aldogo, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), 4,
                                       expected_result=bytearray(b'all dogs, including the dog Aldogo, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), -1,
                                       expected_result=bytearray(b'all dogs, including the dog Aldogo, are animals'))


def test_rfind():
    def rfind(tested: bytearray, item: bytearray) -> int:
        return tested.rfind(item)

    def rfind_start_verifier(tested: bytearray, item: bytearray, start: int) -> int:
        return tested.rfind(item, start)

    def rfind_start_end_verifier(tested: bytearray, item: bytearray, start: int, end: int) -> int:
        return tested.rfind(item, start, end)

    rfind_verifier = verifier_for(rfind)
    rfind_start_verifier = verifier_for(rfind_start_verifier)
    rfind_start_end_verifier = verifier_for(rfind_start_end_verifier)

    rfind_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), expected_result=3)
    rfind_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), expected_result=4)
    rfind_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), expected_result=-1)

    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, expected_result=3)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 5, expected_result=-1)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, expected_result=4)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, expected_result=5)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, expected_result=-1)

    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -3, expected_result=3)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, expected_result=4)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, expected_result=5)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, expected_result=-1)

    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, 2, expected_result=-1)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, 2, expected_result=1)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, 2, expected_result=-1)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, 2, expected_result=-1)

    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -2, -1, expected_result=-1)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, -1, expected_result=4)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, -1, expected_result=-1)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, -1, expected_result=-1)


def test_rindex():
    def rindex(tested: bytearray, item: bytearray) -> int:
        return tested.rindex(item)

    def rindex_start_verifier(tested: bytearray, item: bytearray, start: int) -> int:
        return tested.rindex(item, start)

    def rindex_start_end_verifier(tested: bytearray, item: bytearray, start: int, end: int) -> int:
        return tested.rindex(item, start, end)

    rindex_verifier = verifier_for(rindex)
    rindex_start_verifier = verifier_for(rindex_start_verifier)
    rindex_start_end_verifier = verifier_for(rindex_start_end_verifier)

    rindex_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), expected_result=3)
    rindex_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), expected_result=4)
    rindex_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), expected_error=ValueError)

    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, expected_result=3)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 5, expected_error=ValueError)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, expected_result=4)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, expected_result=5)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, expected_error=ValueError)

    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -3, expected_result=3)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, expected_result=4)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, expected_result=5)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, expected_error=ValueError)

    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, 2, expected_error=ValueError)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, 2, expected_result=1)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, 2, expected_error=ValueError)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, 2, expected_error=ValueError)

    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -2, -1, expected_error=ValueError)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, -1, expected_result=4)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, -1, expected_error=ValueError)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, -1, expected_error=ValueError)

def test_rjust():
    def rjust(tested: bytearray, width: int) -> bytearray:
        return tested.rjust(width)

    def rjust_with_fill(tested: bytearray, width: int, fill: bytearray) -> bytearray:
        return tested.rjust(width, fill)

    rjust_verifier = verifier_for(rjust)
    rjust_with_fill_verifier = verifier_for(rjust_with_fill)

    rjust_verifier.verify(bytearray(b'test'), 10, expected_result=bytearray(b'      test'))
    rjust_verifier.verify(bytearray(b'test'), 9, expected_result=bytearray(b'     test'))
    rjust_verifier.verify(bytearray(b'test'), 4, expected_result=bytearray(b'test'))
    rjust_verifier.verify(bytearray(b'test'), 2, expected_result=bytearray(b'test'))

    rjust_with_fill_verifier.verify(bytearray(b'test'), 10, bytearray(b'#'), expected_result=bytearray(b'######test'))
    rjust_with_fill_verifier.verify(bytearray(b'test'), 9, bytearray(b'#'), expected_result=bytearray(b'#####test'))
    rjust_with_fill_verifier.verify(bytearray(b'test'), 4, bytearray(b'#'), expected_result=bytearray(b'test'))
    rjust_with_fill_verifier.verify(bytearray(b'test'), 2, bytearray(b'#'), expected_result=bytearray(b'test'))


def test_rpartition():
    def rpartition(tested: bytearray, sep: bytearray) -> tuple:
        return tested.rpartition(sep)

    rpartition_verifier = verifier_for(rpartition)

    rpartition_verifier.verify(bytearray(b'before+after+extra'), bytearray(b'+'), expected_result=(bytearray(b'before+after'), bytearray(b'+'), bytearray(b'extra')))
    rpartition_verifier.verify(bytearray(b'before and after and extra'), bytearray(b'+'), expected_result=(bytearray(b''), bytearray(b''),
                                                                                     bytearray(b'before and after and extra')))
    rpartition_verifier.verify(bytearray(b'before and after and extra'), bytearray(b' and '),
                               expected_result=(bytearray(b'before and after'), bytearray(b' and '), bytearray(b'extra')))
    rpartition_verifier.verify(bytearray(b'before+after+extra'), bytearray(b' and '), expected_result=(bytearray(b''), bytearray(b''), bytearray(b'before+after+extra')))


def test_rsplit():
    def rsplit(tested: bytearray) -> list:
        return tested.rsplit()

    def rsplit_with_sep(tested: bytearray, sep: bytearray) -> list:
        return tested.rsplit(sep)

    def rsplit_with_sep_and_count(tested: bytearray, sep: bytearray, count: int) -> list:
        return tested.rsplit(sep, count)

    rsplit_verifier = verifier_for(rsplit)
    rsplit_with_sep_verifier = verifier_for(rsplit_with_sep)
    rsplit_with_sep_and_count_verifier = verifier_for(rsplit_with_sep_and_count)

    rsplit_verifier.verify(bytearray(b'123'), expected_result=[bytearray(b'123')])
    rsplit_verifier.verify(bytearray(b'1 2 3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    rsplit_verifier.verify(bytearray(b' 1 2 3 '), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    rsplit_verifier.verify(bytearray(b'1\n2\n3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    rsplit_verifier.verify(bytearray(b'1\t2\t3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])

    rsplit_with_sep_verifier.verify(bytearray(b'1,2,3'), bytearray(b','), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    rsplit_with_sep_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b''), bytearray(b'3'), bytearray(b'')])
    rsplit_with_sep_verifier.verify(bytearray(b',1,2,,3,'), bytearray(b','), expected_result=[bytearray(b''), bytearray(b'1'), bytearray(b'2'), bytearray(b''), bytearray(b'3'), bytearray(b'')])

    rsplit_with_sep_and_count_verifier.verify(bytearray(b'1,2,3'), bytearray(b','), 1, expected_result=[bytearray(b'1,2'), bytearray(b'3')])
    rsplit_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 1, expected_result=[bytearray(b'1,2,,3'), bytearray(b'')])
    rsplit_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 2, expected_result=[bytearray(b'1,2,'), bytearray(b'3'), bytearray(b'')])
    rsplit_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 3, expected_result=[bytearray(b'1,2'), bytearray(b''),  bytearray(b'3'), bytearray(b'')])
    rsplit_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 4, expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b''),  bytearray(b'3'), bytearray(b'')])


def test_rstrip():
    def rstrip(tested: bytearray) -> bytearray:
        return tested.rstrip()

    def rstrip_with_chars(tested: bytearray, chars: bytearray) -> bytearray:
        return tested.rstrip(chars)

    rstrip_verifier = verifier_for(rstrip)
    rstrip_with_chars_verifier = verifier_for(rstrip_with_chars)

    rstrip_verifier.verify(bytearray(b'   spacious   '), expected_result=bytearray(b'   spacious'))
    rstrip_with_chars_verifier.verify(bytearray(b'www.example.com'), bytearray(b'cmowz.'), expected_result=bytearray(b'www.example'))



def test_split():
    def split(tested: bytearray) -> list:
        return tested.split()

    def split_with_sep(tested: bytearray, sep: bytearray) -> list:
        return tested.split(sep)

    def split_with_sep_and_count(tested: bytearray, sep: bytearray, count: int) -> list:
        return tested.split(sep, count)

    split_verifier = verifier_for(split)
    split_with_sep_verifier = verifier_for(split_with_sep)
    split_with_sep_and_count_verifier = verifier_for(split_with_sep_and_count)

    split_verifier.verify(bytearray(b'123'), expected_result=[bytearray(b'123')])
    split_verifier.verify(bytearray(b'1 2 3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    split_verifier.verify(bytearray(b' 1 2 3 '), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    split_verifier.verify(bytearray(b'1\n2\n3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    split_verifier.verify(bytearray(b'1\t2\t3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])

    split_with_sep_verifier.verify(bytearray(b'1,2,3'), bytearray(b','), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    split_with_sep_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b''), bytearray(b'3'), bytearray(b'')])
    split_with_sep_verifier.verify(bytearray(b',1,2,,3,'), bytearray(b','), expected_result=[bytearray(b''), bytearray(b'1'), bytearray(b'2'), bytearray(b''), bytearray(b'3'), bytearray(b'')])

    split_with_sep_and_count_verifier.verify(bytearray(b'1,2,3'), bytearray(b','), 1, expected_result=[bytearray(b'1'), bytearray(b'2,3')])
    split_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 1, expected_result=[bytearray(b'1'), bytearray(b'2,,3,')])
    split_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 2, expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b',3,')])
    split_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 3, expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b''),  bytearray(b'3,')])
    split_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 4, expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b''),  bytearray(b'3'), bytearray(b'')])


def test_splitlines():
    def splitlines(tested: bytearray) -> list:
        return tested.splitlines()

    def splitlines_keep_ends(tested: bytearray, keep_ends: bool) -> list:
        return tested.splitlines(keep_ends)

    splitlines_verifier = verifier_for(splitlines)
    splitlines_keep_ends_verifier = verifier_for(splitlines_keep_ends)

    splitlines_verifier.verify(bytearray(b'ab c\n\nde fg\rkl\r\n'), expected_result=[bytearray(b'ab c'), bytearray(b''), bytearray(b'de fg'), bytearray(b'kl')])
    splitlines_verifier.verify(bytearray(b''), expected_result=[])
    splitlines_verifier.verify(bytearray(b'One line\n'), expected_result=[bytearray(b'One line')])

    splitlines_keep_ends_verifier.verify(bytearray(b'ab c\n\nde fg\rkl\r\n'), False, expected_result=[bytearray(b'ab c'), bytearray(b''), bytearray(b'de fg'), bytearray(b'kl')])
    splitlines_keep_ends_verifier.verify(bytearray(b'ab c\n\nde fg\rkl\r\n'), True,
                                         expected_result=[bytearray(b'ab c\n'), bytearray(b'\n'), bytearray(b'de fg\r'), bytearray(b'kl\r\n')])
    splitlines_keep_ends_verifier.verify(bytearray(b''), True, expected_result=[])
    splitlines_keep_ends_verifier.verify(bytearray(b''), False, expected_result=[])
    splitlines_keep_ends_verifier.verify(bytearray(b'One line\n'), True, expected_result=[bytearray(b'One line\n')])
    splitlines_keep_ends_verifier.verify(bytearray(b'One line\n'), False, expected_result=[bytearray(b'One line')])


def test_startswith():
    def startswith(tested: bytearray, suffix: bytearray) -> bool:
        return tested.startswith(suffix)

    def startswith_start(tested: bytearray, suffix: bytearray, start: int) -> bool:
        return tested.startswith(suffix, start)

    def startswith_between(tested: bytearray, suffix: bytearray, start: int, end: int) -> bool:
        return tested.startswith(suffix, start, end)

    startswith_verifier = verifier_for(startswith)
    startswith_start_verifier = verifier_for(startswith_start)
    startswith_between_verifier = verifier_for(startswith_between)

    startswith_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), expected_result=True)
    startswith_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), expected_result=False)
    startswith_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), expected_result=False)
    startswith_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), expected_result=True)

    startswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 6, expected_result=True)
    startswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), 6, expected_result=False)
    startswith_start_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), 6, expected_result=False)
    startswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), 6, expected_result=False)

    startswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 6, 11, expected_result=True)
    startswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 7, 11, expected_result=False)
    startswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), 0, 5, expected_result=True)
    startswith_between_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), 0, 5, expected_result=False)
    startswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), 5, 11, expected_result=False)


def test_strip():
    def strip(tested: bytearray) -> bytearray:
        return tested.strip()

    def strip_with_chars(tested: bytearray, chars: bytearray) -> bytearray:
        return tested.strip(chars)

    strip_verifier = verifier_for(strip)
    strip_with_chars_verifier = verifier_for(strip_with_chars)

    strip_verifier.verify(bytearray(b'   spacious   '), expected_result=bytearray(b'spacious'))
    strip_with_chars_verifier.verify(bytearray(b'www.example.com'), bytearray(b'cmowz.'), expected_result=bytearray(b'example'))


def test_swapcase():
    def swapcase(tested: bytearray) -> bytearray:
        return tested.swapcase()

    swapcase_verifier = verifier_for(swapcase)

    swapcase_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    swapcase_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b'ABC'))
    swapcase_verifier.verify(bytearray(b'ABC'), expected_result=bytearray(b'abc'))
    swapcase_verifier.verify(bytearray(b'123'), expected_result=bytearray(b'123'))
    swapcase_verifier.verify(bytearray(b'ABC123'), expected_result=bytearray(b'abc123'))
    swapcase_verifier.verify(bytearray(b'+'), expected_result=bytearray(b'+'))
    swapcase_verifier.verify(bytearray(b'[]'), expected_result=bytearray(b'[]'))
    swapcase_verifier.verify(bytearray(b'-'), expected_result=bytearray(b'-'))
    swapcase_verifier.verify(bytearray(b'%'), expected_result=bytearray(b'%'))
    swapcase_verifier.verify(bytearray('π'.encode()), expected_result=bytearray('π'.encode()))
    swapcase_verifier.verify(bytearray(b'\n'), expected_result=bytearray(b'\n'))
    swapcase_verifier.verify(bytearray(b'\t'), expected_result=bytearray(b'\t'))
    swapcase_verifier.verify(bytearray(b' '), expected_result=bytearray(b' '))


def test_title():
    def title(tested: bytearray) -> bytearray:
        return tested.title()

    title_verifier = verifier_for(title)

    title_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    title_verifier.verify(bytearray(b'Hello world'), expected_result=bytearray(b'Hello World'))
    title_verifier.verify(bytearray(b"they're bill's friends from the UK"),
                              expected_result=bytearray(b"They'Re Bill'S Friends From The Uk"))

    title_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b'Abc'))
    title_verifier.verify(bytearray(b'ABC'), expected_result=bytearray(b'Abc'))
    title_verifier.verify(bytearray(b'123'), expected_result=bytearray(b'123'))
    title_verifier.verify(bytearray(b'ABC123'), expected_result=bytearray(b'Abc123'))
    title_verifier.verify(bytearray(b'+'), expected_result=bytearray(b'+'))
    title_verifier.verify(bytearray(b'[]'), expected_result=bytearray(b'[]'))
    title_verifier.verify(bytearray(b'-'), expected_result=bytearray(b'-'))
    title_verifier.verify(bytearray(b'%'), expected_result=bytearray(b'%'))
    title_verifier.verify(bytearray('π'.encode()), expected_result=bytearray('π'.encode()))
    title_verifier.verify(bytearray(b'\n'), expected_result=bytearray(b'\n'))
    title_verifier.verify(bytearray(b'\t'), expected_result=bytearray(b'\t'))
    title_verifier.verify(bytearray(b' '), expected_result=bytearray(b' '))


def test_translate():
    def translate(tested: bytearray, mapping: bytearray) -> bytearray:
        return tested.translate(mapping)

    translate_verifier = verifier_for(translate)

    mapping = bytearray(b'').join([bytes([(i + 1) % 256]) for i in range(256)])

    translate_verifier.verify(bytearray(b'hello world'),
                              mapping, expected_result=bytearray(b'ifmmp!xpsme'))


def test_upper():
    def upper(tested: bytearray) -> bytearray:
        return tested.upper()

    upper_verifier = verifier_for(upper)

    upper_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    upper_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b'ABC'))
    upper_verifier.verify(bytearray(b'ABC'), expected_result=bytearray(b'ABC'))
    upper_verifier.verify(bytearray(b'123'), expected_result=bytearray(b'123'))
    upper_verifier.verify(bytearray(b'ABC123'), expected_result=bytearray(b'ABC123'))
    upper_verifier.verify(bytearray(b'+'), expected_result=bytearray(b'+'))
    upper_verifier.verify(bytearray(b'[]'), expected_result=bytearray(b'[]'))
    upper_verifier.verify(bytearray(b'-'), expected_result=bytearray(b'-'))
    upper_verifier.verify(bytearray(b'%'), expected_result=bytearray(b'%'))
    upper_verifier.verify(bytearray(b'\n'), expected_result=bytearray(b'\n'))
    upper_verifier.verify(bytearray(b'\t'), expected_result=bytearray(b'\t'))
    upper_verifier.verify(bytearray(b' '), expected_result=bytearray(b' '))


def test_zfill():
    def zfill(tested: bytearray, padding: int) -> bytearray:
        return tested.zfill(padding)

    zfill_verifier = verifier_for(zfill)

    zfill_verifier.verify(bytearray(b'42'), 5, expected_result=bytearray(b'00042'))
    zfill_verifier.verify(bytearray(b'-42'), 5, expected_result=bytearray(b'-0042'))
    zfill_verifier.verify(bytearray(b'+42'), 5, expected_result=bytearray(b'+0042'))
    zfill_verifier.verify(bytearray(b'42'), 1, expected_result=bytearray(b'42'))
    zfill_verifier.verify(bytearray(b'-42'), 1, expected_result=bytearray(b'-42'))
    zfill_verifier.verify(bytearray(b'+42'), 1, expected_result=bytearray(b'+42'))
    zfill_verifier.verify(bytearray(b'abc'), 10, expected_result=bytearray(b'0000000abc'))
