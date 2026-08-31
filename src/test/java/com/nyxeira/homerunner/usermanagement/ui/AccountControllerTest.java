package com.nyxeira.homerunner.usermanagement.ui;

import com.nyxeira.homerunner.common.web.WebPaths;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.services.exceptions.InvalidCurrentPasswordException;
import com.nyxeira.homerunner.usermanagement.services.UserManagementService;
import com.nyxeira.homerunner.usermanagement.ui.forms.ChangePasswordForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;

/**
 * Tests unitaires purs : BindingResult est une vraie BeanPropertyBindingResult
 * (pas un mock) pour bénéficier du vrai comportement de rejectValue/hasFieldErrors,
 * plus simple et plus fiable qu'un mock ici.
 */
@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    UserManagementService userManagementService;

    @Mock
    Principal principal;

    private AccountController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountController(userManagementService);
    }

    private ChangePasswordForm formValide() {
        ChangePasswordForm form = new ChangePasswordForm();
        form.setOldPassword("ancien-mdp");
        form.setNewPassword("nouveauMdp123");
        form.setConfirmPassword("nouveauMdp123");
        return form;
    }

    @Test
    void redirigeVersLeFormulaireSiErreursDeValidationBean() {
        ChangePasswordForm form = formValide();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.reject("dummy"); // simule une erreur déjà détectée par @Valid

        String view = controller.changePassword(form, bindingResult, principal);

        assertThat(view).isEqualTo(WebPaths.CHANGE_PASSWORD_FULL);
        verifyNoInteractions(userManagementService);
    }

    @Test
    void refuseLeChangementSiNouveauMdpEtConfirmationDifferent() {
        ChangePasswordForm form = formValide();
        form.setConfirmPassword("autreChose123");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        String view = controller.changePassword(form, bindingResult, principal);

        assertThat(bindingResult.hasFieldErrors("confirmPassword")).isTrue();
        assertThat(view).isEqualTo(WebPaths.CHANGE_PASSWORD_FULL);
        // Le service ne doit JAMAIS être appelé si la confirmation ne correspond pas :
        // sinon le mot de passe serait changé malgré une confirmation invalide.
        verifyNoInteractions(userManagementService);
    }

    @Test
    void appelleLeServiceAvecLAncienEtLeNouveauMdpDansLeBonOrdre() {
        when(principal.getName()).thenReturn("alice");
        User updatedUser = UserTestBuilder.aUser().withEmail("a@homerunner.local").withPasswordHash("nouveau-hash").build();
        when(userManagementService.changePassword("alice", "ancien-mdp", "nouveauMdp123"))
                .thenReturn(updatedUser);

        ChangePasswordForm form = formValide();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        String view = controller.changePassword(form, bindingResult, principal);

        assertThat(view).isEqualTo("redirect:/");
    }

    @Test
    void rejetteEtResteSurLeFormulaireSiLAncienMdpEstIncorrect() {
        when(principal.getName()).thenReturn("alice");
        when(userManagementService.changePassword(anyString(), anyString(), anyString()))
                .thenThrow(new InvalidCurrentPasswordException());

        ChangePasswordForm form = formValide();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        String view = controller.changePassword(form, bindingResult, principal);

        assertThat(bindingResult.hasFieldErrors("oldPassword")).isTrue();
        assertThat(view).isEqualTo(WebPaths.CHANGE_PASSWORD_FULL);
    }
}
