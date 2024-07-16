from .conftest import verifier_for


def test_with():
    class ContextManager:
        def __enter__(self):
            return 'Context'

        def __exit__(self, exc_type, exc_val, exc_tb):
            return None

    def my_function():
        with ContextManager() as ctx:
            return ctx

    function_verifier = verifier_for(my_function)

    function_verifier.verify(expected_result='Context')


def test_with_exception_unhandled():
    class ContextManager:
        def __enter__(self):
            return 'Context'

        def __exit__(self, exc_type, exc_val, exc_tb):
            return None

    def my_function():
        with ContextManager() as ctx:
            raise TypeError

    function_verifier = verifier_for(my_function)

    function_verifier.verify(expected_error=TypeError)


def test_with_exception_unhandled_with_result():
    class ContextManager:
        def __enter__(self):
            return 'Context'

        def __exit__(self, exc_type, exc_val, exc_tb):
            return False

    def my_function():
        with ContextManager() as ctx:
            raise TypeError

    function_verifier = verifier_for(my_function)

    function_verifier.verify(expected_error=TypeError)


def test_with_exception_handled():
    class ContextManager:
        def __enter__(self):
            return 'Context'

        def __exit__(self, exc_type, exc_val, exc_tb):
            return True

    def my_function():
        with ContextManager() as ctx:
            raise TypeError
        return 10

    function_verifier = verifier_for(my_function)

    function_verifier.verify(expected_result=10)
