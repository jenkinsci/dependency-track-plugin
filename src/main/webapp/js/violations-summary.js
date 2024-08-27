import { init as echartsInit } from './libs/echarts.esm.min.js';
const currentScript = document.currentScript || document.querySelector('script[type="module"][src*="/plugin/dependency-track/js/violations-summary.js"][data-action-url]');

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

    const container = document.getElementById('dependency-track-violations-summary-chart');
    const textColor = window.getComputedStyle(container).getPropertyValue('color');
    const fontFamily = window.getComputedStyle(container).getPropertyValue('font-family');
    const chart = echartsInit(container);
    chart.setOption({
        tooltip: {
            trigger: 'item',
        },
        grid: {
            left: 20,
            bottom: 10,
            top: 10,
            containLabel: true
        },
        xAxis: {
            type: 'value',
            max: 'dataMax',
            min: 0,
            minInterval: 1,
        },
        yAxis: {
            type: 'category',
            data: ['Fail', 'Warn', 'Info'],
            inverse: true,
            nameGap: 30,
            axisLabel: {
                interval: 0,
            },
        },
        textStyle: {
            color: textColor,
            fontFamily,
        },
        series: [
            {
                id: 'Violations',
                name: 'Violations',
                type: 'bar',
                emphasis: {
                    focus: 'item',
                },
            },
        ]
    });

    window.fetch(`${document.head.dataset.rooturl}/i18n/resourceBundle/?baseName=org.jenkinsci.plugins.DependencyTrack.ViolationsRunAction.summary`, {
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
            series: [
                { id: 'Violations', name: i18n['tooltip.title'] }
            ],
            yAxis: {
                data: [i18n['seriesTitle.fail'], i18n['seriesTitle.warn'], i18n['seriesTitle.info']],
            }
        });
    });

    window.fetch(`${actionUrl.href}/getViolationsJson`, {
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
            const chartData = [
                { name: 'FAIL', value: 0, color: '#dc0000' },
                { name: 'WARN', value: 0, color: '#fdc500' },
                { name: 'INFO', value: 0, color: '#357abd' },
            ];
            data.forEach(violation => {
                chartData.find(item => item.name === violation.state).value++;
            });
            chart.setOption({
                series: [
                    {
                        id: 'Violations',
                        data: chartData.map(item => {
                            return { value: item.value, itemStyle: { color: item.color } };
                        }),
                    },
                ]
            });
        }
    });

