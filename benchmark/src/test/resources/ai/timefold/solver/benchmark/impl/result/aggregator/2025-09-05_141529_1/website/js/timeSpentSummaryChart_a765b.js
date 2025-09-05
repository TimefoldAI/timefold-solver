
var chart_timeSpentSummaryChart_a765b = new Chart(document.getElementById('chart_timeSpentSummaryChart_a765b'), {
    type: 'bar',
    data: {
        labels: [
            'input'
        ],
        datasets: [
            {
                  label: 'Config_0 (favorite)',
                  grouped: true,
                    borderWidth: 4
,
                  data: [
                    0
                  ]
                }
        ]
    },
    options: {
        animation: false,
        responsive: true,
        maintainAspectRatio: false,
        resizeDelay: 100,
        spanGaps: true,
        plugins: {
                tooltip: {
                    callbacks: {
                            label: function(context) {
                                let label = context.dataset.label || '';
                                return label + ": " + humanizeTime(context.parsed.y);
                            }
                    }
                },
            title: {
                display: true,
                text: 'Time spent summary (lower time is better)'
            }
        },
        scales: {
            x: {
                display: true
            },
            y: {
                title: {
                    display: true,
                    text: 'Time spent'
                },
                ticks: {
                        stepSize: 1
                        ,
                        callback: function(value, index, ticks) {
                            return humanizeTime(value);
                        }
                },
                type: 'linear',
                display: true
            }
        },
watermark: {
    image: "website/img/timefold-logo-stacked-positive.svg",
    x: 15,
    y: 15,
    width: 48,
    height: 50,
    opacity: 0.1,
    alignX: "right",
    alignY: "bottom",
    alignToChartArea: true,
    position: "front",
}    },
plugins: [{ 
    id: 'customPlugin',
    beforeDraw: (chart, args, options) => {
          const ctx = chart.canvas.getContext('2d');
          ctx.save();
          ctx.globalCompositeOperation = 'destination-over';
          ctx.fillStyle = 'white';
          ctx.fillRect(0, 0, chart.canvas.width, chart.canvas.height);
          ctx.restore();
    }
}]
});

window.addEventListener('beforeprint', () => {
  chart_timeSpentSummaryChart_a765b.resize(1280, 720);
});
window.addEventListener('afterprint', () => {
  chart_timeSpentSummaryChart_a765b.resize();
});