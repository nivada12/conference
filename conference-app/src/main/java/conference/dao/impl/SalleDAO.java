package conference.dao.impl;

import conference.config.DatabaseConfig;
import conference.model.Salle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des salles
 */
public class SalleDAO {
    
    private Connection getConn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    public List<Salle> findAll() {
        List<Salle> liste = new ArrayList<>();
        String sql = "SELECT * FROM salles ORDER BY nom";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur findAll salles: " + e.getMessage());
        }
        return liste;
    }

    public List<Salle> findActives() {
        List<Salle> liste = new ArrayList<>();
        String sql = "SELECT * FROM salles WHERE actif=TRUE ORDER BY nom";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur findActives salles: " + e.getMessage());
        }
        return liste;
    }

    public List<Salle> findDisponibles(Timestamp dateDebut, Timestamp dateFin) {
        List<Salle> liste = new ArrayList<>();
        String sql = """
            SELECT s.* FROM salles s
            WHERE s.actif = TRUE
            AND s.id NOT IN (
                SELECT r.salle_id FROM reservations r
                WHERE r.statut NOT IN ('ANNULE')
                AND NOT (r.date_fin <= ? OR r.date_debut >= ?)
            )
            ORDER BY s.nom
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, dateDebut);
            ps.setTimestamp(2, dateFin);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur findDisponibles: " + e.getMessage());
        }
        return liste;
    }

    public Salle findById(int id) {
        String sql = "SELECT * FROM salles WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("Erreur findById salle: " + e.getMessage());
        }
        return null;
    }

    public boolean save(Salle s) {
        String sql = "INSERT INTO salles (nom, description, capacite, localisation, etage, superficie, disposition_defaut, actif) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getNom());
            ps.setString(2, s.getDescription());
            ps.setInt(3, s.getCapacite());
            ps.setString(4, s.getLocalisation());
            ps.setString(5, s.getEtage());
            ps.setDouble(6, s.getSuperficie());
            ps.setString(7, s.getDispositionDefaut() != null ? s.getDispositionDefaut().name() : "CONFERENCE");
            ps.setBoolean(8, s.isActif());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) s.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur save salle: " + e.getMessage());
        }
        return false;
    }

    public boolean update(Salle s) {
        String sql = "UPDATE salles SET nom=?, description=?, capacite=?, localisation=?, etage=?, superficie=?, disposition_defaut=?, actif=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, s.getNom());
            ps.setString(2, s.getDescription());
            ps.setInt(3, s.getCapacite());
            ps.setString(4, s.getLocalisation());
            ps.setString(5, s.getEtage());
            ps.setDouble(6, s.getSuperficie());
            ps.setString(7, s.getDispositionDefaut() != null ? s.getDispositionDefaut().name() : "CONFERENCE");
            ps.setBoolean(8, s.isActif());
            ps.setInt(9, s.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update salle: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "UPDATE salles SET actif=FALSE WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur delete salle: " + e.getMessage());
        }
        return false;
    }

    public int countTotal() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM salles WHERE actif=TRUE")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur countTotal salles: " + e.getMessage());
        }
        return 0;
    }

    public boolean isDisponible(int salleId, Timestamp dateDebut, Timestamp dateFin, Integer excludeReservationId) {
        String sql = """
            SELECT COUNT(*) FROM reservations 
            WHERE salle_id = ? AND statut NOT IN ('ANNULE')
            AND id != COALESCE(?, -1)
            AND NOT (date_fin <= ? OR date_debut >= ?)
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, salleId);
            if (excludeReservationId != null) ps.setInt(2, excludeReservationId);
            else ps.setNull(2, Types.INTEGER);
            ps.setTimestamp(3, dateDebut);
            ps.setTimestamp(4, dateFin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) == 0;
        } catch (SQLException e) {
            System.err.println("Erreur isDisponible: " + e.getMessage());
        }
        return false;
    }

    private Salle mapResultSet(ResultSet rs) throws SQLException {
        Salle s = new Salle();
        s.setId(rs.getInt("id"));
        s.setNom(rs.getString("nom"));
        s.setDescription(rs.getString("description"));
        s.setCapacite(rs.getInt("capacite"));
        s.setLocalisation(rs.getString("localisation"));
        s.setEtage(rs.getString("etage"));
        s.setSuperficie(rs.getDouble("superficie"));
        String disp = rs.getString("disposition_defaut");
        if (disp != null) s.setDispositionDefaut(Salle.Disposition.valueOf(disp));
        s.setActif(rs.getBoolean("actif"));
        s.setImagePath(rs.getString("image_path"));
        Timestamp dc = rs.getTimestamp("date_creation");
        if (dc != null) s.setDateCreation(dc.toLocalDateTime());
        return s;
    }
}
