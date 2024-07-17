from .conftest import verifier_for
from typing import Union


def test_membership():
    def membership(tested: list, x: object) -> bool:
        return x in tested

    def not_membership(tested: list, x: object) -> bool:
        return x not in tested

    membership_verifier = verifier_for(membership)
    not_membership_verifier = verifier_for(not_membership)

    membership_verifier.verify([1, 2, 3], 1, expected_result=True)
    not_membership_verifier.verify([1, 2, 3], 1, expected_result=False)

    membership_verifier.verify([1, 2, 3], 4, expected_result=False)
    not_membership_verifier.verify([1, 2, 3], 4, expected_result=True)

    membership_verifier.verify([1, 2, 3], 3, expected_result=True)
    not_membership_verifier.verify([1, 2, 3], 3, expected_result=False)


def test_concat():
    def concat(x: list, y: list) -> tuple:
        out = x + y
        return out, out is x, out is y

    concat_verifier = verifier_for(concat)

    concat_verifier.verify([1, 2], [3, 4], expected_result=([1, 2, 3, 4], False, False))
    concat_verifier.verify([], [1, 2, 3], expected_result=([1, 2, 3], False, False))
    concat_verifier.verify([1, 2, 3], [], expected_result=([1, 2, 3], False, False))
    concat_verifier.verify([3], [2, 1], expected_result=([3, 2, 1], False, False))


def test_repeat():
    def left_repeat(x: list, y: int) -> tuple:
        out = x * y
        return out, out is x, out is y

    def right_repeat(x: int, y: list) -> tuple:
        out = x * y
        return out, out is x, out is y

    left_repeat_verifier = verifier_for(left_repeat)
    right_repeat_verifier = verifier_for(right_repeat)

    left_repeat_verifier.verify([1, 2, 3], 2, expected_result=([1, 2, 3, 1, 2, 3], False, False))
    left_repeat_verifier.verify([1, 2], 4, expected_result=([1, 2, 1, 2, 1, 2, 1, 2], False, False))
    left_repeat_verifier.verify([1, 2, 3], 0, expected_result=([], False, False))
    left_repeat_verifier.verify([1, 2, 3], -1, expected_result=([], False, False))
    left_repeat_verifier.verify([1, 2, 3], -2, expected_result=([], False, False))

    right_repeat_verifier.verify(2, [1, 2, 3], expected_result=([1, 2, 3, 1, 2, 3], False, False))
    right_repeat_verifier.verify(4, [1, 2], expected_result=([1, 2, 1, 2, 1, 2, 1, 2], False, False))
    right_repeat_verifier.verify(0, [1, 2, 3], expected_result=([], False, False))
    right_repeat_verifier.verify(-1, [1, 2, 3], expected_result=([], False, False))
    right_repeat_verifier.verify(-2, [1, 2, 3], expected_result=([], False, False))


def test_get_item():
    def get_item(tested: list, index: int) -> int:
        return tested[index]

    get_item_verifier = verifier_for(get_item)

    get_item_verifier.verify([1, 2, 3], 1, expected_result=2)
    get_item_verifier.verify([1, 2, 3], -1, expected_result=3)
    get_item_verifier.verify([1, 2, 3, 4], -1, expected_result=4)
    get_item_verifier.verify([1, 2, 3, 4], -2, expected_result=3)
    get_item_verifier.verify([1, 2, 3, 4], 0, expected_result=1)
    get_item_verifier.verify([1, 2, 3], 3, expected_error=IndexError)
    get_item_verifier.verify([1, 2, 3], -4, expected_error=IndexError)


def test_get_slice():
    def get_slice(tested: list, start: Union[int, None], end: Union[int, None]) -> list:
        return tested[start:end]

    get_slice_verifier = verifier_for(get_slice)

    get_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, expected_result=[2, 3])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -3, -1, expected_result=[3, 4])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, -2, expected_result=[1, 2, 3])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -3, 4, expected_result=[3, 4])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 3, 1, expected_result=[])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -1, -3, expected_result=[])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 100, 1000, expected_result=[])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, 1000, expected_result=[1, 2, 3, 4, 5])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 1, None, expected_result=[2, 3, 4, 5])
    get_slice_verifier.verify([1, 2, 3, 4, 5], None, 2, expected_result=[1, 2])
    get_slice_verifier.verify([1, 2, 3, 4, 5], None, None, expected_result=[1, 2, 3, 4, 5])


