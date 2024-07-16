from .conftest import verifier_for

def test_function_traceback():
    def my_function_1():
        my_function_2()

    def my_function_2():
        raise Exception('Message')

    def check_traceback(error: Exception):
        from traceback import format_exception
        traceback = '\n'.join(format_exception(type(error), error, error.__traceback__))
        if 'test_traceback.py", line 5, in my_function_1\n' not in traceback:
            return False

        if 'test_traceback.py", line 8, in my_function_2\n' not in traceback:
            return False

        if not traceback.strip().endswith('Exception: Message'):
            return False

        if 'File "PythonException.java"' in traceback:
            return False

        return True

    verifier = verifier_for(my_function_1)
    verifier.verify_error_property(predicate=check_traceback)


def test_class_traceback():
    class A:
        def my_function(self):
            raise ValueError('Message')

    def check_traceback(error: Exception):
        from traceback import format_exception
        traceback = '\n'.join(format_exception(type(error), error, error.__traceback__))
        if 'test_traceback.py", line 34, in my_function\n' not in traceback:
            return False

        if not traceback.strip().endswith('ValueError: Message'):
            return False

        if 'File "PythonException.java"' in traceback:
            return False

        return True

    def call_class_function():
        return A().my_function()

    verifier = verifier_for(call_class_function)
    verifier.verify_error_property(predicate=check_traceback)


def test_chained_traceback():
    def first():
        return second()

    def second():
        try:
            return third()
        except ValueError as e:
            raise RuntimeError('Consequence') from e

    def third():
        raise ValueError("Cause")

    def check_traceback(error: Exception):
        from traceback import format_exception
        traceback = '\n'.join(format_exception(type(error), error, error.__traceback__))
        if 'test_traceback.py", line 59, in first\n' not in traceback:
            return False

        if 'test_traceback.py", line 63, in second\n' not in traceback:
            return False

        if 'ValueError: Cause' not in traceback:
            return False

        if 'The above exception was the direct cause of the following exception:\n' not in traceback:
            return False

        if 'test_traceback.py", line 65, in second\n' not in traceback:
            return False

        if 'test_traceback.py", line 68, in third\n' not in traceback:
            return False

        if not traceback.strip().endswith('RuntimeError: Consequence'):
            return False

        if 'File "PythonException.java"' in traceback:
            return False

        return True

    verifier = verifier_for(first)
    verifier.verify_error_property(predicate=check_traceback)
