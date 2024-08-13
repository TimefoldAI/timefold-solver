"""
This module acts as an interface to the Python bytecode to Java bytecode interpreter
"""
from .jvm_setup import init, set_class_output_directory, get_path, ensure_valid_jvm, get_default_jvm_path, \
    InvalidJVMVersionError
from .annotations import JavaAnnotation, AnnotationValueSupplier, add_class_annotation, add_java_interface
from .conversions import (convert_to_java_python_like_object, unwrap_python_like_object,
                          update_python_object_from_java, is_c_native, add_python_java_type_mapping)
from .translator import (translate_python_bytecode_to_java_bytecode,
                         translate_python_class_to_java_class,
                         force_update_type,
                         generate_proxy_class_for_translated_function,
                         generate_proxy_class_for_translated_class,
                         get_java_type_for_python_type, as_java,
                         as_untyped_java, as_typed_java,
                         is_current_python_version_supported,
                         check_current_python_version_supported, is_python_version_supported,
                         _force_as_java_generator)
