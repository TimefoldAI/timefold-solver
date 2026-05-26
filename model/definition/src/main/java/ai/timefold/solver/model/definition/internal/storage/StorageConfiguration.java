package ai.timefold.solver.model.definition.internal.storage;

public class StorageConfiguration {

    private int deleteAfter;

    public int getDeleteAfter() {
        return deleteAfter;
    }

    public void setDeleteAfter(int deleteAfter) {
        this.deleteAfter = deleteAfter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final StorageConfiguration configuration;

        private Builder() {
            configuration = new StorageConfiguration();
        }

        public Builder deleteAfter(int deleteAfter) {
            this.configuration.deleteAfter = deleteAfter;
            return this;
        }

        public StorageConfiguration build() {
            return configuration;
        }
    }
}
