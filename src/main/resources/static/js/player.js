let activePlayerTab = 'overview';
let cachedPlayerInsights = [];
let expandedMatchId = null;

document.addEventListener('DOMContentLoaded', async () => {
    const puuid = getQueryParam('puuid');

    if (!puuid) {
        showPlayerError('Player PUUID is missing.');
        return;
    }

    const refreshRanksButton = document.getElementById('refreshRanksButton');

    if (refreshRanksButton) {
        refreshRanksButton.addEventListener('click', async () => {
            await refreshPlayerRanks(puuid);
        });
    }

    setupPlayerTabs();
    setupMatchDetailsInline(puuid);

    renderPlayerRanks([]);
    renderPlayerRankChart([]);
    renderPlayerRankHistory([]);
    renderPlayerInsights([]);
    renderPlayerChampions([]);

    const loadMain = async () => {
        try {
            const [summary, matches] = await Promise.all([
                api.getPlayerSummary(puuid),
                api.getPlayerMatches(puuid, 20)
            ]);

            renderPlayerHero(summary);
            renderPlayerStats(summary);
            renderPlayerMatches(matches || []);
        } catch (error) {
            console.error('Could not load player:', error);
            showPlayerError('Could not load player.');
        }
    };

    const loadChampions = async () => {
        try {
            const champions = await api.getPlayerChampions(puuid);
            renderPlayerChampions(champions || []);
        } catch (error) {
            console.error('Could not load player champions:', error);
            renderPlayerChampions([]);
        }
    };

    const loadRanks = async () => {
        try {
            const ranks = await api.getPlayerRanks(puuid);
            renderPlayerRanks(ranks || []);
        } catch (error) {
            console.error('Could not load player ranks:', error);
            renderPlayerRanks([]);
        }
    };

    const loadRankHistory = async () => {
        try {
            const rankHistory = await api.getPlayerRankHistory(puuid);
            renderPlayerRankChart(rankHistory || []);
            renderPlayerRankHistory(rankHistory || []);
        } catch (error) {
            console.error('Could not load rank history:', error);
            renderPlayerRankChart([]);
            renderPlayerRankHistory([]);
        }
    };

    const loadInsights = async () => {
        try {
            cachedPlayerInsights = await api.getPlayerInsights(puuid) || [];
            renderInsightsForActiveTab();
        } catch (error) {
            console.error('Could not load insights:', error);
            cachedPlayerInsights = [];
            renderInsightsForActiveTab();
        }
    };

    await Promise.allSettled([
        loadMain(),
        loadChampions(),
        loadRanks(),
        loadRankHistory(),
        loadInsights()
    ]);
});

function setupPlayerTabs() {
    const buttons = document.querySelectorAll('[data-player-tab-button]');

    buttons.forEach(button => {
        button.addEventListener('click', () => {
            const tab = button.dataset.playerTabButton;

            if (!tab) {
                return;
            }

            activePlayerTab = tab;
            updatePlayerTabs();
            renderInsightsForActiveTab();
        });
    });

    updatePlayerTabs();
}

function updatePlayerTabs() {
    document.querySelectorAll('[data-player-tab-button]').forEach(button => {
        const isActive = button.dataset.playerTabButton === activePlayerTab;
        button.classList.toggle('player-tabs__button--active', isActive);
    });

    document.querySelectorAll('[data-player-tab-panel]').forEach(panel => {
        const tabs = String(panel.dataset.playerTabPanel || '').split(' ');
        panel.hidden = !tabs.includes(activePlayerTab);
    });
}

function setupMatchDetailsInline(puuid) {
    const matchesContainer = document.getElementById('playerMatchesBody');

    if (!matchesContainer) {
        return;
    }

    matchesContainer.addEventListener('click', event => {
        const targetCard = event.target.closest('[data-match-details-card]');

        if (!targetCard) {
            return;
        }

        if (event.target.closest('[data-match-details-panel]')) {
            return;
        }

        if (event.target.closest('a')) {
            return;
        }

        const targetButton = event.target.closest('[data-match-details-button]');

        if (!targetButton && event.target.closest('button')) {
            return;
        }

        const matchId = targetCard.dataset.matchId;
        const panel = targetCard.querySelector('[data-match-details-panel]');

        if (!matchId || !panel || !window.MatchDetailsView) {
            return;
        }

        event.preventDefault();
        toggleMatchDetails(targetCard, panel, matchId, puuid);
    });
}

