from jpype import JProxy, JImplements, JOverride, JConversion
from jpype.types import *
from types import FunctionType
from typing import TYPE_CHECKING
from decimal import Decimal
import timefold.solver._timefold_java_interop as _timefold_java_interop

if TYPE_CHECKING:
    from .score._score import Score


@JImplements('ai.timefold.solver.core.api.score.stream.ConstraintProvider', deferred=True)
class ConstraintProviderFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def defineConstraints(self, constraint_factory):
        return self.delegate(constraint_factory)


@JImplements('java.util.function.Consumer', deferred=True)
class PythonConsumer:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def accept(self, item):
        return self.delegate(item)


@JImplements('java.util.function.Supplier', deferred=True)
class PythonSupplier:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def get(self):
        return self.delegate()


@JImplements('java.util.function.Function', deferred=True)
class PythonFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument):
        return self.delegate(argument)


@JImplements('java.util.function.BiFunction', deferred=True)
class PythonBiFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument1, argument2):
        return self.delegate(argument1, argument2)


@JImplements('ai.timefold.solver.core.api.function.TriFunction', deferred=True)
class PythonTriFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument1, argument2, argument3):
        return self.delegate(argument1, argument2, argument3)


@JImplements('ai.timefold.solver.core.api.function.QuadFunction', deferred=True)
class PythonQuadFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument1, argument2, argument3, argument4):
        return self.delegate(argument1, argument2, argument3, argument4)


@JImplements('ai.timefold.solver.core.api.function.PentaFunction', deferred=True)
class PythonPentaFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument1, argument2, argument3, argument4, argument5):
        return self.delegate(argument1, argument2, argument3, argument4, argument5)


@JImplements('java.util.function.ToIntFunction', deferred=True)
class PythonToIntFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsInt(self, argument):
        return JInt(self.delegate(argument))


@JImplements('java.util.function.ToIntBiFunction', deferred=True)
class PythonToIntBiFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsInt(self, argument1, argument2):
        return JInt(self.delegate(argument1, argument2))


@JImplements('ai.timefold.solver.core.api.function.ToIntTriFunction', deferred=True)
class PythonToIntTriFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsInt(self, argument1, argument2, argument3):
        return JInt(self.delegate(argument1, argument2, argument3))


@JImplements('ai.timefold.solver.core.api.function.ToIntQuadFunction', deferred=True)
class PythonToIntQuadFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsInt(self, argument1, argument2, argument3, argument4):
        return JInt(self.delegate(argument1, argument2, argument3, argument4))


@JImplements('ai.timefold.solver.core.api.function.ToIntPentaFunction', deferred=True)
class PythonToIntPentaFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsInt(self, argument1, argument2, argument3, argument4, argument5):
        return JInt(self.delegate(argument1, argument2, argument3, argument4, argument5))


@JImplements('java.util.function.ToLongFunction', deferred=True)
class PythonToLongFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsLong(self, argument):
        return JLong(self.delegate(argument))


@JImplements('java.util.function.ToLongBiFunction', deferred=True)
class PythonToLongBiFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsLong(self, argument1, argument2):
        return JLong(self.delegate(argument1, argument2))


@JImplements('ai.timefold.solver.core.api.function.ToLongTriFunction', deferred=True)
class PythonToLongTriFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsLong(self, argument1, argument2, argument3):
        return JLong(self.delegate(argument1, argument2, argument3))


@JImplements('ai.timefold.solver.core.api.function.ToLongQuadFunction', deferred=True)
class PythonToLongQuadFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsLong(self, argument1, argument2, argument3, argument4):
        return JLong(self.delegate(argument1, argument2, argument3, argument4))


@JImplements('ai.timefold.solver.core.api.function.ToLongPentaFunction', deferred=True)
class PythonToLongPentaFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsLong(self, argument1, argument2, argument3, argument4, argument5):
        return JLong(self.delegate(argument1, argument2, argument3, argument4, argument5))



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


