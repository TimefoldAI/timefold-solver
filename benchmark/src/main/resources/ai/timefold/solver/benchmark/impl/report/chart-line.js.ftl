<#ftl output_format="JavaScript"> <#-- So that Freemarker escapes automatically. -->
<#assign xAxisLogarithmic = chart.xLogarithmic()>
<#if xAxisLogarithmic>
    <#assign xAxisLabel>${chart.xLabel()} (logarithmic)</#assign>
<#else>
    <#assign xAxisLabel>${chart.xLabel()}</#assign>
</#if>
<#assign yAxisLogarithmic = chart.yLogarithmic()>
<#if yAxisLogarithmic>
    <#assign yAxisLabel>${chart.yLabel()} (logarithmic)</#assign>
<#else>
    <#assign yAxisLabel>${chart.yLabel()}</#assign>
</#if>
<#assign timeOnX = chart.timeOnX()>
<#assign timeOnY = chart.timeOnY()>
<#assign chartId = "chart_" + chart.id()>

var ${chartId} = new Chart(document.getElementById('${chartId}'), {
    type: 'line',
    data: {
        labels: [
            <#list chart.keys() as key>${key?cn}<#sep>, </#sep></#list>
        ],
        datasets: [
            <#list chart.datasets() as dataset>{
                  label: '${dataset.label()}',
                  <#if chart.stepped()>
                        stepped: true,
                        pointRadius: 0,
                  </#if>
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
        spanGaps: true,
        plugins: {
            title: {
                display: true,
                text: '${chart.title()}'
            },
            tooltip: {
                callbacks: {
                    <#if timeOnX>
                        title: function(context) {
                            return humanizeTime(context[0].parsed.x);
                        }
                        <#if timeOnY>,</#if>
                    </#if>
                    <#if timeOnY>
                        label: function(context) {
                            let label = context.dataset.label || '';
                            return label + ": " + humanizeTime(context.parsed.y);
                        }
                    </#if>
                }
            }
        },
        scales: {
            x: {
                title: {
                    display: true,
                    text: '${xAxisLabel}'
                },
                ticks: {
                    <#if !xAxisLogarithmic>
                        stepSize: ${chart.xStepSize()?cn}
                        <#if timeOnX>,</#if>
                    </#if>
                    <#if timeOnX>
                        callback: function(value, index) {
                            return humanizeTime(value);
                        }
                    </#if>
                },
                type: '<#if xAxisLogarithmic>logarithmic<#else>linear</#if>',
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