async function toggleMatchDetails(card, panel, matchId, puuid) {
    const button = card.querySelector('[data-match-details-button]');
    const isAlreadyExpanded = expandedMatchId === matchId && card.classList.contains('player-match-card--expanded');

    if (isAlreadyExpanded) {
        collapseMatchCard(card);
        expandedMatchId = null;
        return;
    }

    collapseExpandedMatchCard(card.closest('#playerMatchesBody'));
    expandedMatchId = matchId;
    card.classList.add('player-match-card--expanded');
    if (button) {
        button.setAttribute('aria-expanded', 'true');
        button.textContent = 'Hide details';
    }
    panel.hidden = false;
    window.MatchDetailsView.renderLoadingState(panel, 'Loading match details...');

    try {
        const details = await window.MatchDetailsView.loadDetails(matchId, puuid);

        if (expandedMatchId !== matchId) {
            return;
        }

        window.MatchDetailsView.renderInto(panel, details);
    } catch (error) {
        console.error('Could not load match details:', error);

        if (expandedMatchId !== matchId) {
            return;
        }

        window.MatchDetailsView.renderErrorState(panel, 'Could not load match details.');
    }
}

function collapseExpandedMatchCard(container) {
    if (!container) {
        return;
    }

    container.querySelectorAll('.player-match-card--expanded').forEach(card => {
        collapseMatchCard(card);
    });
}

function collapseMatchCard(card) {
    card.classList.remove('player-match-card--expanded');
    const panel = card.querySelector('[data-match-details-panel]');
    const button = card.querySelector('[data-match-details-button]');

    if (panel) {
        panel.hidden = true;
        panel.innerHTML = '';
    }

    if (button) {
        button.setAttribute('aria-expanded', 'false');
        button.textContent = 'View details';
    }
}

function renderInsightsForActiveTab() {
    if (activePlayerTab === 'recommendations') {
        renderDetailedRecommendations(cachedPlayerInsights);
        return;
    }

    renderRecommendationSummary(cachedPlayerInsights);
}

function renderRecommendationSummary(insights) {
    const container = document.getElementById('playerInsightsSummary');

    if (!container) {
        return;
    }

    if (!insights || insights.length === 0) {
        container.innerHTML = `<div class="empty-box">No insights generated for this player yet.</div>`;
        return;
    }

    const visibleInsights = insights.slice(0, 3);

    container.innerHTML = `
        <div class="recommendation-summary">
            ${visibleInsights.map(insight => `
                <article class="recommendation-summary-card">
                    <span class="recommendation-summary-card__type">
                        ${getInsightIcon(insight.insightType)}
                        ${escapeHtml(formatInsightType(insight.insightType))}
                    </span>
                    <strong>${escapeHtml(insight.title || 'Recommendation')}</strong>
                </article>
            `).join('')}
        </div>
    `;
}

function renderDetailedRecommendations(insights) {
    renderPlayerInsights(insights || []);
}

async function refreshPlayerRanks(puuid) {
    const button = document.getElementById('refreshRanksButton');

    if (button) {
        button.disabled = true;
        button.textContent = 'Refreshing...';
    }

    try {
        const ranks = await api.refreshPlayerRanks(puuid);
        renderPlayerRanks(ranks || []);

        const rankHistory = await api.getPlayerRankHistory(puuid);
        renderPlayerRankChart(rankHistory || []);
        renderPlayerRankHistory(rankHistory || []);
    } catch (error) {
        console.error('Could not refresh player ranks:', error);
    } finally {
        if (button) {
            button.disabled = false;
            button.textContent = 'Refresh rank';
        }
    }
}

function showPlayerError(message) {
    const hero = document.getElementById('playerHero');

    if (!hero) {
        return;
    }

    hero.innerHTML = `<div class="error-box">${escapeHtml(message)}</div>`;
}

