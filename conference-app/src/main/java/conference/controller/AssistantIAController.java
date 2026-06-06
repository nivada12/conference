package conference.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import conference.model.AIMessage;
import conference.model.AIMessage.Expediteur;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur Assistant IA — Groq API (compatible OpenAI)
 * Utilise Gson (déjà dans pom.xml) + java.net.http (Java 11+)
 * ✅ Aucune dépendance Maven supplémentaire nécessaire
 */
public class AssistantIAController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private VBox       vboxMessages;
    @FXML private ScrollPane scrollPane;
    @FXML private TextArea   txtInput;
    @FXML private Button     btnEnvoyer;
    @FXML private HBox       hboxChargement;
    @FXML private Label      lblNbMessages;
    @FXML private Label      lblStatutAI;

    // ═════════════════════════════════════════════════════════════════════════
    //  ⚙️  PARAMÈTRES À MODIFIER ICI UNIQUEMENT
    // ═════════════════════════════════════════════════════════════════════════
    private static final String API_KEY = "";   // ← REMPLACEZ PAR VOTRE CLÉ GROQ
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";   // ← Changez le modèle si besoin
    // Autres modèles disponibles : "mixtral-8x7b-32768", "gemma2-9b-it", "llama-3.1-8b-instant"
    // ═════════════════════════════════════════════════════════════════════════

    private static final String SYSTEM_PROMPT =
            "Tu es un assistant intelligent intégré dans un système de gestion de réservation " +
                    "de salles de conférence. Tu aides les utilisateurs à : gérer leurs réservations, " +
                    "trouver des salles disponibles, comprendre les statistiques, résoudre des problèmes " +
                    "liés aux équipements, et optimiser l'utilisation des salles. " +
                    "Réponds toujours en français, de manière concise et utile.";

    // ── État ──────────────────────────────────────────────────────────────────
    private final List<AIMessage> historiqueMessages = new ArrayList<>();
    private final HttpClient      httpClient;
    private final Gson            gson;
    private       boolean         enAttente = false;

    // ── Couleurs bulles ───────────────────────────────────────────────────────
    private static final String COULEUR_USER      = "#1e40af";
    private static final String COULEUR_ASSISTANT = "#f1f5f9";
    private static final String TEXTE_ASSISTANT   = "#1e293b";

    // ── Constructeur ──────────────────────────────────────────────────────────
    public AssistantIAController() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }

    // ── Initialisation ────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Entrée = envoyer / Shift+Entrée = nouvelle ligne
        txtInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                envoyerMessage();
            }
        });

        ajouterBulleAssistant(
                "👋 Bonjour ! Je suis votre assistant IA (Groq — " + MODEL + ").\n\n" +
                        "Je peux vous aider à :\n" +
                        "• Consulter vos réservations\n" +
                        "• Trouver des salles disponibles\n" +
                        "• Analyser les statistiques\n" +
                        "• Résoudre des problèmes d'équipements\n\n" +
                        "Comment puis-je vous aider ?"
        );
    }

    // ── Actions FXML ──────────────────────────────────────────────────────────

    @FXML
    private void envoyerMessage() {
        String texte = txtInput.getText().trim();
        if (texte.isEmpty() || enAttente) return;
        txtInput.clear();
        traiterMessage(texte);
    }

    @FXML
    private void effacerConversation() {
        vboxMessages.getChildren().clear();
        historiqueMessages.clear();
        mettreAJourCompteur();
        setStatut("● Prêt", "#22c55e");
        ajouterBulleAssistant("🔄 Conversation effacée. Comment puis-je vous aider ?");
    }

    @FXML
    private void copierConversation() {
        if (historiqueMessages.isEmpty()) return;
        StringBuilder sb = new StringBuilder("=== Conversation IA ===\n\n");
        for (AIMessage msg : historiqueMessages) {
            sb.append(msg.estUtilisateur() ? "Vous" : "Assistant")
                    .append(" [").append(msg.getHorodatageFormate()).append("] :\n")
                    .append(msg.getContenu()).append("\n\n");
        }
        javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(sb.toString());
        cb.setContent(cc);
        afficherNotification("✅ Copié dans le presse-papiers !");
    }

    // ── Suggestions rapides ───────────────────────────────────────────────────

    @FXML private void suggestionReservations() { traiterMessage("Quelles sont mes prochaines réservations ?"); }
    @FXML private void suggestionSalles()       { traiterMessage("Quelles salles sont disponibles aujourd'hui ?"); }
    @FXML private void suggestionStatistiques() { traiterMessage("Donne-moi un résumé des statistiques du système."); }
    @FXML private void suggestionEquipements()  { traiterMessage("Y a-t-il des équipements défectueux ?"); }
    @FXML private void suggestionConseils()     { traiterMessage("Donne-moi des conseils pour optimiser l'utilisation des salles."); }

    // ── Logique principale ────────────────────────────────────────────────────

    private void traiterMessage(String texte) {
        AIMessage msgUser = new AIMessage(texte, Expediteur.UTILISATEUR);
        historiqueMessages.add(msgUser);
        ajouterBulleUtilisateur(texte, msgUser.getHorodatageFormate());
        mettreAJourCompteur();
        setEnAttente(true);

        CompletableFuture.supplyAsync(() -> {
            try {
                return appellerGroq();
            } catch (Exception e) {
                return "❌ Erreur : " + e.getMessage();
            }
        }).thenAccept(reponse -> Platform.runLater(() -> {
            AIMessage msgIA = new AIMessage(reponse, Expediteur.ASSISTANT);
            historiqueMessages.add(msgIA);
            ajouterBulleAssistant(reponse);
            mettreAJourCompteur();
            setEnAttente(false);
        }));
    }

    /**
     * Appelle l'API Groq (format OpenAI)
     * Format : { "model": "...", "messages": [ { "role": "system"/"user"/"assistant", "content": "..." } ] }
     */
    private String appellerGroq() throws IOException, InterruptedException {

        JsonArray messages = new JsonArray();

        // System prompt
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", SYSTEM_PROMPT);
        messages.add(sysMsg);

        // Historique de conversation
        for (AIMessage msg : historiqueMessages) {
            JsonObject message = new JsonObject();
            // Groq/OpenAI utilise "user" et "assistant" (pas "model")
            message.addProperty("role", msg.estUtilisateur() ? "user" : "assistant");
            message.addProperty("content", msg.getContenu());
            messages.add(message);
        }

        // Corps de la requête
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);
        body.addProperty("max_tokens", 1024);
        body.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " : " + response.body());
        }

        return extraireTexteGroq(response.body());
    }

    /**
     * Extrait le texte de la réponse JSON Groq/OpenAI avec Gson
     */
    private String extraireTexteGroq(String jsonReponse) {
        try {
            JsonObject root    = gson.fromJson(jsonReponse, JsonObject.class);
            JsonArray  choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return "Réponse vide.";

            JsonObject choice    = choices.get(0).getAsJsonObject();
            JsonObject message   = choice.getAsJsonObject("message");
            String     content   = message.get("content").getAsString();

            return content;
        } catch (Exception e) {
            return "Erreur parsing réponse : " + e.getMessage();
        }
    }

    // ── Construction des bulles ───────────────────────────────────────────────

    private void ajouterBulleUtilisateur(String texte, String heure) {
        VBox bulle = new VBox(4);
        bulle.setAlignment(Pos.CENTER_RIGHT);
        bulle.setPadding(new Insets(4, 0, 4, 60));

        Label lblTexte = new Label(texte);
        lblTexte.setWrapText(true);
        lblTexte.setMaxWidth(480);
        lblTexte.setStyle(
                "-fx-background-color: " + COULEUR_USER + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10 14 10 14;" +
                        "-fx-background-radius: 18 18 4 18;" +
                        "-fx-font-size: 13px;"
        );

        Label lblHeure = new Label(heure);
        lblHeure.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        bulle.getChildren().addAll(lblTexte, lblHeure);
        vboxMessages.getChildren().add(bulle);
        scrollerEnBas();
    }

    private void ajouterBulleAssistant(String texte) {
        String heure = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        VBox bulle = new VBox(4);
        bulle.setAlignment(Pos.CENTER_LEFT);
        bulle.setPadding(new Insets(4, 60, 4, 0));

        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 18px;");

        Label lblTexte = new Label(texte);
        lblTexte.setWrapText(true);
        lblTexte.setMaxWidth(480);
        lblTexte.setStyle(
                "-fx-background-color: " + COULEUR_ASSISTANT + ";" +
                        "-fx-text-fill: " + TEXTE_ASSISTANT + ";" +
                        "-fx-padding: 10 14 10 14;" +
                        "-fx-background-radius: 18 18 18 4;" +
                        "-fx-font-size: 13px;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 18 18 18 4;"
        );

        Label lblHeure = new Label(heure);
        lblHeure.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        HBox ligne = new HBox(8);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.getChildren().addAll(avatar, lblTexte);

        bulle.getChildren().addAll(ligne, lblHeure);
        vboxMessages.getChildren().add(bulle);
        scrollerEnBas();
    }

    private void afficherNotification(String message) {
        Label notif = new Label(message);
        notif.setStyle(
                "-fx-background-color: #1e40af; -fx-text-fill: white;" +
                        "-fx-padding: 6 12; -fx-background-radius: 8; -fx-font-size: 12px;"
        );
        HBox hbox = new HBox(notif);
        hbox.setAlignment(Pos.CENTER);
        vboxMessages.getChildren().add(hbox);
        scrollerEnBas();
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> vboxMessages.getChildren().remove(hbox));
        }).start();
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void setEnAttente(boolean attente) {
        enAttente = attente;
        btnEnvoyer.setDisable(attente);
        txtInput.setDisable(attente);
        hboxChargement.setVisible(attente);
        hboxChargement.setManaged(attente);
        setStatut(attente ? "● En cours..." : "● Prêt",
                attente ? "#f59e0b"        : "#22c55e");
    }

    private void setStatut(String texte, String couleur) {
        lblStatutAI.setText(texte);
        lblStatutAI.setStyle("-fx-text-fill: " + couleur + "; -fx-font-weight: bold;");
    }

    private void mettreAJourCompteur() {
        lblNbMessages.setText(String.valueOf(historiqueMessages.size()));
    }

    private void scrollerEnBas() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    // ── Méthode statique pour le Dashboard ───────────────────────────────────

    public static CompletableFuture<String> questionRapide(String question) {
        AssistantIAController tmp = new AssistantIAController();
        tmp.historiqueMessages.add(new AIMessage(question, Expediteur.UTILISATEUR));
        return CompletableFuture.supplyAsync(() -> {
            try {
                return tmp.appellerGroq();
            } catch (Exception e) {
                return "❌ Erreur IA : " + e.getMessage();
            }
        });
    }
}