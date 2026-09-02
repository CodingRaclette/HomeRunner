package com.nyxeira.homerunner.entries.ui;

/**
 * Projection légère d'un User, utilisee uniquement pour alimenter le widget de recherche de
 * participants cote client (serialisee en JSON dans la page). On evite volontairement d'exposer
 * l'entite User directement : elle porte passwordHash, mustChangePassword... qui n'ont rien a
 * faire dans le HTML envoyé au navigateur.
 */
public record ParticipantOption(Long id, String name, String login) { }
