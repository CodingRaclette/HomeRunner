package com.nyxeira.homerunner.usermanagement.ui.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
Formulaire de changement de mot de passe
 */
public class ChangePasswordForm {

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String newPassword;

    @NotBlank
    private String confirmPassword;

    public String getOldPassword() { return oldPassword; }
    public String getNewPassword() { return newPassword; }
    public String getConfirmPassword() { return confirmPassword; }

    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
