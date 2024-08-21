import { init as echartsInit } from './libs/echarts.esm.min.js';
const currentScript = document.currentScript || document.querySelector('script[type="module"][src$="/plugin/dependency-track/js/charts-violations.js"][data-action-url]');

    const actionUrl = new URL(currentScript.dataset.actionUrl, window.location.origin);
    if (!(actionUrl.origin === window.location.origin
            && /^https?:$/.test(actionUrl.protocol)
            && actionUrl.pathname.startsWith(`${document.head.dataset.rooturl}/$stapler/bound/`)
    )) {
        throw new Error('malicious URL in data-action-url detected!');
    }

    const crumbHeaderName = document.head.dataset.crumbHeader || 'Jenkins-Crumb';
    const crumbValue = document.head.dataset.crumbValue || currentScript.dataset.crumbValue || '';
    const fetchHeaders = new Headers([
        ['Content-Type', 'application/x-stapler-method-invocation;charset=UTF-8'],
        ['Crumb', crumbValue],
        [crumbHeaderName, crumbValue],
    ]);

    const container = document.getElementById('dtrackTrend-history-chart-violations');
    const textColor = window.getComputedStyle(container).getPropertyValue('color');
    const fontFamily = window.getComputedStyle(container).getPropertyValue('font-family');
    const chart = echartsInit(container);
    chart.setOption({
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                label: {
                    formatter: 'Policy Violations of Build #{value}',
                },
            },
        },
        legend: {
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
            bottom: '20%',
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
                id: 'Violations',
                name: 'Violations',
                nameLocation: 'center',
                boundaryGap: false,
                nameGap: 30,
                nameRotate: 90,
                type: 'value'
            }
        ],
        color: ['#dc0000', '#fdc500', '#357abd'],
        textStyle: {
            color: textColor,
            fontFamily,
        },
        series: [
            {
                id: 'Fail',
                name: 'Fail',
                type: 'line',
                z: 5,
                emphasis: {
                    focus: 'series',
                },
            },
            {
                id: 'Warn',
                name: 'Warning',
                type: 'line',
                z: 3,
                emphasis: {
                    focus: 'series',
                },
            },
            {
                id: 'Info',
                name: 'Info',
                type: 'line',
                z: 1,
                emphasis: {
                    focus: 'series',
                },
            }
        ]
    });
    chart.resize();
    chart.on('click', 'xAxis', event => {
        if (event.targetType === 'axisLabel' && event.value) {
            window.location += parseInt(event.value, 10) + '/dependency-track-violations';
        }
    });
    window.addEventListener('resize', () => {
        chart.resize();
    });

    window.fetch(`${document.head.dataset.rooturl}/i18n/resourceBundle/?baseName=org.jenkinsci.plugins.DependencyTrack.ViolationsJobAction.floatingBox`, {
        mode: 'same-origin',
        credentials: 'same-origin',
        cache: 'default',
        headers: new Headers([
            ['Content-Type', 'application/json'],
            ['Crumb', crumbValue],
            [crumbHeaderName, crumbValue],
        ]),
    })
    .then(response => response.ok ? response.json().then(json => json.data) : {})
    .then(i18n => {
        chart.setOption({
            tooltip: {
                axisPointer: {
                    label: { formatter: i18n['tooltip.title'] }
                },
            },
            yAxis: [
                { id: 'Violations', name: i18n['yAxis.title'] },
            ],
            series: [
                { id: 'Fail', name: i18n['seriesTitle.fail'] },
                { id: 'Warn', name: i18n['seriesTitle.warn'] },
                { id: 'Info', name: i18n['seriesTitle.info'] },
            ]
        });
    });
    
    window.fetch(`${actionUrl.href}/getViolationsTrend`, {
        method: 'POST',
        mode: 'same-origin',
        credentials: 'same-origin',
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
                        id: 'Fail',
                        data: data.map(run => run.fail)
                    },
                    {
                        id: 'Warn',
                        data: data.map(run => run.warn)
                    },
                    {
                        id: 'Info',
                        data: data.map(run => run.info)
                    }
                ]
            });
        }
    });
