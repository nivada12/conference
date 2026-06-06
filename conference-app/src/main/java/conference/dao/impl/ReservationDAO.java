package conference.dao.impl;

import conference.config.DatabaseConfig;
import conference.model.Reservation;
import conference.model.Salle;
import conference.model.Utilisateur;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des réservations
 */
public class ReservationDAO {

    private final SalleDAO salleDAO = new SalleDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    private Connection getConn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ============================================================
    // ===== MÉTHODES DE RÉCUPÉRATION =====
    // ============================================================

    /**
     * ✅ Récupère TOUTES les réservations (pour Admin/Responsable/Dashboard)
     */
    public List<Reservation> findAll() {
        List<Reservation> liste = new ArrayList<>();
        String sql = "SELECT * FROM reservations ORDER BY date_debut DESC";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Reservation r = mapResultSet(rs);
                enrichir(r);
                liste.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findAll: " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    /**
     * ✅ Alias pour compatibilité (DashboardController l'utilise)
     */
    public List<Reservation> findAllComplet() {
        return findAll();
    }

    /**
     * ✅ Récupère les réservations d'un utilisateur spécifique
     */
    public List<Reservation> findByUtilisateur(int utilisateurId) {
        List<Reservation> liste = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE utilisateur_id = ? ORDER BY date_debut DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reservation r = mapResultSet(rs);
                enrichir(r);
                liste.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByUtilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    public List<Reservation> findBySalle(int salleId) {
        List<Reservation> liste = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE salle_id = ? AND statut NOT IN ('ANNULE') ORDER BY date_debut";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, salleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reservation r = mapResultSet(rs);
                enrichir(r);
                liste.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findBySalle: " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    public List<Reservation> findByMois(int annee, int mois) {
        List<Reservation> liste = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE YEAR(date_debut) = ? AND MONTH(date_debut) = ? AND statut NOT IN ('ANNULE') ORDER BY date_debut";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, annee);
            ps.setInt(2, mois);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reservation r = mapResultSet(rs);
                enrichir(r);
                liste.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByMois: " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    public List<Reservation> findByJour(LocalDate date) {
        List<Reservation> liste = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE DATE(date_debut) = ? AND statut NOT IN ('ANNULE') ORDER BY date_debut";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reservation r = mapResultSet(rs);
                enrichir(r);
                liste.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByJour: " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    public Reservation findById(int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Reservation r = mapResultSet(rs);
                enrichir(r);
                return r;
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById reservation: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ============================================================
    // ===== MÉTHODES CRUD =====
    // ============================================================

    public boolean save(Reservation r) {
        String code = "RES-" + java.time.Year.now().getValue() + "-" +
                String.format("%05d", (int)(Math.random() * 99999));
        r.setCodeReservation(code);

        String sql = "INSERT INTO reservations (utilisateur_id, salle_id, titre, description, date_debut, date_fin, nombre_participants, disposition, statut, commentaires, code_reservation) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUtilisateurId());
            ps.setInt(2, r.getSalleId());
            ps.setString(3, r.getTitre());
            ps.setString(4, r.getDescription());
            ps.setTimestamp(5, Timestamp.valueOf(r.getDateDebut()));
            ps.setTimestamp(6, Timestamp.valueOf(r.getDateFin()));
            ps.setInt(7, r.getNombreParticipants());
            ps.setString(8, r.getDisposition() != null ? r.getDisposition().name() : "CONFERENCE");
            ps.setString(9, r.getStatut().name());
            ps.setString(10, r.getCommentaires());
            ps.setString(11, r.getCodeReservation());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) r.setId(keys.getInt(1));
                if (r.getEquipements() != null) {
                    saveEquipements(r.getId(), r.getEquipements());
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur save reservation: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private void saveEquipements(int reservationId, List<conference.model.Equipement> equipements) {
        String sql = "INSERT INTO reservation_equipements (reservation_id, equipement_id, quantite) VALUES (?,?,?) ON DUPLICATE KEY UPDATE quantite=VALUES(quantite)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            for (conference.model.Equipement eq : equipements) {
                if (eq.getQuantiteReservee() > 0) {
                    ps.setInt(1, reservationId);
                    ps.setInt(2, eq.getId());
                    ps.setInt(3, eq.getQuantiteReservee());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (SQLException e) {
            System.err.println("Erreur saveEquipements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean update(Reservation r) {
        String sql = "UPDATE reservations SET titre=?, description=?, date_debut=?, date_fin=?, nombre_participants=?, disposition=?, statut=?, commentaires=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, r.getTitre());
            ps.setString(2, r.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(r.getDateDebut()));
            ps.setTimestamp(4, Timestamp.valueOf(r.getDateFin()));
            ps.setInt(5, r.getNombreParticipants());
            ps.setString(6, r.getDisposition() != null ? r.getDisposition().name() : "CONFERENCE");
            ps.setString(7, r.getStatut().name());
            ps.setString(8, r.getCommentaires());
            ps.setInt(9, r.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update reservation: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateStatut(int id, Reservation.StatutReservation statut) {
        String sql = "UPDATE reservations SET statut=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur updateStatut: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean annuler(int id) {
        return updateStatut(id, Reservation.StatutReservation.ANNULE);
    }

    // ============================================================
    // ===== MÉTHODES STATISTIQUES =====
    // ============================================================

    public int countTotal() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM reservations WHERE statut NOT IN ('ANNULE')")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur countTotal: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public int countAujourdHui() {
        String sql = "SELECT COUNT(*) FROM reservations WHERE DATE(date_debut)=CURDATE() AND statut NOT IN ('ANNULE')";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur countAujourdHui: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public List<Object[]> getStatistiquesParSalle() {
        List<Object[]> stats = new ArrayList<>();
        String sql = "SELECT nom, total_reservations, duree_moyenne_minutes FROM vue_statistiques_salles ORDER BY total_reservations DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stats.add(new Object[]{rs.getString("nom"), rs.getInt("total_reservations"), rs.getDouble("duree_moyenne_minutes")});
            }
        } catch (SQLException e) {
            System.err.println("Erreur getStatistiquesParSalle: " + e.getMessage());
            e.printStackTrace();
        }
        return stats;
    }

    public List<Object[]> getReservationsParMois(int annee) {
        List<Object[]> data = new ArrayList<>();
        String sql = "SELECT MONTH(date_debut) as mois, COUNT(*) as total FROM reservations WHERE YEAR(date_debut)=? AND statut NOT IN ('ANNULE') GROUP BY MONTH(date_debut) ORDER BY mois";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, annee);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new Object[]{rs.getInt(1), rs.getInt(2)});
            }
        } catch (SQLException e) {
            System.err.println("Erreur getReservationsParMois: " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    public int countByUtilisateur(int userId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE utilisateur_id = ? AND statut NOT IN ('ANNULE')";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur countByUtilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public int countByUtilisateurAujourdHui(int userId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE utilisateur_id = ? AND DATE(date_debut) = CURDATE() AND statut NOT IN ('ANNULE')";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur countByUtilisateurAujourdHui: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public List<Object[]> getReservationsParMoisByUtilisateur(int userId, int annee) {
        List<Object[]> data = new ArrayList<>();
        String sql = "SELECT MONTH(date_debut) as mois, COUNT(*) as total FROM reservations WHERE utilisateur_id = ? AND YEAR(date_debut) = ? AND statut NOT IN ('ANNULE') GROUP BY MONTH(date_debut) ORDER BY mois";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, annee);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new Object[]{rs.getInt(1), rs.getInt(2)});
            }
        } catch (SQLException e) {
            System.err.println("Erreur getReservationsParMoisByUtilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    public List<Object[]> getStatistiquesParSalleByUtilisateur(int userId) {
        List<Object[]> stats = new ArrayList<>();
        String sql = "SELECT s.nom, COUNT(*) as total FROM reservations r JOIN salles s ON r.salle_id = s.id WHERE r.utilisateur_id = ? AND r.statut NOT IN ('ANNULE') GROUP BY s.id, s.nom ORDER BY total DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                stats.add(new Object[]{rs.getString("nom"), rs.getInt("total")});
            }
        } catch (SQLException e) {
            System.err.println("Erreur getStatistiquesParSalleByUtilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        return stats;
    }

    // ============================================================
    // ===== MÉTHODES UTILITAIRES =====
    // ============================================================

    private void enrichir(Reservation r) {
        if (r.getSalleId() > 0) r.setSalle(salleDAO.findById(r.getSalleId()));
        if (r.getUtilisateurId() > 0) r.setUtilisateur(utilisateurDAO.findById(r.getUtilisateurId()));
    }

    private Reservation mapResultSet(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setUtilisateurId(rs.getInt("utilisateur_id"));
        r.setSalleId(rs.getInt("salle_id"));
        r.setTitre(rs.getString("titre"));
        r.setDescription(rs.getString("description"));
        Timestamp dd = rs.getTimestamp("date_debut");
        if (dd != null) r.setDateDebut(dd.toLocalDateTime());
        Timestamp df = rs.getTimestamp("date_fin");
        if (df != null) r.setDateFin(df.toLocalDateTime());
        r.setNombreParticipants(rs.getInt("nombre_participants"));
        String disp = rs.getString("disposition");
        if (disp != null) r.setDisposition(Salle.Disposition.valueOf(disp));
        String statut = rs.getString("statut");
        if (statut != null) r.setStatut(Reservation.StatutReservation.valueOf(statut));
        r.setCommentaires(rs.getString("commentaires"));
        r.setCodeReservation(rs.getString("code_reservation"));
        Timestamp dc = rs.getTimestamp("date_creation");
        if (dc != null) r.setDateCreation(dc.toLocalDateTime());
        return r;
    }
}