import builtins
import importlib
import inspect
from copy import copy
from dataclasses import dataclass
from jpype import JLong, JDouble, JBoolean, JProxy
from traceback import TracebackException, StackSummary, FrameSummary
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from java.util import IdentityHashMap
    from java.lang import Throwable


def extract_frames_from_java_error(java_error: 'Throwable'):
    stack_trace = java_error.getStackTrace()
    start_index = 0
    stop_index = len(stack_trace)
    while start_index < stop_index and (stack_trace[start_index].getFileName() is None or
                                        not stack_trace[start_index].getFileName().endswith('.py')):
        start_index += 1

    # If there is no python part, keep the entire exception
    if start_index == stop_index:
        start_index = 0

    for i in range(start_index, stop_index):
        stack_trace_element = stack_trace[stop_index - (i - start_index) - 1]
        file_name = stack_trace_element.getFileName() or '<unknown>'
        if file_name.endswith('.py'):
            class_name = stack_trace_element.getClassName() or '<unknown>'
            function_name = class_name.rsplit('.', 1)[-1].split('$', 1)[0]
        else:
            function_name = stack_trace_element.getMethodName() or '<unknown>'
        line_number = stack_trace_element.getLineNumber() or 0
        yield FrameSummary(file_name, line_number, function_name)


def get_traceback_exception(java_error: 'Throwable', python_exception_type: type, clone_map: 'PythonCloneMap') -> TracebackException:
    out = object.__new__(TracebackException)
    if java_error.getCause() is not None:
        out.__cause__ = get_traceback_exception(java_error.getCause(),
                                                unwrap_python_like_object(java_error.getCause(),
                                                                          clone_map).__class__.__bases__[0],
                                                clone_map)
    else:
        out.__cause__ = None

    out.__suppress_context__ = False
    out.__context__ = None
    out.__notes__ = None
    out.exceptions = None
    out.exc_type = python_exception_type
    out.stack = StackSummary.from_list(extract_frames_from_java_error(java_error))
    out._str = java_error.getMessage() or ''
    return out


def get_translated_java_system_error_message(error):
    from ai.timefold.jpyinterpreter.util import TracebackUtils
    top_line = f'{error.getClass().getSimpleName()}:  {error.getMessage() or ""}'
    traceback = TracebackUtils.getTraceback(error)
    return f'{top_line}\n{traceback}'


class TranslatedJavaSystemError(SystemError):
    def __init__(self, error):
        super().__init__(get_translated_java_system_error_message(error))


# Taken from https://stackoverflow.com/a/60953150
def is_native_module(module):
    """ is_native_module(thing) -> boolean predicate, True if `module`
        is a native-compiled ("extension") module.

        Q.v. this fine StackOverflow answer on this subject:
            https://stackoverflow.com/a/39304199/298171
    """
    import importlib.machinery
    import inspect

    QUALIFIER = '.'
    EXTENSION_SUFFIXES = tuple(suffix.lstrip(QUALIFIER)
                               for suffix
                               in importlib.machinery.EXTENSION_SUFFIXES)

    suffix = lambda filename: QUALIFIER in filename \
                              and filename.rpartition(QUALIFIER)[-1] \
                              or ''
    # Step one: modules only beyond this point:
    if not inspect.ismodule(module):
        return False

    # Step two: return truly when “__loader__” is set:
    if isinstance(getattr(module, '__loader__', None),
                  importlib.machinery.ExtensionFileLoader):
        return True

    # Step three: in leu of either of those indicators,
    # check the module path’s file suffix:
    try:
        ext = suffix(inspect.getfile(module))
    except TypeError as exc:
        return 'is a built-in' in str(exc)

    return ext in EXTENSION_SUFFIXES


def is_c_native(item):
    import importlib
    module = getattr(item, '__module__', '')

    # __main__ is a built-in module, according to Python (and this be seen as c-native). We can also compile builtins,
    # so return False, so we can compile them
    if module == '__main__' or \
            module == 'builtins' \
            or module == '':  # if we cannot find module, assume it is not native
        return False

    try:
        return is_native_module(importlib.import_module(module))
    except:
        return True


