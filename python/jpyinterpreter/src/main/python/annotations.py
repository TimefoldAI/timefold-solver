from collections import defaultdict
from dataclasses import dataclass
from jpype import JClass, JArray
from types import FunctionType, NoneType, UnionType
from typing import TypeVar, Any, List, Tuple, Dict, Union, Annotated, Type, Callable, \
    get_origin, get_args, get_type_hints


class AnnotationValueSupplier:
    def __init__(self, supplier: Callable[[], Any]):
        self.supplier = supplier

    def get_value(self) -> Any:
        return self.supplier()


@dataclass
class JavaAnnotation:
    annotation_type: JClass
    annotation_values: Dict[str, Any]

    def __hash__(self):
        return 0


T = TypeVar('T')


def add_class_annotation(annotation_type, /, **annotation_values: Any) -> Callable[[Type[T]], Type[T]]:
    def decorator(_cls: Type[T]) -> Type[T]:
        from .translator import type_to_compiled_java_class, type_to_annotations
        if _cls in type_to_compiled_java_class:
            raise RuntimeError('Cannot add an annotation after a class has been compiled.')
        annotations = type_to_annotations.get(_cls, [])
        annotation = JavaAnnotation(annotation_type, annotation_values)
        annotations.append(annotation)
        type_to_annotations[_cls] = annotations
        return _cls

    return decorator


def add_java_interface(java_interface: JClass | str, /) -> Callable[[Type[T]], Type[T]]:
    def decorator(_cls: Type[T]) -> Type[T]:
        from .translator import type_to_compiled_java_class, type_to_java_interfaces
        if _cls in type_to_compiled_java_class:
            raise RuntimeError('Cannot add an interface after a class has been compiled.')
        marker_interfaces = type_to_java_interfaces.get(_cls, [])
        marker_interfaces.append(java_interface)
        type_to_java_interfaces[_cls] = marker_interfaces
        return _cls

    return decorator


def copy_type_annotations(hinted_object, default_args, vargs_name, kwargs_name):
    from java.util import HashMap, Collections
    from ai.timefold.jpyinterpreter import TypeHint
    from .translator import type_to_compiled_java_class
    from typing import ClassVar

    out = HashMap()
    try:
        type_hints = get_type_hints(hinted_object, include_extras=True)
    except (AttributeError, NameError, TypeError):
        # Occurs if get_type_hints cannot resolve a forward reference
        type_hints = hinted_object.__annotations__ if hasattr(hinted_object, '__annotations__') else {}

    for name, type_hint in type_hints.items():
        if not isinstance(name, str):
            continue
        if name == vargs_name:
            out.put(name, TypeHint.withoutAnnotations(type_to_compiled_java_class[tuple]))
            continue
        if name == kwargs_name:
            out.put(name, TypeHint.withoutAnnotations(type_to_compiled_java_class[dict]))
            continue

        hint_type = type_hint
        hint_annotations = Collections.emptyList()
        if get_origin(type_hint) is Annotated:
            hint_type = get_args(type_hint)[0]
            hint_annotations = get_java_annotations(type_hint.__metadata__)  # noqa

        if get_origin(type_hint) is ClassVar:
            # Skip over class variables, since they are not
            # instance attributes
            continue

        if name in default_args:
            hint_type = Union[hint_type, type(default_args[name])]

        java_type_hint = get_java_type_hint(hint_type)
        out.put(name, java_type_hint.addAnnotations(hint_annotations))

    return out


def find_closest_common_ancestor(*cls_list: type):
    mros = [(list(cls.__mro__) if hasattr(cls, '__mro__') else [cls]) for cls in cls_list]
    track = defaultdict(int)
    while mros:
        for mro in mros:
            cur = mro.pop(0)
            track[cur] += 1
            if track[cur] == len(cls_list):
                return cur
            if len(mro) == 0:
                mros.remove(mro)
    return object


