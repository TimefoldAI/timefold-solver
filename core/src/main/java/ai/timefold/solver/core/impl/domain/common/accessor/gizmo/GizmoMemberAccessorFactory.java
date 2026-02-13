package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;

import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.ConstructorDesc;

public class GizmoMemberAccessorFactory {
    /**
     * Returns the generated class name for a given member.
     * (Here as accessing any method of GizmoMemberAccessorImplementor
     * will try to load Gizmo code)
     *
     * @param member The member to get the generated class name for
     * @return The generated class name for member
     */
    public static String getGeneratedClassName(Member member) {
        String memberName = Objects.requireNonNullElse(ReflectionHelper.getGetterPropertyName(member), member.getName());
        String memberType = (member instanceof Field) ? "Field" : "Method";

        return member.getDeclaringClass().getName() + "$Timefold$MemberAccessor$" + memberType + "$" + memberName;
    }

    /**
     *
     * @param member never null
     * @param annotationClass may be null if the member is not annotated
     * @param gizmoClassLoader never null
     * @param accessorInfo additional information of the accessor
     * @return never null
     */
    public static MemberAccessor buildGizmoMemberAccessor(Member member, Class<? extends Annotation> annotationClass,
            AccessorInfo accessorInfo, GizmoClassLoader gizmoClassLoader) {
        return GizmoMemberAccessorImplementor.createAccessorFor(member, annotationClass, accessorInfo, gizmoClassLoader);
    }

    public static boolean isGizmoSupported(GizmoClassLoader gizmoClassLoader) {
        return switch (gizmoClassLoader.getGizmoSupportStatus()) {
            case SUPPORTED -> true;
            case UNSUPPORTED -> false;
            case UNKNOWN -> {
                var classPackage = GizmoMemberAccessorFactory.class.getPackage().getName();
                var bytecodeHolder = new AtomicReference<byte[]>();
                var gizmo = Gizmo.create((className, bytecode) -> {
                    bytecodeHolder.set(bytecode);
                });

                var classDesc = ClassDesc.of("%s.Test".formatted(classPackage));
                gizmo.class_(classDesc, classCreator -> {
                    classCreator.constructor(ConstructorDesc.of(classDesc), constructorCreator -> {
                        constructorCreator.public_();
                        var this_ = constructorCreator.this_();
                        constructorCreator.body(constructor -> {
                            constructor.invokeSpecial(ConstructorDesc.of(Object.class), this_);
                            constructor.return_();
                        });
                    });
                });
                try {
                    var generatedClass = MethodHandles.lookup().defineHiddenClass(bytecodeHolder.get(), true).lookupClass();
                    var instance = generatedClass.getConstructor().newInstance();
                    if (instance == null) {
                        // Should be impossible, but a native image might decide to optimize out
                        // instance if it is unused
                        gizmoClassLoader.setGizmoSupportStatus(GizmoSupportStatus.UNSUPPORTED);
                        yield false;
                    } else {
                        gizmoClassLoader.setGizmoSupportStatus(GizmoSupportStatus.SUPPORTED);
                        yield true;
                    }
                } catch (IllegalAccessException | NoSuchMethodException | InstantiationException
                        | InvocationTargetException | Error e) {
                    // Note: GraalVM will throw a com.oracle.svm.core.jdk.UnsupportedFeatureError
                    //       on defineHiddenClass, so we also catch "Error" here so we don't need
                    //       to add GraalVM as a library
                    gizmoClassLoader.setGizmoSupportStatus(GizmoSupportStatus.UNSUPPORTED);
                    yield false;
                }
            }
        };

    }

    private GizmoMemberAccessorFactory() {
    }
}