function renderPlayerHero(player) {
    const hero = document.getElementById('playerHero');

    if (!hero) {
        return;
    }

    const playerName = player.gameName || 'Unknown';
    const tagLine = player.tagLine ? `#${player.tagLine}` : '';
    const iconUrl = getProfileIconUrl(player.profileIconId);

    const iconMarkup = iconUrl
        ? `
            <img
                class="player-hero__icon"
                src="${escapeHtml(iconUrl)}"
                alt="${escapeHtml(playerName)} profile icon"
                onerror="this.onerror=null; this.remove();"
            >
        `
        : `
            <div class="player-hero__icon player-hero__icon--placeholder">
                ${escapeHtml(getPlayerInitials(playerName))}
            </div>
        `;

    hero.innerHTML = `
        <div class="player-hero">
            ${iconMarkup}

            <div class="player-hero__content">
                <h1 class="hero__title">${escapeHtml(playerName)}${escapeHtml(tagLine)}</h1>
                <p class="hero__text">
                    Player profile based on analyzed Riot API matches, ranked data and generated recommendations.
                </p>
            </div>
        </div>
    `;
}

function renderPlayerStats(player) {
    const container = document.getElementById('playerStats');

    if (!container) {
        return;
    }

    container.innerHTML = `
        <article class="stat-card">
            <span class="stat-card__label">Matches</span>
            <strong class="stat-card__value">${formatNumber(player.matches)}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Winrate</span>
            <strong class="stat-card__value">${formatPercent(player.winrate)}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Avg KDA</span>
            <strong class="stat-card__value">
                ${formatDecimal(player.averageKills)}/${formatDecimal(player.averageDeaths)}/${formatDecimal(player.averageAssists)}
            </strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Avg Damage</span>
            <strong class="stat-card__value">${formatNumber(player.averageDamageToChampions)}</strong>
        </article>
    `;
}

function renderPlayerChampions(champions) {
    const container = document.getElementById('playerChampions');

    if (!container) {
        return;
    }

    if (!champions || champions.length === 0) {
        container.innerHTML = `<div class="empty-box">No champion statistics for this player yet.</div>`;
        return;
    }

    container.innerHTML = champions.map(champion => `
        <a class="player-champion-card" href="champion.html?id=${encodeURIComponent(champion.championId)}">
            ${
        champion.imageUrl
            ? `<img
                        class="player-champion-card__image"
                        src="${escapeHtml(champion.imageUrl)}"
                        alt="${escapeHtml(champion.championName || 'Champion')}"
                        onerror="this.onerror=null; this.remove();"
                    >`
            : ''
    }

            <div class="player-champion-card__main">
                <strong class="player-champion-card__name">${escapeHtml(champion.championName || 'Unknown')}</strong>
                <span class="player-champion-card__meta">
                    ${formatNumber(champion.games)} games · ${formatPercent(champion.winrate)} WR
                </span>
            </div>

            <div class="player-champion-card__kda">
                Avg KDA:
                ${formatDecimal(champion.averageKills)}/${formatDecimal(champion.averageDeaths)}/${formatDecimal(champion.averageAssists)}
            </div>
        </a>
    `).join('');
}

function renderPlayerRanks(ranks) {
    const container = document.getElementById('playerRanks');

    if (!container) {
        return;
    }

    const soloRank = findRank(ranks, 'RANKED_SOLO_5x5');
    const flexRank = findRank(ranks, 'RANKED_FLEX_SR');

    container.innerHTML = `
        ${renderRankCard('Solo/Duo', soloRank)}
        ${renderRankCard('Flex', flexRank)}
    `;
}