def get_java_type_hint(hint_type):
    from .translator import get_java_type_for_python_type, type_to_compiled_java_class
    from typing import get_args as get_generic_args
    from java.lang import Class as JavaClass
    from java.util import Collections
    from ai.timefold.jpyinterpreter import TypeHint
    from ai.timefold.jpyinterpreter.types import BuiltinTypes
    from ai.timefold.jpyinterpreter.types.wrappers import JavaObjectWrapper

    origin_type = get_origin(hint_type)
    if origin_type is None:
        # Happens for Callable[[parameter_types], return_type]
        if isinstance(hint_type, list) or isinstance(hint_type, tuple):
            return TypeHint(BuiltinTypes.BASE_TYPE, Collections.emptyList())
        # Not a generic type
        elif hint_type in type_to_compiled_java_class:
            return TypeHint(type_to_compiled_java_class[hint_type], Collections.emptyList())
        elif isinstance(hint_type, (JClass, JavaClass)):
            java_type = JavaObjectWrapper.getPythonTypeForClass(hint_type)
            type_to_compiled_java_class[hint_type] = java_type
            return TypeHint(java_type, Collections.emptyList())
        elif isinstance(hint_type, (type, str)):
            return TypeHint(get_java_type_for_python_type(hint_type), Collections.emptyList())
        else:
            return TypeHint(BuiltinTypes.BASE_TYPE, Collections.emptyList())

    generic_args = get_generic_args(hint_type)

    if origin_type is Union or origin_type is UnionType:
        union_types_excluding_none = []
        union_types_including_none = []
        for union_type in generic_args:
            union_types_including_none.append(union_type)
            if union_type == NoneType:
                continue
            union_types_excluding_none.append(union_type)

        return TypeHint(get_java_type_hint(find_closest_common_ancestor(*union_types_including_none)).type(),
                        Collections.emptyList(),
                        get_java_type_hint(find_closest_common_ancestor(*union_types_excluding_none)).type())

    origin_type_hint = get_java_type_hint(origin_type)
    generic_arg_type_hint_array = JArray(TypeHint)(len(generic_args))
    for i in range(len(generic_args)):
        generic_arg_type_hint_array[i] = get_java_type_hint(generic_args[i])

    return TypeHint(origin_type_hint.type(), Collections.emptyList(), generic_arg_type_hint_array,
                    origin_type_hint.type())


def get_java_annotations(annotated_metadata: List[Any]):
    from java.util import ArrayList
    out = ArrayList()
    for metadata in annotated_metadata:
        if not isinstance(metadata, JavaAnnotation):
            if isinstance(metadata, type) and issubclass(metadata, JavaAnnotation):
                try:
                    metadata = metadata()
                except TypeError as e:
                    raise ValueError(f'The annotation class {metadata.__name__} has required attributes.'
                                     f'Create an instance using {metadata.__name__}(...).') from e
            else:
                continue
        out.add(convert_java_annotation(metadata))
    return out


def convert_java_annotation(java_annotation: JavaAnnotation):
    from java.util import HashMap
    from ai.timefold.jpyinterpreter import AnnotationMetadata
    annotation_values = HashMap()
    for attribute_name, attribute_value in java_annotation.annotation_values.items():
        if isinstance(attribute_value, AnnotationValueSupplier):
            attribute_value = attribute_value.get_value()

        annotation_method = java_annotation.annotation_type.class_.getDeclaredMethod(attribute_name)
        attribute_type = annotation_method.getReturnType()
        java_attribute_value = convert_annotation_value(java_annotation.annotation_type, attribute_type,
                                                        attribute_name, attribute_value)
        if java_attribute_value is not None:
            annotation_values.put(attribute_name, java_attribute_value)
    return AnnotationMetadata(java_annotation.annotation_type.class_, annotation_values)


