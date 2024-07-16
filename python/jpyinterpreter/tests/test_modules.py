from .conftest import verifier_for


def test_import():
    def function(x: int) -> int:
        import datetime
        return datetime.timedelta(days=x).days

    verifier = verifier_for(function)

    verifier.verify(0, expected_result=0)
    verifier.verify(1, expected_result=1)
    verifier.verify(2, expected_result=2)


def test_import_from():
    def function(x: int) -> int:
        from datetime import timedelta
        return timedelta(days=x).days

    verifier = verifier_for(function)

    verifier.verify(0, expected_result=0)
    verifier.verify(1, expected_result=1)
    verifier.verify(2, expected_result=2)


def test_import_native():
    def function(x):
        import math  # TODO: Use another native module once math is builtin to jpyinterpreter
        return math.ceil(x)

    verifier = verifier_for(function)

    verifier.verify(-1.7, expected_result=-1)
    verifier.verify(-1, expected_result=-1)
    verifier.verify(-0.4, expected_result=0)
    verifier.verify(0, expected_result=0)
    verifier.verify(0.5, expected_result=1)
    verifier.verify(1, expected_result=1)
    verifier.verify(1.1, expected_result=2)
    verifier.verify(2.1, expected_result=3)
    verifier.verify(2, expected_result=2)
