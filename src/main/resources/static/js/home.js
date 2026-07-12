document.addEventListener('DOMContentLoaded', async () => {
    let overview = null;
    let leaderboards = null;

    try {
        [overview, leaderboards] = await Promise.all([
            api.getOverview(),
            api.getPlayerLeaderboards().catch(() => null)
        ]);

        renderOverviewStats(overview);
        renderMetaSnapshot(overview, leaderboards);
        renderChampionTable('popularChampionsBody', overview.mostPopularChampions || []);
        renderChampionTable('bestChampionsBody', overview.bestWinrateChampions || []);
        renderCoverage(overview);
        renderLeaderboardSpotlight(leaderboards);
        renderHomePlayers('homeTopPlayers', leaderboards?.bestPlayers || [], 'winrate');
        renderHomePlayers('homeMostActivePlayers', leaderboards?.mostActivePlayers || [], 'matches');
    } catch (error) {
        console.error('Could not load overview stats:', error);

        document.getElementById('overviewStats').innerHTML = `
            <div class="error-box">Could not load overview stats.</div>
        `;

        renderMetaSnapshot(null, null);
        renderLeaderboardSpotlight(null);
        renderHomePlayers('homeTopPlayers', [], 'winrate');
        renderHomePlayers('homeMostActivePlayers', [], 'matches');
    }
});

function renderMetaSnapshot(overview, leaderboards) {
    const container = document.getElementById('metaSnapshot');

    if (!container) {
        return;
    }

    container.innerHTML = buildMetaSnapshotCards(overview, leaderboards);
}

function buildMetaSnapshotCards(overview, leaderboards) {
    const bestChampion = overview?.bestWinrateChampions?.[0];
    const mostPlayedChampion = overview?.mostPopularChampions?.[0];
    const hottestPlayer = leaderboards?.bestPlayers?.[0] || leaderboards?.mostActivePlayers?.[0];

    return [
        buildMetaCard(
            'Best win rate',
            bestChampion?.championName || 'Unavailable',
            bestChampion ? `${formatPercent(bestChampion.winrate)} win rate` : 'No champion data'
        ),
        buildMetaCard(
            'Most played',
            mostPlayedChampion?.championName || 'Unavailable',
            mostPlayedChampion ? `${formatNumber(mostPlayedChampion.games)} games` : 'No champion data'
        ),
        buildMetaCard(
            'Hot player',
            getPlayerDisplayName(hottestPlayer),
            hottestPlayer ? `${formatNumber(hottestPlayer.matches)} matches` : 'No player data'
        )
    ].join('');
}

function buildMetaCard(label, title, meta) {
    return `
        <article class="meta-snapshot__card">
            <span class="meta-snapshot__label">${escapeHtml(label)}</span>
            <strong class="meta-snapshot__title">${escapeHtml(title || 'Unavailable')}</strong>
            <p class="meta-snapshot__meta">${escapeHtml(meta)}</p>
        </article>
    `;
}

function renderOverviewStats(overview) {
    const container = document.getElementById('overviewStats');

    if (!container) {
        return;
    }

    container.innerHTML = `
        <article class="stat-card">
            <span class="stat-card__label">Analyzed matches</span>
            <strong class="stat-card__value">${formatNumber(overview.totalMatches)}</strong>
            <span class="stat-card__meta">Ranked sample across the current local dataset.</span>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Tracked players</span>
            <strong class="stat-card__value">${formatNumber(overview.totalPlayers)}</strong>
            <span class="stat-card__meta">Profiles available for scouting and match review.</span>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Champion entries</span>
            <strong class="stat-card__value">${formatNumber(overview.totalParticipants)}</strong>
            <span class="stat-card__meta">Champion-level samples rolled into the meta view.</span>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Primary queue</span>
            <strong class="stat-card__value">Ranked</strong>
            <span class="stat-card__meta">Optimized for stable comparison, not casual noise.</span>
        </article>
    `;
}

function renderCoverage(overview) {
    const element = document.getElementById('homeCoverageValue');

    if (!element || !overview) {
        return;
    }

    element.textContent = `${formatNumber(overview.totalPlayers)} players / ${formatNumber(overview.totalMatches)} matches`;
}

