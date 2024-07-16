package ai.timefold.jpyinterpreter;

import ai.timefold.jpyinterpreter.types.PythonLikeType;

public record FieldDescriptor(String pythonFieldName, String javaFieldName, String declaringClassInternalName,
        String javaFieldTypeDescriptor, PythonLikeType fieldPythonLikeType,
        boolean isTrueFieldDescriptor, boolean isJavaType) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldDescriptor that = (FieldDescriptor) o;
        return pythonFieldName.equals(that.pythonFieldName) && javaFieldName.equals(that.javaFieldName)
                && declaringClassInternalName.equals(that.declaringClassInternalName)
                && javaFieldTypeDescriptor.equals(that.javaFieldTypeDescriptor)
                && fieldPythonLikeType.equals(that.fieldPythonLikeType)
                && isTrueFieldDescriptor == that.isTrueFieldDescriptor;
    }

    @Override
    public String toString() {
        return "FieldDescriptor{" +
                "pythonFieldName='" + pythonFieldName + '\'' +
                ", javaFieldName='" + javaFieldName + '\'' +
                ", declaringClassInternalName='" + declaringClassInternalName + '\'' +
                ", javaFieldTypeDescriptor='" + javaFieldTypeDescriptor + '\'' +
                ", fieldPythonLikeType=" + fieldPythonLikeType +
                ", isTrueFieldDescriptor=" + isTrueFieldDescriptor +
                '}';
    }
}
