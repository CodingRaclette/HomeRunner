// Bascule dynamique entre EVENT et TASK sur le formulaire de creation d'entree :
// affiche/masque les champs specifiques a chaque type (date de fin + participants pour
// EVENT, assignes pour TASK) selon le bouton radio "type" selectionne, sans recharger
// la page.
document.addEventListener('DOMContentLoaded', function () {
    const radios = document.querySelectorAll('input[name="type"]');
    if (radios.length === 0) {
        return;
    }

    const eventFields = document.querySelectorAll('.entry-field-event');
    const taskFields = document.querySelectorAll('.entry-field-task');
    const typeLabel = document.getElementById('entry-type-label');

    const TYPE_LABELS = {
        EVENT: 'Évènement',
        TASK: 'Tâche'
    };

    function applyVisibility() {
        const checked = document.querySelector('input[name="type"]:checked');
        const value = checked ? checked.value : 'EVENT';
        const isEvent = value === 'EVENT';

        eventFields.forEach(function (field) {
            field.classList.toggle('d-none', !isEvent);
        });
        taskFields.forEach(function (field) {
            field.classList.toggle('d-none', isEvent);
        });

        if (typeLabel) {
            typeLabel.textContent = TYPE_LABELS[value] || value;
        }
    }

    radios.forEach(function (radio) {
        radio.addEventListener('change', applyVisibility);
    });

    applyVisibility(); // synchronise l'affichage avec l'etat initial du formulaire
});
