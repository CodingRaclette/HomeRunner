// Généré par IA (Claude Sonnet 5)
// Widget de recherche/ajout de participants pour le formulaire de creation d'entree.
// Ne depend d'aucune librairie : filtrage cote client sur la variable globale
// `participantCandidates`, injectee par le template (voir entries/form.html).
document.addEventListener('DOMContentLoaded', function () {
    const searchInput = document.getElementById('participant-search');
    if (!searchInput || typeof participantCandidates === 'undefined') {
        return; // formulaire de type TASK : le widget n'est pas present sur la page
    }

    const suggestionsBox = document.getElementById('participant-suggestions');
    const tagsBox = document.getElementById('participant-tags');
    const inputsBox = document.getElementById('participant-inputs');
    const MAX_SUGGESTIONS = 5;

    const selected = new Map(); // id -> candidate

    function matches(candidate, query) {
        return candidate.name.toLowerCase().includes(query) || candidate.login.toLowerCase().includes(query);
    }

    function renderSuggestions() {
        const query = searchInput.value.trim().toLowerCase();
        suggestionsBox.innerHTML = '';
        if (query.length === 0) {
            return;
        }

        participantCandidates
            .filter(c => !selected.has(c.id))
            .filter(c => matches(c, query))
            .slice(0, MAX_SUGGESTIONS)
            .forEach(c => {
                const item = document.createElement('button');
                item.type = 'button';
                item.className = 'list-group-item list-group-item-action';
                item.textContent = c.name + ' (' + c.login + ')';
                item.addEventListener('click', () => addParticipant(c));
                suggestionsBox.appendChild(item);
            });
    }

    function addParticipant(candidate) {
        selected.set(candidate.id, candidate);

        const hiddenInput = document.createElement('input');
        hiddenInput.type = 'hidden';
        hiddenInput.name = 'participantIds';
        hiddenInput.value = String(candidate.id);
        hiddenInput.id = 'participant-input-' + candidate.id;
        inputsBox.appendChild(hiddenInput);

        const tag = document.createElement('span');
        tag.className = 'badge text-bg-secondary d-flex align-items-center gap-2 py-2 px-2';
        tag.id = 'participant-tag-' + candidate.id;
        tag.textContent = candidate.name;

        const removeBtn = document.createElement('button');
        removeBtn.type = 'button';
        removeBtn.className = 'btn-close btn-close-white';
        removeBtn.style.fontSize = '0.6em';
        removeBtn.setAttribute('aria-label', 'Retirer ' + candidate.name);
        removeBtn.addEventListener('click', () => removeParticipant(candidate.id));
        tag.appendChild(removeBtn);

        tagsBox.appendChild(tag);

        searchInput.value = '';
        suggestionsBox.innerHTML = '';
        searchInput.focus();
    }

    function removeParticipant(id) {
        selected.delete(id);
        const tag = document.getElementById('participant-tag-' + id);
        if (tag) tag.remove();
        const hiddenInput = document.getElementById('participant-input-' + id);
        if (hiddenInput) hiddenInput.remove();
    }

    searchInput.addEventListener('input', renderSuggestions);

    // referme les suggestions si on clique ailleurs sur la page
    document.addEventListener('click', function (event) {
        if (event.target !== searchInput && !suggestionsBox.contains(event.target)) {
            suggestionsBox.innerHTML = '';
        }
    });
});
