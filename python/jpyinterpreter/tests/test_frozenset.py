from .conftest import verifier_for


def test_len():
    def length(tested: frozenset) -> int:
        return len(tested)

    len_verifier = verifier_for(length)
    len_verifier.verify(frozenset(), expected_result=0)
    len_verifier.verify(frozenset({1, 2, 3}), expected_result=3)


def test_membership():
    def membership(tested: frozenset, x: object) -> bool:
        return x in tested

    def not_membership(tested: frozenset, x: object) -> bool:
        return x not in tested

    membership_verifier = verifier_for(membership)
    not_membership_verifier = verifier_for(not_membership)

    membership_verifier.verify(frozenset(), 1, expected_result=False)
    not_membership_verifier.verify(frozenset(), 1, expected_result=True)

    membership_verifier.verify(frozenset({1, 2, 3}), 1, expected_result=True)
    not_membership_verifier.verify(frozenset({1, 2, 3}), 1, expected_result=False)


def test_isdisjoint():
    def isdisjoint(x: frozenset, y: frozenset) -> bool:
        return x.isdisjoint(y)

    isdisjoint_verifier = verifier_for(isdisjoint)

    isdisjoint_verifier.verify(frozenset({1, 2, 3}), frozenset({4, 5, 6}), expected_result=True)
    isdisjoint_verifier.verify(frozenset({1, 2, 3}), frozenset({3, 4, 5}), expected_result=False)


def test_issubset():
    def issubset(x: frozenset, y: frozenset) -> bool:
        return x.issubset(y)

    def issubset_le(x: frozenset, y: frozenset) -> bool:
        return x <= y

    def is_strict_subset(x: frozenset, y: frozenset) -> bool:
        return x < y

    issubset_verifier = verifier_for(issubset)
    subset_le_verifier = verifier_for(issubset_le)
    subset_strict_verifier = verifier_for(is_strict_subset)

    issubset_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=True)
    subset_le_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=True)
    subset_strict_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=True)

    issubset_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=True)
    subset_le_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=True)
    subset_strict_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=True)

    issubset_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=True)
    subset_le_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=True)
    subset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=False)

    issubset_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)
    subset_le_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)
    subset_strict_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)

    issubset_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=False)
    subset_le_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=False)
    subset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=False)

    issubset_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=False)
    subset_le_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=False)
    subset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=False)


def test_issuperset():
    def issuperset(x: frozenset, y: frozenset) -> bool:
        return x.issuperset(y)

    def issuperset_ge(x: frozenset, y: frozenset) -> bool:
        return x >= y

    def is_strict_superset(x: frozenset, y: frozenset) -> bool:
        return x > y

    issuperset_verifier = verifier_for(issuperset)
    superset_ge_verifier = verifier_for(issuperset_ge)
    superset_strict_verifier = verifier_for(is_strict_superset)

    issuperset_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=False)
    superset_ge_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=False)
    superset_strict_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=False)

    issuperset_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=False)
    superset_ge_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=False)
    superset_strict_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=False)

    issuperset_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=True)
    superset_ge_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=True)
    superset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=False)

    issuperset_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)
    superset_ge_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)
    superset_strict_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)

    issuperset_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=True)
    superset_ge_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=True)
    superset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=True)

    issuperset_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=True)
    superset_ge_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=True)
    superset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=True)


def test_union():
    def union(x: frozenset, y: frozenset) -> frozenset:
        return x.union(y)

    def union_or(x: frozenset, y: frozenset) -> frozenset:
        return x | y

    union_verifier = verifier_for(union)
    union_or_verifier = verifier_for(union_or)

    union_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1, 2}))
    union_or_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1, 2}))

    union_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1, 2, 3}))
    union_or_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1, 2, 3}))

    union_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset({1}))
    union_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset({1}))

    union_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset({1, 2, 3}))
    union_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset({1, 2, 3}))


def test_intersection():
    def intersection(x: frozenset, y: frozenset) -> frozenset:
        return x.intersection(y)

    def intersection_and(x: frozenset, y: frozenset) -> frozenset:
        return x & y

    intersection_verifier = verifier_for(intersection)
    intersection_and_verifier = verifier_for(intersection_and)

    intersection_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset())
    intersection_and_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset())

    intersection_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({2}))
    intersection_and_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({2}))

    intersection_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset())
    intersection_and_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset())

    intersection_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset({1, 2, 3}))
    intersection_and_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset({1, 2, 3}))


def test_difference():
    def difference(x: frozenset, y: frozenset) -> frozenset:
        return x.difference(y)

    def difference_subtract(x: frozenset, y: frozenset) -> frozenset:
        return x - y

    difference_verifier = verifier_for(difference)
    difference_subtract_verifier = verifier_for(difference_subtract)

    difference_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1}))
    difference_subtract_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1}))

    difference_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1}))
    difference_subtract_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1}))

    difference_verifier.verify(frozenset({2, 3}), frozenset({1, 2}), expected_result=frozenset({3}))
    difference_subtract_verifier.verify(frozenset({2, 3}), frozenset({1, 2}), expected_result=frozenset({3}))

    difference_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset())
    difference_subtract_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset())

    difference_verifier.verify(frozenset({1}), frozenset(), expected_result=frozenset({1}))
    difference_subtract_verifier.verify(frozenset({1}), frozenset(), expected_result=frozenset({1}))

    difference_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset())
    difference_subtract_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset())


def test_symmetric_difference():
    def symmetric_difference(x: frozenset, y: frozenset) -> frozenset:
        return x.symmetric_difference(y)

    def symmetric_difference_xor(x: frozenset, y: frozenset) -> frozenset:
        return x ^ y

    symmetric_difference_verifier = verifier_for(symmetric_difference)
    symmetric_difference_xor_verifier = verifier_for(symmetric_difference_xor)

    symmetric_difference_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1, 2}))
    symmetric_difference_xor_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1, 2}))

    symmetric_difference_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1, 3}))
    symmetric_difference_xor_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1, 3}))

    symmetric_difference_verifier.verify(frozenset({2, 3}), frozenset({1, 2}), expected_result=frozenset({1, 3}))
    symmetric_difference_xor_verifier.verify(frozenset({2, 3}), frozenset({1, 2}), expected_result=frozenset({1, 3}))

    symmetric_difference_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset({1}))
    symmetric_difference_xor_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset({1}))

    symmetric_difference_verifier.verify(frozenset({1}), frozenset(), expected_result=frozenset({1}))
    symmetric_difference_xor_verifier.verify(frozenset({1}), frozenset(), expected_result=frozenset({1}))

    symmetric_difference_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset())
    symmetric_difference_xor_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset())


def test_copy():
    def copy_function(x: frozenset) -> tuple:
        out = x.copy()
        return out, out is x

    copy_verifier = verifier_for(copy_function)

    copy_verifier.verify(frozenset(), expected_result=(frozenset(), True))
    copy_verifier.verify(frozenset({1}), expected_result=(frozenset({1}), True))
    copy_verifier.verify(frozenset({1, 2, 3}), expected_result=(frozenset({1, 2, 3}), True))
