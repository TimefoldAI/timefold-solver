import importlib.resources
import jpype
import jpype.imports
import logging
import pathlib
from _jpyinterpreter import get_path
from jpype.types import *
from typing import cast, TypeVar, Callable, Union, TYPE_CHECKING, Any

from ._jpype_type_conversions import PythonSupplier, ConstraintProviderFunction, PythonConsumer

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from java.lang import ClassLoader
    from ai.timefold.solver.core.api.score.stream import (Constraint as _Constraint,
                                                          ConstraintFactory as _ConstraintFactory)
    from ai.timefold.solver.python import PythonLoggingEvent

logger = logging.getLogger('timefold.solver')

Solution_ = TypeVar('Solution_')
ProblemId_ = TypeVar('ProblemId_')
Score_ = TypeVar('Score_')

_compilation_queue: list[type] = []
_enterprise_installed: bool = False
_mappings_registered: bool = False
_python_score_mapping_dict: dict[str, object] = {}
_java_score_mapping_dict: dict[str, object] = {}
_user_package_prefix = 'org.jpyinterpreter.user.'

def is_enterprise_installed() -> bool:
    global _enterprise_installed
    return _enterprise_installed


def extract_timefold_jars() -> list[str]:
    """
    Returns
    -------
    list[str]
        a list contains classpath entries for
        Timefold Solver's dependencies.
    """
    global _enterprise_installed
    try:

        enterprise_dependencies = [str(get_path('timefold.solver.enterprise.jars',
                                                p.name).__enter__())
                                   for p in importlib.resources.files('timefold.solver.enterprise.jars').iterdir()
                                   if p.name.endswith(".jar")]
        _enterprise_installed = True
    except ModuleNotFoundError:
        enterprise_dependencies = []
        _enterprise_installed = False
    return [str(get_path('timefold.solver.jars', p.name).__enter__())
            for p in importlib.resources.files('timefold.solver.jars').iterdir()
            if p.name.endswith(".jar")] + enterprise_dependencies


def init(*args, path: list[str] = None, include_timefold_jars: bool = True) -> None:
    """
    Most users will never need to call this method.
    Only call this method if you are using other Java libraries.
    This method will automatically be called when a Timefold class is instantiated.

    Initializes the JVM.

    Parameters
    ----------
    args : list[str]
        JVM arguments.
    path : list[str], optional
        List of dependencies to use as the classpath.
    include_timefold_jars : bool, optional
        If the Timefold dependencies should be added to `path`.
        Defaults to True.
    """
    from _jpyinterpreter import init, get_default_jvm_path

    if jpype.isJVMStarted():  # noqa
        raise RuntimeError('JVM already started. Maybe call init before timefold.solver.types imports?')

    if path is None:
        include_timefold_jars = True
        path = []
    if include_timefold_jars:
        path = path + extract_timefold_jars()
    if len(args) == 0:
        args = [get_default_jvm_path()]
    init(*args, path=path, include_translator_jars=False)

    from ai.timefold.solver.python.logging import PythonDelegateAppender
    PythonDelegateAppender.setLogEventConsumer(PythonConsumer(forward_logging_events))
    update_log_level()


def update_log_level() -> None:
    from ai.timefold.solver.python.logging import PythonLoggingToLogbackAdapter
    PythonLoggingToLogbackAdapter.setLevel(logger.getEffectiveLevel())


