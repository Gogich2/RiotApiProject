document.addEventListener('DOMContentLoaded', async () => {
    const championId = getQueryParam('id');

    if (!championId) {
        document.getElementById('championHero').innerHTML = `<div class="error-box">Champion ID is missing.</div>`;
        return;
    }

    try {
        const champion = await api.getChampion(championId);
        renderChampionHero(champion);
        renderChampionStats(champion.summary);
        renderChampionAbilities(champion.abilities || []);
    } catch (error) {
        document.getElementById('championHero').innerHTML = `<div class="error-box">Could not load champion.</div>`;
        return;
    }

    try {
        const items = await api.getChampionItems(championId);
        renderChampionItems(items || []);
    } catch (error) {
        renderChampionItemsError();
    }
});

function renderChampionHero(champion) {
    const championName = formatChampionDisplayName(champion.championName);

    document.getElementById('championHero').innerHTML = `
        <div class="champion-hero__background" style="background-image: url('${champion.splashUrl || ''}')"></div>
        <div class="champion-hero__content">
            <img class="champion-hero__icon" src="${champion.imageUrl || ''}" alt="${championName}">
            <div>
                <h1 class="champion-hero__title">${championName}</h1>
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
            ${ability.imageUrl ? `<img class="ability-card__icon" src="${ability.imageUrl}" alt="${ability.abilityName}"
                onerror="this.onerror=null; this.remove();">` : ''}
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

    if (items.length === 0) {
        body.innerHTML = `
            <tr>
                <td colspan="5">
                    <div class="empty-box">No item statistics loaded yet.</div>
                </td>
            </tr>
        `;
        return;
    }

    body.innerHTML = items.map(item => `
        <tr>
            <td>
                <div class="item-cell">
                    ${item.imageUrl ? `<img class="item-cell__icon" src="${item.imageUrl}" alt="${getItemDisplayName(item)}"
                        onerror="this.onerror=null; this.remove();">` : ''}
                    <div class="item-cell__content">
                        <span class="item-cell__name">${getItemDisplayName(item)}</span>
                        <span class="item-cell__meta">Item ID: ${item.itemId}</span>
                    </div>
                </div>
            </td>
            <td>${formatNumber(item.games)}</td>
            <td>${formatNumber(item.wins)}</td>
            <td>${formatPercent(item.winrate)}</td>
            <td>${formatPercent(item.pickrate)}</td>
        </tr>
    `).join('');
}

function renderChampionItemsError() {
    document.getElementById('championItemsBody').innerHTML = `
        <tr>
            <td colspan="5">
                <div class="error-box">Could not load item statistics.</div>
            </td>
        </tr>
    `;
}

function getItemDisplayName(item) {
    if (item && item.itemName) {
        return item.itemName;
    }

    return `Item ${item.itemId}`;
}

function formatChampionDisplayName(name) {
    if (!name) {
        return '';
    }

    const specialNames = {
        AurelionSol: 'Aurelion Sol',
        Belveth: 'Bel\'Veth',
        Chogath: 'Cho\'Gath',
        DrMundo: 'Dr. Mundo',
        Kaisa: 'Kai\'Sa',
        Khazix: 'Kha\'Zix',
        KogMaw: 'Kog\'Maw',
        LeeSin: 'Lee Sin',
        MasterYi: 'Master Yi',
        MissFortune: 'Miss Fortune',
        Nunu: 'Nunu & Willump',
        TwistedFate: 'Twisted Fate',
        Velkoz: 'Vel\'Koz',
        XinZhao: 'Xin Zhao'
    };

    if (specialNames[name]) {
        return specialNames[name];
    }

    return name.replace(/([a-z])([A-Z])/g, '$1 $2').trim();
}
