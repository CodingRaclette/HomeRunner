// Bascule l'affichage des champs de recurrence (frequence / intervalle / date de fin
// de serie) selon la case "Recurrent". Gere aussi deux cas limites :
//  - a l'activation, pose des valeurs par defaut sensees (frequence quotidienne,
//    intervalle 1) si rien n'est deja renseigne, pour eviter d'envoyer un intervalle
//    a 0 (valeur par defaut du champ int cote backend) ;
//  - a la desactivation, vide le champ frequence : c'est lui qui fait foi cote backend
//    (EntryDTO.toRecurrenceRule()) pour savoir si l'entree est recurrente. Sans ca,
//    decocher la case sans changer la frequence laisserait l'entree recurrente.
document.addEventListener('DOMContentLoaded', function () {
    const toggle = document.getElementById('recurrence-toggle');
    if (!toggle) {
        return;
    }

    const fields = document.querySelectorAll('.entry-field-recurrence');
    const frequencySelect = document.getElementById('frequency');
    const intervalInput = document.getElementById('interval');

    function applyVisibility() {
        fields.forEach(function (field) {
            field.classList.toggle('d-none', !toggle.checked);
            // un champ cache (d-none) reste soumis a la validation native HTML5 (ex.
            // min="1" sur interval) alors qu'il n'est pas focusable : le navigateur
            // bloque alors silencieusement la soumission du formulaire. On desactive
            // donc les champs du bloc recurrence quand ils sont caches, ce qui les
            // exclut a la fois de la validation et de la soumission.
            field.querySelectorAll('input, select').forEach(function (input) {
                input.disabled = !toggle.checked;
            });
        });
    }

    toggle.addEventListener('change', function () {
        if (toggle.checked) {
            if (frequencySelect && !frequencySelect.value) {
                frequencySelect.value = 'DAILY';
            }
            if (intervalInput && (!intervalInput.value || Number(intervalInput.value) <= 0)) {
                intervalInput.value = '1';
            }
        } else if (frequencySelect) {
            frequencySelect.value = '';
        }
        applyVisibility();
    });

    applyVisibility(); // synchronise l'affichage avec l'etat initial du formulaire
});
