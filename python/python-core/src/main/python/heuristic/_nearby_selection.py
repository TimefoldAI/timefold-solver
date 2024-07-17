from typing import Callable, TypeVar
from .._timefold_java_interop import ensure_init, register_java_class

Origin_ = TypeVar('Origin_')
Destination_ = TypeVar('Destination_')


def nearby_distance_meter(distance_function: Callable[[Origin_, Destination_], float], /) \
        -> Callable[[Origin_, Destination_], float]:
    """
    Decorate a function so it can act as a distance meter for nearby selection.

    The function must have the signature ``(Origin_, Destination_) -> float``.

    The function should measure the distance from the origin to the destination.
    The distance can be in any unit, such a meters, foot, seconds or milliseconds.
    For example, vehicle routing often uses driving time in seconds.

    Distances can be asymmetrical:
    the distance from an origin to a destination often differs from the distance from that destination to that origin.

    Implementations are expected to be stateless.
    The solver may choose to reuse instances.

    """
    ensure_init()
    from _jpyinterpreter import translate_python_bytecode_to_java_bytecode, generate_proxy_class_for_translated_function
    from ai.timefold.solver.core.impl.heuristic.selector.common.nearby import NearbyDistanceMeter  # noqa
    java_class = generate_proxy_class_for_translated_function(NearbyDistanceMeter,
                                                              translate_python_bytecode_to_java_bytecode(
                                                                  distance_function,
                                                                  NearbyDistanceMeter))
    return register_java_class(distance_function, java_class)


__all__ = ['nearby_distance_meter']
