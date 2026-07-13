document.addEventListener('DOMContentLoaded', async () => {
    const currentUser = await bootstrapAccountHeader();
    initializeAccountForms(currentUser);
    initializeTokenAction();

    if (currentUser?.authenticated) {
        await renderSavedProfiles();
    }
});

async function bootstrapAccountHeader() {
    const entries = document.querySelectorAll('[data-account-entry]');

    if (entries.length === 0) {
        return null;
    }

    try {
        const currentUser = await api.getCurrentUser();
        entries.forEach(entry => renderAccountEntry(entry, currentUser));
        return currentUser;
    } catch (error) {
        entries.forEach(entry => renderAccountEntry(entry, { authenticated: false }));
        return { authenticated: false };
    }
}

function renderAccountEntry(entry, currentUser) {
    if (!currentUser?.authenticated) {
        const returnTo = encodeURIComponent(`${window.location.pathname.split('/').pop() || 'index.html'}${window.location.search}`);
        entry.innerHTML = `<a class="account-entry__link" href="account.html?returnTo=${returnTo}">Sign in</a>`;
        return;
    }

    entry.innerHTML = `
        <a class="account-entry__identity" href="account.html#saved">
            <span>${escapeAccountHtml(currentUser.user.displayName)}</span>
            <small>Saved profiles</small>
        </a>
        <button class="account-entry__logout" type="button">Sign out</button>
    `;
    entry.querySelector('.account-entry__logout').addEventListener('click', async event => {
        event.currentTarget.disabled = true;
        await api.logout().catch(() => null);
        renderAccountEntry(entry, { authenticated: false });
    });
}

function initializeAccountForms(currentUser) {
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const authPanels = document.getElementById('accountAuthPanels');
    const savedSection = document.getElementById('savedProfilesSection');

    if (currentUser?.authenticated) {
        if (authPanels) authPanels.hidden = true;
        if (savedSection) savedSection.hidden = false;
    }

    bindAccountForm(loginForm, async form => {
        await api.login(form.elements.email.value, form.elements.password.value);
        const returnTo = safeReturnTo(new URLSearchParams(window.location.search).get('returnTo'));
        window.location.assign(returnTo || 'account.html#saved');
    }, 'Signed in. Opening your account...');

    bindAccountForm(registerForm, async form => {
        await api.register(
            form.elements.email.value,
            form.elements.password.value,
            form.elements.displayName.value
        );
    }, 'Account created. Check your email to verify it.');

    const resetPanel = document.getElementById('resetRequestPanel');
    document.getElementById('showResetRequest')?.addEventListener('click', () => {
        resetPanel.hidden = false;
        document.getElementById('resetEmail').focus();
    });
    bindAccountForm(document.getElementById('resetRequestForm'), async form => {
        await api.requestPasswordReset(form.elements.email.value);
    }, 'If that email has an account, a reset link is on its way.');
}

function bindAccountForm(form, submitAction, successMessage) {
    if (!form) return;
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const button = form.querySelector('button[type="submit"]');
        const status = form.querySelector('[data-form-status]');
        button.disabled = true;
        setAccountStatus(status, 'loading', 'Working...');
        try {
            await submitAction(form);
            form.reset();
            setAccountStatus(status, 'success', successMessage);
        } catch (error) {
            setAccountStatus(status, 'error', error.message || 'This action could not be completed.');
        } finally {
            button.disabled = false;
        }
    });
}

