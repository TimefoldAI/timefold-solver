<#ftl output_format="HTML"> <#-- So that Freemarker escapes automatically. -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${benchmarkReport.plannerBenchmarkResult.name} Planner benchmark report</title>

    <link href="website/webjars/timefold/img/timefold-favicon.svg" rel="icon" type="image/svg+xml">
    <link href="website/webjars/timefold/img/timefold-favicon.svg" rel="mask-icon" color="#000000">

    <link href="website/css/app.css" rel="stylesheet"/>
    <link href="website/webjars/bootstrap/css/bootstrap.min.css" rel="stylesheet">
    <link href="website/webjars/timefold/css/timefold-webui.css" rel="stylesheet" />
    <link href="website/css/prettify.css" rel="stylesheet"/>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet" /> <#-- Too large for a webjar. -->

    <script src="https://buttons.github.io/buttons.js"></script>
    <script src="website/js/app.js"></script> <#-- Contains functions called by chart.js; must go first. -->
    <script src="website/webjars/jquery/jquery.min.js"></script>
    <script src="website/webjars/bootstrap/js/bootstrap.bundle.min.js"></script> <#-- Includes Popper for dropdowns. -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.3.0"></script>
    <script src="https://unpkg.com/@sgratzl/chartjs-chart-boxplot@4.2.0"></script>
    <script src="website/js/chartjs-plugin-watermark.js"></script>
    <script src="website/js/prettify.js"></script>
</head>
<#macro addSolverBenchmarkBadges solverBenchmarkResult>
    <#if solverBenchmarkResult.favorite>
        <span class="badge text-bg-success">${solverBenchmarkResult.ranking}</span>
    <#elseif solverBenchmarkResult.ranking??>
        <span class="badge text-bg-secondary">${solverBenchmarkResult.ranking}</span>
    </#if>

    <#if solverBenchmarkResult.hasAnyFailure()>
        <span class="badge text-bg-danger" data-toggle="tooltip" title="Failed benchmark">F</span>
    <#elseif solverBenchmarkResult.hasAnyUninitializedSolution()>
        <span class="badge text-bg-warning" data-toggle="tooltip" title="Has an uninitialized solution">!</span>
    <#elseif solverBenchmarkResult.hasAnyInfeasibleScore()>
        <span class="badge text-bg-warning" data-toggle="tooltip" title="Has an infeasible score">!</span>
    </#if>
</#macro>
<#macro addProblemBenchmarkBadges problemBenchmarkResult>
    <#if problemBenchmarkResult.hasAnyFailure()>
        <span class="badge text-bg-danger" data-toggle="tooltip" title="Failed benchmark">F</span>
    </#if>
</#macro>
<#macro addSolverProblemBenchmarkResultBadges solverProblemBenchmarkResult>
    <#if solverProblemBenchmarkResult.winner>
        <span class="badge text-bg-success">${solverProblemBenchmarkResult.ranking}</span>
    <#elseif solverProblemBenchmarkResult.ranking??>
        <span class="badge text-bg-secondary">${solverProblemBenchmarkResult.ranking}</span>
    </#if>

    <#if solverProblemBenchmarkResult.hasAnyFailure()>
        <span class="badge text-bg-danger" data-toggle="tooltip" title="Failed benchmark">F</span>
    <#elseif !solverProblemBenchmarkResult.initialized>
        <span class="badge text-bg-important" data-toggle="tooltip" title="Uninitialized solution">!</span>
    <#elseif !solverProblemBenchmarkResult.scoreFeasible>
        <span class="badge text-bg-warning" data-toggle="tooltip" title="Infeasible score">!</span>
    </#if>
</#macro>
<#macro addChartList chartList idPrefix>
    <div class="tabbable">
        <div class="tab-content">
            <#assign scoreLevelIndex = 0>
            <#list chartList as chart>
                <#assign tabId = idPrefix + "_chart_" + scoreLevelIndex />
                <div class="tab-pane show<#if scoreLevelIndex == benchmarkReport.defaultShownScoreLevelIndex> active</#if>" id="${tabId}-tab-pane">
                    <@addChart chart=chart />
                </div>
                <#assign scoreLevelIndex = scoreLevelIndex + 1>
            </#list>
        </div>
        <ul class="nav nav-pills justify-content-center mb-2">
            <#assign scoreLevelIndex = 0>
            <#list chartList as chart>
                <#assign tabId = idPrefix + "_chart_" + scoreLevelIndex />
                <li><button class="btn-secondary nav-link<#if scoreLevelIndex == benchmarkReport.defaultShownScoreLevelIndex> active</#if>" data-bs-toggle="pill" data-bs-target="#${tabId}-tab-pane" type="button">${benchmarkReport.plannerBenchmarkResult.findScoreLevelLabel(scoreLevelIndex)?capitalize}</button></li>
                <#assign scoreLevelIndex = scoreLevelIndex + 1>
            </#list>
        </ul>
    </div>
</#macro>
<#macro addChart chart>
    <div class="chart-container mb-2">
        <canvas id="chart_${chart.id()}"></canvas>
        <script async src="website/js/${chart.id()}.js"></script>
    </div>
