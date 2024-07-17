package ai.timefold.jpyinterpreter.util.arguments;

public enum ArgumentKind {
    POSITIONAL_AND_KEYWORD(true, true),
    POSITIONAL_ONLY(true, false),
    KEYWORD_ONLY(false, true),
    VARARGS(false, false);

    final boolean allowPositional;
    final boolean allowKeyword;

    ArgumentKind(boolean allowPositional, boolean allowKeyword) {
        this.allowPositional = allowPositional;
        this.allowKeyword = allowKeyword;
    }

    public boolean isAllowPositional() {
        return allowPositional;
    }

    public boolean isAllowKeyword() {
        return allowKeyword;
    }
}
