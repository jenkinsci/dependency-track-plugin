/* global echarts, dtrackTrendAction */
'use strict';

(function () {
    dtrackTrendAction.getSeverityDistributionTrend(result => {
        const data = result.responseJSON || [];
        if (data.length) {
            const container = document.getElementById('dtrackTrend-history-chart');
            const textColor = window.getComputedStyle(container).getPropertyValue('color');
            const fontFamily = window.getComputedStyle(container).getPropertyValue('font-family');
            const options = {
                tooltip: {
                    trigger: 'axis'
                },
                legend: {
                    data: ['Critical', 'High', 'Medium', 'Low', 'Info', 'Unassigned'],
                    orient: 'horizontal',
                    x: 'center',
                    y: 'bottom',
                    textStyle: {
                        color: textColor,
                        fontFamily,
                    }
                },
                grid: {
                    left: '20',
                    right: '10',
                    bottom: '20',
                    top: '10',
                    containLabel: true
                },
                xAxis: [
                    {
                        type: 'category',
                        boundaryGap: false,
                        data: data.map(run => '#' + run.buildNumber)
                    }
                ],
                yAxis: [
                    {
                        name: 'Vulnerabilities',
                        nameLocation: 'center',
                        boundaryGap: false,
                        nameGap: '30',
                        nameRotate: '90',
                        type: 'value'
                    }
                ],
                color: ['#dc0000', '#fd8c00', '#fdc500', '#4cae4c', '#357abd', '#c0c0c0'],
                textStyle: {
                    color: textColor,
                    fontFamily,
                },
                series: [
                    {
                        name: 'Critical',
                        type: 'line',
                        data: data.map(run => run.critical)
                    },
                    {
                        name: 'High',
                        type: 'line',
                        data: data.map(run => run.high)
                    },
                    {
                        name: 'Medium',
                        type: 'line',
                        data: data.map(run => run.medium)
                    },
                    {
                        name: 'Low',
                        type: 'line',
                        data: data.map(run => run.low)
                    },
                    {
                        name: 'Info',
                        type: 'line',
                        data: data.map(run => run.info)
                    },
                    {
                        name: 'Unassigned',
                        type: 'line',
                        data: data.map(run => run.unassigned)
                    }
                ]
            };

            const chart = echarts.init(container);
            chart.setOption(options);
            chart.resize();
            window.addEventListener('resize', () => {
                chart.resize();
            });
        }
    });
})();