def init_type_to_compiled_java_class():
    from .translator import check_current_python_version_supported, type_to_compiled_java_class
    from ai.timefold.jpyinterpreter.builtins import GlobalBuiltins
    from ai.timefold.jpyinterpreter.types import BuiltinTypes
    import ai.timefold.jpyinterpreter.types.datetime as java_datetime_types
    import datetime
    import builtins
    import decimal

    if len(type_to_compiled_java_class) > 0:
        return

    check_current_python_version_supported()

    type_to_compiled_java_class[staticmethod] = BuiltinTypes.STATIC_FUNCTION_TYPE
    type_to_compiled_java_class[classmethod] = BuiltinTypes.CLASS_FUNCTION_TYPE

    type_to_compiled_java_class[int] = BuiltinTypes.INT_TYPE
    type_to_compiled_java_class[float] = BuiltinTypes.FLOAT_TYPE
    type_to_compiled_java_class[complex] = BuiltinTypes.COMPLEX_TYPE
    type_to_compiled_java_class[bool] = BuiltinTypes.BOOLEAN_TYPE
    type_to_compiled_java_class[decimal.Decimal] = BuiltinTypes.DECIMAL_TYPE

    type_to_compiled_java_class[type(None)] = BuiltinTypes.NONE_TYPE
    type_to_compiled_java_class[str] = BuiltinTypes.STRING_TYPE
    type_to_compiled_java_class[bytes] = BuiltinTypes.BYTES_TYPE
    type_to_compiled_java_class[bytearray] = BuiltinTypes.BYTE_ARRAY_TYPE
    type_to_compiled_java_class[object] = BuiltinTypes.BASE_TYPE

    type_to_compiled_java_class[list] = BuiltinTypes.LIST_TYPE
    type_to_compiled_java_class[tuple] = BuiltinTypes.TUPLE_TYPE
    type_to_compiled_java_class[set] = BuiltinTypes.SET_TYPE
    type_to_compiled_java_class[frozenset] = BuiltinTypes.FROZEN_SET_TYPE
    type_to_compiled_java_class[dict] = BuiltinTypes.DICT_TYPE

    type_to_compiled_java_class[datetime.datetime] = java_datetime_types.PythonDateTime.DATE_TIME_TYPE
    type_to_compiled_java_class[datetime.date] = java_datetime_types.PythonDate.DATE_TYPE
    type_to_compiled_java_class[datetime.time] = java_datetime_types.PythonTime.TIME_TYPE
    type_to_compiled_java_class[datetime.timedelta] = java_datetime_types.PythonTimeDelta.TIME_DELTA_TYPE

    # Type aliases
    type_to_compiled_java_class[any] = BuiltinTypes.BASE_TYPE
    type_to_compiled_java_class[type] = BuiltinTypes.TYPE_TYPE

    for java_type in GlobalBuiltins.getBuiltinTypes():
        try:
            type_to_compiled_java_class[getattr(builtins, java_type.getTypeName())] = java_type
        except AttributeError:
            # This version of python does not have this builtin type; pass
            pass


def add_python_java_type_mapping(mapping):
    from .translator import python_java_type_mappings
    python_java_type_mappings.append(mapping)


def copy_iterable(iterable):
    from java.util import ArrayList
    if iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in iterable:
        iterable_copy.add(item)
    return iterable_copy


def remove_from_instance_map(instance_map, object_id):
    instance_map.remove(object_id)


def put_in_instance_map(instance_map, python_object, java_object):
    global objects_without_weakref_id_set
    instance_map.put(id(python_object), java_object)


class CodeWrapper:
    def __init__(self, wrapped):
        self.wrapped = wrapped

    def __getitem__(self, item):
        if item == 'wrapped':
            return self.wrapped
        else:
            raise KeyError(f'No item: {item}')


