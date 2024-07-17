package ai.timefold.jpyinterpreter.util.arguments;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

public class ArgumentSpecTest {
    @Test
    public void testSpec() {
        ArgumentSpec<?> current = ArgumentSpec.forFunctionReturning("myFunction", PythonLikeTuple.class.getName());

        List<String> argumentNameList = new ArrayList<>();
        List<PythonInteger> argumentValueList = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            List<PythonLikeObject> positionalArguments = new ArrayList<>(argumentValueList);
            Map<PythonString, PythonLikeObject> keywordArguments = new HashMap<>();

            current.extractArgumentList(positionalArguments, keywordArguments);
            while (!positionalArguments.isEmpty()) {
                int toRemove = positionalArguments.size() - 1;
                PythonLikeObject removed = positionalArguments.remove(toRemove);
                keywordArguments.put(PythonString.valueOf(argumentNameList.get(toRemove)), removed);
                List<PythonLikeObject> out = current.extractArgumentList(positionalArguments, keywordArguments);
                assertThat(out).containsExactlyElementsOf(argumentValueList);
            }

            current = current.addArgument("arg" + i, PythonInteger.class.getName());
            argumentNameList.add("arg" + i);
            argumentValueList.add(PythonInteger.valueOf(i));
        }
    }

    @Test
    public void testSpecWithDefaults() {
        ArgumentSpec<?> current = ArgumentSpec.forFunctionReturning("myFunction", PythonLikeTuple.class.getName());

        List<String> argumentNameList = new ArrayList<>();
        List<PythonInteger> argumentValueList = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            for (int missingArgs = 0; missingArgs < i; missingArgs++) {
                List<PythonLikeObject> positionalArguments =
                        new ArrayList<>(argumentValueList.subList(0, argumentValueList.size() - missingArgs));
                Map<PythonString, PythonLikeObject> keywordArguments = new HashMap<>();

                current.extractArgumentList(positionalArguments, keywordArguments);
                while (!positionalArguments.isEmpty()) {
                    int toRemove = positionalArguments.size() - 1;
                    PythonLikeObject removed = positionalArguments.remove(toRemove);
                    keywordArguments.put(PythonString.valueOf(argumentNameList.get(toRemove)), removed);
                    List<PythonLikeObject> out = current.extractArgumentList(positionalArguments, keywordArguments);
                    List<PythonInteger> expected =
                            new ArrayList<>(argumentValueList.subList(0, argumentValueList.size() - missingArgs));
                    for (int j = i - missingArgs; j < i; j++) {
                        expected.add(PythonInteger.valueOf(-j));
                    }
                    assertThat(out).containsExactlyElementsOf(expected);
                }
            }

            current = current.addArgument("arg" + i, PythonInteger.class.getName(), PythonInteger.valueOf(-i));
            argumentNameList.add("arg" + i);
            argumentValueList.add(PythonInteger.valueOf(i));
        }
    }

    @Test
    public void testSpecMissingArgument() {
        ArgumentSpec<?> current = ArgumentSpec.forFunctionReturning("myFunction", PythonLikeTuple.class.getName())
                .addArgument("_arg0", PythonInteger.class.getName());

        List<String> argumentNameList = new ArrayList<>();
        List<PythonInteger> argumentValueList = new ArrayList<>();

        for (int i = 0; i < 19; i++) {
            ArgumentSpec<?> finalCurrent = current;

            List<PythonLikeObject> positionalArguments = new ArrayList<>(argumentValueList);
            Map<PythonString, PythonLikeObject> keywordArguments = new HashMap<>();
            assertThatCode(() -> finalCurrent.extractArgumentList(positionalArguments, keywordArguments))
                    .isInstanceOf(TypeError.class)
                    .hasMessageContaining("myFunction() missing 1 required positional argument: '");
            while (!positionalArguments.isEmpty()) {
                int toRemove = positionalArguments.size() - 1;
                PythonLikeObject removed = positionalArguments.remove(toRemove);
                keywordArguments.put(PythonString.valueOf(argumentNameList.get(toRemove)), removed);

                assertThatCode(() -> finalCurrent.extractArgumentList(positionalArguments, keywordArguments))
                        .isInstanceOf(TypeError.class)
                        .hasMessageContaining("myFunction() missing 1 required positional argument: '");
            }

            current = current.addArgument("arg" + i, PythonInteger.class.getName());
            argumentNameList.add("arg" + i);
            argumentValueList.add(PythonInteger.valueOf(i));
        }
    }

    @Test
    public void testSpecExtraArgument() {
        ArgumentSpec<?> current = ArgumentSpec.forFunctionReturning("myFunction", PythonLikeTuple.class.getName());

        List<String> argumentNameList = new ArrayList<>();
        List<PythonInteger> argumentValueList = new ArrayList<>();
        argumentNameList.add("_arg0");
        argumentValueList.add(PythonInteger.valueOf(-1));

        for (int i = 0; i < 20; i++) {
            ArgumentSpec<?> finalCurrent = current;

            List<PythonLikeObject> positionalArguments = new ArrayList<>(argumentValueList);
            Map<PythonString, PythonLikeObject> keywordArguments = new HashMap<>();
            String[] possibleErrorMessages = new String[2 + i];
            possibleErrorMessages[0] = "myFunction() takes " + i + " positional arguments but " + (i + 1) + " were given";
            possibleErrorMessages[1] = "myFunction() got an unexpected keyword argument '_arg0'";
            for (int arg = 0; arg < i; arg++) {
                possibleErrorMessages[2 + arg] = "myFunction() got multiple values for argument 'arg" + arg + "'";
            }

            assertThatCode(() -> finalCurrent.extractArgumentList(positionalArguments, keywordArguments))
                    .isInstanceOf(TypeError.class)
                    .extracting(Throwable::getMessage, as(InstanceOfAssertFactories.STRING))
                    .containsAnyOf(possibleErrorMessages);
            while (!positionalArguments.isEmpty()) {
                int toRemove = positionalArguments.size() - 1;
                PythonLikeObject removed = positionalArguments.remove(toRemove);
                keywordArguments.put(PythonString.valueOf(argumentNameList.get(toRemove)), removed);

                assertThatCode(() -> finalCurrent.extractArgumentList(positionalArguments, keywordArguments))
                        .isInstanceOf(TypeError.class)
                        .extracting(Throwable::getMessage, as(InstanceOfAssertFactories.STRING))
                        .containsAnyOf(possibleErrorMessages);
            }

            current = current.addArgument("arg" + i, PythonInteger.class.getName());
            argumentNameList.add("arg" + i);
            argumentValueList.add(PythonInteger.valueOf(i));
        }
    }
}
