package conference.dao.impl;

import conference.config.DatabaseConfig;
import conference.model.Utilisateur;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des utilisateurs
 */
public class UtilisateurDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    public Utilisateur authentifier(String email, String motDePasse) {
        String sql = "SELECT * FROM utilisateurs WHERE email = ? AND actif = TRUE";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Utilisateur u = mapResultSet(rs);
                if (BCrypt.checkpw(motDePasse, u.getMotDePasse())) {
                    // Mettre à jour la dernière connexion
                    updateDerniereConnexion(u.getId());
                    return u;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur authentification: " + e.getMessage());
        }
        return null;
    }

    private void updateDerniereConnexion(int userId) {
        String sql = "UPDATE utilisateurs SET derniere_connexion = NOW() WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur update connexion: " + e.getMessage());
        }
    }

    public List<Utilisateur> findAll() {
        List<Utilisateur> liste = new ArrayList<>();
        String sql = "SELECT * FROM utilisateurs ORDER BY nom, prenom";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findAll utilisateurs: " + e.getMessage());
        }
        return liste;
    }

    public Utilisateur findById(int id) {
        String sql = "SELECT * FROM utilisateurs WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("Erreur findById utilisateur: " + e.getMessage());
        }
        return null;
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM utilisateurs WHERE email = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Erreur emailExists: " + e.getMessage());
        }
        return false;
    }

    public boolean save(Utilisateur u) {
        String sql = "INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, telephone, role, actif) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            // Hasher le mot de passe avec BCrypt
            ps.setString(4, BCrypt.hashpw(u.getMotDePasse(), BCrypt.gensalt(10)));
            ps.setString(5, u.getTelephone());
            ps.setString(6, u.getRole().name());
            ps.setBoolean(7, u.isActif());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) u.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur save utilisateur: " + e.getMessage());
        }
        return false;
    }

    /**
     * Met à jour les informations d'un utilisateur (sans le mot de passe)
     */
    public boolean update(Utilisateur u) {
        String sql = "UPDATE utilisateurs SET nom=?, prenom=?, email=?, telephone=?, role=?, actif=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getTelephone());
            ps.setString(5, u.getRole().name());
            ps.setBoolean(6, u.isActif());
            ps.setInt(7, u.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update utilisateur: " + e.getMessage());
        }
        return false;
    }

    /**
     * Met à jour le mot de passe d'un utilisateur
     */
    public boolean updateMotDePasse(int userId, String nouveauMotDePasse) {
        if (nouveauMotDePasse == null || nouveauMotDePasse.trim().isEmpty()) {
            return false;
        }
        String sql = "UPDATE utilisateurs SET mot_de_passe=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(nouveauMotDePasse, BCrypt.gensalt(10)));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur updateMotDePasse: " + e.getMessage());
        }
        return false;
    }

    /**
     * Met à jour complètement un utilisateur (y compris le mot de passe si fourni)
     * Cette méthode est utile pour l'édition complète d'un utilisateur
     */
    public boolean updateComplet(Utilisateur u, String nouveauMotDePasse) {
        // Si un nouveau mot de passe est fourni, l'utiliser
        if (nouveauMotDePasse != null && !nouveauMotDePasse.trim().isEmpty()) {
            String sql = "UPDATE utilisateurs SET nom=?, prenom=?, email=?, mot_de_passe=?, telephone=?, role=?, actif=? WHERE id=?";
            try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                ps.setString(1, u.getNom());
                ps.setString(2, u.getPrenom());
                ps.setString(3, u.getEmail());
                ps.setString(4, BCrypt.hashpw(nouveauMotDePasse, BCrypt.gensalt(10)));
                ps.setString(5, u.getTelephone());
                ps.setString(6, u.getRole().name());
                ps.setBoolean(7, u.isActif());
                ps.setInt(8, u.getId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("Erreur updateComplet utilisateur: " + e.getMessage());
            }
        } else {
            // Sinon, mettre à jour sans changer le mot de passe
            return update(u);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "UPDATE utilisateurs SET actif=FALSE WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur delete utilisateur: " + e.getMessage());
        }
        return false;
    }

    public int countTotal() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM utilisateurs WHERE actif=TRUE")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur countTotal: " + e.getMessage());
        }
        return 0;
    }

    private Utilisateur mapResultSet(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setTelephone(rs.getString("telephone"));
        u.setRole(Utilisateur.Role.valueOf(rs.getString("role")));
        u.setActif(rs.getBoolean("actif"));
        Timestamp dc = rs.getTimestamp("date_creation");
        if (dc != null) u.setDateCreation(dc.toLocalDateTime());
        Timestamp dl = rs.getTimestamp("derniere_connexion");
        if (dl != null) u.setDerniereConnexion(dl.toLocalDateTime());
        return u;
    }
}