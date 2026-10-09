package ai.timefold.solver.service.definition.impl.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;

class CompressionUtilsTest {

    private static final byte[] CONTENT = "{\"id\":\"dataset-1\",\"value\":\"the content of the dataset\"}"
            .repeat(100)
            .getBytes(StandardCharsets.UTF_8);

    @Test
    void compress_producesACompleteGzipStream() throws IOException {
        byte[] compressed = CompressionUtils.compress(CONTENT);

        assertThat(CompressionUtils.isCompressed(compressed)).isTrue();
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            assertThat(gzip.readAllBytes()).isEqualTo(CONTENT);
        }
    }

    @Test
    void compressedContent_uncompressesToTheOriginal() {
        assertThat(CompressionUtils.uncompress(CompressionUtils.compress(CONTENT))).isEqualTo(CONTENT);
    }

    @Test
    void compressedContent_isDecompressedWhenStreamed() throws IOException {
        try (InputStream content =
                CompressionUtils.decompressIfNeeded(new ByteArrayInputStream(CompressionUtils.compress(CONTENT)))) {
            assertThat(content.readAllBytes()).isEqualTo(CONTENT);
        }
    }

    @Test
    void transferredContent_isCompressedOnceAndUncompressesToTheOriginal() throws IOException {
        var target = new ByteArrayOutputStream();
        CompressionUtils.transferDataCompressIfNeeded(new ByteArrayInputStream(CONTENT), target);

        var again = new ByteArrayOutputStream();
        CompressionUtils.transferDataCompressIfNeeded(new ByteArrayInputStream(target.toByteArray()), again);

        assertThat(again.toByteArray()).as("content that is compressed already is passed on as it is")
                .isEqualTo(target.toByteArray());
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(target.toByteArray()))) {
            assertThat(gzip.readAllBytes()).isEqualTo(CONTENT);
        }
    }
}
