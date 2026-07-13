(function exposeChampionBuilds(scope, factory) {
    const instance = factory();

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = instance;
    }

    scope.championBuilds = instance;
})(typeof globalThis === 'undefined' ? window : globalThis, function createChampionBuilds() {
    const championBuilds = {
        async mount({
            championId,
            root,
            apiClient,
            sessionStorage,
            history,
            location,
            renderer = championBuilds
        }) {
            this.state = {
                championId,
                root,
                apiClient,
                sessionStorage,
                history,
                location,
                renderer,
                filters: readUrlFilters(location.search),
                options: null,
                filteredOpponents: [],
                activeOpponentIndex: -1,
                currentResponse: null,
                lastFailedAction: null,
                optionsRequestGeneration: 0,
                buildRequestGeneration: 0
            };

            bindEvents(this.state);

            if (!await fetchOptions(this.state)) {
                return;
            }

            applyDefaults(this.state);
            renderOptions(this.state);
            this.syncUrl(this.state.filters);
            await fetchBuild(this.state);
        },

        render(response) {
            const state = this.state;
            const build = response.build || {};

            setHidden(state, 'buildContent', false);
            setHidden(state, 'buildInsufficientState', true);
            setHidden(state, 'buildUnavailableState', true);
            setHidden(state, 'buildRequestError', true);
            renderEvidence(state, response);
            setHtml(state, 'buildStartingItems', renderChoices(build.startingItems));
            setHtml(state, 'buildBootsCore', `
                <div><h4>Boots</h4>${renderChoices(build.boots)}</div>
                <div><h4>Core path</h4>${renderChoices(build.coreItems)}</div>
            `);
            setHtml(state, 'buildSituationalItems', renderChoices(build.situationalItems));
            setHtml(state, 'buildRunes', renderChoices(build.runePages));
            setHtml(state, 'buildSpells', renderChoices(build.spellPairs));
            setHtml(state, 'buildSkillOrder', renderChoices(build.skillOrders));
            setHtml(state, 'buildMaxPriority', renderSkillPriority(build.skillMaxPriority));
            renderFallbackState(state, response);
        },

        renderUnavailable(response) {
            const state = this.state;
            setHidden(state, 'buildContent', true);
            setHidden(state, 'buildFallbackState', true);
            setHidden(state, 'buildRequestError', true);
            const stateId = response.confidence === 'INSUFFICIENT'
                ? 'buildInsufficientState'
                : 'buildUnavailableState';
            const otherStateId = stateId === 'buildInsufficientState'
                ? 'buildUnavailableState'
                : 'buildInsufficientState';
            setHidden(state, otherStateId, true);
            setText(state, stateId, response.explanation || 'Build data is not available for these filters.');
            setHidden(state, stateId, false);
        },

        renderRequestError(error, context = {}) {
            const state = this.state;
            const message = context.retained
                ? 'Could not refresh build data. The previous result remains visible and may be stale.'
                : 'Could not load build data. Retry the request.';
            const requestError = element(state, 'buildRequestError');
            requestError?.setAttribute('role', context.retained ? 'status' : 'alert');
            requestError?.setAttribute('aria-live', context.retained ? 'polite' : 'assertive');

            setText(state, 'buildRequestError', message, true);
            setHidden(state, 'buildRequestError', false);
        },

        syncUrl(filters) {
            const state = this.state;
            const params = new URLSearchParams(state.location.search);
            params.set('queue', String(filters.queueId));
            params.set('patch', filters.patch);
            params.set('role', filters.role);
            if (filters.opponentId === null || filters.opponentId === undefined) {
                params.delete('opponent');
            } else {
                params.set('opponent', String(filters.opponentId));
            }
            state.history.replaceState(null, '', `${state.location.pathname}?${params.toString()}`);
        },

        cacheKey(championId, filters) {
            const opponentId = filters.opponentId === null || filters.opponentId === undefined
                ? 'none'
                : filters.opponentId;
            return `champion-build:${championId}:queue=${filters.queueId}:patch=${filters.patch}:role=${filters.role}:opponent=${opponentId}`;
        }
    };

    function bindEvents(state) {
        element(state, 'buildQueueTabs')?.addEventListener('click', async event => {
            const button = event.target.closest?.('[data-queue-id]')
                || (event.target.dataset?.queueId ? event.target : null);
            if (button) {
                await changePrimaryFilter(state, 'queueId', Number(button.dataset.queueId));
            }
        });

        element(state, 'buildPatchSelect')?.addEventListener('change', async event => {
            await changePrimaryFilter(state, 'patch', event.target.value);
        });

        element(state, 'buildRoleTabs')?.addEventListener('click', async event => {
            const button = event.target.closest?.('[data-role]')
                || (event.target.dataset?.role ? event.target : null);
            if (button) {
                await changePrimaryFilter(state, 'role', button.dataset.role);
            }
        });

        element(state, 'buildOpponentRail')?.addEventListener('click', async event => {
            const baselineButton = event.target.closest?.('[data-baseline-opponent]')
                || (event.target.dataset && 'baselineOpponent' in event.target.dataset ? event.target : null);
            if (baselineButton) {
                await selectOpponent(state, null);
                return;
            }
            const button = event.target.closest?.('[data-opponent-id]')
                || (event.target.dataset?.opponentId ? event.target : null);
            if (button) {
                await selectOpponent(state, Number(button.dataset.opponentId));
            }
        });

        element(state, 'buildOpponentSearch')?.addEventListener('input', event => {
            filterOpponents(state, event.target.value);
        });

        element(state, 'buildOpponentSearch')?.addEventListener('keydown', async event => {
            const choiceCount = state.filteredOpponents.length + 1;

            if (event.key === 'ArrowDown') {
                event.preventDefault();
                state.activeOpponentIndex = state.activeOpponentIndex + 1;
                if (state.activeOpponentIndex >= choiceCount) {
                    state.activeOpponentIndex = 0;
                }
                renderOpponentRail(state);
            } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                state.activeOpponentIndex = state.activeOpponentIndex - 1;
                if (state.activeOpponentIndex < 0) {
                    state.activeOpponentIndex = choiceCount - 1;
                }
                renderOpponentRail(state);
            } else if (event.key === 'Enter' && state.activeOpponentIndex >= 0) {
                event.preventDefault();
                const opponentId = state.activeOpponentIndex === 0
                    ? null
                    : state.filteredOpponents[state.activeOpponentIndex - 1].championId;
                await selectOpponent(state, opponentId);
            }
        });

        element(state, 'buildRetryButton')?.addEventListener('click', async () => {
            if (state.lastFailedAction === 'options') {
                if (await fetchOptions(state)) {
                    applyDefaults(state);
                    renderOptions(state);
                    championBuilds.syncUrl(state.filters);
                    await fetchBuild(state);
                }
                return;
            }
            await fetchBuild(state);
        });
    }

    async function changePrimaryFilter(state, name, value) {
        state.filters[name] = value;
        state.filters.opponentId = null;
        state.activeOpponentIndex = -1;
        state.buildRequestGeneration += 1;
        championBuilds.syncUrl(state.filters);
        const search = element(state, 'buildOpponentSearch');
        if (search) {
            search.value = '';
        }

        if (!await fetchOptions(state)) {
            return;
        }

        renderOptions(state);
        await fetchBuild(state);
    }

    async function selectOpponent(state, opponentId) {
        state.filters.opponentId = opponentId;
        championBuilds.syncUrl(state.filters);
        renderOpponentRail(state);
        await fetchBuild(state);
    }

    async function fetchOptions(state) {
        const requestGeneration = ++state.optionsRequestGeneration;
        const buildGeneration = state.buildRequestGeneration;
        const requestFilters = { ...state.filters };
        try {
            const options = await state.apiClient.getChampionBuildOptions(
                state.championId,
                optionFilters(requestFilters)
            );
            if (requestGeneration !== state.optionsRequestGeneration) {
                return false;
            }
            state.options = options;
            state.lastFailedAction = null;
            setHidden(state, 'buildRequestError', true);
            return true;
        } catch (error) {
            if (requestGeneration !== state.optionsRequestGeneration) {
                return false;
            }
            if (buildGeneration === state.buildRequestGeneration) {
                setHidden(state, 'buildLoadingState', true);
            }
            state.lastFailedAction = 'options';
            if (state.currentResponse === null && hasCompleteFilters(requestFilters)) {
                const restored = restoreCachedResponse(state, requestFilters);
                if (restored) {
                    state.currentResponse = restored;
                    showResponse(state, restored);
                }
            }
            state.renderer.renderRequestError(error, {
                retained: state.currentResponse !== null,
                currentResponse: state.currentResponse
            });
            return false;
        }
    }

    async function fetchBuild(state) {
        const requestGeneration = ++state.buildRequestGeneration;
        const requestFilters = { ...state.filters };
        setHidden(state, 'buildLoadingState', false);
        try {
            const response = await state.apiClient.getChampionBuild(state.championId, requestFilters);
            if (requestGeneration !== state.buildRequestGeneration) {
                return;
            }
            state.currentResponse = response;
            state.lastFailedAction = null;
            storeCachedResponse(state, requestFilters, response);
            showResponse(state, response);
        } catch (error) {
            if (requestGeneration !== state.buildRequestGeneration) {
                return;
            }
            state.lastFailedAction = 'build';
            let restored = null;

            if (state.currentResponse === null) {
                restored = restoreCachedResponse(state, requestFilters);
                if (restored) {
                    state.currentResponse = restored;
                    showResponse(state, restored);
                }
            }

            state.renderer.renderRequestError(error, {
                retained: state.currentResponse !== null,
                cached: restored !== null,
                currentResponse: state.currentResponse
            });
        } finally {
            if (requestGeneration === state.buildRequestGeneration) {
                setHidden(state, 'buildLoadingState', true);
            }
        }
    }

    function restoreCachedResponse(state, filters) {
        try {
            const key = championBuilds.cacheKey(state.championId, filters);
            const cached = state.sessionStorage.getItem(key);
            if (!cached) return null;
            return { ...JSON.parse(cached), stale: true };
        } catch (error) {
            return null;
        }
    }

    function storeCachedResponse(state, filters, response) {
        try {
            state.sessionStorage.setItem(
                championBuilds.cacheKey(state.championId, filters),
                JSON.stringify(response)
            );
        } catch (error) {
            // Public build rendering must not depend on storage availability.
        }
    }

    function showResponse(state, response) {
        if (response.available) {
            state.renderer.render(response);
        } else {
            state.renderer.renderUnavailable(response);
        }
    }

    function hasCompleteFilters(filters) {
        return filters.queueId !== undefined && filters.patch !== undefined && filters.role !== undefined;
    }

    function readUrlFilters(search) {
        const params = new URLSearchParams(search);
        const filters = {};
        if (params.has('queue')) filters.queueId = Number(params.get('queue'));
        if (params.has('patch')) filters.patch = params.get('patch');
        if (params.has('role')) filters.role = params.get('role');
        if (params.has('opponent')) filters.opponentId = Number(params.get('opponent'));
        return filters;
    }

    function applyDefaults(state) {
        const defaults = state.options.defaults || {};
        state.filters = {
            queueId: state.filters.queueId ?? defaults.queueId,
            patch: state.filters.patch ?? defaults.patch,
            role: state.filters.role ?? defaults.role,
            opponentId: state.filters.opponentId ?? defaults.opponentId ?? null
        };
    }

    function optionFilters(filters) {
        const result = {};
        if (filters.queueId !== undefined) result.queueId = filters.queueId;
        if (filters.patch !== undefined) result.patch = filters.patch;
        if (filters.role !== undefined) result.role = filters.role;
        return result;
    }

    function renderOptions(state) {
        setHtml(state, 'buildQueueTabs', (state.options.queues || []).map(queue => `
            <button type="button" data-queue-id="${escapeHtml(queue.queueId)}"
                    aria-pressed="${queue.queueId === state.filters.queueId}">
                ${escapeHtml(queue.label)}${queue.available ? '' : ' (unavailable)'}
            </button>
        `).join(''));

        setHtml(state, 'buildPatchSelect', (state.options.patches || []).map(option => `
            <option value="${escapeHtml(option.patch)}"${option.patch === state.filters.patch ? ' selected' : ''}>
                ${escapeHtml(option.patch)}
            </option>
        `).join(''));

        setHtml(state, 'buildRoleTabs', (state.options.roles || []).map(option => `
            <button type="button" data-role="${escapeHtml(option.role)}"
                    aria-pressed="${option.role === state.filters.role}">
                ${escapeHtml(roleLabel(option.role))}${option.available ? '' : ' (unavailable)'}
            </button>
        `).join(''));

        state.filteredOpponents = [...(state.options.opponents || [])];
        state.activeOpponentIndex = -1;
        renderOpponentRail(state);
    }

    function filterOpponents(state, query) {
        const normalized = String(query || '').trim().toLocaleLowerCase();
        state.filteredOpponents = (state.options.opponents || []).filter(opponent =>
            opponent.label.toLocaleLowerCase().includes(normalized)
        );
        state.activeOpponentIndex = -1;
        renderOpponentRail(state);
    }

    function renderOpponentRail(state) {
        const baseline = `
            <button type="button" role="option" id="buildOpponent-all"
                    data-baseline-opponent aria-selected="${state.filters.opponentId === null}">
                <span>All opponents</span>
                <small>Champion-role baseline</small>
            </button>
        `;
        setHtml(state, 'buildOpponentRail', baseline + state.filteredOpponents.map((opponent, index) => `
            <button type="button" role="option" id="buildOpponent-${escapeHtml(opponent.championId)}"
                    data-opponent-id="${escapeHtml(opponent.championId)}"
                    aria-selected="${opponent.championId === state.filters.opponentId}"
                    ${index + 1 === state.activeOpponentIndex ? 'data-active="true"' : ''}>
                ${opponent.imageUrl ? `<img src="${escapeHtml(opponent.imageUrl)}" alt="" width="44" height="44">` : ''}
                <span>${escapeHtml(opponent.label)}</span>
                <small>${escapeHtml(opponent.games)} games</small>
            </button>
        `).join(''));
        setHidden(state, 'buildOpponentEmpty', state.filteredOpponents.length !== 0);

        const search = element(state, 'buildOpponentSearch');
        if (search) {
            search.setAttribute('aria-expanded', 'true');
            if (state.activeOpponentIndex >= 0) {
                const activeId = state.activeOpponentIndex === 0
                    ? 'buildOpponent-all'
                    : `buildOpponent-${state.filteredOpponents[state.activeOpponentIndex - 1].championId}`;
                search.setAttribute(
                    'aria-activedescendant',
                    activeId
                );
            } else {
                search.removeAttribute('aria-activedescendant');
            }
        }
    }

    function renderEvidence(state, response) {
        setHtml(state, 'buildEvidence', `
            <strong>${escapeHtml(response.evidenceLabel || '')}</strong>
            <span>Confidence: ${escapeHtml(response.confidence || 'Unavailable')}</span>
            <span>Observed win rate: ${escapeHtml(response.winRate)}%</span>
            <span>${escapeHtml(response.explanation || '')}</span>
        `);
    }

    function renderFallbackState(state, response) {
        const hasFallbackReason = response.fallbackReason && response.fallbackReason !== 'NONE';
        if (!response.stale && !response.historical && !hasFallbackReason) {
            setHidden(state, 'buildFallbackState', true);
            return;
        }

        const prefix = response.stale ? 'Stale result. ' : 'Fallback result. ';
        setText(state, 'buildFallbackState', `${prefix}${response.explanation || ''}`);
        setHidden(state, 'buildFallbackState', false);
    }

    function renderChoices(choices = []) {
        if (!choices.length) {
            return '<p>No supported component returned.</p>';
        }

        return choices.map(choice => `
            <article class="build-choice">
                <div class="build-choice__assets">
                    ${(choice.assets || []).map(asset => `
                        <span class="build-choice__asset">
                            ${asset.imageUrl ? `<img src="${escapeHtml(asset.imageUrl)}" alt="" width="44" height="44">` : ''}
                            <span>${escapeHtml(asset.label)}</span>
                        </span>
                    `).join('')}
                </div>
                <small>${escapeHtml(choice.pickRate)}% pick, ${escapeHtml(choice.winRate)}% win, ${escapeHtml(choice.games)} games</small>
            </article>
        `).join('');
    }

    function renderSkillPriority(priority = []) {
        if (!priority.length) {
            return '<p>No supported max priority returned.</p>';
        }
        return priority.map(skill => `<span>${escapeHtml(skillLabel(skill))}</span>`).join(' <span aria-hidden="true">→</span> ');
    }

    function skillLabel(skill) {
        return ({ 1: 'Q', 2: 'W', 3: 'E', 4: 'R' })[skill] || String(skill);
    }

    function roleLabel(role) {
        return ({
            TOP: 'Top',
            JUNGLE: 'Jungle',
            MIDDLE: 'Mid',
            BOTTOM: 'Bottom',
            UTILITY: 'Support'
        })[role] || role;
    }

    function element(state, id) {
        return state.root?.querySelector(`#${id}`) || null;
    }

    function setHidden(state, id, hidden) {
        const target = element(state, id);
        if (target) target.hidden = hidden;
    }

    function setHtml(state, id, html) {
        const target = element(state, id);
        if (target) target.innerHTML = html;
    }

    function setText(state, id, text, keepChildren = false) {
        const target = element(state, id);
        if (!target) return;
        if (keepChildren && target.querySelector) {
            const message = target.querySelector('span');
            if (message) {
                message.textContent = text;
                return;
            }
        }
        target.textContent = text;
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    return championBuilds;
});