def convert_object_to_java_python_like_object(value, instance_map=None):
    import datetime
    from .annotations import JavaAnnotation
    from .translator import (translate_python_bytecode_to_java_bytecode,
                             translate_python_class_to_java_class)
    from .translator import (translate_python_code_to_java_class,
                             translate_python_code_to_python_wrapper_class,
                             type_to_compiled_java_class)
    from java.lang import Object, ClassNotFoundException, NoSuchMethodException
    from java.util import HashMap
    from ai.timefold.jpyinterpreter import PythonInterpreter, CPythonBackedPythonInterpreter
    from ai.timefold.jpyinterpreter.types import PythonLikeType, AbstractPythonLikeObject, CPythonBackedPythonLikeObject
    from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference, CPythonType, JavaObjectWrapper, PythonLikeFunctionWrapper
    from ai.timefold.jpyinterpreter.types.datetime import PythonDate, PythonDateTime, PythonTime, PythonTimeDelta

    if instance_map is None:
        instance_map = HashMap()

    if isinstance(value, Object):
        out = JavaObjectWrapper(value)
        put_in_instance_map(instance_map, value, out)
        return out
    if isinstance(value, JavaAnnotation):
        return None
    elif isinstance(value, datetime.datetime):
        out = PythonDateTime.of(value.year, value.month, value.day, value.hour, value.minute, value.second,
                                value.microsecond, value.tzname(), value.fold)
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, datetime.date):
        out = PythonDate.of(value.year, value.month, value.day)
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, datetime.time):
        out = PythonTime.of(value.hour, value.minute, value.second, value.microsecond, value.tzname(), value.fold)
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, datetime.timedelta):
        out = PythonTimeDelta.of(value.days, value.seconds, value.microseconds)
        put_in_instance_map(instance_map, value, out)
        return out
    elif inspect.iscode(value):
        try:
            from ai.timefold.jpyinterpreter.types import PythonLikeFunction, PythonCode
            java_class = translate_python_code_to_java_class(value, PythonLikeFunction)
            out = PythonCode(java_class)
            put_in_instance_map(instance_map, value, out)
            return out
        except:
            from ai.timefold.jpyinterpreter.types import PythonLikeFunction, PythonCode
            java_class = translate_python_code_to_python_wrapper_class(value)
            out = PythonCode(java_class)
            put_in_instance_map(instance_map, value, out)
            return out
    elif type(value) is object:
        java_type = type_to_compiled_java_class[type(value)]
        out = CPythonBackedPythonLikeObject(PythonInterpreter.DEFAULT, java_type)
        put_in_instance_map(instance_map, value, out)
        CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                        JProxy(OpaquePythonReference, inst=value,
                                                                               convert=True),
                                                                        instance_map)
        return out
    elif not inspect.isfunction(value) and type(value) in type_to_compiled_java_class:
        if type_to_compiled_java_class[type(value)] is None:
            return None
        java_type = type_to_compiled_java_class[type(value)]
        if isinstance(java_type, CPythonType):
            return None
        try:
            java_class = java_type.getJavaClass()
        except ClassNotFoundException:
            # Class is currently being generated
            return None

        try:
            out = java_class.getConstructor(PythonInterpreter, PythonLikeType).newInstance(PythonInterpreter.DEFAULT,
                                                                                           java_type)
        except NoSuchMethodException:
            # Value class, such as the error classes ValueError, and are independent of the interpreter
            out = java_class.getConstructor(PythonLikeType).newInstance(java_type)

        put_in_instance_map(instance_map, value, out)
        CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                        JProxy(OpaquePythonReference, inst=value,
                                                                               convert=True),
                                                                        instance_map)

        if isinstance(out, AbstractPythonLikeObject):
            try:
                for (key, value) in object.__getattribute__(value, '__dict__').items():
                    out.setAttribute(key, convert_to_java_python_like_object(value, instance_map))
            except AttributeError:
                pass

        return out
    elif inspect.isbuiltin(value) or is_c_native(value):
        return None
    elif inspect.isfunction(value):
        try:
            from ai.timefold.jpyinterpreter.types import PythonLikeFunction
            wrapped = PythonLikeFunctionWrapper()
            put_in_instance_map(instance_map, value, wrapped)
            out = translate_python_bytecode_to_java_bytecode(value, PythonLikeFunction)
            wrapped.setWrapped(out)
            put_in_instance_map(instance_map, value, out)
            return out
        except:
            return None
    else:
        try:
            java_type = translate_python_class_to_java_class(type(value))
            if isinstance(java_type, CPythonType):
                return None
            java_class = java_type.getJavaClass()
            try:
                out = java_class.getConstructor(PythonInterpreter, PythonLikeType).newInstance(PythonInterpreter.DEFAULT,
                                                                                               java_type)
            except NoSuchMethodException:
                # Value class, such as the error classes ValueError, and are independent of the interpreter
                out = java_class.getConstructor(PythonLikeType).newInstance(java_type)

            put_in_instance_map(instance_map, value, out)
            CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                            JProxy(OpaquePythonReference, inst=value,
                                                                                   convert=True),
                                                                            instance_map)

            if isinstance(out, AbstractPythonLikeObject):
                try:
                    for (key, value) in object.__getattribute__(value, '__dict__').items():
                        out.setAttribute(key, convert_to_java_python_like_object(value, instance_map))
                except AttributeError:
                    pass

            return out
        except:
            return None


