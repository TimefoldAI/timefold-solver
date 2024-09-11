import importlib.resources
import jpype
import jpype.imports
import locale
import os
import pathlib
from typing import List, ContextManager


def _normalize_path(path):
    """Normalize a path by ensuring it is a string.

    If the resulting string contains path separators, an exception is raised.
    """
    str_path = str(path)
    parent, file_name = os.path.split(str_path)
    if parent:
        raise ValueError(f'{path!r} must be only a file name')
    return file_name


def get_path(package: str, resource: str) -> ContextManager[pathlib.Path]:
    """
    Workaround since importlib.resources.path is now deprecated for removal
    """
    return importlib.resources.as_file(importlib.resources.files(package) /
                                       _normalize_path(resource))


def extract_python_translator_jars() -> list[str]:
    """Extracts and return a list of the Python Translator Java dependencies
    """
    return [str(get_path('jpyinterpreter.jars', p.name).__enter__())
            for p in importlib.resources.files('jpyinterpreter.jars').iterdir()
            if p.name.endswith(".jar")]


def init(*args, path: List[str] = None, include_translator_jars: bool = True,
         class_output_path: pathlib.Path = None):
    """Start the JVM. Throws a RuntimeError if it is already started.

    :param args: JVM args.
    :param path: If not None, a list of dependencies to use as the classpath. Default to None.
    :param include_translator_jars: If True, add translators jars to path. Default to True.
    :param class_output_path: If not None, sets the generated class output path. If None, no class
                              files are written. Can be changed by set_class_output_directory
    :return: None
    """
    if jpype.isJVMStarted():  # noqa
        raise RuntimeError('JVM already started.')
    if path is None:
        include_translator_jars = True
        path = []
    if include_translator_jars:
        path = path + extract_python_translator_jars()

    user_locale = locale.getlocale()[0]
    extra_jvm_args = []
    if user_locale is not None:
        user_locale = locale.normalize(user_locale)
        if '.' in user_locale:
            user_locale, _ = user_locale.split('.', 1)
        if '_' in user_locale:
            lang, country = user_locale.rsplit('_', maxsplit=1)
            extra_jvm_args.append(f'-Duser.language={lang}')
            extra_jvm_args.append(f'-Duser.country={country}')
        else:
            extra_jvm_args.append(f'-Duser.language={user_locale}')
    else:
        # C Locale
        extra_jvm_args.append(f'-Duser.language=C')

    jpype.startJVM(*args, *extra_jvm_args, classpath=path, convertStrings=True)  # noqa

    ensure_valid_jvm()

    if class_output_path is not None:
        from ai.timefold.jpyinterpreter import InterpreterStartupOptions  # noqa
        InterpreterStartupOptions.classOutputRootPath = class_output_path

    import ai.timefold.jpyinterpreter.CPythonBackedPythonInterpreter as CPythonBackedPythonInterpreter
    CPythonBackedPythonInterpreter.lookupPythonReferenceIdPythonFunction = GetPythonObjectId()
    CPythonBackedPythonInterpreter.lookupPythonReferenceTypePythonFunction = GetPythonObjectType()
    CPythonBackedPythonInterpreter.lookupAttributeOnPythonReferencePythonFunction = GetAttributeOnPythonObject()
    CPythonBackedPythonInterpreter.loadObjectFromPythonGlobalDict = GetNameFromGlobals()
    CPythonBackedPythonInterpreter.lookupPointerForAttributeOnPythonReferencePythonFunction = \
        GetAttributePointerOnPythonObject()
    CPythonBackedPythonInterpreter.lookupPointerArrayForAttributeOnPythonReferencePythonFunction = \
        GetAttributePointerArrayOnPythonObject()
    CPythonBackedPythonInterpreter.lookupAttributeOnPythonReferenceWithMapPythonFunction = \
        GetAttributeOnPythonObjectWithMap()
    CPythonBackedPythonInterpreter.lookupDictOnPythonReferencePythonFunction = GetDictOnPythonObject()
    CPythonBackedPythonInterpreter.setAttributeOnPythonReferencePythonFunction = SetAttributeOnPythonObject()
    CPythonBackedPythonInterpreter.deleteAttributeOnPythonReferencePythonFunction = DeleteAttributeOnPythonObject()
    CPythonBackedPythonInterpreter.callPythonFunction = CallPythonFunction()
    CPythonBackedPythonInterpreter.createFunctionFromCodeFunction = CreateFunctionFromCode()
    CPythonBackedPythonInterpreter.importModuleFunction = ImportModule()


