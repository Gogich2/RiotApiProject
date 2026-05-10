const API_BASE_URL = 'https://handbrake-cognitive-railroad.ngrok-free.dev';

const api = {
    async getOverview() {
        return fetchJson('/api/stats/overview');
    },

    async search(query) {
        return fetchJson(`/api/search?query=${encodeURIComponent(query)}`);
    },

    async getChampion(championId) {
        return fetchJson(`/api/champions/${encodeURIComponent(championId)}`);
    },

    async getChampionItems(championId) {
        return fetchJson(`/api/champions/${encodeURIComponent(championId)}/items`);
    },

    async getPlayerSummary(puuid) {
        return fetchJson(`/api/players/${encodeURIComponent(puuid)}/summary`);
    },

    async getPlayerMatches(puuid, limit = 20) {
        return fetchJson(`/api/players/${encodeURIComponent(puuid)}/matches?limit=${limit}`);
    },

    async getPlayerInsights(puuid) {
        return fetchJson(`/api/players/${encodeURIComponent(puuid)}/insights`);
    }
};

async function fetchJson(url) {
    const response = await fetch(`${API_BASE_URL}${url}`, {
        method: 'GET',
        headers: {
            'ngrok-skip-browser-warning': 'true',
            'Accept': 'application/json'
        }
    });

    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }

    return response.json();
}

function getQueryParam(name) {
    return new URLSearchParams(window.location.search).get(name);
}

function formatPercent(value) {
    if (value === null || value === undefined) {
        return '0%';
    }

    return `${Number(value).toFixed(2)}%`;
}

function formatNumber(value) {
    if (value === null || value === undefined) {
        return '0';
    }

    return Number(value).toLocaleString('en-US');
}

function formatDuration(ms) {
    if (!ms) {
        return '-';
    }

    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);

    return `${minutes}:${String(seconds).padStart(2, '0')}`;
}