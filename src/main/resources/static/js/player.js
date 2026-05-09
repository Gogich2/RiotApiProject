document.addEventListener('DOMContentLoaded', async () => {
    const puuid = getQueryParam('puuid');

    if (!puuid) {
        showPlayerError('Player PUUID is missing.');
        return;
    }

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
        return;
    }

    try {
        const insights = await api.getPlayerInsights(puuid);
        renderPlayerInsights(insights || []);
    } catch (error) {
        console.error('Could not load insights:', error);
        renderPlayerInsights([]);
    }
});

function showPlayerError(message) {
    const hero = document.getElementById('playerHero');

    if (!hero) {
        return;
    }

    hero.innerHTML = `<div class="error-box">${message}</div>`;
}

function renderPlayerHero(player) {
    const hero = document.getElementById('playerHero');

    if (!hero) {
        return;
    }

    const playerName = player.gameName || 'Unknown';
    const tagLine = player.tagLine ? `#${player.tagLine}` : '';

    hero.innerHTML = `
        <h1 class="hero__title">${escapeHtml(playerName)}${escapeHtml(tagLine)}</h1>
        <p class="hero__text">
            Player profile based on analyzed Riot API matches.
        </p>
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
                <span class="insight-card__type">${escapeHtml(insight.insightType || 'Insight')}</span>
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
    const body = document.getElementById('playerMatchesBody');

    if (!body) {
        return;
    }

    if (!matches || matches.length === 0) {
        body.innerHTML = `
            <tr>
                <td colspan="6">No recent matches found.</td>
            </tr>
        `;
        return;
    }

    body.innerHTML = matches.map(match => `
        <tr>
            <td>
                <a href="/champion.html?id=${encodeURIComponent(match.championId)}">
                    ${escapeHtml(match.championName || 'Unknown')}
                </a>
            </td>
            <td>
                <span class="${match.win ? 'result result--win' : 'result result--loss'}">
                    ${match.win ? 'Win' : 'Loss'}
                </span>
            </td>
            <td>${formatKda(match.kills, match.deaths, match.assists)}</td>
            <td>${match.queueId || '-'}</td>
            <td>${escapeHtml(match.gameVersion || '-')}</td>
            <td>${formatDuration(match.gameDurationMs)}</td>
        </tr>
    `).join('');
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