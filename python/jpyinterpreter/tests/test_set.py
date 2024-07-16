from .conftest import verifier_for


def test_len():
    def length(tested: set) -> int:
        return len(tested)

    len_verifier = verifier_for(length)
    len_verifier.verify(set(), expected_result=0)
    len_verifier.verify({1, 2, 3}, expected_result=3)


def test_membership():
    def membership(tested: set, x: object) -> bool:
        return x in tested

    def not_membership(tested: set, x: object) -> bool:
        return x not in tested


    membership_verifier = verifier_for(membership)
    not_membership_verifier = verifier_for(not_membership)

    membership_verifier.verify(set(), 1, expected_result=False)
    not_membership_verifier.verify(set(), 1, expected_result=True)

    membership_verifier.verify({1, 2, 3}, 1, expected_result=True)
    not_membership_verifier.verify({1, 2, 3}, 1, expected_result=False)


def test_isdisjoint():
    def isdisjoint(x: set, y: set) -> bool:
        return x.isdisjoint(y)

    isdisjoint_verifier = verifier_for(isdisjoint)

    isdisjoint_verifier.verify({1, 2, 3}, {4, 5, 6}, expected_result=True)
    isdisjoint_verifier.verify({1, 2, 3}, {3, 4, 5}, expected_result=False)


def test_issubset():
    def issubset(x: set, y: set) -> bool:
        return x.issubset(y)

    def issubset_le(x: set, y: set) -> bool:
        return x <= y

    def issubset_strict(x: set, y: set) -> bool:
        return x < y

    issubset_verifier = verifier_for(issubset)
    subset_le_verifier = verifier_for(issubset_le)
    subset_strict_verifier = verifier_for(issubset_strict)

    issubset_verifier.verify(set(), {1, 2, 3}, expected_result=True)
    subset_le_verifier.verify(set(), {1, 2, 3}, expected_result=True)
    subset_strict_verifier.verify(set(), {1, 2, 3}, expected_result=True)

    issubset_verifier.verify({1}, {1, 2, 3}, expected_result=True)
    subset_le_verifier.verify({1}, {1, 2, 3}, expected_result=True)
    subset_strict_verifier.verify({1}, {1, 2, 3}, expected_result=True)

    issubset_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result=True)
    subset_le_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result=True)
    subset_strict_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result=False)

    issubset_verifier.verify({1, 4}, {1, 2, 3}, expected_result=False)
    subset_le_verifier.verify({1, 4}, {1, 2, 3}, expected_result=False)
    subset_strict_verifier.verify({1, 4}, {1, 2, 3}, expected_result=False)

    issubset_verifier.verify({1, 2, 3}, {1}, expected_result=False)
    subset_le_verifier.verify({1, 2, 3}, {1}, expected_result=False)
    subset_strict_verifier.verify({1, 2, 3}, {1}, expected_result=False)

    issubset_verifier.verify({1, 2, 3}, set(), expected_result=False)
    subset_le_verifier.verify({1, 2, 3}, set(), expected_result=False)
    subset_strict_verifier.verify({1, 2, 3}, set(), expected_result=False)


def test_issuperset():
    def issuperset(x: set, y: set) -> bool:
        return x.issuperset(y)

    def issuperset_ge(x: set, y: set) -> bool:
        return x >= y

    def issuperset_strict(x: set, y: set) -> bool:
        return x > y

    issuperset_verifier = verifier_for(issuperset)
    superset_ge_verifier = verifier_for(issuperset_ge)
    superset_strict_verifier = verifier_for(issuperset_strict)

    issuperset_verifier.verify(set(), {1, 2, 3}, expected_result=False)
    superset_ge_verifier.verify(set(), {1, 2, 3}, expected_result=False)
    superset_strict_verifier.verify(set(), {1, 2, 3}, expected_result=False)

    issuperset_verifier.verify({1}, {1, 2, 3}, expected_result=False)
    superset_ge_verifier.verify({1}, {1, 2, 3}, expected_result=False)
    superset_strict_verifier.verify({1}, {1, 2, 3}, expected_result=False)

    issuperset_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result=True)
    superset_ge_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result=True)
    superset_strict_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result=False)

    issuperset_verifier.verify({1, 4}, {1, 2, 3}, expected_result=False)
    superset_ge_verifier.verify({1, 4}, {1, 2, 3}, expected_result=False)
    superset_strict_verifier.verify({1, 4}, {1, 2, 3}, expected_result=False)

    issuperset_verifier.verify({1, 2, 3}, {1}, expected_result=True)
    superset_ge_verifier.verify({1, 2, 3}, {1}, expected_result=True)
    superset_strict_verifier.verify({1, 2, 3}, {1}, expected_result=True)

    issuperset_verifier.verify({1, 2, 3}, set(), expected_result=True)
    superset_ge_verifier.verify({1, 2, 3}, set(), expected_result=True)
    superset_strict_verifier.verify({1, 2, 3}, set(), expected_result=True)


