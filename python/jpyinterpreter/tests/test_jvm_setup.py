import pytest
import jpyinterpreter

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