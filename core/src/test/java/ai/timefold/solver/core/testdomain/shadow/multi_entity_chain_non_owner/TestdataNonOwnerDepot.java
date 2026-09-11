package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_non_owner;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A declarative entity class that does not own the list variable.
 */
@PlanningEntity
public class TestdataNonOwnerDepot extends TestdataObject {

    List<TestdataNonOwnerDepot> otherDepots = new ArrayList<>();
    int baseTime;

    @ShadowVariable(supplierName = "openTimeSupplier")
    Integer openTime;

    public TestdataNonOwnerDepot() {
    }

    public TestdataNonOwnerDepot(String code, int baseTime) {
        super(code);
        this.baseTime = baseTime;
    }

    @ShadowSources("otherDepots[].openTime")
    public Integer openTimeSupplier() {
        var max = baseTime;
        for (var otherDepot : otherDepots) {
            if (otherDepot.getOpenTime() == null) {
                return null;
            }
            max = Math.max(max, otherDepot.getOpenTime());
        }
        return max;
    }

    public List<TestdataNonOwnerDepot> getOtherDepots() {
        return otherDepots;
    }

    public void setOtherDepots(List<TestdataNonOwnerDepot> otherDepots) {
        this.otherDepots = otherDepots;
    }

    public int getBaseTime() {
        return baseTime;
    }

    public void setBaseTime(int baseTime) {
        this.baseTime = baseTime;
    }

    public Integer getOpenTime() {
        return openTime;
    }

    public void setOpenTime(Integer openTime) {
        this.openTime = openTime;
    }
}
