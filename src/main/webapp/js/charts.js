import { init as echartsInit } from './libs/echarts.esm.min.js';
const currentScript = document.currentScript || document.querySelector('script[type="module"][src$="/plugin/dependency-track/js/charts.js"][data-action-url]');

const actionUrl = new URL(currentScript.dataset.actionUrl, window.location.origin);
if (!(actionUrl.origin === window.location.origin
        && /^https?:$/.test(actionUrl.protocol)
        && actionUrl.pathname.startsWith(`${document.head.dataset.rooturl}/$stapler/bound/`)
        )) {
    throw new Error('malicious URL in data-action-url detected!');
}

const crumbHeaderName = document.head.dataset.crumbHeader || 'Jenkins-Crumb';
const crumbValue = document.head.dataset.crumbValue || currentScript.dataset.crumbValue || '';

const initChart = data => {
    const container = document.getElementById('dtrackTrend-history-chart');
    const textColor = window.getComputedStyle(container).getPropertyValue('color');
    const fontFamily = window.getComputedStyle(container).getPropertyValue('font-family');
    const chart = echartsInit(container, null, {
        width: 500,
        height: 210,
    });
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
            data: data.map(run => run.buildNumber),
        },
        yAxis: [
            {
                id: 'Vulnerabilities',
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
                id: 'Critical',
                name: 'Critical',
                type: 'line',
                z: 5,
                emphasis: {
                    focus: 'series',
                },
                data: data.map(run => run.critical),
            },
            {
                id: 'High',
                name: 'High',
                type: 'line',
                z: 4,
                emphasis: {
                    focus: 'series',
                },
                data: data.map(run => run.high),
            },
            {
                id: 'Medium',
                name: 'Medium',
                type: 'line',
                z: 3,
                emphasis: {
                    focus: 'series',
                },
                data: data.map(run => run.medium),
            },
            {
                id: 'Low',
                name: 'Low',
                type: 'line',
                z: 2,
                emphasis: {
                    focus: 'series',
                },
                data: data.map(run => run.low),
            },
            {
                id: 'Info',
                name: 'Info',
                type: 'line',
                z: 1,
                emphasis: {
                    focus: 'series',
                },
                data: data.map(run => run.info),
            },
            {
                id: 'Unassigned',
                name: 'Unassigned',
                type: 'line',
                z: 0,
                emphasis: {
                    focus: 'series',
                },
                data: data.map(run => run.unassigned),
            }
        ]
    });
    chart.on('click', 'xAxis', event => {
        if (event.targetType === 'axisLabel' && event.value) {
            window.location += parseInt(event.value, 10) + '/dependency-track-findings';
        }
    });

    window.fetch(`${document.head.dataset.rooturl}/i18n/resourceBundle/?baseName=org.jenkinsci.plugins.DependencyTrack.JobAction.floatingBox`, {
        mode: 'same-origin',
        credentials: 'same-origin',
        cache: 'default',
        headers: new Headers([
            ['Content-Type', 'application/json'],
            ['Crumb', crumbValue],
            [crumbHeaderName, crumbValue],
        ]),
    })
    .then(response => response.ok ? response.json().then(json => json.data) : { })
    .then(i18n => {
        chart.setOption({
            tooltip: {
                axisPointer: {
                    label: { formatter: i18n['tooltip.title'] }
                },
            },
            series: [
                { id: 'Critical', name: i18n['seriesTitle.critical'] },
                { id: 'High', name: i18n['seriesTitle.high'] },
                { id: 'Medium', name: i18n['seriesTitle.medium'] },
                { id: 'Low', name: i18n['seriesTitle.low'] },
                { id: 'Info', name: i18n['seriesTitle.info'] },
                { id: 'Unassigned', name: i18n['seriesTitle.unassigned'] },
            ]
        });
    });
};

window.fetch(`${actionUrl.href}/getSeverityDistributionTrend`, {
    method: 'POST',
    mode: 'same-origin',
    credentials: 'same-origin',
    cache: 'default',
    body: '[]',
    headers: new Headers([
        ['Content-Type', 'application/x-stapler-method-invocation;charset=UTF-8'],
        ['Crumb', crumbValue],
        [crumbHeaderName, crumbValue],
    ]),
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
        initChart(data);
    }
});
