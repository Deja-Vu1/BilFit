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
import java.util.UUID;

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

    private String generateRandomCode(int length) {
        String chars = "0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
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
     * Registers a new student to the database.
     * Checks for duplicate email or student ID before insertion.
     * @param name Student's full name
     * @param bilkentMail Student's Bilkent email address
     * @param studentId Student's ID number
     * @param password Student's password (should be hashed before calling this method)
     * @return DbStatus indicating SUCCESS, EMAIL_ALREADY_EXISTS, ID_ALREADY_EXISTS, etc.
     */
    public DbStatus registerStudent(String name, String bilkentMail, String studentId, String password) {
        // Çakışma kontrolü için SQL
        String checkSql = "SELECT bilkent_email, student_id FROM users WHERE bilkent_email = ? OR student_id = ?";
        
        // Yeni kullanıcı ekleme SQL'i (is_activated varsayılan olarak FALSE ile başlatılır)
        String insertSql = "INSERT INTO users (name, bilkent_email, student_id, password, is_activated) VALUES (?, ?, ?, ?, FALSE)";

        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            // 1. ADIM: E-posta veya Öğrenci ID'si sistemde zaten kayıtlı mı kontrolü
            checkStmt.setString(1, bilkentMail);
            checkStmt.setString(2, studentId);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    String existingEmail = rs.getString("bilkent_email");
                    String existingId = rs.getString("student_id");

                    // Hangisinin çakıştığını bulup uygun durumu döndürüyoruz
                    if (bilkentMail.equals(existingEmail)) {
                        return DbStatus.EMAIL_ALREADY_EXISTS; 
                    } else if (studentId.equals(existingId)) {
                        return DbStatus.ID_ALREADY_EXISTS; 
                    }
                }
            }

            // 2. ADIM: Herhangi bir çakışma yoksa, kayıt işlemi (INSERT)
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, name);
                insertStmt.setString(2, bilkentMail);
                insertStmt.setString(3, studentId);
                insertStmt.setString(4, password); 

                int insertedRows = insertStmt.executeUpdate();
                return insertedRows > 0 ? DbStatus.SUCCESS : DbStatus.QUERY_ERROR;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            
            // Veritabanı bağlantı hatası
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            // PostgreSQL Unique Constraint Violation Error Code (23505)
            // Eğer üstteki SELECT işlemi bir anlık atlanır ve veritabanı kendi kısıtlamasıyla (UNIQUE) hata fırlatırsa yakalarız
            if ("23505".equals(e.getSQLState())) {
                 return DbStatus.QUERY_ERROR; // veya sisteminde varsa DbStatus.DATA_ALREADY_EXISTS
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
                return DbStatus.AUTHENTICATION_FAILED; // Wrong email/password or account not active
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
     * Modify user's profile to set is_activated to true
     * @param email User's email address
     * @return DbStatus (SUCCESS, DATA_NOT_FOUND, CONNECTION_ERROR, QUERY_ERROR)
     */
    public DbStatus setProfileActivation(String email) {
        String updateSql = "UPDATE users SET is_activated = TRUE WHERE bilkent_email = ?";

        try (Connection conn = getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            updateStmt.setString(1, email);
            int updated = updateStmt.executeUpdate();
            
            // Eğer 0 satır güncellendiyse, e-posta adresi tabloda yok demektir
            return updated > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Verify the activation code provided by the user against the database record.
     * If you want to set the user's profile as active after successful verification, you can call setProfileActivation() method separately.
     * activation of the account is optional becuase verification code can be used for other purposes as well, such as password reset. So we keep them separate.
     * It only verify 30 minutes old activation codes to prevent abuse.
     * After verification, if the code is correct, method automatically deletes the activation code from the database to prevent reuse.
     * @param email User's email address
     * @param inputCode Activation code provided by the user
     * @return DbStatus (SUCCESS, INVALID_CODE, EXPIRED_CODE, DATA_NOT_FOUND, CONNECTION_ERROR, QUERY_ERROR)
     */
    public DbStatus verifyActivationCode(String email, String inputCode) {
        String selectSql = "SELECT a.activation_code FROM activation a INNER JOIN users u ON a.user_id = u.id WHERE u.bilkent_email = ? AND a.created_at >= NOW() - INTERVAL '30 minutes'";
        
        String deleteSql = "DELETE FROM activation WHERE user_id = (SELECT id FROM users WHERE bilkent_email = ?)";

        try (Connection conn = getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            selectStmt.setString(1, email);
            
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    String dbCode = rs.getString("activation_code");
                    
                    if (dbCode == null) {
                        return DbStatus.DATA_NOT_FOUND;
                    }
                    
                    if (dbCode.equals(inputCode)) {
                        // KOD DOĞRU! Silme işlemini (DELETE) gerçekleştir:
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setString(1, email);
                            deleteStmt.executeUpdate();
                        }
                        
                        return DbStatus.SUCCESS; // Doğrulandı ve kod silindi
                    } else {
                        return DbStatus.INVALID_CODE; // Yanlış kod girildi
                    }
                } else {
                    // Kayıt yoksa ya hiç istenmemiştir ya da 30 dakikayı geçtiği için sorgu getirmemiştir
                    return DbStatus.EXPIRED_CODE; 
                }
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
     * Create or update an activation code for a user in the database.
     * Also sends the activation code to the user's email address.
     * @param email User's email address
     * @return DbStatus indicating the result of the operation
     */
    public DbStatus createActivationCode(String email) {
        String findUserSql = "SELECT id FROM users WHERE bilkent_email = ?";
        String updateActivationSql = "UPDATE activation SET activation_code = ?, created_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        String insertActivationSql = "INSERT INTO activation (user_id, activation_code) VALUES (?, ?)";
        String activationCode = generateRandomCode(6); // Creates a random 6-digit code

        try (Connection conn = getConnection();
            PreparedStatement findUserStmt = conn.prepareStatement(findUserSql)) {
            EmailService emailService = new EmailService();
            findUserStmt.setString(1, email);
            ResultSet rs = findUserStmt.executeQuery();

            if (rs.next()) {
                // 1. DÜZELTME: UUID'yi ResultSet'ten Object olarak alıp cast ediyoruz
                // PostgreSQL sürücüsü UUID'yi doğrudan java.util.UUID olarak döndürür
                UUID userId = rs.getObject("id", UUID.class);

                try (PreparedStatement updateStmt = conn.prepareStatement(updateActivationSql)) {
                    updateStmt.setString(1, activationCode);
                    
                    // 2. DÜZELTME: PreparedStatement içine UUID'yi setObject ile yerleştiriyoruz
                    updateStmt.setObject(2, userId);
                    int updatedRows = updateStmt.executeUpdate();

                    if (updatedRows == 0) {
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertActivationSql)) {
                            // 3. DÜZELTME: INSERT için de setObject kullanıyoruz
                            insertStmt.setObject(1, userId);
                            insertStmt.setString(2, activationCode);
                            int insertedRows = insertStmt.executeUpdate();
                            if(insertedRows > 0) {
                                emailService.sendActivationEmail(email, activationCode);
                            }
                            return insertedRows > 0 ? DbStatus.SUCCESS : DbStatus.QUERY_ERROR;
                        }
                    }
                    emailService.sendActivationEmail(email, activationCode);
                    return DbStatus.SUCCESS;
                }
            } else {
                return DbStatus.DATA_NOT_FOUND;
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