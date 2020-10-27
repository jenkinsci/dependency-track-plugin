/* global echarts */
'use strict';

/**
 * Renders a trend chart in the specified div using ECharts.
 *
 * @param {String} chartDivId - the ID of the div where the chart should be shown in
 * @param {JSON} chartModel - the line chart model
 */
function renderTrendChart(chartDivId, chartModel) {
    var chart = echarts.init(document.getElementById(chartDivId));

    var builds = [];
    var critical = [];
    var high = [];
    var medium = [];
    var low = [];
    var info = [];
    var unassigned = [];
    for (var i=0; i<chartModel.length; i++) {
        builds.unshift("#" + chartModel[i].buildNumber);
        critical.unshift(chartModel[i].critical);
        high.unshift(chartModel[i].high);
        medium.unshift(chartModel[i].medium);
        low.unshift(chartModel[i].low);
        info.unshift(chartModel[i].info);
        unassigned.unshift(chartModel[i].unassigned);
    }

    var options = {
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: ['Critical', 'High', 'Medium', 'Low', 'Unassigned'],
            orient: 'horizontal',
            x: 'center',
            y: 'bottom'
        },
        grid: {
            left: '20',
            right: '10',
            bottom: '20',
            top: '10',
            containLabel: true
        },
        xAxis : [
            {
                type : 'category',
                boundaryGap : false,
                data : builds
            }
        ],
        yAxis : [
            {
                name: 'vulnerabilities',
                nameLocation: 'center',
                boundaryGap : false,
                nameGap: '30',
                nameRotate: '90',
                type: 'value'
            }
        ],
        color: ['#dc0000', '#fd8c00', '#fdc500', '#4cae4c', '#c0c0c0'],
        series : [
            {
                name: 'Critical',
                type: 'line',
                data: critical
            },
            {
                name: 'High',
                type: 'line',
                data: high
            },
            {
                name: 'Medium',
                type: 'line',
                data: medium
            },
            {
                name: 'Low',
                type: 'line',
                data: low
            },
            {
                name: 'Unassigned',
                type: 'line',
                data: unassigned
            }
        ]
    };

    chart.setOption(options);
    chart.resize();
    window.onresize = function() {
        chart.resize();
    };
}
