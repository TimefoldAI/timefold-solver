package ai.timefold.jpyinterpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.jpyinterpreter.types.PythonLikeType;

public record TypeHint(PythonLikeType type, List<AnnotationMetadata> annotationList, TypeHint[] genericArgs,
        PythonLikeType javaGetterType) {
    public TypeHint {
        annotationList = Collections.unmodifiableList(annotationList);
    }

    public TypeHint(PythonLikeType type, List<AnnotationMetadata> annotationList) {
        this(type, annotationList, null, type);
    }

    public TypeHint(PythonLikeType type, List<AnnotationMetadata> annotationList, PythonLikeType javaGetterType) {
        this(type, annotationList, null, javaGetterType);
    }

    public TypeHint addAnnotations(List<AnnotationMetadata> addedAnnotations) {
        List<AnnotationMetadata> combinedAnnotations = new ArrayList<>(annotationList.size() + addedAnnotations.size());
        combinedAnnotations.addAll(annotationList);
        combinedAnnotations.addAll(addedAnnotations);
        return new TypeHint(type, combinedAnnotations, genericArgs, javaGetterType);
    }

    public static TypeHint withoutAnnotations(PythonLikeType type) {
        return new TypeHint(type, Collections.emptyList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeHint typeHint)) {
            return false;
        }
        return Objects.equals(type, typeHint.type) && Objects.deepEquals(genericArgs,
                typeHint.genericArgs)
                && Objects.equals(javaGetterType,
                        typeHint.javaGetterType)
                && Objects.equals(annotationList, typeHint.annotationList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, annotationList, Arrays.hashCode(genericArgs), javaGetterType);
    }

    @Override
    public String toString() {
        return "TypeHint{" +
                "type=" + type +
                ", annotationList=" + annotationList +
                ", genericArgs=" + Arrays.toString(genericArgs) +
                ", javaGetterType=" + javaGetterType +
                '}';
    }
}
