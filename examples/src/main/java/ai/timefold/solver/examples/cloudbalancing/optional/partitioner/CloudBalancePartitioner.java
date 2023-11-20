package ai.timefold.solver.examples.cloudbalancing.optional.partitioner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.partitionedsearch.partitioner.SolutionPartitioner;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudBalance;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudComputer;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudProcess;

public class CloudBalancePartitioner implements SolutionPartitioner<CloudBalance> {

    private int partCount = 4;
    private int minimumProcessListSize = 25;

    @SuppressWarnings("unused")
    public void setPartCount(int partCount) {
        this.partCount = partCount;
    }

    @SuppressWarnings("unused")
    public void setMinimumProcessListSize(int minimumProcessListSize) {
        this.minimumProcessListSize = minimumProcessListSize;
    }

    @Override
    public List<CloudBalance> splitWorkingSolution(ScoreDirector<CloudBalance> scoreDirector, Integer runnablePartThreadLimit) {
        CloudBalance originalSolution = scoreDirector.getWorkingSolution();
        List<CloudComputer> originalComputerList = originalSolution.getComputerList();
        List<CloudProcess> originalProcessList = originalSolution.getProcessList();
        int partCount = this.partCount;
        if (originalProcessList.size() / partCount < minimumProcessListSize) {
            partCount = originalProcessList.size() / minimumProcessListSize;
        }
        List<CloudBalance> partList = new ArrayList<>(partCount);
        for (int i = 0; i < partCount; i++) {
            CloudBalance partSolution = new CloudBalance(originalSolution.getId(),
                    new ArrayList<>(originalComputerList.size() / partCount + 1),
                    new ArrayList<>(originalProcessList.size() / partCount + 1));
            partList.add(partSolution);
        }

        int partIndex = 0;
        Map<Long, Pair<Integer, CloudComputer>> idToPartIndexAndComputerMap = new HashMap<>(originalComputerList.size());
        for (CloudComputer originalComputer : originalComputerList) {
            CloudBalance part = partList.get(partIndex);
            CloudComputer computer = new CloudComputer(
                    originalComputer.getId(),
                    originalComputer.getCpuPower(), originalComputer.getMemory(),
                    originalComputer.getNetworkBandwidth(), originalComputer.getCost());
            part.getComputerList().add(computer);
            idToPartIndexAndComputerMap.put(computer.getId(), new Pair<>(partIndex, computer));
            partIndex = (partIndex + 1) % partList.size();
        }

        partIndex = 0;
        for (CloudProcess originalProcess : originalProcessList) {
            CloudBalance part = partList.get(partIndex);
            CloudProcess process = new CloudProcess(
                    originalProcess.getId(),
                    originalProcess.getRequiredCpuPower(), originalProcess.getRequiredMemory(),
                    originalProcess.getRequiredNetworkBandwidth());
            part.getProcessList().add(process);
            if (originalProcess.getComputer() != null) {
                Pair<Integer, CloudComputer> partIndexAndComputer = idToPartIndexAndComputerMap.get(
                        originalProcess.getComputer().getId());
                if (partIndexAndComputer == null) {
                    throw new IllegalStateException("The initialized process (" + originalProcess
                            + ") has a computer (" + originalProcess.getComputer()
                            + ") which doesn't exist in the originalSolution (" + originalSolution + ").");
                }
                if (partIndex != partIndexAndComputer.key().intValue()) {
                    throw new IllegalStateException("The initialized process (" + originalProcess
                            + ") with partIndex (" + partIndex
                            + ") has a computer (" + originalProcess.getComputer()
                            + ") which belongs to another partIndex (" + partIndexAndComputer.key() + ").");
                }
                process.setComputer(partIndexAndComputer.value());
            }
            partIndex = (partIndex + 1) % partList.size();
        }
        return partList;
    }

    private record Pair<A, B>(A key, B value) {

    }

}
