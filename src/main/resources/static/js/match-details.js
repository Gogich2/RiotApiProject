(function () {
    const CARD_SELECTOR = '[data-match-details-card]';
    const detailsCache = new Map();

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
            renderDetailsView(container, details);
        } catch (error) {
            console.error('Could not load match details:', error);
            renderErrorState(container, 'Could not load match details.');
        }
    }

    function renderDetailsView(container, details) {
        const selectedParticipant = details.selectedParticipant
            || (details.participants && details.participants[0])
            || null;
        const timelineEvents = details.timelineEvents || [];
        const timelineFilter = 'ALL';

        container.innerHTML = `
            <div class="match-details-view">
                ${renderMatchHero(details.match || {}, selectedParticipant, details.teams || [])}
                <div class="player-tabs match-tabs" data-match-tabs>
                    ${renderTabButton('postgame', 'Post Game', true)}
                    ${renderTabButton('performance', 'Performance', false)}
                    ${renderTabButton('build', 'Item Build', false)}
                    ${renderTabButton('timeline', 'Timeline', false)}
                    ${renderTabButton('metrics', 'Metrics', false)}
                </div>

                <section class="section" data-match-tab-panel="postgame">
                    <h2 class="section__title">Post Game</h2>
                    <div class="match-teams">
                        ${(details.teams || []).map(team => renderPostGameTeam(team)).join('')}
                    </div>
                </section>

                <section class="section" data-match-tab-panel="performance" hidden>
                    <h2 class="section__title">Performance</h2>
                    <div class="table-wrapper">
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

                <section class="section" data-match-tab-panel="build" hidden>
                    <h2 class="section__title">Item Build</h2>
                    ${renderItemBuild(selectedParticipant)}
                </section>

                <section class="section" data-match-tab-panel="timeline" hidden>
                    <div class="section-header">
                        <h2 class="section__title">Timeline</h2>
                        <div class="match-filters" data-match-timeline-filters>
                            ${renderTimelineFilterButton('ALL', 'All', true)}
                            ${renderTimelineFilterButton('KILLS', 'Kills', false)}
                            ${renderTimelineFilterButton('OBJECTIVES', 'Objectives', false)}
                            ${renderTimelineFilterButton('ITEMS', 'Items', false)}
                            ${renderTimelineFilterButton('VISION', 'Vision', false)}
                        </div>
                    </div>
                    <div class="match-timeline-layout">
                        <div class="match-timeline-map">
                            <img src="img/ui/match-map.svg" alt="Summoner's Rift map"
                                 onerror="this.onerror=null; this.remove();">
                        </div>
                        <div class="match-timeline-list" data-match-timeline-list>
                            ${renderTimelineList(timelineEvents, timelineFilter, details.participants || [])}
                        </div>
                    </div>
                </section>

                <section class="section" data-match-tab-panel="metrics" hidden>
                    <h2 class="section__title">Metrics</h2>
                    <div class="empty-box">
                        ${escapeHtml(getMetricsMessage(details.metrics))}
                    </div>
                </section>
            </div>
        `;

        setupTabInteractions(container);
        setupTimelineFilterInteractions(container, details);
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

    function renderMatchHero(match, participant, teams) {
        return `
            <section class="match-hero">
                <div class="match-hero__header">
                    <div>
                        <span class="match-hero__queue">${escapeHtml(match.queueName || 'Match')}</span>
                        <h1 class="match-hero__title">${escapeHtml(match.matchId || 'Unknown match')}</h1>
                        <div class="match-hero__meta">
                            <span>${escapeHtml(match.patch || '-')}</span>
                            <span>${escapeHtml(formatMatchDuration(match.gameDurationMs))}</span>
                            <span>${escapeHtml(formatDateTime(match.gameCreationMs))}</span>
                        </div>
                    </div>
                    ${participant ? renderSelectedSummary(participant) : ''}
                </div>
                <div class="match-hero__teams">
                    ${teams.map(team => renderTeamCompact(team)).join('')}
                </div>
            </section>
        `;
    }

    function renderSelectedSummary(participant) {
        const finalItems = participant.finalItems || [];

        return `
            <div class="match-selected">
                <div class="match-selected__top">
                    ${participant.championImageUrl ? `
                        <img class="match-selected__icon"
                             src="${escapeHtml(participant.championImageUrl)}"
                             alt="${escapeHtml(participant.championName || 'Champion')}"
                             onerror="this.onerror=null; this.remove();">
                    ` : ''}
                    <div>
                        <div class="match-selected__name">
                            ${escapeHtml(participant.championName || 'Unknown')}
                        </div>
                        <div class="match-selected__player">
                            ${escapeHtml(formatPlayerName(participant))}
                        </div>
                    </div>
                    <span class="${participant.win ? 'result result--win' : 'result result--loss'}">
                        ${participant.win ? 'Victory' : 'Defeat'}
                    </span>
                </div>
                <div class="match-selected__stats">
                    <span><strong>${escapeHtml(formatKdaValue(participant))}</strong> KDA</span>
                    <span><strong>${formatNumberValue(getCsValue(participant))}</strong> CS</span>
                    <span><strong>${formatNumberValue(participant.visionScore)}</strong> Vision</span>
                </div>
                <div class="player-match-items">
                    ${finalItems.length > 0
                        ? finalItems.map(item => renderItemIcon(item)).join('')
                        : '<span class="player-match-items__empty">No final items recorded.</span>'}
                </div>
            </div>
        `;
    }

    function renderTeamCompact(team) {
        const participants = team.participants || [];

        return `
            <article class="match-team-compact">
                <div class="match-team-compact__header">
                    <strong>${escapeHtml(team.teamName || 'Team')}</strong>
                    <span class="${team.win ? 'result result--win' : 'result result--loss'}">
                        ${team.win ? 'Victory' : 'Defeat'}
                    </span>
                </div>
                <div class="match-team-compact__players">
                    ${participants.map(participant => `
                        <div class="match-team-compact__player">
                            ${participant.championImageUrl ? `
                                <img class="match-team-compact__icon"
                                     src="${escapeHtml(participant.championImageUrl)}"
                                     alt="${escapeHtml(participant.championName || 'Champion')}"
                                     onerror="this.onerror=null; this.remove();">
                            ` : ''}
                            <span>${escapeHtml(formatPlayerName(participant))}</span>
                        </div>
                    `).join('')}
                </div>
            </article>
        `;
    }

    function renderPostGameTeam(team) {
        return `
            <section class="match-team-card">
                <div class="match-team-card__header">
                    <h3>${escapeHtml(team.teamName || 'Team')}</h3>
                    <span class="${team.win ? 'result result--win' : 'result result--loss'}">
                        ${team.win ? 'Victory' : 'Defeat'}
                    </span>
                </div>
                <div class="match-team-card__list">
                    ${(team.participants || []).map(participant => renderPostGameParticipant(participant)).join('')}
                </div>
            </section>
        `;
    }

    function renderPostGameParticipant(participant) {
        const finalItems = participant.finalItems || [];

        return `
            <article class="match-participant-row">
                <div class="match-participant-row__identity">
                    <a class="match-champion-link" href="champion.html?id=${encodeURIComponent(participant.championId)}">
                        ${participant.championImageUrl ? `
                            <img class="match-champion-link__image"
                                 src="${escapeHtml(participant.championImageUrl)}"
                                 alt="${escapeHtml(participant.championName || 'Champion')}"
                                 onerror="this.onerror=null; this.remove();">
                        ` : ''}
                        <span>${escapeHtml(participant.championName || 'Unknown')}</span>
                    </a>
                    <a class="match-participant-row__player"
                       href="player.html?puuid=${encodeURIComponent(participant.puuid || '')}">
                        ${escapeHtml(formatPlayerName(participant))}
                    </a>
                </div>
                <div class="match-participant-row__stats">
                    <span><strong>${escapeHtml(formatKdaValue(participant))}</strong> KDA</span>
                    <span><strong>${formatNumberValue(participant.totalDamageToChampions)}</strong> DMG</span>
                    <span><strong>${formatNumberValue(participant.goldEarned)}</strong> Gold</span>
                    <span><strong>${formatNumberValue(getCsValue(participant))}</strong> CS</span>
                    <span><strong>${formatNumberValue(participant.visionScore)}</strong> Vision</span>
                    <span>
                        <strong>${formatNumberValue(participant.wardsPlaced)}</strong>/
                        <strong>${formatNumberValue(participant.wardsKilled)}</strong> Wards
                    </span>
                </div>
                <div class="player-match-items match-participant-row__items">
                    ${finalItems.length > 0
                        ? finalItems.map(item => renderItemIcon(item)).join('')
                        : '<span class="player-match-items__empty">No items</span>'}
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
                        <div class="empty-box">No participant performance data.</div>
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
            return '<div class="empty-box">No participant selected for build details.</div>';
        }

        const runeGroups = groupRunesByStyleType(participant.runes || []);
        const primaryRunes = runeGroups.primary || [];
        const secondaryRunes = runeGroups.secondary || [];
        const skillOrder = participant.skillOrder || [];
        const itemEvents = participant.itemEvents || [];

        return `
            <div class="match-build">
                <div class="match-build__section">
                    <h3 class="match-build__title">Runes</h3>
                    <div class="match-runes">
                        ${renderRuneGroup('Primary', primaryRunes)}
                        ${renderRuneGroup('Secondary', secondaryRunes)}
                    </div>
                </div>
                <div class="match-build__section">
                    <h3 class="match-build__title">Skill order</h3>
                    ${renderSkillOrderGrid(skillOrder)}
                </div>
                <div class="match-build__section">
                    <h3 class="match-build__title">Items timeline</h3>
                    <div class="match-build-items">
                        ${itemEvents.length > 0
                            ? itemEvents.map(event => renderItemEvent(event)).join('')
                            : '<div class="empty-box">No item purchase events recorded.</div>'}
                    </div>
                </div>
            </div>
        `;
    }

    function renderRuneGroup(label, runes) {
        if (!runes || runes.length === 0) {
            return `
                <div class="match-rune-group">
                    <h4>${escapeHtml(label)}</h4>
                    <div class="empty-box">No rune data.</div>
                </div>
            `;
        }

        const styleName = runes[0].styleName || label;
        const styleIconUrl = runes[0].styleIconUrl;

        return `
            <div class="match-rune-group">
                <h4>
                    ${styleIconUrl ? `
                        <img class="match-rune-group__style-icon"
                             src="${escapeHtml(styleIconUrl)}"
                             alt="${escapeHtml(styleName)}"
                             onerror="this.onerror=null; this.remove();">
                    ` : ''}
                    ${escapeHtml(styleName)}
                </h4>
                <div class="match-rune-group__items">
                    ${runes.map(rune => `
                        <span class="match-rune-item" title="${escapeHtml(rune.runeName || 'Rune')}">
                            ${rune.runeIconUrl ? `
                                <img class="match-rune-item__icon"
                                     src="${escapeHtml(rune.runeIconUrl)}"
                                     alt="${escapeHtml(rune.runeName || 'Rune')}"
                                     onerror="this.onerror=null; this.remove();">
                            ` : ''}
                        </span>
                    `).join('')}
                </div>
            </div>
        `;
    }

    function renderSkillOrderGrid(skillOrder) {
        if (!skillOrder || skillOrder.length === 0) {
            return '<div class="empty-box">No skill order data recorded.</div>';
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
                <div>
                    <strong>${escapeHtml(event.itemName || 'Item')}</strong>
                    <div class="match-build-item__meta">
                        ${escapeHtml(event.eventType || 'ITEM_PURCHASED')}
                    </div>
                </div>
            </div>
        `;
    }

    function renderTimelineList(events, filter, participants) {
        const filtered = (events || []).filter(event => timelineEventMatchesFilter(event, filter));

        if (filtered.length === 0) {
            return '<div class="empty-box">No timeline events for this filter.</div>';
        }

        return filtered.map(event => `
            <article class="match-timeline-event">
                <div class="match-timeline-event__time">${formatMinute(event.minute)}</div>
                <div class="match-timeline-event__content">
                    <strong>${escapeHtml(formatTimelineEventTitle(event, participants))}</strong>
                    <div class="match-timeline-event__meta">
                        ${event.itemName ? `<span>${escapeHtml(event.itemName)}</span>` : ''}
                        ${event.position ? `
                            <span>
                                X:${formatNumberValue(event.position.x)} Y:${formatNumberValue(event.position.y)}
                            </span>
                        ` : ''}
                    </div>
                </div>
            </article>
        `).join('');
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

        const type = event.type || '';

        if (filter === 'KILLS') {
            return type === 'CHAMPION_KILL';
        }

        if (filter === 'OBJECTIVES') {
            return type === 'ELITE_MONSTER_KILL' || type === 'BUILDING_KILL';
        }

        if (filter === 'ITEMS') {
            return type === 'ITEM_PURCHASED';
        }

        if (filter === 'VISION') {
            return type === 'WARD_PLACED' || type === 'WARD_KILL';
        }

        return true;
    }

    function formatTimelineEventTitle(event, participants) {
        const type = event.type || 'EVENT';

        if (type === 'CHAMPION_KILL') {
            return `Kill: ${formatParticipantRef(participants, event.killerId)}`
                + ` -> ${formatParticipantRef(participants, event.victimId)}`;
        }

        if (type === 'ELITE_MONSTER_KILL') {
            return `Objective taken by ${formatParticipantRef(
                participants,
                event.killerId || event.participantId
            )}`;
        }

        if (type === 'BUILDING_KILL') {
            return `Building destroyed by ${formatParticipantRef(
                participants,
                event.killerId || event.participantId
            )}`;
        }

        if (type === 'ITEM_PURCHASED') {
            return `${formatParticipantRef(participants, event.participantId)} purchased an item`;
        }

        if (type === 'WARD_PLACED') {
            return `${formatParticipantRef(participants, event.participantId)} placed a ward`;
        }

        if (type === 'WARD_KILL') {
            return `${formatParticipantRef(
                participants,
                event.killerId || event.participantId
            )} cleared vision`;
        }

        return type;
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

    function renderItemIcon(item) {
        const imageUrl = item.imageUrl;
        const itemName = item.itemName || 'Item';

        if (!imageUrl) {
            return `
                <span class="player-match-item player-match-item--text">
                    ${escapeHtml(itemName)}
                </span>
            `;
        }

        return `
            <span class="player-match-item" title="${escapeHtml(itemName)}">
                <img class="player-match-item__image"
                     src="${escapeHtml(imageUrl)}"
                     alt="${escapeHtml(itemName)}"
                     onerror="this.onerror=null; this.remove();">
            </span>
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
            year: 'numeric',
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
                <div class="empty-box">${escapeHtml(message)}</div>
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