def test_union():
    def union(x: set, y: set) -> set:
        return x.union(y)

    def union_or(x: set, y: set) -> set:
        return x | y

    union_verifier = verifier_for(union)
    union_or_verifier = verifier_for(union_or)

    union_verifier.verify({1}, {2}, expected_result={1, 2})
    union_or_verifier.verify({1}, {2}, expected_result={1, 2})

    union_verifier.verify({1, 2}, {2, 3}, expected_result={1, 2, 3})
    union_or_verifier.verify({1, 2}, {2, 3}, expected_result={1, 2, 3})

    union_verifier.verify(set(), {1}, expected_result={1})
    union_verifier.verify(set(), {1}, expected_result={1})

    union_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result={1, 2, 3})
    union_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result={1, 2, 3})


def test_intersection():
    def intersection(x: set, y: set) -> set:
        return x.intersection(y)

    def intersection_and(x: set, y: set) -> set:
        return x & y

    intersection_verifier = verifier_for(intersection)
    intersection_and_verifier = verifier_for(intersection_and)

    intersection_verifier.verify({1}, {2}, expected_result=set())
    intersection_and_verifier.verify({1}, {2}, expected_result=set())

    intersection_verifier.verify({1, 2}, {2, 3}, expected_result={2})
    intersection_and_verifier.verify({1, 2}, {2, 3}, expected_result={2})

    intersection_verifier.verify(set(), {1}, expected_result=set())
    intersection_and_verifier.verify(set(), {1}, expected_result=set())

    intersection_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result={1, 2, 3})
    intersection_and_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result={1, 2, 3})


def test_difference():
    def difference(x: set, y: set) -> set:
        return x.difference(y)

    def difference_subtract(x: set, y: set) -> set:
        return x - y

    difference_verifier = verifier_for(difference)
    difference_subtract_verifier = verifier_for(difference_subtract)

    difference_verifier.verify({1}, {2}, expected_result={1})
    difference_subtract_verifier.verify({1}, {2}, expected_result={1})

    difference_verifier.verify({1, 2}, {2, 3}, expected_result={1})
    difference_subtract_verifier.verify({1, 2}, {2, 3}, expected_result={1})

    difference_verifier.verify({2, 3}, {1, 2}, expected_result={3})
    difference_subtract_verifier.verify({2, 3}, {1, 2}, expected_result={3})

    difference_verifier.verify(set(), {1}, expected_result=set())
    difference_subtract_verifier.verify(set(), {1}, expected_result=set())

    difference_verifier.verify({1}, set(), expected_result={1})
    difference_subtract_verifier.verify({1}, set(), expected_result={1})

    difference_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result=set())
    difference_subtract_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result=set())


def test_symmetric_difference():
    def symmetric_difference(x: set, y: set) -> set:
        return x.symmetric_difference(y)

    def symmetric_difference_xor(x: set, y: set) -> set:
        return x ^ y

    symmetric_difference_verifier = verifier_for(symmetric_difference)
    symmetric_difference_xor_verifier = verifier_for(symmetric_difference_xor)

    symmetric_difference_verifier.verify({1}, {2}, expected_result={1, 2})
    symmetric_difference_xor_verifier.verify({1}, {2}, expected_result={1, 2})

    symmetric_difference_verifier.verify({1, 2}, {2, 3}, expected_result={1, 3})
    symmetric_difference_xor_verifier.verify({1, 2}, {2, 3}, expected_result={1, 3})

    symmetric_difference_verifier.verify({2, 3}, {1, 2}, expected_result={1, 3})
    symmetric_difference_xor_verifier.verify({2, 3}, {1, 2}, expected_result={1, 3})

    symmetric_difference_verifier.verify(set(), {1}, expected_result={1})
    symmetric_difference_xor_verifier.verify(set(), {1}, expected_result={1})

    symmetric_difference_verifier.verify({1}, set(), expected_result={1})
    symmetric_difference_xor_verifier.verify({1}, set(), expected_result={1})

    symmetric_difference_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result=set())
    symmetric_difference_xor_verifier.verify({1, 2, 3}, {1, 2, 3}, expected_result=set())


def test_copy():
    def copy_function(x: set) -> tuple:
        out = x.copy()
        return out, out is x

    copy_verifier = verifier_for(copy_function)

    copy_verifier.verify(set(), expected_result=(set(), False))
    copy_verifier.verify({1}, expected_result=({1}, False))
    copy_verifier.verify({1, 2, 3}, expected_result=({1, 2, 3}, False))


def test_update():
    def update_function(x: set, y: set) -> set:
        x.update(y)
        return x

    def update_ior_function(x: set, y: set) -> set:
        x |= y
        return x

    update_verifier = verifier_for(update_function)
    update_ior_verifier = verifier_for(update_ior_function)

    update_verifier.verify(set(), {1, 2}, expected_result={1, 2})
    update_ior_verifier.verify(set(), {1, 2}, expected_result={1, 2})

    update_verifier.verify({1, 2}, {3}, expected_result={1, 2, 3})
    update_ior_verifier.verify({1, 2}, {3}, expected_result={1, 2, 3})

    update_verifier.verify({3}, {1, 2}, expected_result={1, 2, 3})
    update_ior_verifier.verify({3}, {1, 2}, expected_result={1, 2, 3})


