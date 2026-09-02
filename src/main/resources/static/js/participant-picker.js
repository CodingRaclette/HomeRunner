// Widget generique de recherche/ajout de personnes (participants d'un Event, assignes
// d'une Task...) pour le formulaire de creation d'entree. Aucune dependance : filtrage
// cote client sur un tableau de candidats fourni en options, ajout sous forme de tags +
// champs caches (name = options.inputName) qui alimentent la liste d'IDs cote serveur.
function createPersonPicker(options) {
    const searchInput = document.getElementById(options.searchInputId);
    if (!searchInput) {
        return; // ce bloc n'est pas present sur la page (ex : type TASK masque les participants)
    }

    const suggestionsBox = document.getElementById(options.suggestionsId);
    const tagsBox = document.getElementById(options.tagsId);
    const inputsBox = document.getElementById(options.inputsId);
    const candidates = options.candidates || [];
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

        candidates
            .filter(c => !selected.has(c.id))
            .filter(c => matches(c, query))
            .slice(0, MAX_SUGGESTIONS)
            .forEach(c => {
                const item = document.createElement('button');
                item.type = 'button';
                item.className = 'list-group-item list-group-item-action';
                item.textContent = c.name + ' (' + c.login + ')';
                item.addEventListener('click', () => addPerson(c));
                suggestionsBox.appendChild(item);
            });
    }

    function addPerson(candidate) {
        selected.set(candidate.id, candidate);

        const hiddenInput = document.createElement('input');
        hiddenInput.type = 'hidden';
        hiddenInput.name = options.inputName;
        hiddenInput.value = String(candidate.id);
        hiddenInput.id = options.inputsId + '-' + candidate.id;
        inputsBox.appendChild(hiddenInput);

        const tag = document.createElement('span');
        tag.className = 'badge text-bg-secondary d-flex align-items-center gap-2 py-2 px-2';
        tag.id = options.tagsId + '-' + candidate.id;
        tag.textContent = candidate.name;

        const removeBtn = document.createElement('button');
        removeBtn.type = 'button';
        removeBtn.className = 'btn-close btn-close-white';
        removeBtn.style.fontSize = '0.6em';
        removeBtn.setAttribute('aria-label', 'Retirer ' + candidate.name);
        removeBtn.addEventListener('click', () => removePerson(candidate.id));
        tag.appendChild(removeBtn);

        tagsBox.appendChild(tag);

        searchInput.value = '';
        suggestionsBox.innerHTML = '';
        searchInput.focus();
    }

    function removePerson(id) {
        selected.delete(id);
        const tag = document.getElementById(options.tagsId + '-' + id);
        if (tag) tag.remove();
        const hiddenInput = document.getElementById(options.inputsId + '-' + id);
        if (hiddenInput) hiddenInput.remove();
    }

    searchInput.addEventListener('input', renderSuggestions);

    // referme les suggestions si on clique ailleurs sur la page
    document.addEventListener('click', function (event) {
        if (event.target !== searchInput && !suggestionsBox.contains(event.target)) {
            suggestionsBox.innerHTML = '';
        }
    });
}

document.addEventListener('DOMContentLoaded', function () {
    if (typeof participantCandidates === 'undefined') {
        return;
    }

    createPersonPicker({
        searchInputId: 'participant-search',
        suggestionsId: 'participant-suggestions',
        tagsId: 'participant-tags',
        inputsId: 'participant-inputs',
        inputName: 'participantIds',
        candidates: participantCandidates
    });

    createPersonPicker({
        searchInputId: 'assignee-search',
        suggestionsId: 'assignee-suggestions',
        tagsId: 'assignee-tags',
        inputsId: 'assignee-inputs',
        inputName: 'assigneeIds',
        candidates: participantCandidates
    });
});
