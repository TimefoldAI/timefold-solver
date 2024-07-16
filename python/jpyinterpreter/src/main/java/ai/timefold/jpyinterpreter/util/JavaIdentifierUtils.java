package ai.timefold.jpyinterpreter.util;

import java.util.Set;

public class JavaIdentifierUtils {
    private static final Set<String> JAVA_KEYWORD_SET = Set.of(
            "abstract",
            "continue",
            "for",
            "new",
            "switch",
            "assert",
            "default",
            "goto",
            "package",
            "synchronized",
            "boolean",
            "do",
            "if",
            "private",
            "this",
            "break",
            "double",
            "implements",
            "protected",
            "throw",
            "byte",
            "else",
            "import",
            "public",
            "throws",
            "case",
            "enum",
            "instanceof",
            "return",
            "transient",
            "catch",
            "extends",
            "int",
            "short",
            "try",
            "char",
            "final",
            "interface",
            "static",
            "void",
            "class",
            "finally",
            "long",
            "strictfp",
            "volatile",
            "const",
            "float",
            "native",
            "super",
            "while");

    private JavaIdentifierUtils() {
    }

    public static String sanitizeClassName(String pythonClassName) {
        StringBuilder builder = new StringBuilder();
        pythonClassName.chars().forEachOrdered(character -> {
            if (character != '.' && !Character.isJavaIdentifierPart(character)) {
                String replacement = "$_" + character + "_$";
                builder.append(replacement);
            } else {
                builder.appendCodePoint(character);
            }
        });
        String out = builder.toString();
        if (JAVA_KEYWORD_SET.contains(out)) {
            return "$" + out;
        } else {
            return out;
        }
    }

    public static String sanitizeFieldName(String pythonFieldName) {
        StringBuilder builder = new StringBuilder();
        pythonFieldName.chars().forEachOrdered(character -> {
            if (!Character.isJavaIdentifierPart(character)) {
                String replacement = "$_" + character + "_$";
                builder.append(replacement);
            } else {
                builder.appendCodePoint(character);
            }
        });
        String out = builder.toString();
        if (JAVA_KEYWORD_SET.contains(out)) {
            return "$" + out;
        } else if (pythonFieldName.isEmpty()) {
            return "$$";
        } else if (Character.isDigit(pythonFieldName.charAt(0))) {
            return "$" + out;
        } else {
            return out;
        }
    }
}
