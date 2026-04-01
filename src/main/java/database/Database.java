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
    private Connection conn;

    public Database() {
        loadProperties();
        try {
            getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
    private void getConnection() throws SQLException {
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
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
        try{
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
     * Before insertion, it runs a Garbage Collector to delete any unactivated accounts older than 30 minutes.
     * Checks for duplicate email or student ID before insertion.
     * Inserts the user into 'users' table, retrieves the generated UUID, and creates a corresponding profile in 'students' table.
     * Finally, it creates an activation code and sends it via email.
     * @param name Student's full name
     * @param bilkentMail Student's Bilkent email address
     * @param studentId Student's ID number
     * @param passwordHash Student's hashed password
     * @return DbStatus indicating SUCCESS, EMAIL_ALREADY_EXISTS, ID_ALREADY_EXISTS, etc.
     */
    public DbStatus registerStudent(String name, String bilkentMail, String studentId, String passwordHash) {
        
        String gcSql = "DELETE FROM users WHERE is_activated = FALSE AND created_at < NOW() - INTERVAL '30 minutes'";
        String checkSql = "SELECT bilkent_email, student_id FROM users WHERE bilkent_email = ? OR student_id = ?";
        
        String insertUserSql = "INSERT INTO users (full_name, bilkent_email, student_id, password_hash, role, is_activated) VALUES (?, ?, ?, ?, 'student', FALSE) RETURNING id";
        
        String insertStudentSql = "INSERT INTO students (user_id, elo_point, penalty_points, realibility_score, matches_played, win_rate, is_public_profile, is_elo_matchmaking_enabled) " +
                                  "VALUES (?, 1000, 0, 100.0, 0, 0.0, TRUE, TRUE)";

        try{

            try (PreparedStatement gcStmt = conn.prepareStatement(gcSql)) {
                gcStmt.executeUpdate();
            }

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, bilkentMail);
                checkStmt.setString(2, studentId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        String existingEmail = rs.getString("bilkent_email");
                        String existingId = rs.getString("student_id");

                        if (bilkentMail.equals(existingEmail)) {
                            return DbStatus.EMAIL_ALREADY_EXISTS; 
                        } else if (studentId.equals(existingId)) {
                            return DbStatus.ID_ALREADY_EXISTS; 
                        }
                    }
                }
            }

            UUID generatedUserId = null;
            try (PreparedStatement insertUserStmt = conn.prepareStatement(insertUserSql)) {
                
                insertUserStmt.setString(1, name);
                insertUserStmt.setString(2, bilkentMail);
                insertUserStmt.setString(3, studentId);
                insertUserStmt.setString(4, passwordHash);

                try (ResultSet rs = insertUserStmt.executeQuery()) {
                    if (rs.next()) {
                        generatedUserId = rs.getObject("id", java.util.UUID.class);
                    }
                }
            }

            if (generatedUserId == null) {
                return DbStatus.QUERY_ERROR;
            }

            try (PreparedStatement insertStudentStmt = conn.prepareStatement(insertStudentSql)) {
                insertStudentStmt.setObject(1, generatedUserId);
                
                int studentInsertedRows = insertStudentStmt.executeUpdate();
                
                if (studentInsertedRows > 0) {
                    createActivationCode(bilkentMail);
                    return DbStatus.SUCCESS;
                }
            }
            
            return DbStatus.QUERY_ERROR;

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            if ("23505".equals(e.getSQLState())) {
                 return DbStatus.QUERY_ERROR; // Unique constraint violation
            }
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Registers a new admin to the database.
     * Checks for duplicate email before insertion.
     * student_id is implicitly set to NULL and role is set to 'admin'.
     * @param name Admin's full name
     * @param email Admin's email address
     * @param passwordHash Admin's hashed password
     * @return DbStatus indicating SUCCESS, EMAIL_ALREADY_EXISTS, QUERY_ERROR, etc.
     */
    public DbStatus registerAdmin(String name, String email, String passwordHash) {
        
        String gcSql = "DELETE FROM users WHERE is_activated = FALSE AND created_at < NOW() - INTERVAL '30 minutes'";
        
        String checkSql = "SELECT bilkent_email FROM users WHERE bilkent_email = ?";
        
        String insertSql = "INSERT INTO users (full_name, bilkent_email, student_id, password_hash, role, is_activated) VALUES (?, ?, NULL, ?, 'admin', FALSE)";

        try {

            try (PreparedStatement gcStmt = conn.prepareStatement(gcSql)) {
                gcStmt.executeUpdate();
            }

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, email);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return DbStatus.EMAIL_ALREADY_EXISTS; 
                    }
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                
                passwordHash = hashPassword(passwordHash); 
                
                insertStmt.setString(1, name);           // full_name
                insertStmt.setString(2, email);          // bilkent_email
                insertStmt.setString(3, passwordHash);   // password_hash

                int insertedRows = insertStmt.executeUpdate();
                
                if (insertedRows > 0) {
                    createActivationCode(email);
                    return DbStatus.SUCCESS;
                }
                
                return DbStatus.QUERY_ERROR;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            if ("23505".equals(e.getSQLState())) {
                 return DbStatus.QUERY_ERROR; 
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Authenticates a student based on email and password.
     * Ensures the account is activated and the user has the 'student' role.
     * @param email Student's Bilkent email address
     * @param plainPassword The raw password entered by the user
     * @return DbStatus indicating SUCCESS, ACCOUNT_NOT_ACTIVATED, INVALID_CREDENTIALS, etc.
     */
    public DbStatus loginStudent(String email, String plainPassword) {
        String sql = "SELECT password_hash, role, is_activated FROM users WHERE bilkent_email = ?";

        try (
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean isActivated = rs.getBoolean("is_activated");
                    String role = rs.getString("role");
                    String dbPasswordHash = rs.getString("password_hash");

                    if (!isActivated) {
                        return DbStatus.ACCOUNT_NOT_ACTIVATED;
                    }

                    if (!"student".equals(role)) {
                        return DbStatus.INVALID_CREDENTIALS;
                    }

                    boolean isPasswordCorrect = verifyPassword(plainPassword, dbPasswordHash); 
                    
                    if (isPasswordCorrect) {
                        return DbStatus.SUCCESS;
                    } else {
                        return DbStatus.INVALID_CREDENTIALS;
                    }
                } else {
                    return DbStatus.DATA_NOT_FOUND; 
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
     * Authenticates an admin based on email and password.
     * Ensures the account is activated and the user has the 'admin' role.
     * @param email Admin's email address
     * @param plainPassword The raw password entered by the user
     * @return DbStatus indicating SUCCESS, ACCOUNT_NOT_ACTIVATED, INVALID_CREDENTIALS, etc.
     */
    public DbStatus loginAdmin(String email, String plainPassword) {
        
        String sql = "SELECT password_hash, role, is_activated FROM users WHERE bilkent_email = ?";

        try (
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean isActivated = rs.getBoolean("is_activated");
                    String role = rs.getString("role");
                    String dbPasswordHash = rs.getString("password_hash");

                    if (!isActivated) {
                        return DbStatus.ACCOUNT_NOT_ACTIVATED; 
                    }

                    if (!"admin".equals(role)) {
                        return DbStatus.INVALID_CREDENTIALS; 
                    }

                    boolean isPasswordCorrect = verifyPassword(plainPassword, dbPasswordHash); 
                    
                    if (isPasswordCorrect) {
                        return DbStatus.SUCCESS;
                    } else {
                        return DbStatus.INVALID_CREDENTIALS;
                    }
                } else {
                    return DbStatus.DATA_NOT_FOUND;
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

    private boolean verifyPassword(String plainPassword, String hashedPassword) {
        plainPassword = hashPassword(plainPassword);
        return plainPassword.equals(hashedPassword);
    }

/**
     * Modify user's profile to set is_activated to true
     * @param email User's email address
     * @return DbStatus (SUCCESS, DATA_NOT_FOUND, CONNECTION_ERROR, QUERY_ERROR)
     */
    public DbStatus setProfileActivation(String email) {
        String updateSql = "UPDATE users SET is_activated = TRUE WHERE bilkent_email = ?";

        try (
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
     * Activation of the account is optional because verification code can be used for other purposes as well, such as password reset. So we keep them separate.
     * It only verifies 30 minutes old activation codes to prevent abuse.
     * After verification, if the code is correct, method automatically deletes the activation code from the database to prevent reuse.
     * If the code is incorrect, it triggers a Garbage Collector mechanism to delete ALL expired codes in the database.
     * @param email User's email address
     * @param inputCode Activation code provided by the user
     * @return DbStatus (SUCCESS, INVALID_CODE, EXPIRED_CODE, DATA_NOT_FOUND, CONNECTION_ERROR, QUERY_ERROR)
     */
    public DbStatus verifyActivationCode(String email, String inputCode) {
        String selectSql = "SELECT a.activation_code FROM activation a INNER JOIN users u ON a.user_id = u.id WHERE u.bilkent_email = ? AND a.created_at >= NOW() - INTERVAL '30 minutes'";
        
        String deleteSql = "DELETE FROM activation WHERE user_id = (SELECT id FROM users WHERE bilkent_email = ?)";
        
        String gcSql = "DELETE FROM activation WHERE created_at < NOW() - INTERVAL '30 minutes'";

        try (
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            selectStmt.setString(1, email);
            
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    String dbCode = rs.getString("activation_code");
                    
                    if (dbCode == null) {
                        return DbStatus.DATA_NOT_FOUND;
                    }
                    
                    if (dbCode.equals(inputCode)) {
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setString(1, email);
                            deleteStmt.executeUpdate();
                        }
                        
                        return DbStatus.SUCCESS; // Correct code provided
                    } else {
                        try (PreparedStatement gcStmt = conn.prepareStatement(gcSql)) {
                            gcStmt.executeUpdate();
                        }
                        
                        return DbStatus.INVALID_CODE; // Wrong code provided
                    }
                } else {
                    try (PreparedStatement gcStmt = conn.prepareStatement(gcSql)) {
                            gcStmt.executeUpdate();
                        }
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

        try (
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