def test_get_slice_with_step():
    def get_slice_with_step(tested: list, start: Union[int, None], end: Union[int, None],
                            step: Union[int, None]) -> list:
        return tested[start:end:step]

    get_slice_verifier = verifier_for(get_slice_with_step)

    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, None, 2, expected_result=[1, 3, 5])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 1, None, 2, expected_result=[2, 4])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, 5, 2, expected_result=[1, 3, 5])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 1, 5, 2, expected_result=[2, 4])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, -1, 2, expected_result=[1, 3])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 1, -1, 2, expected_result=[2, 4])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 4, None, -2, expected_result=[5, 3, 1])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 3, None, -2, expected_result=[4, 2])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -1, -6, -2, expected_result=[5, 3, 1])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -2, -6, -2, expected_result=[4, 2])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 4, 0, -2, expected_result=[5, 3])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 3, 0, -2, expected_result=[4, 2])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, None, None, expected_result=[1, 2, 3, 4, 5])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, 3, None, expected_result=[1, 2, 3])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 3, 1, -1, expected_result=[4, 3])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -1, -3, -1, expected_result=[5, 4])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 3, 1, 1, expected_result=[])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -1, -3, 1, expected_result=[])


def test_len():
    def length(tested: list) -> int:
        return len(tested)

    len_verifier = verifier_for(length)

    len_verifier.verify([], expected_result=0)
    len_verifier.verify([1], expected_result=1)
    len_verifier.verify([1, 2], expected_result=2)
    len_verifier.verify([3, 2, 1], expected_result=3)


def test_index():
    def index(tested: list, item: object) -> int:
        return tested.index(item)

    def index_start(tested: list, item: object, start: int) -> int:
        return tested.index(item, start)

    def index_start_end(tested: list, item: object, start: int, end: int) -> int:
        return tested.index(item, start, end)

    index_verifier = verifier_for(index)
    index_start_verifier = verifier_for(index_start)
    index_start_end_verifier = verifier_for(index_start_end)

    index_verifier.verify([1, 2, 3], 1, expected_result=0)
    index_verifier.verify([1, 2, 3], 2, expected_result=1)
    index_verifier.verify([1, 2, 3], 5, expected_error=ValueError)

    index_start_verifier.verify([1, 2, 3], 1, 1, expected_error=ValueError)
    index_start_verifier.verify([1, 2, 3], 2, 1, expected_result=1)
    index_start_verifier.verify([1, 2, 3], 3, 1, expected_result=2)
    index_start_verifier.verify([1, 2, 3], 5, 1, expected_error=ValueError)

    index_start_verifier.verify([1, 2, 3], 1, -2, expected_error=ValueError)
    index_start_verifier.verify([1, 2, 3], 2, -2, expected_result=1)
    index_start_verifier.verify([1, 2, 3], 3, -2, expected_result=2)
    index_start_verifier.verify([1, 2, 3], 5, -2, expected_error=ValueError)

    index_start_end_verifier.verify([1, 2, 3], 1, 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify([1, 2, 3], 2, 1, 2, expected_result=1)
    index_start_end_verifier.verify([1, 2, 3], 3, 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify([1, 2, 3], 5, 1, 2, expected_error=ValueError)

    index_start_end_verifier.verify([1, 2, 3], 1, -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify([1, 2, 3], 2, -2, -1, expected_result=1)
    index_start_end_verifier.verify([1, 2, 3], 3, -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify([1, 2, 3], 5, -2, -1, expected_error=ValueError)


def test_count():
    def count(tested: list, item: object) -> int:
        return tested.count(item)

    count_verifier = verifier_for(count)

    count_verifier.verify([1, 2, 3], 1, expected_result=1)
    count_verifier.verify([1, 2, 3], 2, expected_result=1)
    count_verifier.verify([1, 2, 3], 3, expected_result=1)
    count_verifier.verify([1, 2, 3], 4, expected_result=0)

    count_verifier.verify([1, 2, 3, 1], 1, expected_result=2)
    count_verifier.verify([1, 1, 3, 1], 1, expected_result=3)
    count_verifier.verify([], 1, expected_result=0)


def test_set_item():
    def set_item(tested: list, index: int, value: object) -> list:
        tested[index] = value
        return tested

    set_item_verifier = verifier_for(set_item)
    set_item_verifier.verify([1, 2, 3, 4, 5], 0, 10, expected_result=[10, 2, 3, 4, 5])
    set_item_verifier.verify([1, 2, 3, 4, 5], 2, -3, expected_result=[1, 2, -3, 4, 5])
    set_item_verifier.verify([1, 2, 3, 4, 5], -1, -5, expected_result=[1, 2, 3, 4, -5])
    set_item_verifier.verify([1, 2, 3, 4, 5], -10, 1, expected_error=IndexError)
    set_item_verifier.verify([1, 2, 3, 4, 5], 10, 1, expected_error=IndexError)

def test_set_slice():
    def set_slice(tested: list, start: Union[int, None], stop: Union[int, None], value: list) -> list:
        tested[start:stop] = value
        return tested

    set_slice_verifier = verifier_for(set_slice)
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, [], expected_result=[1, 4, 5])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 2, 2, [30], expected_result=[1, 2, 30, 3, 4, 5])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, [20], expected_result=[1, 20, 4, 5])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, [20, 30], expected_result=[1, 20, 30, 4, 5])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, [20, 30, 40], expected_result=[1, 20, 30, 40, 4, 5])

    set_slice_verifier.verify([1, 2, 3, 4, 5], -4, -2, [20, 30], expected_result=[1, 20, 30, 4, 5])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, -2, [20, 30], expected_result=[1, 20, 30, 4, 5])

    set_slice_verifier.verify([1, 2, 3, 4, 5], 5, 5, [6], expected_result=[1, 2, 3, 4, 5, 6])

    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, None, [], expected_result=[1])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, None, [20, 30], expected_result=[1, 20, 30])