def is_banned_module(module: str):
    banned_modules = {'jpype', 'importlib', 'builtins'}
    for banned_module in banned_modules:
        if module == banned_module:
            return True
        elif module == f'_{banned_module}':
            return True
        elif module.startswith(f'{banned_module}.'):
            return True
        elif module.startswith(f'_{banned_module}.'):
            return True
    return False


def convert_to_java_python_like_object(value, instance_map=None):
    from .translator import translate_python_class_to_java_class, type_to_compiled_java_class
    from .annotations import erase_generic_args
    from java.util import HashMap
    from java.math import BigInteger
    from types import ModuleType
    from decimal import Decimal
    from ai.timefold.jpyinterpreter import PythonLikeObject, CPythonBackedPythonInterpreter
    from ai.timefold.jpyinterpreter.types import PythonString, PythonBytes, PythonByteArray, PythonNone, \
        PythonModule, PythonSlice, PythonRange, NotImplemented as JavaNotImplemented
    from ai.timefold.jpyinterpreter.types.collections import PythonLikeList, PythonLikeTuple, PythonLikeSet, \
        PythonLikeFrozenSet, PythonLikeDict
    from ai.timefold.jpyinterpreter.types.numeric import PythonInteger, PythonFloat, PythonBoolean, PythonComplex, \
        PythonDecimal
    from ai.timefold.jpyinterpreter.types.wrappers import PythonObjectWrapper, OpaquePythonReference

    if instance_map is None:
        instance_map = HashMap()

    if instance_map.containsKey(JLong(id(value))):
        return instance_map.get(JLong(id(value)))
    elif isinstance(value, PythonLikeObject):
        put_in_instance_map(instance_map, value, value)
        return value
    elif value is None:
        return PythonNone.INSTANCE
    elif value is NotImplemented:
        return JavaNotImplemented.INSTANCE
    elif isinstance(value, bool):
        return PythonBoolean.valueOf(JBoolean(value))
    elif isinstance(value, int):
        out = PythonInteger.valueOf(BigInteger("{0:x}".format(value), 16))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, float):
        out = PythonFloat.valueOf(JDouble(value))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, Decimal):
        out = PythonDecimal.valueOf(str(value))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, complex):
        out = PythonComplex.valueOf(convert_to_java_python_like_object(value.real, instance_map),
                                    convert_to_java_python_like_object(value.imag, instance_map))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, str):
        out = PythonString.valueOf(value)
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, bytes):
        out = PythonBytes.fromIntTuple(convert_to_java_python_like_object(tuple(value)))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, bytearray):
        out = PythonByteArray.fromIntTuple(convert_to_java_python_like_object(tuple(value)))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, tuple):
        out = PythonLikeTuple()
        put_in_instance_map(instance_map, value, out)
        for item in value:
            out.add(convert_to_java_python_like_object(item, instance_map))
        return out
    elif isinstance(value, list):
        out = PythonLikeList()
        put_in_instance_map(instance_map, value, out)
        for item in value:
            out.add(convert_to_java_python_like_object(item, instance_map))
        return out
    elif isinstance(value, set):
        out = PythonLikeSet()
        put_in_instance_map(instance_map, value, out)
        for item in value:
            out.add(convert_to_java_python_like_object(item, instance_map))
        return out
    elif isinstance(value, frozenset):
        out = PythonLikeFrozenSet()
        put_in_instance_map(instance_map, value, out)
        for item in value:
            out.delegate.add(convert_to_java_python_like_object(item, instance_map))
        return out
    elif isinstance(value, dict):
        out = PythonLikeDict()
        put_in_instance_map(instance_map, value, out)
        for map_key, map_value in value.items():
            out.put(convert_to_java_python_like_object(map_key, instance_map),
                    convert_to_java_python_like_object(map_value, instance_map))
        return out
    elif isinstance(value, slice):
        out = PythonSlice(convert_to_java_python_like_object(value.start, instance_map),
                          convert_to_java_python_like_object(value.stop, instance_map),
                          convert_to_java_python_like_object(value.step, instance_map))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, range):
        out = PythonRange(convert_to_java_python_like_object(value.start, instance_map),
                          convert_to_java_python_like_object(value.stop, instance_map),
                          convert_to_java_python_like_object(value.step, instance_map))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, type):
        raw_type = erase_generic_args(value)
        if raw_type in type_to_compiled_java_class:
            if type_to_compiled_java_class[raw_type] is None:
                return None
            out = type_to_compiled_java_class[raw_type]
            put_in_instance_map(instance_map, value, out)
            return out
        else:
            out = translate_python_class_to_java_class(raw_type)
            put_in_instance_map(instance_map, value, out)
            return out
    elif isinstance(value, ModuleType) and repr(value).startswith('<module \'') and not \
            is_banned_module(value.__name__):  # should not convert java modules
        out = PythonModule(instance_map)
        out.setPythonReference(JProxy(OpaquePythonReference, inst=value, convert=True))
        put_in_instance_map(instance_map, value, out)
        # Module is populated lazily
        return out
    else:
        out = convert_object_to_java_python_like_object(value, instance_map)
        if out is not None:
            return out

        proxy = JProxy(OpaquePythonReference, inst=value, convert=True)
        out = PythonObjectWrapper(proxy)
        put_in_instance_map(instance_map, value, out)
        CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                        proxy,
                                                                        instance_map)
        return out


