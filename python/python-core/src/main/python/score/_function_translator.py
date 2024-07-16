from .._jpype_type_conversions import (_convert_to_java_compatible_object, PythonFunction, PythonBiFunction,
                                       PythonTriFunction, PythonQuadFunction, PythonPentaFunction, PythonToIntFunction,
                                       PythonToIntBiFunction, PythonToIntTriFunction, PythonToIntQuadFunction,
                                       PythonToLongFunction, PythonToLongBiFunction, PythonToLongTriFunction,
                                       PythonToLongQuadFunction, PythonPredicate, PythonBiPredicate,
                                       PythonTriPredicate, PythonQuadPredicate, PythonPentaPredicate)
from _jpyinterpreter import translate_python_bytecode_to_java_bytecode, check_current_python_version_supported
import jpype.imports  # noqa
from jpype import JImplements, JOverride
import inspect
import logging


logger = logging.getLogger('timefold.solver')


def _check_if_bytecode_translation_possible():
    check_current_python_version_supported()


@JImplements('java.util.function.Predicate', deferred=True)
class PythonPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument):
        return self.delegate(argument)


@JImplements('java.util.function.BiPredicate', deferred=True)
class PythonBiPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2):
        return self.delegate(argument1, argument2)


@JImplements('ai.timefold.solver.core.api.function.TriPredicate', deferred=True)
class PythonTriPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2, argument3):
        return self.delegate(argument1, argument2, argument3)


@JImplements('ai.timefold.solver.core.api.function.QuadPredicate', deferred=True)
class PythonQuadPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2, argument3, argument4):
        return self.delegate(argument1, argument2, argument3, argument4)


@JImplements('ai.timefold.solver.core.api.function.PentaPredicate', deferred=True)
class PythonPentaPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2, argument3, argument4, argument5):
        return self.delegate(argument1, argument2, argument3, argument4, argument5)


def _check_if_type_args_are_python_object_wrappers(type_args):
    from ai.timefold.jpyinterpreter.types.wrappers import PythonObjectWrapper

    for cls in type_args:
        if PythonObjectWrapper.class_.isAssignableFrom(cls):
            return True

    return False


def function_cast(function, *type_args, return_type=None):
    arg_count = len(inspect.signature(function).parameters)
    if len(type_args) != arg_count:
        raise ValueError(f'Invalid function: expected {len(type_args)} arguments but got {arg_count}')

    if _check_if_type_args_are_python_object_wrappers(type_args):
        return default_function_cast(function, arg_count)

    from java.util.function import Function, BiFunction
    from ai.timefold.solver.core.api.function import TriFunction, QuadFunction, PentaFunction
    from ai.timefold.jpyinterpreter import PythonLikeObject

    if return_type is None:
        return_type = PythonLikeObject

    try:
        _check_if_bytecode_translation_possible()
        if arg_count == 1:
            return translate_python_bytecode_to_java_bytecode(function, Function, *type_args, return_type)
        elif arg_count == 2:
            return translate_python_bytecode_to_java_bytecode(function, BiFunction, *type_args, return_type)
        elif arg_count == 3:
            return translate_python_bytecode_to_java_bytecode(function, TriFunction, *type_args, return_type)
        elif arg_count == 4:
            return translate_python_bytecode_to_java_bytecode(function, QuadFunction, *type_args, return_type)
        elif arg_count == 5:
            return translate_python_bytecode_to_java_bytecode(function, PentaFunction, *type_args, return_type)
    except:  # noqa
        return default_function_cast(function, arg_count)

    raise ValueError(f'Unexpected argument count: {arg_count}')


def default_function_cast(function, arg_count):
    if arg_count == 1:
        return PythonFunction(lambda a: _convert_to_java_compatible_object(function(a)))
    elif arg_count == 2:
        return PythonBiFunction(lambda a, b: _convert_to_java_compatible_object(function(a, b)))
    elif arg_count == 3:
        return PythonTriFunction(lambda a, b, c: _convert_to_java_compatible_object(function(a, b, c)))
    elif arg_count == 4:
        return PythonQuadFunction(lambda a, b, c, d: _convert_to_java_compatible_object(function(a, b, c, d)))
    elif arg_count == 5:
        return PythonPentaFunction(lambda a, b, c, d, e: _convert_to_java_compatible_object(function(a, b, c, d, e)))
    else:
        raise ValueError(f'Unexpected argument count: {arg_count}')


def predicate_cast(predicate, *type_args):
    arg_count = len(inspect.signature(predicate).parameters)
    if len(type_args) != arg_count:
        raise ValueError(f'Invalid function: expected {len(type_args)} arguments but got {arg_count}')

    if _check_if_type_args_are_python_object_wrappers(type_args):
        return default_predicate_cast(predicate, arg_count)

    from java.util.function import Predicate, BiPredicate
    from ai.timefold.solver.core.api.function import TriPredicate, QuadPredicate, PentaPredicate
    try:
        _check_if_bytecode_translation_possible()
        if arg_count == 1:
            return translate_python_bytecode_to_java_bytecode(predicate, Predicate, *type_args)
        elif arg_count == 2:
            return translate_python_bytecode_to_java_bytecode(predicate, BiPredicate, *type_args)
        elif arg_count == 3:
            return translate_python_bytecode_to_java_bytecode(predicate, TriPredicate, *type_args)
        elif arg_count == 4:
            return translate_python_bytecode_to_java_bytecode(predicate, QuadPredicate, *type_args)
        elif arg_count == 5:
            return translate_python_bytecode_to_java_bytecode(predicate, PentaPredicate, *type_args)
    except:  # noqa
        all_translated_successfully = False
        return default_predicate_cast(predicate, arg_count)

    raise ValueError(f'Unexpected argument count: {arg_count}')


