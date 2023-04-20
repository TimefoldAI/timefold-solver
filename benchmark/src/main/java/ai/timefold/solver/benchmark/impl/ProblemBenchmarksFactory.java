package ai.timefold.solver.benchmark.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.ProblemBenchmarksConfig;
import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.config.statistic.SingleStatisticType;
import ai.timefold.solver.benchmark.impl.loader.FileProblemProvider;
import ai.timefold.solver.benchmark.impl.loader.InstanceProblemProvider;
import ai.timefold.solver.benchmark.impl.loader.ProblemProvider;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class ProblemBenchmarksFactory {
    private final ProblemBenchmarksConfig config;

    public ProblemBenchmarksFactory(ProblemBenchmarksConfig config) {
        this.config = config;
    }

    public <Solution_> void buildProblemBenchmarkList(SolverBenchmarkResult solverBenchmarkResult,
            Solution_[] extraProblems) {
        PlannerBenchmarkResult plannerBenchmarkResult = solverBenchmarkResult.getPlannerBenchmarkResult();
        List<ProblemBenchmarkResult> unifiedProblemBenchmarkResultList = plannerBenchmarkResult
                .getUnifiedProblemBenchmarkResultList();
        for (ProblemProvider<Solution_> problemProvider : buildProblemProviderList(
                solverBenchmarkResult, extraProblems)) {
            // 2 SolverBenchmarks containing equal ProblemBenchmarks should contain the same instance
            ProblemBenchmarkResult<Solution_> newProblemBenchmarkResult = buildProblemBenchmark(
                    plannerBenchmarkResult, problemProvider);
            ProblemBenchmarkResult<Solution_> problemBenchmarkResult;
            int index = unifiedProblemBenchmarkResultList.indexOf(newProblemBenchmarkResult);
            if (index < 0) {
                problemBenchmarkResult = newProblemBenchmarkResult;
                unifiedProblemBenchmarkResultList.add(problemBenchmarkResult);
            } else {
                problemBenchmarkResult = unifiedProblemBenchmarkResultList.get(index);
            }
            buildSingleBenchmark(solverBenchmarkResult, problemBenchmarkResult);
        }
    }

    private <Solution_> List<ProblemProvider<Solution_>> buildProblemProviderList(
            SolverBenchmarkResult solverBenchmarkResult, Solution_[] extraProblems) {
        if (ConfigUtils.isEmptyCollection(config.getInputSolutionFileList()) && extraProblems.length == 0) {
            throw new IllegalArgumentException(
                    "The solverBenchmarkResult (" + solverBenchmarkResult.getName() + ") has no problems.\n"
                            + "Maybe configure at least 1 <inputSolutionFile> directly or indirectly by inheriting it.\n"
                            + "Or maybe pass at least one problem to " + PlannerBenchmarkFactory.class.getSimpleName()
                            + ".buildPlannerBenchmark().");
        }
        List<ProblemProvider<Solution_>> problemProviderList = new ArrayList<>(
                extraProblems.length
                        + (config.getInputSolutionFileList() == null ? 0 : config.getInputSolutionFileList().size()));
        DefaultSolverFactory<Solution_> defaultSolverFactory =
                new DefaultSolverFactory<>(solverBenchmarkResult.getSolverConfig());
        SolutionDescriptor<Solution_> solutionDescriptor = defaultSolverFactory.getSolutionDescriptor();
        int extraProblemIndex = 0;
        for (Solution_ extraProblem : extraProblems) {
            if (extraProblem == null) {
                throw new IllegalStateException("The benchmark problem (" + extraProblem + ") is null.");
            }
            String problemName = "Problem_" + extraProblemIndex;
            problemProviderList.add(new InstanceProblemProvider<>(problemName, solutionDescriptor, extraProblem));
            extraProblemIndex++;
        }
        if (ConfigUtils.isEmptyCollection(config.getInputSolutionFileList())) {
            if (config.getSolutionFileIOClass() != null) {
                throw new IllegalArgumentException("Cannot use solutionFileIOClass (" + config.getSolutionFileIOClass()
                        + ") with an empty inputSolutionFileList (" + config.getInputSolutionFileList() + ").");
            }
        } else {
            SolutionFileIO<Solution_> solutionFileIO = buildSolutionFileIO();
            for (File inputSolutionFile : config.getInputSolutionFileList()) {
                if (!inputSolutionFile.exists()) {
                    throw new IllegalArgumentException("The inputSolutionFile (" + inputSolutionFile
                            + ") does not exist.");
                }
                problemProviderList.add(new FileProblemProvider<>(solutionFileIO, inputSolutionFile));
            }
        }
        return problemProviderList;
    }

    private <Solution_> SolutionFileIO<Solution_> buildSolutionFileIO() {
        if (config.getSolutionFileIOClass() == null) {
            throw new IllegalArgumentException(
                    "The solutionFileIOClass (" + config.getSolutionFileIOClass() + ") cannot be null.");
        }
        return (SolutionFileIO<Solution_>) ConfigUtils.newInstance(config, "solutionFileIOClass",
                config.getSolutionFileIOClass());
    }

    private <Solution_> ProblemBenchmarkResult<Solution_> buildProblemBenchmark(
            PlannerBenchmarkResult plannerBenchmarkResult, ProblemProvider<Solution_> problemProvider) {
        ProblemBenchmarkResult<Solution_> problemBenchmarkResult = new ProblemBenchmarkResult<>(plannerBenchmarkResult);
        problemBenchmarkResult.setName(problemProvider.getProblemName());
        problemBenchmarkResult.setProblemProvider(problemProvider);
        problemBenchmarkResult.setWriteOutputSolutionEnabled(
                config.getWriteOutputSolutionEnabled() == null ? false : config.getWriteOutputSolutionEnabled());
        List<ProblemStatistic> problemStatisticList;
        if (config.getProblemStatisticEnabled() != null && !config.getProblemStatisticEnabled()) {
            if (!ConfigUtils.isEmptyCollection(config.getProblemStatisticTypeList())) {
                throw new IllegalArgumentException("The problemStatisticEnabled (" + config.getProblemStatisticEnabled()
                        + ") and problemStatisticTypeList (" + config.getProblemStatisticTypeList()
                        + ") cannot be used together.");
            }
            problemStatisticList = Collections.emptyList();
        } else {
            List<ProblemStatisticType> problemStatisticTypeList_ = config.determineProblemStatisticTypeList();
            problemStatisticList = new ArrayList<>(problemStatisticTypeList_.size());
            for (ProblemStatisticType problemStatisticType : problemStatisticTypeList_) {
                problemStatisticList.add(problemStatisticType.buildProblemStatistic(problemBenchmarkResult));
            }
        }
        problemBenchmarkResult.setProblemStatisticList(problemStatisticList);
        problemBenchmarkResult.setSingleBenchmarkResultList(new ArrayList<>());
        return problemBenchmarkResult;
    }

    private void buildSingleBenchmark(SolverBenchmarkResult solverBenchmarkResult,
            ProblemBenchmarkResult problemBenchmarkResult) {
        SingleBenchmarkResult singleBenchmarkResult = new SingleBenchmarkResult(solverBenchmarkResult, problemBenchmarkResult);
        buildSubSingleBenchmarks(singleBenchmarkResult, solverBenchmarkResult.getSubSingleCount());
        List<SingleStatisticType> singleStatisticTypeList = config.determineSingleStatisticTypeList();
        for (SubSingleBenchmarkResult subSingleBenchmarkResult : singleBenchmarkResult.getSubSingleBenchmarkResultList()) {
            subSingleBenchmarkResult.setPureSubSingleStatisticList(new ArrayList<>(singleStatisticTypeList.size()));
        }
        for (SingleStatisticType singleStatisticType : singleStatisticTypeList) {
            for (SubSingleBenchmarkResult subSingleBenchmarkResult : singleBenchmarkResult
                    .getSubSingleBenchmarkResultList()) {
                subSingleBenchmarkResult.getPureSubSingleStatisticList().add(
                        singleStatisticType.buildPureSubSingleStatistic(subSingleBenchmarkResult));
            }
        }
        singleBenchmarkResult.initSubSingleStatisticMaps();
        solverBenchmarkResult.getSingleBenchmarkResultList().add(singleBenchmarkResult);
        problemBenchmarkResult.getSingleBenchmarkResultList().add(singleBenchmarkResult);
    }

    private void buildSubSingleBenchmarks(SingleBenchmarkResult parent, int subSingleCount) {
        List<SubSingleBenchmarkResult> subSingleBenchmarkResultList = new ArrayList<>(subSingleCount);
        for (int i = 0; i < subSingleCount; i++) {
            SubSingleBenchmarkResult subSingleBenchmarkResult = new SubSingleBenchmarkResult(parent, i);
            subSingleBenchmarkResultList.add(subSingleBenchmarkResult);
        }
        parent.setSubSingleBenchmarkResultList(subSingleBenchmarkResultList);
    }
}
