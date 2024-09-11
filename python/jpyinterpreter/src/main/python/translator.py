import ctypes
import dis
import inspect
import sys
from jpype import JInt, JBoolean, JProxy, JClass, JArray
from typing import Protocol

MINIMUM_SUPPORTED_PYTHON_VERSION = (3, 10)
MAXIMUM_SUPPORTED_PYTHON_VERSION = (3, 12)

global_dict_to_instance = dict()
global_dict_to_key_set = dict()
java_globals_to_python_globals = dict()

type_to_compiled_java_class = dict()
type_to_annotations = dict()
type_to_java_interfaces = dict()
python_java_type_mappings = list()

function_interface_pair_to_instance = dict()
function_interface_pair_to_class = dict()


def get_file_for_module(module_name):
    import pathlib
    import sys

    if module_name is None:
        return '<unknown>'

    module = sys.modules[module_name]
    if hasattr(module, '__file__'):
        file_path = sys.modules[module_name].__file__
        if file_path is not None:
            return file_path

    # Do not know file for module; predict file from module name
    if module_name == '__main__':
        return '<stdin>'

    path_parts = module_name.split('.')
    path_parts[-1] = f'{path_parts[-1]}.py'
    return str(pathlib.Path(*path_parts))


def is_python_version_supported(python_version):
    python_version_major_minor = python_version[0:2]
    return MINIMUM_SUPPORTED_PYTHON_VERSION <= python_version_major_minor <= MAXIMUM_SUPPORTED_PYTHON_VERSION


def is_current_python_version_supported():
    return is_python_version_supported(sys.version_info)


def check_current_python_version_supported():
    if not is_current_python_version_supported():
        raise NotImplementedError(f'The translator does not support the current Python version ({sys.version}). '
                                  f'The minimum version currently supported is '
                                  f'{MINIMUM_SUPPORTED_PYTHON_VERSION[0]}.{MINIMUM_SUPPORTED_PYTHON_VERSION[1]}. '
                                  f'The maximum version currently supported is '
                                  f'{MAXIMUM_SUPPORTED_PYTHON_VERSION[0]}.{MAXIMUM_SUPPORTED_PYTHON_VERSION[1]}.')


def get_java_type_for_python_type(the_type):
    from .annotations import erase_generic_args
    from ai.timefold.jpyinterpreter.types import BuiltinTypes
    global type_to_compiled_java_class

    if isinstance(the_type, type):
        the_type = erase_generic_args(the_type)
        if the_type in type_to_compiled_java_class:
            return type_to_compiled_java_class[the_type]
        else:
            try:
                return translate_python_class_to_java_class(the_type)
            except:
                return type_to_compiled_java_class[the_type]
    if isinstance(the_type, str):
        try:
            the_type = erase_generic_args(the_type)
            maybe_type = globals()[the_type]
            if isinstance(maybe_type, type):
                return get_java_type_for_python_type(maybe_type)
            return BuiltinTypes.BASE_TYPE
        except:
            return BuiltinTypes.BASE_TYPE
    # return base type, since users could use something like 1
    return BuiltinTypes.BASE_TYPE


def get_default_args(func):
    signature = inspect.signature(func)
    return {
        k: v.default
        for k, v in signature.parameters.items()
        if v.default is not inspect.Parameter.empty
    }


def generate_proxy_class_for_translated_function(interface_type, translated_function):
    from ai.timefold.jpyinterpreter import InterfaceProxyGenerator
    return InterfaceProxyGenerator.generateProxyForFunction(interface_type, translated_function)


def generate_proxy_class_for_translated_class(interface_type, translated_class):
    from ai.timefold.jpyinterpreter import InterfaceProxyGenerator
    return InterfaceProxyGenerator.generateProxyForClass(interface_type, translated_class)


def copy_constants(constants_iterable):
    from .conversions import convert_to_java_python_like_object
    from java.util import ArrayList
    from ai.timefold.jpyinterpreter import CPythonBackedPythonInterpreter
    if constants_iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in constants_iterable:
        iterable_copy.add(convert_to_java_python_like_object(item, CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap))
    return iterable_copy


