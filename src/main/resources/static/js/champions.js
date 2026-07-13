document.addEventListener('DOMContentLoaded', async () => {
    const filterInput = document.getElementById('championFilterInput');
    const roleButtonsContainer = document.getElementById('championRoleButtons');
    let champions = [];
    let selectedRole = 'default';

    try {
        champions = await api.getChampions();
        renderRoleIcons();
        renderVisibleChampions(champions, filterInput, selectedRole);
        renderChampionListMeta(champions.length, selectedRole, '');
    } catch (error) {
        document.getElementById('championsGrid').innerHTML = `
            <div class="error-box">Could not load champions.</div>
        `;
        return;
    }

    if (!filterInput) {
        return;
    }

    filterInput.addEventListener('input', () => {
        renderVisibleChampions(champions, filterInput, selectedRole);
    });

    if (roleButtonsContainer) {
        roleButtonsContainer.addEventListener('click', event => {
            const button = event.target.closest('[data-role-sort]');

            if (!button) {
                return;
            }

            selectedRole = button.dataset.roleSort || 'default';
            setActiveRoleButton(roleButtonsContainer, selectedRole);
            renderVisibleChampions(champions, filterInput, selectedRole);
        });
    }
});

function renderChampions(champions) {
    const container = document.getElementById('championsGrid');

    if (!container) {
        return;
    }

    if (!champions || champions.length === 0) {
        container.innerHTML = `<div class="empty-box">No champions found.</div>`;
        return;
    }

    container.innerHTML = champions.map(champion => {
        const championName = formatChampionDisplayName(champion.championName);

        return `
            <a class="champion-card" href="champion.html?id=${encodeURIComponent(champion.championId)}">
                ${champion.imageUrl ? `<img
                    class="champion-card__image"
                    src="${escapeHtml(champion.imageUrl)}"
                    alt="${escapeHtml(championName)}"
                    onerror="this.onerror=null; this.remove();"
                >` : ''}
                <div class="champion-card__content">
                    <div class="champion-card__identity">
                        <strong class="champion-card__title">${escapeHtml(championName)}</strong>
                        ${champion.primaryRole ? `
                            <span class="champion-card__role">${escapeHtml(formatRoleLabel(champion.primaryRole))}</span>
                        ` : ''}
                    </div>
                    <div class="champion-card__stats">
                        <span class="champion-card__stat">
                            <span class="champion-card__label">Games</span>
                            <strong>${formatNumber(champion.games)}</strong>
                        </span>
                        <span class="champion-card__stat">
                            <span class="champion-card__label">Wins</span>
                            <strong>${formatNumber(champion.wins)}</strong>
                        </span>
                        <span class="champion-card__stat">
                            <span class="champion-card__label">Winrate</span>
                            <strong class="champion-card__stat-accent">${formatPercent(champion.winrate)}</strong>
                        </span>
                    </div>
                </div>
            </a>
        `;
    }).join('');
}

function renderVisibleChampions(champions, filterInput, selectedRole) {
    const query = filterInput ? filterInput.value.trim().toLowerCase() : '';
    const filtered = champions.filter(champion => {
        const name = formatChampionDisplayName(champion.championName).toLowerCase();
        return name.includes(query);
    });
    const sorted = [...filtered].sort((left, right) => compareChampions(left, right, selectedRole));

    renderChampions(sorted);
    renderChampionListMeta(sorted.length, selectedRole, query);
}

function compareChampions(left, right, selectedRole) {
    const roleWeight = getRoleSortWeight(right, selectedRole) - getRoleSortWeight(left, selectedRole);

    if (roleWeight !== 0) {
        return roleWeight;
    }

    const gameDiff = Number(right.games || 0) - Number(left.games || 0);

    if (gameDiff !== 0) {
        return gameDiff;
    }

    return formatChampionDisplayName(left.championName)
        .localeCompare(formatChampionDisplayName(right.championName));
}

function getRoleSortWeight(champion, selectedRole) {
    if (!selectedRole || selectedRole === 'default') {
        return 0;
    }

    return champion.primaryRole === selectedRole ? 1 : 0;
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

function setActiveRoleButton(container, selectedRole) {
    const buttons = container.querySelectorAll('[data-role-sort]');

    buttons.forEach(button => {
        const isActive = (button.dataset.roleSort || 'default') === selectedRole;
        button.classList.toggle('role-sort-button--active', isActive);
    });
}

function renderChampionListMeta(count, selectedRole, query) {
    const meta = document.getElementById('championListMeta');

    if (!meta) {
        return;
    }

    const roleLabel = !selectedRole || selectedRole === 'default'
        ? 'all roles'
        : `${formatRoleLabel(selectedRole)} priority`;
    const queryLabel = query ? ` for "${query}"` : '';

    meta.textContent = `${formatNumber(count)} champions visible across ${roleLabel}${queryLabel}.`;
}

function renderRoleIcons() {
    const icons = {
        TOP: buildRoleIcon('#8ab4ff', 'M8 2 L14 8 L11.5 8 L11.5 14 L4.5 14 L4.5 8 L2 8 Z'),
        JUNGLE: buildRoleIcon('#86efac', 'M8 2 C11 4.5 13.5 8 13.5 10.5 C13.5 13 11 15 8 15 C5 15 2.5 13 2.5 10.5 C2.5 8 5 4.5 8 2 Z'),
        MIDDLE: buildRoleIcon('#facc15', 'M8 2 L14 14 L2 14 Z'),
        BOTTOM: buildRoleIcon('#fca5a5', 'M2.5 4.5 H13.5 V7 H9.5 V13.5 H6.5 V7 H2.5 Z'),
        UTILITY: buildRoleIcon('#c4b5fd', 'M8 2.5 L9.6 5.8 L13.2 6.3 L10.6 8.8 L11.2 12.5 L8 10.8 L4.8 12.5 L5.4 8.8 L2.8 6.3 L6.4 5.8 Z')
    };

    document.querySelectorAll('[data-role-icon]').forEach(icon => {
        const role = icon.dataset.roleIcon;

        if (role && icons[role]) {
            icon.innerHTML = `<img src="${icons[role]}" alt="" aria-hidden="true">`;
        }
    });
}

function buildRoleIcon(color, path) {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16">
        <path fill="${color}" d="${path}"/>
    </svg>`;

    return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
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

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
