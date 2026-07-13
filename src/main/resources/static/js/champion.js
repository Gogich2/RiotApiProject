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

    await championBuilds.mount({
        championId,
        root: document.getElementById('championBuilds'),
        apiClient: api,
        sessionStorage: window.sessionStorage,
        history: window.history,
        location: window.location
    });
});

function renderChampionHero(champion) {
    const championName = formatChampionDisplayName(champion.championName);
    const summary = champion.summary || {};
    const badges = [
        champion.primaryRole ? formatRoleLabel(champion.primaryRole) : null,
        summary.games ? `${formatNumber(summary.games)} games` : null,
        summary.winrate ? `${formatPercent(summary.winrate)} win rate` : null
    ].filter(Boolean);

    document.getElementById('championHero').innerHTML = `
        <div class="champion-hero__background" style="background-image: url('${escapeHtml(champion.splashUrl || '')}')"></div>
        <div class="champion-hero__overlay"></div>
        <div class="champion-hero__content">
            <div class="champion-hero__identity">
                <img class="champion-hero__icon" src="${escapeHtml(champion.imageUrl || '')}" alt="${escapeHtml(championName)}">
                <div>
                    <span class="champion-hero__eyebrow">Champion dossier</span>
                    <h1 class="champion-hero__title">${escapeHtml(championName)}</h1>
                    <p class="champion-hero__subtitle">${escapeHtml(champion.title || '')}</p>
                    <p class="champion-hero__lore">${escapeHtml(champion.lore || '')}</p>
                    <div class="champion-hero__badges">
                        ${badges.map(badge => `<span class="champion-hero__badge">${escapeHtml(badge)}</span>`).join('')}
                    </div>
                </div>
            </div>

            <div class="champion-hero__summary">
                <article class="champion-hero__summary-card">
                    <span>Average KDA</span>
                    <strong>${formatDecimal(summary.averageKills)}/${formatDecimal(summary.averageDeaths)}/${formatDecimal(summary.averageAssists)}</strong>
                </article>
                <article class="champion-hero__summary-card">
                    <span>Damage profile</span>
                    <strong>${formatNumber(summary.averageDamageToChampions)}</strong>
                </article>
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
            <span class="stat-card__meta">Champion sample size in the local dataset.</span>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Winrate</span>
            <strong class="stat-card__value">${formatPercent(summary.winrate)}</strong>
            <span class="stat-card__meta">Observed win efficiency across tracked games.</span>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Avg KDA</span>
            <strong class="stat-card__value">${formatDecimal(summary.averageKills)}/${formatDecimal(summary.averageDeaths)}/${formatDecimal(summary.averageAssists)}</strong>
            <span class="stat-card__meta">Kills, deaths, and assists per appearance.</span>
        </article>
        <article class="stat-card">
            <span class="stat-card__label">Avg Damage</span>
            <strong class="stat-card__value">${formatNumber(summary.averageDamageToChampions)}</strong>
            <span class="stat-card__meta">Average champion damage dealt in tracked matches.</span>
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
        <article class="ability-card" tabindex="0">
            ${ability.imageUrl ? `<img class="ability-card__icon" src="${ability.imageUrl}" alt="${getAbilityDisplayName(ability)}"
                onerror="this.onerror=null; this.remove();">` : ''}
            <div class="ability-card__content">
                <span class="ability-card__key">${ability.abilityKey || 'Ability'}</span>
                <h3 class="ability-card__title">${getAbilityDisplayName(ability)}</h3>
            </div>
            ${ability.abilityDescription ? `
                <div class="ability-card__tooltip">
                    <p class="ability-card__text">${ability.abilityDescription}</p>
                </div>
            ` : ''}
        </article>
    `).join('');
}

function getAbilityDisplayName(ability) {
    if (ability && ability.abilityName) {
        return ability.abilityName;
    }

    if (ability && ability.abilityKey) {
        return ability.abilityKey;
    }

    return 'Unknown ability';
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

function formatRoleLabel(role) {
    const labels = {
        TOP: 'Top',
        JUNGLE: 'Jungle',
        MIDDLE: 'Mid',
        BOTTOM: 'Bottom',
        UTILITY: 'Support'
    };

    return labels[role] || role || '';
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
