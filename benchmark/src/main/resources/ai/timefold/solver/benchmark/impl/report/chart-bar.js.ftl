<#ftl output_format="JavaScript"> <#-- So that Freemarker escapes automatically. -->
<#assign xAxisLabel>${chart.xLabel()}</#assign>
<#assign yAxisLogarithmic = chart.yLogarithmic()>
<#if yAxisLogarithmic>
    <#assign yAxisLabel>${chart.yLabel()} (logarithmic)</#assign>
<#else>
    <#assign yAxisLabel>${chart.yLabel()}</#assign>
</#if>
<#assign timeOnY = chart.timeOnY()>
<#assign chartId = "chart_" + chart.id()>

var ${chartId} = new Chart(document.getElementById('${chartId}'), {
    type: 'bar',
    data: {
        labels: [
            <#list chart.categories() as category>'${category}'<#sep>, </#sep></#list>
        ],
        datasets: [
            <#list chart.datasets() as dataset>{
                  label: '${dataset.label()}',
                  grouped: true,
                  <#if dataset.favorite()>
                    borderWidth: 4
                  <#else>
                    borderWidth: 1
                  </#if>,
                  data: [
                    <#list dataset.data() as datum><#if datum??>${datum?cn}</#if><#sep>, </#sep></#list>
                  ]
                }<#sep>, </#sep>
            </#list>
        ]
    },
    options: {
        animation: false,
        responsive: true,
        maintainAspectRatio: false,
        resizeDelay: 100,
        spanGaps: true,
        plugins: {
            <#if timeOnY>
                tooltip: {
                    callbacks: {
                            label: function(context) {
                                let label = context.dataset.label || '';
                                return label + ": " + humanizeTime(context.parsed.y);
                            }
                    }
                },
            </#if>
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
                ticks: {
                    <#if !yAxisLogarithmic>
                        stepSize: ${chart.yStepSize()?cn}
                        <#if timeOnY>,</#if>
                    </#if>
                    <#if timeOnY>
                        callback: function(value, index, ticks) {
                            return humanizeTime(value);
                        }
                    </#if>
                },
                suggestedMin: ${chart.yMin()?cn},
                suggestedMax: ${chart.yMax()?cn},
                type: '<#if yAxisLogarithmic>logarithmic<#else>linear</#if>',
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