@jpype.JImplements('java.util.function.BiConsumer', deferred=True)
class GetNameFromGlobals:
    @jpype.JOverride()
    def accept(self, java_globals, name):
        from .translator import java_globals_to_python_globals
        from .conversions import convert_to_java_python_like_object
        from ai.timefold.jpyinterpreter.util import PythonGlobalsBackedMap

        if not isinstance(java_globals, PythonGlobalsBackedMap):
            return

        python_globals = java_globals_to_python_globals[java_globals.getPythonGlobalsId()]
        try:
            python_object = python_globals[name]
            java_globals.put(name, convert_to_java_python_like_object(python_object))
        except KeyError:
            java_globals.put(name, None)


@jpype.JImplements('java.util.function.Function', deferred=True)
class GetPythonObjectId:
    @jpype.JOverride()
    def apply(self, python_object):
        return id(python_object)


@jpype.JImplements('java.util.function.Function', deferred=True)
class GetPythonObjectType:
    @jpype.JOverride()
    def apply(self, python_object):
        from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference
        return jpype.JProxy(OpaquePythonReference, inst=type(python_object), convert=True)


@jpype.JImplements('java.util.function.BiFunction', deferred=True)
class GetAttributeOnPythonObject:
    @jpype.JOverride()
    def apply(self, python_object, attribute_name):
        from .conversions import convert_to_java_python_like_object
        if not hasattr(python_object, attribute_name):
            return None
        out = getattr(python_object, attribute_name)
        return convert_to_java_python_like_object(out)


@jpype.JImplements('java.util.function.BiFunction', deferred=True)
class GetAttributePointerOnPythonObject:
    @jpype.JOverride()
    def apply(self, python_object, attribute_name):
        from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference
        if not hasattr(python_object, attribute_name):
            return None
        out = getattr(python_object, attribute_name)
        return jpype.JProxy(OpaquePythonReference, inst=out, convert=True)


@jpype.JImplements('java.util.function.BiFunction', deferred=True)
class GetAttributePointerArrayOnPythonObject:
    @jpype.JOverride()
    def apply(self, python_object, attribute_name):
        from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference
        if not hasattr(python_object, attribute_name):
            return None
        out = getattr(python_object, attribute_name)()
        out_array = OpaquePythonReference[len(out)]

        for i in range(len(out)):
            out_array[i] = jpype.JProxy(OpaquePythonReference, inst=out[i], convert=True)

        return out_array


@jpype.JImplements('ai.timefold.jpyinterpreter.util.function.TriFunction', deferred=True)
class GetAttributeOnPythonObjectWithMap:
    @jpype.JOverride()
    def apply(self, python_object, attribute_name, instance_map):
        from .conversions import convert_to_java_python_like_object
        if not hasattr(python_object, attribute_name):
            return None
        out = getattr(python_object, attribute_name)
        return convert_to_java_python_like_object(out, instance_map)


@jpype.JImplements('ai.timefold.jpyinterpreter.util.function.QuadConsumer', deferred=True)
class SetAttributeOnPythonObject:
    @jpype.JOverride()
    def accept(self, python_object, clone_map, attribute_name, value):
        from .conversions import unwrap_python_like_object
        unwrapped_object = unwrap_python_like_object(value,
                                                     clone_map)
        try:
            setattr(python_object, attribute_name, unwrapped_object)
        except:
            object.__setattr__(python_object, attribute_name, unwrapped_object)


@jpype.JImplements('java.util.function.BiConsumer', deferred=True)
class DeleteAttributeOnPythonObject:
    @jpype.JOverride()
    def accept(self, python_object, attribute_name):
        delattr(python_object, attribute_name)


@jpype.JImplements('java.util.function.BiFunction', deferred=True)
class GetDictOnPythonObject:
    @jpype.JOverride()
    def apply(self, python_object, instance_map):
        from java.util import HashMap
        from .conversions import convert_to_java_python_like_object

        out = HashMap()
        for key in dir(python_object):
            out.put(key, convert_to_java_python_like_object(getattr(python_object, key), instance_map))

        return out


