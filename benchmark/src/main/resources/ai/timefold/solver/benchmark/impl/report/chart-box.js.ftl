<#ftl output_format="JavaScript"> <#-- So that Freemarker escapes automatically. -->
<#assign xAxisLabel>${chart.xLabel()}</#assign>
<#assign yAxisLabel>${chart.yLabel()}</#assign>
<#assign chartId = "chart_" + chart.id()>

var ${chartId} = new Chart(document.getElementById('${chartId}'), {
    type: 'boxplot',
    data: {
        labels: [
            <#list chart.categories() as category>'${category}'<#sep>, </#sep></#list>
        ],
        datasets: [
            <#assign ranges = chart.ranges() />
            <#list ranges?keys as rangeLabel>
                <#assign rangeList = ranges[rangeLabel] />
                {
                    label: '${rangeLabel}',
                    <#if chart.favorites()?seq_contains(rangeLabel)>
                        borderWidth: 4
                    <#else>
                        borderWidth: 1
                    </#if>,
                    data: [
                        <#list rangeList as range>
                            <#if range??>
                                {
                                    min: ${range.min()?cn},
                                    max: ${range.max()?cn},
                                    q1: ${range.q1()?cn},
                                    q3: ${range.q3()?cn},
                                    median: ${range.median()?cn},
                                    mean: ${range.average()?cn},
                                    items: [],
                                    outliers: [],
                                }
                            <#else>
                                null
                            </#if>
                            <#sep>, </#sep>
                        </#list>
                    ]
                }<#sep>, </#sep>
            </#list>
        ]
    },
    options: {
        animation: false,
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            title: {
                display: true,
                text: '${chart.title()}'
            }
        },
        scales: {
            x: {
                display: true
            },
            y: {
                title: {
                    display: true,
                    text: '${yAxisLabel}'
                },
                display: true
            }
        },
        <#include "shared-watermark.js.ftl" />
    },
    <#include "shared-background.js.ftl" />
});

window.addEventListener('beforeprint', () => {
  ${chartId}.resize(1280, 720);
});
window.addEventListener('afterprint', () => {
  ${chartId}.resize();
});