def copy_closure(closure):
    from .conversions import convert_to_java_python_like_object
    from ai.timefold.jpyinterpreter.types import PythonCell
    from ai.timefold.jpyinterpreter.types.collections import PythonLikeTuple
    from ai.timefold.jpyinterpreter import CPythonBackedPythonInterpreter
    out = PythonLikeTuple()
    if closure is None:
        return out
    else:
        for cell in closure:
            java_cell = PythonCell()
            java_cell.cellValue = convert_to_java_python_like_object(cell.cell_contents, CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap)
            out.add(java_cell)
        return out


def copy_globals(globals_dict, co_names, python_class):
    global global_dict_to_instance
    global global_dict_to_key_set
    from .conversions import convert_to_java_python_like_object
    from ai.timefold.jpyinterpreter.util import PythonGlobalsBackedMap
    from ai.timefold.jpyinterpreter import CPythonBackedPythonInterpreter

    globals_dict_key = id(globals_dict)
    instance_map = CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap
    if globals_dict_key in global_dict_to_instance:
        out = global_dict_to_instance[globals_dict_key]
        key_set = global_dict_to_key_set[globals_dict_key]
    else:
        out = PythonGlobalsBackedMap(globals_dict_key)
        key_set = set()
        global_dict_to_instance[globals_dict_key] = out
        global_dict_to_key_set[globals_dict_key] = key_set
        java_globals_to_python_globals[globals_dict_key] = globals_dict
        out['__name__'] = convert_to_java_python_like_object(globals_dict['__name__'],
                                                             instance_map)  # required for imports

    for key, value in globals_dict.items():
        if key not in key_set and key in co_names:
            if python_class is not None:
                if isinstance(value, type):
                    if issubclass(value, python_class):
                        continue
                elif isinstance(value, python_class):
                    continue
            key_set.add(key)
            out.put(key, convert_to_java_python_like_object(value, instance_map))
    return out


def find_globals_dict_for_java_map(java_globals):
    for python_global_id in global_dict_to_instance:
        if global_dict_to_instance[python_global_id] == java_globals:
            return ctypes.cast(python_global_id, ctypes.py_object).value

    if java_globals.isEmpty():
        # Java globals should have at least one entry (__name__) if it
        # was generated from Python.
        # If it is an empty map, then it not from Python and we can return
        # an empty dict
        return {}

    raise ValueError(f'Could not find python globals corresponding to {str(java_globals.toString())}')


def get_instructions(python_function):
    try:
        yield from dis.get_instructions(python_function, show_caches=True)  # Python 3.11 and above
    except TypeError:  # Python 3.10 and below
        yield from dis.get_instructions(python_function)


# From https://github.com/python/cpython/blob/main/Objects/exception_handling_notes.txt
def parse_varint(iterator):
    b = next(iterator)
    val = b & 63
    while b&64:
        val <<= 6
        b = next(iterator)
        val |= b&63
    return val


# From https://github.com/python/cpython/blob/main/Objects/exception_handling_notes.txt
def parse_exception_table(code):
    iterator = iter(code.co_exceptiontable)
    try:
        while True:
            start = parse_varint(iterator)*2
            length = parse_varint(iterator)*2
            end = start + length - 2 # Present as inclusive, not exclusive
            target = parse_varint(iterator)*2
            dl = parse_varint(iterator)
            depth = dl >> 1
            lasti = bool(dl&1)
            yield start, end, target, depth, lasti
    except StopIteration:
        return


def get_python_exception_table(python_code):
    from ai.timefold.jpyinterpreter import PythonExceptionTable, PythonVersion
    out = PythonExceptionTable()

    if hasattr(python_code, 'co_exceptiontable'):
        python_version = PythonVersion(sys.hexversion)
        for start, end, target, depth, lasti in parse_exception_table(python_code):
            out.addEntry(python_version, start, end, target, depth, lasti)

    return out


