package ai.timefold.jpyinterpreter.types.wrappers.inaccessible;

public interface PublicInterface {
    String interfaceMethod();

    static PublicInterface getInstance() {
        return new PrivateObject();
    }
}
