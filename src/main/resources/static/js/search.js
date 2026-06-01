(function initGlobalSearch() {
    const input = document.getElementById('globalSearchInput');
    const results = document.getElementById('globalSearchResults');

    if (!input || !results) {
        return;
    }

    let timeoutId = null;

    input.addEventListener('input', () => {
        clearTimeout(timeoutId);

        const query = input.value.trim();

        if (query.length < 2) {
            results.innerHTML = '';
            results.classList.remove('search__results--visible');
            return;
        }

        timeoutId = setTimeout(async () => {
            try {
                const data = await api.search(query);
                renderSearchResults(data, results);
            } catch (error) {
                results.innerHTML = `<div class="search__empty">Search failed</div>`;
                results.classList.add('search__results--visible');
            }
        }, 250);
    });

    document.addEventListener('click', (event) => {
        if (!event.target.closest('.search')) {
            results.classList.remove('search__results--visible');
        }
    });
})();

function renderSearchResults(data, container) {
    const champions = data.champions || [];
    const players = data.players || [];

    if (champions.length === 0 && players.length === 0) {
        container.innerHTML = `<div class="search__empty">Nothing found</div>`;
        container.classList.add('search__results--visible');
        return;
    }

    const championHtml = champions.map(champion => `
        <a class="search__item" href="champion.html?id=${champion.championId}">
            <span class="search__item-type">Champion</span>
            <strong>${champion.championName}</strong>
            <span>${formatNumber(champion.games)} games</span>
        </a>
    `).join('');

    const playerHtml = players.map(player => `
        <a class="search__item" href="player.html?puuid=${encodeURIComponent(player.puuid)}">
            <span class="search__item-type">Player</span>
            <strong>${player.gameName || 'Unknown'}${player.tagLine ? '#' + player.tagLine : ''}</strong>
            <span>${formatNumber(player.matches)} matches</span>
        </a>
    `).join('');

    container.innerHTML = championHtml + playerHtml;
    container.classList.add('search__results--visible');
}
