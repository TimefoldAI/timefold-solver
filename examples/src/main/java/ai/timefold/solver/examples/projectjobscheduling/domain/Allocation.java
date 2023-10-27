package ai.timefold.solver.examples.projectjobscheduling.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.common.persistence.jackson.JacksonUniqueIdGenerator;
import ai.timefold.solver.examples.common.swingui.components.Labeled;
import ai.timefold.solver.examples.projectjobscheduling.domain.solver.DelayStrengthComparator;
import ai.timefold.solver.examples.projectjobscheduling.domain.solver.ExecutionModeStrengthWeightFactory;
import ai.timefold.solver.examples.projectjobscheduling.domain.solver.NotSourceOrSinkAllocationFilter;
import ai.timefold.solver.examples.projectjobscheduling.domain.solver.PredecessorsDoneDateUpdatingVariableListener;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;

@PlanningEntity(pinningFilter = NotSourceOrSinkAllocationFilter.class)
@JsonIdentityInfo(generator = JacksonUniqueIdGenerator.class)
public class Allocation extends AbstractPersistable implements Labeled {

    private Job job;

    private Allocation sourceAllocation;
    private Allocation sinkAllocation;
    private List<Allocation> predecessorAllocationList;
    private List<Allocation> successorAllocationList;

    // Planning variables: changes during planning, between score calculations.
    private ExecutionMode executionMode;
    private Integer delay; // In days

    // Shadow variables
    private Integer predecessorsDoneDate;

    // Filled from shadow variables
    private Integer startDate;
    private Integer endDate;
    private List<Integer> busyDates;

    public Allocation() {
    }

    public Allocation(long id, Job job) {
        super(id);
        this.job = job;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Allocation getSourceAllocation() {
        return sourceAllocation;
    }

    public void setSourceAllocation(Allocation sourceAllocation) {
        this.sourceAllocation = sourceAllocation;
    }

    public Allocation getSinkAllocation() {
        return sinkAllocation;
    }

    public void setSinkAllocation(Allocation sinkAllocation) {
        this.sinkAllocation = sinkAllocation;
    }

    public List<Allocation> getPredecessorAllocationList() {
        return predecessorAllocationList;
    }

    public void setPredecessorAllocationList(List<Allocation> predecessorAllocationList) {
        this.predecessorAllocationList = predecessorAllocationList;
    }

    public List<Allocation> getSuccessorAllocationList() {
        return successorAllocationList;
    }

    public void setSuccessorAllocationList(List<Allocation> successorAllocationList) {
        this.successorAllocationList = successorAllocationList;
    }

    @PlanningVariable(strengthWeightFactoryClass = ExecutionModeStrengthWeightFactory.class)
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
        invalidateComputedVariables();
    }

    private void invalidateComputedVariables() {
        this.startDate = null;
        this.endDate = null;
        this.busyDates = null;
    }

    @PlanningVariable(strengthComparatorClass = DelayStrengthComparator.class)
    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
        invalidateComputedVariables();
    }

    @ShadowVariable(variableListenerClass = PredecessorsDoneDateUpdatingVariableListener.class,
            sourceVariableName = "executionMode")
    @ShadowVariable(variableListenerClass = PredecessorsDoneDateUpdatingVariableListener.class, sourceVariableName = "delay")
    public Integer getPredecessorsDoneDate() {
        return predecessorsDoneDate;
    }

    public void setPredecessorsDoneDate(Integer predecessorsDoneDate) {
        this.predecessorsDoneDate = predecessorsDoneDate;
        invalidateComputedVariables();
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @JsonIgnore
    public Integer getStartDate() {
        if (startDate == null && predecessorsDoneDate != null) {
            startDate = predecessorsDoneDate + Objects.requireNonNullElse(delay, 0);
        }
        return startDate;
    }

    @JsonIgnore
    public Integer getEndDate() {
        if (endDate == null && predecessorsDoneDate != null) {
            endDate = getStartDate() + (executionMode == null ? 0 : executionMode.getDuration());
        }
        return endDate;
    }

    @JsonIgnore
    public List<Integer> getBusyDates() {
        if (busyDates == null) {
            if (predecessorsDoneDate == null) {
                busyDates = Collections.emptyList();
            } else {
                var startDate = getStartDate();
                var endDate = getEndDate();
                var dates = new Integer[endDate - startDate];
                for (int i = 0; i < dates.length; i++) {
                    dates[i] = startDate + i;
                }
                busyDates = Arrays.asList(dates);
            }
        }
        return busyDates;
    }

    @JsonIgnore
    public Project getProject() {
        return job.getProject();
    }

    @JsonIgnore
    public int getProjectCriticalPathEndDate() {
        return job.getProject().getCriticalPathEndDate();
    }

    @JsonIgnore
    public JobType getJobType() {
        return job.getJobType();
    }

    @Override
    public String getLabel() {
        return "Job " + job.getId();
    }

    // ************************************************************************
    // Ranges
    // ************************************************************************

    @ValueRangeProvider
    @JsonIgnore
    public List<ExecutionMode> getExecutionModeRange() {
        return job.getExecutionModeList();
    }

    @ValueRangeProvider
    @JsonIgnore
    public CountableValueRange<Integer> getDelayRange() {
        return ValueRangeFactory.createIntValueRange(0, 500);
    }

}
