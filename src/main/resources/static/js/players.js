document.addEventListener('DOMContentLoaded', async () => {
    try {
        const leaderboards = await api.getPlayerLeaderboards();

        renderLeaderboardTable('bestPlayersBody', leaderboards.bestPlayers || []);
        renderLeaderboardTable('mostActivePlayersBody', leaderboards.mostActivePlayers || []);
    } catch (error) {
        renderLeaderboardError('bestPlayersBody', 'Could not load best players.');
        renderLeaderboardError('mostActivePlayersBody', 'Could not load active players.');
    }
});

function renderLeaderboardTable(elementId, players) {
    const body = document.getElementById(elementId);

    if (!body) {
        return;
    }

    if (!players || players.length === 0) {
        body.innerHTML = `
            <tr>
                <td colspan="5">
                    <div class="empty-box">No players found.</div>
                </td>
            </tr>
        `;
        return;
    }

    body.innerHTML = players.map(player => `
        <tr>
            <td>
                <a class="player-leaderboard-link" href="player.html?puuid=${encodeURIComponent(player.puuid)}">
                    ${player.profileIconUrl ? `<img
                        class="player-leaderboard-link__image"
                        src="${escapeHtml(player.profileIconUrl)}"
                        alt="${escapeHtml(getPlayerDisplayName(player))}"
                        onerror="this.onerror=null; this.remove();"
                    >` : ''}
                    <span class="player-leaderboard-link__content">
                        <strong>${escapeHtml(getPlayerDisplayName(player))}</strong>
                        <span>${formatNumber(player.matches)} matches</span>
                    </span>
                </a>
            </td>
            <td>${formatNumber(player.matches)}</td>
            <td>${formatNumber(player.wins)}</td>
            <td><span class="winrate-pill">${formatPercent(player.winrate)}</span></td>
            <td>${formatKda(player)}</td>
        </tr>
    `).join('');
}

function renderLeaderboardError(elementId, message) {
    const body = document.getElementById(elementId);

    if (!body) {
        return;
    }

    body.innerHTML = `
        <tr>
            <td colspan="5">
                <div class="error-box">${escapeHtml(message)}</div>
            </td>
        </tr>
    `;
}

function getPlayerDisplayName(player) {
    const gameName = player.gameName || 'Unknown';
    return player.tagLine ? `${gameName}#${player.tagLine}` : gameName;
}

function formatKda(player) {
    const kills = Number(player.averageKills || 0).toFixed(2);
    const deaths = Number(player.averageDeaths || 0).toFixed(2);
    const assists = Number(player.averageAssists || 0).toFixed(2);

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
