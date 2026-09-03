package com.nyxeira.homerunner.usermanagement.ui.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
Formulaire de reinitialisation du mot de passe d'un utilisateur par un admin : pas d'ancien mot
de passe a fournir (l'admin ne le connait pas), contrairement a ChangePasswordForm.
 */
public class ResetPasswordForm {

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String newPassword;

    @NotBlank
    private String confirmPassword;

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
