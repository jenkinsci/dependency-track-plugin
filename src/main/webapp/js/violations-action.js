'use strict';

// create vue.js based table
(function () {
    function getNativeFunction(clazz, func) {
        const frame = document.createElement('iframe');
        frame.style.display = 'none';
        document.body.appendChild(frame);
        const nativeClazz = frame.contentWindow[clazz];
        frame.parentNode.removeChild(frame);
        return nativeClazz.prototype[func];
    }

// make String.trim() work again like it should
// otherwise sorting the vue.js-table will not work
    if (String.prototype.trim.toString().indexOf('[native code]') === -1) {
        String.prototype.trim = getNativeFunction('String', 'trim');
    }
// restore Array.filter ... damn ancient prototype.js!
// otherwise "updateAriaDescribedby" in boostrape-vue (2.21+) will not work because 3rd arg in filter callback is undefined
    if (Array.prototype.filter.toString().indexOf('[native code]') === -1) {
        Array.prototype.filter = getNativeFunction('Array', 'filter');
    }

    const actionUrl = new URL(document.currentScript.dataset.actionUrl, window.location.origin);
    if (!(actionUrl.origin === window.location.origin
            && /^https?:$/.test(actionUrl.protocol)
            && actionUrl.pathname.startsWith(`${document.head.dataset.rooturl}/$stapler/bound/`)
    )) {
        throw new Error('malicious URL in data-action-url detected!');
    }

    const crumbHeaderName = document.head.dataset.crumbHeader || 'Jenkins-Crumb';
    const crumbValue = document.head.dataset.crumbValue || document.currentScript.dataset.crumbValue || '';
    const fetchHeaders = new Headers([
        ['Content-Type', 'application/x-stapler-method-invocation;charset=UTF-8'],
        ['Crumb', crumbValue],
        [crumbHeaderName, crumbValue],
    ]);

    /**
     * update severity-bar
     * 
     * @param {object[]} items
     */
    function updateCounter(items) {
        this.countByState.FAIL = items.filter(violation => violation.state === 'FAIL').length;
        this.countByState.WARN = items.filter(violation => violation.state === 'WARN').length;
        this.countByState.INFO = items.filter(violation => violation.state === 'INFO').length;
    }

    /**
     * Trigger pagination to update the number of buttons/pages due to filtering
     * 
     * @param {object[]} items
     */
    function updatePaging(items) {
        this.rows = items.length;
        this.currentPage = 1;
    }

    const app = new Vue({
        el: '#app',
        data: {
            rows: 0,
            perPage: 10,
            currentPage: 1,
            isBusy: false,
            sortBy: 'stateRank',
            sortDesc: false,
            filter: null,
            filterOn: [],
            fields: [
                { key: 'component.name', label: 'Name', sortable: true },
                { key: 'component.version', label: 'Version', sortable: true },
                { key: 'component.group', label: 'Group', sortable: true },
                { key: 'policyName', label: 'Policy Name', sortable: true },
                { key: 'type', label: 'Type', sortable: true },
                { key: 'stateRank', label: 'State', sortable: true },
            ],
            countByState: {
                FAIL: 0,
                WARN: 0,
                INFO: 0,
            }
        },
        methods: {
            items(ctx, callback) {
                return window.fetch(`${actionUrl.href}/getViolationsJson`, {
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
                }).then(items => {
                    updatePaging.call(this, items);
                    updateCounter.call(this, items);
                    return items;
                });
            },
            onFiltered(filteredItems) {
                updatePaging.call(this, filteredItems);
                updateCounter.call(this, filteredItems);
            },
            matchesFilter(item, term) {
                const keys = this.filterOn.length ? this.filterOn : this.fields.map(field => field.key);
                const searchValues = [];
                const accessPaths = { };
                keys.filter(key => key.includes('.')).forEach(key => {
                    const parts = key.split('.');
                    if (accessPaths[parts[0]] === undefined) {
                        accessPaths[parts[0]] = [parts[1]];
                    } else {
                        accessPaths[parts[0]].push(parts[1]);
                    }
                });
                keys.filter(key => !key.includes('.')).forEach(key => {
                   accessPaths[key] = key;
                });
                Object.entries(accessPaths).forEach(p1 => {
                    if (Array.isArray(p1[1])) {
                        searchValues.push(...p1[1].map(p2 => item[p1[0]][p2].toLowerCase()).filter(value => value.length));
                    } else {
                        if (p1[0] === 'stateRank' && item['state']) {
                            searchValues.push(item['state'].toLowerCase());
                        } else if (item[p1[0]]) {
                            searchValues.push(item[p1[0]].toLowerCase());
                        }
                    }
                });
                return searchValues.some(value => value.includes(term.trim().toLowerCase()));
            }
        },
    });

    window.fetch(`${document.head.dataset.rooturl}/i18n/resourceBundle/?baseName=org.jenkinsci.plugins.DependencyTrack.ViolationsRunAction.index`, {
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
        app.fields.find(field => field.key === 'component.name').label = i18n['filter.value.name'];
        app.fields.find(field => field.key === 'component.version').label = i18n['filter.value.version'];
        app.fields.find(field => field.key === 'component.group').label = i18n['filter.value.group'];
        app.fields.find(field => field.key === 'policyName').label = i18n['filter.value.policyName'];
        app.fields.find(field => field.key === 'type').label = i18n['filter.value.type'];
        app.fields.find(field => field.key === 'stateRank').label = i18n['filter.value.state'];
    });
})();