def test_intersection_update():
    def intersection_update_function(x: set, y: set) -> set:
        x.intersection_update(y)
        return x

    def intersection_update_iand_function(x: set, y: set) -> set:
        x &= y
        return x

    intersection_update_verifier = verifier_for(intersection_update_function)
    intersection_update_iand_verifier = verifier_for(intersection_update_iand_function)

    intersection_update_verifier.verify(set(), {1, 2}, expected_result=set())
    intersection_update_iand_verifier.verify(set(), {1, 2}, expected_result=set())

    intersection_update_verifier.verify({1, 2}, {3}, expected_result=set())
    intersection_update_iand_verifier.verify({1, 2}, {3}, expected_result=set())

    intersection_update_verifier.verify({1, 2, 3}, {2}, expected_result={2})
    intersection_update_iand_verifier.verify({1, 2, 3}, {2}, expected_result={2})


def test_difference_update():
    def difference_update_function(x: set, y: set) -> set:
        x.difference_update(y)
        return x

    def difference_update_isub_function(x: set, y: set) -> set:
        x -= y
        return x

    difference_update_verifier = verifier_for(difference_update_function)
    difference_update_isub_verifier = verifier_for(difference_update_isub_function)

    difference_update_verifier.verify(set(), {1, 2}, expected_result=set())
    difference_update_isub_verifier.verify(set(), {1, 2}, expected_result=set())

    difference_update_verifier.verify({1, 2}, {3}, expected_result={1, 2})
    difference_update_isub_verifier.verify({1, 2}, {3}, expected_result={1, 2})

    difference_update_verifier.verify({1, 2, 3}, {2}, expected_result={1, 3})
    difference_update_isub_verifier.verify({1, 2, 3}, {2}, expected_result={1, 3})


def test_symmetric_difference_update():
    def symmetric_difference_update_function(x: set, y: set) -> set:
        x.symmetric_difference_update(y)
        return x

    def symmetric_difference_update_ixor_function(x: set, y: set) -> set:
        x ^= y
        return x

    symmetric_difference_update_verifier = verifier_for(symmetric_difference_update_function)
    symmetric_difference_update_ixor_verifier = verifier_for(symmetric_difference_update_ixor_function)

    symmetric_difference_update_verifier.verify(set(), {1, 2}, expected_result={1, 2})
    symmetric_difference_update_ixor_verifier.verify(set(), {1, 2}, expected_result={1, 2})

    symmetric_difference_update_verifier.verify({1, 2}, {3}, expected_result={1, 2, 3})
    symmetric_difference_update_ixor_verifier.verify({1, 2}, {3}, expected_result={1, 2, 3})

    symmetric_difference_update_verifier.verify({1, 2, 3}, {2}, expected_result={1, 3})
    symmetric_difference_update_ixor_verifier.verify({1, 2, 3}, {2}, expected_result={1, 3})

    symmetric_difference_update_verifier.verify({2}, {1, 2, 3}, expected_result={1, 3})
    symmetric_difference_update_ixor_verifier.verify({2}, {1, 2, 3}, expected_result={1, 3})


def test_add():
    def add_function(x: set, y: object) -> set:
        x.add(y)
        return x

    add_verifier = verifier_for(add_function)

    add_verifier.verify(set(), 1, expected_result={1})
    add_verifier.verify({1, 2}, 3, expected_result={1, 2, 3})
    add_verifier.verify({1, 2, 3}, 1, expected_result={1, 2, 3})


def test_remove():
    def remove_function(x: set, y: object) -> set:
        x.remove(y)
        return x

    remove_verifier = verifier_for(remove_function)

    remove_verifier.verify(set(), 1, expected_error=KeyError)
    remove_verifier.verify({1, 2}, 3, expected_error=KeyError)
    remove_verifier.verify({1, 2, 3}, 2, expected_result={1, 3})


def test_discard():
    def discard_function(x: set, y: object) -> set:
        x.discard(y)
        return x

    discard_verifier = verifier_for(discard_function)

    discard_verifier.verify(set(), 1, expected_result=set())
    discard_verifier.verify({1, 2}, 3, expected_result={1, 2})
    discard_verifier.verify({1, 2, 3}, 2, expected_result={1, 3})


def test_pop():
    def pop_function(x: set) -> tuple:
        element = x.pop()
        return element, x

    def pop_property(x):
        return lambda result: result[0] in x and result[1] == x - {result[0]}

    pop_verifier = verifier_for(pop_function)

    pop_verifier.verify(set(), expected_error=KeyError)
    pop_verifier.verify_property({1}, predicate=pop_property({1}))
    pop_verifier.verify_property({1, 2, 3}, predicate=pop_property({1, 2, 3}))


def test_clear():
    def clear_function(x: set) -> set:
        x.clear()
        return x

    clear_verifier = verifier_for(clear_function)

    clear_verifier.verify(set(), expected_result=set())
    clear_verifier.verify({1}, expected_result=set())
    clear_verifier.verify({1, 2, 3}, expected_result=set())
