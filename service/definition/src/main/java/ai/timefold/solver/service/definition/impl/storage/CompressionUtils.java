package ai.timefold.solver.service.definition.impl.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility methods to compress and decompress content that is stored in a
 * {@link ai.timefold.solver.service.definition.internal.storage.Storage}.
 * <p>
 * Content is compressed with gzip. As storages can also contain content that was not written by the service, all
 * decompression methods first check for the gzip magic bytes and pass the content through unchanged when it is not
 * compressed.
 */
public class CompressionUtils {

    private static final int GZIP_MAGIC_LENGTH = 2;

    public static byte[] compress(byte[] data) {
        byte[] processed;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOs = new GZIPOutputStream(os)) {

            gzipOs.write(data, 0, data.length);

            gzipOs.close();

            processed = os.toByteArray();
        } catch (IOException e) {
            processed = data;
        }
        return processed;
    }

    public static byte[] uncompress(byte[] data) {
        if (isCompressed(data)) {

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data))) {
                gis.transferTo(os);

                return os.toByteArray();
            } catch (IOException e) {
                return data;
            }
        } else {
            return data;
        }
    }

    public static boolean isCompressed(final byte[] compressed) {
        return compressed.length >= GZIP_MAGIC_LENGTH
                && (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    /**
     * Wraps given stream into a decompressing one, in case its content is compressed.
     *
     * @param source stream to read the content from
     * @return stream that provides the uncompressed content, closing it closes the source stream as well
     */
    public static InputStream decompressIfNeeded(InputStream source) throws IOException {
        PushbackInputStream pushback = new PushbackInputStream(source, GZIP_MAGIC_LENGTH);
        byte[] magic = new byte[GZIP_MAGIC_LENGTH];
        int read = pushback.read(magic);
        if (read > 0) {
            pushback.unread(magic, 0, read);
        }
        if (read == GZIP_MAGIC_LENGTH && isCompressed(magic)) {
            return new GZIPInputStream(pushback);
        }
        return pushback;
    }

    /**
     * Transfers the content of the source stream to the target one, compressing it on the fly in case it is not
     * compressed already.
     *
     * @param source stream to read the content from
     * @param target stream to write the (compressed) content to
     */
    public static void transferDataCompressIfNeeded(InputStream source, OutputStream target) throws IOException {
        byte[] twoFirst = source.readNBytes(GZIP_MAGIC_LENGTH);
        if (isCompressed(twoFirst)) {
            target.write(twoFirst);
            source.transferTo(target);
        } else {
            GZIPOutputStream gzipOut = new GZIPOutputStream(target);
            gzipOut.write(twoFirst);
            source.transferTo(gzipOut);
            gzipOut.finish();
        }
    }
}