function renderRankCard(title, rank) {
    if (!rank) {
        return `
            <article class="rank-card rank-card--unranked">
                <div class="rank-card__content">
                    <div class="rank-card__header">
                        <span class="rank-card__queue">${escapeHtml(title)}</span>
                        <span class="rank-card__badge">Unranked</span>
                    </div>

                    <strong class="rank-card__rank">Unranked</strong>
                    <p class="rank-card__meta">No ranked data for this queue yet.</p>
                </div>
            </article>
        `;
    }

    const games = (rank.wins || 0) + (rank.losses || 0);
    const winrate = games > 0 ? (rank.wins * 100 / games) : 0;
    const tier = rank.tier || 'unknown';
    const rankImageUrl = getRankImageUrl(tier);

    return `
        <article class="rank-card rank-card--${escapeHtml(tier.toLowerCase())}">
            ${
        rankImageUrl
            ? `<img
                        class="rank-card__image"
                        src="${escapeHtml(rankImageUrl)}"
                        alt="${escapeHtml(tier)} rank"
                        onerror="this.onerror=null; this.remove();"
                    >`
            : ''
    }

            <div class="rank-card__content">
                <div class="rank-card__header">
                    <span class="rank-card__queue">${escapeHtml(title)}</span>
                    <span class="rank-card__badge">${escapeHtml(formatRankTier(rank))}</span>
                </div>

                <strong class="rank-card__rank">${escapeHtml(formatRank(rank))}</strong>

                <div class="rank-card__lp">${formatNumber(rank.leaguePoints)} LP</div>

                <div class="rank-card__stats">
                    <span>${formatNumber(rank.wins)}W</span>
                    <span>${formatNumber(rank.losses)}L</span>
                    <span>${formatPercent(winrate)}</span>
                </div>

                <div class="rank-card__flags">
                    ${rank.hotStreak ? '<span class="rank-flag">Hot streak</span>' : ''}
                    ${rank.veteran ? '<span class="rank-flag">Veteran</span>' : ''}
                    ${rank.freshBlood ? '<span class="rank-flag">Fresh blood</span>' : ''}
                    ${rank.inactive ? '<span class="rank-flag rank-flag--muted">Inactive</span>' : ''}
                </div>
            </div>
        </article>
    `;
}

function renderPlayerRankChart(history) {
    const container = document.getElementById('playerRankChart');

    if (!container) {
        return;
    }

    if (!history || history.length < 2) {
        container.innerHTML = `<div class="empty-box">Not enough rank history to build a chart yet.</div>`;
        return;
    }

    const grouped = groupHistoryByQueue(history);
    const preferredQueue = grouped.RANKED_SOLO_5x5 ? 'RANKED_SOLO_5x5' : Object.keys(grouped)[0];
    const entries = [...grouped[preferredQueue]]
        .sort((a, b) => new Date(a.syncedAt) - new Date(b.syncedAt))
        .slice(-12);

    if (entries.length < 2) {
        container.innerHTML = `<div class="empty-box">Not enough rank history to build a chart yet.</div>`;
        return;
    }

    const points = entries.map((entry, index) => ({
        index,
        label: formatRank(entry),
        date: formatDateTime(entry.syncedAt),
        lp: entry.leaguePoints ?? 0,
        score: getRankScore(entry)
    }));

    const width = 900;
    const height = 260;
    const padding = 42;

    const minScore = Math.min(...points.map(point => point.score));
    const maxScore = Math.max(...points.map(point => point.score));
    const scoreRange = Math.max(maxScore - minScore, 1);
    const xStep = points.length === 1 ? 0 : (width - padding * 2) / (points.length - 1);

    const chartPoints = points.map((point, index) => {
        const x = padding + index * xStep;
        const y = height - padding - ((point.score - minScore) / scoreRange) * (height - padding * 2);

        return {
            ...point,
            x,
            y
        };
    });

    const polyline = chartPoints
        .map(point => `${point.x},${point.y}`)
        .join(' ');

    const first = chartPoints[0];
    const last = chartPoints[chartPoints.length - 1];
    const totalChange = last.score - first.score;

    container.innerHTML = `
        <article class="rank-chart-card">
            <div class="rank-chart-card__header">
                <div>
                    <span class="rank-chart-card__eyebrow">${escapeHtml(formatQueueName(preferredQueue))}</span>
                    <h3 class="rank-chart-card__title">${escapeHtml(first.label)} → ${escapeHtml(last.label)}</h3>
                </div>

                <div class="${totalChange >= 0 ? 'rank-chart-card__delta rank-chart-card__delta--up' : 'rank-chart-card__delta rank-chart-card__delta--down'}">
                    ${formatSignedNumber(totalChange)} rank score
                </div>
            </div>

            <div class="rank-chart-card__body">
                <svg class="rank-chart" viewBox="0 0 ${width} ${height}" role="img" aria-label="Rank history chart">
                    <line class="rank-chart__grid" x1="${padding}" y1="${padding}" x2="${width - padding}" y2="${padding}"></line>
                    <line class="rank-chart__grid" x1="${padding}" y1="${height / 2}" x2="${width - padding}" y2="${height / 2}"></line>
                    <line class="rank-chart__grid" x1="${padding}" y1="${height - padding}" x2="${width - padding}" y2="${height - padding}"></line>

                    <polyline class="rank-chart__line" points="${polyline}"></polyline>

                    ${chartPoints.map(point => `
                        <g
                            class="rank-chart__point-group"
                            data-rank="${escapeHtml(point.label)}"
                            data-lp="${formatNumber(point.lp)} LP"
                            data-date="${escapeHtml(point.date)}"
                            data-score="${formatNumber(point.score)} rank score"
                        >
                            <circle class="rank-chart__point" cx="${point.x}" cy="${point.y}" r="7"></circle>
                        </g>
                    `).join('')}
                </svg>
            </div>

            <div class="rank-chart-card__labels">
                ${chartPoints.map(point => `
                    <div class="rank-chart-label">
                        <strong>${escapeHtml(point.label)}</strong>
                        <span>${formatNumber(point.lp)} LP</span>
                    </div>
                `).join('')}
            </div>
        </article>
    `;

    bindRankChartTooltip(container);
}