@jpype.JImplements('ai.timefold.jpyinterpreter.util.function.TriFunction', deferred=True)
class CallPythonFunction:
    @jpype.JOverride()
    def apply(self, python_object, var_args_list, keyword_args_map):
        from .conversions import unwrap_python_like_object, convert_to_java_python_like_object
        actual_vargs = unwrap_python_like_object(var_args_list)
        actual_keyword_args = unwrap_python_like_object(keyword_args_map)
        if actual_keyword_args is None:
            actual_keyword_args = dict()
        try:
            out = python_object(*actual_vargs, **actual_keyword_args)
            return convert_to_java_python_like_object(out)
        except Exception as e:
            from ai.timefold.jpyinterpreter.types.errors import CPythonException
            raise CPythonException(str(e))


@jpype.JImplements('ai.timefold.jpyinterpreter.util.function.QuadFunction', deferred=True)
class CreateFunctionFromCode:
    @jpype.JOverride()
    def apply(self, code_object, function_globals, closure, name):
        from types import FunctionType
        from .conversions import unwrap_python_like_object
        from .translator import find_globals_dict_for_java_map
        from ai.timefold.jpyinterpreter import CPythonBackedPythonInterpreter  # noqa
        from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference, PythonObjectWrapper  # noqa
        from java.util import HashMap
        from jpype import JProxy

        instance_map = HashMap()
        python_code = JProxy.unwrap(code_object).wrapped
        python_globals_args = find_globals_dict_for_java_map(function_globals)
        python_closure = unwrap_python_like_object(closure)
        python_name = unwrap_python_like_object(name)

        python_function = FunctionType(code=python_code,
                                       globals=python_globals_args,
                                       name=python_name,
                                       closure=python_closure)

        proxy = JProxy(OpaquePythonReference, inst=python_function, convert=True)
        out = PythonObjectWrapper(proxy)
        CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                        proxy,
                                                                        instance_map)
        return out


@jpype.JImplements('ai.timefold.jpyinterpreter.util.function.PentaFunction', deferred=True)
class ImportModule:
    @jpype.JOverride()
    def apply(self, module_name, globals_map, locals_map, from_list, level):
        from .conversions import unwrap_python_like_object, convert_to_java_python_like_object
        from ai.timefold.jpyinterpreter import CPythonBackedPythonInterpreter  # noqa
        python_globals = unwrap_python_like_object(globals_map, None)
        python_locals = unwrap_python_like_object(locals_map, None)
        python_from_list = unwrap_python_like_object(from_list, None)

        try:
            item = __import__(module_name, python_globals, python_locals, python_from_list, level)
        except KeyError:
            from ai.timefold.jpyinterpreter.types.errors.lookup import KeyError as JavaKeyError
            raise JavaKeyError(f'Failed to import "{module_name}"')
        return convert_to_java_python_like_object(item,
                                                  CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap
        )


class InvalidJVMVersionError(Exception):
    pass


def ensure_valid_jvm(runtime=None):
    if runtime is None:
        import java.lang.Runtime as runtime
    try:
        version = runtime.version().feature()
        if version < 17:
            raise InvalidJVMVersionError(
                f"Timefold Solver for Python requires JVM (java) version 17 or later. Your JVM version {version} is not supported. Maybe use sdkman (https://sdkman.io) to install a more modern version of Java.")
    except AttributeError:
        raise InvalidJVMVersionError(
            f"Timefold Solver for Python requires JVM (java) version 17 or later. Your JVM version is not supported. Maybe use sdkman (https://sdkman.io) to install a more modern version of Java.")


def get_default_jvm_path(jvm_getter=jpype.getDefaultJVMPath):
    try:
        return jvm_getter()
    except jpype.JVMNotFoundException:
        raise InvalidJVMVersionError(
            f"Timefold Solver for Python requires JVM (java) version 17 or later. You have none installed. Maybe use sdkman (https://sdkman.io) to install a more modern version of Java.")


def ensure_init():
    """Start the JVM if it isn't started; does nothing otherwise

    Used by the translator to start the JVM when needed by a method, so
    users don't need to start the JVM themselves.

    :return: None
    """
    if jpype.isJVMStarted():  # noqa
        return
    else:
        init()


def set_class_output_directory(path: pathlib.Path):
    ensure_init()

    from ai.timefold.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator  # noqa
    PythonBytecodeToJavaBytecodeTranslator.classOutputRootPath = path