def register_python_java_type_mappings():
    global _mappings_registered, _java_score_mapping_dict, _python_score_mapping_dict
    if _mappings_registered:
        return

    _mappings_registered = True

    from .score._score import (SimpleScore, HardSoftScore, HardMediumSoftScore, BendableScore,
                               SimpleDecimalScore, HardSoftDecimalScore, HardMediumSoftDecimalScore,
                               BendableDecimalScore)
    from .domain._types import ConstraintWeightOverrides
    from ai.timefold.solver.core.api.score.buildin.simplelong import SimpleLongScore as _SimpleScore
    from ai.timefold.solver.core.api.score.buildin.hardsoftlong import HardSoftLongScore as _HardSoftScore
    from ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong import HardMediumSoftLongScore as _HardMediumSoftScore
    from ai.timefold.solver.core.api.score.buildin.bendablelong import BendableLongScore as _BendableScore

    from ai.timefold.solver.core.api.score.buildin.simplebigdecimal import SimpleBigDecimalScore as _SimpleDecimalScore
    from ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal import HardSoftBigDecimalScore as _HardSoftDecimalScore
    from ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal import HardMediumSoftBigDecimalScore as _HardMediumSoftDecimalScore
    from ai.timefold.solver.core.api.score.buildin.bendablebigdecimal import BendableBigDecimalScore as _BendableDecimalScore

    from ai.timefold.solver.python.score import (SimpleScorePythonJavaTypeMapping,
                                                 HardSoftScorePythonJavaTypeMapping,
                                                 HardMediumSoftScorePythonJavaTypeMapping,
                                                 BendableScorePythonJavaTypeMapping,
                                                 SimpleDecimalScorePythonJavaTypeMapping,
                                                 HardSoftDecimalScorePythonJavaTypeMapping,
                                                 HardMediumSoftDecimalScorePythonJavaTypeMapping,
                                                 BendableDecimalScorePythonJavaTypeMapping,
                                                 )
    from ai.timefold.solver.python.domain import ConstraintWeightOverridesTypeMapping
    from _jpyinterpreter import translate_python_class_to_java_class, add_python_java_type_mapping

    _python_score_mapping_dict['SimpleScore'] = SimpleScore
    _python_score_mapping_dict['HardSoftScore'] = HardSoftScore
    _python_score_mapping_dict['HardMediumSoftScore'] = HardMediumSoftScore
    _python_score_mapping_dict['BendableScore'] = BendableScore
    _python_score_mapping_dict['SimpleDecimalScore'] = SimpleDecimalScore
    _python_score_mapping_dict['HardSoftDecimalScore'] = HardSoftDecimalScore
    _python_score_mapping_dict['HardMediumSoftDecimalScore'] = HardMediumSoftDecimalScore
    _python_score_mapping_dict['BendableDecimalScore'] = BendableDecimalScore

    _java_score_mapping_dict['SimpleScore'] = _SimpleScore
    _java_score_mapping_dict['HardSoftScore'] = _HardSoftScore
    _java_score_mapping_dict['HardMediumSoftScore'] = _HardMediumSoftScore
    _java_score_mapping_dict['BendableScore'] = _BendableScore
    _java_score_mapping_dict['SimpleDecimalScore'] = _SimpleDecimalScore
    _java_score_mapping_dict['HardSoftDecimalScore'] = _HardSoftDecimalScore
    _java_score_mapping_dict['HardMediumSoftDecimalScore'] = _HardMediumSoftDecimalScore
    _java_score_mapping_dict['BendableDecimalScore'] = _BendableDecimalScore

    SimpleScoreType = translate_python_class_to_java_class(SimpleScore)
    HardSoftScoreType = translate_python_class_to_java_class(HardSoftScore)
    HardMediumSoftScoreType = translate_python_class_to_java_class(HardMediumSoftScore)
    BendableScoreType = translate_python_class_to_java_class(BendableScore)

    SimpleDecimalScoreType = translate_python_class_to_java_class(SimpleDecimalScore)
    HardSoftDecimalScoreType = translate_python_class_to_java_class(HardSoftDecimalScore)
    HardMediumSoftDecimalScoreType = translate_python_class_to_java_class(HardMediumSoftDecimalScore)
    BendableDecimalScoreType = translate_python_class_to_java_class(BendableDecimalScore)

    ConstraintWeightOverridesType = translate_python_class_to_java_class(ConstraintWeightOverrides)

    add_python_java_type_mapping(SimpleScorePythonJavaTypeMapping(SimpleScoreType))
    add_python_java_type_mapping(HardSoftScorePythonJavaTypeMapping(HardSoftScoreType))
    add_python_java_type_mapping(HardMediumSoftScorePythonJavaTypeMapping(HardMediumSoftScoreType))
    add_python_java_type_mapping(BendableScorePythonJavaTypeMapping(BendableScoreType))

    add_python_java_type_mapping(SimpleDecimalScorePythonJavaTypeMapping(SimpleDecimalScoreType))
    add_python_java_type_mapping(HardSoftDecimalScorePythonJavaTypeMapping(HardSoftDecimalScoreType))
    add_python_java_type_mapping(HardMediumSoftDecimalScorePythonJavaTypeMapping(HardMediumSoftDecimalScoreType))
    add_python_java_type_mapping(BendableDecimalScorePythonJavaTypeMapping(BendableDecimalScoreType))

    add_python_java_type_mapping(ConstraintWeightOverridesTypeMapping(ConstraintWeightOverridesType))


