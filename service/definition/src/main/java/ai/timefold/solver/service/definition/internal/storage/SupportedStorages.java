package ai.timefold.solver.service.definition.internal.storage;

public class SupportedStorages {

    public static final String STORAGE_TYPE_PROPERTY = "ai.timefold.storage.type";
    public static final String GOOGLE_CLOUD_STORAGE = "googlecloud";
    public static final String AZURE_STORAGE = "azure";
    public static final String INMEMORY_STORAGE = "inmemory";
    public static final String FILESYSTEM_STORAGE = "filesystem";
    public static final String S3_STORAGE = "s3";

    public enum Variant {
        S3(S3_STORAGE),
        GoogleCloud(GOOGLE_CLOUD_STORAGE),
        Azure(AZURE_STORAGE),
        InMemory(INMEMORY_STORAGE),
        FileSystem(FILESYSTEM_STORAGE);

        private String identifier;

        Variant(String identifier) {
            this.identifier = identifier;
        }

        public String identifier() {
            return this.identifier;
        }
    }
}