function renderChampionTable(elementId, champions) {
    const body = document.getElementById(elementId);

    if (!body) {
        return;
    }

    if (!champions || champions.length === 0) {
        body.innerHTML = `
            <tr>
                <td colspan="4">No champion statistics found.</td>
            </tr>
        `;
        return;
    }

    body.innerHTML = champions.map(champion => `
        <tr>
            <td>
                <a class="home-champion-link" href="champion.html?id=${encodeURIComponent(champion.championId)}">
                    ${
        champion.imageUrl
            ? `<img
                                class="home-champion-link__image"
                                src="${escapeHtml(champion.imageUrl)}"
                                alt="${escapeHtml(champion.championName || 'Champion')}"
                                onerror="this.onerror=null; this.remove();"
                            >`
            : ''
    }
                    <span>${escapeHtml(champion.championName || 'Unknown')}</span>
                </a>
            </td>
            <td>${formatNumber(champion.games)}</td>
            <td>${formatNumber(champion.wins)}</td>
            <td>
                <span class="winrate-pill">${formatPercent(champion.winrate)}</span>
            </td>
        </tr>
    `).join('');
}

function renderLeaderboardSpotlight(leaderboards) {
    const container = document.getElementById('leaderboardSpotlight');

    if (!container) {
        return;
    }

    const bestPlayer = leaderboards?.bestPlayers?.[0];
    const activePlayer = leaderboards?.mostActivePlayers?.[0];

    if (!bestPlayer && !activePlayer) {
        container.innerHTML = `
            <span class="hero-summary-card__label">Leaderboard</span>
            <strong class="hero-summary-card__value">Player signals unavailable</strong>
            <p class="hero-summary-card__text">The overview loaded, but player leaderboard data is not available right now.</p>
        `;
        return;
    }

    container.innerHTML = `
        <span class="hero-summary-card__label">Leaderboard</span>
        <strong class="hero-summary-card__value">${escapeHtml(getPlayerDisplayName(bestPlayer || activePlayer))}</strong>
        <p class="hero-summary-card__text">
            ${bestPlayer ? `${formatPercent(bestPlayer.winrate)} win rate leader` : 'Top player spotlight'}
            ${activePlayer ? ` and ${formatNumber(activePlayer.matches)} analyzed matches on the activity board.` : '.'}
        </p>
    `;
}

function renderHomePlayers(elementId, players, valueType) {
    const container = document.getElementById(elementId);

    if (!container) {
        return;
    }

    if (!players || players.length === 0) {
        container.innerHTML = `<div class="empty-box">No player data available.</div>`;
        return;
    }

    container.innerHTML = players.slice(0, 4).map(player => `
        <a class="home-player-card" href="player.html?puuid=${encodeURIComponent(player.puuid)}">
            ${player.profileIconUrl ? `<img
                class="home-player-card__image"
                src="${escapeHtml(player.profileIconUrl)}"
                alt="${escapeHtml(getPlayerDisplayName(player))}"
                onerror="this.onerror=null; this.remove();"
            >` : '<div class="home-player-card__image"></div>'}
            <span class="home-player-card__identity">
                <strong class="home-player-card__name">${escapeHtml(getPlayerDisplayName(player))}</strong>
                <span class="home-player-card__meta">${formatNumber(player.matches)} matches · ${formatKda(player)}</span>
            </span>
            <span class="home-player-card__value">${formatHomeValue(player, valueType)}</span>
        </a>
    `).join('');
}

function formatHomeValue(player, valueType) {
    if (valueType === 'matches') {
        return `${formatNumber(player.matches)} games`;
    }

    return formatPercent(player.winrate);
}

function getPlayerDisplayName(player) {
    if (!player) {
        return 'Unknown';
    }

    const gameName = player.gameName || 'Unknown';
    return player.tagLine ? `${gameName}#${player.tagLine}` : gameName;
}

function formatKda(player) {
    const kills = Number(player.averageKills || 0).toFixed(1);
    const deaths = Number(player.averageDeaths || 0).toFixed(1);
    const assists = Number(player.averageAssists || 0).toFixed(1);

    return `${kills} / ${deaths} / ${assists}`;
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