function bindRankChartTooltip(container) {
    const points = container.querySelectorAll('.rank-chart__point-group');

    points.forEach(point => {
        point.addEventListener('mouseenter', event => {
            const tooltip = getOrCreateRankTooltip();

            tooltip.innerHTML = `
                <strong>${point.dataset.rank || 'Unknown rank'}</strong>
                <span>${point.dataset.lp || ''}</span>
                <small>${point.dataset.date || ''}</small>
            `;

            tooltip.classList.add('rank-chart-tooltip--visible');
            moveRankTooltip(event, tooltip);
        });

        point.addEventListener('mousemove', event => {
            const tooltip = getOrCreateRankTooltip();
            moveRankTooltip(event, tooltip);
        });

        point.addEventListener('mouseleave', () => {
            const tooltip = getOrCreateRankTooltip();
            tooltip.classList.remove('rank-chart-tooltip--visible');
        });
    });
}

function getOrCreateRankTooltip() {
    let tooltip = document.getElementById('rankChartTooltip');

    if (!tooltip) {
        tooltip = document.createElement('div');
        tooltip.id = 'rankChartTooltip';
        tooltip.className = 'rank-chart-tooltip';
        document.body.appendChild(tooltip);
    }

    return tooltip;
}

function moveRankTooltip(event, tooltip) {
    tooltip.style.left = `${event.clientX + 14}px`;
    tooltip.style.top = `${event.clientY + 14}px`;
}

function renderPlayerRankHistory(history) {
    const container = document.getElementById('playerRankHistory');

    if (!container) {
        return;
    }

    if (!history || history.length === 0) {
        container.innerHTML = `<div class="empty-box">No rank history recorded yet.</div>`;
        return;
    }

    const grouped = groupHistoryByQueue(history);
    const blocks = Object.entries(grouped)
        .map(([queueType, entries]) => renderQueueHistory(queueType, entries))
        .join('');

    container.innerHTML = blocks || `<div class="empty-box">No rank history recorded yet.</div>`;
}

function renderQueueHistory(queueType, entries) {
    const sortedEntries = [...entries]
        .sort((a, b) => new Date(b.syncedAt) - new Date(a.syncedAt));

    const visibleEntries = sortedEntries.slice(0, 6);

    return `
        <article class="rank-history-card">
            <div class="rank-history-card__header">
                <h3 class="rank-history-card__title">${escapeHtml(formatQueueName(queueType))}</h3>
                <span class="rank-history-card__count">${entries.length} records</span>
            </div>

            <div class="rank-history-list">
                ${visibleEntries.map((entry, index) => renderHistoryEntry(entry, visibleEntries[index + 1])).join('')}
            </div>
        </article>
    `;
}

