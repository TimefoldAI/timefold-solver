import jpype.imports
import jpyinterpreter

jpyinterpreter.init(path=['target/example-1.0.0.jar'])

from java.util.function import Function

from org.acme import MyClass


def time(iterations, function, argument):
    from timeit import default_timer as timer
    java_function = jpyinterpreter.translate_python_bytecode_to_java_bytecode(function, Function)
    
    start = timer()

    result = MyClass.iterate(iterations, argument, function)
    
    end = timer()

    print(f"Python Result: {result}")
    print(f"Time for Python: {end - start}s")

    start = timer()

    result = MyClass.iterate(iterations, argument, java_function)

    end = timer()

    print(f"Java Result (translated from Python bytecode): {result}")
    print(f"Time for Java (translated from Python bytecode): {end - start}s")


def test1(arg):
    return arg + 1

def test2(arg):
    index = 0
    while index <= 10:
        index += 1
    return arg + 1

def test3(arg):
    index = 0
    while index <= 100:
        index += 1
    return arg + 1

time(100000, test1, 0)
time(100000, test2, 0)
time(100000, test3, 0)