</#macro>
<body onload="prettyPrint()">
    <nav class="navbar sticky-top navbar-expand-lg navbar-dark bg-black shadow">
        <div class="container">
            <div class="text-white">
                <a href="#" class="navbar-brand">
                    <img src="website/webjars/timefold/img/timefold-logo-horizontal-negative.svg" alt="Timefold Logo (horizontal, negative)">
                </a>
                <br />
                Benchmark Report
            </div>

            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarContent">
                <span class="navbar-toggler-icon"></span>
            </button>

            <div class="collapse navbar-collapse" id="navbarContent">
                <ul class="navbar-nav mx-auto">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">
                            Summary
                        </a>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" href="#summary_result">Results</a></li>
                            <li><a class="dropdown-item" href="#summary_performance">Performance</a></li>
                        </ul>
                    </li>
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">
                            Problems
                        </a>
                        <ul class="dropdown-menu">
                            <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                <li><a class="dropdown-item" href="#problemBenchmark_${problemBenchmarkResult.anchorId}"><@addProblemBenchmarkBadges problemBenchmarkResult=problemBenchmarkResult/>&nbsp;${problemBenchmarkResult.name}</a></li>
                            </#list>
                        </ul>
                    </li>
                    <li class="nav-item"><a class="nav-link" href="#solverBenchmarkResult">Solvers</a></li>
                    <li class="nav-item"><a class="nav-link" href="#environmentInformation">Environment</a></li>
                </ul>
                <ul class="navbar-nav">
                    <li class="d-inline-flex align-items-center">
                        <span class="mx-1" style="position:relative; top:3px;"><a class="github-button" aria-label="Star Timefold on GitHub" href="https://github.com/TimefoldAI/timefold-solver" data-show-count="true" data-color-scheme="no-preference: dark; light: dark; dark: dark;">Star</a></span>
                        <a class="mx-1" href="https://stackoverflow.com/questions/tagged/timefold"><i class="fa-brands fa-stack-overflow text-white"></i></a>
                        <a class="mx-1" href="https://youtube.com/@timefold"><i class="fa-brands fa-youtube text-white"></i></a>
                        <a class="mx-1" href="https://twitter.com/TimefoldAI"><i class="fa-brands fa-twitter text-white"></i></a>
                        <a role="button" class="btn btn-sm tf-btn-contact btn-outline-light mx-1" href="https://timefold.ai/company/contact">Contact</a>
                    </li>
                </ul>
            </div>
        </div>
    </nav>

    <main class="container-fluid p-2 w-75"> <!-- Content -->
        <header class="visually-hidden">
            <h1>Timefold Benchmark report</h1>
        </header>

        <section id="summary_result">
            <h2>Result summary</h2>
            <#if benchmarkReport.plannerBenchmarkResult.hasAnyFailure()>
                <div class="alert alert-danger">
                    <p>${benchmarkReport.plannerBenchmarkResult.failureCount} benchmarks have failed!</p>
                </div>
            </#if>
            <#list benchmarkReport.getWarningList() as warning>
                <div class="alert alert-warning">
                    <p>${warning}</p>
                </div>
            </#list>
            <div class="tabbable">
                <ul class="nav nav-pills justify-content-center">
                    <li class="nav-item">
                        <button class="nav-link active" id="summary_bestScore-tab" data-bs-toggle="pill" data-bs-target="#summary_bestScore-tab-pane" type="button">Best score</button>
                    </li>
                    <li class="nav-item">
                        <button class="nav-link" id="summary_bestScoreScalability-tab" data-bs-toggle="pill" data-bs-target="#summary_bestScoreScalability-tab-pane" type="button">Best score scalability</button>
                    </li>
                    <li class="nav-item">
                        <button class="nav-link" id="summary_bestScoreDistribution-tab" data-bs-toggle="pill" data-bs-target="#summary_bestScoreDistribution-tab-pane" type="button">Best score distribution</button>
                    </li>
                    <li class="nav-item">
                        <button class="nav-link" id="summary_winningScoreDifference-tab" data-bs-toggle="pill" data-bs-target="#summary_winningScoreDifference-tab-pane" type="button">Winning score difference</button>
                    </li>
                    <li class="nav-item">
                        <button class="nav-link" id="summary_worstScoreDifferencePercentage-tab" data-bs-toggle="pill" data-bs-target="#summary_worstScoreDifferencePercentage-tab-pane" type="button">Worst score difference percentage (ROI)</button>
                    </li>
                </ul>
                <div class="tab-content p-2">
                    <div class="tab-pane show active" id="summary_bestScore-tab-pane">
                        <h3 class="visually-hidden">Best score summary</h3>
                        <p>Useful for visualizing the best solver configuration.</p>
                        <@addChartList chartList=benchmarkReport.bestScoreSummaryChartList idPrefix="summary_bestScore" />
                        <div class="table-responsive">
                            <table class="table table-hover table-striped table-bordered">
                                <thead>
                                    <tr>
                                        <th rowspan="2">Solver</th>
                                        <th rowspan="2">Total</th>
                                        <th rowspan="2">Average</th>
                                        <th rowspan="2">Standard Deviation</th>
                                        <th colspan="${benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList?size}">Problem</th>
                                    </tr>
                                    <tr>
                                    <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                        <th>${problemBenchmarkResult.name}</th>
                                    </#list>
                                    </tr>
                                </thead>
                                <tbody class="table-group-divider">
                                    <#list benchmarkReport.plannerBenchmarkResult.solverBenchmarkResultList as solverBenchmarkResult>
                                        <tr<#if solverBenchmarkResult.favorite> class="table-success"</#if>>
                                            <th>${solverBenchmarkResult.name}&nbsp;<@addSolverBenchmarkBadges solverBenchmarkResult=solverBenchmarkResult/></th>
                                            <td>${solverBenchmarkResult.totalScore!""}</td>
                                            <td>${solverBenchmarkResult.averageScore!""}</td>
                                            <td>${solverBenchmarkResult.standardDeviationString!""}</td>
                                            <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                                <#if !solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)??>
                                                    <td></td>
                                                <#else>
                                                    <#assign singleBenchmarkResult = solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)>
                                                    <#if !singleBenchmarkResult.hasAllSuccess()>
                                                        <td><span class="label label-important">Failed</span></td>
                                                    <#else>
                                                        <#if solverBenchmarkResult.subSingleCount lte 1>
                                                            <td>${singleBenchmarkResult.averageScore!""}&nbsp;<@addSolverProblemBenchmarkResultBadges solverProblemBenchmarkResult=singleBenchmarkResult/></td>
                                                        <#else>
                                                            <td>
                                                                <span class="dropdown">
                                                                    <button class="btn btn-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown">
                                                                        ${singleBenchmarkResult.averageScore!""}&nbsp;<@addSolverProblemBenchmarkResultBadges solverProblemBenchmarkResult=singleBenchmarkResult/>
                                                                    </button>
                                                                    <ul class="dropdown-menu">
                                                                        <#list singleBenchmarkResult.subSingleBenchmarkResultList as subSingleBenchmarkResult>
                                                                            <li class="dropdown-header">Run #${subSingleBenchmarkResult.getSubSingleBenchmarkIndex()}</li>
                                                                            <li class="dropdown-item">${subSingleBenchmarkResult.score!""}&nbsp;<@addSolverProblemBenchmarkResultBadges solverProblemBenchmarkResult=subSingleBenchmarkResult/></li>
                                                                        </#list>
                                                                        <li><hr class="dropdown-divider"></li>
                                                                        <li class="dropdown-header">Median</li>
                                                                        <li class="dropdown-item">${singleBenchmarkResult.median.score!""}</li>
                                                                        <li class="dropdown-header">Standard Deviation</li>
                                                                        <li class="dropdown-item">${singleBenchmarkResult.standardDeviationString!""}</li>
                                                                        <li class="dropdown-header">Best</li>
                                                                        <li class="dropdown-item">${singleBenchmarkResult.best.score!""}</li>
                                                                        <li class="dropdown-header">Average</li>
                                                                        <li class="dropdown-item">${singleBenchmarkResult.averageScore!""}</li>
                                                                        <li class="dropdown-header">Worst</li>
                                                                        <li class="dropdown-item">${singleBenchmarkResult.worst.score!""}</li>
                                                                    </ul>
                                                                </span>
                                                            </td>
                                                        </#if>
                                                    </#if>
                                                </#if>
                                            </#list>
                                        </tr>
                                    </#list>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="tab-pane show" id="summary_bestScoreScalability-tab-pane">
                        <h3 class="visually-hidden">Best score scalability summary</h3>
                        <p>Useful for visualizing the scalability of each solver configuration.</p>
                        <@addChartList chartList=benchmarkReport.bestScoreScalabilitySummaryChartList idPrefix="summary_bestScoreScalability" />
                    </div>
                    <div class="tab-pane show" id="summary_bestScoreDistribution-tab-pane">
                        <h3 class="visually-hidden">Best score distribution summary</h3>
                        <p>Useful for visualizing the reliability of each solver configuration.</p>
                        <#assign maximumSubSingleCount = benchmarkReport.plannerBenchmarkResult.getMaximumSubSingleCount()>
                        <#if maximumSubSingleCount lte 1>
                            <div class="alert alert-info">
                                <p>Benchmarker did not run multiple subSingles, so there is no distribution and therefore no reliability indication.</p>
                            </div>
                        </#if>
                        <@addChartList chartList=benchmarkReport.bestScoreDistributionSummaryChartList idPrefix="summary_bestScoreDistribution" />
                    </div>
                    <div class="tab-pane show" id="summary_winningScoreDifference-tab-pane">
                        <h3 class="visually-hidden">Winning score difference summary</h3>
                        <p>Useful for zooming in on the results of the best score summary.</p>
                        <@addChartList chartList=benchmarkReport.winningScoreDifferenceSummaryChartList idPrefix="summary_winningScoreDifference" />
                        <div class="table-responsive">
                            <table class="table table-hover table-striped table-bordered">
                                <thead>
                                    <tr>
                                        <th rowspan="2">Solver</th>
                                        <th rowspan="2">Total</th>
                                        <th rowspan="2">Average</th>
                                        <th colspan="${benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList?size}">Problem</th>
                                    </tr>
                                    <tr>
                                    <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                        <th>${problemBenchmarkResult.name}</th>
                                    </#list>
                                    </tr>
                                </thead>
                                <tbody class="table-group-divider">
                                    <#list benchmarkReport.plannerBenchmarkResult.solverBenchmarkResultList as solverBenchmarkResult>
                                        <tr<#if solverBenchmarkResult.favorite> class="table-success"</#if>>
                                            <th>${solverBenchmarkResult.name}&nbsp;<@addSolverBenchmarkBadges solverBenchmarkResult=solverBenchmarkResult/></th>
                                            <td>${solverBenchmarkResult.totalWinningScoreDifference!""}</td>
                                            <td>${solverBenchmarkResult.averageWinningScoreDifference!""}</td>
                                            <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                                <#if !solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)??>
                                                    <td></td>
                                                <#else>
                                                    <#assign singleBenchmarkResult = solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)>
                                                    <#if !singleBenchmarkResult.hasAllSuccess()>
                                                        <td><span class="label label-important">Failed</span></td>
                                                    <#else>
                                                        <td>${singleBenchmarkResult.winningScoreDifference}&nbsp;<@addSolverProblemBenchmarkResultBadges solverProblemBenchmarkResult=singleBenchmarkResult/></td>
                                                    </#if>
                                                </#if>
                                            </#list>
                                        </tr>
                                    </#list>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="tab-pane show" id="summary_worstScoreDifferencePercentage-tab-pane">
                        <h3 class="visually-hidden">Worst score difference percentage summary (ROI)</h3>
                        <p>Useful for visualizing the return on investment (ROI) to decision makers.</p>
                        <@addChartList chartList=benchmarkReport.worstScoreDifferencePercentageSummaryChartList idPrefix="summary_worstScoreDifferencePercentage" />
                        <div class="table-responsive">
                            <table class="table table-hover table-striped table-bordered">
                                <thead>
                                    <tr>
                                        <th rowspan="2">Solver</th>
                                        <th rowspan="2">Average</th>
                                        <th colspan="${benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList?size}">Problem</th>
                                    </tr>
                                    <tr>
                                    <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                        <th>${problemBenchmarkResult.name}</th>
                                    </#list>
                                    </tr>
                                </thead>
                                <tbody class="table-group-divider">
                                    <#list benchmarkReport.plannerBenchmarkResult.solverBenchmarkResultList as solverBenchmarkResult>
                                        <tr<#if solverBenchmarkResult.favorite> class="table-success"</#if>>
                                            <th>${solverBenchmarkResult.name}&nbsp;<@addSolverBenchmarkBadges solverBenchmarkResult=solverBenchmarkResult/></th>
                                            <#if !solverBenchmarkResult.averageWorstScoreDifferencePercentage??>
                                                <td></td>
                                            <#else>
                                                <td>${solverBenchmarkResult.averageWorstScoreDifferencePercentage.toString(.locale_object)}</td>
                                            </#if>
                                            <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                                <#if !solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)??>
                                                    <td></td>
                                                <#else>
                                                    <#assign singleBenchmarkResult = solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)>
                                                    <#if !singleBenchmarkResult.hasAllSuccess()>
                                                        <td><span class="label label-important">Failed</span></td>
                                                    <#else>
                                                        <td>${singleBenchmarkResult.worstScoreDifferencePercentage.toString(.locale_object)}&nbsp;<@addSolverProblemBenchmarkResultBadges solverProblemBenchmarkResult=singleBenchmarkResult/></td>
                                                    </#if>
                                                </#if>
                                            </#list>
                                        </tr>
                                    </#list>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <section id="summary_performance">
            <h2>Performance summary</h2>
            <div class="tabbable">
                <ul class="nav nav-pills justify-content-center">
                    <li class="active">
                        <button class="nav-link active" id="summary_scoreCalculationSpeed-tab" data-bs-toggle="pill" data-bs-target="#summary_scoreCalculationSpeed-tab-pane" type="button">Score calculation speed</button>
                    </li>
                    <li>
                        <button class="nav-link" id="summary_worstScoreCalculationSpeedDifferencePercentage-tab" data-bs-toggle="pill" data-bs-target="#summary_worstScoreCalculationSpeedDifferencePercentage-tab-pane" type="button">Worst score calculation speed difference percentage</button>
                    </li>
                    <li>
                        <button class="nav-link" id="summary_timeSpent-tab" data-bs-toggle="pill" data-bs-target="#summary_timeSpent-tab-pane" type="button">Time spent</button>
                    </li>
                    <li>
                        <button class="nav-link" id="summary_timeSpentScalability-tab" data-bs-toggle="pill" data-bs-target="#summary_timeSpentScalability-tab-pane" type="button">Time spent scalability</button>
                    </li>
                    <li>
                        <button class="nav-link" id="summary_bestScorePerTimeSpent-tab" data-bs-toggle="pill" data-bs-target="#summary_bestScorePerTimeSpent-tab-pane" type="button">Best score per time spent</button>
                    </li>
                </ul>
                <div class="tab-content p-2">
                    <div class="tab-pane show active" id="summary_scoreCalculationSpeed-tab-pane">
                        <h3 class="visually-hidden">Score calculation speed summary</h3>
                        <p>
                            Useful for comparing different score calculators and/or constraint implementations
                            (presuming that the solver configurations do not differ otherwise).
                            Also useful to measure the scalability cost of an extra constraint.
                        </p>
                        <@addChart chart=benchmarkReport.scoreCalculationSpeedSummaryChart />
                        <div class="table-responsive">
                            <table class="table table-hover table-striped table-bordered">
                                <thead>
                                    <tr>
                                        <th rowspan="2">Solver</th>
                                        <th rowspan="2">Average</th>
                                        <th colspan="${benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList?size}">Problem</th>
                                    </tr>
                                    <tr>
                                        <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                            <th>${problemBenchmarkResult.name}</th>
                                        </#list>
                                    </tr>
                                    <tr class="table-info">
                                        <th>Problem scale</th>
                                        <td>${benchmarkReport.plannerBenchmarkResult.averageProblemScale!""}</td>
                                        <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                            <td class="problemScale">${problemBenchmarkResult.problemScale!""}</td>
                                        </#list>
                                    </tr>
                                </thead>
                                <tbody class="table-group-divider">
                                    <#list benchmarkReport.plannerBenchmarkResult.solverBenchmarkResultList as solverBenchmarkResult>
                                        <tr<#if solverBenchmarkResult.favorite> class="table-success"</#if>>
                                            <th>${solverBenchmarkResult.name}&nbsp;<@addSolverBenchmarkBadges solverBenchmarkResult=solverBenchmarkResult/></th>
                                            <td>${solverBenchmarkResult.averageScoreCalculationSpeed!""}/s</td>
                                            <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                                <#if !solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)??>
                                                    <td></td>
                                                <#else>
                                                    <#assign singleBenchmarkResult = solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)>
                                                    <#if !singleBenchmarkResult.hasAllSuccess()>
                                                        <td><span class="label label-important">Failed</span></td>
                                                    <#else>
                                                        <#if solverBenchmarkResult.subSingleCount lte 1>
                                                            <td>${singleBenchmarkResult.scoreCalculationSpeed}/s</td>
                                                        <#else>
                                                            <td>
                                                                <span class="dropdown">
                                                                    <button class="btn btn-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown">
                                                                        ${singleBenchmarkResult.scoreCalculationSpeed!""}/s&nbsp;<@addSolverProblemBenchmarkResultBadges solverProblemBenchmarkResult=singleBenchmarkResult/>
                                                                    </button>
                                                                    <ul class="dropdown-menu">
                                                                        <#list singleBenchmarkResult.subSingleBenchmarkResultList as subSingleBenchmarkResult>
                                                                            <li class="dropdown-header"><strong>Run #${subSingleBenchmarkResult.getSubSingleBenchmarkIndex()}</strong></li>
                                                                            <li class="dropdown-item">${subSingleBenchmarkResult.scoreCalculationSpeed!""}/s</li>
                                                                        </#list>
                                                                    </ul>
                                                                </span>
                                                            </td>
                                                        </#if>
                                                    </#if>
                                                </#if>
                                            </#list>
                                        </tr>
                                    </#list>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="tab-pane show" id="summary_worstScoreCalculationSpeedDifferencePercentage-tab-pane">
                        <h3 class="visually-hidden">Worst score calculation speed difference percentage</h3>
                        <p>
                            Useful for comparing different score calculators and/or constraint implementations
                            (presuming that the solver configurations do not differ otherwise).
                            Also useful to measure the scalability cost of an extra constraint.
                        </p>
                        <@addChart chart=benchmarkReport.worstScoreCalculationSpeedDifferencePercentageSummaryChart />
                        <div class="table-responsive">
                            <table class="table table-hover table-striped table-bordered">
                                <thead>
                                    <tr>
                                        <th rowspan="2">Solver</th>
                                        <th rowspan="2">Average</th>
                                        <th colspan="${benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList?size}">Problem</th>
                                    </tr>
                                    <tr>
                                        <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                            <th>${problemBenchmarkResult.name}</th>
                                        </#list>
                                    </tr>
                                </thead>
                                <tbody class="table-group-divider">
                                    <#list benchmarkReport.plannerBenchmarkResult.solverBenchmarkResultList as solverBenchmarkResult>
                                        <tr<#if solverBenchmarkResult.favorite> class="table-success"</#if>>
                                            <th>${solverBenchmarkResult.name}&nbsp;<@addSolverBenchmarkBadges solverBenchmarkResult=solverBenchmarkResult/></th>
                                            <#if solverBenchmarkResult.averageWorstScoreCalculationSpeedDifferencePercentage??>
                                                <td>${solverBenchmarkResult.averageWorstScoreCalculationSpeedDifferencePercentage?string["0.00%"]!""}</td>
                                            <#else>
                                                <td></td>
                                            </#if>
                                            <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                                <#if !solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)??>
                                                    <td></td>
                                                <#else>
                                                    <#assign singleBenchmarkResult = solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)>
                                                    <#if !singleBenchmarkResult.hasAllSuccess()>
                                                        <td><span class="label label-important">Failed</span></td>
                                                    <#else>
                                                        <td>${singleBenchmarkResult.worstScoreCalculationSpeedDifferencePercentage?string["0.00%"]!""}</td>
                                                    </#if>
                                                </#if>
                                            </#list>
                                        </tr>
                                    </#list>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="tab-pane show" id="summary_timeSpent-tab-pane">
                        <h3 class="visually-hidden">Time spent summary</h3>
                        <p>Useful for visualizing the performance of construction heuristics (presuming that no other solver phases are configured).</p>
                        <@addChart chart=benchmarkReport.timeSpentSummaryChart />
                        <div class="table-responsive">
                            <table class="table table-striped table-bordered">
                                <thead>
                                    <tr>
                                        <th rowspan="2">Solver</th>
                                        <th rowspan="2">Average</th>
                                        <th colspan="${benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList?size}">Problem</th>
                                    </tr>
                                    <tr>
                                        <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                            <th>${problemBenchmarkResult.name}</th>
                                        </#list>
                                    </tr>
                                    <tr class="table-info">
                                        <th>Problem scale</th>
                                        <td>${benchmarkReport.plannerBenchmarkResult.averageProblemScale!""}</td>
                                        <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                            <td class="problemScale">${problemBenchmarkResult.problemScale!""}</td>
                                        </#list>
                                    </tr>
                                </thead>
                                <tbody class="table-group-divider">
                                    <#list benchmarkReport.plannerBenchmarkResult.solverBenchmarkResultList as solverBenchmarkResult>
                                        <tr<#if solverBenchmarkResult.favorite> class="table-success"</#if>>
                                            <th>${solverBenchmarkResult.name}&nbsp;<@addSolverBenchmarkBadges solverBenchmarkResult=solverBenchmarkResult/></th>
                                            <td>${solverBenchmarkResult.averageTimeMillisSpent!0?string.@msDuration}</td>
                                            <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                                                <#if !solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)??>
                                                    <td></td>
                                                <#else>
                                                    <#assign singleBenchmarkResult = solverBenchmarkResult.findSingleBenchmark(problemBenchmarkResult)>
                                                    <#if !singleBenchmarkResult.hasAllSuccess()>
                                                        <td><span class="label label-important">Failed</span></td>
                                                    <#else>
                                                        <#if solverBenchmarkResult.subSingleCount lte 1>
                                                            <td>${singleBenchmarkResult.timeMillisSpent?string.@msDuration}</td>
                                                        <#else>
                                                            <td>
                                                                <span class="dropdown">
                                                                    <button class="btn btn-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown">
                                                                        ${singleBenchmarkResult.timeMillisSpent?string.@msDuration}/s&nbsp;<@addSolverProblemBenchmarkResultBadges solverProblemBenchmarkResult=singleBenchmarkResult/>
                                                                    </button>
                                                                    <ul class="dropdown-menu">
                                                                        <#list singleBenchmarkResult.subSingleBenchmarkResultList as subSingleBenchmarkResult>
                                                                            <li class="dropdown-header">Run #${subSingleBenchmarkResult.getSubSingleBenchmarkIndex()}</li>
                                                                            <li class="dropdown-item">${subSingleBenchmarkResult.timeMillisSpent?string.@msDuration}</li>
                                                                        </#list>
                                                                    </ul>
                                                                </span>
                                                            </td>
                                                        </#if>
                                                    </#if>
                                                </#if>
                                            </#list>
                                        </tr>
                                    </#list>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="tab-pane show" id="summary_timeSpentScalability-tab-pane">
                        <h3 class="visually-hidden">Time spent scalability summary</h3>
                        <p>Useful for extrapolating the scalability of construction heuristics (presuming that no other solver phases are configured).</p>
                        <@addChart chart=benchmarkReport.timeSpentScalabilitySummaryChart />
                    </div>
                    <div class="tab-pane show" id="summary_bestScorePerTimeSpent-tab-pane">
                        <h3 class="visually-hidden">Best score per time spent summary</h3>
                        <p>Useful for visualizing trade-off between the best score versus the time spent for construction heuristics (presuming that no other solver phases are configured).</p>
                        <@addChartList chartList=benchmarkReport.bestScorePerTimeSpentSummaryChartList idPrefix="summary_bestScorePerTimeSpent" />
                    </div>
                </div>
            </div>
        </section>

        <section id="problemBenchmarkResult">
            <h2>Problem benchmarks</h2>
            <#list benchmarkReport.plannerBenchmarkResult.unifiedProblemBenchmarkResultList as problemBenchmarkResult>
                <section id="problemBenchmark_${problemBenchmarkResult.anchorId}">
                    <h3>${problemBenchmarkResult.name}</h3>
                    <#if problemBenchmarkResult.hasAnyFailure()>
                        <div class="alert alert-danger">
                            <p>${problemBenchmarkResult.failureCount} benchmarks have failed!</p>
                        </div>
                    </#if>
                    <table class="table table-striped">
                        <tbody>
                            <tr>
                                <th>Entity count</th>
                                <td>${problemBenchmarkResult.entityCount!""}</td>
                            </tr>
                            <tr>
                                <th>Variable count</th>
                                <td>${problemBenchmarkResult.variableCount!""}</td>
                            </tr>
                            <tr>
                                <th>Maximum value count</th>
                                <td>${problemBenchmarkResult.maximumValueCount!""}</td>
                            </tr>
                            <tr>
                                <th>Problem scale</th>
                                <td>${problemBenchmarkResult.problemScale!""}</td>
                            </tr>
                            <#if problemBenchmarkResult.inputSolutionLoadingTimeMillisSpent??>
                                <tr>
                                    <th>Time to load input solution</th>
                                    <td><#if problemBenchmarkResult.inputSolutionLoadingTimeMillisSpent lt 1>&lt; 1 ms<#else>${problemBenchmarkResult.inputSolutionLoadingTimeMillisSpent?string.@msDuration}</#if></td>
                                </tr>
                            </#if>
                            <#if problemBenchmarkResult.averageUsedMemoryAfterInputSolution??>
                                <tr>
                                    <th>Memory usage after loading input solution</th>
                                    <td>${problemBenchmarkResult.averageUsedMemoryAfterInputSolution?string.number} bytes on average</td>
                                </tr>
                            </#if>
                        </tbody>
                    </table>
                    <#if problemBenchmarkResult.hasAnySuccess() && problemBenchmarkResult.hasAnyStatistic()>
                        <#if problemBenchmarkResult.getMaximumSubSingleCount() gt 1>
                            <div class="alert alert-info">
                                <p>Only the median sub single run of each solver is shown in the statistics below.</p>
                            </div>
                        </#if>
                        <div class="tabbable">
                            <ul class="nav nav-pills justify-content-center">
                                <#assign firstRow = true>
                                <#list problemBenchmarkResult.problemStatisticList as problemStatistic>
                                    <#assign tabId = "problemStatistic_" + problemStatistic.anchorId />
                                    <li class="nav-item">
                                        <button class="nav-link<#if firstRow> active</#if>" id="${tabId}-tab" data-bs-toggle="pill" data-bs-target="#${tabId}-tab-pane" type="button">${problemStatistic.problemStatisticType.label?capitalize}</button>
                                    </li>
                                    <#assign firstRow = false>
                                </#list>
                                <#list problemBenchmarkResult.extractSingleStatisticTypeList() as singleStatisticType>
                                    <#assign tabId = "singleStatistic_" + problemBenchmarkResult.anchorId + "_" + singleStatisticType.anchorId />
                                    <li<#if firstRow> class="active"</#if>>
                                        <button class="nav-link<#if firstRow> active</#if>" id="${tabId}-tab" data-bs-toggle="pill" data-bs-target="#${tabId}-tab-pane" type="button">${singleStatisticType.label?capitalize}</button>
                                    </li>
                                    <#assign firstRow = false>
                                </#list>
                            </ul>
                            <div class="tab-content p-2">
                                <#assign firstRow = true>
                                <#list problemBenchmarkResult.problemStatisticList as problemStatistic>
                                    <#assign tabId = "problemStatistic_" + problemStatistic.anchorId />
                                    <div class="tab-pane show<#if firstRow> active</#if>" id="${tabId}-tab-pane">
                                        <#-- TODO somehow figure out the solver name; sub single stats have it. -->
                                        <#list problemStatistic.getWarningList() as warning>
                                            <div class="alert alert-warning">
                                                <p>${warning}</p>
                                            </div>
                                        </#list>
                                        <#assign chartList = problemStatistic.getChartList()>
                                        <#if chartList?size != 0>
                                            <#if problemStatistic.problemStatisticType.hasScoreLevels()>
                                                <@addChartList chartList=chartList idPrefix="problemStatistic_" + problemStatistic.anchorId />
                                            <#else>
                                                <@addChart chart=chartList[0] />
                                            </#if>
                                        <#else>
                                            <div class="alert alert-warning">
                                                <p>Graph not available. Either the statistic is not available for this solver configuration, or the benchmark failed.</p>
                                            </div>
                                        </#if>
                                        <#if !benchmarkReport.plannerBenchmarkResult.aggregation>
                                            <span>CSV files per solver:</span>
                                            <div class="btn-group">
                                                <#list problemStatistic.subSingleStatisticList as subSingleStatistic>
                                                    <button class="btn" onclick="window.location.href='${subSingleStatistic.relativeCsvFilePath}'"><i class="fa fa-solid fa-download"></i></button>
                                                </#list>
                                            </div>
                                        </#if>
                                    </div>
                                    <#assign firstRow = false>
                                </#list>
                                <#list problemBenchmarkResult.extractSingleStatisticTypeList() as singleStatisticType>
                                    <#assign tabId = "singleStatistic_" + problemBenchmarkResult.anchorId + "_" + singleStatisticType.anchorId />
                                    <div class="tab-pane show<#if firstRow> active</#if>" id="${tabId}-tab-pane">
                                        <#list problemBenchmarkResult.extractPureSubSingleStatisticList(singleStatisticType) as pureSubSingleStatistic>
                                            <h4>${pureSubSingleStatistic.subSingleBenchmarkResult.singleBenchmarkResult.solverBenchmarkResult.name}</h4>
                                            <#assign chartList = pureSubSingleStatistic.getChartList()>
                                            <#if chartList?size != 0>
                                                <#if singleStatisticType.hasScoreLevels()>
                                                    <@addChartList chartList=chartList idPrefix="singleStatistic_" + problemBenchmarkResult.anchorId + "_" + singleStatisticType.anchorId />
                                                <#else>
                                                    <@addChart chart=chartList[0] />
                                                </#if>
                                            <#else>
                                                <div class="alert alert-warning">
                                                    <p>Graph not available. Either the statistic is not available for this solver configuration, or the benchmark failed.</p>
                                                </div>
                                            </#if>
                                            <#if !benchmarkReport.plannerBenchmarkResult.aggregation>
                                                <span>CSV file:</span>
                                                <div class="btn-group">
                                                    <button class="btn" onclick="window.location.href='${pureSubSingleStatistic.relativeCsvFilePath}'"><i class="fa fa-solid fa-download"></i></button>
                                                </div>
                                            </#if>
                                        </#list>
                                    </div>
                                    <#assign firstRow = false>
                                </#list>
                            </div>
                        </div>
                    </#if>
                    <#list problemBenchmarkResult.singleBenchmarkResultList as singleBenchmarkResult>
                        <section id="singleBenchmark_${singleBenchmarkResult.anchorId}">
                            <h4>${singleBenchmarkResult.name}</h4>
                            <#if singleBenchmarkResult.hasAnyFailure()>
                                <div class="alert alert-danger">
                                    <p>${singleBenchmarkResult.failureCount} benchmarks have failed!</p>
                                </div>
                            <#else>
                                <#if singleBenchmarkResult.getScoreExplanationSummary()??>
                                    <#if singleBenchmarkResult.getSubSingleCount() gt 1 >
                                        <div class="alert alert-info">
                                            <p>Only the median sub single run of each solver is shown in the statistics below.</p>
                                        </div>
                                    </#if>
                                    <pre class="prettyprint p-2">${singleBenchmarkResult.scoreExplanationSummary}</pre>
                                <#else>
                                    <p>Score summary not provided.</p>
                                </#if>
                            </#if>
                        </section>
                    </#list>
                </section>
            </#list>
        </section>

        <section id="solverBenchmarkResult">
            <h2>Solver benchmarks</h2>
            <ul class="nav nav-pills justify-content-center p-2" id="solverBenchmarkResultTabs">
                <#assign firstRow = true>
                <#list benchmarkReport.plannerBenchmarkResult.solverBenchmarkResultList as solverBenchmarkResult>
                    <#assign tabId="solverBenchmark_" + solverBenchmarkResult.anchorId + "_config" />
                    <li class="nav-item">
                        <button class="nav-link<#if firstRow> active</#if>" id="${tabId}-tab" data-bs-toggle="pill" data-bs-target="#${tabId}-tab-pane" type="button">
                            ${solverBenchmarkResult.name}&nbsp;<@addSolverBenchmarkBadges solverBenchmarkResult=solverBenchmarkResult/>
                        </button>
                    </li>
                    <#assign firstRow = false>
                </#list>
            </ul>
            <div class="tab-content">
                <#assign firstRow = true>
                <#list benchmarkReport.plannerBenchmarkResult.solverBenchmarkResultList as solverBenchmarkResult>
                    <#assign tabId="solverBenchmark_" + solverBenchmarkResult.anchorId + "_config" />
                    <div class="tab-pane show<#if firstRow> active</#if>" id="${tabId}-tab-pane">
                        <div class="tab-content" id="${tabId}-tabContent">
                            <#if solverBenchmarkResult.hasAnyFailure()>
                                <div class="alert alert-danger">
                                    <p>${solverBenchmarkResult.failureCount} benchmarks have failed!</p>
                                </div>
                            </#if>
                            <pre class="prettyprint language-xml p-2"><code>${solverBenchmarkResult.solverConfigAsString}</code></pre>
                        </div>
                    </div>
                    <#assign firstRow = false>
                </#list>
            </div>
        </section>

        <section id="environmentInformation">
            <h2>Environment Information</h2>
            <table class="table table-striped">
                <tr>
                    <th>Name</th>
                    <td>${benchmarkReport.plannerBenchmarkResult.name}</td>
                </tr>
                <tr>
                    <th>Aggregation</th>
                    <td>${benchmarkReport.plannerBenchmarkResult.aggregation?string}</td>
                </tr>
                <tr>
                    <th>Failure count</th>
                    <td>${benchmarkReport.plannerBenchmarkResult.failureCount}</td>
                </tr>
                <tr>
                    <th>Starting timestamp</th>
                    <td>${(benchmarkReport.plannerBenchmarkResult.startingTimestampAsMediumString)!"Differs"}</td>
                </tr>
                <tr>
                    <th>Warm-up time spent</th>
                    <#if benchmarkReport.plannerBenchmarkResult.warmUpTimeMillisSpentLimit??>
                        <td>${benchmarkReport.plannerBenchmarkResult.warmUpTimeMillisSpentLimit?string.@msDuration}</td>
                    <#else>
                        <td>Differs</td>
                    </#if>
                </tr>
                <tr>
                    <th>Parallel benchmark count / available processors</th>
                    <#if benchmarkReport.plannerBenchmarkResult.parallelBenchmarkCount?? && benchmarkReport.plannerBenchmarkResult.availableProcessors??>
                        <td>${benchmarkReport.plannerBenchmarkResult.parallelBenchmarkCount} / ${benchmarkReport.plannerBenchmarkResult.availableProcessors}</td>
                    <#else>
                        <td>Differs</td>
                    </#if>
                </tr>
                <tr>
                    <th>Benchmark time spent</th>
                    <#if benchmarkReport.plannerBenchmarkResult.benchmarkTimeMillisSpent??>
                        <td>${benchmarkReport.plannerBenchmarkResult.benchmarkTimeMillisSpent?string.@msDuration}</td>
                    <#else>
                        <td>Differs</td>
                    </#if>
                </tr>
                <tr>
                    <th>Environment mode</th>
                    <td>${benchmarkReport.plannerBenchmarkResult.environmentMode!"Differs"}</td>
                </tr>
                <tr>
                    <th>Logging level for ai.timefold.solver.core</th>
                    <td>${benchmarkReport.plannerBenchmarkResult.loggingLevelTimefoldSolverCore!"Differs"}</td>
                </tr>
                <tr>
                    <th>Solver ranking class</th>
                    <td>
                        <span data-toggle="tooltip" title="${benchmarkReport.solverRankingClassFullName!"Unknown"}">
                            ${benchmarkReport.solverRankingClassSimpleName!"Unknown"}
                        </span>
                    </td>
                </tr>
                <tr>
                    <th>VM max memory (as in -Xmx but lower)</th>
                    <#if (benchmarkReport.plannerBenchmarkResult.maxMemory?string.number)??>
                        <td>${benchmarkReport.plannerBenchmarkResult.maxMemory?string.number} bytes</td>
                    <#else>
                        <td>Differs</td>
                    </#if>
                </tr>
                <tr>
                    <th>Timefold version</th>
                    <td>${benchmarkReport.plannerBenchmarkResult.timefoldSolverVersion!"Differs"}</td>
                </tr>
                <tr>
                    <th>Java version</th>
                    <td>${benchmarkReport.plannerBenchmarkResult.javaVersion!"Differs"}</td>
                </tr>
                <tr>
                    <th>Java VM</th>
                    <td>${benchmarkReport.plannerBenchmarkResult.javaVM!"Differs"}</td>
                </tr>
                <tr>
                    <th>Operating system</th>
                    <td>${benchmarkReport.plannerBenchmarkResult.operatingSystem!"Differs"}</td>
                </tr>
                <tr>
                    <th>Report locale</th>
                    <td>${benchmarkReport.locale_object!"Unknown"}</td>
                </tr>
                <tr>
                    <th>Report timezone</th>
                    <td>${benchmarkReport.timezoneId!"Unknown"}</td>
                </tr>
            </table>
        </section>
    </main>
    <footer class="bg-black text-white-50">
        <div class="container">
            <div class="hstack gap-3 p-4">
                <div class="ms-auto"><a class="text-white" href="https://timefold.ai">Timefold</a></div>
                <div class="vr"></div>
                <div><a class="text-white" href="https://timefold.ai/docs">Documentation</a></div>
                <div class="vr"></div>
                <div><a class="text-white" href="https://github.com/TimefoldAI">Code</a></div>
                <div class="vr"></div>
                <div class="me-auto"><a class="text-white" href="https://timefold.ai/product/support/">Support</a></div>
            </div>
        </div>
    </footer>
</body>
</html>