function renderHistoryEntry(current, previous) {
    const lpChange = previous ? calculateLpChange(current, previous) : null;
    const gamesChange = previous ? calculateGamesChange(current, previous) : null;

    return `
        <div class="rank-history-item">
            <div>
                <strong class="rank-history-item__rank">${escapeHtml(formatRank(current))}</strong>
                <div class="rank-history-item__date">${escapeHtml(formatDateTime(current.syncedAt))}</div>
            </div>

            <div class="rank-history-item__meta">
                <span>${formatNumber(current.leaguePoints)} LP</span>
                ${lpChange !== null ? `<span class="${lpChange >= 0 ? 'rank-delta rank-delta--up' : 'rank-delta rank-delta--down'}">${formatSignedNumber(lpChange)} LP</span>` : ''}
                ${gamesChange ? `<span>${escapeHtml(gamesChange)}</span>` : ''}
            </div>
        </div>
    `;
}

function renderPlayerInsights(insights) {
    const container = document.getElementById('playerInsights');

    if (!container) {
        return;
    }

    if (!insights || insights.length === 0) {
        container.innerHTML = `<div class="empty-box">No insights generated for this player yet.</div>`;
        return;
    }

    container.innerHTML = insights.map(insight => `
        <article class="insight-card">
            <div class="insight-card__header">
                <span class="insight-card__type">
                    ${getInsightIcon(insight.insightType)}
                    ${escapeHtml(formatInsightType(insight.insightType))}
                </span>
                ${insight.score !== null && insight.score !== undefined
        ? `<span class="insight-card__score">${formatDecimal(insight.score)}</span>`
        : ''}
            </div>
            <h3 class="insight-card__title">${escapeHtml(insight.title || 'Recommendation')}</h3>
            <p class="insight-card__text">${escapeHtml(insight.description || '')}</p>
        </article>
    `).join('');
}

function renderPlayerMatches(matches) {
    const container = document.getElementById('playerMatchesBody');

    if (!container) {
        return;
    }

    if (!matches || matches.length === 0) {
        container.innerHTML = `<div class="empty-box">No recent matches found.</div>`;
        return;
    }

    container.innerHTML = matches.map(match => {
        const championId = getValue(match, 'championId', 'champion_id');
        const championName = getValue(match, 'championName', 'champion_name') || 'Unknown';
        const championImageUrl = getValue(match, 'championImageUrl', 'champion_image_url');
        const queueId = getValue(match, 'queueId', 'queue_id');
        const gameVersion = getValue(match, 'gameVersion', 'game_version');
        const gameDurationMs = getValue(match, 'gameDurationMs', 'game_duration_ms');
        const gameDurationSeconds = getValue(match, 'gameDurationSeconds', 'game_duration_seconds');
        const matchId = getValue(match, 'matchId', 'match_id');
        const finalItems = getValue(match, 'finalItems', 'final_items') || [];
        const matchResultClass = match.win ? 'player-match-card--win' : 'player-match-card--loss';
        return `
            <article class="player-match-card ${matchResultClass}"
                     data-match-details-card
                     data-match-id="${escapeHtml(matchId)}">
                <div class="player-match-card__top">
                    <div class="player-match-card__identity">
                        <span class="${match.win ? 'result result--win' : 'result result--loss'}">
                            ${match.win ? 'Win' : 'Loss'}
                        </span>
                        <a class="match-champion-link" href="champion.html?id=${encodeURIComponent(championId)}">
                            ${
            championImageUrl
                ? `<img
                                        class="match-champion-link__image"
                                        src="${escapeHtml(championImageUrl)}"
                                        alt="${escapeHtml(championName)}"
                                        onerror="this.onerror=null; this.remove();"
                                    >`
                : ''
        }
                            <span>${escapeHtml(championName)}</span>
                        </a>
                    </div>
                    <button class="button button--secondary player-match-card__button"
                            data-match-details-button type="button" aria-expanded="false">
                        View details
                    </button>
                </div>

                <div class="player-match-card__meta">
                    <span class="player-match-card__meta-item">
                        <span class="player-match-card__label">KDA</span>
                        <strong>${formatKda(match.kills, match.deaths, match.assists)}</strong>
                    </span>
                    <span class="player-match-card__meta-item">
                        <span class="player-match-card__label">Queue</span>
                        <strong>${escapeHtml(formatQueue(queueId))}</strong>
                    </span>
                    <span class="player-match-card__meta-item">
                        <span class="player-match-card__label">Patch</span>
                        <strong>${escapeHtml(formatPatchVersion(gameVersion))}</strong>
                    </span>
                    <span class="player-match-card__meta-item">
                        <span class="player-match-card__label">Duration</span>
                        <strong>${formatMatchDuration(gameDurationMs, gameDurationSeconds)}</strong>
                    </span>
                </div>

                <div class="player-match-card__items">
                    <span class="player-match-card__label">Final items</span>
                    <div class="player-match-items">
                        ${
            finalItems.length > 0
                ? finalItems.map(item => renderMatchItem(item)).join('')
                : `<span class="player-match-items__empty">No final items recorded.</span>`
        }
                    </div>
                </div>

                <div class="match-details-inline" data-match-details-panel hidden></div>
            </article>
        `;
    }).join('');
}

