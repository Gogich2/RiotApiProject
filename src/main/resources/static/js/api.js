const API_BASE_STORAGE_KEY = 'riot-stats-api-base-url';
const NGROK_HOST_MARKERS = ['ngrok-free.app', 'ngrok-free.dev', 'ngrok.app', 'ngrok.dev'];

const api = {
    async getOverview() {
        return fetchJson(buildApiUrl('/stats/overview'));
    },

    async search(query) {
        return fetchJson(buildApiUrl('/search', { query }));
    },

    async getChampion(championId) {
        return fetchJson(buildApiUrl(`/champions/${encodeURIComponent(championId)}`));
    },

    async getChampions() {
        return fetchJson(buildApiUrl('/champions'));
    },

    async getChampionItems(championId) {
        return fetchJson(buildApiUrl(`/champions/${encodeURIComponent(championId)}/items`));
    },

    async getPlayerSummary(puuid) {
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/summary`));
    },

    async getPlayerLeaderboards() {
        return fetchJson(buildApiUrl('/players/leaderboard'));
    },

    async getPlayerMatches(puuid, limit = 20) {
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/matches`, { limit }));
    },

    async getPlayerChampions(puuid) {
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/champions`));
    },

    async getPlayerInsights(puuid) {
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/insights`));
    },

    async getPlayerRanks(puuid) {
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/ranks`));
    },

    async refreshPlayerRanks(puuid) {
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/refresh-ranks`), {
            method: 'POST'
        });
    },

    async getPlayerRankHistory(puuid) {
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/rank-history`));
    },

    async getMatchDetails(matchId, puuid) {
        return fetchJson(buildApiUrl(`/matches/${encodeURIComponent(matchId)}/details`, { puuid }));
    }
};

async function fetchJson(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: buildRequestHeaders(url, options.headers)
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

function buildApiUrl(path, queryParams = null) {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    const configuredBaseUrl = getConfiguredApiBaseUrl();
    const baseUrl = configuredBaseUrl || '/api';

    return appendQueryParams(`${trimTrailingSlash(baseUrl)}${normalizedPath}`, queryParams);
}

function buildRequestHeaders(url, customHeaders = {}) {
    const headers = {
        Accept: 'application/json',
        ...customHeaders
    };

    if (isNgrokUrl(url)) {
        headers['ngrok-skip-browser-warning'] = 'true';
    }

    return headers;
}

function getConfiguredApiBaseUrl() {
    const globalConfig = window.RIOT_STATS_CONFIG?.apiBaseUrl;
    const storedConfig = window.localStorage.getItem(API_BASE_STORAGE_KEY);
    const configuredValue = globalConfig || storedConfig || '';

    return normalizeApiBaseUrl(configuredValue);
}

function normalizeApiBaseUrl(value) {
    const trimmed = trimTrailingSlash(String(value || '').trim());

    if (!trimmed) {
        return '';
    }

    if (trimmed.endsWith('/api')) {
        return trimmed;
    }

    return `${trimmed}/api`;
}

function appendQueryParams(url, queryParams) {
    if (!queryParams) {
        return url;
    }

    const params = new URLSearchParams();

    Object.entries(queryParams).forEach(([key, value]) => {
        if (value !== null && value !== undefined) {
            params.set(key, String(value));
        }
    });

    const queryString = params.toString();

    if (!queryString) {
        return url;
    }

    return `${url}?${queryString}`;
}

function isNgrokUrl(url) {
    return NGROK_HOST_MARKERS.some(marker => url.includes(marker));
}

function trimTrailingSlash(value) {
    return value.replace(/\/+$/, '');
}
