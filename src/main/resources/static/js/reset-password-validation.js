// Validation cote client des formulaires de reinitialisation de mot de passe (menu Actions
// de la liste des utilisateurs, admin/users.html) : verifie que newPassword et confirmPassword
// correspondent avant l'envoi. Le controleur garde son propre controle cote serveur en filet de
// securite (voir AdminController) ; en cas d'echec serveur, la page se contente d'un message
// d'erreur generique, la modale ne se rouvre pas automatiquement.
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form.reset-password-form').forEach(function (form) {
        var newPassword = form.querySelector('input[name="newPassword"]');
        var confirmPassword = form.querySelector('input[name="confirmPassword"]');

        function clearMismatch() {
            confirmPassword.classList.remove('is-invalid');
        }

        newPassword.addEventListener('input', clearMismatch);
        confirmPassword.addEventListener('input', clearMismatch);

        form.addEventListener('submit', function (event) {
            if (newPassword.value !== confirmPassword.value) {
                event.preventDefault();
                confirmPassword.classList.add('is-invalid');
                confirmPassword.focus();
            }
        });
    });
});
