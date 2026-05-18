document.addEventListener('DOMContentLoaded', async () => {
    try {
        const overview = await api.getOverview();

        renderOverviewStats(overview);
        renderChampionTable('popularChampionsBody', overview.mostPopularChampions || []);
        renderChampionTable('bestChampionsBody', overview.bestWinrateChampions || []);
    } catch (error) {
        console.error('Could not load overview stats:', error);

        document.getElementById('overviewStats').innerHTML = `
            <div class="error-box">Could not load overview stats.</div>
        `;
    }
});

function renderOverviewStats(overview) {
    document.getElementById('overviewStats').innerHTML = `
        <article class="stat-card">
            <span class="stat-card__label">Analyzed matches</span>
            <strong class="stat-card__value">${formatNumber(overview.totalMatches)}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Tracked players</span>
            <strong class="stat-card__value">${formatNumber(overview.totalPlayers)}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Champions analyzed</span>
            <strong class="stat-card__value">${formatNumber(overview.totalParticipants)}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Analysis focus</span>
            <strong class="stat-card__value">Ranked</strong>
        </article>
    `;
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
                <a class="home-champion-link" href="/champion.html?id=${encodeURIComponent(champion.championId)}">
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

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}