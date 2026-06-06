package conference.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuration et gestion de la connexion à la base de données MySQL
 */
public class DatabaseConfig {

    private static DatabaseConfig instance;
    private Connection connection;

    // Paramètres par défaut (peuvent être surchargés par database.properties)
    private String url = "jdbc:mysql://localhost:3306/conference_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";
    private String username = "root";
    private String password = "Tunisie20292029!";

    private DatabaseConfig() {
        loadProperties();
    }

    public static DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                url = props.getProperty("db.url", url);
                username = props.getProperty("db.username", username);
                password = props.getProperty("db.password", password);
            }
        } catch (IOException e) {
            System.out.println("Utilisation des paramètres de connexion par défaut.");
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, username, password);
                connection.setAutoCommit(true);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL non trouvé: " + e.getMessage());
            }
        }
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Erreur fermeture connexion: " + e.getMessage());
            }
        }
    }

    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // Getters/Setters pour configuration dynamique
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; connection = null; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; connection = null; }
    public void setPassword(String password) { this.password = password; connection = null; }
}
