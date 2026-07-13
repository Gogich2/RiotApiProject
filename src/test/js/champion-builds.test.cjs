const test = require('node:test');
const assert = require('node:assert/strict');

const championBuilds = require('../../main/resources/static/js/champion-builds.js');

class FakeElement {
    constructor() {
        this.listeners = new Map();
        this.dataset = {};
        this.hidden = false;
        this.innerHTML = '';
        this.textContent = '';
        this.value = '';
        this.attributes = new Map();
    }

    addEventListener(type, listener) {
        this.listeners.set(type, listener);
    }

    setAttribute(name, value) {
        this.attributes.set(name, String(value));
    }

    removeAttribute(name) {
        this.attributes.delete(name);
    }

    async dispatch(type, init = {}) {
        const event = {
            preventDefault() {},
            target: this,
            key: init.key,
            ...init
        };
        return this.listeners.get(type)?.(event);
    }
}

function harness({
    search = '?id=11',
    options,
    builds,
    cache = {},
    throwOnStorageGet = false,
    throwOnStorageSet = false
} = {}) {
    const ids = [
        'buildQueueTabs', 'buildPatchSelect', 'buildRoleTabs', 'buildOpponentSearch',
        'buildOpponentRail', 'buildOpponentEmpty', 'buildRetryButton', 'buildStatus',
        'buildLoadingState', 'buildFallbackState', 'buildUnavailableState',
        'buildRequestError', 'buildEvidence', 'buildContent', 'buildStartingItems',
        'buildBootsCore', 'buildSituationalItems', 'buildRunes', 'buildSpells',
        'buildSkillOrder', 'buildMaxPriority'
    ];
    const elements = Object.fromEntries(ids.map(id => [id, new FakeElement()]));
    const root = {
        querySelector(selector) {
            return elements[selector.replace(/^#/, '')] || null;
        }
    };
    const optionCalls = [];
    const buildCalls = [];
    const apiClient = {
        async getChampionBuildOptions(championId, filters) {
            optionCalls.push({ championId, filters: { ...filters } });
            return typeof options === 'function' ? options(optionCalls.length, filters) : (options || optionResponse());
        },
        async getChampionBuild(championId, filters) {
            buildCalls.push({ championId, filters: { ...filters } });
            return typeof builds === 'function' ? builds(buildCalls.length, filters) : (builds || buildResponse());
        }
    };
    const values = new Map(Object.entries(cache));
    const storage = {
        reads: [],
        writes: [],
        getItem(key) {
            this.reads.push(key);
            if (throwOnStorageGet) throw new Error('storage get blocked');
            return values.get(key) ?? null;
        },
        setItem(key, value) {
            this.writes.push(key);
            if (throwOnStorageSet) throw new Error('storage set blocked');
            values.set(key, value);
        }
    };
    const history = {
        calls: [],
        replaceState(...args) {
            this.calls.push(args);
        }
    };
    const location = { pathname: '/champion.html', search };
    const renderer = {
        rendered: [],
        unavailable: [],
        errors: [],
        render(response) {
            this.rendered.push(response);
        },
        renderUnavailable(response) {
            this.unavailable.push(response);
        },
        renderRequestError(error, context) {
            this.errors.push({ error, context });
        }
    };

    return { root, elements, apiClient, optionCalls, buildCalls, storage, history, location, renderer };
}

function deferred() {
    let resolve;
    let reject;
    const promise = new Promise((resolvePromise, rejectPromise) => {
        resolve = resolvePromise;
        reject = rejectPromise;
    });
    return { promise, resolve, reject };
}

function optionResponse() {
    return {
        championId: 11,
        queues: [
            { queueId: 420, label: 'Solo/Duo', available: true },
            { queueId: 440, label: 'Flex', available: false }
        ],
        patches: [{ patch: '16.9' }, { patch: '16.8' }],
        roles: [
            { role: 'MIDDLE', games: 30, available: true },
            { role: 'TOP', games: 12, available: true }
        ],
        opponents: [
            { championId: 2, label: 'Zed', imageUrl: '/zed.png', games: 14 },
            { championId: 3, label: 'Ahri', imageUrl: '/ahri.png', games: 11 }
        ],
        defaults: { queueId: 420, patch: '16.9', role: 'MIDDLE', opponentId: null }
    };
}

function buildResponse(overrides = {}) {
    return {
        available: true,
        requested: { queueId: 420, patch: '16.9', role: 'MIDDLE', opponentId: null },
        resolved: { queueId: 420, anchorPatch: '16.9', comparisonPatch: '16.8', role: 'MIDDLE', opponentId: null },
        resultScope: 'CHAMPION_ROLE',
        confidence: 'MEDIUM',
        games: 30,
        wins: 16,
        winRate: 53.33,
        stale: false,
        historical: false,
        fallbackReason: null,
        evidenceLabel: '30 observed games',
        explanation: 'Observed champion-role build.',
        build: {
            startingItems: [], boots: [], coreItems: [], situationalItems: [],
            runePages: [], spellPairs: [], skillOrders: [], skillMaxPriority: []
        },
        ...overrides
    };
}

async function mount(h) {
    return championBuilds.mount({
        championId: 11,
        root: h.root,
        apiClient: h.apiClient,
        sessionStorage: h.storage,
        history: h.history,
        location: h.location,
        renderer: h.renderer
    });
}

async function mountWithDefaultRenderer(h) {
    return championBuilds.mount({
        championId: 11,
        root: h.root,
        apiClient: h.apiClient,
        sessionStorage: h.storage,
        history: h.history,
        location: h.location
    });
}

test('restores queue, patch, role, and opponent from the URL', async () => {
    const h = harness({ search: '?id=11&queue=440&patch=16.8&role=TOP&opponent=2' });

    await mount(h);

    assert.deepEqual(h.optionCalls[0].filters, { queueId: 440, patch: '16.8', role: 'TOP' });
    assert.deepEqual(h.buildCalls[0].filters, { queueId: 440, patch: '16.8', role: 'TOP', opponentId: 2 });
});

test('filter changes synchronize the URL and refetch options for queue, patch, and role', async () => {
    const h = harness();
    await mount(h);

    await h.elements.buildQueueTabs.dispatch('click', { target: { dataset: { queueId: '440' } } });
    h.elements.buildPatchSelect.value = '16.8';
    await h.elements.buildPatchSelect.dispatch('change');
    await h.elements.buildRoleTabs.dispatch('click', { target: { dataset: { role: 'TOP' } } });

    assert.equal(h.optionCalls.length, 4);
    assert.deepEqual(h.optionCalls.at(-1).filters, { queueId: 440, patch: '16.8', role: 'TOP' });
    const url = h.history.calls.at(-1)[2];
    assert.match(url, /queue=440/);
    assert.match(url, /patch=16\.8/);
    assert.match(url, /role=TOP/);
    assert.doesNotMatch(url, /opponent=/);
});

test('cache keys include champion, queue, patch, role, and opponent', () => {
    const base = { queueId: 420, patch: '16.9', role: 'MIDDLE', opponentId: 2 };
    const key = championBuilds.cacheKey(11, base);

    assert.match(key, /11/);
    assert.match(key, /420/);
    assert.match(key, /16\.9/);
    assert.match(key, /MIDDLE/);
    assert.match(key, /2/);
    assert.notEqual(key, championBuilds.cacheKey(12, base));
    assert.notEqual(key, championBuilds.cacheKey(11, { ...base, queueId: 440 }));
    assert.notEqual(key, championBuilds.cacheKey(11, { ...base, patch: '16.8' }));
    assert.notEqual(key, championBuilds.cacheKey(11, { ...base, role: 'TOP' }));
    assert.notEqual(key, championBuilds.cacheKey(11, { ...base, opponentId: 3 }));
});

test('restores only the exact-key cached response and marks it stale', async () => {
    const filters = { queueId: 420, patch: '16.9', role: 'MIDDLE', opponentId: null };
    const exactKey = championBuilds.cacheKey(11, filters);
    const otherKey = championBuilds.cacheKey(11, { ...filters, role: 'TOP' });
    const cached = buildResponse();
    const h = harness({
        cache: { [exactKey]: JSON.stringify(cached), [otherKey]: JSON.stringify(buildResponse({ games: 999 })) },
        builds: async () => { throw new Error('offline'); }
    });

    await mount(h);

    assert.deepEqual(h.storage.reads, [exactKey]);
    assert.equal(h.renderer.rendered.length, 1);
    assert.equal(h.renderer.rendered[0].games, cached.games);
    assert.equal(h.renderer.rendered[0].stale, true);
    assert.equal(h.renderer.errors.length, 1);
});

test('retry repeats the failed request and renders the successful response', async () => {
    const response = buildResponse({ games: 44 });
    const h = harness({
        builds: async call => {
            if (call === 1) throw new Error('temporary');
            return response;
        }
    });
    await mount(h);

    await h.elements.buildRetryButton.dispatch('click');

    assert.equal(h.buildCalls.length, 2);
    assert.deepEqual(h.renderer.rendered, [response]);
});

test('a failed replacement request preserves the currently rendered response', async () => {
    const current = buildResponse();
    const h = harness({
        builds: async call => {
            if (call === 1) return current;
            throw new Error('replacement failed');
        }
    });
    await mount(h);

    await h.elements.buildRoleTabs.dispatch('click', { target: { dataset: { role: 'TOP' } } });

    assert.deepEqual(h.renderer.rendered, [current]);
    assert.equal(h.renderer.errors.length, 1);
    assert.equal(h.renderer.errors[0].context.retained, true);
});

test('a retained request error uses one error message and preserves meaningful fallback context', async () => {
    const fallback = buildResponse({
        fallbackReason: 'MATCHUP_SAMPLE_TOO_SMALL',
        explanation: 'Exact matchup sample is too small. Showing the champion-role baseline.'
    });
    const h = harness({
        builds: async call => {
            if (call === 1) return fallback;
            throw new Error('replacement failed');
        }
    });
    await mountWithDefaultRenderer(h);
    const fallbackMessage = h.elements.buildFallbackState.textContent;

    await h.elements.buildRoleTabs.dispatch('click', { target: { dataset: { role: 'TOP' } } });

    assert.equal(h.elements.buildFallbackState.textContent, fallbackMessage);
    assert.equal(h.elements.buildFallbackState.hidden, false);
    assert.match(h.elements.buildRequestError.textContent, /Could not refresh build data/);
    assert.notEqual(h.elements.buildRequestError.textContent, fallbackMessage);
});

test('a filter change still updates the URL when its option refresh fails', async () => {
    const h = harness({
        options: async call => {
            if (call === 1) return optionResponse();
            throw new Error('options failed');
        }
    });
    await mount(h);

    await h.elements.buildRoleTabs.dispatch('click', { target: { dataset: { role: 'TOP' } } });

    assert.match(h.history.calls.at(-1)[2], /role=TOP/);
    assert.equal(h.renderer.errors.length, 1);
});

test('an older option response cannot replace or request after a newer filter response', async () => {
    const older = deferred();
    const newer = deferred();
    const h = harness({
        options: call => {
            if (call === 1) return optionResponse();
            return call === 2 ? older.promise : newer.promise;
        }
    });
    await mount(h);

    const olderChange = h.elements.buildQueueTabs.dispatch('click', {
        target: { dataset: { queueId: '440' } }
    });
    const newerChange = h.elements.buildRoleTabs.dispatch('click', {
        target: { dataset: { role: 'TOP' } }
    });
    newer.resolve({
        ...optionResponse(),
        opponents: [{ championId: 8, label: 'Latest opponent', imageUrl: '', games: 4 }]
    });
    await newerChange;
    older.resolve({
        ...optionResponse(),
        opponents: [{ championId: 9, label: 'Stale opponent', imageUrl: '', games: 4 }]
    });
    await olderChange;

    assert.equal(h.buildCalls.length, 2);
    assert.deepEqual(h.buildCalls.at(-1).filters, {
        queueId: 440, patch: '16.9', role: 'TOP', opponentId: null
    });
    assert.match(h.elements.buildOpponentRail.innerHTML, /Latest opponent/);
    assert.doesNotMatch(h.elements.buildOpponentRail.innerHTML, /Stale opponent/);
});

test('an older build response cannot render or cache under newer filters', async () => {
    const older = deferred();
    const newer = deferred();
    const h = harness({
        builds: call => {
            if (call === 1) return buildResponse();
            return call === 2 ? older.promise : newer.promise;
        }
    });
    await mount(h);

    const olderRequest = h.elements.buildOpponentRail.dispatch('click', {
        target: { dataset: { opponentId: '2' } }
    });
    const newerRequest = h.elements.buildOpponentRail.dispatch('click', {
        target: { dataset: { opponentId: '3' } }
    });
    const latestResponse = buildResponse({ games: 33 });
    newer.resolve(latestResponse);
    await newerRequest;
    older.resolve(buildResponse({ games: 22 }));
    await olderRequest;

    assert.deepEqual(h.renderer.rendered, [h.renderer.rendered[0], latestResponse]);
    assert.equal(h.renderer.rendered.length, 2);
    assert.match(h.storage.writes.at(-1), /opponent=3$/);
    assert.equal(h.storage.writes.some(key => /opponent=2$/.test(key)), false);
});

test('a current options error clears an invalidated old build loader without erasing the current response', async () => {
    const oldBuild = deferred();
    const h = harness({
        options: async call => {
            if (call === 1) return optionResponse();
            throw new Error('options failed');
        },
        builds: call => call === 1
            ? buildResponse({ evidenceLabel: 'Current response' })
            : oldBuild.promise
    });
    await mountWithDefaultRenderer(h);

    const oldRequest = h.elements.buildOpponentRail.dispatch('click', {
        target: { dataset: { opponentId: '2' } }
    });
    const filterChange = h.elements.buildRoleTabs.dispatch('click', {
        target: { dataset: { role: 'TOP' } }
    });
    await filterChange;

    assert.equal(h.elements.buildLoadingState.hidden, true);
    assert.equal(h.elements.buildRequestError.hidden, false);
    assert.match(h.elements.buildEvidence.innerHTML, /Current response/);

    oldBuild.resolve(buildResponse({ evidenceLabel: 'Stale response' }));
    await oldRequest;
    assert.equal(h.elements.buildLoadingState.hidden, true);
    assert.match(h.elements.buildEvidence.innerHTML, /Current response/);
    assert.doesNotMatch(h.elements.buildEvidence.innerHTML, /Stale response/);
});

test('an option request error restores an exact URL cache only when nothing is rendered', async () => {
    const filters = { queueId: 420, patch: '16.9', role: 'MIDDLE', opponentId: 2 };
    const exactKey = championBuilds.cacheKey(11, filters);
    const h = harness({
        search: '?id=11&queue=420&patch=16.9&role=MIDDLE&opponent=2',
        cache: { [exactKey]: JSON.stringify(buildResponse({ games: 21 })) },
        options: async () => { throw new Error('options failed'); }
    });

    await mount(h);

    assert.deepEqual(h.storage.reads, [exactKey]);
    assert.equal(h.renderer.rendered[0].games, 21);
    assert.equal(h.renderer.rendered[0].stale, true);
    assert.equal(h.renderer.errors[0].context.retained, true);
});

test('Arrow keys and Enter choose an opponent from backend order after label filtering', async () => {
    const h = harness();
    await mount(h);
    h.elements.buildOpponentSearch.value = 'a';
    await h.elements.buildOpponentSearch.dispatch('input');

    await h.elements.buildOpponentSearch.dispatch('keydown', { key: 'ArrowDown' });
    await h.elements.buildOpponentSearch.dispatch('keydown', { key: 'ArrowDown' });
    await h.elements.buildOpponentSearch.dispatch('keydown', { key: 'Enter' });

    assert.equal(h.buildCalls.length, 2);
    assert.equal(h.buildCalls[1].filters.opponentId, 3);
    assert.match(h.history.calls.at(-1)[2], /opponent=3/);
});

test('All opponents is first, selected for baseline, and keyboard-clears an opponent', async () => {
    const h = harness({ search: '?id=11&queue=420&patch=16.9&role=MIDDLE&opponent=2' });
    await mount(h);

    assert.match(h.elements.buildOpponentRail.innerHTML, /^\s*<button[^>]*data-baseline-opponent/);
    assert.match(h.elements.buildOpponentRail.innerHTML, /All opponents/);
    assert.match(h.elements.buildOpponentRail.innerHTML, /data-baseline-opponent[^>]*aria-selected="false"/s);
    assert.ok(h.elements.buildOpponentRail.innerHTML.indexOf('Zed')
        < h.elements.buildOpponentRail.innerHTML.indexOf('Ahri'));

    await h.elements.buildOpponentSearch.dispatch('keydown', { key: 'ArrowDown' });
    await h.elements.buildOpponentSearch.dispatch('keydown', { key: 'Enter' });

    assert.equal(h.buildCalls.at(-1).filters.opponentId, null);
    assert.doesNotMatch(h.history.calls.at(-1)[2], /opponent=/);
    assert.match(h.elements.buildOpponentRail.innerHTML, /data-baseline-opponent[^>]*aria-selected="true"/s);
});

test('fallback reason NONE does not show the fallback state', async () => {
    const h = harness({ builds: buildResponse({ fallbackReason: 'NONE' }) });

    await mountWithDefaultRenderer(h);

    assert.equal(h.elements.buildFallbackState.hidden, true);
});

test('max priority stays in backend order and visible option copy uses plain separators', async () => {
    const response = buildResponse({
        build: {
            startingItems: [{
                assets: [{ label: 'Doran Ring', imageUrl: '' }],
                pickRate: 40,
                winRate: 52,
                games: 20
            }],
            boots: [], coreItems: [], situationalItems: [], runePages: [],
            spellPairs: [], skillOrders: [], skillMaxPriority: [1, 3, 2]
        }
    });
    const h = harness({ builds: response });

    await mountWithDefaultRenderer(h);

    assert.match(h.elements.buildMaxPriority.innerHTML, /Q.*→.*E.*→.*W/s);
    assert.match(h.elements.buildQueueTabs.innerHTML, /Flex \(unavailable\)/);
    assert.doesNotMatch(h.elements.buildQueueTabs.innerHTML, /[—–]/);
    assert.match(h.elements.buildStartingItems.innerHTML, /40% pick, 52% win, 20 games/);
    assert.doesNotMatch(h.elements.buildStartingItems.innerHTML, /[·—–]/);
});

test('a throwing sessionStorage setItem cannot suppress a live response', async () => {
    const response = buildResponse({ games: 51 });
    const h = harness({ builds: response, throwOnStorageSet: true });

    await mount(h);

    assert.deepEqual(h.renderer.rendered, [response]);
    assert.equal(h.renderer.errors.length, 0);
});

test('throwing sessionStorage getItem stays inside error handling and retry still works', async () => {
    const response = buildResponse({ games: 52 });
    const h = harness({
        throwOnStorageGet: true,
        throwOnStorageSet: true,
        builds: async call => {
            if (call === 1) throw new Error('offline');
            return response;
        }
    });

    await assert.doesNotReject(mount(h));
    assert.equal(h.renderer.errors.length, 1);
    await assert.doesNotReject(h.elements.buildRetryButton.dispatch('click'));
    assert.deepEqual(h.renderer.rendered, [response]);
});

test('opponent search exposes its empty result without requesting new data', async () => {
    const h = harness();
    await mount(h);
    h.elements.buildOpponentSearch.value = 'not returned by backend';

    await h.elements.buildOpponentSearch.dispatch('input');

    assert.equal(h.elements.buildOpponentEmpty.hidden, false);
    assert.equal(h.elements.buildOpponentSearch.attributes.get('aria-expanded'), 'true');
    assert.equal(h.buildCalls.length, 1);
});
