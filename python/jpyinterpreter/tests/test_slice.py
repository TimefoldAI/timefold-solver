from .conftest import verifier_for


def test_list_slice():
    def my_function(sequence: list, x: int, y: int) -> list:
        return sequence[x:y]

    def my_function_with_step(sequence: list, x: int, y: int, z: int) -> list:
        return sequence[x:y:z]

    slice_verifier = verifier_for(my_function)
    slice_with_step_verifier = verifier_for(my_function_with_step)

    slice_verifier.verify([2, 4, 6, 8, 10], 1, 3, expected_result=[4, 6])
    slice_verifier.verify([2, 4, 6, 8, 10], 2, -1, expected_result=[6, 8])
    slice_verifier.verify([2, 4, 6, 8, 10], -3, 3, expected_result=[6])

    slice_with_step_verifier.verify([2, 4, 6, 8, 10], 0, 3, 2, expected_result=[2, 6])
    slice_with_step_verifier.verify([2, 4, 6, 8, 10], -1, -3, -1, expected_result=[10, 8])


def test_tuple_slice():
    def my_function(sequence: tuple, x: int, y: int) -> tuple:
        return sequence[x:y]

    def my_function_with_step(sequence: tuple, x: int, y: int, z: int) -> tuple:
        return sequence[x:y:z]

    slice_verifier = verifier_for(my_function)
    slice_with_step_verifier = verifier_for(my_function_with_step)

    slice_verifier.verify((2, 4, 6, 8, 10), 1, 3, expected_result=(4, 6))
    slice_verifier.verify((2, 4, 6, 8, 10), 2, -1, expected_result=(6, 8))
    slice_verifier.verify((2, 4, 6, 8, 10), -3, 3, expected_result=(6,))

    slice_with_step_verifier.verify((2, 4, 6, 8, 10), 0, 3, 2, expected_result=(2, 6))
    slice_with_step_verifier.verify((2, 4, 6, 8, 10), -1, -3, -1, expected_result=(10, 8))


def test_str_slice():
    def my_function(sequence: str, x: int, y: int) -> str:
        return sequence[x:y]

    def my_function_with_step(sequence: str, x: int, y: int, z: int) -> str:
        return sequence[x:y:z]

    slice_verifier = verifier_for(my_function)
    slice_with_step_verifier = verifier_for(my_function_with_step)

    slice_verifier.verify('abcde', 1, 3, expected_result='bc')
    slice_verifier.verify('abcde', 2, -1, expected_result='cd')
    slice_verifier.verify('abcde', -3, 3, expected_result='c')

    slice_with_step_verifier.verify('abcde', 0, 3, 2, expected_result='ac')
    slice_with_step_verifier.verify('abcde', -1, -3, -1, expected_result='ed')
