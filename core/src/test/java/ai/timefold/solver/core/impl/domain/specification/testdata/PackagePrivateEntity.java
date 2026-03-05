package ai.timefold.solver.core.impl.domain.specification.testdata;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * A planning entity with package-private visibility for testing Lookup-based access.
 */
@PlanningEntity
class PackagePrivateEntity {

    @PlanningId
    private String id;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private PackagePrivateValue value;

    PackagePrivateEntity() {
    }

    PackagePrivateEntity(String id) {
        this.id = id;
    }

    PackagePrivateEntity(String id, PackagePrivateValue value) {
        this.id = id;
        this.value = value;
    }

    String getId() {
        return id;
    }

    PackagePrivateValue getValue() {
        return value;
    }

    void setValue(PackagePrivateValue value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "PackagePrivateEntity(" + id + ")";
    }
}