def test_delete_slice():
    def delete_slice(tested: list, start: Union[int, None], stop: Union[int, None]) -> list:
        del tested[start:stop]
        return tested

    delete_slice_verifier = verifier_for(delete_slice)
    delete_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, expected_result=[1, 4, 5])
    delete_slice_verifier.verify([1, 2, 3, 4, 5], 3, 5, expected_result=[1, 2, 3])
    delete_slice_verifier.verify([1, 2, 3, 4, 5], 1, None, expected_result=[1])
    delete_slice_verifier.verify([1, 2, 3, 4, 5], None, 3, expected_result=[4, 5])
    delete_slice_verifier.verify([1, 2, 3, 4, 5], None, None, expected_result=[])


def test_set_slice_with_step():
    def set_slice_with_step(tested: list, start: Union[int, None], stop: Union[int, None], step: Union[int, None],
                            value: list) -> list:
        tested[start:stop:step] = value
        return tested

    set_slice_with_step_verifier = verifier_for(set_slice_with_step)

    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 3, 0, -1, [20, 30, 40], expected_result=[1, 40, 30, 20, 5])
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 1, 4, 2, [20, 40], expected_result=[1, 20, 3, 40, 5])
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 1, -1, 2, [20, 40], expected_result=[1, 20, 3, 40, 5])
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 0, None, 2, [10, 30, 50],
                                        expected_result=[10, 2, 30, 4, 50])
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], None, 4, 2, [10, 30],
                                        expected_result=[10, 2, 30, 4, 5])
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], None, None, 2, [10, 30, 50],
                                        expected_result=[10, 2, 30, 4, 50])

    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 3, 0, -1, [], expected_error=ValueError)
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 3, 0, -1, [20, 30, 40, 50], expected_error=ValueError)


def test_delete_slice_with_step():
    def delete_slice_with_step(tested: list, start: Union[int, None], stop: Union[int, None],
                               step: Union[int, None]) -> list:
        del tested[start:stop:step]
        return tested

    delete_slice_with_step_verifier = verifier_for(delete_slice_with_step)

    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 3, 0, -1, expected_result=[1, 5])
    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 1, 4, 2, expected_result=[1, 3, 5])
    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 1, -1, 2, expected_result=[1, 3, 5])
    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 0, None, 2,
                                        expected_result=[2, 4])
    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], None, 4, 2,
                                        expected_result=[2, 4, 5])
    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], None, None, 2,
                                        expected_result=[2, 4])


