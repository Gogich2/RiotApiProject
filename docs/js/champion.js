document.addEventListener('DOMContentLoaded', async () => {
    const championId = getQueryParam('id');

    if (!championId) {
        document.getElementById('championHero').innerHTML = `<div class="error-box">Champion ID is missing.</div>`;
        return;
    }

    try {
        const [champion, items] = await Promise.all([
            api.getChampion(championId),
            api.getChampionItems(championId)
        ]);

        renderChampionHero(champion);
        renderChampionStats(champion.summary);
        renderChampionAbilities(champion.abilities || []);
        renderChampionItems(items || []);
    } catch (error) {
        document.getElementById('championHero').innerHTML = `<div class="error-box">Could not load champion.</div>`;
    }
});

function renderChampionHero(champion) {
    document.getElementById('championHero').innerHTML = `
        <div class="champion-hero__background" style="background-image: url('${champion.splashUrl || ''}')"></div>
        <div class="champion-hero__content">
            <img class="champion-hero__icon" src="${champion.imageUrl || ''}" alt="${champion.championName}">
            <div>
                <h1 class="champion-hero__title">${champion.championName}</h1>
                <p class="champion-hero__subtitle">${champion.title || ''}</p>
                <p class="champion-hero__lore">${champion.lore || ''}</p>
            </div>
        </div>
    `;
}

function renderChampionStats(summary) {
    if (!summary) {
        return;
    }

    document.getElementById('championStats').innerHTML = `
        <article class="stat-card">
            <span class="stat-card__label">Games</span>
            <strong class="stat-card__value">${formatNumber(summary.games)}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Winrate</span>
            <strong class="stat-card__value">${formatPercent(summary.winrate)}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Avg KDA</span>
            <strong class="stat-card__value">${summary.averageKills}/${summary.averageDeaths}/${summary.averageAssists}</strong>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Avg Damage</span>
            <strong class="stat-card__value">${formatNumber(summary.averageDamageToChampions)}</strong>
        </article>
    `;
}

function renderChampionAbilities(abilities) {
    const container = document.getElementById('championAbilities');

    if (abilities.length === 0) {
        container.innerHTML = `<div class="empty-box">No abilities loaded yet.</div>`;
        return;
    }

    container.innerHTML = abilities.map(ability => `
        <article class="ability-card">
            ${ability.imageUrl ? `<img class="ability-card__icon" src="${ability.imageUrl}" alt="${ability.abilityName}">` : ''}
            <div>
                <span class="ability-card__key">${ability.abilityKey}</span>
                <h3 class="ability-card__title">${ability.abilityName}</h3>
                <p class="ability-card__text">${ability.abilityDescription || ''}</p>
            </div>
        </article>
    `).join('');
}

function renderChampionItems(items) {
    const body = document.getElementById('championItemsBody');

    body.innerHTML = items.map(item => `
        <tr>
            <td>${item.itemId}</td>
            <td>${formatNumber(item.games)}</td>
            <td>${formatNumber(item.wins)}</td>
            <td>${formatPercent(item.winrate)}</td>
            <td>${formatPercent(item.pickrate)}</td>
        </tr>
    `).join('');
}