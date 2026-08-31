package com.nyxeira.homerunner.common.web;


/**
 * Point unique de référence pour tous les chemins d'URL de l'application.
 * Toute classe qui a besoin de référencer un chemin, que ce soit pour le mapper ou pour le comparer,
 * vient s'y servir plutôt que de le recopier en dur.
 * Cela permettra à l'avenir une plus grande fluidité concernant les chemins, par rapport au fait de les renseigner en
 * dur (oublis de renommage, fautes de frappe, ...)
 */
public final class WebPaths {

    private WebPaths() {} // classe utilitaire jamais instanciée

    // Ressources statiques
    public static final String STATIC_CSS = "/css/**";
    public static final String STATIC_JS = "/js/**";
    public static final String WEBJARS = "/webjars/**";
    public static final String H2_CONSOLE = "/h2-console/**";

    // Administration
    public static final String ADMIN = "/admin";
    public static final String USERS = "/users";
    public static final String USERS_FULL = ADMIN + USERS;


    // Gestion de la connexion
    public static final String LOGOUT = "/logout";

    // Gestion de compte
    public static final String ACCOUNT = "/account";
    public static final String CHANGE_PASSWORD = "/change-password";
    public static final String CHANGE_PASSWORD_FULL = ACCOUNT + CHANGE_PASSWORD;


    // Gestion des entrées
    public static final String ENTRIES = "/entries";
    public static final String ENTRY_NEW = "/new";
    public static final String ENTRY_NEW_FULL = ENTRIES + ENTRY_NEW;



}
