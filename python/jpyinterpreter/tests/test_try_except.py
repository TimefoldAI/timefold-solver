from .conftest import verifier_for


def test_try_except_with_exception_type():
    def my_function(arg: str) -> int:
        try:
            if arg == 'ValueError':
                raise ValueError
            elif arg == 'TypeError':
                raise TypeError
            return 0
        except ValueError:
            return 1

    function_verifier = verifier_for(my_function)

    function_verifier.verify('Normal', expected_result=0)
    function_verifier.verify('ValueError', expected_result=1)
    function_verifier.verify('TypeError', expected_error=TypeError)


def test_try_except_with_exception_instance():
    def my_function(arg: str) -> object:
        try:
            if arg == 'ValueError':
                raise ValueError('A value error')
            elif arg == 'TypeError':
                raise TypeError('A type error')
            return 0
        except ValueError as e:
            return e.args[0]

    function_verifier = verifier_for(my_function)

    function_verifier.verify('Normal', expected_result=0)
    function_verifier.verify('ValueError', expected_result='A value error')
    function_verifier.verify('TypeError', expected_error=TypeError)


def test_nested_try_except():
    def my_function(arg: str) -> int:
        try:
            if arg == 'ValueError1':
                raise ValueError
            try:
                if arg == 'ValueError2':
                    raise ValueError
                elif arg == 'TypeError':
                    raise TypeError
                elif arg == 'KeyError':
                    raise KeyError
            except ValueError:
                return 1
        except ValueError:
            return 2
        except TypeError:
            return 3
        return 4

    function_verifier = verifier_for(my_function)

    function_verifier.verify('Normal', expected_result=4)
    function_verifier.verify('ValueError1', expected_result=2)
    function_verifier.verify('ValueError2', expected_result=1)
    function_verifier.verify('TypeError', expected_result=3)
    function_verifier.verify('KeyError', expected_error=KeyError)


def test_try_except_finally():
    def my_function(arg: str) -> list:
        out = []
        try:
            try:
                if arg == 'ValueError':
                    raise ValueError
                elif arg == 'KeyError':
                    raise KeyError
                out.append('Try')
                return out
            except ValueError:
                out.append('ValueError')
            finally:
                out.append('Finally')
        finally:
            return out

    function_verifier = verifier_for(my_function)

    function_verifier.verify('Normal', expected_result=['Try', 'Finally'])
    function_verifier.verify('ValueError', expected_result=['ValueError', 'Finally'])
    function_verifier.verify('KeyError', expected_result=['Finally'])


def test_raise_with_cause():
    def my_function(arg: BaseException) -> str:
        try:
            raise ValueError from arg
        except ValueError as e:
            return e.__cause__.args[0]

    function_verifier = verifier_for(my_function)

    function_verifier.verify(ValueError('value message'), expected_result='value message')
    function_verifier.verify(TypeError('type message'), expected_result='type message')
    function_verifier.verify(KeyError('key message'), expected_result='key message')


def test_reraise():
    def my_function(arg: str) -> int:
        try:
            if arg == 'ValueError':
                raise ValueError
            elif arg == 'KeyError':
                raise KeyError
            return 0
        except Exception:
            raise

    function_verifier = verifier_for(my_function)

    function_verifier.verify('Normal', expected_result=0)
    function_verifier.verify('ValueError', expected_error=ValueError)
    function_verifier.verify('KeyError', expected_error=KeyError)