def test_append():
    def append(tested: list, item: object) -> list:
        tested.append(item)
        return tested

    append_verifier = verifier_for(append)

    append_verifier.verify([], 1, expected_result=[1])
    append_verifier.verify([1], 2, expected_result=[1, 2])
    append_verifier.verify([1, 2], 3, expected_result=[1, 2, 3])
    append_verifier.verify([1, 2, 3], 3, expected_result=[1, 2, 3, 3])


def test_clear():
    def clear(tested: list) -> list:
        tested.clear()
        return tested

    clear_verifier = verifier_for(clear)

    clear_verifier.verify([], expected_result=[])
    clear_verifier.verify([1], expected_result=[])
    clear_verifier.verify([1, 2], expected_result=[])
    clear_verifier.verify([1, 2, 3], expected_result=[])


def test_copy():
    def copy(tested: list) -> tuple:
        out = tested.copy()
        return out, out is tested

    copy_verifier = verifier_for(copy)

    copy_verifier.verify([], expected_result=([], False))
    copy_verifier.verify([1], expected_result=([1], False))
    copy_verifier.verify([1, 2], expected_result=([1, 2], False))
    copy_verifier.verify([1, 2, 3], expected_result=([1, 2, 3], False))


def test_extend():
    def extend(tested: list, item: object) -> list:
        tested.extend(item)
        return tested

    extend_verifier = verifier_for(extend)

    extend_verifier.verify([], [1], expected_result=[1])
    extend_verifier.verify([1], [2], expected_result=[1, 2])
    extend_verifier.verify([1, 2], [3], expected_result=[1, 2, 3])
    extend_verifier.verify([1, 2, 3], [], expected_result=[1, 2, 3])
    extend_verifier.verify([1, 2, 3], [4, 5], expected_result=[1, 2, 3, 4, 5])
    extend_verifier.verify([1, 2, 3], [[4, 5], [6, 7]], expected_result=[1, 2, 3, [4, 5], [6, 7]])


def test_inplace_add():
    def extend(tested: list, item: list) -> list:
        tested += item
        return tested

    extend_verifier = verifier_for(extend)

    extend_verifier.verify([], [1], expected_result=[1])
    extend_verifier.verify([1], [2], expected_result=[1, 2])
    extend_verifier.verify([1, 2], [3], expected_result=[1, 2, 3])
    extend_verifier.verify([1, 2, 3], [], expected_result=[1, 2, 3])
    extend_verifier.verify([1, 2, 3], [4, 5], expected_result=[1, 2, 3, 4, 5])
    extend_verifier.verify([1, 2, 3], [[4, 5], [6, 7]], expected_result=[1, 2, 3, [4, 5], [6, 7]])


def test_inplace_multiply():
    def multiply(tested: list, item: int) -> list:
        tested *= item
        return tested

    multiply_verifier = verifier_for(multiply)

    multiply_verifier.verify([1, 2, 3], 1, expected_result=[1, 2, 3])
    multiply_verifier.verify([1, 2], 2, expected_result=[1, 2, 1, 2])
    multiply_verifier.verify([1, 2], 3, expected_result=[1, 2, 1, 2, 1, 2])
    multiply_verifier.verify([1, 2, 3], 0, expected_result=[])
    multiply_verifier.verify([1, 2, 3], -1, expected_result=[])



def test_insert():
    def insert(tested: list, index: int, item: object) -> list:
        tested.insert(index, item)
        return tested

    insert_verifier = verifier_for(insert)

    insert_verifier.verify([], 0, 1, expected_result=[1])
    insert_verifier.verify([1], 0, 2, expected_result=[2, 1])
    insert_verifier.verify([1], 1, 2, expected_result=[1, 2])
    insert_verifier.verify([1, 2], 0, 3, expected_result=[3, 1, 2])
    insert_verifier.verify([1, 2], 1, 3, expected_result=[1, 3, 2])
    insert_verifier.verify([1, 2], 2, 3, expected_result=[1, 2, 3])
    insert_verifier.verify([1, 2, 3], -1, 4, expected_result=[1, 2, 4, 3])
    insert_verifier.verify([1, 2, 3], -2, 4, expected_result=[1, 4, 2, 3])
    insert_verifier.verify([1, 2, 3], 3, 4, expected_result=[1, 2, 3, 4])
    insert_verifier.verify([1, 2, 3], 4, 4, expected_result=[1, 2, 3, 4])
    insert_verifier.verify([1, 2, 3], -4, 4, expected_result=[4, 1, 2, 3])
    insert_verifier.verify([1, 2, 3], -5, 4, expected_result=[4, 1, 2, 3])


