package ai.timefold.solver.core.impl.domain.specification.testdata;

/**
 * A simple value class with package-private visibility for testing Lookup-based access.
 */
class PackagePrivateValue {

    private String code;

    PackagePrivateValue() {
    }

    PackagePrivateValue(String code) {
        this.code = code;
    }

    String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "PackagePrivateValue(" + code + ")";
    }
}