def get_function_bytecode_object(python_function, python_class: type = None):
    from .annotations import copy_type_annotations
    from .conversions import copy_iterable, init_type_to_compiled_java_class, convert_to_java_python_like_object
    from java.util import ArrayList
    from ai.timefold.jpyinterpreter import PythonBytecodeInstruction, PythonCompiledFunction, PythonVersion # noqa

    init_type_to_compiled_java_class()

    python_compiled_function = PythonCompiledFunction()
    instruction_list = ArrayList()
    for instruction in get_instructions(python_function):
        java_instruction = (
            PythonBytecodeInstruction
            .atOffset(instruction.opname, JInt(instruction.offset // 2))
            .withIsJumpTarget(JBoolean(instruction.is_jump_target)))
        if instruction.arg is not None:
            java_instruction = java_instruction.withArg(instruction.arg).withArgRepr(instruction.argrepr)
        if instruction.starts_line:
            java_instruction = java_instruction.startsLine(instruction.starts_line)

        instruction_list.add(java_instruction)

    python_compiled_function.module = python_function.__module__
    python_compiled_function.moduleFilePath = get_file_for_module(python_function.__module__)
    python_compiled_function.qualifiedName = python_function.__qualname__
    python_compiled_function.instructionList = instruction_list
    python_compiled_function.co_exceptiontable = get_python_exception_table(python_function.__code__)
    python_compiled_function.co_names = copy_iterable(python_function.__code__.co_names)
    python_compiled_function.co_varnames = copy_variable_names(python_function.__code__.co_varnames)
    python_compiled_function.co_cellvars = copy_variable_names(python_function.__code__.co_cellvars)
    python_compiled_function.co_freevars = copy_variable_names(python_function.__code__.co_freevars)
    python_compiled_function.co_constants = copy_constants(python_function.__code__.co_consts)
    python_compiled_function.co_argcount = python_function.__code__.co_argcount
    python_compiled_function.co_kwonlyargcount = python_function.__code__.co_kwonlyargcount
    python_compiled_function.closure = copy_closure(python_function.__closure__)
    python_compiled_function.globalsMap = copy_globals(python_function.__globals__, python_function.__code__.co_names,
                                                       python_class)
    python_compiled_function.typeAnnotations = copy_type_annotations(python_function,
                                                                     get_default_args(python_function),
                                                                     inspect.getfullargspec(python_function).varargs,
                                                                     inspect.getfullargspec(python_function).varkw)
    python_compiled_function.defaultPositionalArguments = convert_to_java_python_like_object(
        python_function.__defaults__ if python_function.__defaults__ else tuple())
    python_compiled_function.defaultKeywordArguments = convert_to_java_python_like_object(
        python_function.__kwdefaults__ if python_function.__kwdefaults__ else dict())
    python_compiled_function.supportExtraPositionalArgs = inspect.getfullargspec(python_function).varargs is not None
    python_compiled_function.supportExtraKeywordsArgs = inspect.getfullargspec(python_function).varkw is not None
    python_compiled_function.pythonVersion = PythonVersion(sys.hexversion)
    return python_compiled_function


def get_static_function_bytecode_object(the_class, python_function):
    return get_function_bytecode_object(python_function.__get__(the_class), python_class=the_class)


def copy_variable_names(iterable):
    from java.util import ArrayList
    from ai.timefold.jpyinterpreter.util import JavaIdentifierUtils

    if iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in iterable:
        iterable_copy.add(JavaIdentifierUtils.sanitizeFieldName(item))
    return iterable_copy


def get_code_bytecode_object(python_code):
    from .conversions import copy_iterable, init_type_to_compiled_java_class, convert_to_java_python_like_object
    from java.util import ArrayList, HashMap
    from ai.timefold.jpyinterpreter import PythonBytecodeInstruction, PythonCompiledFunction, PythonVersion # noqa

    init_type_to_compiled_java_class()

    python_compiled_function = PythonCompiledFunction()
    instruction_list = ArrayList()
    for instruction in get_instructions(python_code):
        java_instruction = (
            PythonBytecodeInstruction
            .atOffset(instruction.opname, JInt(instruction.offset // 2))
            .withIsJumpTarget(JBoolean(instruction.is_jump_target)))
        if instruction.arg is not None:
            java_instruction = java_instruction.withArg(instruction.arg).withArgRepr(instruction.argrepr)
        if instruction.starts_line:
            java_instruction = java_instruction.startsLine(instruction.starts_line)

        instruction_list.add(java_instruction)

    python_compiled_function.module = '__code__'
    python_compiled_function.qualifiedName = '__code__'
    python_compiled_function.instructionList = instruction_list
    python_compiled_function.co_exceptiontable = get_python_exception_table(python_code)
    python_compiled_function.co_names = copy_iterable(python_code.co_names)
    python_compiled_function.co_varnames = copy_variable_names(python_code.co_varnames)
    python_compiled_function.co_cellvars = copy_variable_names(python_code.co_cellvars)
    python_compiled_function.co_freevars = copy_variable_names(python_code.co_freevars)
    python_compiled_function.co_constants = copy_constants(python_code.co_consts)
    python_compiled_function.co_argcount = python_code.co_argcount
    python_compiled_function.co_kwonlyargcount = python_code.co_kwonlyargcount
    python_compiled_function.closure = copy_closure(None)
    python_compiled_function.globalsMap = HashMap()
    python_compiled_function.typeAnnotations = HashMap()
    python_compiled_function.defaultPositionalArguments = convert_to_java_python_like_object(tuple())
    python_compiled_function.defaultKeywordArguments = convert_to_java_python_like_object(dict())
    python_compiled_function.typeAnnotations = HashMap()
    python_compiled_function.supportExtraPositionalArgs = False
    python_compiled_function.supportExtraKeywordsArgs = False
    python_compiled_function.pythonVersion = PythonVersion(sys.hexversion)
    return python_compiled_function


def translate_python_bytecode_to_java_bytecode(python_function, java_function_type, *type_args):
    from .conversions import copy_iterable
    from ai.timefold.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator # noqa
    if (python_function, java_function_type, type_args) in function_interface_pair_to_instance:
        return function_interface_pair_to_instance[(python_function, java_function_type, type_args)]

    python_compiled_function = get_function_bytecode_object(python_function)

    if len(type_args) == 0:
        out = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(python_compiled_function,
                                                                             java_function_type)
        function_interface_pair_to_instance[(python_function, java_function_type, type_args)] = out
        return out
    else:
        out = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(python_compiled_function,
                                                                             java_function_type,
                                                                             copy_iterable(type_args))
        function_interface_pair_to_instance[(python_function, java_function_type, type_args)] = out
        return out


def _force_translate_python_bytecode_to_generator_java_bytecode(python_function, java_function_type):
    from ai.timefold.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator # noqa
    if (python_function, java_function_type) in function_interface_pair_to_instance:
        return function_interface_pair_to_instance[(python_function, java_function_type)]

    python_compiled_function = get_function_bytecode_object(python_function)

    out = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(python_compiled_function,
                                                                         java_function_type)
    function_interface_pair_to_instance[(python_function, java_function_type)] = out
    return out


def translate_python_code_to_java_class(python_function, java_function_type, *type_args):
    from .conversions import copy_iterable
    from ai.timefold.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator # noqa
    if (python_function, java_function_type, type_args) in function_interface_pair_to_class:
        return function_interface_pair_to_class[(python_function, java_function_type, type_args)]

    python_compiled_function = get_code_bytecode_object(python_function)

    if len(type_args) == 0:
        out = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(python_compiled_function,
                                                                                    java_function_type)
        function_interface_pair_to_class[(python_function, java_function_type, type_args)] = out
        return out
    else:
        out = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(python_compiled_function,
                                                                                    java_function_type,
                                                                                    copy_iterable(type_args))
        function_interface_pair_to_class[(python_function, java_function_type, type_args)] = out
        return out


def translate_python_code_to_python_wrapper_class(python_function):
    from .conversions import CodeWrapper
    from ai.timefold.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator # noqa
    from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference # noqa
    if (python_function,) in function_interface_pair_to_class:
        return function_interface_pair_to_class[(python_function,)]

    python_compiled_function = get_code_bytecode_object(python_function)
    out = PythonBytecodeToJavaBytecodeTranslator.\
        translatePythonBytecodeToPythonWrapperClass(python_compiled_function, JProxy(OpaquePythonReference,
                                                                                     CodeWrapper(python_function),
                                                                                     convert=True))
    function_interface_pair_to_class[(python_function,)] = out
    return out


def wrap_untyped_java_function(java_function):
    from .conversions import convert_to_java_python_like_object, unwrap_python_like_object
    def wrapped_function(*args, **kwargs):
        from java.util import ArrayList, HashMap

        instance_map = HashMap()
        java_args = ArrayList(len(args))
        java_kwargs = HashMap()

        for arg in args:
            java_args.add(convert_to_java_python_like_object(arg, instance_map))

        for key, value in kwargs:
            java_kwargs.put(convert_to_java_python_like_object(key, instance_map),
                            convert_to_java_python_like_object(value, instance_map))

        out = None
        error = None

        try:
            out = unwrap_python_like_object(getattr(java_function, '$call')(java_args, java_kwargs, None))
        except Exception as e:
            error = unwrap_python_like_object(e)

        if error is not None:
            raise error

        return out


    return wrapped_function


def wrap_typed_java_function(java_function):
    def wrapped_function(*args):
        from .conversions import convert_to_java_python_like_object, unwrap_python_like_object
        from java.util import HashMap

        instance_map = HashMap()
        java_args = [convert_to_java_python_like_object(arg, instance_map) for arg in args]

        out = None
        error = None

        try:
            out = unwrap_python_like_object(java_function.invoke(*java_args))
        except Exception as e:
            error = unwrap_python_like_object(e)

        if error is not None:
            raise error

        return out

    return wrapped_function


def try_or_reraise(function):
    from java.lang import Exception as JException
    try:
        return function()
    except JException as e:
        raise RuntimeError(f'{e.getClass().getSimpleName()}: {e.getMessage()}\n{e.stacktrace()}')


def as_java(python_function):
    return as_typed_java(python_function)


def as_untyped_java(python_function):
    from ai.timefold.jpyinterpreter.types import PythonLikeFunction
    java_function = try_or_reraise(lambda: translate_python_bytecode_to_java_bytecode(python_function, PythonLikeFunction))
    return wrap_untyped_java_function(java_function)


def as_typed_java(python_function):
    from ai.timefold.jpyinterpreter import PythonClassTranslator
    function_bytecode = get_function_bytecode_object(python_function)
    function_interface_declaration = PythonClassTranslator.getInterfaceForPythonFunction(function_bytecode)
    function_interface_class = PythonClassTranslator.getInterfaceClassForDeclaration(function_interface_declaration)
    java_function = try_or_reraise(lambda: translate_python_bytecode_to_java_bytecode(python_function, function_interface_class))
    return wrap_typed_java_function(java_function)


def _force_as_java_generator(python_function):
    from ai.timefold.jpyinterpreter.types import PythonLikeFunction
    java_function = try_or_reraise(lambda: _force_translate_python_bytecode_to_generator_java_bytecode(python_function,
                                                                                PythonLikeFunction))
    return wrap_untyped_java_function(java_function)


class MethodTypeHelper:
    @classmethod
    def class_method_type(cls):
        pass

    @staticmethod
    def static_method_type():
        pass


__CLASS_METHOD_TYPE = type(MethodTypeHelper.__dict__['class_method_type'])
__STATIC_METHOD_TYPE = type(MethodTypeHelper.__dict__['static_method_type'])


def force_update_type(python_type, java_type):
    global type_to_compiled_java_class
    type_to_compiled_java_class[python_type] = java_type


# TODO: Remove me when minimum Python version is 3.11
def get_members_static(object, predicate):
    try:
        return inspect.getmembers_static(object, predicate)
    except AttributeError:
        return _getmembers(object, predicate, type.__getattribute__)


# TODO: Remove me when minimum Python version is 3.11
def _getmembers(object, predicate, getter):
    import types
    results = []
    processed = set()
    names = dir(object)
    if inspect.isclass(object):
        mro = (object,) + inspect.getmro(object)
        # add any DynamicClassAttributes to the list of names if object is a class;
        # this may result in duplicate entries if, for example, a virtual
        # attribute with the same name as a DynamicClassAttribute exists
        try:
            for base in object.__bases__:
                for k, v in base.__dict__.items():
                    if isinstance(v, types.DynamicClassAttribute):
                        names.append(k)
        except AttributeError:
            pass
    else:
        mro = ()
    for key in names:
        # First try to get the value via getattr.  Some descriptors don't
        # like calling their __get__ (see bug #1785), so fall back to
        # looking in the __dict__.
        try:
            value = getter(object, key)
            # handle the duplicate key
            if key in processed:
                raise AttributeError
        except AttributeError:
            for base in mro:
                if key in base.__dict__:
                    value = base.__dict__[key]
                    break
            else:
                # could be a (currently) missing slot member, or a buggy
                # __dir__; discard and move on
                continue
        if not predicate or predicate(value):
            results.append((key, value))
        processed.add(key)
    results.sort(key=lambda pair: pair[0])
    return results


def translate_python_class_to_java_class(python_class):
    import collections.abc as collections_abc
    from .annotations import erase_generic_args, convert_java_annotation, copy_type_annotations
    from .conversions import (
        init_type_to_compiled_java_class, is_banned_module, is_c_native, convert_to_java_python_like_object
    )
    from java.lang import Class as JavaClass
    from java.util import ArrayList, HashMap, HashSet
    from ai.timefold.jpyinterpreter import AnnotationMetadata, PythonCompiledClass, PythonClassTranslator, CPythonBackedPythonInterpreter # noqa
    from ai.timefold.jpyinterpreter.types import BuiltinTypes
    from ai.timefold.jpyinterpreter.types.wrappers import JavaObjectWrapper, OpaquePythonReference, CPythonType # noqa

    global type_to_compiled_java_class

    init_type_to_compiled_java_class()

    raw_type = erase_generic_args(python_class)
    if raw_type in type_to_compiled_java_class:
        return type_to_compiled_java_class[raw_type]

    if Protocol in python_class.__bases__:
        python_class_java_type = BuiltinTypes.BASE_TYPE
        type_to_compiled_java_class[python_class] = python_class_java_type
        return python_class_java_type

    if hasattr(python_class, '__module__') and python_class.__module__ is not None:
        if python_class.__module__ == collections_abc.Collection.__module__:
            python_class_java_type = BuiltinTypes.BASE_TYPE
            type_to_compiled_java_class[python_class] = python_class_java_type
            return python_class_java_type

        if is_banned_module(python_class.__module__):
            python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
            type_to_compiled_java_class[python_class] = python_class_java_type
            return python_class_java_type

    if isinstance(python_class, JArray):
        python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
        type_to_compiled_java_class[python_class] = python_class_java_type
        return python_class_java_type

    if isinstance(python_class, (JClass, JavaClass)):
        try:
            out = JavaObjectWrapper.getPythonTypeForClass(python_class)
            type_to_compiled_java_class[python_class] = out
            return out
        except TypeError:
            print(f'Bad type: {type(python_class)}, from {python_class}')
            python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
            type_to_compiled_java_class[python_class] = python_class_java_type
            return python_class_java_type

    if is_c_native(python_class):
        python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
        type_to_compiled_java_class[python_class] = python_class_java_type
        return python_class_java_type

    prepared_class_info = PythonClassTranslator.getPreparedClassInfo(python_class.__name__,
                                                                     python_class.__module__,
                                                                     python_class.__qualname__)
    type_to_compiled_java_class[python_class] = prepared_class_info.type()
    methods = []
    for method_name in python_class.__dict__:
        method = inspect.getattr_static(python_class, method_name)
        if inspect.isfunction(method) or \
                isinstance(method, __STATIC_METHOD_TYPE) or \
                isinstance(method, __CLASS_METHOD_TYPE):
            methods.append((method_name, method))

    all_static_attributes = get_members_static(python_class,
                                               predicate=lambda member: not (inspect.isfunction(member)
                                                                             or isinstance(member,
                                                                                           __STATIC_METHOD_TYPE)
                                                                             or isinstance(member, __CLASS_METHOD_TYPE)
                                                                             ))
    static_attributes = [attribute for attribute in all_static_attributes if attribute[0] in python_class.__dict__]
    static_methods = [method for method in methods if isinstance(method[1], __STATIC_METHOD_TYPE)]
    class_methods = [method for method in methods if isinstance(method[1], __CLASS_METHOD_TYPE)]
    instance_methods = [method for method in methods if method not in static_methods and method not in class_methods]

    superclass_list = ArrayList()
    for superclass in python_class.__bases__:
        superclass = erase_generic_args(superclass)
        if superclass in type_to_compiled_java_class:
            if isinstance(type_to_compiled_java_class[superclass], CPythonType):
                python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
                type_to_compiled_java_class[python_class] = python_class_java_type
                return python_class_java_type
            superclass_list.add(type_to_compiled_java_class[superclass])
        else:
            try:
                superclass_list.add(translate_python_class_to_java_class(superclass))
                if isinstance(type_to_compiled_java_class[superclass], CPythonType):
                    python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
                    type_to_compiled_java_class[python_class] = python_class_java_type
                    return python_class_java_type
            except Exception:
                superclass_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=superclass, convert=True))
                type_to_compiled_java_class[superclass] = superclass_java_type
                python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
                type_to_compiled_java_class[python_class] = python_class_java_type
                return python_class_java_type

    static_method_map = HashMap()
    for method in static_methods:
        static_method_map.put(method[0], get_static_function_bytecode_object(python_class, method[1]))

    class_method_map = HashMap()
    for method in class_methods:
        class_method_map.put(method[0], get_static_function_bytecode_object(python_class, method[1]))

    instance_method_map = HashMap()
    for method in instance_methods:
        instance_method_map.put(method[0], get_function_bytecode_object(method[1], python_class=python_class))

    static_attributes_map = HashMap()
    static_attributes_to_class_instance_map = HashMap()
    static_attribute_descriptor_names = HashSet()
    static_attribute_descriptor_names.add('__class__')
    static_attribute_descriptor_names.add('__module__')

    for attribute in static_attributes:
        attribute_type = type(attribute[1])
        if issubclass(attribute_type, python_class):
            static_attributes_to_class_instance_map.put(attribute[0],
                                                        JProxy(OpaquePythonReference,
                                                               inst=attribute[1], convert=True))
        else:
            if attribute_type not in type_to_compiled_java_class:
                try:
                    translate_python_class_to_java_class(attribute_type)
                except:
                    superclass_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=attribute_type, convert=True))
                    type_to_compiled_java_class[attribute_type] = superclass_java_type

            static_attributes_map.put(attribute[0], convert_to_java_python_like_object(attribute[1]))

    for attribute in all_static_attributes:
        attribute_type = type(attribute[1])
        if (hasattr(attribute_type, '__get__') or hasattr(attribute_type, '__set__') or
                hasattr(attribute[1], '__get__') or hasattr(attribute[1], '__set__')):
            static_attribute_descriptor_names.add(attribute[0])

    python_compiled_class = PythonCompiledClass()
    python_compiled_class.annotations = ArrayList()
    python_compiled_class.javaInterfaces = ArrayList()
    python_compiled_class.pythonJavaTypeMappings = ArrayList()

    for annotation in type_to_annotations.get(python_class, []):
        python_compiled_class.annotations.add(convert_java_annotation(annotation))

    for java_interface in type_to_java_interfaces.get(python_class, []):
        if isinstance(java_interface, str):
            java_interface = JClass(java_interface)

        python_compiled_class.javaInterfaces.add(java_interface)

    for python_java_type_mapping in python_java_type_mappings:
        python_compiled_class.pythonJavaTypeMappings.add(python_java_type_mapping)

    python_compiled_class.binaryType = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class,
                                                                  convert=True))
    python_compiled_class.module = python_class.__module__
    python_compiled_class.moduleFilePath = get_file_for_module(python_class.__module__)
    python_compiled_class.qualifiedName = python_class.__qualname__
    python_compiled_class.className = python_class.__name__
    python_compiled_class.typeAnnotations = copy_type_annotations(python_class,
                                                                  dict(),
                                                                  None,
                                                                  None)
    python_compiled_class.superclassList = superclass_list
    python_compiled_class.instanceFunctionNameToPythonBytecode = instance_method_map
    python_compiled_class.staticFunctionNameToPythonBytecode = static_method_map
    python_compiled_class.classFunctionNameToPythonBytecode = class_method_map
    python_compiled_class.staticAttributeNameToObject = static_attributes_map
    python_compiled_class.staticAttributeNameToClassInstance = static_attributes_to_class_instance_map
    python_compiled_class.staticAttributeDescriptorNames = static_attribute_descriptor_names

    out = PythonClassTranslator.translatePythonClass(python_compiled_class, prepared_class_info)
    PythonClassTranslator.setSelfStaticInstances(python_compiled_class, out.getJavaClass(), out,
                                                 CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap)
    return out
