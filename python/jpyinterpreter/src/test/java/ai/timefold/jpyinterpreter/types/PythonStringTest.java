package ai.timefold.jpyinterpreter.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PythonStringTest {

    // Other methods are tested in test_str.py
    // These methods are tested here since they are internal,
    // and has edge cases CPython won't hit

    @Test
    void asAsciiBytes() {
        var simple = PythonString.valueOf("abc");
        assertThat(simple.asAsciiBytes().asByteArray()).isEqualTo(new byte[] { 'a', 'b', 'c' });

        var unicode = PythonString.valueOf("π");
        // UTF-16 encoding
        assertThat(unicode.asAsciiBytes().asByteArray()).isEqualTo(new byte[] { (byte) 0x03, (byte) 0xC0 });

        var mixed = PythonString.valueOf("aπc");
        // UTF-16 encoding
        assertThat(mixed.asAsciiBytes().asByteArray()).isEqualTo(new byte[] { 'a', (byte) 0x03, (byte) 0xC0, 'c' });
    }

    @Test
    void asAsciiByteArray() {
        var simple = PythonString.valueOf("abc");
        assertThat(simple.asAsciiByteArray().asByteArray()).isEqualTo(new byte[] { 'a', 'b', 'c' });

        var unicode = PythonString.valueOf("π");
        // UTF-16 encoding
        assertThat(unicode.asAsciiByteArray().asByteArray()).isEqualTo(new byte[] { (byte) 0x03, (byte) 0xC0 });

        var mixed = PythonString.valueOf("aπc");
        // UTF-16 encoding
        assertThat(mixed.asAsciiByteArray().asByteArray()).isEqualTo(new byte[] { 'a', (byte) 0x03, (byte) 0xC0, 'c' });
    }
}