def forward_logging_events(event: 'PythonLoggingEvent') -> None:
    logger.log(event.level().getPythonLevelNumber(),
               event.message())


def ensure_init():
    """
    Start the JVM if it isn't started; does nothing otherwise
    """
    if jpype.isJVMStarted(): # noqa
        return
    else:
        init()


def set_class_output_directory(path: pathlib.Path):
    """
    Sets the output directory for classes generated by Timefold Solver.
    By default, the classes are only stored in memory.

    Parameters
    ----------
    path : pathlib.Path
        Path to the output directory.
        It will be created if it doesn't exist.
    """
    ensure_init()

    from ai.timefold.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator # noqa
    PythonBytecodeToJavaBytecodeTranslator.classOutputRootPath = path


def get_class(python_class: Union[type, Callable]) -> JClass:
    """Return the Java Class for the given Python Class"""
    from java.lang import Object, Class
    from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference
    from _jpyinterpreter import is_c_native, get_java_type_for_python_type

    if python_class is None:
        return cast(JClass, None)
    if isinstance(python_class, jpype.JClass):
        return cast(JClass, python_class).class_
    if isinstance(python_class, Class):
        return cast(JClass, python_class)
    if python_class == int:
        from java.lang import Integer
        return cast(JClass, Integer).class_
    if python_class == str:
        from java.lang import String
        return cast(JClass, String).class_
    if python_class == bool:
        from java.lang import Boolean
        return cast(JClass, Boolean).class_
    if hasattr(python_class, '_timefold_java_class'):
        return cast(JClass, python_class._timefold_java_class)
    if isinstance(python_class, type):
        return cast(JClass, get_java_type_for_python_type(python_class).getJavaClass())
    if is_c_native(python_class):
        return cast(JClass, OpaquePythonReference.class_)
    return cast(JClass, Object)


def get_asm_type(python_class: Union[type, Callable]) -> Any:
    """Return the ASM type for the given Python Class"""
    from java.lang import Object, Class
    from ai.timefold.jpyinterpreter import AnnotationMetadata
    from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference
    from _jpyinterpreter import is_c_native, get_java_type_for_python_type

    if python_class is None:
        return None
    if isinstance(python_class, jpype.JClass):
        return AnnotationMetadata.getValueAsType(python_class.class_.getName())
    if isinstance(python_class, Class):
        return AnnotationMetadata.getValueAsType(python_class.getName())
    if python_class == int:
        from java.lang import Integer
        return AnnotationMetadata.getValueAsType(Integer.class_.getName())
    if python_class == str:
        from java.lang import String
        return AnnotationMetadata.getValueAsType(String.class_.getName())
    if python_class == bool:
        from java.lang import Boolean
        return AnnotationMetadata.getValueAsType(Boolean.class_.getName())
    if hasattr(python_class, '_timefold_java_class'):
        return AnnotationMetadata.getValueAsType(python_class._timefold_java_class.getName())
    if isinstance(python_class, type):
        return AnnotationMetadata.getValueAsType(get_java_type_for_python_type(python_class).getJavaTypeInternalName())
    if is_c_native(python_class):
        return AnnotationMetadata.getValueAsType(OpaquePythonReference.class_.getName())
    return AnnotationMetadata.getValueAsType(Object.class_.getName())


def register_java_class(python_object: Solution_,
                        java_class: JClass) -> Solution_:
    python_object._timefold_java_class = java_class
    class_identifier = _get_class_identifier_for_object(python_object)
    class_identifier_to_java_class_map[class_identifier] = java_class
    return python_object