function initializeTokenAction() {
    const params = new URLSearchParams(window.location.search);
    const action = params.get('action');
    const token = params.get('token');
    const panel = document.getElementById('tokenActionPanel');

    if (!panel || !action || !token) return;
    panel.hidden = false;
    const status = document.getElementById('tokenActionStatus');

    if (action === 'verify') {
        document.getElementById('tokenActionTitle').textContent = 'Verify your email';
        setAccountStatus(status, 'loading', 'Verifying your email...');
        api.verifyEmail(token)
            .then(() => setAccountStatus(status, 'success', 'Email verified. You can sign in now.'))
            .catch(error => setAccountStatus(status, 'error', error.message));
    }

    if (action === 'reset') {
        document.getElementById('tokenActionTitle').textContent = 'Choose a new password';
        const form = document.getElementById('resetConfirmForm');
        form.hidden = false;
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const button = form.querySelector('button');
            button.disabled = true;
            try {
                await api.confirmPasswordReset(token, form.elements.newPassword.value);
                form.hidden = true;
                setAccountStatus(status, 'success', 'Password updated. Sign in with your new password.');
            } catch (error) {
                setAccountStatus(status, 'error', error.message);
                button.disabled = false;
            }
        });
    }
}

async function renderSavedProfiles() {
    const section = document.getElementById('savedProfilesSection');
    const list = document.getElementById('savedProfilesList');

    if (!section || !list) return;
    section.hidden = false;
    list.innerHTML = '<div class="empty-box">Loading saved profiles...</div>';
    try {
        const profiles = await api.getSavedProfiles();
        list.innerHTML = profiles.length === 0
            ? '<div class="empty-box">No saved profiles yet. Save one from any public player dashboard.</div>'
            : profiles.map(savedProfileMarkup).join('');
        bindSavedProfileActions(list);
    } catch (error) {
        list.innerHTML = '<div class="error-box">Saved profiles could not be loaded.</div>';
    }
}

function savedProfileMarkup(profile) {
    const displayName = profile.tagLine ? `${profile.gameName}#${profile.tagLine}` : profile.gameName;
    return `
        <article class="saved-profile-card" data-saved-profile-id="${escapeAccountHtml(profile.id)}">
            <a class="saved-profile-card__identity" href="player.html?puuid=${encodeURIComponent(profile.puuid)}">
                <strong>${escapeAccountHtml(displayName || 'Unknown player')}</strong>
                <span>${profile.isDefault ? 'Default profile' : 'Saved profile'}</span>
            </a>
            <div class="saved-profile-card__controls">
                <label class="sr-only" for="label-${escapeAccountHtml(profile.id)}">Private label</label>
                <input
                    class="form-input"
                    id="label-${escapeAccountHtml(profile.id)}"
                    name="personalLabel"
                    maxlength="80"
                    value="${escapeAccountHtml(profile.personalLabel || '')}"
                    placeholder="Private label"
                >
                <button class="button button--secondary" type="button" data-save-label>Save label</button>
                <button class="text-button" type="button" data-make-default>Make default</button>
                <button class="text-button text-button--danger" type="button" data-remove-saved>Remove</button>
            </div>
            <p class="form-status" data-saved-status aria-live="polite"></p>
        </article>
    `;
}

function bindSavedProfileActions(list) {
    list.querySelectorAll('[data-saved-profile-id]').forEach(card => {
        const id = card.dataset.savedProfileId;
        const label = card.querySelector('[name="personalLabel"]');
        const status = card.querySelector('[data-saved-status]');
        card.querySelector('[data-save-label]').addEventListener('click', async () => {
            await updateSavedCard(id, label.value, false, status);
        });
        card.querySelector('[data-make-default]').addEventListener('click', async () => {
            await updateSavedCard(id, label.value, true, status);
            await renderSavedProfiles();
        });
        card.querySelector('[data-remove-saved]').addEventListener('click', async () => {
            await api.deleteSavedProfile(id);
            card.remove();
        });
    });
}

async function updateSavedCard(id, label, isDefault, status) {
    try {
        await api.updateSavedProfile(id, label, isDefault);
        setAccountStatus(status, 'success', isDefault ? 'Default profile updated.' : 'Private label saved.');
    } catch (error) {
        setAccountStatus(status, 'error', error.message);
    }
}

function setAccountStatus(element, state, message) {
    if (!element) return;
    element.dataset.state = state;
    element.textContent = message;
}

function safeReturnTo(value) {
    if (!value || value.startsWith('/') || !/^[a-z0-9-]+\.html(?:[?#].*)?$/i.test(value)) return null;
    return value;
}

function escapeAccountHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