@dataclass
class PythonCloneMap:
    java_object_to_clone_id: 'IdentityHashMap'
    clone_id_to_python_object: dict

    def add_clone(self, java_object, python_object):
        object_id = self.java_object_to_clone_id.size()
        self.java_object_to_clone_id[java_object] = object_id
        self.clone_id_to_python_object[object_id] = python_object
        return python_object

    def has_clone(self, java_object):
        return self.java_object_to_clone_id.containsKey(java_object)

    def get_clone(self, java_object):
        return self.clone_id_to_python_object[self.java_object_to_clone_id.get(java_object)]


class WrappedExceptionMeta(type):
    def __new__(cls, name, bases, attrs, *, exception_cls=None):
        return super().__new__(cls, exception_cls.__name__, bases, attrs)


def unwrap_python_like_object(python_like_object, clone_map=None, default=NotImplementedError):
    from .translator import type_to_compiled_java_class
    from ai.timefold.jpyinterpreter import PythonLikeObject, PythonBytecodeToJavaBytecodeTranslator
    from java.util import List, Map, Set, Iterator, IdentityHashMap
    from ai.timefold.jpyinterpreter.types import PythonString, PythonBytes, PythonByteArray, PythonNone, \
        PythonModule, PythonSlice, PythonRange, CPythonBackedPythonLikeObject, PythonLikeType, PythonLikeGenericType, \
        NotImplemented as JavaNotImplemented, PythonCell, PythonLikeFunction
    from ai.timefold.jpyinterpreter.types.collections import PythonLikeTuple, PythonLikeFrozenSet
    from ai.timefold.jpyinterpreter.types.numeric import PythonInteger, PythonFloat, PythonBoolean, PythonComplex, \
        PythonDecimal
    from ai.timefold.jpyinterpreter.types.wrappers import JavaObjectWrapper, PythonObjectWrapper, CPythonType, \
        OpaquePythonReference
    from types import CellType
    from decimal import Decimal

    if clone_map is None:
        clone_map = PythonCloneMap(IdentityHashMap(), dict())

    if clone_map.has_clone(python_like_object):
        return clone_map.get_clone(python_like_object)

    if isinstance(python_like_object, (PythonObjectWrapper, JavaObjectWrapper)):
        out = python_like_object.getWrappedObject()
        return clone_map.add_clone(python_like_object, out)
    elif isinstance(python_like_object, PythonNone):
        return clone_map.add_clone(python_like_object, None)
    elif isinstance(python_like_object, JavaNotImplemented):
        return clone_map.add_clone(python_like_object, NotImplemented)
    elif isinstance(python_like_object, PythonFloat):
        return clone_map.add_clone(python_like_object, float(python_like_object.getValue()))
    elif isinstance(python_like_object, PythonString):
        return clone_map.add_clone(python_like_object, python_like_object.getValue())
    elif isinstance(python_like_object, PythonBytes):
        return clone_map.add_clone(python_like_object,
                                   bytes(unwrap_python_like_object(python_like_object.asIntTuple(),
                                                                   clone_map, default)))
    elif isinstance(python_like_object, PythonByteArray):
        return clone_map.add_clone(python_like_object, bytearray(unwrap_python_like_object(
            python_like_object.asIntTuple(), clone_map, default)))
    elif isinstance(python_like_object, PythonBoolean):
        return clone_map.add_clone(python_like_object, python_like_object == PythonBoolean.TRUE)
    elif isinstance(python_like_object, PythonInteger):
        return clone_map.add_clone(python_like_object, int(python_like_object.getValue().toString(16), 16))
    elif isinstance(python_like_object, PythonDecimal):
        return clone_map.add_clone(python_like_object, Decimal(str(python_like_object)))
    elif isinstance(python_like_object, PythonComplex):
        real = unwrap_python_like_object(python_like_object.getReal(), clone_map, default)
        imaginary = unwrap_python_like_object(python_like_object.getImaginary(), clone_map, default)
        return clone_map.add_clone(python_like_object, complex(real, imaginary))
    elif isinstance(python_like_object, (PythonLikeTuple, tuple)):
        out = []
        for item in python_like_object:
            out.append(unwrap_python_like_object(item, clone_map, default))
        return clone_map.add_clone(python_like_object, tuple(out))
    elif isinstance(python_like_object, List):
        out = []
        clone_map.add_clone(python_like_object, out)
        for item in python_like_object:
            out.append(unwrap_python_like_object(item, clone_map, default))
        return out
    elif isinstance(python_like_object, Set):
        out = set()
        if not isinstance(python_like_object, PythonLikeFrozenSet):
            clone_map.add_clone(python_like_object, out)

        for item in python_like_object:
            out.add(unwrap_python_like_object(item, clone_map, default))

        if isinstance(python_like_object, PythonLikeFrozenSet):
            return clone_map.add_clone(python_like_object, frozenset(out))

        return out
    elif isinstance(python_like_object, Map):
        out = dict()
        clone_map.add_clone(python_like_object, out)
        for entry in python_like_object.entrySet():
            out[unwrap_python_like_object(entry.getKey(), clone_map, default)] = (
                unwrap_python_like_object(entry.getValue(), clone_map, default))
        return out
    elif isinstance(python_like_object, PythonSlice):
        return clone_map.add_clone(python_like_object, slice(
            unwrap_python_like_object(python_like_object.start, clone_map, default),
            unwrap_python_like_object(python_like_object.stop, clone_map, default),
            unwrap_python_like_object(python_like_object.step, clone_map, default)))
    elif isinstance(python_like_object, PythonRange):
        return clone_map.add_clone(python_like_object, range(unwrap_python_like_object(python_like_object.start, clone_map, default),
                                                             unwrap_python_like_object(python_like_object.stop, clone_map, default),
                                                             unwrap_python_like_object(python_like_object.step, clone_map, default)))
    elif isinstance(python_like_object, Iterator):
        class JavaIterator:
            def __init__(self, iterator):
                self.iterator = iterator

            def __iter__(self):
                return self

            def __next__(self):
                try:
                    if not self.iterator.hasNext():
                        raise StopIteration()
                    else:
                        return unwrap_python_like_object(self.iterator.next(), clone_map, default)
                except StopIteration:
                    raise
                except Exception as e:
                    raise unwrap_python_like_object(e, clone_map, default)

            def send(self, sent):
                try:
                    return unwrap_python_like_object(self.iterator.send(convert_to_java_python_like_object(sent)),
                                                     clone_map,
                                                     default)
                except Exception as e:
                    raise unwrap_python_like_object(e, clone_map, default)

            def throw(self, thrown):
                try:
                    return unwrap_python_like_object(
                        self.iterator.throwValue(convert_to_java_python_like_object(thrown)),
                        clone_map, default)
                except Exception as e:
                    raise unwrap_python_like_object(e, clone_map, default)

        return clone_map.add_clone(python_like_object, JavaIterator(python_like_object))
    elif isinstance(python_like_object, PythonCell):
        out = CellType()
        clone_map.add_clone(python_like_object, out)
        out.cell_contents = unwrap_python_like_object(python_like_object.cellValue, clone_map, default)
        return out
    elif isinstance(python_like_object, PythonModule):
        return clone_map.add_clone(python_like_object, python_like_object.getPythonReference())
    elif isinstance(python_like_object, CPythonBackedPythonLikeObject):
        existing_instance = getattr(python_like_object, '$cpythonReference')
        if getattr(python_like_object, '$shouldCreateNewInstance')():
            maybe_cpython_type = getattr(python_like_object, "$CPYTHON_TYPE")
            if isinstance(maybe_cpython_type, CPythonType):
                out = (copy(existing_instance) if existing_instance is not None
                       else object.__new__(maybe_cpython_type.getPythonReference()))
                getattr(python_like_object, '$setCPythonReference')(
                    JProxy(OpaquePythonReference, inst=out, convert=True))
            else:
                out = None
        else:
            out = existing_instance

        if out is not None:
            clone_map.add_clone(python_like_object, out)
            update_python_object_from_java(python_like_object, clone_map)
            return out
    elif isinstance(python_like_object, Exception):
        try:
            exception_name = getattr(python_like_object, '$TYPE').getTypeName()
            exception_python_type = getattr(builtins, exception_name)
            args = unwrap_python_like_object(getattr(python_like_object, '$getArgs')(),
                                             clone_map, default)
            traceback_exception = get_traceback_exception(python_like_object, exception_python_type, clone_map)

            class WrappedException(exception_python_type,
                                   metaclass=WrappedExceptionMeta,
                                   exception_cls=exception_python_type):
                wrapped_type: type
                def __init__(self, *args):
                    super().__init__(*args)

                def __str__(self):
                    return '\n' + exception_name + ': ' + (
                            python_like_object.getMessage() or '') + '\n' + ''.join(traceback_exception.format())

            return clone_map.add_clone(python_like_object, WrappedException(*args))
        except AttributeError:
            return clone_map.add_clone(python_like_object, TranslatedJavaSystemError(python_like_object))
    elif isinstance(python_like_object, PythonLikeType):
        if python_like_object.getClass() == PythonLikeGenericType:
            return clone_map.add_clone(python_like_object, type)

        for (key, value) in type_to_compiled_java_class.items():
            if value == python_like_object:
                return clone_map.add_clone(python_like_object, key)
        else:
            raise KeyError(f'Cannot find corresponding Python type for Java class {python_like_object.getClass().getName()}')
    elif not isinstance(python_like_object, PythonLikeObject):
        return clone_map.add_clone(python_like_object, python_like_object)
    else:
        out = unwrap_python_like_builtin_module_object(python_like_object, clone_map, default)
        if out is not None:
            return out

        if isinstance(python_like_object, PythonLikeFunction):
            qualified_name = python_like_object.getClass().getCanonicalName()[
                             len(PythonBytecodeToJavaBytecodeTranslator.USER_PACKAGE_BASE):]
            module_name, _, function_name = qualified_name.rpartition('.')
            return getattr(importlib.import_module(module_name), function_name)

        if default == NotImplementedError:
            raise NotImplementedError(f'Unable to convert object of type {type(python_like_object)}')
        return default