def test_pop():
    def pop(tested: list) -> tuple:
        item = tested.pop()
        return item, tested

    pop_verifier = verifier_for(pop)

    pop_verifier.verify([1, 2, 3], expected_result=(3, [1, 2]))
    pop_verifier.verify([1, 2], expected_result=(2, [1]))
    pop_verifier.verify([1], expected_result=(1, []))

    pop_verifier.verify([1, 2, 5], expected_result=(5, [1, 2]))

    pop_verifier.verify([], expected_error=IndexError)


def test_pop_at_index():
    def pop_at_index(tested: list, index: int) -> tuple:
        item = tested.pop(index)
        return item, tested

    pop_at_index_verifier = verifier_for(pop_at_index)

    pop_at_index_verifier.verify([1, 2, 3], -1, expected_result=(3, [1, 2]))
    pop_at_index_verifier.verify([1, 2], -1, expected_result=(2, [1]))
    pop_at_index_verifier.verify([1], -1, expected_result=(1, []))

    pop_at_index_verifier.verify([1, 2, 3], 1, expected_result=(2, [1, 3]))
    pop_at_index_verifier.verify([1, 2, 3], 0, expected_result=(1, [2, 3]))
    pop_at_index_verifier.verify([1, 2, 3], 2, expected_result=(3, [1, 2]))
    pop_at_index_verifier.verify([1, 2, 3], -2, expected_result=(2, [1, 3]))

    pop_at_index_verifier.verify([1, 2, 3], -4, expected_error=IndexError)
    pop_at_index_verifier.verify([1, 2, 3], 4, expected_error=IndexError)
    pop_at_index_verifier.verify([], 0, expected_error=IndexError)


def test_remove():
    def remove(tested: list, item: object) -> list:
        tested.remove(item)
        return tested

    remove_verifier = verifier_for(remove)

    remove_verifier.verify([1, 2, 3], 1, expected_result=[2, 3])
    remove_verifier.verify([1, 2, 3], 2, expected_result=[1, 3])
    remove_verifier.verify([1, 2, 3], 3, expected_result=[1, 2])

    remove_verifier.verify([1, 3, 5], 3, expected_result=[1, 5])

    remove_verifier.verify([1, 2, 3], 4, expected_error=ValueError)
    remove_verifier.verify([], 1, expected_error=ValueError)


def test_reverse():
    def reverse(tested: list) -> list:
        tested.reverse()
        return tested

    reverse_verifier = verifier_for(reverse)

    reverse_verifier.verify([1, 2, 3], expected_result=[3, 2, 1])
    reverse_verifier.verify([3, 2, 1], expected_result=[1, 2, 3])
    reverse_verifier.verify([1, 2], expected_result=[2, 1])
    reverse_verifier.verify([2, 1], expected_result=[1, 2])
    reverse_verifier.verify([1], expected_result=[1])
    reverse_verifier.verify([], expected_result=[])


def test_sort():
    def sort(tested: list) -> list:
        tested.sort()
        return tested

    sort_verifier = verifier_for(sort)

    sort_verifier.verify([1, 2, 3], expected_result=[1, 2, 3])
    sort_verifier.verify([1, 3, 2], expected_result=[1, 2, 3])
    sort_verifier.verify([2, 1, 3], expected_result=[1, 2, 3])
    sort_verifier.verify([2, 3, 1], expected_result=[1, 2, 3])
    sort_verifier.verify([3, 1, 2], expected_result=[1, 2, 3])
    sort_verifier.verify([3, 2, 1], expected_result=[1, 2, 3])
