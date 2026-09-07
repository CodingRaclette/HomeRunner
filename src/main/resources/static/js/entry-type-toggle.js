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

    // "Toute la journee" : masque les champs date/heure precis (.entry-field-datetime)
    // et ne laisse qu'un champ date unique (#all-day-date). Ce champ n'est pas relie
    // au formulaire (pas de th:field) : juste avant l'envoi, on recopie sa valeur dans
    // les champs caches "date"/"endDate" (minuit -> 23:59 le meme jour) pour que le
    // backend continue de recevoir des datetime-local classiques sans rien changer cote
    // modele.
    const allDayToggle = document.getElementById('all-day-toggle');
    const dateTimeFields = document.querySelectorAll('.entry-field-datetime');
    const allDayField = document.querySelector('.entry-field-allday');
    const allDayInput = document.getElementById('all-day-date');
    const dateInput = document.getElementById('date');
    const endDateInput = document.getElementById('endDate');

    const TYPE_LABELS = {
        EVENT: 'Évènement',
        TASK: 'Tâche'
    };

    function isEventSelected() {
        const checked = document.querySelector('input[name="type"]:checked');
        return (checked ? checked.value : 'EVENT') === 'EVENT';
    }

    function formatDateTimeLocal(d) {
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
            'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    // Le backend ne pre-remplit "endDate" (date de debut + 1h) que si le formulaire est
    // ouvert directement en EVENT. Si on bascule TASK -> EVENT cote client (sans recharger
    // la page), ce calcul n'a jamais eu lieu : on le refait ici a la volee.
    function ensureDefaultEndDate() {
        if (!endDateInput || endDateInput.value) {
            return;
        }
        if (dateInput && !dateInput.value) {
            dateInput.value = formatDateTimeLocal(new Date());
        }
        const base = dateInput ? new Date(dateInput.value) : new Date();
        if (isNaN(base.getTime())) {
            return;
        }
        base.setHours(base.getHours() + 1);
        endDateInput.value = formatDateTimeLocal(base);
    }

    function applyVisibility() {
        const isEvent = isEventSelected();
        const isAllDay = !!(allDayToggle && allDayToggle.checked);

        eventFields.forEach(function (field) {
            field.classList.toggle('d-none', !isEvent);
        });
        taskFields.forEach(function (field) {
            field.classList.toggle('d-none', isEvent);
        });

        // Un champ date/heure precis reste soumis a la regle EVENT/TASK habituelle
        // (entry-field-event), en plus d'etre masque des que "Toute la journee" est coche.
        dateTimeFields.forEach(function (field) {
            const relevantForType = !field.classList.contains('entry-field-event') || isEvent;
            field.classList.toggle('d-none', isAllDay || !relevantForType);
        });
        if (allDayField) {
            allDayField.classList.toggle('d-none', !isAllDay);
        }

        if (typeLabel) {
            typeLabel.textContent = TYPE_LABELS[isEvent ? 'EVENT' : 'TASK'];
        }
    }

    radios.forEach(function (radio) {
        radio.addEventListener('change', function () {
            if (isEventSelected()) {
                ensureDefaultEndDate();
            }
            applyVisibility();
        });
    });

    if (allDayToggle) {
        allDayToggle.addEventListener('change', function () {
            // A l'activation, initialise le champ date unique avec la date deja saisie.
            if (allDayToggle.checked && allDayInput && !allDayInput.value && dateInput && dateInput.value) {
                allDayInput.value = dateInput.value.slice(0, 10);
            }
            applyVisibility();
        });

        const form = allDayToggle.closest('form');
        if (form) {
            form.addEventListener('submit', function () {
                if (allDayToggle.checked && allDayInput && allDayInput.value) {
                    if (dateInput) {
                        dateInput.value = allDayInput.value + 'T00:00';
                    }
                    if (endDateInput && isEventSelected()) {
                        endDateInput.value = allDayInput.value + 'T23:59';
                    }
                }
            });
        }

        // Heuristique de reouverture (edition) : si la date va deja de minuit a 23:59,
        // on considere que l'entree a ete creee "toute la journee" et on re-coche la case.
        // Limitation : cote backend, rien ne distingue vraiment un evenement "toute la
        // journee" d'un evenement qui se trouve juste durer de 00:00 a 23:59.
        const startsAtMidnight = dateInput && /T00:00$/.test(dateInput.value);
        const endsAtEndOfDay = !isEventSelected() || !endDateInput || /T23:59$/.test(endDateInput.value);
        if (startsAtMidnight && endsAtEndOfDay && dateInput.value) {
            allDayToggle.checked = true;
            allDayInput.value = dateInput.value.slice(0, 10);
        }
    }

    applyVisibility(); // synchronise l'affichage avec l'etat initial du formulaire
});
