package ai.timefold.jpyinterpreter;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public record AnnotationMetadata(Class<? extends Annotation> annotationType, Map<String, Object> annotationValueMap) {
    public void addAnnotationTo(ClassVisitor classVisitor) {
        visitAnnotation(classVisitor.visitAnnotation(Type.getDescriptor(annotationType), true));
    }

    public void addAnnotationTo(FieldVisitor fieldVisitor) {
        visitAnnotation(fieldVisitor.visitAnnotation(Type.getDescriptor(annotationType), true));
    }

    public void addAnnotationTo(MethodVisitor methodVisitor) {
        visitAnnotation(methodVisitor.visitAnnotation(Type.getDescriptor(annotationType), true));
    }

    public static List<AnnotationMetadata> getAnnotationListWithoutRepeatable(List<AnnotationMetadata> metadata) {
        List<AnnotationMetadata> out = new ArrayList<>();
        Map<Class<? extends Annotation>, List<AnnotationMetadata>> repeatableAnnotationMap = new LinkedHashMap<>();
        for (AnnotationMetadata annotation : metadata) {
            Repeatable repeatable = annotation.annotationType().getAnnotation(Repeatable.class);
            if (repeatable == null) {
                out.add(annotation);
                continue;
            }
            var annotationContainer = repeatable.value();
            repeatableAnnotationMap.computeIfAbsent(annotationContainer,
                    ignored -> new ArrayList<>()).add(annotation);
        }
        for (var entry : repeatableAnnotationMap.entrySet()) {
            out.add(new AnnotationMetadata(entry.getKey(),
                    Map.of("value", entry.getValue().toArray(AnnotationMetadata[]::new))));
        }
        return out;
    }

    public static Type getValueAsType(String className) {
        return Type.getType("L" + className.replace('.', '/') + ";");
    }

    private void visitAnnotation(AnnotationVisitor annotationVisitor) {
        for (var entry : annotationValueMap.entrySet()) {
            var annotationAttributeName = entry.getKey();
            var annotationAttributeValue = entry.getValue();

            visitAnnotationAttribute(annotationVisitor, annotationAttributeName, annotationAttributeValue);
        }
        annotationVisitor.visitEnd();
    }

    private void visitAnnotationAttribute(AnnotationVisitor annotationVisitor, String attributeName, Object attributeValue) {
        if (attributeValue instanceof Number
                || attributeValue instanceof Boolean
                || attributeValue instanceof Character
                || attributeValue instanceof String) {
            annotationVisitor.visit(attributeName, attributeValue);
            return;
        }

        if (attributeValue instanceof Type type) {
            annotationVisitor.visit(attributeName, type);
            return;
        }

        if (attributeValue instanceof AnnotationMetadata annotationMetadata) {
            annotationMetadata.visitAnnotation(
                    annotationVisitor.visitAnnotation(attributeName, Type.getDescriptor(annotationMetadata.annotationType)));
            return;
        }

        if (attributeValue instanceof Enum<?> enumValue) {
            annotationVisitor.visitEnum(attributeName, Type.getDescriptor(enumValue.getClass()),
                    enumValue.name());
            return;
        }

        if (attributeValue.getClass().isArray()) {
            var arrayAnnotationVisitor = annotationVisitor.visitArray(attributeName);
            var arrayLength = Array.getLength(attributeValue);
            for (int i = 0; i < arrayLength; i++) {
                visitAnnotationAttribute(arrayAnnotationVisitor, attributeName, Array.get(attributeValue, i));
            }
            arrayAnnotationVisitor.visitEnd();
            return;
        }
        throw new IllegalArgumentException("Annotation of type %s has an illegal value %s for attribute %s."
                .formatted(annotationType, attributeValue, attributeName));
    }
}