function renderMatchItem(item) {
    const itemId = getValue(item, 'itemId', 'item_id');
    const itemName = getValue(item, 'itemName', 'item_name') || `Item ${itemId}`;
    const imageUrl = getValue(item, 'imageUrl', 'image_url');

    if (!imageUrl) {
        return `
            <span class="player-match-item player-match-item--text" title="${escapeHtml(itemName)}">
                <span class="player-match-item__fallback">${escapeHtml(itemName)}</span>
            </span>
        `;
    }

    return `
        <span class="player-match-item" title="${escapeHtml(itemName)}">
            <img
                class="player-match-item__image"
                src="${escapeHtml(imageUrl)}"
                alt="${escapeHtml(itemName)}"
                onerror="this.onerror=null; this.remove();"
            >
        </span>
    `;
}

function getValue(object, camelCaseKey, snakeCaseKey) {
    if (!object) {
        return null;
    }

    if (object[camelCaseKey] !== undefined && object[camelCaseKey] !== null) {
        return object[camelCaseKey];
    }

    if (object[snakeCaseKey] !== undefined && object[snakeCaseKey] !== null) {
        return object[snakeCaseKey];
    }

    return null;
}

function findRank(ranks, queueType) {
    return ranks.find(rank => rank.queueType === queueType);
}

function groupHistoryByQueue(history) {
    return history.reduce((groups, entry) => {
        const queueType = entry.queueType || 'UNKNOWN';

        if (!groups[queueType]) {
            groups[queueType] = [];
        }

        groups[queueType].push(entry);
        return groups;
    }, {});
}

function formatQueueName(queueType) {
    if (queueType === 'RANKED_SOLO_5x5') {
        return 'Solo/Duo';
    }

    if (queueType === 'RANKED_FLEX_SR') {
        return 'Flex';
    }

    return queueType || 'Unknown queue';
}

function formatQueue(queueId) {
    const queues = {
        400: 'Normal Draft',
        420: 'Ranked Solo/Duo',
        430: 'Normal Blind',
        440: 'Ranked Flex',
        450: 'ARAM',
        700: 'Clash',
        720: 'ARAM Clash',
        830: 'Co-op Intro',
        840: 'Co-op Beginner',
        850: 'Co-op Intermediate',
        900: 'URF',
        1020: 'One for All',
        1300: 'Nexus Blitz',
        1400: 'Ultimate Spellbook',
        1700: 'Arena',
        1710: 'Arena'
    };

    if (queueId === null || queueId === undefined || queueId === '') {
        return '-';
    }

    const normalized = Number(queueId);

    if (!Number.isFinite(normalized) || normalized <= 0) {
        return '-';
    }

    return queues[normalized] || `Queue ${normalized}`;
}

function formatPatchVersion(gameVersion) {
    if (!gameVersion) {
        return '-';
    }

    return String(gameVersion).split('.').slice(0, 2).join('.');
}

function formatMatchDuration(gameDurationMs, gameDurationSeconds) {
    if (gameDurationMs) {
        return formatDuration(gameDurationMs);
    }

    if (gameDurationSeconds) {
        return formatDuration(Number(gameDurationSeconds) * 1000);
    }

    return '-';
}

function formatRank(rank) {
    if (!rank || !rank.tier) {
        return 'Unranked';
    }

    return `${rank.tier} ${rank.rankValue || ''}`.trim();
}

function formatRankTier(rank) {
    if (!rank || !rank.tier) {
        return 'Unranked';
    }

    return rank.tier;
}

