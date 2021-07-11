/* global echarts */
'use strict';

(function () {
    const actionUrl = new URL(document.currentScript.dataset.actionUrl, window.location.origin);
    if (!(actionUrl.origin === window.location.origin
            && /^https?:$/.test(actionUrl.protocol)
            && actionUrl.pathname.startsWith(`${document.head.dataset.rooturl}/$stapler/bound/`)
        )) {
        throw new Error('malicious URL in data-action-url detected!');
    }
    
    const crumbHeaderName = document.head.dataset.crumbHeader || 'Jenkins-Crumb';
    const crumbValue = document.head.dataset.crumbValue || '';
    const fetchHeaders = {
        'Content-Type': 'application/x-stapler-method-invocation;charset=UTF-8',
        Crumb: crumbValue
    };
    fetchHeaders[crumbHeaderName] = crumbValue;

    window.fetch(`${actionUrl.href}/getSeverityDistributionTrend`, {
        method: 'POST',
        mode: 'cors',
        cache: 'default',
        body: '[]',
        headers: fetchHeaders,
    })
    .then(response => {
        if (response.ok) {
            return response.json().then(data => Array.isArray(data) ? data : []);
        } else {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
    })
    .then(data => {
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