def default_predicate_cast(predicate, arg_count):
    if arg_count == 1:
        return PythonPredicate(predicate)
    elif arg_count == 2:
        return PythonBiPredicate(predicate)
    elif arg_count == 3:
        return PythonTriPredicate(predicate)
    elif arg_count == 4:
        return PythonQuadPredicate(predicate)
    elif arg_count == 5:
        return PythonPentaPredicate(predicate)
    else:
        raise ValueError(f'Unexpected argument count: {arg_count}')


def to_int_function_cast(function, *type_args):
    arg_count = len(inspect.signature(function).parameters)
    if len(type_args) != arg_count:
        raise ValueError(f'Invalid function: expected {len(type_args)} arguments but got {arg_count}')

    if _check_if_type_args_are_python_object_wrappers(type_args):
        return default_to_int_function_cast(function, arg_count)

    from java.util.function import ToIntFunction, ToIntBiFunction
    from ai.timefold.solver.core.api.function import ToIntTriFunction, ToIntQuadFunction
    try:
        _check_if_bytecode_translation_possible()
        if arg_count == 1:
            return translate_python_bytecode_to_java_bytecode(function, ToIntFunction, *type_args)
        elif arg_count == 2:
            return translate_python_bytecode_to_java_bytecode(function, ToIntBiFunction, *type_args)
        elif arg_count == 3:
            return translate_python_bytecode_to_java_bytecode(function, ToIntTriFunction, *type_args)
        elif arg_count == 4:
            return translate_python_bytecode_to_java_bytecode(function, ToIntQuadFunction, *type_args)
    except:  # noqa
        return default_to_int_function_cast(function, arg_count)

    raise ValueError(f'Unexpected argument count: {arg_count}')


def default_to_int_function_cast(function, arg_count):
    if arg_count == 1:
        return PythonToIntFunction(lambda a: _convert_to_java_compatible_object(function(a)))
    elif arg_count == 2:
        return PythonToIntBiFunction(lambda a, b: _convert_to_java_compatible_object(function(a, b)))
    elif arg_count == 3:
        return PythonToIntTriFunction(lambda a, b, c: _convert_to_java_compatible_object(function(a, b, c)))
    elif arg_count == 4:
        return PythonToIntQuadFunction(lambda a, b, c, d: _convert_to_java_compatible_object(function(a, b, c, d)))
    else:
        raise ValueError(f'Unexpected argument count: {arg_count}')


def to_long_function_cast(function, *type_args):
    arg_count = len(inspect.signature(function).parameters)
    if len(type_args) != arg_count:
        raise ValueError(f'Invalid function: expected {len(type_args)} arguments but got {arg_count}')

    if _check_if_type_args_are_python_object_wrappers(type_args):
        return default_to_long_function_cast(function, arg_count)

    from java.util.function import ToLongFunction, ToLongBiFunction
    from ai.timefold.solver.core.api.function import ToLongTriFunction, ToLongQuadFunction
    try:
        _check_if_bytecode_translation_possible()
        if arg_count == 1:
            return translate_python_bytecode_to_java_bytecode(function, ToLongFunction, *type_args)
        elif arg_count == 2:
            return translate_python_bytecode_to_java_bytecode(function, ToLongBiFunction, *type_args)
        elif arg_count == 3:
            return translate_python_bytecode_to_java_bytecode(function, ToLongTriFunction, *type_args)
        elif arg_count == 4:
            return translate_python_bytecode_to_java_bytecode(function, ToLongQuadFunction, *type_args)
    except:  # noqa
        return default_to_long_function_cast(function, arg_count)

    raise ValueError(f'Unexpected argument count: {arg_count}')


def default_to_long_function_cast(function, arg_count):
    if arg_count == 1:
        return PythonToLongFunction(lambda a: _convert_to_java_compatible_object(function(a)))
    elif arg_count == 2:
        return PythonToLongBiFunction(lambda a, b: _convert_to_java_compatible_object(function(a, b)))
    elif arg_count == 3:
        return PythonToLongTriFunction(lambda a, b, c: _convert_to_java_compatible_object(function(a, b, c)))
    elif arg_count == 4:
        return PythonToLongQuadFunction(lambda a, b, c, d: _convert_to_java_compatible_object(function(a, b, c, d)))
    else:
        raise ValueError(f'Unexpected argument count: {arg_count}')


__all__ = ['predicate_cast',
           'function_cast',
           'to_int_function_cast',
           'to_long_function_cast']