def to_python_score(score) -> 'Score':
    if isinstance(score, _timefold_java_interop._java_score_mapping_dict['SimpleScore']):
        return _timefold_java_interop._python_score_mapping_dict['SimpleScore'](score.score(),
                                                                                init_score=score.initScore())
    elif isinstance(score, _timefold_java_interop._java_score_mapping_dict['HardSoftScore']):
        return _timefold_java_interop._python_score_mapping_dict['HardSoftScore'](score.hardScore(),
                                                                                  score.softScore(),
                                                                                  init_score=score.initScore())
    elif isinstance(score, _timefold_java_interop._java_score_mapping_dict['HardMediumSoftScore']):
        return _timefold_java_interop._python_score_mapping_dict['HardMediumSoftScore'](score.hardScore(),
                                                                                        score.mediumScore(),
                                                                                        score.softScore(),
                                                                                        init_score=score.initScore())
    elif isinstance(score, _timefold_java_interop._java_score_mapping_dict['BendableScore']):
        return _timefold_java_interop._python_score_mapping_dict['BendableScore'](score.hardScores(),
                                                                                  score.softScores(),
                                                                                  init_score=score.initScore())
    elif isinstance(score, _timefold_java_interop._java_score_mapping_dict['SimpleDecimalScore']):
        return _timefold_java_interop._python_score_mapping_dict['SimpleDecimalScore'](Decimal(score.score().toPlainString()),
                                                                                       init_score=score.initScore())
    elif isinstance(score, _timefold_java_interop._java_score_mapping_dict['HardSoftDecimalScore']):
        return _timefold_java_interop._python_score_mapping_dict['HardSoftDecimalScore'](Decimal(score.hardScore().toPlainString()),
                                                                                         Decimal(score.softScore().toPlainString()),
                                                                                         init_score=score.initScore())
    elif isinstance(score, _timefold_java_interop._java_score_mapping_dict['HardMediumSoftDecimalScore']):
        return _timefold_java_interop._python_score_mapping_dict['HardMediumSoftDecimalScore'](Decimal(score.hardScore().toPlainString()),
                                                                                               Decimal(score.mediumScore().toPlainString()),
                                                                                               Decimal(score.softScore().toPlainString()),
                                                                                               init_score=score.initScore())
    elif isinstance(score, _timefold_java_interop._java_score_mapping_dict['BendableDecimalScore']):
        return _timefold_java_interop._python_score_mapping_dict['BendableDecimalScore']([Decimal(part.toPlainString())
                                                                                          for part in score.hardScores()
                                                                                         ],
                                                                                         [Decimal(part.toPlainString())
                                                                                          for part in score.softScores()
                                                                                         ],
                                                                                         init_score=score.initScore())
    else:
        raise TypeError(f'Unexpected score type: {type(score)}')


# Function convertors
def _has_java_class(item):
    if isinstance(item, (JObject, int, str, bool)):
        return True
    if hasattr(type(item), '_timefold_java_class'):
        return True
    return False


def _proxy(value):
    from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference  # noqa
    return JProxy(OpaquePythonReference, inst=value, convert=True)


def _convert_to_java_compatible_object(item):
    from ai.timefold.solver.python import PythonComparable  # noqa
    if _has_java_class(item) or item is None:
        return item
    return PythonComparable(_proxy(item))


@JConversion('java.lang.Class', exact=type)
def _convert_type_to_class(jcls, type_obj):
    from ._timefold_java_interop import get_class
    from java.lang import Object
    out = get_class(type_obj)
    if out == Object and type_obj != Object:
        raise ValueError(f'Type {type_obj} does not have a Java class proxy. Maybe annotate it with '
                         f'@problem_fact, @planning_entity, or @planning_solution?')

    return out


@JConversion('java.lang.Class', exact=FunctionType)
def _convert_function_to_class(jcls, function_obj):
    from ._timefold_java_interop import get_class
    from java.lang import Object
    out = get_class(function_obj)
    if out == Object and function_obj != Object:
        raise ValueError(f'Function {function_obj} does not have a Java class proxy. Maybe annotate it with '
                         f'@constraint_provider?')

    return out


# Jpype convert int to primitive, but not to their wrappers, so add implicit conversion to wrappers
@JConversion('java.lang.Integer', exact=int)
def _convert_to_integer(jcls, obj):
    from java.lang import Integer
    return Integer @ JInt(obj)


@JConversion('java.lang.Long', exact=int)
def _convert_to_long(jcls, obj):
    from java.lang import Long
    return Long @ JLong(obj)


@JConversion('java.lang.Short', exact=int)
def _convert_to_short(jcls, obj):
    from java.lang import Short
    return Short @ JShort(obj)


@JConversion('java.lang.Byte', exact=int)
def _convert_to_byte(jcls, obj):
    from java.lang import Byte
    return Byte @ JByte(obj)
