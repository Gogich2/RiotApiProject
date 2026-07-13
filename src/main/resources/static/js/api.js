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

    async getPlayerDashboard(puuid) {
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/dashboard`));
    },

    async resolveRiotId(gameName, tagLine) {
        await ensureCsrfToken();
        return fetchJson(buildApiUrl('/players/resolve'), {
            method: 'POST',
            body: JSON.stringify({ gameName, tagLine })
        });
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
        await ensureCsrfToken();
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/refresh-ranks`), {
            method: 'POST'
        });
    },

    async refreshPlayer(puuid) {
        await ensureCsrfToken();
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/refresh`), {
            method: 'POST'
        });
    },

    async getPlayerRefreshStatus(puuid) {
        return fetchJson(buildApiUrl(`/players/${encodeURIComponent(puuid)}/refresh-status`));
    },

    async getCurrentUser() {
        return fetchJson(buildApiUrl('/auth/me'));
    },

    async register(email, password, displayName) {
        return postJson('/auth/register', { email, password, displayName });
    },

    async verifyEmail(token) {
        return postJson('/auth/verify-email', { token });
    },

    async login(email, password) {
        return postJson('/auth/login', { email, password });
    },

    async logout() {
        return postJson('/auth/logout');
    },

    async requestPasswordReset(email) {
        return postJson('/auth/password-reset/request', { email });
    },

    async confirmPasswordReset(token, newPassword) {
        return postJson('/auth/password-reset/confirm', { token, newPassword });
    },

    async getSavedProfiles() {
        return fetchJson(buildApiUrl('/account/saved-profiles'));
    },

    async saveProfile(puuid, personalLabel = null) {
        return postJson('/account/saved-profiles', { puuid, personalLabel });
    },

    async updateSavedProfile(id, personalLabel, isDefault) {
        await ensureCsrfToken();
        return fetchJson(buildApiUrl(`/account/saved-profiles/${encodeURIComponent(id)}`), {
            method: 'PATCH',
            body: JSON.stringify({ personalLabel, isDefault })
        });
    },

    async deleteSavedProfile(id) {
        await ensureCsrfToken();
        return fetchJson(buildApiUrl(`/account/saved-profiles/${encodeURIComponent(id)}`), {
            method: 'DELETE'
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
    const method = (options.method || 'GET').toUpperCase();

    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
        await ensureCsrfToken();
    }

    const response = await fetch(url, {
        ...options,
        headers: buildRequestHeaders(url, options)
    });

    if (!response.ok) {
        const payload = await response.json().catch(() => null);
        throw new ApiRequestError(response.status, payload?.message || `Request failed: ${response.status}`, payload);
    }

    const contentType = response.headers.get('content-type') || '';

    if (response.status === 204 || !contentType.includes('application/json')) {
        return null;
    }

    return response.json();
}

async function postJson(path, body = null) {
    await ensureCsrfToken();
    return fetchJson(buildApiUrl(path), {
        method: 'POST',
        body: body === null ? null : JSON.stringify(body)
    });
}

async function ensureCsrfToken() {
    if (!getCookie('XSRF-TOKEN')) {
        await fetchJson(buildApiUrl('/auth/csrf'));
    }
}

function getCookie(name) {
    return document.cookie.split('; ')
        .find(row => row.startsWith(`${name}=`))
        ?.split('=')[1] || '';
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

function buildRequestHeaders(url, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = {
        Accept: 'application/json',
        ...(options.headers || {})
    };

    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
        headers['X-XSRF-TOKEN'] = decodeURIComponent(getCookie('XSRF-TOKEN'));
        headers['Content-Type'] = 'application/json';
    }

    if (isNgrokUrl(url)) {
        headers['ngrok-skip-browser-warning'] = 'true';
    }

    return headers;
}

class ApiRequestError extends Error {
    constructor(status, message, payload = null) {
        super(message);
        this.name = 'ApiRequestError';
        this.status = status;
        this.payload = payload;
    }
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
