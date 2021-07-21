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

    const container = document.getElementById('dtrackTrend-history-chart');
    const textColor = window.getComputedStyle(container).getPropertyValue('color');
    const fontFamily = window.getComputedStyle(container).getPropertyValue('font-family');
    const chart = echarts.init(container);
    chart.setOption({
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                label: {
                    formatter: 'Vulnerabilities of Build #{value}',
                },
            },
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
            left: 20,
            right: 10,
            bottom: 30,
            top: 10,
            containLabel: true
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            axisLabel: {
                fontWeight: 'bolder',
                formatter: '#{value}',
            },
            triggerEvent: true,
        },
        yAxis: [
            {
                name: 'Vulnerabilities',
                nameLocation: 'center',
                boundaryGap: false,
                nameGap: 30,
                nameRotate: 90,
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
                z: 5,
                emphasis: {
                    focus: 'series',
                },
            },
            {
                name: 'High',
                type: 'line',
                z: 4,
                emphasis: {
                    focus: 'series',
                },
            },
            {
                name: 'Medium',
                type: 'line',
                z: 3,
                emphasis: {
                    focus: 'series',
                },
            },
            {
                name: 'Low',
                type: 'line',
                z: 2,
                emphasis: {
                    focus: 'series',
                },
            },
            {
                name: 'Info',
                type: 'line',
                z: 1,
                emphasis: {
                    focus: 'series',
                },
            },
            {
                name: 'Unassigned',
                type: 'line',
                z: 0,
                emphasis: {
                    focus: 'series',
                },
            }
        ]
    });
    chart.resize();
    chart.on('click', 'xAxis', event => {
        if (event.targetType === 'axisLabel' && event.value) {
            window.location += parseInt(event.value, 10) + '/dependency-track-findings';
        }
    });
    window.addEventListener('resize', () => {
        chart.resize();
    });

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
            chart.setOption({
                xAxis: {
                    data: data.map(run => run.buildNumber),
                },
                series: [
                    {
                        name: 'Critical',
                        data: data.map(run => run.critical)
                    },
                    {
                        name: 'High',
                        data: data.map(run => run.high)
                    },
                    {
                        name: 'Medium',
                        data: data.map(run => run.medium)
                    },
                    {
                        name: 'Low',
                        data: data.map(run => run.low)
                    },
                    {
                        name: 'Info',
                        data: data.map(run => run.info)
                    },
                    {
                        name: 'Unassigned',
                        data: data.map(run => run.unassigned)
                    }
                ]
            });
        }
    });
})();
