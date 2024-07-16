import pathlib
import jpype
import stubgenj

jars = list(map(str, pathlib.Path('target/dependency').glob('**/*.jar')))

jpype.startJVM(classpath=jars, convertStrings=True)

import jpype.imports  # noqa
import ai.timefold.solver.core.api  # noqa
import ai.timefold.solver.core.config  # noqa
import ai.timefold.jpyinterpreter  # noqa
import java.lang # noqa
import java.time # noqa
import java.util # noqa

stubgenj.generateJavaStubs([java.lang, java.time, java.util, ai.timefold.solver.core.api,
                            ai.timefold.solver.core.config, ai.timefold.jpyinterpreter],
                           useStubsSuffix=True)

