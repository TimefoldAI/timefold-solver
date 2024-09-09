from .conftest import verifier_for


def test_relative_import():
    def func() -> int:
        from .pkg import variable
        return variable

    verifier = verifier_for(func)
    verifier.verify(expected_result=10)
