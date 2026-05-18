(function () {
    const CARD_SELECTOR = '[data-match-details-card]';
    const detailsCache = new Map();
    const timelineTypeGroups = {
        KILLS: ['CHAMPION_KILL'],
        OBJECTIVES: ['ELITE_MONSTER_KILL', 'BUILDING_KILL', 'TURRET_PLATE_DESTROYED'],
        ITEMS: ['ITEM_PURCHASED', 'ITEM_DESTROYED', 'ITEM_SOLD', 'ITEM_UNDO'],
        VISION: ['WARD_PLACED', 'WARD_KILL']
    };

    async function loadDetails(matchId, puuid) {
        const cacheKey = `${matchId}::${puuid || ''}`;

        if (detailsCache.has(cacheKey)) {
            return detailsCache.get(cacheKey);
        }

        const details = await api.getMatchDetails(matchId, puuid);
        detailsCache.set(cacheKey, details);
        return details;
    }

    async function renderStandaloneFromQuery() {
        const container = document.getElementById('matchPageContent');
        const matchId = getQueryParam('id');
        const puuid = getQueryParam('puuid');

        if (!container) {
            return;
        }

        if (!matchId) {
            renderErrorState(container, 'Match ID is missing.');
            return;
        }

        renderLoadingState(container, 'Loading match details...');

        try {
            const details = await loadDetails(matchId, puuid);
            renderDetailsView(container, details, { standalone: true });
        } catch (error) {
            console.error('Could not load match details:', error);
            renderErrorState(container, 'Could not load match details.');
        }
    }

    function renderDetailsView(container, details, options = {}) {
        const selectedParticipant = details.selectedParticipant
            || (details.participants && details.participants[0])
            || null;
        const isStandalone = Boolean(options.standalone);

        container.innerHTML = `
            <div class="match-details-view ${isStandalone ? 'match-details-view--standalone' : ''}">
                ${renderSummary(details.match || {}, selectedParticipant, details.teams || [])}
                <div class="match-details-tabs" data-match-tabs>
                    ${renderTabButton('postgame', 'Post Game', true)}
                    ${renderTabButton('performance', 'Performance', false)}
                    ${renderTabButton('build', 'Item Build', false)}
                    ${renderTabButton('timeline', 'Timeline', false)}
                    ${renderTabButton('metrics', 'Metrics', false)}
                </div>

                <section class="match-panel" data-match-tab-panel="postgame">
                    <div class="match-postgame">
                        ${(details.teams || []).map(team => renderPostGameTeam(team)).join('')}
                    </div>
                </section>

                <section class="match-panel" data-match-tab-panel="performance" hidden>
                    <div class="table-wrapper match-performance-table">
                        <table class="table">
                            <thead>
                            <tr>
                                <th>Player</th>
                                <th>Kills</th>
                                <th>KDA</th>
                                <th>Damage</th>
                                <th>Gold</th>
                                <th>Wards</th>
                                <th>CS</th>
                            </tr>
                            </thead>
                            <tbody>
                                ${renderPerformanceRows(details.participants || [])}
                            </tbody>
                        </table>
                    </div>
                </section>

                <section class="match-panel" data-match-tab-panel="build" hidden>
                    ${renderItemBuild(selectedParticipant)}
                </section>

                <section class="match-panel" data-match-tab-panel="timeline" hidden>
                    <div class="match-timeline-toolbar">
                        <div class="match-filters" data-match-timeline-filters>
                            ${renderTimelineFilterButton('ALL', 'All', true)}
                            ${renderTimelineFilterButton('KILLS', 'Kills', false)}
                            ${renderTimelineFilterButton('OBJECTIVES', 'Objectives', false)}
                            ${renderTimelineFilterButton('ITEMS', 'Items', false)}
                            ${renderTimelineFilterButton('VISION', 'Vision', false)}
                        </div>
                    </div>
                    <div class="match-timeline-layout">
                        <div class="match-timeline-list" data-match-timeline-list>
                            ${renderTimelineList(details.timelineEvents || [], 'ALL', details.participants || [])}
                        </div>
                        <div class="match-timeline-map">
                            <img src="img/ui/match-map.svg" alt="Summoner's Rift map"
                                 onerror="this.onerror=null; this.parentElement.hidden=true;">
                        </div>
                    </div>
                </section>

                <section class="match-panel" data-match-tab-panel="metrics" hidden>
                    <div class="match-compact-empty">
                        ${escapeHtml(getMetricsMessage(details.metrics))}
                    </div>
                </section>
            </div>
        `;

        setupTabInteractions(container);
        setupTimelineFilterInteractions(container, details);
    }

    function renderSummary(match, participant, teams) {
        return `
            <section class="match-summary">
                <div class="match-summary__top">
                    <div class="match-summary__meta">
                        <span class="match-summary__queue">${escapeHtml(match.queueName || 'Match')}</span>
                        <strong class="match-summary__id">${escapeHtml(match.matchId || 'Unknown match')}</strong>
                        <div class="match-summary__chips">
                            ${renderSummaryChip(match.patch || '-')}
                            ${renderSummaryChip(formatMatchDuration(match.gameDurationMs))}
                            ${renderSummaryChip(formatDateTime(match.gameCreationMs))}
                        </div>
                    </div>
                    ${participant ? renderSelectedSummary(participant) : ''}
                </div>
                <div class="match-summary__teams">
                    ${teams.map(team => renderTeamCompact(team)).join('')}
                </div>
            </section>
        `;
    }

    function renderSelectedSummary(participant) {
        return `
            <div class="match-selected">
                <div class="match-selected__identity">
                    ${renderChampionThumb(
                        participant.championImageUrl,
                        participant.championName || 'Champion',
                        'match-selected__icon'
                    )}
                    <div class="match-selected__copy">
                        <strong>${escapeHtml(participant.championName || 'Unknown')}</strong>
                        <span>${escapeHtml(formatPlayerName(participant))}</span>
                    </div>
                    <span class="${participant.win ? 'result result--win' : 'result result--loss'}">
                        ${participant.win ? 'Victory' : 'Defeat'}
                    </span>
                </div>
                <div class="match-selected__stats">
                    ${renderSummaryStat('KDA', formatKdaValue(participant))}
                    ${renderSummaryStat('CS', formatNumberValue(getCsValue(participant)))}
                    ${renderSummaryStat('Vision', formatNumberValue(participant.visionScore))}
                </div>
                <div class="player-match-items player-match-items--compact">
                    ${renderItemRow(participant.finalItems || [], 'No final items')}
                </div>
            </div>
        `;
    }

    function renderTeamCompact(team) {
        return `
            <article class="match-team-strip">
                <div class="match-team-strip__header">
                    <strong>${escapeHtml(team.teamName || 'Team')}</strong>
                    <span class="${team.win ? 'result result--win' : 'result result--loss'}">
                        ${team.win ? 'Victory' : 'Defeat'}
                    </span>
                </div>
                <div class="match-team-strip__players">
                    ${(team.participants || []).map(participant => `
                        <span class="match-team-strip__player" title="${escapeHtml(formatPlayerName(participant))}">
                            ${renderChampionThumb(
                                participant.championImageUrl,
                                participant.championName || 'Champion',
                                'match-team-strip__icon'
                            )}
                            <span>${escapeHtml(formatPlayerName(participant))}</span>
                        </span>
                    `).join('')}
                </div>
            </article>
        `;
    }

    function renderPostGameTeam(team) {
        return `
            <section class="match-team-table">
                <div class="match-team-table__header">
                    <h3>${escapeHtml(team.teamName || 'Team')}</h3>
                    <span class="${team.win ? 'result result--win' : 'result result--loss'}">
                        ${team.win ? 'Victory' : 'Defeat'}
                    </span>
                </div>
                <div class="match-team-table__rows">
                    ${(team.participants || []).map(participant => renderPostGameParticipant(participant)).join('')}
                </div>
            </section>
        `;
    }

    function renderPostGameParticipant(participant) {
        return `
            <article class="match-participant-row">
                <div class="match-participant-row__identity">
                    <a class="match-champion-link" href="champion.html?id=${encodeURIComponent(participant.championId)}">
                        ${renderChampionThumb(
                            participant.championImageUrl,
                            participant.championName || 'Champion',
                            'match-champion-link__image'
                        )}
                        <span>${escapeHtml(participant.championName || 'Unknown')}</span>
                    </a>
                    <a class="match-participant-row__player"
                       href="player.html?puuid=${encodeURIComponent(participant.puuid || '')}">
                        ${escapeHtml(formatPlayerName(participant))}
                    </a>
                </div>
                <div class="match-participant-row__stats">
                    ${renderRowStat('KDA', formatKdaValue(participant))}
                    ${renderRowStat('DMG', formatNumberValue(participant.totalDamageToChampions))}
                    ${renderRowStat('Gold', formatNumberValue(participant.goldEarned))}
                    ${renderRowStat('CS', formatNumberValue(getCsValue(participant)))}
                    ${renderRowStat('Vision', formatNumberValue(participant.visionScore))}
                    ${renderRowStat(
                        'Wards',
                        `${formatNumberValue(participant.wardsPlaced)}/${formatNumberValue(participant.wardsKilled)}`
                    )}
                </div>
                <div class="player-match-items player-match-items--compact match-participant-row__items">
                    ${renderItemRow(participant.finalItems || [], 'No items')}
                </div>
            </article>
        `;
    }

    function renderPerformanceRows(participants) {
        const sorted = [...participants].sort((left, right) =>
            Number(right.totalDamageToChampions || 0) - Number(left.totalDamageToChampions || 0));

        if (sorted.length === 0) {
            return `
                <tr>
                    <td colspan="7">
                        <div class="match-compact-empty">No participant performance data.</div>
                    </td>
                </tr>
            `;
        }

        return sorted.map(participant => `
            <tr>
                <td>${escapeHtml(formatPlayerName(participant))}</td>
                <td>${formatNumberValue(participant.kills)}</td>
                <td>${escapeHtml(formatKdaValue(participant))}</td>
                <td>${formatNumberValue(participant.totalDamageToChampions)}</td>
                <td>${formatNumberValue(participant.goldEarned)}</td>
                <td>${formatNumberValue(participant.wardsPlaced)}</td>
                <td>${formatNumberValue(getCsValue(participant))}</td>
            </tr>
        `).join('');
    }

    function renderItemBuild(participant) {
        if (!participant) {
            return '<div class="match-compact-empty">No participant selected for build details.</div>';
        }

        const runeGroups = groupRunesByStyleType(participant.runes || []);
        const primaryRunes = runeGroups.primary || [];
        const secondaryRunes = runeGroups.secondary || [];
        const skillOrder = participant.skillOrder || [];
        const itemEvents = participant.itemEvents || [];

        return `
            <div class="match-build">
                <section class="match-build__section">
                    <div class="match-build__header">
                        <h3 class="match-build__title">Runes</h3>
                        <span>${escapeHtml(formatPlayerName(participant))}</span>
                    </div>
                    <div class="match-runes">
                        ${renderRuneGroup('Primary', primaryRunes)}
                        ${renderRuneGroup('Secondary', secondaryRunes)}
                    </div>
                </section>
                <section class="match-build__section">
                    <h3 class="match-build__title">Skill order</h3>
                    ${renderSkillOrderGrid(skillOrder)}
                </section>
                <section class="match-build__section">
                    <h3 class="match-build__title">Items timeline</h3>
                    <div class="match-build-items">
                        ${itemEvents.length > 0
                            ? itemEvents.map(event => renderItemEvent(event)).join('')
                            : '<div class="match-compact-empty">No item purchase events recorded.</div>'}
                    </div>
                </section>
            </div>
        `;
    }

    function renderRuneGroup(label, runes) {
        if (!runes || runes.length === 0) {
            return `
                <div class="match-rune-group">
                    <div class="match-rune-group__header">
                        <strong>${escapeHtml(label)}</strong>
                    </div>
                    <div class="match-compact-empty">No rune data.</div>
                </div>
            `;
        }

        const styleName = runes[0].styleName || label;
        const styleIconUrl = runes[0].styleIconUrl;

        return `
            <div class="match-rune-group">
                <div class="match-rune-group__header">
                    ${renderRunePlaceholder(
                        styleIconUrl,
                        styleName,
                        'match-rune-item match-rune-item--style'
                    )}
                    <strong>${escapeHtml(styleName)}</strong>
                </div>
                <div class="match-rune-group__items">
                    ${runes.map(rune => renderRunePlaceholder(
                        rune.runeIconUrl,
                        rune.runeName || 'Rune',
                        `match-rune-item ${rune.isKeystone ? 'match-rune-item--keystone' : ''}`
                    )).join('')}
                </div>
            </div>
        `;
    }

    function renderSkillOrderGrid(skillOrder) {
        if (!skillOrder || skillOrder.length === 0) {
            return '<div class="match-compact-empty">No skill order data recorded.</div>';
        }

        const skillMap = { 1: 'Q', 2: 'W', 3: 'E', 4: 'R' };
        const byLevel = {};
        const levels = Array.from({ length: 18 }, (_, index) => index + 1);
        const rows = ['Q', 'W', 'E', 'R'];

        skillOrder.forEach(entry => {
            byLevel[entry.skillOrder] = entry.skillSlot;
        });

        return `
            <div class="match-skill-grid">
                <div class="match-skill-grid__row match-skill-grid__row--header">
                    <span class="match-skill-grid__label"></span>
                    ${levels.map(level => `
                        <span class="match-skill-grid__cell">${level}</span>
                    `).join('')}
                </div>
                ${rows.map(row => `
                    <div class="match-skill-grid__row">
                        <span class="match-skill-grid__label">${row}</span>
                        ${levels.map(level => {
                            const isLearned = skillMap[byLevel[level]] === row;
                            return `
                                <span class="match-skill-grid__cell
                                        ${isLearned ? 'match-skill-grid__cell--active' : ''}">
                                    ${isLearned ? row : ''}
                                </span>
                            `;
                        }).join('')}
                    </div>
                `).join('')}
            </div>
        `;
    }

    function renderItemEvent(event) {
        return `
            <div class="match-build-item">
                <span class="match-build-item__minute">${formatMinute(event.minute)}</span>
                ${renderItemIcon(event)}
                <div class="match-build-item__copy">
                    <strong>${escapeHtml(event.itemName || 'Item')}</strong>
                    <span>${escapeHtml(event.eventType || 'ITEM_PURCHASED')}</span>
                </div>
            </div>
        `;
    }

    function renderTimelineList(events, filter, participants) {
        const filtered = (events || []).filter(event => timelineEventMatchesFilter(event, filter));

        if (filtered.length === 0) {
            return '<div class="match-compact-empty">No timeline events for this filter.</div>';
        }

        return filtered.map(event => `
            <article class="match-timeline-event">
                <div class="match-timeline-event__time">${formatMinute(event.minute)}</div>
                <div class="match-timeline-event__content">
                    <strong>${escapeHtml(formatTimelineEventTitle(event, participants))}</strong>
                    <div class="match-timeline-event__meta">
                        ${renderTimelineMeta(event)}
                    </div>
                </div>
            </article>
        `).join('');
    }

    function renderTimelineMeta(event) {
        const parts = [];

        if (event.itemName) {
            parts.push(`<span>${escapeHtml(event.itemName)}</span>`);
        }

        if (event.wardType) {
            parts.push(`<span>${escapeHtml(event.wardType)}</span>`);
        }

        if (event.buildingType) {
            parts.push(`<span>${escapeHtml(event.buildingType)}</span>`);
        }

        if (event.laneType) {
            parts.push(`<span>${escapeHtml(event.laneType)}</span>`);
        }

        if (event.position) {
            parts.push(`
                <span>X:${formatNumberValue(event.position.x)} Y:${formatNumberValue(event.position.y)}</span>
            `);
        }

        return parts.join('');
    }

    function setupTabInteractions(container) {
        const tabs = container.querySelector('[data-match-tabs]');

        if (!tabs) {
            return;
        }

        tabs.addEventListener('click', event => {
            const button = event.target.closest('[data-match-tab-button]');

            if (!button) {
                return;
            }

            const target = button.dataset.matchTabButton;

            tabs.querySelectorAll('[data-match-tab-button]').forEach(tabButton => {
                tabButton.classList.toggle('player-tabs__button--active', tabButton === button);
            });

            container.querySelectorAll('[data-match-tab-panel]').forEach(panel => {
                panel.hidden = panel.dataset.matchTabPanel !== target;
            });
        });
    }

    function setupTimelineFilterInteractions(container, details) {
        const filters = container.querySelector('[data-match-timeline-filters]');
        const timelineList = container.querySelector('[data-match-timeline-list]');

        if (!filters || !timelineList) {
            return;
        }

        filters.addEventListener('click', event => {
            const button = event.target.closest('[data-timeline-filter]');

            if (!button) {
                return;
            }

            const filter = button.dataset.timelineFilter || 'ALL';

            filters.querySelectorAll('[data-timeline-filter]').forEach(filterButton => {
                filterButton.classList.toggle('match-filter-button--active', filterButton === button);
            });

            timelineList.innerHTML = renderTimelineList(
                details.timelineEvents || [],
                filter,
                details.participants || []
            );
        });
    }

    function timelineEventMatchesFilter(event, filter) {
        if (!filter || filter === 'ALL') {
            return true;
        }

        const type = String(event.type || '').toUpperCase();

        if (filter === 'ITEMS') {
            return type.startsWith('ITEM_') || timelineTypeGroups.ITEMS.includes(type);
        }

        if (filter === 'VISION') {
            return type.includes('WARD') || timelineTypeGroups.VISION.includes(type);
        }

        const types = timelineTypeGroups[filter];
        return Array.isArray(types) ? types.includes(type) : true;
    }

    function formatTimelineEventTitle(event, participants) {
        const type = String(event.type || 'EVENT').toUpperCase();

        if (type === 'CHAMPION_KILL') {
            return `Kill: ${formatParticipantRef(participants, event.killerId)}`
                + ` -> ${formatParticipantRef(participants, event.victimId)}`;
        }

        if (type === 'ELITE_MONSTER_KILL') {
            return `${formatParticipantRef(participants, event.killerId || event.participantId)}`
                + ' secured an epic monster';
        }

        if (type === 'BUILDING_KILL') {
            const building = event.buildingType ? `${event.buildingType.toLowerCase()} ` : '';
            return `${formatParticipantRef(participants, event.killerId || event.participantId)} destroyed a `
                + `${building}structure`;
        }

        if (type.startsWith('ITEM_')) {
            return `${formatParticipantRef(participants, event.participantId)} ${formatItemEventVerb(type)}`;
        }

        if (type.includes('WARD')) {
            return `${formatParticipantRef(participants, event.killerId || event.participantId)} ${formatWardEvent(type)}`;
        }

        return prettifyType(type);
    }

    function formatItemEventVerb(type) {
        if (type === 'ITEM_PURCHASED') {
            return 'purchased an item';
        }

        if (type === 'ITEM_DESTROYED') {
            return 'used an item';
        }

        if (type === 'ITEM_SOLD') {
            return 'sold an item';
        }

        if (type === 'ITEM_UNDO') {
            return 'undid an item action';
        }

        return prettifyType(type).toLowerCase();
    }

    function formatWardEvent(type) {
        if (type === 'WARD_PLACED') {
            return 'placed a ward';
        }

        if (type === 'WARD_KILL') {
            return 'cleared vision';
        }

        return prettifyType(type).toLowerCase();
    }

    function prettifyType(type) {
        return String(type || 'EVENT')
            .toLowerCase()
            .split('_')
            .map(part => part.charAt(0).toUpperCase() + part.slice(1))
            .join(' ');
    }

    function formatParticipantRef(participants, participantId) {
        if (!participants || participants.length === 0) {
            return `P${participantId || '?'}`;
        }

        const participant = participants.find(entry => entry.participantId === participantId);

        if (!participant) {
            return `P${participantId || '?'}`;
        }

        return formatPlayerName(participant);
    }

    function groupRunesByStyleType(runes) {
        return runes.reduce((groups, rune) => {
            const key = String(rune.styleType || '').toLowerCase() === 'secondary'
                ? 'secondary'
                : 'primary';

            if (!groups[key]) {
                groups[key] = [];
            }

            groups[key].push(rune);
            return groups;
        }, {});
    }

    function renderItemRow(items, emptyText) {
        if (!items || items.length === 0) {
            return `<span class="player-match-items__empty">${escapeHtml(emptyText)}</span>`;
        }

        return items.map(item => renderItemIcon(item)).join('');
    }

    function renderItemIcon(item) {
        const imageUrl = item.imageUrl;
        const itemName = item.itemName || 'Item';

        if (!imageUrl) {
            return `
                <span class="player-match-item player-match-item--text" title="${escapeHtml(itemName)}">
                    ${escapeHtml(itemName)}
                </span>
            `;
        }

        return `
            <span class="player-match-item" title="${escapeHtml(itemName)}">
                <img class="player-match-item__image"
                     src="${escapeHtml(imageUrl)}"
                     alt="${escapeHtml(itemName)}"
                     onerror="this.onerror=null; this.parentElement.remove();">
            </span>
        `;
    }

    function renderRunePlaceholder(iconUrl, label, className) {
        const safeLabel = escapeHtml(label || 'Rune');

        if (!iconUrl) {
            return `<span class="${className} match-rune-item--empty" title="${safeLabel}"></span>`;
        }

        return `
            <span class="${className}" title="${safeLabel}">
                <img class="match-rune-item__icon"
                     src="${escapeHtml(iconUrl)}"
                     alt="${safeLabel}"
                     onerror="this.onerror=null; this.parentElement.classList.add('match-rune-item--empty'); this.remove();">
            </span>
        `;
    }

    function renderChampionThumb(imageUrl, label, className) {
        if (!imageUrl) {
            return '';
        }

        return `
            <img class="${className}"
                 src="${escapeHtml(imageUrl)}"
                 alt="${escapeHtml(label)}"
                 onerror="this.onerror=null; this.remove();">
        `;
    }

    function renderSummaryChip(value) {
        return `<span class="match-summary__chip">${escapeHtml(value)}</span>`;
    }

    function renderSummaryStat(label, value) {
        return `
            <span class="match-selected__stat">
                <small>${escapeHtml(label)}</small>
                <strong>${escapeHtml(value)}</strong>
            </span>
        `;
    }

    function renderRowStat(label, value) {
        return `
            <span class="match-row-stat">
                <small>${escapeHtml(label)}</small>
                <strong>${escapeHtml(value)}</strong>
            </span>
        `;
    }

    function renderTabButton(tabId, label, isActive) {
        return `
            <button class="player-tabs__button ${isActive ? 'player-tabs__button--active' : ''}"
                    type="button" data-match-tab-button="${tabId}">
                ${escapeHtml(label)}
            </button>
        `;
    }

    function renderTimelineFilterButton(filter, label, isActive) {
        return `
            <button class="button button--secondary match-filter-button
                    ${isActive ? 'match-filter-button--active' : ''}"
                    type="button" data-timeline-filter="${filter}">
                ${escapeHtml(label)}
            </button>
        `;
    }

    function getCsValue(participant) {
        return Number(participant.totalMinionsKilled || 0)
            + Number(participant.neutralMinionsKilled || 0);
    }

    function formatPlayerName(participant) {
        const gameName = participant.gameName || 'Unknown';
        return participant.tagLine ? `${gameName}#${participant.tagLine}` : gameName;
    }

    function formatKdaValue(participant) {
        const kills = Number(participant.kills || 0);
        const deaths = Number(participant.deaths || 0);
        const assists = Number(participant.assists || 0);
        const ratio = Number(participant.kda || 0).toFixed(2);

        return `${kills}/${deaths}/${assists} (${ratio})`;
    }

    function formatMinute(minute) {
        if (minute === null || minute === undefined) {
            return '-';
        }

        return `${minute}m`;
    }

    function formatMatchDuration(durationMs) {
        if (!durationMs) {
            return '-';
        }

        return typeof formatDuration === 'function' ? formatDuration(durationMs) : String(durationMs);
    }

    function formatDateTime(timestampMs) {
        if (!timestampMs) {
            return '-';
        }

        return new Date(Number(timestampMs)).toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function formatNumberValue(value) {
        if (typeof formatNumber === 'function') {
            return formatNumber(value);
        }

        return String(value ?? 0);
    }

    function getMetricsMessage(metrics) {
        return metrics && metrics.message
            ? metrics.message
            : 'Metrics charts will be added later.';
    }

    function renderLoadingState(container, message) {
        container.innerHTML = `
            <div class="match-details-inline__state">
                <div class="match-compact-empty">${escapeHtml(message)}</div>
            </div>
        `;
    }

    function renderErrorState(container, message) {
        container.innerHTML = `
            <div class="match-details-inline__state">
                <div class="error-box">${escapeHtml(message)}</div>
            </div>
        `;
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    window.MatchDetailsView = {
        loadDetails,
        renderInto: renderDetailsView,
        renderLoadingState,
        renderErrorState,
        renderStandaloneFromQuery,
        selectors: {
            card: CARD_SELECTOR
        }
    };
})();
