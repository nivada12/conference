package conference.dao.impl;

import conference.config.DatabaseConfig;
import conference.model.Equipement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des équipements
 */
public class EquipementDAO {
    
    private Connection getConn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    public List<Equipement> findAll() {
        List<Equipement> liste = new ArrayList<>();
        String sql = "SELECT * FROM equipements ORDER BY type, nom";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur findAll equipements: " + e.getMessage());
        }
        return liste;
    }

    public List<Equipement> findActifs() {
        List<Equipement> liste = new ArrayList<>();
        String sql = "SELECT * FROM equipements WHERE actif=TRUE ORDER BY type, nom";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur findActifs equipements: " + e.getMessage());
        }
        return liste;
    }

    public List<Equipement> findBySalle(int salleId) {
        List<Equipement> liste = new ArrayList<>();
        String sql = "SELECT e.*, se.quantite as quantite_salle FROM equipements e JOIN salle_equipements se ON e.id=se.equipement_id WHERE se.salle_id=? AND e.actif=TRUE";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, salleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Equipement eq = mapResultSet(rs);
                liste.add(eq);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findBySalle: " + e.getMessage());
        }
        return liste;
    }

    public Equipement findById(int id) {
        String sql = "SELECT * FROM equipements WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("Erreur findById equipement: " + e.getMessage());
        }
        return null;
    }

    public boolean save(Equipement eq) {
        String sql = "INSERT INTO equipements (nom, description, type, quantite_totale, quantite_disponible, etat, numero_serie, actif) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, eq.getNom());
            ps.setString(2, eq.getDescription());
            ps.setString(3, eq.getType().name());
            ps.setInt(4, eq.getQuantiteTotale());
            ps.setInt(5, eq.getQuantiteDisponible());
            ps.setString(6, eq.getEtat().name());
            ps.setString(7, eq.getNumeroSerie());
            ps.setBoolean(8, eq.isActif());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) eq.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur save equipement: " + e.getMessage());
        }
        return false;
    }

    public boolean update(Equipement eq) {
        String sql = "UPDATE equipements SET nom=?, description=?, type=?, quantite_totale=?, quantite_disponible=?, etat=?, numero_serie=?, actif=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, eq.getNom());
            ps.setString(2, eq.getDescription());
            ps.setString(3, eq.getType().name());
            ps.setInt(4, eq.getQuantiteTotale());
            ps.setInt(5, eq.getQuantiteDisponible());
            ps.setString(6, eq.getEtat().name());
            ps.setString(7, eq.getNumeroSerie());
            ps.setBoolean(8, eq.isActif());
            ps.setInt(9, eq.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update equipement: " + e.getMessage());
        }
        return false;
    }

    public boolean signalerDefaut(int equipementId, int utilisateurId, Integer reservationId, String description) {
        String sql = "INSERT INTO signalements (equipement_id, utilisateur_id, reservation_id, description) VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, equipementId);
            ps.setInt(2, utilisateurId);
            if (reservationId != null) ps.setInt(3, reservationId);
            else ps.setNull(3, Types.INTEGER);
            ps.setString(4, description);
            if (ps.executeUpdate() > 0) {
                // Marquer comme défectueux
                String updateSql = "UPDATE equipements SET etat='DEFECTUEUX' WHERE id=?";
                try (PreparedStatement ps2 = getConn().prepareStatement(updateSql)) {
                    ps2.setInt(1, equipementId);
                    ps2.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur signalerDefaut: " + e.getMessage());
        }
        return false;
    }

    public int countDefectueux() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM equipements WHERE etat='DEFECTUEUX' AND actif=TRUE")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur countDefectueux: " + e.getMessage());
        }
        return 0;
    }

    private Equipement mapResultSet(ResultSet rs) throws SQLException {
        Equipement eq = new Equipement();
        eq.setId(rs.getInt("id"));
        eq.setNom(rs.getString("nom"));
        eq.setDescription(rs.getString("description"));
        String type = rs.getString("type");
        if (type != null) eq.setType(Equipement.TypeEquipement.valueOf(type));
        eq.setQuantiteTotale(rs.getInt("quantite_totale"));
        eq.setQuantiteDisponible(rs.getInt("quantite_disponible"));
        String etat = rs.getString("etat");
        if (etat != null) eq.setEtat(Equipement.EtatEquipement.valueOf(etat));
        eq.setNumeroSerie(rs.getString("numero_serie"));
        eq.setActif(rs.getBoolean("actif"));
        return eq;
    }
}
