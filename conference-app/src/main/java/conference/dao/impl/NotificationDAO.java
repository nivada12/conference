package conference.dao.impl;

import conference.config.DatabaseConfig;
import conference.model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des notifications
 */
public class NotificationDAO {
    
    private Connection getConn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    public List<Notification> findByUtilisateur(int utilisateurId) {
        List<Notification> liste = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE utilisateur_id=? ORDER BY date_envoi DESC LIMIT 50";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur findByUtilisateur notifications: " + e.getMessage());
        }
        return liste;
    }

    public int countNonLues(int utilisateurId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE utilisateur_id=? AND lu=FALSE";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur countNonLues: " + e.getMessage());
        }
        return 0;
    }

    public boolean save(Notification n) {
        String sql = "INSERT INTO notifications (utilisateur_id, reservation_id, type, titre, message) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, n.getUtilisateurId());
            if (n.getReservationId() != null) ps.setInt(2, n.getReservationId());
            else ps.setNull(2, Types.INTEGER);
            ps.setString(3, n.getType().name());
            ps.setString(4, n.getTitre());
            ps.setString(5, n.getMessage());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) n.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur save notification: " + e.getMessage());
        }
        return false;
    }

    public boolean marquerLu(int id) {
        String sql = "UPDATE notifications SET lu=TRUE, date_lecture=NOW() WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur marquerLu: " + e.getMessage());
        }
        return false;
    }

    public boolean marquerToutesLues(int utilisateurId) {
        String sql = "UPDATE notifications SET lu=TRUE, date_lecture=NOW() WHERE utilisateur_id=? AND lu=FALSE";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            System.err.println("Erreur marquerToutesLues: " + e.getMessage());
        }
        return false;
    }

    private Notification mapResultSet(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUtilisateurId(rs.getInt("utilisateur_id"));
        int rid = rs.getInt("reservation_id");
        if (!rs.wasNull()) n.setReservationId(rid);
        n.setType(Notification.TypeNotification.valueOf(rs.getString("type")));
        n.setTitre(rs.getString("titre"));
        n.setMessage(rs.getString("message"));
        n.setLu(rs.getBoolean("lu"));
        Timestamp de = rs.getTimestamp("date_envoi");
        if (de != null) n.setDateEnvoi(de.toLocalDateTime());
        Timestamp dl = rs.getTimestamp("date_lecture");
        if (dl != null) n.setDateLecture(dl.toLocalDateTime());
        return n;
    }
}
