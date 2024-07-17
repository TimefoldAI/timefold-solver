package ai.timefold.jpyinterpreter;

import java.util.Objects;

public final class PythonVersion implements Comparable<PythonVersion> {
    private final int hexversion;

    public static final PythonVersion PYTHON_3_10 = new PythonVersion(3, 10);
    public static final PythonVersion PYTHON_3_11 = new PythonVersion(3, 11);
    public static final PythonVersion PYTHON_3_12 = new PythonVersion(3, 12);

    public static final PythonVersion MINIMUM_PYTHON_VERSION = PYTHON_3_10;

    public PythonVersion(int hexversion) {
        this.hexversion = hexversion;
    }

    public PythonVersion(int major, int minor) {
        // Select the final version for the first Python release with the given major and minor
        this(major, minor, 0);
    }

    public PythonVersion(int major, int minor, int micro) {
        // Select the final version for the first Python release with the given major, minor and micro
        this((major << (8 * 3)) + (minor << (8 * 2)) + (micro << 8) + 0xF0);
    }

    public int getMajorVersion() {
        return (hexversion & 0xFF000000) >> (8 * 3);
    }

    public int getMinorVersion() {
        return (hexversion & 0x00FF0000) >> (8 * 2);
    }

    public int getMicroVersion() {
        return (hexversion & 0x0000FF00) >> 8;
    }

    public int getReleaseLevel() {
        return (hexversion & 0x000000F0) >> 4;
    }

    public int getReleaseSerial() {
        return (hexversion & 0x0000000F);
    }

    @Override
    public int compareTo(PythonVersion pythonVersion) {
        return hexversion - pythonVersion.hexversion;
    }

    public boolean isBefore(PythonVersion release) {
        return compareTo(release) < 0;
    }

    public boolean isAfter(PythonVersion release) {
        return compareTo(release) > 0;
    }

    public boolean isAtLeast(PythonVersion release) {
        return compareTo(release) >= 0;
    }

    public boolean isBetween(PythonVersion afterInclusive, PythonVersion beforeInclusive) {
        return compareTo(afterInclusive) >= 0 && compareTo(beforeInclusive) <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonVersion that = (PythonVersion) o;
        return hexversion == that.hexversion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hexversion);
    }

    public String toString() {
        return getMajorVersion() + "." + getMinorVersion() + "." + getMinorVersion() + getReleaseLevelString()
                + getReleaseSerial();
    }

    private String getReleaseLevelString() {
        switch (getReleaseLevel()) {
            case 0xA:
                return "a"; // Alpha release

            case 0xB:
                return "b"; // Beta release

            case 0xC:
                return "rc"; // Release Candidate

            case 0xF:
                return ""; // Final

            default:
                // https://docs.python.org/3/c-api/apiabiversion.html#apiabiversion
                // only use 4 states out of the possible 16.
                return ("<" + getReleaseLevel() + ">");
        }
    }
}
