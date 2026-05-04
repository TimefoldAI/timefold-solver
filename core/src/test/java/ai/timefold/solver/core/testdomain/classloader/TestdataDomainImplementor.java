package ai.timefold.solver.core.testdomain.classloader;

import java.lang.constant.ClassDesc;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testutil.CodeAssertable;

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.GenericType;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.TypeArgument;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

public class TestdataDomainImplementor {

    private static final String PACKAGE = "ai.timefold.solver.core.testdomain";
    private static final ClassDesc STRING_DESC = ClassDesc.of(String.class.getCanonicalName());
    private static final ClassDesc INT_DESC = ClassDesc.ofDescriptor("I");
    private static final ClassDesc BOOLEAN_DESC = ClassDesc.ofDescriptor("Z");

    static final FieldDesc CODE_FIELD_DESC;

    static {
        try {
            var codeField = TestdataObject.class.getDeclaredField("code");
            CODE_FIELD_DESC = FieldDesc.of(codeField);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, byte[]> generateClasses() {
        var classBytecodeHolder = new HashMap<String, byte[]>();
        ClassOutput classOutput = (path, byteCode) -> {
            String className = path.substring(0, path.length() - ".class".length()).replace('/', '.');
            classBytecodeHolder.put(className, byteCode);
        };

        var gizmo = Gizmo.create(classOutput);

        generateTestdataObject(gizmo);
        generateTestdataValue(gizmo);
        generateTestdataEntity(gizmo);
        generateTestdataSolution(gizmo);

        return classBytecodeHolder;
    }

    private static void generateTestdataObject(Gizmo gizmo) {
        var className = "%s.TestdataObject".formatted(PACKAGE);
        gizmo.class_(className, classCreator -> {
            classCreator.public_();
            classCreator.extends_(Object.class);
            classCreator.implements_(CodeAssertable.class);

            // @PlanningId protected String code;
            FieldDesc codeField = classCreator.field("code", fieldCreator -> {
                fieldCreator.protected_();
                fieldCreator.setType(String.class);
                fieldCreator.addAnnotation(PlanningId.class);
            });

            // public TestdataObject()
            classCreator.constructor(constructorCreator -> {
                constructorCreator.public_();
                constructorCreator.body(blockCreator -> {
                    blockCreator.invokeSpecial(ConstructorDesc.of(Object.class), constructorCreator.this_());
                    blockCreator.return_();
                });
            });

            // public TestdataObject(String code)
            classCreator.constructor(constructorCreator -> {
                constructorCreator.public_();
                Var code = constructorCreator.parameter("code", String.class);
                constructorCreator.body(blockCreator -> {
                    var thisRef = constructorCreator.this_();
                    blockCreator.invokeSpecial(ConstructorDesc.of(Object.class), thisRef);
                    blockCreator.set(thisRef.field(codeField), code);
                    blockCreator.return_();
                });
            });

            // public String getCode()
            classCreator.method("getCode", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(String.class);
                methodCreator.body(blockCreator -> {
                    blockCreator.return_(methodCreator.this_().field(codeField));
                });
            });

            // public void setCode(String code)
            classCreator.method("setCode", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(void.class);
                Var code = methodCreator.parameter("code", String.class);
                methodCreator.body(blockCreator -> {
                    blockCreator.set(methodCreator.this_().field(codeField), code);
                    blockCreator.return_();
                });
            });

            // public String toString()
            classCreator.method("toString", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(String.class);
                methodCreator.body(blockCreator -> {
                    blockCreator.return_(methodCreator.this_().field(codeField));
                });
            });
        });
    }

    private static void generateTestdataValue(Gizmo gizmo) {
        var className = "%s.TestdataValue".formatted(PACKAGE);
        var superClassDesc = ClassDesc.of(TestdataObject.class.getCanonicalName());
        gizmo.class_(className, classCreator -> {
            classCreator.public_();
            classCreator.extends_(superClassDesc);

            // public TestdataValue()
            classCreator.constructor(constructorCreator -> {
                constructorCreator.public_();
                constructorCreator.body(blockCreator -> {
                    blockCreator.invokeSpecial(ConstructorDesc.of(superClassDesc), constructorCreator.this_());
                    blockCreator.return_();
                });
            });

            // public TestdataValue(String code)
            classCreator.constructor(constructorCreator -> {
                constructorCreator.public_();
                Var code = constructorCreator.parameter("code", String.class);
                constructorCreator.body(blockCreator -> {
                    var thisRef = constructorCreator.this_();
                    blockCreator.invokeSpecial(ConstructorDesc.of(superClassDesc, STRING_DESC), thisRef, code);
                    blockCreator.return_();
                });
            });
        });
    }

    private static void generateTestdataEntity(Gizmo gizmo) {
        var className = "%s.TestdataEntity".formatted(PACKAGE);
        var superClassDesc = ClassDesc.of(TestdataObject.class.getCanonicalName());
        var valueClassDesc = ClassDesc.of(TestdataValue.class.getCanonicalName());
        var solutionClassDesc = ClassDesc.of(TestdataSolution.class.getCanonicalName());

        gizmo.class_(className, classCreator -> {
            classCreator.public_();
            classCreator.extends_(superClassDesc);
            classCreator.addAnnotation(PlanningEntity.class);

            // public static final String VALUE_FIELD = "value";
            classCreator.staticField("VALUE_FIELD", fieldCreator -> {
                fieldCreator.public_();
                fieldCreator.final_();
                fieldCreator.setType(String.class);
                fieldCreator.setInitial("value");
            });

            // private TestdataValue value;
            FieldDesc valueField = classCreator.field("value", fieldCreator -> {
                fieldCreator.private_();
                fieldCreator.setType(valueClassDesc);
            });

            // public TestdataEntity()
            classCreator.constructor(constructorCreator -> {
                constructorCreator.public_();
                constructorCreator.body(blockCreator -> {
                    blockCreator.invokeSpecial(ConstructorDesc.of(superClassDesc), constructorCreator.this_());
                    blockCreator.return_();
                });
            });

            // public TestdataEntity(String code)
            classCreator.constructor(constructorCreator -> {
                constructorCreator.public_();
                Var code = constructorCreator.parameter("code", String.class);
                constructorCreator.body(blockCreator -> {
                    var thisRef = constructorCreator.this_();
                    blockCreator.invokeSpecial(ConstructorDesc.of(superClassDesc, STRING_DESC), thisRef, code);
                    blockCreator.return_();
                });
            });

            // public TestdataEntity(String code, TestdataValue value)
            classCreator.constructor(constructorCreator -> {
                constructorCreator.public_();
                Var code = constructorCreator.parameter("code", String.class);
                Var value = constructorCreator.parameter("value", valueClassDesc);
                constructorCreator.body(blockCreator -> {
                    var thisRef = constructorCreator.this_();
                    blockCreator.invokeSpecial(ConstructorDesc.of(superClassDesc, STRING_DESC), thisRef, code);
                    blockCreator.set(thisRef.field(valueField), value);
                    blockCreator.return_();
                });
            });

            // @PlanningVariable(valueRangeProviderRefs = "valueRange") public TestdataValue getValue()
            classCreator.method("getValue", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(valueClassDesc);
                methodCreator.addAnnotation(PlanningVariable.class, annotationCreator -> {
                    annotationCreator.addArray("valueRangeProviderRefs", "valueRange");
                });
                methodCreator.body(blockCreator -> {
                    blockCreator.return_(methodCreator.this_().field(valueField));
                });
            });

            // public void setValue(TestdataValue value)
            classCreator.method("setValue", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(void.class);
                Var value = methodCreator.parameter("value", valueClassDesc);
                methodCreator.body(blockCreator -> {
                    blockCreator.set(methodCreator.this_().field(valueField), value);
                    blockCreator.return_();
                });
            });

            // public void updateValue()
            classCreator.method("updateValue", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(void.class);
                methodCreator.body(blockCreator -> {
                    var thisRef = methodCreator.this_();
                    var currentValue = thisRef.field(valueField);

                    // value.code + "/" + value.code
                    var codeField = currentValue.field(CODE_FIELD_DESC);
                    var slash = Const.of("/");
                    var newCode = blockCreator.invokeVirtual(
                            MethodDesc.of(String.class, "concat", String.class, String.class),
                            blockCreator.invokeVirtual(
                                    MethodDesc.of(String.class, "concat", String.class, String.class),
                                    codeField, slash),
                            codeField);

                    // new TestdataValue(newCode)
                    var newValue = blockCreator.new_(ConstructorDesc.of(valueClassDesc, STRING_DESC), newCode);

                    // this.value = newValue
                    blockCreator.set(thisRef.field(valueField), newValue);
                    blockCreator.return_();
                });
            });
        });
    }

    private static void generateTestdataSolution(Gizmo gizmo) {
        var className = "%s.TestdataSolution".formatted(PACKAGE);
        var superClassDesc = ClassDesc.of(TestdataObject.class.getCanonicalName());
        var entityClassDesc = ClassDesc.of(TestdataEntity.class.getCanonicalName());
        var valueClassDesc = ClassDesc.of(TestdataValue.class.getCanonicalName());
        var thisClassDesc = ClassDesc.of(className);

        gizmo.class_(className, classCreator -> {
            classCreator.public_();
            classCreator.extends_(superClassDesc);
            classCreator.addAnnotation(PlanningSolution.class);

            // private List<TestdataValue> valueList;
            var valueListField = classCreator.field("valueList", fieldCreator -> {
                fieldCreator.private_();
                fieldCreator.setType(java.util.List.class);
            });

            // private List<TestdataEntity> entityList;
            var entityListField = classCreator.field("entityList", fieldCreator -> {
                fieldCreator.private_();
                fieldCreator.setType(java.util.List.class);
            });

            // private SimpleScore score;
            var scoreField = classCreator.field("score", fieldCreator -> {
                fieldCreator.private_();
                fieldCreator.setType(SimpleScore.class);
            });

            // public TestdataSolution()
            classCreator.constructor(constructorCreator -> {
                constructorCreator.public_();
                constructorCreator.body(blockCreator -> {
                    blockCreator.invokeSpecial(ConstructorDesc.of(superClassDesc), constructorCreator.this_());
                    blockCreator.return_();
                });
            });

            // public TestdataSolution(String code)
            classCreator.constructor(constructorCreator -> {
                constructorCreator.public_();
                var code = constructorCreator.parameter("code", String.class);
                constructorCreator.body(blockCreator -> {
                    var thisRef = constructorCreator.this_();
                    blockCreator.invokeSpecial(ConstructorDesc.of(superClassDesc, STRING_DESC), thisRef, code);
                    blockCreator.return_();
                });
            });

            // @ValueRangeProvider(id = "valueRange") @ProblemFactCollectionProperty public List<TestdataValue> getValueList()
            classCreator.method("getValueList", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(GenericType.of(java.util.List.class, List.of(TypeArgument.of(valueClassDesc))));
                methodCreator.addAnnotation(ValueRangeProvider.class, annotationCreator -> {
                    annotationCreator.add("id", "valueRange");
                });
                methodCreator.addAnnotation(ProblemFactCollectionProperty.class);
                methodCreator.body(blockCreator -> {
                    blockCreator.return_(methodCreator.this_().field(valueListField));
                });
            });

            // public void setValueList(List<TestdataValue> valueList)
            classCreator.method("setValueList", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(void.class);
                var valueList = methodCreator.parameter("valueList", java.util.List.class);
                methodCreator.body(blockCreator -> {
                    blockCreator.set(methodCreator.this_().field(valueListField), valueList);
                    blockCreator.return_();
                });
            });

            // @PlanningEntityCollectionProperty public List<TestdataEntity> getEntityList()
            classCreator.method("getEntityList", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(GenericType.of(java.util.List.class, List.of(TypeArgument.of(entityClassDesc))));
                methodCreator.addAnnotation(PlanningEntityCollectionProperty.class);
                methodCreator.body(blockCreator -> {
                    blockCreator.return_(methodCreator.this_().field(entityListField));
                });
            });

            // public void setEntityList(List<TestdataEntity> entityList)
            classCreator.method("setEntityList", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(void.class);
                var entityList = methodCreator.parameter("entityList", java.util.List.class);
                methodCreator.body(blockCreator -> {
                    blockCreator.set(methodCreator.this_().field(entityListField), entityList);
                    blockCreator.return_();
                });
            });

            // @PlanningScore public SimpleScore getScore()
            classCreator.method("getScore", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(SimpleScore.class);
                methodCreator.addAnnotation(PlanningScore.class);
                methodCreator.body(blockCreator -> {
                    blockCreator.return_(methodCreator.this_().field(scoreField));
                });
            });

            // public void setScore(SimpleScore score)
            classCreator.method("setScore", methodCreator -> {
                methodCreator.public_();
                methodCreator.returning(void.class);
                var score = methodCreator.parameter("score", SimpleScore.class);
                methodCreator.body(blockCreator -> {
                    blockCreator.set(methodCreator.this_().field(scoreField), score);
                    blockCreator.return_();
                });
            });
        });
    }
}