def wrap_errors(func):
    def wrapped_func(*args, **kwargs):
        nonlocal func
        try:
            return func(*args, **kwargs)
        except Exception as e:
            import traceback
            msg = ''.join(traceback.TracebackException.from_exception(e).format())
            raise RuntimeError(msg)

    wrapped_func.__doc__ = func.__doc__
    wrapped_func.__qualname__ = func.__qualname__
    wrapped_func.__name__ = func.__name__
    return wrapped_func


unique_class_id = 0
"""A unique identifier; used to guarantee the generated class java name is unique"""

class_identifier_to_java_class_map = dict()
"""Maps a class identifier to the corresponding java class (the last one defined with that identifier)"""


def _get_class_identifier_for_object(python_object):
    module = getattr(python_object, '__module__', '__main__')
    if module == '__main__':
        return python_object.__qualname__
    else:
        return f'{module}.{python_object.__qualname__}'


def _compose_unique_class_name(class_identifier: str):
    from jpype import JInt
    from ai.timefold.jpyinterpreter.util import JavaIdentifierUtils
    from ai.timefold.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator
    unique_class_name = f'org.jpyinterpreter.user.{class_identifier}'
    unique_class_name = JavaIdentifierUtils.sanitizeClassName(unique_class_name)
    number_of_instances = PythonBytecodeToJavaBytecodeTranslator.classNameToSharedInstanceCount.merge(
        unique_class_name, JInt(1), lambda a, b: JInt(a + b))
    if number_of_instances > 1:
        unique_class_name = f'{unique_class_name}$${number_of_instances}'
    return unique_class_name


class OverrideClassLoader:
    thread_class_loader: 'ClassLoader'

    def __enter__(self):
        from java.lang import Thread
        from ai.timefold.solver.python import PythonWrapperGenerator  # noqa
        class_loader = PythonWrapperGenerator.getClassLoaderForAliasMap(class_identifier_to_java_class_map)
        current_thread = Thread.currentThread()
        self.thread_class_loader = current_thread.getContextClassLoader()
        current_thread.setContextClassLoader(class_loader)
        return class_loader

    def __exit__(self, exc_type, exc_val, exc_tb):
        from java.lang import Thread
        current_thread = Thread.currentThread()
        current_thread.setContextClassLoader(self.thread_class_loader)


def compile_class(python_class: type) -> None:
    from _jpyinterpreter import translate_python_class_to_java_class
    ensure_init()
    class_identifier = _get_class_identifier_for_object(python_class)
    out = translate_python_class_to_java_class(python_class).getJavaClass()
    class_identifier_to_java_class_map[class_identifier] = out


def _add_to_compilation_queue(python_class: type | PythonSupplier) -> None:
    global _compilation_queue
    _compilation_queue.append(python_class)


def _process_compilation_queue() -> None:
    global _compilation_queue

    register_python_java_type_mappings()
    while len(_compilation_queue) > 0:
        python_class = _compilation_queue.pop(0)

        if isinstance(python_class, PythonSupplier):
            python_class = python_class.get()

        compile_class(python_class)


def _to_constraint_java_array(python_list: list['_Constraint']) -> JArray:
    # reimport since the one in global scope is only for type checking
    import ai.timefold.solver.core.api.score.stream.Constraint as ActualConstraintClass
    out = jpype.JArray(ActualConstraintClass)(len(python_list))
    for i in range(len(python_list)):
        out[i] = python_list[i]
    return out


def _generate_constraint_provider_class(original_function: Callable[['_ConstraintFactory'], list['_Constraint']],
                                        wrapped_constraint_provider: Callable[['_ConstraintFactory'],
                                                                              list['_Constraint']]) -> JClass:
    ensure_init()
    register_python_java_type_mappings()
    from ai.timefold.solver.python import PythonWrapperGenerator  # noqa
    from ai.timefold.solver.core.api.score.stream import ConstraintProvider
    class_identifier = _get_class_identifier_for_object(original_function)
    out = PythonWrapperGenerator.defineConstraintProviderClass(
        _compose_unique_class_name(class_identifier),
        JObject(ConstraintProviderFunction(lambda cf: _to_constraint_java_array(wrapped_constraint_provider(cf))),
                ConstraintProvider))
    class_identifier_to_java_class_map[class_identifier] = out
    return out