def update_python_object_from_java(java_object, clone_map=None):
    from java.util import IdentityHashMap
    from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference
    if clone_map is None:
        clone_map = PythonCloneMap(IdentityHashMap(), dict())

    try:
        getattr(java_object, '$writeFieldsToCPythonReference')(JProxy(OpaquePythonReference,
                                                                      inst=clone_map,
                                                                      convert=True))
    except TypeError:
        # The Python Object is immutable; so no changes from Java
        pass


def unwrap_python_like_builtin_module_object(python_like_object, clone_map, default=NotImplementedError):
    from java.util import IdentityHashMap
    from ai.timefold.jpyinterpreter.types.datetime import PythonDate, PythonTime, PythonDateTime, PythonTimeDelta
    import datetime

    if clone_map is None:
        clone_map = PythonCloneMap(IdentityHashMap(), dict())

    if isinstance(python_like_object, PythonDateTime):
        return clone_map.add_clone(python_like_object, datetime.datetime(unwrap_python_like_object(python_like_object.year, clone_map, default),
                                                                         unwrap_python_like_object(python_like_object.month, clone_map, default),
                                                                         unwrap_python_like_object(python_like_object.day, clone_map, default),
                                                                         unwrap_python_like_object(python_like_object.hour, clone_map, default),
                                                                         unwrap_python_like_object(python_like_object.minute, clone_map, default),
                                                                         unwrap_python_like_object(python_like_object.second, clone_map, default),
                                                                         unwrap_python_like_object(python_like_object.microsecond, clone_map, default),
                                                                         tzinfo=None,  # TODO: Support timezones
                                                                         fold=unwrap_python_like_object(python_like_object.fold, clone_map, default)))

    if isinstance(python_like_object, PythonDate):
        return clone_map.add_clone(python_like_object, datetime.date(unwrap_python_like_object(python_like_object.year, clone_map, default),
                                                                     unwrap_python_like_object(python_like_object.month, clone_map, default),
                                                                     unwrap_python_like_object(python_like_object.day, clone_map, default)))

    if isinstance(python_like_object, PythonTime):
        return clone_map.add_clone(python_like_object, datetime.time(unwrap_python_like_object(python_like_object.hour, clone_map, default),
                                                                     unwrap_python_like_object(python_like_object.minute, clone_map, default),
                                                                     unwrap_python_like_object(python_like_object.second, clone_map, default),
                                                                     unwrap_python_like_object(python_like_object.microsecond, clone_map, default),
                                                                     tzinfo=None,  # TODO: Support timezones
                                                                     fold=unwrap_python_like_object(python_like_object.fold, clone_map, default)))

    if isinstance(python_like_object, PythonTimeDelta):
        return clone_map.add_clone(python_like_object, datetime.timedelta(unwrap_python_like_object(python_like_object.days, clone_map, default),
                                                                          unwrap_python_like_object(python_like_object.seconds, clone_map, default),
                                                                          unwrap_python_like_object(python_like_object.microseconds, clone_map, default)))

    return None