def convert_annotation_value(annotation_type: JClass, attribute_type: JClass, attribute_name: str, attribute_value: Any):
    from .translator import (get_java_type_for_python_type,
                             translate_python_bytecode_to_java_bytecode,
                             generate_proxy_class_for_translated_function)
    from jpype import JBoolean, JByte, JChar, JShort, JInt, JLong, JFloat, JDouble, JString, JArray
    from ai.timefold.jpyinterpreter import AnnotationMetadata
    from org.objectweb.asm import Type as ASMType

    if attribute_value is None:
        return None
    # See 9.6.1 of the Java spec for possible element values of annotations
    if attribute_type == JClass('boolean').class_:
        return JBoolean(attribute_value)
    elif attribute_type == JClass('byte').class_:
        return JByte(attribute_value)
    elif attribute_type == JClass('char').class_:
        return JChar(attribute_value)
    elif attribute_type == JClass('short').class_:
        return JShort(attribute_value)
    elif attribute_type == JClass('int').class_:
        return JInt(attribute_value)
    elif attribute_type == JClass('long').class_:
        return JLong(attribute_value)
    elif attribute_type == JClass('float').class_:
        return JFloat(attribute_value)
    elif attribute_type == JClass('double').class_:
        return JDouble(attribute_value)
    elif attribute_type == JClass('java.lang.String').class_:
        return JString(attribute_value)
    elif attribute_type == JClass('java.lang.Class').class_:
        if isinstance(attribute_value, ASMType):
            return attribute_value
        if isinstance(attribute_value, JClass('java.lang.Class')):
            return AnnotationMetadata.getValueAsType(attribute_value.getName())
        elif isinstance(attribute_value, type):
            out = get_java_type_for_python_type(attribute_value)
            return AnnotationMetadata.getValueAsType(out.getJavaTypeInternalName())
        elif isinstance(attribute_value, FunctionType):
            method = annotation_type.class_.getDeclaredMethod(attribute_name)
            generic_type = method.getGenericReturnType()
            try:
                function_type_and_generic_args = resolve_java_function_type_as_tuple(generic_type)
                instance = translate_python_bytecode_to_java_bytecode(attribute_value, *function_type_and_generic_args)
                return AnnotationMetadata.getValueAsType(generate_proxy_class_for_translated_function(
                    function_type_and_generic_args[0], instance).getName())
            except ValueError:
                raw_type = resolve_raw_type(generic_type.getActualTypeArguments()[0])
                instance = translate_python_bytecode_to_java_bytecode(attribute_value, raw_type)
                return AnnotationMetadata.getValueAsType(generate_proxy_class_for_translated_function(
                    raw_type, instance).getName())
        else:
            raise ValueError(f'Illegal value for {attribute_name} in annotation {annotation_type}: {attribute_value}')
    elif attribute_type.isEnum():
        return attribute_value
    elif attribute_type.isArray():
        dimensions = get_dimensions(attribute_type)
        component_type = get_component_type(attribute_type)
        return JArray(component_type, dims=dimensions)(convert_annotation_array_elements(annotation_type,
                                                                                         component_type.class_,
                                                                                         attribute_name,
                                                                                         attribute_value))
    elif JClass('java.lang.Annotation').class_.isAssignableFrom(attribute_type):
        if not isinstance(attribute_value, JavaAnnotation):
            raise ValueError(f'Illegal value for {attribute_name} in annotation {annotation_type}: {attribute_value}')
        return convert_java_annotation(attribute_value)
    else:
        raise ValueError(f'Illegal type for annotation element {attribute_type} for element named '
                         f'{attribute_name} on annotation type {annotation_type}.')


def resolve_java_function_type_as_tuple(function_class) -> Tuple[JClass]:
    from java.lang.reflect import ParameterizedType, WildcardType
    if isinstance(function_class, WildcardType):
        return resolve_java_type_as_tuple(function_class.getUpperBounds()[0])
    elif isinstance(function_class, ParameterizedType):
        return resolve_java_type_as_tuple(function_class.getActualTypeArguments()[0])
    else:
        raise ValueError(f'Unable to determine interface for type {function_class}')


def resolve_java_type_as_tuple(generic_type) -> Tuple[JClass]:
    from java.lang.reflect import ParameterizedType, WildcardType
    if isinstance(generic_type, WildcardType):
        return (*map(resolve_java_type_as_tuple, generic_type.getUpperBounds()),)
    elif isinstance(generic_type, ParameterizedType):
        return resolve_raw_types(generic_type.getRawType(), *generic_type.getActualTypeArguments())
    elif isinstance(generic_type, JClass):
        return (generic_type,)
    else:
        raise ValueError(f'Unable to determine interface for type {generic_type}')


def resolve_raw_types(*type_arguments) -> Tuple[JClass]:
    return (*map(resolve_raw_type, type_arguments),)


def resolve_raw_type(type_argument) -> JClass:
    from java.lang.reflect import ParameterizedType, WildcardType
    if isinstance(type_argument, ParameterizedType):
        return resolve_raw_type(type_argument.getRawType())
    elif isinstance(type_argument, WildcardType):
        return resolve_raw_type(type_argument.getUpperBounds()[0])
    return type_argument


def convert_annotation_array_elements(annotation_type: JClass, component_type: JClass, attribute_name: str,
                                      array_elements: List) -> List:
    out = []
    for item in array_elements:
        if isinstance(item, (list, tuple)):
            out.append(convert_annotation_array_elements(annotation_type, component_type, attribute_name, item))
        else:
            out.append(convert_annotation_value(annotation_type, component_type, attribute_name, item))
    return out


def get_dimensions(array_type: JClass) -> int:
    if array_type.getComponentType() is None:
        return 0
    return get_dimensions(array_type.getComponentType()) + 1


def get_component_type(array_type: JClass) -> JClass:
    if not array_type.getComponentType().isArray():
        return JClass(array_type.getComponentType().getCanonicalName())
    return get_component_type(array_type.getComponentType())


def erase_generic_args(python_type):
    from typing import get_origin
    if isinstance(python_type, type):
        out = python_type
        if get_origin(out) is not None:
            return get_origin(out)
        return out
    elif isinstance(python_type, str):
        try:
            generics_start = python_type.index('[')
            return python_type[generics_start:-2]
        except ValueError:
            return python_type
    else:
        raise ValueError
