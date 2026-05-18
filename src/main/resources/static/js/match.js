document.addEventListener('DOMContentLoaded', () => {
    const matchId = getQueryParam('id');
    const placeholderText = document.getElementById('matchPlaceholderText');
    const placeholderBox = document.getElementById('matchPlaceholderBox');

    if (!matchId) {
        if (placeholderText) {
            placeholderText.textContent = 'Match ID is missing.';
        }

        if (placeholderBox) {
            placeholderBox.textContent = 'Match details coming soon. Open this page with a match ID.';
        }

        return;
    }

    if (placeholderText) {
        placeholderText.textContent = `Match ${matchId} details are coming soon.`;
    }

    if (placeholderBox) {
        placeholderBox.textContent = `Detailed breakdown for ${matchId} will be added here.`;
    }
});
