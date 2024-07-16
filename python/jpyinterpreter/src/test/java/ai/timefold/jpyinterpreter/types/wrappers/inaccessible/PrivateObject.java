package ai.timefold.jpyinterpreter.types.wrappers.inaccessible;

record PrivateObject() implements PublicInterface {
    public String interfaceMethod() {
        return "PrivateObject";
    }
}
