package database;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class Database {

    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String salt;

    public Database() {
        loadProperties();
    }

    private void loadProperties() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("db.properties")) {
            // Dosyayı yükle
            prop.load(input);

            // Değerleri değişkenlere ata
            this.dbUrl = prop.getProperty("db.url");
            this.dbUser = prop.getProperty("db.user");
            this.dbPassword = prop.getProperty("db.password");
            this.salt = prop.getProperty("security.salt");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Yardımcı metot: Bağlantıyı oluşturur
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Veritabanı bağlantısının başarılı olup olmadığını test eder.
     */
    public DbStatus testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                return DbStatus.SUCCESS;
            }
            return DbStatus.CONNECTION_ERROR;
        } catch (SQLException e) {
            e.printStackTrace();
            return DbStatus.CONNECTION_ERROR;
        }
    }

    private String hashPassword(String rawPassword) {
        try {
            String saltedPassword = rawPassword + this.salt;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(saltedPassword.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algoritması bulunamadı!", e);
        }
    }

    /**
     * Yeni kullanıcı kaydı oluşturur.
     */
    public DbStatus registerUser(String email, String rawPassword, String activationCode, String role) {
        String hashedPassword = hashPassword(rawPassword);
        String sql = "INSERT INTO users (email, password_hash, role, activation_code, is_active) VALUES (?, ?, ?, ?, false)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, role);
            pstmt.setString(4, activationCode);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0 ? DbStatus.SUCCESS : DbStatus.QUERY_ERROR;

        } catch (SQLException e) {
            e.printStackTrace();
            // PostgreSQL'de 23505 kodu Unique Violation
            if ("23505".equals(e.getSQLState())) {
                return DbStatus.DUPLICATE_ENTRY;
            }
            // Connection error
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Kullanıcı girişi işlemini yapar.
     * daha bitmedi eklenecek
     */
    public DbStatus login(String email, String rawPassword) {
        String hashedPassword = hashPassword(rawPassword);
        String sql = "SELECT id FROM users WHERE email = ? AND password_hash = ? AND is_active = true";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, hashedPassword);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return DbStatus.SUCCESS;
            } else {
                return DbStatus.AUTHENTICATION_FAILED; // Şifre/Mail yanlış veya hesap aktif değil
            }

        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Kullanıcının girdiği aktivasyon kodunu kontrol eder ve hesabı aktifleştirir.
     * daha bitmedi eklenecek
     */
    public DbStatus verifyActivationCode(String email, String inputCode) {
        String selectSql = "SELECT activation_code FROM users WHERE email = ?";
        String updateSql = "UPDATE users SET is_active = true, activation_code = NULL WHERE email = ?";

        try (Connection conn = getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            selectStmt.setString(1, email);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                String dbCode = rs.getString("activation_code");
                
                if (dbCode == null) {
                    return DbStatus.DATA_NOT_FOUND; // Kullanıcı zaten aktif edilmiş olabilir (kod NULL ise)
                }
                
                if (dbCode.equals(inputCode)) {
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, email);
                        int updated = updateStmt.executeUpdate();
                        return updated > 0 ? DbStatus.SUCCESS : DbStatus.QUERY_ERROR;
                    }
                } else {
                    return DbStatus.INVALID_CODE; // Kodlar eşleşmedi
                }
            } else {
                return DbStatus.DATA_NOT_FOUND; // Veritabanında böyle bir email yok
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            return DbStatus.QUERY_ERROR;
        }
    }
}