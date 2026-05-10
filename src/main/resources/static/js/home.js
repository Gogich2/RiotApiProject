document.addEventListener('DOMContentLoaded', async () => {
    try {
        const overview = await api.getOverview();

        renderOverviewStats(overview);
        renderChampionTable('popularChampionsBody', overview.mostPopularChampions || []);
        renderChampionTable('bestChampionsBody', overview.bestWinrateChampions || []);
    } catch (error) {
        document.getElementById('overviewStats').innerHTML = `
            <div class="error-box">Could not load overview stats.</div>
        `;
    }
});

function renderOverviewStats(overview) {
    document.getElementById('overviewStats').innerHTML = `
        <article class="stat-card">
            <span class="stat-card__label">Matches</span>
            <strong class="stat-card__value">${formatNumber(overview.totalMatches)}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Players</span>
            <strong class="stat-card__value">${formatNumber(overview.totalPlayers)}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Participants</span>
            <strong class="stat-card__value">${formatNumber(overview.totalParticipants)}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Avg. duration</span>
            <strong class="stat-card__value">${overview.averageMatchDurationMinutes || 0} min</strong>
        </article>
    `;
}

function renderChampionTable(elementId, champions) {
    const body = document.getElementById(elementId);

    body.innerHTML = champions.map(champion => `
        <tr>
            <td>
                <a href="/champion.html?id=${champion.championId}">
                    ${champion.championName}
                </a>
            </td>
            <td>${formatNumber(champion.games)}</td>
            <td>${formatNumber(champion.wins)}</td>
            <td>${formatPercent(champion.winrate)}</td>
        </tr>
    `).join('');
}