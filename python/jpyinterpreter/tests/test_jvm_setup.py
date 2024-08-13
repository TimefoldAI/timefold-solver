import pytest
import jpyinterpreter
import jpype

class Java17Runtime:
    def version(self):
        class Version:
            def feature(self):
                return 17
        return Version()

class Java10Runtime:
    def version(self):
        class Version:
            def feature(self):
                return 10
        return Version()

class Java9Runtime:
    def version(self):
        class Version:
            def major(self):
                return 17
        return Version()

class Java8Runtime:
    pass

def test_jvm_setup():
    jpyinterpreter.ensure_valid_jvm(Java17Runtime())
    with pytest.raises(jpyinterpreter.InvalidJVMVersionError):
        jpyinterpreter.ensure_valid_jvm(Java8Runtime())
    with pytest.raises(jpyinterpreter.InvalidJVMVersionError):
        jpyinterpreter.ensure_valid_jvm(Java9Runtime())
    with pytest.raises(jpyinterpreter.InvalidJVMVersionError):
        jpyinterpreter.ensure_valid_jvm(Java10Runtime())

def jvm_not_found():
    raise jpype.JVMNotFoundException()

def test_jvm_get_default_jvm_path():
    jpyinterpreter.get_default_jvm_path()
    with pytest.raises(jpyinterpreter.InvalidJVMVersionError):
        jpyinterpreter.get_default_jvm_path(jvm_not_found)
