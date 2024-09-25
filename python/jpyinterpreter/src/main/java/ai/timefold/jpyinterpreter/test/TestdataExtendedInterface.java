package ai.timefold.jpyinterpreter.test;

import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

/**
 * Not a real interface; in main sources instead of test sources
 * so a Python test can use it.
 */
public interface TestdataExtendedInterface {
    PythonString stringMethod(PythonString name);

    PythonInteger intMethod(PythonInteger value);

    static String getString(TestdataExtendedInterface instance, String name) {
        return instance.stringMethod(PythonString.valueOf(name)).value;
    }

    static int getInt(TestdataExtendedInterface instance, int value) {
        return instance.intMethod(PythonInteger.valueOf(value)).value.intValue();
    }
}
