package conference.controller;

import conference.dao.impl.NotificationDAO;
import conference.model.Notification;
import conference.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationsController implements Initializable {

    @FXML private VBox listNotifications;
    @FXML private Label lblTotalNonLues;

    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadNotifications();
    }

    private void loadNotifications() {
        int userId = SessionManager.getInstance().getUserId();
        List<Notification> notifications = notificationDAO.findByUtilisateur(userId);
        long nonLues = notifications.stream().filter(n -> !n.isLu()).count();
        
        if (lblTotalNonLues != null) lblTotalNonLues.setText(nonLues + " notification(s) non lue(s)");
        
        if (listNotifications != null) {
            listNotifications.getChildren().clear();
            for (Notification n : notifications) {
                listNotifications.getChildren().add(createNotifCard(n));
            }
            if (notifications.isEmpty()) {
                Label lbl = new Label("Aucune notification");
                lbl.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 14px; -fx-padding: 20;");
                listNotifications.getChildren().add(lbl);
            }
        }
    }

    private VBox createNotifCard(Notification n) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle(n.isLu() ?
            "-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 1);" :
            "-fx-background-color: #E3F2FD; -fx-background-radius: 8; -fx-border-left-color: #1976D2; -fx-border-left-width: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);");
        
        HBox header = new HBox(8);
        Label icone = new Label(n.getType().getIcone());
        icone.setStyle("-fx-font-size: 16px;");
        Label titre = new Label(n.getTitre());
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1A237E;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label date = new Label(n.getDateEnvoi() != null ? n.getDateEnvoi().format(fmt) : "");
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #90A4AE;");
        
        Button btnLire = new Button(n.isLu() ? "✓ Lu" : "Marquer comme lu");
        btnLire.setStyle("-fx-background-color: transparent; -fx-text-fill: #1976D2; -fx-font-size: 11px; -fx-cursor: hand;");
        if (!n.isLu()) btnLire.setOnAction(e -> { notificationDAO.marquerLu(n.getId()); loadNotifications(); });
        
        header.getChildren().addAll(icone, titre, spacer, date, btnLire);
        
        Label message = new Label(n.getMessage());
        message.setStyle("-fx-font-size: 12px; -fx-text-fill: #37474F; -fx-wrap-text: true;");
        message.setWrapText(true);
        
        card.getChildren().addAll(header, message);
        VBox.setMargin(card, new Insets(0, 0, 8, 0));
        return card;
    }

    @FXML private void handleMarquerToutesLues() {
        notificationDAO.marquerToutesLues(SessionManager.getInstance().getUserId());
        loadNotifications();
        SessionManager.showInfo("Succès", "Toutes les notifications ont été marquées comme lues.");
    }

    @FXML private void handleRafraichir() { loadNotifications(); }
}