function getRankImageUrl(tier) {
    const allowed = [
        'iron',
        'bronze',
        'silver',
        'gold',
        'platinum',
        'emerald',
        'diamond',
        'master',
        'grandmaster',
        'challenger'
    ];

    const normalized = String(tier || '').toLowerCase();

    if (!allowed.includes(normalized)) {
        return null;
    }

    return `img/ranks/${normalized}.png`;
}

function getRankScore(entry) {
    const tierScore = {
        IRON: 0,
        BRONZE: 400,
        SILVER: 800,
        GOLD: 1200,
        PLATINUM: 1600,
        EMERALD: 2000,
        DIAMOND: 2400,
        MASTER: 2800,
        GRANDMASTER: 3200,
        CHALLENGER: 3600
    };

    const divisionScore = {
        IV: 0,
        III: 100,
        II: 200,
        I: 300
    };

    const tier = String(entry.tier || '').toUpperCase();
    const rankValue = String(entry.rankValue || '').toUpperCase();

    return (tierScore[tier] ?? 0)
        + (divisionScore[rankValue] ?? 0)
        + Number(entry.leaguePoints ?? 0);
}

function getProfileIconUrl(profileIconId) {
    if (!profileIconId) {
        return null;
    }

    return `https://ddragon.leagueoflegends.com/cdn/15.10.1/img/profileicon/${profileIconId}.png`;
}

function getPlayerInitials(playerName) {
    if (!playerName) {
        return '?';
    }

    return String(playerName).trim().charAt(0).toUpperCase() || '?';
}

function calculateLpChange(current, previous) {
    const currentLp = Number(current.leaguePoints ?? 0);
    const previousLp = Number(previous.leaguePoints ?? 0);

    if (current.tier !== previous.tier || current.rankValue !== previous.rankValue) {
        return null;
    }

    return currentLp - previousLp;
}

function calculateGamesChange(current, previous) {
    const winsChange = Number(current.wins ?? 0) - Number(previous.wins ?? 0);
    const lossesChange = Number(current.losses ?? 0) - Number(previous.losses ?? 0);

    if (winsChange === 0 && lossesChange === 0) {
        return null;
    }

    return `${formatSignedNumber(winsChange)}W / ${formatSignedNumber(lossesChange)}L`;
}

function formatInsightType(type) {
    const labels = {
        VISION_WEAKNESS: 'Vision control',
        KDA_WEAKNESS: 'Fight survival',
        FARM_WEAKNESS: 'Farming',
        GOLD_WEAKNESS: 'Gold efficiency',
        ITEM_BUILD_WEAKNESS: 'Item build',
        ITEM_TIMING_WEAKNESS: 'Item timing',
        RUNE_WEAKNESS: 'Runes',
        SKILL_ORDER_WEAKNESS: 'Skill order',
        CHAMPION_WEAKNESS: 'Champion performance',
        DEATHS_WEAKNESS: 'Death control'
    };

    return labels[type] || prettifyInsightType(type);
}

function prettifyInsightType(type) {
    if (!type) {
        return 'Insight';
    }

    return String(type)
        .toLowerCase()
        .split('_')
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ');
}

function getInsightIcon(type) {
    const icons = {
        VISION_WEAKNESS: '👁️',
        KDA_WEAKNESS: '⚔️',
        FARM_WEAKNESS: '🌾',
        GOLD_WEAKNESS: '🪙',
        ITEM_BUILD_WEAKNESS: '🛡️',
        ITEM_TIMING_WEAKNESS: '⏱️',
        RUNE_WEAKNESS: '✨',
        SKILL_ORDER_WEAKNESS: '⬆️',
        CHAMPION_WEAKNESS: '🎯',
        DEATHS_WEAKNESS: '💀'
    };

    return icons[type] || '💡';
}

function formatSignedNumber(value) {
    const number = Number(value);

    if (number > 0) {
        return `+${number}`;
    }

    return String(number);
}

function formatDateTime(value) {
    if (!value) {
        return '-';
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return '-';
    }

    return date.toLocaleString('en-US', {
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatKda(kills, deaths, assists) {
    return `${kills ?? 0}/${deaths ?? 0}/${assists ?? 0}`;
}

function formatDecimal(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return '0.00';
    }

    return Number(value).toFixed(2);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
