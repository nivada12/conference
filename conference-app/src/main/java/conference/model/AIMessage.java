package conference.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modèle représentant un message dans la conversation avec l'IA
 */
public class AIMessage {

    public enum Expediteur {
        UTILISATEUR, ASSISTANT
    }

    private final String contenu;
    private final Expediteur expediteur;
    private final LocalDateTime horodatage;

    public AIMessage(String contenu, Expediteur expediteur) {
        this.contenu     = contenu;
        this.expediteur  = expediteur;
        this.horodatage  = LocalDateTime.now();
    }

    // ── Getters ──────────────────────────────────────────────
    public String getContenu()         { return contenu; }
    public Expediteur getExpediteur()  { return expediteur; }
    public LocalDateTime getHorodatage() { return horodatage; }

    public boolean estUtilisateur()  { return expediteur == Expediteur.UTILISATEUR; }
    public boolean estAssistant()    { return expediteur == Expediteur.ASSISTANT; }

    public String getHorodatageFormate() {
        return horodatage.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /** Format JSON attendu par l'API Claude / OpenAI */
    public String getRoleAPI() {
        return expediteur == Expediteur.UTILISATEUR ? "user" : "assistant";
    }

    @Override
    public String toString() {
        return "[" + getHorodatageFormate() + "] " + expediteur + ": " + contenu;
    }
}