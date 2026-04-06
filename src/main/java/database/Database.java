package database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import models.Facility;
import models.Reservation;
import models.SportType;
import models.Student;

public class Database {

    // 1. Uygulama boyunca yaşayacak TEK ortak nesne
    private static Database instance;

    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String salt;
    private Connection conn;
    private String SUPABASE_URL;
    private String SUPABASE_KEY;

    // 2. Constructor'ı "public" yerine "private" yapıyoruz.
    // Bu sayede dışarıdan kimse "new Database()" diyemez!
    private Database() {
        loadProperties();
        try {
            getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 3. Herkesin bu ortak nesneye ulaşmak için kullanacağı metod
    public static Database getInstance() {
        if (instance == null) {
            // Eğer obje henüz hiç oluşturulmadıysa 1 kere oluştur
            instance = new Database();
        }
        // Eğer zaten oluşturulduysa var olanı ver
        return instance;
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
            this.SUPABASE_URL = prop.getProperty("supabase.url");
            this.SUPABASE_KEY = prop.getProperty("supabase.key");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Yardımcı metot: Bağlantıyı oluşturur veya kapalıysa yeniden açar
    private Connection getConnection() throws SQLException {
        // Bağlantı yoksa, kapandıysa veya geçerliliğini yitirdiyse yenisini aç
        if (conn == null || conn.isClosed()) { // 2 saniye timeout ile kontrol et
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        }
        return conn;
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
        
        String insertStudentSql = "INSERT INTO students (user_id, elo_point, penalty_points, reliability_score, matches_played, win_rate, is_public_profile, is_elo_matching_enabled) " +
                                  "VALUES (?, 1000, 0, 100.0, 0, 0.0, TRUE, TRUE)";

        try{

            try (PreparedStatement gcStmt = getConnection().prepareStatement(gcSql)) {
                gcStmt.executeUpdate();
            }

            try (PreparedStatement checkStmt = getConnection().prepareStatement(checkSql)) {
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
            try (PreparedStatement insertUserStmt = getConnection().prepareStatement(insertUserSql)) {
                passwordHash = hashPassword(passwordHash); 
                
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

            try (PreparedStatement insertStudentStmt = getConnection().prepareStatement(insertStudentSql)) {
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
     * Requires a valid code from the 'admin_activation' table to proceed.
     * Checks for duplicate email before insertion.
     * Inserts the generated user UUID into the 'admins' table.
     * student_id is implicitly set to NULL and role is set to 'admin'.
     * @param name Admin's full name
     * @param email Admin's email address
     * @param passwordHash The raw password to be hashed (Note: Variable name implies hash, but it's hashed inside)
     * @param adminActivationCode The special code required to register as an admin
     * @return DbStatus indicating SUCCESS, EMAIL_ALREADY_EXISTS, INVALID_CODE, QUERY_ERROR, etc.
     */
    public DbStatus registerAdmin(String name, String email, String passwordHash, String adminActivationCode) {
        
        String gcSql = "DELETE FROM users WHERE is_activated = FALSE AND created_at < NOW() - INTERVAL '30 minutes'";
        String checkEmailSql = "SELECT bilkent_email FROM users WHERE bilkent_email = ?";
        String checkAdminCodeSql = "SELECT id FROM admin_activation WHERE activation_code = ?";
        String deleteAdminCodeSql = "DELETE FROM admin_activation WHERE activation_code = ?";
        
        String insertUserSql = "INSERT INTO users (full_name, bilkent_email, student_id, password_hash, role, is_activated) " +
                               "VALUES (?, ?, NULL, ?, 'admin', FALSE) RETURNING id";
                               
        String insertAdminSql = "INSERT INTO admins (admin_id) VALUES (?)";

        Connection conn = null;
        try {
            conn = getConnection();
            
            try (PreparedStatement gcStmt = conn.prepareStatement(gcSql)) {
                gcStmt.executeUpdate();
            }

            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement(checkEmailSql)) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        return DbStatus.EMAIL_ALREADY_EXISTS; 
                    }
                }
            }

            try (PreparedStatement checkCodeStmt = conn.prepareStatement(checkAdminCodeSql)) {
                checkCodeStmt.setString(1, adminActivationCode); 
                try (ResultSet rs = checkCodeStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return DbStatus.INVALID_CODE; 
                    }
                }
            }

            java.util.UUID newUserId = null;
            String finalHashedPassword = hashPassword(passwordHash); 

            try (PreparedStatement insertUserStmt = conn.prepareStatement(insertUserSql)) {
                insertUserStmt.setString(1, name);
                insertUserStmt.setString(2, email);
                insertUserStmt.setString(3, finalHashedPassword);

                try (ResultSet rs = insertUserStmt.executeQuery()) {
                    if (rs.next()) {
                        newUserId = (java.util.UUID) rs.getObject("id");
                    } else {
                        conn.rollback();
                        return DbStatus.QUERY_ERROR;
                    }
                }
            }

            try (PreparedStatement insertAdminStmt = conn.prepareStatement(insertAdminSql)) {
                insertAdminStmt.setObject(1, newUserId);
                insertAdminStmt.executeUpdate();
            }

            try (PreparedStatement deleteCodeStmt = conn.prepareStatement(deleteAdminCodeSql)) {
                deleteCodeStmt.setString(1, adminActivationCode);
                deleteCodeStmt.executeUpdate();
            }

            conn.commit();
            
            createActivationCode(email);
            
            return DbStatus.SUCCESS;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            if ("23505".equals(e.getSQLState())) {
                 return DbStatus.QUERY_ERROR; 
            }
            
            return DbStatus.QUERY_ERROR;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
    
    /**
     * Authenticates a student based on email and password.
     * Ensures the account is activated, NOT banned (students.banned_at is NULL), and the user has the 'student' role.
     * If authentication is successful, updates the 'last_seen' timestamp in the students table.
     * @param email Student's Bilkent email address
     * @param plainPassword The raw password entered by the user
     * @return DbStatus indicating SUCCESS, ACCOUNT_NOT_ACTIVATED, ACCOUNT_BANNED, INVALID_CREDENTIALS, etc.
     */
    public DbStatus loginStudent(String email, String plainPassword) {

        // users ve students tablolarını birleştirerek ban durumunu çekiyoruz
        String selectSql = "SELECT u.id, u.password_hash, u.role, u.is_activated, s.banned_at " +
                           "FROM users u " +
                           "INNER JOIN students s ON u.id = s.user_id " +
                           "WHERE u.bilkent_email = ?";
        
        String updateLastSeenSql = "UPDATE students SET last_seen = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(selectSql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.util.UUID userId = rs.getObject("id", java.util.UUID.class);
                    boolean isActivated = rs.getBoolean("is_activated");
                    
                    // students tablosundan gelen ban tarihini çekiyoruz
                    java.sql.Timestamp bannedAt = rs.getTimestamp("banned_at"); 
                    
                    String role = rs.getString("role");
                    String dbPasswordHash = rs.getString("password_hash");

                    // 1. Aktivasyon Kontrolü
                    if (!isActivated) {
                        return DbStatus.ACCOUNT_NOT_ACTIVATED;
                    }
                    
                    // 2. Ban Kontrolü (students tablosundaki banned_at null değilse banlıdır)
                    if (bannedAt != null) {
                        return DbStatus.ACCOUNT_BANNED;
                    }

                    // 3. Rol Kontrolü
                    if (!"student".equals(role)) {
                        return DbStatus.INVALID_CREDENTIALS;
                    }

                    // 4. Şifre Kontrolü
                    boolean isPasswordCorrect = verifyPassword(plainPassword, dbPasswordHash); 
                    
                    if (isPasswordCorrect) {
                        // Şifre doğruysa students tablosundaki last_seen kolonunu güncelle
                        try (PreparedStatement updateStmt = getConnection().prepareStatement(updateLastSeenSql)) {
                            updateStmt.setObject(1, userId);
                            updateStmt.executeUpdate();
                        }
                        
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
             PreparedStatement stmt = getConnection().prepareStatement(sql)) {

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
             PreparedStatement updateStmt = getConnection().prepareStatement(updateSql)) {

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
             PreparedStatement selectStmt = getConnection().prepareStatement(selectSql)) {

            selectStmt.setString(1, email);
            
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    String dbCode = rs.getString("activation_code");
                    
                    if (dbCode == null) {
                        return DbStatus.DATA_NOT_FOUND;
                    }
                    
                    if (dbCode.equals(inputCode)) {
                        try (PreparedStatement deleteStmt = getConnection().prepareStatement(deleteSql)) {
                            deleteStmt.setString(1, email);
                            deleteStmt.executeUpdate();
                        }
                        
                        return DbStatus.SUCCESS; // Correct code provided
                    } else {
                        try (PreparedStatement gcStmt = getConnection().prepareStatement(gcSql)) {
                            gcStmt.executeUpdate();
                        }
                        
                        return DbStatus.INVALID_CODE; // Wrong code provided
                    }
                } else {
                    try (PreparedStatement gcStmt = getConnection().prepareStatement(gcSql)) {
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
            PreparedStatement findUserStmt = getConnection().prepareStatement(findUserSql)) {
            EmailService emailService = new EmailService();
            findUserStmt.setString(1, email);
            ResultSet rs = findUserStmt.executeQuery();

            if (rs.next()) {
                // 1. DÜZELTME: UUID'yi ResultSet'ten Object olarak alıp cast ediyoruz
                // PostgreSQL sürücüsü UUID'yi doğrudan java.util.UUID olarak döndürür
                UUID userId = rs.getObject("id", UUID.class);

                try (PreparedStatement updateStmt = getConnection().prepareStatement(updateActivationSql)) {
                    updateStmt.setString(1, activationCode);
                    
                    // 2. DÜZELTME: PreparedStatement içine UUID'yi setObject ile yerleştiriyoruz
                    updateStmt.setObject(2, userId);
                    int updatedRows = updateStmt.executeUpdate();

                    if (updatedRows == 0) {
                        try (PreparedStatement insertStmt = getConnection().prepareStatement(insertActivationSql)) {
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

    /**
     * Adds a new sport interest for a student.
     * Prevents duplicate entries for the same student and sport.
     * It automatically checks if the provided sport name exists in the sports table and if the student email exists in the users table.
     * Prohibit case sensitivity for sport names to prevent duplicates like "Football" and "football".
     * @param mail Student's Bilkent email address
     * @param sportName Name of the sport to add
     * @return DbStatus indicating SUCCESS, INTEREST_ALREADY_EXISTS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus insertStudentInterest(String mail, String sportName) {
        
        String findUserSql = "SELECT id FROM users WHERE bilkent_email = ?";
        String findSportSql = "SELECT id FROM sports WHERE LOWER(name) = LOWER(?)";
        
        String checkInterestExistsSql = "SELECT 1 FROM student_interests WHERE student_id = ? AND sport_id = ?";
        
        String insertInterestSql = "INSERT INTO student_interests (student_id, sport_id) VALUES (?, ?)";

        try (Connection conn = getConnection()) {

            java.util.UUID userId = null;
            try (PreparedStatement userStmt = conn.prepareStatement(findUserSql)) {
                userStmt.setString(1, mail);
                try (ResultSet rs = userStmt.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getObject("id", java.util.UUID.class);
                    } else {
                        return DbStatus.DATA_NOT_FOUND; 
                    }
                }
            }

            int sportId = -1;
            try (PreparedStatement sportStmt = conn.prepareStatement(findSportSql)) {
                sportStmt.setString(1, sportName);
                try (ResultSet rs = sportStmt.executeQuery()) {
                    if (rs.next()) {
                        sportId = rs.getInt("id");
                    } else {
                        return DbStatus.DATA_NOT_FOUND; 
                    }
                }
            }

            try (PreparedStatement checkStmt = conn.prepareStatement(checkInterestExistsSql)) {
                checkStmt.setObject(1, userId);
                checkStmt.setInt(2, sportId);
                
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return DbStatus.QUERY_ERROR; 
                    }
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertInterestSql)) {
                insertStmt.setObject(1, userId);
                insertStmt.setInt(2, sportId);

                int insertedRows = insertStmt.executeUpdate();
                return insertedRows > 0 ? DbStatus.SUCCESS : DbStatus.QUERY_ERROR;
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
     * Removes a specific sport from a student's interests.
     * Uses case-insensitive matching for the sport name.
     * @param mail Student's Bilkent email address
     * @param sportName Name of the sport to remove
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if the interest didn't exist), or errors.
     */
    public DbStatus deleteStudentInterest(String mail, String sportName) {
        
        String deleteSql = "DELETE FROM student_interests " +
                           "WHERE student_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                           "AND sport_id = (SELECT id FROM sports WHERE LOWER(name) = LOWER(?))";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {

            stmt.setString(1, mail);
            stmt.setString(2, sportName);

            int deletedRows = stmt.executeUpdate();

            return deletedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the ELO matchmaking status for a student.
     * Finds the user by email and updates their preference in the students table.
     * @param mail Student's Bilkent email address
     * @param enabled True to enable ELO matchmaking, false to disable
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateEloMatchingStatus(String mail, boolean enabled) {
        
        // İç içe sorgu (Subquery) sayesinde e-posta üzerinden doğrudan students tablosunu güncelliyoruz
        String updateSql = "UPDATE students " +
                           "SET is_elo_matching_enabled = ? " +
                           "WHERE user_id = (SELECT id FROM users WHERE bilkent_email = ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {

            stmt.setBoolean(1, enabled);
            stmt.setString(2, mail);

            // executeUpdate() güncellenen satır sayısını döndürür
            int updatedRows = stmt.executeUpdate();

            // Eğer 0'dan büyükse işlem başarılıdır. 
            // 0 dönerse, bu e-postaya sahip bir kullanıcı (veya students tablosunda profili) yok demektir.
            return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates a user's password directly without any prior verification.
     * Checks if the new password is the same as the current one.
     * @param email User's Bilkent email address
     * @param newPassword The new raw password to be set
     * @return DbStatus indicating SUCCESS, SAME_PASSWORD, DATA_NOT_FOUND, CONNECTION_ERROR, or QUERY_ERROR
     */
    public DbStatus updatePassword(String email, String newPassword) {
        
        // Önce mevcut şifreyi çekmek için SELECT sorgusu
        String selectSql = "SELECT password_hash FROM users WHERE bilkent_email = ?";
        // Güncelleme için UPDATE sorgusu
        String updateSql = "UPDATE users SET password_hash = ? WHERE bilkent_email = ?";

        try {
            // Yeni şifreyi metodun başında bir kez hashliyoruz
            String newPasswordHash = hashPassword(newPassword);

            // 1. ADIM: Mevcut şifreyi veritabanından çek ve karşılaştır
            try (PreparedStatement selectStmt = getConnection().prepareStatement(selectSql)) {
                selectStmt.setString(1, email);
                
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        String currentHash = rs.getString("password_hash");
                        
                        // Yeni şifrenin hash'i veritabanındaki ile eşleşiyorsa güncellemeyi durdur
                        if (newPasswordHash.equals(currentHash)) {
                            return DbStatus.SAME_PASSWORD; // DbStatus enum'ına eklemeyi unutma!
                        }
                    } else {
                        // Eğer rs.next() false dönerse böyle bir e-posta veritabanında yoktur
                        return DbStatus.DATA_NOT_FOUND;
                    }
                }
            }

            // 2. ADIM: Şifreler farklıysa (yukarıdaki if'e girmediyse) güncellemeyi yap
            try (PreparedStatement updateStmt = getConnection().prepareStatement(updateSql)) {
                updateStmt.setString(1, newPasswordHash);
                updateStmt.setString(2, email);

                int updatedRows = updateStmt.executeUpdate();
                return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.QUERY_ERROR;
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
     * Updates the public profile visibility status for a student.
     * Finds the user by email and updates their preference in the students table.
     * @param email Student's Bilkent email address
     * @param isPublic True to make the profile public, false to make it private
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateStudentProfileVisibility(String email, boolean isPublic) {
        
        String updateSql = "UPDATE students " +
                           "SET is_public_profile = ? " +
                           "WHERE user_id = (SELECT id FROM users WHERE bilkent_email = ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(updateSql)) {

            stmt.setBoolean(1, isPublic);
            stmt.setString(2, email);

            int updatedRows = stmt.executeUpdate();

            return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Fetches student records from the 'users' and 'students' tables,
     * including their sport interests and profile picture, and updates the provided Student object.
     * @param student The existing Student object to be updated
     * @param email Student's Bilkent email address to query the database
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus fillStudentDataByEmail(Student student, String email) {
        
        if (student == null) {
            return DbStatus.QUERY_ERROR;
        }

        // SELECT kısmında u.profile_pic_url çekiliyor
        String sql = "SELECT u.full_name, u.bilkent_email, u.student_id AS uni_id, u.password_hash, u.profile_pic_url, " +
                     "s.elo_point, s.penalty_points, s.reliability_score, s.matches_played, " +
                     "s.win_rate, s.is_public_profile, s.is_elo_matching_enabled, " +
                     "STRING_AGG(sp.name, ',') AS sport_interests " +
                     "FROM users u " +
                     "INNER JOIN students s ON u.id = s.user_id " +
                     "LEFT JOIN student_interests si ON u.id = si.student_id " +
                     "LEFT JOIN sports sp ON si.sport_id = sp.id " +
                     "WHERE u.bilkent_email = ? AND u.role = 'student' " +
                     "GROUP BY u.id, s.user_id"; 

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    
                    student.setFullName(rs.getString("full_name"));
                    student.setBilkentEmail(rs.getString("bilkent_email"));
                    student.setPassword(rs.getString("password_hash"));
                    
                    // Kendi metodunla profile picture set ediliyor
                    student.setProfilePictureUrl(rs.getString("profile_pic_url"));
                    
                    student.setStudentId(rs.getString("uni_id"));
                    student.setEloPoint(rs.getInt("elo_point"));
                    student.setPenaltyPoints(rs.getInt("penalty_points"));
                    student.setReliabilityScore(rs.getDouble("reliability_score"));
                    student.setMatchesPlayed(rs.getInt("matches_played"));
                    student.setWinRate(rs.getDouble("win_rate"));
                    student.setPublicProfile(rs.getBoolean("is_public_profile"));
                    student.setEloMatchingEnabled(rs.getBoolean("is_elo_matching_enabled"));
                    
                    int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                    student.setMatchesWon(matchesWon);

                    String interestsStr = rs.getString("sport_interests");
                    java.util.List<models.SportType> interestsList = new java.util.ArrayList<>();
                    
                    if (interestsStr != null && !interestsStr.trim().isEmpty()) {
                        String[] sportNames = interestsStr.split(",");
                        
                        for (String sportName : sportNames) {
                            try {
                                String formattedSportName = sportName.trim().toUpperCase().replace(" ", "_");
                                interestsList.add(models.SportType.valueOf(formattedSportName));
                            } catch (IllegalArgumentException e) {
                                System.err.println("Uyarı: Geçersiz spor türü -> " + sportName);
                            }
                        }
                    }
                    
                    student.setInterests(interestsList);

                    return DbStatus.SUCCESS;
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
     * Updates the ELO score for a student.
     * Finds the user by email and updates their elo_point in the students table.
     * @param email Student's Bilkent email address
     * @param score The new ELO score to be set
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateStudentElo(String email, int score) {
        
        String updateSql = "UPDATE students " +
                           "SET elo_point = ? " +
                           "WHERE user_id = (SELECT id FROM users WHERE bilkent_email = ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(updateSql)) {

            stmt.setInt(1, score);
            stmt.setString(2, email);

            int updatedRows = stmt.executeUpdate();

            return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the penalty points for a student.
     * Finds the user by their Bilkent email address and updates their penalty_points in the students table.
     * @param email Student's Bilkent email address
     * @param newPenaltyPoints The new total penalty points to be set
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateStudentPenalty(String email, int newPenaltyPoints) {
        
        String updateSql = "UPDATE students " +
                           "SET penalty_points = ? " +
                           "WHERE user_id = (SELECT id FROM users WHERE bilkent_email = ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(updateSql)) {

            stmt.setInt(1, newPenaltyPoints);
            stmt.setString(2, email);

            int updatedRows = stmt.executeUpdate();

            return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the reliability score for a student.
     * Finds the user by their Bilkent email address and updates their reliability_score in the students table.
     * @param email Student's Bilkent email address
     * @param newReliabilityScore The new reliability score to be set (double value)
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateStudentReliability(String email, double newReliabilityScore) {
        
        if (email == null || email.trim().isEmpty()) {
            return DbStatus.QUERY_ERROR;
        }

        // users tablosundan email'i bulup, students tablosundaki user_id'ye göre eşleştirerek güncelliyoruz
        String sql = "UPDATE students SET reliability_score = ? " +
                     "WHERE user_id = (SELECT id FROM users WHERE bilkent_email = ?)";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            // Değeri setDouble ile veritabanına gönderiyoruz
            stmt.setDouble(1, newReliabilityScore);
            stmt.setString(2, email);
            
            int updatedRows = stmt.executeUpdate();
            
            return (updatedRows > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            return DbStatus.QUERY_ERROR;
        }
    }
    /**
     * Updates the ban status for a student.
     * If isBanned is true, sets banned_at to the current timestamp.
     * If isBanned is false, sets banned_at to NULL (lifts the ban).
     * @param email Student's Bilkent email address
     * @param isBanned True to ban the user, false to unban
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateStudentBanStatus(String email, boolean isBanned) {
        
        String updateSql = "UPDATE students " +
                           "SET banned_at = CASE WHEN ? = TRUE THEN CURRENT_TIMESTAMP ELSE NULL END " +
                           "WHERE user_id = (SELECT id FROM users WHERE bilkent_email = ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(updateSql)) {

            stmt.setBoolean(1, isBanned);
            stmt.setString(2, email);

            int updatedRows = stmt.executeUpdate();

            return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();
            
            // Veritabanı bağlantı hatası kontrolü
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the full name (nickname) of a user.
     * Overwrites the 'full_name' column in the 'users' table with the provided nickname.
     * @param email User's Bilkent email address
     * @param newNickname The new nickname/full name to be set
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateUserNickname(String email, String newNickname) {
        
        String updateSql = "UPDATE users SET full_name = ? WHERE bilkent_email = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(updateSql)) {

            stmt.setString(1, newNickname);
            stmt.setString(2, email);

            int updatedRows = stmt.executeUpdate();

            return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

/**
     * Creates a new friend request between two students.
     * Looks up both users by email and inserts a record with 'PENDING' status into the 'friendships' table.
     * @param senderEmail The Bilkent email of the user sending the request
     * @param targetEmail The Bilkent email of the user receiving the request
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if an email doesn't exist), or errors.
     */
    public DbStatus insertFriendRequest(String senderEmail, String targetEmail) {
        
        if (senderEmail != null && senderEmail.equalsIgnoreCase(targetEmail)) {
            return DbStatus.QUERY_ERROR; 
        }

        String insertSql = "INSERT INTO friendships (requester_id, receiver_id, status) " +
                           "SELECT sender.id, target.id, 'Pending' " +
                           "FROM users sender, users target " +
                           "WHERE sender.bilkent_email = ? AND target.bilkent_email = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(insertSql)) {

            stmt.setString(1, senderEmail);
            stmt.setString(2, targetEmail);

            int insertedRows = stmt.executeUpdate();

            return insertedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

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
     * Accepts a pending friend request.
     * Updates the status of the friendship from 'Pending' to 'Accepted'.
     * @param receiverEmail The Bilkent email of the user who received the request (the one accepting)
     * @param requesterEmail The Bilkent email of the user who originally sent the request
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if no such pending request exists), or errors.
     */
    public DbStatus acceptFriendRequest(String requesterEmail, String receiverEmail) {
        
        String updateSql = "UPDATE friendships " +
                           "SET status = 'Accepted' " +
                           "WHERE requester_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                           "  AND receiver_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                           "  AND status = 'Pending'";

        try (PreparedStatement stmt = getConnection().prepareStatement(updateSql)) {

            stmt.setString(1, requesterEmail);
            
            stmt.setString(2, receiverEmail);

            int updatedRows = stmt.executeUpdate();

            return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();
            
            // Veritabanı bağlantı hatası
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Deletes a friendship or a pending friend request between two users.
     * Checks both directions to ensure the relationship is removed regardless of who initiated it.
     * @param senderEmail The Bilkent email of the first user (sender)
     * @param receiverEmail The Bilkent email of the second user (receiver)
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if friendship didn't exist), or errors.
     */
    public DbStatus deleteFriend(String senderEmail, String receiverEmail) {
        
        String deleteSql = "DELETE FROM friendships " +
                           "WHERE (requester_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                           "  AND receiver_id = (SELECT id FROM users WHERE bilkent_email = ?)) " +
                           "   OR (requester_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                           "  AND receiver_id = (SELECT id FROM users WHERE bilkent_email = ?))";

        try (PreparedStatement stmt = getConnection().prepareStatement(deleteSql)) {

            stmt.setString(1, senderEmail);
            stmt.setString(2, receiverEmail);
            
            stmt.setString(3, receiverEmail);
            stmt.setString(4, senderEmail);

            int deletedRows = stmt.executeUpdate();

            return deletedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();

            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Retrieves all activated students whose profiles are public.
     * Includes their sport interests mapped to the SportType enum.
     * Used primarily for the matching/recommendation system.
     * * @return A list of public Student objects
     */
    public List<Student> getAllPublicStudents() {
        
        List<Student> studentsList = new ArrayList<>();

        String sql = "SELECT u.full_name, u.bilkent_email, u.student_id AS uni_id, " +
                     "s.elo_point, s.penalty_points, s.reliability_score, s.matches_played, " +
                     "s.win_rate, s.is_public_profile, s.is_elo_matching_enabled, " +
                     "STRING_AGG(sp.name, ',') AS sport_interests " +
                     "FROM users u " +
                     "INNER JOIN students s ON u.id = s.user_id " +
                     "LEFT JOIN student_interests si ON u.id = si.student_id " +
                     "LEFT JOIN sports sp ON si.sport_id = sp.id " +
                     "WHERE u.role = 'student' AND u.is_activated = TRUE AND s.is_public_profile = TRUE " +
                     "GROUP BY u.id, s.user_id";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Student student = new Student(rs.getString("full_name"), rs.getString("bilkent_email"), rs.getString("uni_id"));
                
                student.setFullName(rs.getString("full_name"));
                student.setBilkentEmail(rs.getString("bilkent_email"));
                student.setStudentId(rs.getString("uni_id"));
                student.setEloPoint(rs.getInt("elo_point"));
                student.setPenaltyPoints(rs.getInt("penalty_points"));
                student.setReliabilityScore(rs.getDouble("reliability_score"));
                
                int matchesPlayed = rs.getInt("matches_played");
                double winRate = rs.getDouble("win_rate");
                student.setMatchesPlayed(matchesPlayed);
                student.setWinRate(winRate);
                student.setMatchesWon((int) Math.round(matchesPlayed * winRate));
                
                student.setPublicProfile(rs.getBoolean("is_public_profile"));
                student.setEloMatchingEnabled(rs.getBoolean("is_elo_matching_enabled"));

                String interestsStr = rs.getString("sport_interests");
                if (interestsStr != null && !interestsStr.trim().isEmpty()) {
                    List<SportType> interests = new ArrayList<>();
                    String[] sportNames = interestsStr.split(",");
                    
                    for (String sportName : sportNames) {
                        try {
                            interests.add(SportType.valueOf(sportName.trim().toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            System.err.println("Uyarı: Geçersiz spor türü bulundu ve Enum'a çevrilemedi -> " + sportName);
                        }
                    }
                    
                    student.setInterests(interests);
                }

                studentsList.add(student);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return studentsList;
    }

    /**
     * Checks if a facility is available for a specific date and time slot.
     * A facility is considered available if it exists, is not under maintenance, 
     * and the number of active (non-cancelled) reservations is strictly less than its capacity.
     * @param facilityName The name of the facility
     * @param date The date of the reservation
     * @param timeSlot The specific time slot (e.g., "14:00-15:30")
     * @return true if available, false if full, under maintenance, or not found.
     */
    public boolean checkFacilityAvailability(String facilityName, java.time.LocalDate date, String timeSlot) {
        
        String sql = "SELECT f.capacity, f.is_under_maintenance, " +
                     "(SELECT COUNT(*) FROM reservations r " +
                     " WHERE r.facility_id = f.facility_id " +
                     "   AND r.reservation_date = ? " +
                     "   AND r.time_slot = ? " +
                     "   AND r.is_cancelled = FALSE) AS active_reservations " +
                     "FROM facilities f " +
                     "WHERE f.name = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(date));
            stmt.setString(2, timeSlot);
            stmt.setString(3, facilityName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean isUnderMaintenance = rs.getBoolean("is_under_maintenance");
                    int capacity = rs.getInt("capacity");
                    int activeReservations = rs.getInt("active_reservations");

                    if (isUnderMaintenance) {
                        return false;
                    }

                    return activeReservations < capacity;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * THIS IS AN OVERLOADED VERSION OF checkFacilityAvailability() THAT ALSO CHECKS FOR DOUBLE BOOKING.
     * Checks if a facility is available for a specific date and time slot, AND ensures 
     * the given student does not already have a reservation at the exact same time (Double Booking).
     * @param facilityName The name of the facility
     * @param date The date of the reservation
     * @param timeSlot The specific time slot (e.g., "14:00-15:30")
     * @param currentStudent The student requesting the reservation
     * @return true if available AND student is not double-booked, false otherwise.
     */
    public boolean checkFacilityAvailability(String facilityName, java.time.LocalDate date, String timeSlot, models.Student currentStudent) {
        
        if (currentStudent == null || currentStudent.getBilkentEmail() == null) {
            return false;
        }

        // 1. Tesis kapasitesi ve aktif rezervasyonlar
        // 2. Öğrencinin o saatteki aktif (iptal edilmemiş) rezervasyonu var mı kontrolü (EXISTS ile)
        String sql = "SELECT f.capacity, f.is_under_maintenance, " +
                     
                     // Tesisin o saatteki doluluk oranı alt sorgusu
                     "(SELECT COUNT(*) FROM reservations r " +
                     " WHERE r.facility_id = f.facility_id " +
                     "   AND r.reservation_date = ? " +
                     "   AND r.time_slot = ? " +
                     "   AND r.is_cancelled = FALSE) AS active_reservations, " +
                     
                     // Öğrenci Çifte Rezervasyon (Double Booking) alt sorgusu
                     "(SELECT EXISTS (" +
                     "   SELECT 1 FROM reservations r2 " +
                     "   INNER JOIN reservation_attendees ra ON r2.reservation_id = ra.reservation_id " +
                     "   WHERE ra.student_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                     "     AND r2.reservation_date = ? " +
                     "     AND r2.time_slot = ? " +
                     "     AND r2.is_cancelled = FALSE" +
                     ")) AS is_double_booked " +
                     
                     "FROM facilities f " +
                     "WHERE f.name = ?";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {

            java.sql.Date sqlDate = java.sql.Date.valueOf(date);

            // 1. Kapasite alt sorgusu için parametreler
            stmt.setDate(1, sqlDate);
            stmt.setString(2, timeSlot);
            
            // 2. Double booking alt sorgusu için parametreler
            stmt.setString(3, currentStudent.getBilkentEmail());
            stmt.setDate(4, sqlDate);
            stmt.setString(5, timeSlot);
            
            // 3. Ana sorgu için tesis adı
            stmt.setString(6, facilityName);

            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean isUnderMaintenance = rs.getBoolean("is_under_maintenance");
                    int capacity = rs.getInt("capacity");
                    int activeReservations = rs.getInt("active_reservations");
                    boolean isDoubleBooked = rs.getBoolean("is_double_booked");

                    // Eğer tesis bakımdaysa VEYA öğrencinin o saatte zaten işi varsa direkt false
                    if (isUnderMaintenance || isDoubleBooked) {
                        if (isDoubleBooked) {
                            System.err.println("Uyarı: Öğrencinin bu tarih ve saatte zaten başka bir rezervasyonu bulunuyor (Double Booking)!");
                        }
                        return false;
                    }

                    // Her şey yolundaysa kapasitenin dolup dolmadığına bak
                    return activeReservations < capacity;
                }
            }

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Creates a new reservation for a student at a specific facility.
     * Automatically adds the reservation creator as the first attendee in the 'reservation_attendees' table.
     * @param studentEmail The Bilkent email of the student making the reservation
     * @param facilityName The name of the facility
     * @param date The date of the reservation
     * @param timeSlot The specific time slot (e.g., "14:00-15:30")
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, UNAVAILABLE, or errors.
     */
    public DbStatus insertReservation(String studentEmail, String facilityName, java.time.LocalDate date, String timeSlot) {

        if (!checkFacilityAvailability(facilityName, date, timeSlot)) {
            return DbStatus.UNAVAILABLE; 
        }

        // Ana rezervasyon tablosuna ekleme sorgusu
        String insertResSql = "INSERT INTO reservations " +
                              "(reservation_id, facility_id, reserved_by, reservation_date, time_slot, is_cancelled, has_attended, type) " +
                              "SELECT ?, f.facility_id, u.id, ?, ?, FALSE, FALSE, 'Standard' " +
                              "FROM facilities f, users u " +
                              "WHERE f.name = ? AND u.bilkent_email = ?";
                              
        // Yeni tabloya (katılımcılar) ekleme sorgusu
        String insertAttendeeSql = "INSERT INTO reservation_attendees (reservation_id, student_id) " +
                                   "SELECT ?, id FROM users WHERE bilkent_email = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(insertResSql)) {
            
            java.util.UUID newReservationId = java.util.UUID.randomUUID();
            
            stmt.setObject(1, newReservationId);
            stmt.setDate(2, java.sql.Date.valueOf(date)); 
            stmt.setString(3, timeSlot);
            stmt.setString(4, facilityName);
            stmt.setString(5, studentEmail);

            int insertedRows = stmt.executeUpdate();

            if (insertedRows == 0) {
                return DbStatus.DATA_NOT_FOUND;
            }
            
            // Rezervasyon başarıyla oluştu, kurucuyu otomatik olarak katılımcı yapıyoruz
            try (PreparedStatement attendeeStmt = getConnection().prepareStatement(insertAttendeeSql)) {
                attendeeStmt.setObject(1, newReservationId);
                attendeeStmt.setString(2, studentEmail);
                attendeeStmt.executeUpdate();
            }

            return DbStatus.SUCCESS; 

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the date and time slot of an existing reservation.
     * Attendees naturally shift to the new time since they are linked via Foreign Key.
     * @param reservationId The UUID of the reservation as a String
     * @param newDate The new date for the reservation
     * @param newTimeSlot The new time slot (e.g., "16:00-17:30")
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, UNAVAILABLE, or errors.
     */
    public DbStatus updateReservationTime(String reservationId, java.time.LocalDate newDate, String newTimeSlot) {
        
        String getFacilitySql = "SELECT f.name FROM reservations r " +
                                "INNER JOIN facilities f ON r.facility_id = f.facility_id " +
                                "WHERE r.reservation_id = ? AND r.is_cancelled = FALSE";
                                
        String updateSql = "UPDATE reservations SET reservation_date = ?, time_slot = ? WHERE reservation_id = ?";

        try {
            java.util.UUID resId = java.util.UUID.fromString(reservationId);
            String facilityName = null;

            try (PreparedStatement getStmt = getConnection().prepareStatement(getFacilitySql)) {
                getStmt.setObject(1, resId);
                
                try (ResultSet rs = getStmt.executeQuery()) {
                    if (rs.next()) {
                        facilityName = rs.getString("name");
                    } else {
                        return DbStatus.DATA_NOT_FOUND;
                    }
                }
            }

            if (!checkFacilityAvailability(facilityName, newDate, newTimeSlot)) {
                return DbStatus.UNAVAILABLE; 
            }

            try (PreparedStatement updateStmt = getConnection().prepareStatement(updateSql)) {
                updateStmt.setDate(1, java.sql.Date.valueOf(newDate));
                updateStmt.setString(2, newTimeSlot);
                updateStmt.setObject(3, resId);

                int updatedRows = updateStmt.executeUpdate();
                
                return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;
            }

        } catch (IllegalArgumentException e) {
            return DbStatus.QUERY_ERROR;
        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Cancels an existing reservation (Soft Delete).
     * Sets 'is_cancelled' flag to true. Attendees in 'reservation_attendees' remain untouched for historical logs.
     * @param reservationId The UUID of the reservation to be cancelled
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if not found or already cancelled), or errors.
     */
    public DbStatus deleteReservation(String reservationId) {
        
        String cancelSql = "UPDATE reservations SET is_cancelled = TRUE " +
                           "WHERE reservation_id = ? AND is_cancelled = FALSE";

        try {
            java.util.UUID resId = java.util.UUID.fromString(reservationId);

            try (PreparedStatement stmt = getConnection().prepareStatement(cancelSql)) {
                
                stmt.setObject(1, resId);

                int updatedRows = stmt.executeUpdate();

                return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;
            }

        } catch (IllegalArgumentException e) {
            return DbStatus.QUERY_ERROR;
        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the attendance status of a specific reservation.
     * Only applies to reservations that have not been cancelled.
     * @param reservationId The UUID of the reservation as a String
     * @param hasAttended True if the student attended, false if they missed it
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if invalid ID or already cancelled), or errors.
     */
    public DbStatus updateReservationAttendance(String reservationId, boolean hasAttended) {
        
        // Sadece iptal edilmemiş rezervasyonların katılım durumu güncellenebilir
        String updateSql = "UPDATE reservations SET has_attended = ? " +
                           "WHERE reservation_id = ? AND is_cancelled = FALSE";

        try {
            // String formatındaki ID'yi veritabanı ile uyumlu UUID nesnesine çeviriyoruz
            java.util.UUID resId = java.util.UUID.fromString(reservationId);

            try (PreparedStatement stmt = getConnection().prepareStatement(updateSql)) {
                
                stmt.setBoolean(1, hasAttended);
                stmt.setObject(2, resId);

                int updatedRows = stmt.executeUpdate();

                // Eğer etkilenen satır 0 ise: 
                // Ya böyle bir reservation_id yoktur ya da rezervasyon is_cancelled = TRUE durumundadır.
                return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;
            }

        } catch (IllegalArgumentException e) {
            // Gönderilen reservationId geçerli bir UUID formatında değilse
            return DbStatus.QUERY_ERROR;
        } catch (SQLException e) {
            e.printStackTrace();
            
            // Veritabanı bağlantı hatası kontrolü
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the maintenance status of a specific facility.
     * @param facilityName The name of the facility to update (e.g., "Main Sports Hall - Court A")
     * @param isUnderMaintenance True to put the facility under maintenance, false to make it operational
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if facility doesn't exist), or errors.
     */
    public DbStatus updateFacilityMaintenance(String facilityName, boolean isUnderMaintenance) {
        
        String updateSql = "UPDATE facilities SET is_under_maintenance = ? WHERE name = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(updateSql)) {
            
            stmt.setBoolean(1, isUnderMaintenance);
            stmt.setString(2, facilityName);

            int updatedRows = stmt.executeUpdate();

            return updatedRows > 0 ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Creates a special 'Duello' record for an **EXISTING** reservation.
     * Verifies the ownership of the reservation, updates its type to 'Duello',
     * generates a 6-digit access code, and inserts it into the duellos table.
     * @param reservationId The UUID of the existing reservation as a String
     * @param creatorStudentEmail The Bilkent email of the student who owns the reservation
     * @param requiredSkillLevel The required skill level for the duello (e.g., "Beginner", "Advanced")
     * @param emptySlots The number of empty slots available in the duello
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus insertDuello(String reservationId, String creatorStudentEmail, String requiredSkillLevel, int emptySlots) {
        
        String updateReservationSql = "UPDATE reservations SET type = 'Duello' " +
                                      "WHERE reservation_id = ? " +
                                      "AND reserved_by = (SELECT id FROM users WHERE bilkent_email = ?)";
                                      
        String insertDuelloSql = "INSERT INTO duellos (reservation_id, access_code, required_skill_level, empty_slots, is_matched) " +
                                 "VALUES (?, ?, ?, ?, FALSE)";

        try {
            java.util.UUID resId = java.util.UUID.fromString(reservationId);
            String accessCode = generateRandomCode(6);

            try (PreparedStatement updateStmt = getConnection().prepareStatement(updateReservationSql)) {
                updateStmt.setObject(1, resId);
                updateStmt.setString(2, creatorStudentEmail);

                int updatedRows = updateStmt.executeUpdate();

                if (updatedRows == 0) {
                    return DbStatus.DATA_NOT_FOUND;
                }
            }

            try (PreparedStatement insertStmt = getConnection().prepareStatement(insertDuelloSql)) {
                insertStmt.setObject(1, resId);
                insertStmt.setString(2, accessCode);
                
                insertStmt.setString(3, requiredSkillLevel);
                insertStmt.setInt(4, emptySlots);
                
                int insertedRows = insertStmt.executeUpdate();
                return insertedRows > 0 ? DbStatus.SUCCESS : DbStatus.QUERY_ERROR;
            }

        } catch (IllegalArgumentException e) {
            return DbStatus.QUERY_ERROR;
        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Accepts a duello request and adds the student as an official participant.
     * Updates request status, decrements empty slots, manages matching status,
     * and auto-rejects other pending requests if the duello becomes full.
     * @param reservationId The UUID of the duello/reservation
     * @param studentEmail The Bilkent email of the student being accepted
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateDuelloParticipant(String reservationId, String studentEmail) {
        
        // 1. İsteği kabul et
        String updateRequestSql = "UPDATE duello_requests SET status = 'Accepted' " +
                                  "WHERE reservation_id = ? AND requester_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                                  "AND status = 'Pending'";

        // 2. Kontenjanı düşür ve is_matched durumunu güncelle
        String updateDuelloSql = "UPDATE duellos SET empty_slots = empty_slots - 1, " +
                                 "is_matched = CASE WHEN empty_slots - 1 = 0 THEN TRUE ELSE FALSE END " +
                                 "WHERE reservation_id = ? AND empty_slots > 0";

        // 3. Öğrenciyi katılımcılara ekle
        String insertAttendeeSql = "INSERT INTO reservation_attendees (reservation_id, student_id) " +
                                   "SELECT ?, id FROM users WHERE bilkent_email = ?";
                                   
        // 4. (YENİ) Eğer düello dolduysa, bekleyen diğer tüm istekleri reddet
        String autoRejectOthersSql = "UPDATE duello_requests SET status = 'Rejected' " +
                                     "WHERE reservation_id = ? AND status = 'Pending' " +
                                     "AND (SELECT empty_slots FROM duellos WHERE reservation_id = ?) = 0";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Transaction başlat

            java.util.UUID resId = java.util.UUID.fromString(reservationId);

            try (PreparedStatement stmt = conn.prepareStatement(updateRequestSql)) {
                stmt.setObject(1, resId);
                stmt.setString(2, studentEmail);
                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    return DbStatus.DATA_NOT_FOUND;
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(updateDuelloSql)) {
                stmt.setObject(1, resId);
                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    return DbStatus.QUERY_ERROR;
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(insertAttendeeSql)) {
                stmt.setObject(1, resId);
                stmt.setString(2, studentEmail);
                stmt.executeUpdate();
            }
            
            // YENİ EKLENEN KISIM: Diğer istekleri temizle (Düello dolduysa çalışır)
            try (PreparedStatement stmt = conn.prepareStatement(autoRejectOthersSql)) {
                stmt.setObject(1, resId);
                stmt.setObject(2, resId);
                stmt.executeUpdate(); // Etkilenen satır 0 olsa da sorun yok, hata fırlatmaz
            }

            conn.commit();
            return DbStatus.SUCCESS;

        // EKSİKTİ, EKLENDİ
        } catch (IllegalArgumentException e) {
            return DbStatus.QUERY_ERROR;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

   /**
     * Verifies the access code for a private duello and adds the student as a participant if correct.
     * Checks if the code matches, if the duello is not full, and then updates participants.
     * @param reservationId The UUID of the duello/reservation
     * @param studentEmail The Bilkent email of the student trying to join
     * @param code The 6-digit access code entered by the user
     * @return DbStatus indicating SUCCESS, INVALID_CODE, DATA_NOT_FOUND, or errors.
     */
    public DbStatus verifyAndJoinDuello(String reservationId, String studentEmail, String code) {
        
        String checkCodeSql = "SELECT access_code, empty_slots FROM duellos WHERE reservation_id = ? AND is_matched = FALSE";
        
        String updateDuelloSql = "UPDATE duellos SET empty_slots = empty_slots - 1, " +
                                 "is_matched = CASE WHEN empty_slots - 1 = 0 THEN TRUE ELSE FALSE END " +
                                 "WHERE reservation_id = ? AND access_code = ? AND empty_slots > 0";

        String insertAttendeeSql = "INSERT INTO reservation_attendees (reservation_id, student_id) " +
                                   "SELECT ?, id FROM users WHERE bilkent_email = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // İşlemi atomik hale getir

            java.util.UUID resId = java.util.UUID.fromString(reservationId);

            try (PreparedStatement checkStmt = conn.prepareStatement(checkCodeSql)) {
                checkStmt.setObject(1, resId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        String dbCode = rs.getString("access_code");
                        int slots = rs.getInt("empty_slots");

                        if (!dbCode.equals(code)) {
                            conn.rollback();
                            return DbStatus.INVALID_CODE; // Şifre yanlış
                        }
                        if (slots <= 0) {
                            conn.rollback();
                            return DbStatus.UNAVAILABLE; // Yer kalmamış
                        }
                    } else {
                        conn.rollback();
                        return DbStatus.DATA_NOT_FOUND; // Düello bulunamadı veya çoktan eşleşti
                    }
                }
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateDuelloSql)) {
                updateStmt.setObject(1, resId);
                updateStmt.setString(2, code);
                if (updateStmt.executeUpdate() == 0) {
                    conn.rollback();
                    return DbStatus.QUERY_ERROR;
                }
            }

            try (PreparedStatement attendeeStmt = conn.prepareStatement(insertAttendeeSql)) {
                attendeeStmt.setObject(1, resId);
                attendeeStmt.setString(2, studentEmail);
                attendeeStmt.executeUpdate();
            }

            conn.commit();
            return DbStatus.SUCCESS;

        } catch (IllegalArgumentException e) {
            return DbStatus.QUERY_ERROR;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return (e.getSQLState() != null && e.getSQLState().startsWith("08")) 
                    ? DbStatus.CONNECTION_ERROR : DbStatus.QUERY_ERROR;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
    
    /**
     * Updates the match result and automatically calculates/updates ELO points, 
     * win rates, and matches played for both the creator and the opponent in a single transaction.
     * @param matchId The UUID of the match/duello (reservation_id)
     * @param isCreatorWin TRUE if the creator won, FALSE if the opponent won, NULL for a draw.
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateMatchWinner(String matchId, Boolean isCreatorWin) {
        
        // 1. Maçtaki oyuncuların mevcut istatistiklerini çekmek için sorgu
        String fetchStatsSql = "SELECT r.reserved_by AS c_id, cs.elo_point AS c_elo, cs.matches_played AS c_mp, cs.win_rate AS c_wr, " +
                               "ra.student_id AS o_id, os.elo_point AS o_elo, os.matches_played AS o_mp, os.win_rate AS o_wr " +
                               "FROM reservations r " +
                               "INNER JOIN students cs ON r.reserved_by = cs.user_id " +
                               "INNER JOIN reservation_attendees ra ON r.reservation_id = ra.reservation_id " +
                               "INNER JOIN students os ON ra.student_id = os.user_id " +
                               "WHERE r.reservation_id = ?";

        // 2. Duello sonucunu güncellemek için sorgu
        String updateDuelloSql = "UPDATE duellos SET is_creator_win = ? WHERE reservation_id = ?";

        // 3. Öğrenci istatistiklerini güncellemek için sorgu
        String updateStudentSql = "UPDATE students SET elo_point = ?, matches_played = ?, win_rate = ? WHERE user_id = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            java.util.UUID resId = java.util.UUID.fromString(matchId);

            java.util.UUID creatorId = null;
            int c_elo = 0, c_mp = 0;
            double c_wr = 0.0;

            java.util.UUID opponentId = null;
            int o_elo = 0, o_mp = 0;
            double o_wr = 0.0;

            try (PreparedStatement fetchStmt = conn.prepareStatement(fetchStatsSql)) {
                fetchStmt.setObject(1, resId);
                try (ResultSet rs = fetchStmt.executeQuery()) {
                    if (rs.next()) {
                        creatorId = (java.util.UUID) rs.getObject("c_id");
                        c_elo = rs.getInt("c_elo");
                        c_mp = rs.getInt("c_mp");
                        c_wr = rs.getDouble("c_wr");

                        opponentId = (java.util.UUID) rs.getObject("o_id");
                        o_elo = rs.getInt("o_elo");
                        o_mp = rs.getInt("o_mp");
                        o_wr = rs.getDouble("o_wr");
                    } else {
                        conn.rollback();
                        return DbStatus.DATA_NOT_FOUND; // Maç veya katılımcı bulunamadı
                    }
                }
            }

            double c_score = (isCreatorWin == null) ? 0.5 : (isCreatorWin ? 1.0 : 0.0);
            double o_score = (isCreatorWin == null) ? 0.5 : (isCreatorWin ? 0.0 : 1.0);

            double expected_c = 1.0 / (1.0 + Math.pow(10.0, (o_elo - c_elo) / 400.0));
            double expected_o = 1.0 / (1.0 + Math.pow(10.0, (c_elo - o_elo) / 400.0));

            int K = 32;
            int new_c_elo = c_elo + (int) Math.round(K * (c_score - expected_c));
            int new_o_elo = o_elo + (int) Math.round(K * (o_score - expected_o));

            int c_wins = (int) Math.round(c_mp * c_wr) + (c_score == 1.0 ? 1 : 0);
            int o_wins = (int) Math.round(o_mp * o_wr) + (o_score == 1.0 ? 1 : 0);
            
            int new_c_mp = c_mp + 1;
            int new_o_mp = o_mp + 1;
            
            double new_c_wr = (double) c_wins / new_c_mp;
            double new_o_wr = (double) o_wins / new_o_mp;


            try (PreparedStatement duelloStmt = conn.prepareStatement(updateDuelloSql)) {
                if (isCreatorWin == null) {
                    duelloStmt.setNull(1, java.sql.Types.BOOLEAN);
                } else {
                    duelloStmt.setBoolean(1, isCreatorWin);
                }
                duelloStmt.setObject(2, resId);
                duelloStmt.executeUpdate();
            }

            try (PreparedStatement updateCStmt = conn.prepareStatement(updateStudentSql)) {
                updateCStmt.setInt(1, new_c_elo);
                updateCStmt.setInt(2, new_c_mp);
                updateCStmt.setDouble(3, new_c_wr);
                updateCStmt.setObject(4, creatorId);
                updateCStmt.executeUpdate();
            }

            try (PreparedStatement updateOStmt = conn.prepareStatement(updateStudentSql)) {
                updateOStmt.setInt(1, new_o_elo);
                updateOStmt.setInt(2, new_o_mp);
                updateOStmt.setDouble(3, new_o_wr);
                updateOStmt.setObject(4, opponentId);
                updateOStmt.executeUpdate();
            }

            conn.commit(); // Tüm güncellemeler başarılıysa veritabanına kalıcı olarak kaydet
            return DbStatus.SUCCESS;

        } catch (IllegalArgumentException e) {
            return DbStatus.QUERY_ERROR;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return (e.getSQLState() != null && e.getSQLState().startsWith("08")) 
                    ? DbStatus.CONNECTION_ERROR : DbStatus.QUERY_ERROR;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * Retrieves all facilities from the database and maps them to Facility objects.
     * @return An ArrayList containing all Facility objects found in the database.
     */
    public ArrayList<Facility> getFacilities() {
        
        ArrayList<Facility> facilitiesList = new ArrayList<>();
        
        String sql = "SELECT f.facility_id, f.name, f.campus_location, f.capacity, f.is_under_maintenance, s.name AS sport_name " +
                     "FROM facilities f " +
                     "INNER JOIN sports s ON f.sport_id = s.id";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getObject("facility_id").toString();
                String name = rs.getString("name");
                String location = rs.getString("campus_location");
                int capacity = rs.getInt("capacity");
                boolean maintenance = rs.getBoolean("is_under_maintenance");
                
                models.SportType st = null;
                try {
                    String sportName = rs.getString("sport_name");
                    if (sportName != null) {
                        String formattedSportName = sportName.trim().toUpperCase().replace(" ", "_");
                        st = models.SportType.valueOf(formattedSportName);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Uyarı: Geçersiz veya eşleşmeyen spor türü bulundu -> " + rs.getString("sport_name"));
                }

                models.Facility facility = new models.Facility(id, name, location, st, capacity);
                facility.setUnderMaintenance(maintenance);
                
                facilitiesList.add(facility);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return facilitiesList;
    }

    /**
     * Retrieves all reservations associated with a specific student's email.
     * Includes both the reservations they created and the ones they joined as an attendee.
     * Also fetches the associated Facility and SportType data.
     * @param studentEmail The Bilkent email of the student
     * @return An ArrayList of Reservation objects ordered by date and time (newest first).
     */
    public ArrayList<Reservation> getReservationsByEmail(String studentEmail) {
        
        ArrayList<Reservation> reservationsList = new java.util.ArrayList<>();

        String sql = "SELECT r.reservation_id, r.reservation_date, r.time_slot, r.is_cancelled, r.has_attended, " +
                     "f.facility_id, f.name AS facility_name, f.campus_location, f.capacity, f.is_under_maintenance, " +
                     "s.name AS sport_name " +
                     "FROM reservations r " +
                     "INNER JOIN reservation_attendees ra ON r.reservation_id = ra.reservation_id " +
                     "INNER JOIN users u ON ra.student_id = u.id " +
                     "INNER JOIN facilities f ON r.facility_id = f.facility_id " +
                     "INNER JOIN sports s ON f.sport_id = s.id " +
                     "WHERE u.bilkent_email = ? " +
                     "ORDER BY r.reservation_date DESC, r.time_slot DESC";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, studentEmail);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    
                    String facilityId = rs.getObject("facility_id").toString();
                    String facilityName = rs.getString("facility_name");
                    String location = rs.getString("campus_location");
                    int capacity = rs.getInt("capacity");
                    boolean maintenance = rs.getBoolean("is_under_maintenance");
                    
                    models.SportType st = null;
                    try {
                        String sportName = rs.getString("sport_name");
                        if (sportName != null) {
                            st = models.SportType.valueOf(sportName.trim().toUpperCase().replace(" ", "_"));
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Uyarı: Geçersiz spor türü -> " + rs.getString("sport_name"));
                    }

                    models.Facility facility = new models.Facility(facilityId, facilityName, location, st, capacity);
                    facility.setUnderMaintenance(maintenance);


                    String reservationId = rs.getObject("reservation_id").toString();
                    String timeSlot = rs.getString("time_slot");
                    boolean isCancelled = rs.getBoolean("is_cancelled");
                    boolean hasAttended = rs.getBoolean("has_attended");
                    
                    java.sql.Date sqlDate = rs.getDate("reservation_date");
                    java.time.LocalDate resDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;

                    models.Reservation reservation = new models.Reservation(reservationId, facility, resDate, timeSlot);
                    reservation.setCancelled(isCancelled);
                    reservation.setHasAttended(hasAttended);
                    
                    reservationsList.add(reservation);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reservationsList;
    }

 /**
     * Fetches the accepted friends of a student based on their email
     * and populates the 'friends' list inside the provided Student object.
     * @param currentStudent The Student object to be populated with friends
     * @return The updated Student object containing their friends list
     */
    public Student fillFriendsByEmail(Student currentStudent) {
        
        if (currentStudent == null) {
            return null;
        }

        String email = currentStudent.getBilkentEmail();
        java.util.List<models.Student> friendsList = new java.util.ArrayList<>();

        // u alias'ları friend olarak değiştirildi.
        // user_id_1 ve user_id_2 kolonları requester_id ve receiver_id olarak güncellendi.
        String sql = "SELECT friend.full_name, friend.bilkent_email, friend.student_id AS uni_id, " +
                     "s.elo_point, s.penalty_points, s.reliability_score, s.matches_played, s.win_rate " +
                     "FROM friendships f " +
                     "INNER JOIN users me ON (me.id = f.requester_id OR me.id = f.receiver_id) " +
                     "INNER JOIN users friend ON (friend.id = f.requester_id OR friend.id = f.receiver_id) " +
                     "INNER JOIN students s ON friend.id = s.user_id " +
                     "WHERE me.bilkent_email = ? " +
                     "  AND friend.id != me.id " +
                     "  AND f.status = 'Accepted'";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    models.Student friend = new Student(rs.getString("full_name"), rs.getString("bilkent_email"), rs.getString("uni_id"));
                    
                    friend.setEloPoint(rs.getInt("elo_point"));
                    friend.setPenaltyPoints(rs.getInt("penalty_points"));
                    friend.setReliabilityScore(rs.getDouble("reliability_score"));
                    friend.setMatchesPlayed(rs.getInt("matches_played"));
                    friend.setWinRate(rs.getDouble("win_rate"));
                    
                    int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                    friend.setMatchesWon(matchesWon);

                    friendsList.add(friend);
                }
            }
            
            currentStudent.setFriends(friendsList);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return currentStudent;
    }

/**
     * Fetches the incoming friend requests (Pending) for a student based on their email
     * and populates the 'incomingFriendRequests' list inside the provided Student object.
     * @param currentStudent The Student object to be populated
     * @return The updated Student object
     */
    public Student fillIncomingFriendRequests(Student currentStudent) {
        
        if (currentStudent == null) {
            return null;
        }

        String email = currentStudent.getBilkentEmail();
        java.util.List<models.Student> incomingList = new java.util.ArrayList<>();

        // Sen (me) receiver'sın. Bize isteği atan requester'ın verileri lazım.
        String sql = "SELECT req.full_name, req.bilkent_email, req.student_id AS uni_id, " +
                     "s.elo_point, s.penalty_points, s.reliability_score, s.matches_played, s.win_rate " +
                     "FROM friendships f " +
                     "INNER JOIN users me ON me.id = f.receiver_id " +
                     "INNER JOIN users req ON req.id = f.requester_id " +
                     "INNER JOIN students s ON req.id = s.user_id " +
                     "WHERE me.bilkent_email = ? " +
                     "  AND f.status = 'Pending'";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    models.Student requester = new Student(
                        rs.getString("full_name"), 
                        rs.getString("bilkent_email"), 
                        rs.getString("uni_id")
                    );
                    
                    requester.setEloPoint(rs.getInt("elo_point"));
                    requester.setPenaltyPoints(rs.getInt("penalty_points"));
                    requester.setReliabilityScore(rs.getDouble("reliability_score"));
                    requester.setMatchesPlayed(rs.getInt("matches_played"));
                    requester.setWinRate(rs.getDouble("win_rate"));
                    
                    int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                    requester.setMatchesWon(matchesWon);

                    incomingList.add(requester);
                }
            }
            
            currentStudent.setIncomingFriendRequests(incomingList);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return currentStudent;
    }

    /**
     * Fetches the outgoing friend requests (Pending) sent by a student based on their email
     * and populates the 'outgoingFriendRequests' list inside the provided Student object.
     * @param currentStudent The Student object to be populated
     * @return The updated Student object
     */
    public Student fillOutgoingFriendRequests(Student currentStudent) {
        
        if (currentStudent == null) {
            return null;
        }

        String email = currentStudent.getBilkentEmail();
        java.util.List<models.Student> outgoingList = new java.util.ArrayList<>();

        // Sen (me) requester'sın. Bize isteği attığın receiver'ın verileri lazım.
        String sql = "SELECT rec.full_name, rec.bilkent_email, rec.student_id AS uni_id, " +
                     "s.elo_point, s.penalty_points, s.reliability_score, s.matches_played, s.win_rate " +
                     "FROM friendships f " +
                     "INNER JOIN users me ON me.id = f.requester_id " +
                     "INNER JOIN users rec ON rec.id = f.receiver_id " +
                     "INNER JOIN students s ON rec.id = s.user_id " +
                     "WHERE me.bilkent_email = ? " +
                     "  AND f.status = 'Pending'";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    models.Student receiver = new Student(
                        rs.getString("full_name"), 
                        rs.getString("bilkent_email"), 
                        rs.getString("uni_id")
                    );
                    
                    receiver.setEloPoint(rs.getInt("elo_point"));
                    receiver.setPenaltyPoints(rs.getInt("penalty_points"));
                    receiver.setReliabilityScore(rs.getDouble("reliability_score"));
                    receiver.setMatchesPlayed(rs.getInt("matches_played"));
                    receiver.setWinRate(rs.getDouble("win_rate"));
                    
                    int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                    receiver.setMatchesWon(matchesWon);

                    outgoingList.add(receiver);
                }
            }
            
            currentStudent.setOutgoingFriendRequests(outgoingList);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return currentStudent;
    }

    /**
     * Finds up to 5 suitable open Duellos for a specific sport.
     * Searches for active duellos created by other users, ordered by how close 
     * the creator's ELO point is to the current student's ELO.
     * Also fetches the creator of each duello and adds them to the duello's attendees list.
     * @param currentStudent The student looking for a match
     * @param sportName The name of the sport (e.g., "TENNIS" or "TABLE TENNIS")
     * @return An ArrayList containing a maximum of 5 matching Duello objects.
     */
    public ArrayList<models.Duello> findOpponentForMatch(models.Student currentStudent, String sportName) {
        
        ArrayList<models.Duello> suitableDuellos = new ArrayList<>();

        if (currentStudent == null || sportName == null || sportName.trim().isEmpty()) {
            return suitableDuellos; 
        }

        String formattedSportName = sportName.trim().toUpperCase().replace(" ", "_");

        // SELECT kısmına kurucunun (creator_u ve creator_s) tüm kişisel ve istatistiksel verileri eklendi
        String sql = "SELECT d.reservation_id, d.access_code, d.required_skill_level, d.empty_slots, d.is_matched, " +
                     "r.reservation_date, r.time_slot, r.is_cancelled, r.has_attended, " +
                     "f.facility_id, f.name AS facility_name, f.campus_location, f.capacity, f.is_under_maintenance, " +
                     "sp.name AS sport_name, " +
                     "creator_u.full_name, creator_u.bilkent_email, creator_u.student_id AS uni_id, " +
                     "creator_s.elo_point, creator_s.penalty_points, creator_s.reliability_score, creator_s.matches_played, creator_s.win_rate " +
                     "FROM duellos d " +
                     "INNER JOIN reservations r ON d.reservation_id = r.reservation_id " +
                     "INNER JOIN facilities f ON r.facility_id = f.facility_id " +
                     "INNER JOIN sports sp ON f.sport_id = sp.id " +
                     "INNER JOIN users creator_u ON r.reserved_by = creator_u.id " +
                     "INNER JOIN students creator_s ON creator_u.id = creator_s.user_id " +
                     "WHERE creator_u.bilkent_email != ? " + // Kendi açtığın düelloları görme
                     "  AND d.is_matched = FALSE " +         // Eşleşme henüz tamamlanmamış olsun
                     "  AND d.empty_slots > 0 " +            // Boş yer olsun
                     "  AND r.is_cancelled = FALSE " +       // İptal edilmemiş olsun
                     "  AND r.reservation_date >= CURRENT_DATE " + // Geçmiş tarihli düelloları gösterme
                     "  AND UPPER(REPLACE(sp.name, ' ', '_')) = ? " + // Seçilen spor türü
                     "ORDER BY ABS(creator_s.elo_point - ?) ASC " + // Kurucunun ELO'su senin ELO'na en yakın olanlar üstte
                     "LIMIT 5";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, currentStudent.getBilkentEmail());
            stmt.setString(2, formattedSportName);
            stmt.setInt(3, currentStudent.getEloPoint()); // Senin ELO'nu referans alıyoruz

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    
                    // 1. Tesis (Facility) Objesini Oluşturma
                    String facilityId = rs.getObject("facility_id").toString();
                    String facilityName = rs.getString("facility_name");
                    String location = rs.getString("campus_location");
                    int capacity = rs.getInt("capacity");
                    boolean maintenance = rs.getBoolean("is_under_maintenance");
                    
                    models.SportType st = null;
                    try {
                        String fetchedSportName = rs.getString("sport_name");
                        if (fetchedSportName != null) {
                            st = models.SportType.valueOf(fetchedSportName.trim().toUpperCase().replace(" ", "_"));
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Uyarı: Geçersiz spor türü -> " + rs.getString("sport_name"));
                    }

                    models.Facility facility = new models.Facility(facilityId, facilityName, location, st, capacity);
                    facility.setUnderMaintenance(maintenance);

                    // 2. Duello ve Reservation Özelliklerini Çekme
                    String reservationId = rs.getObject("reservation_id").toString();
                    java.sql.Date sqlDate = rs.getDate("reservation_date");
                    java.time.LocalDate resDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;
                    String timeSlot = rs.getString("time_slot");
                    
                    String accessCode = rs.getString("access_code");
                    String reqSkill = rs.getString("required_skill_level");
                    int slots = rs.getInt("empty_slots");
                    boolean matched = rs.getBoolean("is_matched");
                    boolean cancelled = rs.getBoolean("is_cancelled");
                    boolean attended = rs.getBoolean("has_attended");

                    // 3. Duello Objesini Oluşturma
                    models.Duello duello = new models.Duello(
                        reservationId, facility, resDate, timeSlot, accessCode, reqSkill, slots
                    );
                    duello.setMatched(matched);
                    duello.setCancelled(cancelled);
                    duello.setHasAttended(attended);

                    // 4. Kurucu (Creator) Objesini Oluşturma ve Ekleme
                    models.Student creator = new models.Student(
                        rs.getString("full_name"), 
                        rs.getString("bilkent_email"), 
                        rs.getString("uni_id")
                    );
                    
                    creator.setEloPoint(rs.getInt("elo_point"));
                    creator.setPenaltyPoints(rs.getInt("penalty_points"));
                    creator.setReliabilityScore(rs.getDouble("reliability_score"));
                    creator.setMatchesPlayed(rs.getInt("matches_played"));
                    creator.setWinRate(rs.getDouble("win_rate"));
                    
                    int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                    creator.setMatchesWon(matchesWon);

                    // Eğer attendees listesi null ise hata almamak için kontrol edip başlatıyoruz
                    if (duello.getAttendees() == null) {
                        duello.setAttendees(new java.util.ArrayList<>());
                    }
                    
                    // Kurucuyu katılımcı listesine ekliyoruz (İlk eleman / get(0) olarak erişilebilir)
                    duello.getAttendees().add(creator);
                    
                    suitableDuellos.add(duello);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return suitableDuellos;
    }
    
/**
     * Deletes a duello request from the database.
     * Can be used to either cancel an outgoing request or reject an incoming request.
     * @param reservationId The UUID of the duello/reservation
     * @param requesterEmail The Bilkent email of the student who made the request
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus deleteDuelloRequest(String reservationId, String requesterEmail) {
        
        // İlgili rezervasyon ID'sine ve isteği atan kişinin email'ine göre silme işlemi
        String deleteSql = "DELETE FROM duello_requests " +
                           "WHERE reservation_id = ? " +
                           "AND requester_id = (SELECT id FROM users WHERE bilkent_email = ?)";

        try {
            java.util.UUID resId = java.util.UUID.fromString(reservationId);

            try (PreparedStatement stmt = getConnection().prepareStatement(deleteSql)) {
                
                stmt.setObject(1, resId);
                stmt.setString(2, requesterEmail);

                int deletedRows = stmt.executeUpdate();

                // Eğer etkilenen satır 0 ise: Böyle bir istek veritabanında yok demektir (zaten silinmiş veya hiç açılmamış)
                if (deletedRows == 0) {
                    return DbStatus.DATA_NOT_FOUND; 
                }

                return DbStatus.SUCCESS;
            }

        } catch (IllegalArgumentException e) {
            return DbStatus.QUERY_ERROR; // String'den UUID'ye çevirirken format hatası olursa
        } catch (SQLException e) {
            e.printStackTrace();
            
            // Veritabanı bağlantı hatası kontrolü
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Deletes a duello, reverts the associated reservation back to 'Standard' type,
     * and removes all other attendees from the reservation except the creator.
     * Ensures that only the creator of the reservation can perform this action.
     * Note: Due to ON DELETE CASCADE, all associated duello requests will also be deleted automatically.
     * @param reservationId The UUID of the reservation/duello
     * @param creatorEmail The Bilkent email of the student who owns the reservation
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus deleteDuello(String reservationId, String creatorEmail) {
        
        // 1. Rezervasyonun sahibini doğrula ve tipini tekrar 'Standard' yap
        String updateResSql = "UPDATE reservations SET type = 'Standard' " +
                              "WHERE reservation_id = ? AND reserved_by = (SELECT id FROM users WHERE bilkent_email = ?)";
                              
        // 2. Duellos tablosundan kaydı sil (ON DELETE CASCADE ile istekler de silinir)
        String deleteDuelloSql = "DELETE FROM duellos WHERE reservation_id = ?";
        
        // 3. Kurucu (creator) hariç diğer tüm katılımcıları rezervasyondan çıkar
        String deleteAttendeesSql = "DELETE FROM reservation_attendees " +
                                    "WHERE reservation_id = ? AND student_id != (SELECT id FROM users WHERE bilkent_email = ?)";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Transaction başlat

            java.util.UUID resId = java.util.UUID.fromString(reservationId);

            // ADIM 1: Rezervasyonu güncelle ve yetkiyi kontrol et
            try (PreparedStatement updateStmt = conn.prepareStatement(updateResSql)) {
                updateStmt.setObject(1, resId);
                updateStmt.setString(2, creatorEmail);
                
                int updatedRows = updateStmt.executeUpdate();

                // Eğer 0 satır güncellendiyse: Ya ID yanlıştır ya da bu işlemi yapmaya çalışan kişi kurucu değildir
                if (updatedRows == 0) {
                    conn.rollback();
                    return DbStatus.DATA_NOT_FOUND; 
                }
            }

            // ADIM 2: Duelloyu sil
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteDuelloSql)) {
                deleteStmt.setObject(1, resId);
                deleteStmt.executeUpdate(); 
            }
            
            // ADIM 3: Diğer katılımcıları (rakipleri) rezervasyondan çıkar
            try (PreparedStatement attendeeStmt = conn.prepareStatement(deleteAttendeesSql)) {
                attendeeStmt.setObject(1, resId);
                attendeeStmt.setString(2, creatorEmail);
                attendeeStmt.executeUpdate(); // Kimse yoksa 0 satır etkilenir, sorun teşkil etmez.
            }

            conn.commit(); // Tüm işlemleri onayla
            return DbStatus.SUCCESS;

        } catch (IllegalArgumentException e) {
            return DbStatus.QUERY_ERROR;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * Sends a join request to an open duello.
     * Ensures the duello is not already matched and has empty slots available.
     * @param reservationId The UUID of the duello/reservation
     * @param studentEmail The Bilkent email of the requester
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if duello is full/unavailable), or errors.
     */
    public DbStatus insertDuelloRequest(String reservationId, String studentEmail) {
        
        String insertSql = "INSERT INTO duello_requests (reservation_id, requester_id, status) " +
                           "SELECT d.reservation_id, u.id, 'Pending' " +
                           "FROM users u, duellos d " +
                           "WHERE u.bilkent_email = ? " +
                           "  AND d.reservation_id = ? " +
                           "  AND d.is_matched = FALSE " +
                           "  AND d.empty_slots > 0";

        try {
            java.util.UUID resId = java.util.UUID.fromString(reservationId);

            try (PreparedStatement stmt = getConnection().prepareStatement(insertSql)) {
                
                stmt.setString(1, studentEmail);
                stmt.setObject(2, resId);

                int insertedRows = stmt.executeUpdate();
                if (insertedRows == 0) {
                    return DbStatus.DATA_NOT_FOUND; 
                }

                return DbStatus.SUCCESS;
            }

        } catch (IllegalArgumentException e) {
            return DbStatus.QUERY_ERROR;
        } catch (SQLException e) {
            e.printStackTrace();
            
            if ("23505".equals(e.getSQLState())) {
                return DbStatus.QUERY_ERROR; 
            }
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Inserts a notification into the database.
     * If the target is "BROADCAST", target_user_id is set to NULL (visible to everyone).
     * If a specific target is provided, it looks up the user's UUID by their Bilkent email.
     * @param target "BROADCAST" for everyone, or the Bilkent email of the specific user
     * @param title The title of the notification
     * @param message The content of the notification
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if specific user doesn't exist), or errors.
     */
    public DbStatus insertNotification(String targetEmail, String title, String message) {
        String target = targetEmail;
        if (target == null || title == null || message == null) {
            return DbStatus.QUERY_ERROR;
        }

        boolean isBroadcast = target.equalsIgnoreCase("BROADCAST");
        String sql;

        if (isBroadcast) {
            sql = "INSERT INTO notifications (target_user_id, title, message) VALUES (NULL, ?, ?)";
        } else {
            sql = "INSERT INTO notifications (target_user_id, title, message) " +
                  "SELECT id, ?, ? FROM users WHERE bilkent_email = ?";
        }

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            if (isBroadcast) {
                stmt.setString(1, title);
                stmt.setString(2, message);
            } else {
                stmt.setString(1, title);
                stmt.setString(2, message);
                stmt.setString(3, target); // Spesifik kullanıcının emaili
            }

            int insertedRows = stmt.executeUpdate();

            if (!isBroadcast && insertedRows == 0) {
                return DbStatus.DATA_NOT_FOUND;
            }

            return DbStatus.SUCCESS;

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Retrieves all notifications for a specific student.
     * This includes specific notifications targeted at the student AND system-wide broadcasts (target_user_id IS NULL).
     * @param currentStudent The student requesting their notifications
     * @return An ArrayList of Notification objects ordered by date (newest first).
     */
    public ArrayList<models.Notification> getNotificationsByStudent(models.Student currentStudent) {
        
        ArrayList<models.Notification> notifications = new ArrayList<>();

        if (currentStudent == null || currentStudent.getBilkentEmail() == null) {
            return notifications;
        }

        String sql = "SELECT notification_id, title, message, created_date " +
                     "FROM notifications " +
                     "WHERE target_user_id IS NULL " +
                     "   OR target_user_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                     "ORDER BY created_date DESC";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, currentStudent.getBilkentEmail());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    
                    String notificationId = rs.getString("notification_id");
                    String title = rs.getString("title");
                    String message = rs.getString("message");
                    
                    java.sql.Timestamp dbTimestamp = rs.getTimestamp("created_date");

                    models.Notification notification = new models.Notification(notificationId, title, message);
                    
                    if (dbTimestamp != null) {
                        notification.setDate(dbTimestamp.toLocalDateTime());
                    }
                    
                    notifications.add(notification);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return notifications;
    }

    /**
     * Retrieves all Duellos associated with a specific student.
     * This includes duellos created by the student AND duellos the student has joined as an attendee.
     * Also fetches the creator of each duello and adds them to the duello's attendees list.
     * @param currentStudent The student whose duellos are being fetched
     * @return An ArrayList of Duello objects ordered by date (newest first).
     */
    public ArrayList<models.Duello> getUserDuellos(models.Student currentStudent) {
        
        ArrayList<models.Duello> userDuellos = new ArrayList<>();

        if (currentStudent == null || currentStudent.getBilkentEmail() == null) {
            return userDuellos;
        }

        // Sorguya kurucunun (creator_u ve creator_s) bilgileri eklendi
        String sql = "SELECT DISTINCT d.reservation_id, d.access_code, d.required_skill_level, d.empty_slots, d.is_matched, " +
                     "r.reservation_date, r.time_slot, r.is_cancelled, r.has_attended, " +
                     "f.facility_id, f.name AS facility_name, f.campus_location, f.capacity, f.is_under_maintenance, " +
                     "sp.name AS sport_name, " +
                     "creator_u.full_name, creator_u.bilkent_email, creator_u.student_id AS uni_id, " +
                     "creator_s.elo_point, creator_s.penalty_points, creator_s.reliability_score, creator_s.matches_played, creator_s.win_rate " +
                     "FROM duellos d " +
                     "INNER JOIN reservations r ON d.reservation_id = r.reservation_id " +
                     "INNER JOIN facilities f ON r.facility_id = f.facility_id " +
                     "LEFT JOIN sports sp ON f.sport_id = sp.id " +
                     "LEFT JOIN reservation_attendees ra ON r.reservation_id = ra.reservation_id " +
                     "INNER JOIN users creator_u ON r.reserved_by = creator_u.id " +
                     "INNER JOIN students creator_s ON creator_u.id = creator_s.user_id " +
                     "WHERE r.reserved_by = (SELECT id FROM users WHERE bilkent_email = ?) " +
                     "   OR ra.student_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                     "ORDER BY r.reservation_date DESC, r.time_slot DESC";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            String email = currentStudent.getBilkentEmail();
            stmt.setString(1, email);
            stmt.setString(2, email);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    
                    // 1. Tesis (Facility) Objesini Oluşturma
                    String facilityId = rs.getObject("facility_id").toString();
                    String facilityName = rs.getString("facility_name");
                    String location = rs.getString("campus_location");
                    int capacity = rs.getInt("capacity");
                    boolean maintenance = rs.getBoolean("is_under_maintenance");
                    
                    models.SportType st = null;
                    try {
                        String sportName = rs.getString("sport_name");
                        if (sportName != null) {
                            st = models.SportType.valueOf(sportName.trim().toUpperCase().replace(" ", "_"));
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Uyarı: Geçersiz spor türü -> " + rs.getString("sport_name"));
                    }

                    models.Facility facility = new models.Facility(facilityId, facilityName, location, st, capacity);
                    facility.setUnderMaintenance(maintenance);

                    // 2. Duello ve Reservation Özelliklerini Çekme
                    String reservationId = rs.getObject("reservation_id").toString();
                    java.sql.Date sqlDate = rs.getDate("reservation_date");
                    java.time.LocalDate resDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;
                    String timeSlot = rs.getString("time_slot");
                    
                    String accessCode = rs.getString("access_code");
                    String reqSkill = rs.getString("required_skill_level");
                    int slots = rs.getInt("empty_slots");
                    boolean matched = rs.getBoolean("is_matched");
                    boolean cancelled = rs.getBoolean("is_cancelled");
                    boolean attended = rs.getBoolean("has_attended");

                    // 3. Duello Objesini Oluşturma
                    models.Duello duello = new models.Duello(
                            reservationId, 
                            facility, 
                            resDate, 
                            timeSlot, 
                            accessCode, 
                            reqSkill, 
                            slots
                    );
                    
                    duello.setMatched(matched);
                    duello.setCancelled(cancelled);
                    duello.setHasAttended(attended);

                    // 4. Kurucu (Creator) Objesini Oluşturma ve Ekleme
                    models.Student creator = new models.Student(
                        rs.getString("full_name"), 
                        rs.getString("bilkent_email"), 
                        rs.getString("uni_id")
                    );
                    
                    creator.setEloPoint(rs.getInt("elo_point"));
                    creator.setPenaltyPoints(rs.getInt("penalty_points"));
                    creator.setReliabilityScore(rs.getDouble("reliability_score"));
                    creator.setMatchesPlayed(rs.getInt("matches_played"));
                    creator.setWinRate(rs.getDouble("win_rate"));
                    
                    int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                    creator.setMatchesWon(matchesWon);

                    // Listeyi güvenli bir şekilde başlat ve kurucuyu ekle
                    if (duello.getAttendees() == null) {
                        duello.setAttendees(new java.util.ArrayList<>());
                    }
                    duello.getAttendees().add(creator);
                    
                    userDuellos.add(duello);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userDuellos;
    }
    /**
     * Retrieves a specific Duello based on its unique access code.
     * Also fetches the creator of the duello and adds them to the attendees list.
     * @param code The 6-digit access code of the duello
     * @return The matching Duello object, or null if not found
     */
    public models.Duello getDuelloByCode(String code) {
        
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT d.reservation_id, d.access_code, d.required_skill_level, d.empty_slots, d.is_matched, " +
                     "r.reservation_date, r.time_slot, r.is_cancelled, r.has_attended, " +
                     "f.facility_id, f.name AS facility_name, f.campus_location, f.capacity, f.is_under_maintenance, " +
                     "sp.name AS sport_name, " +
                     "u.full_name, u.bilkent_email, u.student_id AS uni_id, " +
                     "s.elo_point, s.penalty_points, s.reliability_score, s.matches_played, s.win_rate " +
                     "FROM duellos d " +
                     "INNER JOIN reservations r ON d.reservation_id = r.reservation_id " +
                     "INNER JOIN facilities f ON r.facility_id = f.facility_id " +
                     "LEFT JOIN sports sp ON f.sport_id = sp.id " +
                     "INNER JOIN users u ON r.reserved_by = u.id " +
                     "INNER JOIN students s ON u.id = s.user_id " +
                     "WHERE d.access_code = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, code);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    
                    String facilityId = rs.getObject("facility_id").toString();
                    String facilityName = rs.getString("facility_name");
                    String location = rs.getString("campus_location");
                    int capacity = rs.getInt("capacity");
                    boolean maintenance = rs.getBoolean("is_under_maintenance");
                    
                    models.SportType st = null;
                    try {
                        String sportName = rs.getString("sport_name");
                        if (sportName != null) {
                            st = models.SportType.valueOf(sportName.trim().toUpperCase().replace(" ", "_"));
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Uyarı: Geçersiz spor türü -> " + rs.getString("sport_name"));
                    }

                    models.Facility facility = new models.Facility(facilityId, facilityName, location, st, capacity);
                    facility.setUnderMaintenance(maintenance);

                    String reservationId = rs.getObject("reservation_id").toString();
                    java.sql.Date sqlDate = rs.getDate("reservation_date");
                    java.time.LocalDate resDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;
                    String timeSlot = rs.getString("time_slot");
                    
                    String accessCode = rs.getString("access_code");
                    String reqSkill = rs.getString("required_skill_level");
                    int slots = rs.getInt("empty_slots");
                    boolean matched = rs.getBoolean("is_matched");
                    boolean cancelled = rs.getBoolean("is_cancelled");
                    boolean attended = rs.getBoolean("has_attended");

                    models.Duello duello = new models.Duello(
                            reservationId, 
                            facility, 
                            resDate, 
                            timeSlot, 
                            accessCode, 
                            reqSkill, 
                            slots
                    );
                    
                    duello.setMatched(matched);
                    duello.setCancelled(cancelled);
                    duello.setHasAttended(attended);

                    models.Student creator = new models.Student(
                        rs.getString("full_name"), 
                        rs.getString("bilkent_email"), 
                        rs.getString("uni_id")
                    );
                    
                    creator.setEloPoint(rs.getInt("elo_point"));
                    creator.setPenaltyPoints(rs.getInt("penalty_points"));
                    creator.setReliabilityScore(rs.getDouble("reliability_score"));
                    creator.setMatchesPlayed(rs.getInt("matches_played"));
                    creator.setWinRate(rs.getDouble("win_rate"));
                    
                    int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                    creator.setMatchesWon(matchesWon);

                    if (duello.getAttendees() == null) {
                        duello.setAttendees(new java.util.ArrayList<>());
                    }
                    
                    duello.getAttendees().add(creator);
                    
                    return duello; // Bulunan düelloyu döndür
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // Düello bulunamazsa veya hata çıkarsa null döner
    }

    /**
     * Retrieves a list of students who have 'Pending' requests to join a specific duello.
     * @param reservationId The UUID of the duello/reservation
     * @return An ArrayList of Student objects representing the requesters.
     */
    public ArrayList<models.Student> getPendingRequestsForDuello(String reservationId) {
        
        ArrayList<models.Student> pendingStudents = new ArrayList<>();

        if (reservationId == null || reservationId.trim().isEmpty()) {
            return pendingStudents;
        }

        // duello_requests tablosu üzerinden users ve students tablolarını birleştirerek 
        // istek atan kişinin tüm istatistiklerini çekiyoruz.
        String sql = "SELECT u.full_name, u.bilkent_email, u.student_id AS uni_id, " +
                     "s.elo_point, s.penalty_points, s.reliability_score, s.matches_played, s.win_rate " +
                     "FROM duello_requests dr " +
                     "INNER JOIN users u ON dr.requester_id = u.id " +
                     "INNER JOIN students s ON u.id = s.user_id " +
                     "WHERE dr.reservation_id = ? AND dr.status = 'Pending'";

        try {
            java.util.UUID resId = java.util.UUID.fromString(reservationId);

            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                
                stmt.setObject(1, resId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        
                        // 1. Temel bilgileri kullanarak Student objesini oluştur
                        models.Student requester = new models.Student(
                            rs.getString("full_name"), 
                            rs.getString("bilkent_email"), 
                            rs.getString("uni_id")
                        );
                        
                        // 2. Öğrencinin spor/oyun istatistiklerini set et
                        requester.setEloPoint(rs.getInt("elo_point"));
                        requester.setPenaltyPoints(rs.getInt("penalty_points"));
                        requester.setReliabilityScore(rs.getDouble("reliability_score"));
                        requester.setMatchesPlayed(rs.getInt("matches_played"));
                        requester.setWinRate(rs.getDouble("win_rate"));
                        
                        // Kazanılan maç sayısını (win_rate * matches_played) formülüyle hesapla
                        int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                        requester.setMatchesWon(matchesWon);

                        // 3. Listeye ekle
                        pendingStudents.add(requester);
                    }
                }
            }

        } catch (IllegalArgumentException e) {
            // Geçersiz formattaki UUID'leri yakalar (Sistemin çökmesini engeller)
            System.err.println("Geçersiz Reservation ID formatı: " + reservationId);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return pendingStudents;
    }
    
    /**
     * Fetches admin records from the 'users' and 'admins' tables
     * and updates the provided Admin object with this data, including the profile picture.
     * @param admin The existing Admin object to be updated
     * @param email Admin's Bilkent email address to query the database
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus fillAdminDataByEmail(models.Admin admin, String email) {
        
        // Null kontrolü, gelen objenin boş olmasını engeller
        if (admin == null) {
            return DbStatus.QUERY_ERROR;
        }

        // users ve admins tablolarını birleştiren sorgu (u.profile_pic_url eklendi)
        String sql = "SELECT u.full_name, u.bilkent_email, u.password_hash, u.profile_pic_url, " +
                     "a.actions_performed " +
                     "FROM users u " +
                     "INNER JOIN admins a ON u.id = a.admin_id " +
                     "WHERE u.bilkent_email = ? AND u.role = 'admin'";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    
                    // 1. Üst sınıf (User) verilerini güncelle
                    admin.setFullName(rs.getString("full_name"));
                    admin.setBilkentEmail(rs.getString("bilkent_email"));
                    admin.setPassword(rs.getString("password_hash"));
                    
                    // Miras alınan User modeline profil fotoğrafını set ediyoruz
                    admin.setProfilePictureUrl(rs.getString("profile_pic_url"));
                    
                    // 2. Admin'e özel verileri set et
                    // Güncellenen modele uygun olarak setActionsPerformed kullanıldı
                    admin.setActionsPerformed(rs.getInt("actions_performed"));

                    return DbStatus.SUCCESS;
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
     * Inserts a new facility into the database.
     * @param facilityName The name of the facility (e.g., "Main Sports Hall")
     * @param location The campus location (e.g., "Main Campus" or "East Campus")
     * @param capacity The maximum capacity of the facility
     * @param sportName The name of the sport associated with this facility (e.g., "BASKETBALL")
     * @param isUnderMaintenance Whether the facility is initially under maintenance
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if sport not found), or errors.
     */
    public DbStatus insertFacility(String facilityName, String location, int capacity, String sportName, boolean isUnderMaintenance) {
        
        String insertSql = "INSERT INTO facilities (name, campus_location, capacity, is_under_maintenance, sport_id) " +
                           "SELECT ?, ?, ?, ?, id FROM sports WHERE UPPER(REPLACE(name, ' ', '_')) = ?";

        try (PreparedStatement insertStmt = getConnection().prepareStatement(insertSql)) {
            
            String formattedSportName = sportName.trim().toUpperCase().replace(" ", "_");

            insertStmt.setString(1, facilityName);
            insertStmt.setString(2, location);
            insertStmt.setInt(3, capacity);
            insertStmt.setBoolean(4, isUnderMaintenance);
            insertStmt.setString(5, formattedSportName);
            
            int insertedRows = insertStmt.executeUpdate();
            
            if (insertedRows == 0) {
                return DbStatus.DATA_NOT_FOUND; 
            }

            return DbStatus.SUCCESS;

        } catch (SQLException e) {
            e.printStackTrace();
            
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Deletes a facility from the database using its name.
     * Note: If multiple facilities have the exact same name, all of them will be deleted.
     * @param facilityName The exact name of the facility to be deleted
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND (if no facility matched), or errors.
     */
    public DbStatus deleteFacility(String facilityName) {
        
        if (facilityName == null || facilityName.trim().isEmpty()) {
            return DbStatus.QUERY_ERROR;
        }

        String sql = "DELETE FROM facilities WHERE name = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, facilityName);
            
            int deletedRows = stmt.executeUpdate();
            
            // Eğer silinen satır sayısı 0 ise, bu isme ait bir tesis yok demektir
            if (deletedRows == 0) {
                return DbStatus.DATA_NOT_FOUND; 
            }

            return DbStatus.SUCCESS;

        } catch (SQLException e) {
            e.printStackTrace();
            
            // Veritabanı bağlantı hatası
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            // Yabancı Anahtar (Foreign Key) İhlali - Hata Kodu: 23503
            // Eğer bu tesise ait geçmiş veya gelecek rezervasyonlar varsa, veritabanı silmeye izin vermez.
            if ("23503".equals(e.getSQLState())) {
                System.err.println("Uyarı: Bu tesise bağlı rezervasyonlar olduğu için tesis silinemiyor!");
                return DbStatus.QUERY_ERROR; 
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Increments the 'actions_performed' count for the specified admin in the database.
     * Uses RETURNING to instantly fetch the updated count and sync it with the Admin object.
     * @param currentAdmin The Admin object performing the action
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus addActionPerformed(models.Admin currentAdmin) {
        
        // Objenin veya email'in null olma ihtimaline karşı güvenlik kontrolü
        if (currentAdmin == null || currentAdmin.getBilkentEmail() == null) {
            return DbStatus.QUERY_ERROR;
        }

        // UPDATE işlemini yapar ve yeni değeri RETURNING ile ResultSet olarak döndürür
        String sql = "UPDATE admins SET actions_performed = actions_performed + 1 " +
                     "WHERE admin_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                     "RETURNING actions_performed";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, currentAdmin.getBilkentEmail());

            // RETURNING kullandığımız için executeUpdate() yerine executeQuery() kullanıyoruz
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    
                    // Veritabanındaki güncel değeri alıp anında Java objesine set ediyoruz
                    int updatedCount = rs.getInt("actions_performed");
                    currentAdmin.setActionsPerformed(updatedCount);
                    
                    return DbStatus.SUCCESS;
                } else {
                    // Eğer rs.next() false ise, bu e-postaya ait bir admin bulunamadı demektir
                    return DbStatus.DATA_NOT_FOUND;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            
            // Veritabanı bağlantı hatası kontrolü
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Retrieves the total count of student and admin users.
     * @return A List where index 0 is the number of students, and index 1 is the number of admins.
     */
    public java.util.List<Integer> getUsersCount() {
        
        java.util.List<Integer> counts = new java.util.ArrayList<>();
        counts.add(0); // 0. index: student sayısı
        counts.add(0); // 1. index: admin sayısı

        String sql = "SELECT " +
                     "COUNT(*) FILTER (WHERE role = 'student') AS student_count, " +
                     "COUNT(*) FILTER (WHERE role = 'admin') AS admin_count " +
                     "FROM users";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                counts.set(0, rs.getInt("student_count"));
                counts.set(1, rs.getInt("admin_count"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return counts;
    }

    /**
     * Uploads a profile picture to Supabase Storage and updates the user's profile picture URL in the database.
     * Validates the file format (PNG, JPG, JPEG) and enforces a maximum file size limit of 5 MB.
     * Automatically deletes the old profile picture from Storage to save space.
     * @param email The Bilkent email of the user updating their profile picture
     * @param pictureFile The image file to be uploaded to the 'avatars' bucket
     * @return DbStatus indicating SUCCESS, FILE_TOO_LARGE, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateProfilePicture(String email, File pictureFile) {
        
        // 1. Dosya Geçerlilik Kontrolü
        if (pictureFile == null || !pictureFile.exists()) {
            return DbStatus.QUERY_ERROR;
        }

        // --- BOYUT KONTROLÜ (Maksimum 5 MB) ---
        long maxFileSize = 5 * 1024 * 1024; // 5 MB'ın byte karşılığı
        if (pictureFile.length() > maxFileSize) {
            System.err.println("Uyarı: Yüklenen dosya çok büyük! Maksimum 5 MB destekleniyor.");
            return DbStatus.FILE_TOO_LARGE; 
        }

        String fileName = pictureFile.getName().toLowerCase();
        if (!fileName.endsWith(".png") && !fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")) {
            return DbStatus.QUERY_ERROR; 
        }

        try {
            // YENİ ADIM: İşleme başlamadan önce kullanıcının mevcut fotoğraf linkini veritabanından çek (Yedekte tut)
            String oldProfilePicUrl = getCurrentProfilePicUrl(email);

            // 2. Supabase Storage İçin Benzersiz Dosya Adı Oluşturma
            String extension = fileName.substring(fileName.lastIndexOf("."));
            String storagePath = "profile_" + email.replace("@", "_").replace(".", "_") + "_" + System.currentTimeMillis() + extension;

            // 3. Dosyayı Supabase Storage'a Yükleme
            String uploadUrl = SUPABASE_URL + "/storage/v1/object/avatars/" + storagePath;
            
            java.net.URL url = java.net.URI.create(uploadUrl).toURL();
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST"); 
            connection.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            connection.setRequestProperty("apikey", SUPABASE_KEY); 
            
            String contentType = Files.probeContentType(pictureFile.toPath());
            if (contentType == null) {
                contentType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
            }
            connection.setRequestProperty("Content-Type", contentType);
            connection.setDoOutput(true);

            try (java.io.InputStream fileInput = new FileInputStream(pictureFile);
                 java.io.OutputStream requestBody = connection.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    requestBody.write(buffer, 0, bytesRead);
                }
            }

            int statusCode = connection.getResponseCode();
            if (statusCode == 200 || statusCode == 201) {
                
                // 4. Başarılı Yükleme Sonrası Public URL Oluşturma
                String publicUrl = SUPABASE_URL + "/storage/v1/object/public/avatars/" + storagePath;

                // 5. Veritabanındaki 'users' tablosunu yeni URL ile güncelle
                DbStatus updateStatus = updateProfilePicUrlInDatabase(email, publicUrl);
                
                // YENİ ADIM: Eğer veritabanı başarıyla güncellendiyse ve eski bir fotoğraf varsa, eskiyi Storage'dan SİL!
                if (updateStatus == DbStatus.SUCCESS && oldProfilePicUrl != null && !oldProfilePicUrl.trim().isEmpty()) {
                    deleteFileFromStorage(oldProfilePicUrl);
                }
                
                return updateStatus;
                
            } else {
                java.io.InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    try (java.util.Scanner scanner = new java.util.Scanner(errorStream)) {
                        scanner.useDelimiter("\\A");
                        String errorBody = scanner.hasNext() ? scanner.next() : "Detay yok";
                        System.err.println("Storage Hatası Detayı: " + errorBody);
                    }
                }
                System.err.println("Storage Hatası: HTTP " + statusCode);
                return DbStatus.QUERY_ERROR;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return DbStatus.CONNECTION_ERROR;
        }
    }

    /**
     * Yardımcı Metot 1: Veritabanındaki URL kolonunu günceller.
     */
    private DbStatus updateProfilePicUrlInDatabase(String email, String publicUrl) {
        String sql = "UPDATE users SET profile_pic_url = ? WHERE bilkent_email = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, publicUrl);
            stmt.setString(2, email);
            
            int updatedRows = stmt.executeUpdate();
            return (updatedRows > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Yardımcı Metot 2: Kullanıcının veritabanındaki mevcut profil fotoğrafı URL'sini çeker.
     */
    private String getCurrentProfilePicUrl(String email) {
        String sql = "SELECT profile_pic_url FROM users WHERE bilkent_email = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("profile_pic_url");
                }
            }
        } catch (SQLException e) {
            System.err.println("Mevcut fotoğraf URL'si çekilirken hata oluştu: " + e.getMessage());
        }
        return null;
    }

    /**
     * Yardımcı Metot 3: Supabase Storage API'sini kullanarak verilen URL'ye ait dosyayı fiziksel olarak siler.
     */
    private void deleteFileFromStorage(String oldUrl) {
        // Geçersiz bir URL geldiyse (veya bizim bucket'tan değilse) işlemi iptal et
        if (oldUrl == null || !oldUrl.contains("/avatars/")) return;

        try {
            // URL'nin sonundaki dosya adını çekiyoruz (Örn: "profile_berkin_..._162345.png")
            String fileName = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
            
            // Supabase Delete API Endpoint'i
            String deleteApiUrl = SUPABASE_URL + "/storage/v1/object/avatars/" + fileName;
            
            java.net.URL url = java.net.URI.create(deleteApiUrl).toURL();
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            connection.setRequestProperty("apikey", SUPABASE_KEY);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Uyarı: Eski profil fotoğrafı Storage'dan silinemedi. HTTP Kodu: " + responseCode);
            } else {
                System.out.println("Eski profil fotoğrafı Storage'dan başarıyla temizlendi.");
            }
            
        } catch (Exception e) {
            System.err.println("Eski fotoğraf silinirken bir istisna oluştu: " + e.getMessage());
        }
    }

    /**
     * Creates a new team for a given tournament with the specified team name and creator student.
     * Validates that the creator student is not already an 'ACCEPTED' member of another team in the same tournament.
     * If validation passes, it creates the team and adds the creator as the captain with 'ACCEPTED' status in a single transaction.
     * @param creatorStudent The student who will be the captain of the new team
     * @param currentTournament The tournament for which the team is being created
     * @param teamName The desired name of the new team
     * @return DbStatus indicating SUCCESS, ALREADY_IN_TOURNAMENT, or errors.
     */
    public DbStatus insertTeam(models.Student creatorStudent, models.Tournament currentTournament, String teamName) {
        
        if (creatorStudent == null || currentTournament == null || teamName == null || teamName.trim().isEmpty()) {
            return DbStatus.QUERY_ERROR;
        }

        // 1. AŞAMA: Kullanıcının bu turnuvada zaten 'ACCEPTED' olduğu bir takım var mı kontrolü
        String checkSql = "SELECT 1 FROM team_members tm " +
                          "INNER JOIN teams t ON tm.team_id = t.team_id " +
                          "WHERE tm.student_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                          "AND tm.status = 'ACCEPTED' " +
                          "AND t.tournament_id = ?";

        String teamId = java.util.UUID.randomUUID().toString();
        String accessCode = generateRandomCode(6); 
        
        String insertTeamSql = "INSERT INTO teams (team_id, tournament_id, team_name, access_code, max_capacity, ge250_requested, captain_id) " +
                               "VALUES (?, ?, ?, ?, ?, ?, (SELECT id FROM users WHERE bilkent_email = ?))";
        
        // 2. AŞAMA: Kaptanı 'ACCEPTED' olarak ekleyecek şekilde güncellenen SQL
        String insertMemberSql = "INSERT INTO team_members (team_id, student_id, status) " +
                                 "VALUES (?, (SELECT id FROM users WHERE bilkent_email = ?), 'ACCEPTED')";

        Connection conn = null;
        try {
            conn = getConnection();
            
            // --- KONTROL İŞLEMİ ---
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, creatorStudent.getBilkentEmail());
                checkStmt.setObject(2, java.util.UUID.fromString(currentTournament.getTournamentId()));
                
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.err.println("Hata: Öğrenci zaten bu turnuvadaki bir takımda (ACCEPTED) bulunuyor, yeni takım kuramaz!");
                        // Arayüzde özel uyarı vermek için DbStatus.ALREADY_IN_TOURNAMENT gibi bir şey dönebilirsin
                        return DbStatus.QUERY_ERROR; 
                    }
                }
            }
            
            // Kontrolü geçtiysek Transaction'ı başlatıyoruz
            conn.setAutoCommit(false); 
            
            // 1. Takımı oluştur
            try (PreparedStatement stmt1 = conn.prepareStatement(insertTeamSql)) {
                stmt1.setObject(1, java.util.UUID.fromString(teamId));
                stmt1.setObject(2, java.util.UUID.fromString(currentTournament.getTournamentId()));
                stmt1.setString(3, teamName);
                stmt1.setString(4, accessCode);
                stmt1.setInt(5, currentTournament.getMaxPlayersPerTeam());
                stmt1.setBoolean(6, currentTournament.isHasGe250()); 
                stmt1.setString(7, creatorStudent.getBilkentEmail());
                stmt1.executeUpdate();
            }
            
            // 2. Kaptanı üye olarak (ACCEPTED) ekle
            try (PreparedStatement stmt2 = conn.prepareStatement(insertMemberSql)) {
                stmt2.setObject(1, java.util.UUID.fromString(teamId));
                stmt2.setString(2, creatorStudent.getBilkentEmail());
                stmt2.executeUpdate();
            }
            
            conn.commit();
            return DbStatus.SUCCESS;

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    
    /**
     * Allows a student to join a team using the team's access code.
     * Validates the access code, checks for team capacity, and ensures the student 
     * is not already in another team in the same tournament before adding them.
     * @param currentStudent The student who is trying to join the team
     * @param inputCode The access code provided by the student to join the team
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or error types.
     */
    public DbStatus beTeamMember(models.Student currentStudent, String inputCode) {
        
        if (currentStudent == null || inputCode == null || inputCode.trim().isEmpty()) {
            return DbStatus.QUERY_ERROR;
        }

        // 1. ADIM: Girilen koda ait takımı, kapasitesini ve mevcut üye sayısını bul
        String findTeamSql = "SELECT t.team_id, t.tournament_id, t.max_capacity, " +
                             "(SELECT COUNT(*) FROM team_members tm WHERE tm.team_id = t.team_id AND tm.status = 'ACCEPTED') AS current_count " +
                             "FROM teams t WHERE t.access_code = ?";

        // 2. ADIM: Öğrencinin aynı turnuvada başka bir takımda olup olmadığını kontrol et
        String checkTournamentSql = "SELECT 1 FROM team_members tm " +
                                    "INNER JOIN teams t ON tm.team_id = t.team_id " +
                                    "WHERE tm.student_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                                    "AND tm.status = 'ACCEPTED' AND t.tournament_id = ?";

        // 3. ADIM: Öğrenciyi takıma doğrudan 'ACCEPTED' olarak ekle
        String insertMemberSql = "INSERT INTO team_members (team_id, student_id, status) " +
                                 "VALUES (?, (SELECT id FROM users WHERE bilkent_email = ?), 'ACCEPTED')";

        try {
            java.util.UUID teamId = null;
            java.util.UUID tournamentId = null;

            // --- KONTROL 1: Takımı Bul ve Kapasiteyi Denetle ---
            try (java.sql.PreparedStatement findStmt = getConnection().prepareStatement(findTeamSql)) {
                findStmt.setString(1, inputCode.trim().toUpperCase()); // Kodları her ihtimale karşı büyük harf ve boşluksuz arıyoruz
                
                try (java.sql.ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        int maxCap = rs.getInt("max_capacity");
                        int currentCount = rs.getInt("current_count");
                        
                        if (currentCount >= maxCap) {
                            System.err.println("Hata: Katılmaya çalıştığınız takımın kapasitesi dolu!");
                            return DbStatus.QUERY_ERROR; // Kapasite hatası
                        }
                        
                        teamId = (java.util.UUID) rs.getObject("team_id");
                        tournamentId = (java.util.UUID) rs.getObject("tournament_id");
                    } else {
                        System.err.println("Hata: Geçersiz veya bulunamayan katılım kodu!");
                        return DbStatus.DATA_NOT_FOUND; // Takım bulunamadı
                    }
                }
            }

            // --- KONTROL 2: Aynı Turnuvada Başka Takımda Mı? ---
            try (java.sql.PreparedStatement checkStmt = getConnection().prepareStatement(checkTournamentSql)) {
                checkStmt.setString(1, currentStudent.getBilkentEmail());
                checkStmt.setObject(2, tournamentId);
                
                try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.err.println("Hata: Öğrenci bu turnuvada zaten başka bir takıma kayıtlı!");
                        return DbStatus.QUERY_ERROR; // Zaten turnuvada var
                    }
                }
            }

            // --- İŞLEM: Takıma Ekle ---
            try (java.sql.PreparedStatement insertStmt = getConnection().prepareStatement(insertMemberSql)) {
                insertStmt.setObject(1, teamId);
                insertStmt.setString(2, currentStudent.getBilkentEmail());
                
                int insertedRows = insertStmt.executeUpdate();
                return (insertedRows > 0) ? DbStatus.SUCCESS : DbStatus.QUERY_ERROR;
            }

        } catch (java.sql.SQLException e) {
            // "23505" hatası PostgreSQL'de "Unique Violation" yani mükerrer kayıt demektir.
            // Bu durumda öğrenci zaten o takımdadır (veya PENDING olarak daha önceden istek atılmıştır).
            if ("23505".equals(e.getSQLState())) {
                System.err.println("Uyarı: Öğrenci zaten bu takımda bulunuyor veya davetiye bekliyor!");
            } else {
                e.printStackTrace();
            }
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) return DbStatus.CONNECTION_ERROR;
            return DbStatus.QUERY_ERROR;
        }
    }
    
    /**
     * Removes a student from a team based on the team ID and student's email.
     * @param teamId The UUID of the team from which the student wants to be removed
     * @param studentEmail The Bilkent email of the student to be removed from the team
     * @return DbStatus indicating SUCCESS or error types.
     */
    public DbStatus deleteTeamMember(String teamId, String studentEmail) {
        String sql = "DELETE FROM team_members WHERE team_id = ? " +
                     "AND student_id = (SELECT id FROM users WHERE bilkent_email = ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setObject(1, java.util.UUID.fromString(teamId));
            stmt.setString(2, studentEmail);
            
            return (stmt.executeUpdate() > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;
        } catch (SQLException e) {
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Deletes a team from the database. 
     * Since 'ON DELETE CASCADE' is enabled, all members in 'team_members' 
     * will be automatically removed by the database.
     * @param teamId The UUID of the team to be deleted
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus deleteTeam(String teamId) {
        if (teamId == null) return DbStatus.QUERY_ERROR;

        String sql = "DELETE FROM teams WHERE team_id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setObject(1, java.util.UUID.fromString(teamId));
            
            int deletedRows = stmt.executeUpdate();
            return (deletedRows > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (IllegalArgumentException | SQLException e) {
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        }
    }


    /**
     * Inserts a new tournament into the database.
     * Automatically generates a UUID and a 6-character access code.
     * * @param tournamentName Name of the tournament
     * @param sportName Name of the sport (e.g., "BASKETBALL")
     * @param startDate Tournament start date
     * @param endDate Tournament end date
     * @param maxPlayersPerTeam Maximum capacity for each team
     * @param hasGe250 Whether the tournament gives GE250 points
     * @param campusLocation The campus location (e.g., "Main Campus" or "East Campus")
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus insertTournament(String tournamentName, String sportName, LocalDate startDate, LocalDate endDate, int maxPlayersPerTeam, boolean hasGe250, String campusLocation) {
        
        // campusLocation için null ve boşluk kontrolü eklendi
        if (tournamentName == null || sportName == null || startDate == null || endDate == null || campusLocation == null || campusLocation.trim().isEmpty()) {
            return DbStatus.QUERY_ERROR;
        }

        String tournamentId = java.util.UUID.randomUUID().toString();
        String accessCode = generateRandomCode(6); // Daha önce yazdığın metodu kullanır
        String formattedSportName = sportName.trim().toUpperCase().replace(" ", "_");

        // sport_id değerini sports tablosundan çekerek ekliyoruz
        String sql = "INSERT INTO tournaments (tournament_id, tournament_name, sport_id, start_date, end_date, " +
                     "max_players_per_team, has_ge250, access_code, is_active, campus_location) " +
                     "SELECT ?, ?, id, ?, ?, ?, ?, ?, ?, ? FROM sports WHERE UPPER(REPLACE(name, ' ', '_')) = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setObject(1, java.util.UUID.fromString(tournamentId));
            stmt.setString(2, tournamentName);
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(4, java.sql.Timestamp.valueOf(endDate.atStartOfDay()));
            stmt.setInt(5, maxPlayersPerTeam);
            
            // Parametreden gelen değer kullanılıyor
            stmt.setBoolean(6, hasGe250); 
            
            stmt.setString(7, accessCode);
            stmt.setBoolean(8, true); // Yeni turnuva varsayılan olarak aktiftir
            
            // Parametreden gelen değer kullanılıyor
            stmt.setString(9, campusLocation); 
            
            stmt.setString(10, formattedSportName);
            
            int insertedRows = stmt.executeUpdate();
            return (insertedRows > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND; // Spor bulunamazsa

        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) return DbStatus.CONNECTION_ERROR;
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the name and maximum players per team for a given tournament.
     * @param tournamentId The UUID of the tournament to be updated
     * @param newName The new name for the tournament
     * @param newMaxPlayers The new maximum number of players allowed per team in the tournament
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateTournamentDetails(String tournamentId, String newName, int newMaxPlayers) {
        
        if (tournamentId == null || newName == null || newName.trim().isEmpty()) return DbStatus.QUERY_ERROR;

        String sql = "UPDATE tournaments SET tournament_name = ?, max_players_per_team = ? WHERE tournament_id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, newName);
            stmt.setInt(2, newMaxPlayers);
            stmt.setObject(3, java.util.UUID.fromString(tournamentId));
            
            return (stmt.executeUpdate() > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (IllegalArgumentException | SQLException e) {
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Activates or deactivates a tournament by updating its 'is_active' status in the database.
     * @param tournamentId The UUID of the tournament to be updated
     * @param isActive The new active status for the tournament (true for active, false for inactive)
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateTournamentStatus(String tournamentId, boolean isActive) {
        
        if (tournamentId == null) return DbStatus.QUERY_ERROR;

        String sql = "UPDATE tournaments SET is_active = ? WHERE tournament_id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setBoolean(1, isActive);
            stmt.setObject(2, java.util.UUID.fromString(tournamentId));
            
            return (stmt.executeUpdate() > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (IllegalArgumentException | SQLException e) {
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the start and end dates of a tournament in the database.
     * @param tournamentId The UUID of the tournament to be updated
     * @param newStart The new start date for the tournament
     * @param newEnd The new end date for the tournament
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateTournamentDates(String tournamentId, LocalDate newStart, LocalDate newEnd) {
        
        if (tournamentId == null || newStart == null || newEnd == null) return DbStatus.QUERY_ERROR;

        String sql = "UPDATE tournaments SET start_date = ?, end_date = ? WHERE tournament_id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(newStart.atStartOfDay()));
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(newEnd.atStartOfDay()));
            stmt.setObject(3, java.util.UUID.fromString(tournamentId));
            
            return (stmt.executeUpdate() > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (IllegalArgumentException | SQLException e) {
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Retrieves a list of all active tournaments from the database, including their associated sport names.
     * Joins the 'tournaments' table with the 'sports' table to fetch the sport name for each tournament.
     * Converts database records into Tournament model objects and returns them in a list.
     * @return A List of Tournament objects representing all active tournaments.
     */
    public java.util.List<models.Tournament> getAllActiveTournaments() {
        
        java.util.List<models.Tournament> activeTournaments = new java.util.ArrayList<>();
        
        String sql = "SELECT t.tournament_id, t.tournament_name, s.name AS sport_name, " +
                     "t.start_date, t.end_date, t.max_players_per_team, t.has_ge250, " +
                     "t.access_code, t.campus_location " +
                     "FROM tournaments t " +
                     "INNER JOIN sports s ON t.sport_id = s.id " +
                     "WHERE t.is_active = true";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String tId = rs.getObject("tournament_id").toString();
                String tName = rs.getString("tournament_name");
                
                // String spor adını Enum'a çeviriyoruz
                String sportNameStr = rs.getString("sport_name");
                models.SportType sType = null;
                try {
                    sType = models.SportType.valueOf(sportNameStr.trim().toUpperCase().replace(" ", "_"));
                } catch (IllegalArgumentException e) {
                    System.err.println("Geçersiz spor türü: " + sportNameStr);
                    continue; // Hatalı spor varsa bu turnuvayı atla
                }

                // timestamptz verisini LocalDate'e dönüştürüyoruz
                LocalDate sDate = rs.getTimestamp("start_date").toLocalDateTime().toLocalDate();
                LocalDate eDate = rs.getTimestamp("end_date").toLocalDateTime().toLocalDate();
                
                int maxPlayers = rs.getInt("max_players_per_team");
                boolean ge250 = rs.getBoolean("has_ge250");
                String code = rs.getString("access_code");
                String location = rs.getString("campus_location");

                // Constructor üzerinden Tournament objesini oluşturup listeye ekliyoruz
                models.Tournament tournament = new models.Tournament(
                    tId, tName, sType, sDate, eDate, maxPlayers, ge250, code, location
                );
                
                activeTournaments.add(tournament);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return activeTournaments;
    }

    /**
     * Sends a team invite to a student by inserting a pending invitation record into the 'team_members' table.
     * The invitation is represented as a record with 'PENDING' status, which can later be accepted or rejected by the student.
     * @param teamId The UUID of the team sending the invite
     * @param receiverEmail The Bilkent email of the student receiving the invite
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus sendTeamInvite(String teamId, String receiverEmail) {
        if (teamId == null || receiverEmail == null) return DbStatus.QUERY_ERROR;

        // 1. AŞAMA: Kullanıcının aynı turnuvada zaten bir takımda olup olmadığını kontrol eden SQL
        String checkSql = "SELECT 1 FROM team_members tm " +
                          "INNER JOIN teams t ON tm.team_id = t.team_id " +
                          "WHERE tm.student_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                          "AND tm.status = 'ACCEPTED' " +
                          "AND t.tournament_id = (SELECT tournament_id FROM teams WHERE team_id = ?)";

        // 2. AŞAMA: İsteği PENDING olarak ekleyen SQL
        String insertSql = "INSERT INTO team_members (team_id, student_id, status) " +
                           "VALUES (?, (SELECT id FROM users WHERE bilkent_email = ?), 'PENDING')";

        try {
            java.util.UUID tId = java.util.UUID.fromString(teamId);

            // Önce turnuva kontrolünü yapıyoruz
            try (PreparedStatement checkStmt = getConnection().prepareStatement(checkSql)) {
                checkStmt.setString(1, receiverEmail);
                checkStmt.setObject(2, tId);
                
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.err.println("Reddedildi: Kullanıcı zaten bu turnuvadaki bir takımda yer alıyor.");
                        // Arayüzde özel bir uyarı göstermek istersen DbStatus enum'ına 
                        // ALREADY_IN_TOURNAMENT gibi yeni bir flag ekleyip onu döndürebilirsin.
                        return DbStatus.QUERY_ERROR; 
                    }
                }
            }

            // Kontrol temiz çıkarsa (kullanıcı turnuvada değilse) daveti atıyoruz
            try (PreparedStatement insertStmt = getConnection().prepareStatement(insertSql)) {
                insertStmt.setObject(1, tId);
                insertStmt.setString(2, receiverEmail);
                
                return (insertStmt.executeUpdate() > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;
            }

        } catch (SQLException e) {
            // Composite Primary Key (team_id, student_id) ihlali (Zaten bu takımda veya davet bekliyor)
            if ("23505".equals(e.getSQLState())) {
                System.err.println("Uyarı: Bu kullanıcıya bu takım için zaten davet atılmış veya kullanıcı zaten takımda!");
                return DbStatus.ALREADY_IN_TOURNAMENT;
            } else {
                e.printStackTrace();
            }
            return DbStatus.QUERY_ERROR;
        }
    }
    /**
     * Retrieves a list of tournaments that the given student is currently a member of.
     * Joins multiple tables to fetch tournaments where the student has an 'ACCEPTED' membership status.
     * Converts database records into Tournament model objects and returns them in a list.
     * @param currentStudent The student for whom to fetch the tournaments
     * @return A List of Tournament objects representing the tournaments the student is part of.
     */
    public java.util.ArrayList<models.Tournament> getUserTournaments(Student currentStudent) {
        java.util.ArrayList<models.Tournament> tournaments = new java.util.ArrayList<>();
        if (currentStudent == null) return tournaments;

        String sql = "SELECT t.tournament_id, t.tournament_name, s.name AS sport_name, " +
                     "t.start_date, t.end_date, t.max_players_per_team, t.has_ge250, " +
                     "t.access_code, t.campus_location " +
                     "FROM tournaments t " +
                     "INNER JOIN sports s ON t.sport_id = s.id " +
                     "INNER JOIN teams tm ON t.tournament_id = tm.tournament_id " +
                     "INNER JOIN team_members tmem ON tm.team_id = tmem.team_id " +
                     "INNER JOIN users u ON tmem.student_id = u.id " +
                     "WHERE u.bilkent_email = ? AND tmem.status = 'ACCEPTED' AND t.is_active = true";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, currentStudent.getBilkentEmail());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String sportNameStr = rs.getString("sport_name");
                    models.SportType sType = null;
                    try {
                        sType = models.SportType.valueOf(sportNameStr.trim().toUpperCase().replace(" ", "_"));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    tournaments.add(new models.Tournament(
                        rs.getObject("tournament_id").toString(),
                        rs.getString("tournament_name"),
                        sType,
                        rs.getTimestamp("start_date").toLocalDateTime().toLocalDate(),
                        rs.getTimestamp("end_date").toLocalDateTime().toLocalDate(),
                        rs.getInt("max_players_per_team"),
                        rs.getBoolean("has_ge250"),
                        rs.getString("access_code"),
                        rs.getString("campus_location")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tournaments;
    }

    /**
     * Allows a student to reject a team invite by deleting the corresponding pending record from the 'team_members' table.
     * The method checks for the existence of a pending invitation for the given team and student before attempting deletion.
     * @param teamId The UUID of the team whose invite is being rejected
     * @param receiverEmail The Bilkent email of the student rejecting the invite
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus rejectTeamInvite(String teamId, String receiverEmail) {
        if (teamId == null || receiverEmail == null) return DbStatus.QUERY_ERROR;

        String sql = "DELETE FROM team_members " +
                     "WHERE team_id = ? AND student_id = (SELECT id FROM users WHERE bilkent_email = ?) AND status = 'PENDING'";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setObject(1, java.util.UUID.fromString(teamId));
            stmt.setString(2, receiverEmail);
            
            return (stmt.executeUpdate() > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;
        } catch (SQLException e) {
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Allows a student to accept a team invite by updating the corresponding record in the 'team_members' table from 'PENDING' to 'ACCEPTED'.
     * The method checks for the existence of a pending invitation for the given team and student before attempting the update.
     * @param teamId The UUID of the team whose invite is being accepted
     * @param receiverEmail The Bilkent email of the student accepting the invite
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus acceptTeamInvite(String teamId, String receiverEmail) {
        if (teamId == null || receiverEmail == null) return DbStatus.QUERY_ERROR;

        String sql = "UPDATE team_members SET status = 'ACCEPTED' " +
                     "WHERE team_id = ? AND student_id = (SELECT id FROM users WHERE bilkent_email = ?) AND status = 'PENDING'";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setObject(1, java.util.UUID.fromString(teamId));
            stmt.setString(2, receiverEmail);
            
            return (stmt.executeUpdate() > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;
        } catch (SQLException e) {
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Retrieves a list of teams that have sent pending join requests to a specific student.
     * Joins multiple tables to fetch team details along with the captain's information for teams where the student has a 'PENDING' status.
     * @param teamId Optional UUID of a specific team to filter the incoming requests (if null, fetches all incoming requests)
     * @param currentStudent The student for whom to fetch the incoming requests (used to filter requests relevant to this student)
     * @return A List of Team objects representing the teams that have sent join requests to the specified student.
     */
    public java.util.ArrayList<models.Team> getTeamIncomingRequests(String teamId, models.Student currentStudent) {
        java.util.ArrayList<models.Team> incomingTeams = new java.util.ArrayList<>();
        if (currentStudent == null) return incomingTeams;

        // t.* -> Takım bilgileri
        // c_u.* ve c_s.* -> Kaptanın bilgileri (users ve students tablolarından)
        StringBuilder sql = new StringBuilder(
            "SELECT t.team_id, t.team_name, t.access_code, t.max_capacity, t.ge250_requested, " +
            "c_u.full_name AS cap_name, c_u.bilkent_email AS cap_email, c_u.student_id AS cap_uni_id, c_u.profile_pic_url AS cap_pic, " +
            "c_s.elo_point, c_s.win_rate " +
            "FROM teams t " +
            "INNER JOIN team_members tm ON t.team_id = tm.team_id " +
            "INNER JOIN users u ON tm.student_id = u.id " +
            "INNER JOIN users c_u ON t.captain_id = c_u.id " +
            "INNER JOIN students c_s ON c_u.id = c_s.user_id " +
            "WHERE u.bilkent_email = ? AND tm.status = 'PENDING'"
        );

        if (teamId != null && !teamId.trim().isEmpty()) {
            sql.append(" AND tm.team_id = ?");
        }

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            stmt.setString(1, currentStudent.getBilkentEmail());
            if (teamId != null && !teamId.trim().isEmpty()) {
                stmt.setObject(2, java.util.UUID.fromString(teamId));
            }

            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // 1. Kaptanı oluşturuyoruz
                    models.Student captain = new models.Student(
                        rs.getString("cap_name"), 
                        rs.getString("cap_email"), 
                        rs.getString("cap_uni_id")
                    );
                    captain.setProfilePictureUrl(rs.getString("cap_pic"));
                    captain.setEloPoint(rs.getInt("elo_point"));
                    captain.setWinRate(rs.getDouble("win_rate"));

                    // 2. Takımı oluşturuyoruz
                    models.Team team = new models.Team(
                        rs.getObject("team_id").toString(),
                        rs.getString("team_name"),
                        rs.getString("access_code"),
                        rs.getInt("max_capacity"),
                        rs.getBoolean("ge250_requested"),
                        captain
                    );

                    incomingTeams.add(team);
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return incomingTeams;
    }
    /**
     * Retrieves a list of students who have received pending join requests from a specific team.
     * Joins multiple tables to fetch students who have a 'PENDING' status for the given team ID and the current student's email.
     * @param teamId The UUID of the team for which to fetch outgoing join requests
     * @param currentStudent The student for whom to fetch the outgoing requests (used to filter requests relevant to this student)
     * @return A List of Student objects representing the students who have received join requests from the specified team.
     */
    public java.util.ArrayList<models.Student> getTeamOutgoingRequests(String teamId, Student currentStudent) {
        java.util.ArrayList<models.Student> requestReceivers = new java.util.ArrayList<>();
        if (currentStudent == null) return requestReceivers;

        StringBuilder sql = new StringBuilder(
            "SELECT u.full_name, u.bilkent_email, u.student_id AS uni_id, u.profile_pic_url, " +
            "s.elo_point, s.penalty_points, s.reliability_score, s.matches_played, " +
            "s.win_rate, s.is_public_profile, s.is_elo_matching_enabled " +
            "FROM team_members tm " +
            "INNER JOIN teams t ON tm.team_id = t.team_id " +
            "INNER JOIN users u ON tm.student_id = u.id " +
            "INNER JOIN students s ON u.id = s.user_id " +
            "WHERE t.captain_id = (SELECT id FROM users WHERE bilkent_email = ?) AND tm.status = 'PENDING'"
        );

        if (teamId != null && !teamId.trim().isEmpty()) {
            sql.append(" AND tm.team_id = ?");
        }

        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            stmt.setString(1, currentStudent.getBilkentEmail());
            if (teamId != null && !teamId.trim().isEmpty()) {
                stmt.setObject(2, java.util.UUID.fromString(teamId));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    models.Student receiver = new Student(rs.getString("full_name"), rs.getString("bilkent_email"), rs.getString("uni_id"));
                    
                    receiver.setFullName(rs.getString("full_name"));
                    receiver.setBilkentEmail(rs.getString("bilkent_email"));
                    receiver.setStudentId(rs.getString("uni_id"));
                    receiver.setProfilePictureUrl(rs.getString("profile_pic_url"));
                    receiver.setEloPoint(rs.getInt("elo_point"));
                    receiver.setPenaltyPoints(rs.getInt("penalty_points"));
                    receiver.setReliabilityScore(rs.getDouble("reliability_score"));
                    receiver.setMatchesPlayed(rs.getInt("matches_played"));
                    receiver.setWinRate(rs.getDouble("win_rate"));
                    receiver.setPublicProfile(rs.getBoolean("is_public_profile"));
                    receiver.setEloMatchingEnabled(rs.getBoolean("is_elo_matching_enabled"));
                    
                    int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                    receiver.setMatchesWon(matchesWon);

                    requestReceivers.add(receiver);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requestReceivers;
    }

    /**
     * Retrieves a list of teams that the given student is currently a member of.
     * Joins multiple tables to fetch teams where the student has an 'ACCEPTED' membership status.
     * Converts database records into Team model objects and returns them in a list.
     * @param currentStudent The student for whom to fetch the teams
     * @return A List of Team objects representing the teams the student is part of.
     */
    public java.util.ArrayList<models.Team> getMyTeams(models.Student currentStudent) {
        java.util.ArrayList<models.Team> myTeams = new java.util.ArrayList<>();
        
        if (currentStudent == null) return myTeams;

        // t.* -> Takım bilgileri
        // c_u.* ve c_s.* -> Kaptanın bilgileri (users ve students tablolarından)
        // c_u.student_id AS cap_uni_id SQL sorgusuna eklendi!
        String sql = "SELECT t.team_id, t.team_name, t.access_code, t.max_capacity, t.ge250_requested, " +
                     "c_u.full_name AS cap_name, c_u.bilkent_email AS cap_email, c_u.student_id AS cap_uni_id, c_u.profile_pic_url AS cap_pic, " +
                     "c_s.elo_point, c_s.win_rate " +
                     "FROM teams t " +
                     "INNER JOIN team_members tm ON t.team_id = tm.team_id " +
                     "INNER JOIN users u ON tm.student_id = u.id " +
                     "INNER JOIN users c_u ON t.captain_id = c_u.id " +
                     "INNER JOIN students c_s ON c_u.id = c_s.user_id " +
                     "WHERE u.bilkent_email = ? AND tm.status = 'ACCEPTED'";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, currentStudent.getBilkentEmail());

            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    
                    // 1. Önce Kaptan (Student) objesini doğru SQL alias'ları ile oluşturuyoruz
                    models.Student captain = new models.Student(
                        rs.getString("cap_name"), 
                        rs.getString("cap_email"), 
                        rs.getString("cap_uni_id")
                    );
                    
                    // Geri kalan bilgileri setter'lar ile ekliyoruz
                    captain.setProfilePictureUrl(rs.getString("cap_pic"));
                    captain.setEloPoint(rs.getInt("elo_point"));
                    captain.setWinRate(rs.getDouble("win_rate"));

                    // 2. Senin modelindeki Constructor'ı kullanarak Team objesini oluşturuyoruz
                    models.Team team = new models.Team(
                        rs.getObject("team_id").toString(),
                        rs.getString("team_name"),
                        rs.getString("access_code"),
                        rs.getInt("max_capacity"),
                        rs.getBoolean("ge250_requested"),
                        captain
                    );
                    
                    myTeams.add(team);
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        
        return myTeams;
    }
    
    /**
     * Retrieves a list of teams that are participating in a specific tournament.
     * Joins multiple tables to fetch teams associated with the given tournament ID, along with their captains' information.
     * Converts database records into Team model objects and returns them in a list.
     * @param tournament_id The UUID of the tournament for which to fetch the teams
     * @return A List of Team objects representing the teams participating in the specified tournament.
     */
    public java.util.ArrayList<models.Team> getTournamentTeams(String tournament_id) {
        java.util.ArrayList<models.Team> tournamentTeams = new java.util.ArrayList<>();
        
        if (tournament_id == null || tournament_id.trim().isEmpty()) return tournamentTeams;

        // SQL sorgusuna kaptanın okul numarası (c_u.student_id AS cap_uni_id) eklendi!
        String sql = "SELECT t.team_id, t.team_name, t.access_code, t.max_capacity, t.ge250_requested, " +
                     "c_u.full_name AS cap_name, c_u.bilkent_email AS cap_email, c_u.student_id AS cap_uni_id, c_u.profile_pic_url AS cap_pic, " +
                     "c_s.elo_point, c_s.win_rate " +
                     "FROM teams t " +
                     "INNER JOIN users c_u ON t.captain_id = c_u.id " +
                     "INNER JOIN students c_s ON c_u.id = c_s.user_id " +
                     "WHERE t.tournament_id = ?";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setObject(1, java.util.UUID.fromString(tournament_id));

            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    
                    // 1. Kaptanı zorunlu 3 parametreli Constructor ile oluştur
                    models.Student captain = new models.Student(
                        rs.getString("cap_name"), 
                        rs.getString("cap_email"), 
                        rs.getString("cap_uni_id")
                    );
                    
                    // Geri kalan değerleri Setter ile ata
                    captain.setProfilePictureUrl(rs.getString("cap_pic"));
                    captain.setEloPoint(rs.getInt("elo_point"));
                    captain.setWinRate(rs.getDouble("win_rate"));

                    // 2. Takımı oluştur
                    models.Team team = new models.Team(
                        rs.getObject("team_id").toString(),
                        rs.getString("team_name"),
                        rs.getString("access_code"),
                        rs.getInt("max_capacity"),
                        rs.getBoolean("ge250_requested"),
                        captain
                    );
                    
                    tournamentTeams.add(team);
                }
            }
        } catch (IllegalArgumentException | java.sql.SQLException e) {
            e.printStackTrace();
            System.err.println("Geçersiz turnuva ID formatı veya SQL hatası!");
        }
        
        return tournamentTeams;
    }

    /**
     * Inserts a new match record into the 'matches' table, associating it with the specified tournament and teams.
     * Automatically generates a UUID for the match and retrieves the sport_id from the associated tournament.
     * @param currentTournament The Tournament object to which the match belongs
     * @param team1 The first team participating in the match
     * @param team2 The second team participating in the match
     * @param matchDate The date and time when the match is scheduled to take place
     * @param pointChange The number of points to be awarded or deducted based on the match outcome
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus insertMatch(models.Tournament currentTournament, models.Team team1, models.Team team2, java.time.OffsetDateTime matchDate, int pointChange) {
        
        // currentTournament için null kontrolü eklendi
        if (currentTournament == null || team1 == null || team2 == null || matchDate == null) {
            return DbStatus.QUERY_ERROR;
        }

        String matchId = java.util.UUID.randomUUID().toString();
        
        // Turnuva ID'sini artık doğrudan metoda gönderdiğin turnuva objesinden alıyoruz
        String tournamentId = currentTournament.getTournamentId();

        // sport_id'yi tournaments tablosundan otomatik çeken alt sorgulu (subquery) SQL
        String sql = "INSERT INTO matches (match_id, tournament_id, sport_id, match_date, " +
                     "team1_id, team2_id, point_change, is_concluded) " +
                     "VALUES (?, ?, (SELECT sport_id FROM tournaments WHERE tournament_id = ?), ?, ?, ?, ?, false)";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setObject(1, java.util.UUID.fromString(matchId));
            stmt.setObject(2, java.util.UUID.fromString(tournamentId));
            
            // Subquery (Alt sorgu) içindeki '?' parametresi için turnuva ID'sini tekrar gönderiyoruz
            stmt.setObject(3, java.util.UUID.fromString(tournamentId)); 
            
            // timestamptz için setObject ile OffsetDateTime gönderiyoruz
            stmt.setObject(4, matchDate); 
            
            stmt.setObject(5, java.util.UUID.fromString(team1.getTeamId()));
            stmt.setObject(6, java.util.UUID.fromString(team2.getTeamId()));
            stmt.setInt(7, pointChange);
            
            return (stmt.executeUpdate() > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) return DbStatus.CONNECTION_ERROR;
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the conclusion status of a match in the database.
     * @param matchId The UUID of the match to be updated
     * @param isConcluded The new conclusion status for the match (true if the match is concluded, false otherwise)
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateMatchStatus(String matchId, boolean isConcluded) {
        
        if (matchId == null || matchId.trim().isEmpty()) return DbStatus.QUERY_ERROR;

        String sql = "UPDATE matches SET is_concluded = ? WHERE match_id = ?";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setBoolean(1, isConcluded);
            stmt.setObject(2, java.util.UUID.fromString(matchId));
            
            return (stmt.executeUpdate() > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (IllegalArgumentException | java.sql.SQLException e) {
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Updates the winner team of a match in the database.
     * If the winnerTeam parameter is null, it sets the winner_team_id to SQL NULL, indicating a draw or no winner.
     * @param matchId The UUID of the match to be updated
     * @param winnerTeam The Team object representing the winner team (can be null for draw/no winner)
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateMatchWinner(String matchId, models.Team winnerTeam) {
        
        if (matchId == null || matchId.trim().isEmpty()) return DbStatus.QUERY_ERROR;

        String sql = "UPDATE matches SET winner_team_id = ? WHERE match_id = ?";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            if (winnerTeam != null && winnerTeam.getTeamId() != null) {
                stmt.setObject(1, java.util.UUID.fromString(winnerTeam.getTeamId()));
            } else {
                // Eğer winnerTeam null gelirse, veritabanına SQL NULL kaydederiz (Beraberlik durumu vb.)
                stmt.setNull(1, java.sql.Types.OTHER); 
            }
            
            stmt.setObject(2, java.util.UUID.fromString(matchId));
            
            return (stmt.executeUpdate() > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (IllegalArgumentException | java.sql.SQLException e) {
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Retrieves a list of matches from a specific tournament that involve the given student.
     * Joins multiple tables to fetch matches where the student is a member of either team1 or team2 with 'ACCEPTED' status.
     * Converts database records into Match model objects and returns them in a list.
     * @param tournamentId The UUID of the tournament for which to fetch the matches
     * @param currentStudent The student for whom to fetch the matches (used to filter matches relevant to this student)
     * @return A List of Match objects representing the matches in the specified tournament that involve the given student.
     */
    public java.util.ArrayList<models.Match> getStudentTournamentMatches(String tournamentId, models.Student currentStudent) {
        java.util.ArrayList<models.Match> studentMatches = new java.util.ArrayList<>();
        
        if (tournamentId == null || tournamentId.trim().isEmpty() || currentStudent == null) {
            return studentMatches;
        }

        // Maç, Spor, Takımlar ve Kaptanları çeken, ancak sadece öğrencinin olduğu maçları filtreleyen sorgu
        String sql = "SELECT m.match_id, m.match_date, m.point_change, m.winner_team_id, " +
                     "sp.name AS sport_name, " +
                     
                     // 1. Takım ve Kaptan Bilgileri
                     "t1.team_id AS t1_id, t1.team_name AS t1_name, t1.access_code AS t1_code, t1.max_capacity AS t1_cap, t1.ge250_requested AS t1_ge250, " +
                     "c1_u.full_name AS c1_name, c1_u.bilkent_email AS c1_email, c1_u.profile_pic_url AS c1_pic, c1_s.elo_point AS c1_elo, c1_s.win_rate AS c1_win, " +
                     
                     // 2. Takım ve Kaptan Bilgileri
                     "t2.team_id AS t2_id, t2.team_name AS t2_name, t2.access_code AS t2_code, t2.max_capacity AS t2_cap, t2.ge250_requested AS t2_ge250, " +
                     "c2_u.full_name AS c2_name, c2_u.bilkent_email AS c2_email, c2_u.profile_pic_url AS c2_pic, c2_s.elo_point AS c2_elo, c2_s.win_rate AS c2_win " +
                     
                     "FROM matches m " +
                     "INNER JOIN sports sp ON m.sport_id = sp.id " +
                     "INNER JOIN teams t1 ON m.team1_id = t1.team_id " +
                     "INNER JOIN users c1_u ON t1.captain_id = c1_u.id " +
                     "INNER JOIN students c1_s ON c1_u.id = c1_s.user_id " +
                     "INNER JOIN teams t2 ON m.team2_id = t2.team_id " +
                     "INNER JOIN users c2_u ON t2.captain_id = c2_u.id " +
                     "INNER JOIN students c2_s ON c2_u.id = c2_s.user_id " +
                     "WHERE m.tournament_id = ? " +
                     
                     // --- FİLTRELEME KISMI: Öğrenci bu iki takımdan birinde ACCEPTED olarak var mı? ---
                     "AND EXISTS (" +
                     "    SELECT 1 FROM team_members tm " +
                     "    INNER JOIN users u ON tm.student_id = u.id " +
                     "    WHERE u.bilkent_email = ? AND tm.status = 'ACCEPTED' " +
                     "    AND (tm.team_id = m.team1_id OR tm.team_id = m.team2_id)" +
                     ")";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            // 1. Parametre: Turnuva ID'si
            stmt.setObject(1, java.util.UUID.fromString(tournamentId));
            
            // 2. Parametre: EXISTS alt sorgusundaki e-posta kontrolü
            stmt.setString(2, currentStudent.getBilkentEmail());

            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    
                    // --- 1. TAKIMIN OLUŞTURULMASI ---
                    models.Student captain1 = new Student(rs.getString("full_name"), rs.getString("bilkent_email"), rs.getString("uni_id"));
                    captain1.setFullName(rs.getString("c1_name"));
                    captain1.setBilkentEmail(rs.getString("c1_email"));
                    captain1.setProfilePictureUrl(rs.getString("c1_pic"));
                    captain1.setEloPoint(rs.getInt("c1_elo"));
                    captain1.setWinRate(rs.getDouble("c1_win"));

                    models.Team team1 = new models.Team(
                        rs.getObject("t1_id").toString(), rs.getString("t1_name"), 
                        rs.getString("t1_code"), rs.getInt("t1_cap"), 
                        rs.getBoolean("t1_ge250"), captain1
                    );

                    // --- 2. TAKIMIN OLUŞTURULMASI ---
                    models.Student captain2 = new Student(rs.getString("full_name"), rs.getString("bilkent_email"), rs.getString("uni_id"));
                    captain2.setFullName(rs.getString("c2_name"));
                    captain2.setBilkentEmail(rs.getString("c2_email"));
                    captain2.setProfilePictureUrl(rs.getString("c2_pic"));
                    captain2.setEloPoint(rs.getInt("c2_elo"));
                    captain2.setWinRate(rs.getDouble("c2_win"));

                    models.Team team2 = new models.Team(
                        rs.getObject("t2_id").toString(), rs.getString("t2_name"), 
                        rs.getString("t2_code"), rs.getInt("t2_cap"), 
                        rs.getBoolean("t2_ge250"), captain2
                    );

                    // --- SPOR TÜRÜ VE TARİH DÖNÜŞÜMÜ ---
                    models.SportType sType = null;
                    try {
                        sType = models.SportType.valueOf(rs.getString("sport_name").trim().toUpperCase().replace(" ", "_"));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Uyarı: Veritabanında eşleşmeyen spor türü: " + rs.getString("sport_name"));
                    }
                    
                    java.time.LocalDateTime matchDate = rs.getTimestamp("match_date").toLocalDateTime();

                    // --- MAÇ OBJESİNİN OLUŞTURULMASI ---
                    models.Match match = new models.Match(
                        rs.getObject("match_id").toString(),
                        matchDate,
                        sType,
                        team1,
                        team2
                    );
                    
                    match.setPointChange(rs.getInt("point_change"));

                    // --- KAZANAN TAKIMIN BELİRLENMESİ ---
                    String winnerId = rs.getString("winner_team_id");
                    if (winnerId != null) {
                        if (winnerId.equals(team1.getTeamId())) {
                            match.setWinner(team1);
                        } else if (winnerId.equals(team2.getTeamId())) {
                            match.setWinner(team2);
                        }
                    } 

                    studentMatches.add(match);
                }
            }
        } catch (IllegalArgumentException | java.sql.SQLException e) {
            e.printStackTrace();
        }

        return studentMatches;
    }

    /**
     * Retrieves a list of students who are members of a specific team.
     * Joins multiple tables to fetch students with 'ACCEPTED' status for the given team ID.
     * Converts database records into Student model objects and returns them in a list.
     * @param teamId The UUID of the team for which to fetch the members
     * @return A List of Student objects representing the members of the specified team.
     */
    public java.util.ArrayList<models.Student> getTeamMembers(String teamId) {
        java.util.ArrayList<models.Student> membersList = new java.util.ArrayList<>();
        
        if (teamId == null || teamId.trim().isEmpty()) {
            return membersList;
        }

        String sql = "SELECT u.full_name, u.bilkent_email, u.student_id AS uni_id, u.profile_pic_url, " +
                     "s.elo_point, s.penalty_points, s.reliability_score, s.matches_played, " +
                     "s.win_rate, s.is_public_profile, s.is_elo_matching_enabled " +
                     "FROM team_members tm " +
                     "INNER JOIN users u ON tm.student_id = u.id " +
                     "INNER JOIN students s ON u.id = s.user_id " +
                     "WHERE tm.team_id = ? AND tm.status = 'ACCEPTED'";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setObject(1, java.util.UUID.fromString(teamId));

            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    
                    // 1. Zorunlu alanlarla Student objesini oluşturuyoruz
                    models.Student member = new models.Student(
                        rs.getString("full_name"), 
                        rs.getString("bilkent_email"), 
                        rs.getString("uni_id")
                    );
                    
                    // 2. Geri kalan özellikleri Setter'lar ile ekliyoruz
                    member.setProfilePictureUrl(rs.getString("profile_pic_url"));
                    member.setEloPoint(rs.getInt("elo_point"));
                    member.setPenaltyPoints(rs.getInt("penalty_points"));
                    member.setReliabilityScore(rs.getDouble("reliability_score"));
                    member.setMatchesPlayed(rs.getInt("matches_played"));
                    member.setWinRate(rs.getDouble("win_rate"));
                    member.setPublicProfile(rs.getBoolean("is_public_profile"));
                    member.setEloMatchingEnabled(rs.getBoolean("is_elo_matching_enabled"));
                    
                    // 3. Kazanılan maç sayısını (matches_won) oran üzerinden hesaplıyoruz
                    int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                    member.setMatchesWon(matchesWon);

                    membersList.add(member);
                }
            }
        } catch (IllegalArgumentException | java.sql.SQLException e) {
            e.printStackTrace();
            System.err.println("Takım üyeleri çekilirken hata oluştu. Team ID: " + teamId);
        }
        
        return membersList;
    }

    /**
     * Checks if any matches have been created for a specific tournament by querying the 'matches' table.
     * Uses an efficient SQL query to determine the existence of at least one match record associated with the given tournament ID.
     * @param tournamentId The UUID of the tournament for which to check match creation
     * @return true if at least one match exists for the tournament, false otherwise or in case of errors.
     */
    public boolean checkIfFixtureCreated(String tournamentId) {
        if (tournamentId == null || tournamentId.trim().isEmpty()) return false;

        // COUNT kullanmak yerine sadece 1 kayıt bulması yeterli (Çok daha performanslıdır)
        String sql = "SELECT 1 FROM matches WHERE tournament_id = ? LIMIT 1";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setObject(1, java.util.UUID.fromString(tournamentId));
            
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Eğer kayıt bulursa true döner
            }
        } catch (IllegalArgumentException | java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves the tournament associated with a specific team by joining the 'tournaments', 'teams', and 'sports' tables.
     * Converts the database record into a Tournament model object and returns it.
     * @param teamId The UUID of the team for which to fetch the tournament
     * @return A Tournament object representing the tournament associated with the specified team, or null if not found or in case of errors.
     */
    public models.Tournament getTournamentByTeamId(String teamId) {
        if (teamId == null || teamId.trim().isEmpty()) return null;

        String sql = "SELECT t.tournament_id, t.tournament_name, s.name AS sport_name, " +
                     "t.start_date, t.end_date, t.max_players_per_team, t.has_ge250, " +
                     "t.access_code, t.campus_location, t.is_active " +
                     "FROM tournaments t " +
                     "INNER JOIN teams tm ON t.tournament_id = tm.tournament_id " +
                     "INNER JOIN sports s ON t.sport_id = s.id " +
                     "WHERE tm.team_id = ?";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setObject(1, java.util.UUID.fromString(teamId));

            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    models.SportType sType = null;
                    try {
                        sType = models.SportType.valueOf(rs.getString("sport_name").trim().toUpperCase().replace(" ", "_"));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Geçersiz spor türü: " + rs.getString("sport_name"));
                    }

                    java.time.LocalDate sDate = rs.getTimestamp("start_date").toLocalDateTime().toLocalDate();
                    java.time.LocalDate eDate = rs.getTimestamp("end_date").toLocalDateTime().toLocalDate();

                    models.Tournament tournament = new models.Tournament(
                        rs.getObject("tournament_id").toString(),
                        rs.getString("tournament_name"),
                        sType,
                        sDate,
                        eDate,
                        rs.getInt("max_players_per_team"),
                        rs.getBoolean("has_ge250"),
                        rs.getString("access_code"),
                        rs.getString("campus_location")
                    );
                    
                    tournament.setActive(rs.getBoolean("is_active"));
                    return tournament;
                }
            }
        } catch (IllegalArgumentException | java.sql.SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Inserts multiple match records into the 'matches' table for a specific tournament.
     * Uses batch processing to efficiently insert a list of Match objects into the database.
     * @param matches A List of Match objects to be inserted into the database
     * @param tournamentId The UUID of the tournament to which the matches belong
     * @return DbStatus indicating SUCCESS, QUERY_ERROR, or other relevant statuses based on the operation outcome.
     */
    public DbStatus saveMatches(java.util.List<models.Match> matches, String tournamentId) {
        if (matches == null || tournamentId == null || tournamentId.trim().isEmpty()) return DbStatus.QUERY_ERROR;
        if (matches.isEmpty()) return DbStatus.SUCCESS; // Boş liste geldiyse başarılı sayılır

        String sql = "INSERT INTO matches (match_id, tournament_id, sport_id, match_date, " +
                     "team1_id, team2_id, point_change, is_concluded) " +
                     "VALUES (?, ?, (SELECT sport_id FROM tournaments WHERE tournament_id = ?), ?, ?, ?, ?, false)";

        java.sql.Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Transaction başlat

            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                java.util.UUID tId = java.util.UUID.fromString(tournamentId);

                for (models.Match m : matches) {
                    stmt.setObject(1, java.util.UUID.fromString(m.getMatchId()));
                    stmt.setObject(2, tId);
                    stmt.setObject(3, tId); // Subquery için
                    
                    // LocalDateTime'ı veritabanı Timestamp'ine çeviriyoruz
                    stmt.setTimestamp(4, java.sql.Timestamp.valueOf(m.getDate())); 
                    
                    stmt.setObject(5, java.util.UUID.fromString(m.getTeam1().getTeamId()));
                    stmt.setObject(6, java.util.UUID.fromString(m.getTeam2().getTeamId()));
                    stmt.setInt(7, m.getPointChange());
                    
                    stmt.addBatch(); // Sorguyu batch (toplu işlem) listesine ekle
                }
                
                stmt.executeBatch(); // Tüm maçları tek seferde veritabanına yaz
            }
            
            conn.commit();
            return DbStatus.SUCCESS;

        } catch (IllegalArgumentException | java.sql.SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (java.sql.SQLException ex) {}
            e.printStackTrace();
            return DbStatus.QUERY_ERROR;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (java.sql.SQLException ex) {}
        }
    }

    /**
     * Retrieves a list of all matches associated with a specific tournament, regardless of the student's involvement.
     * Joins multiple tables to fetch match details, including teams and their captains, for the given tournament ID.
     * Converts database records into Match model objects and returns them in a list.
     * @param tournamentId The UUID of the tournament for which to fetch all matches
     * @return A List of Match objects representing all matches in the specified tournament, or an empty list if none found or in case of errors.
     */
    public java.util.ArrayList<models.Match> getAllTournamentMatches(String tournamentId) {
        java.util.ArrayList<models.Match> tournamentMatches = new java.util.ArrayList<>();
        
        if (tournamentId == null || tournamentId.trim().isEmpty()) return tournamentMatches;

        String sql = "SELECT m.match_id, m.match_date, m.point_change, m.winner_team_id, " +
                     "sp.name AS sport_name, " +
                     
                     // 1. Takım ve Kaptan Bilgileri (student_id uni_id için eklendi)
                     "t1.team_id AS t1_id, t1.team_name AS t1_name, t1.access_code AS t1_code, t1.max_capacity AS t1_cap, t1.ge250_requested AS t1_ge250, " +
                     "c1_u.full_name AS c1_name, c1_u.bilkent_email AS c1_email, c1_u.student_id AS c1_uni, c1_u.profile_pic_url AS c1_pic, c1_s.elo_point AS c1_elo, c1_s.win_rate AS c1_win, " +
                     
                     // 2. Takım ve Kaptan Bilgileri
                     "t2.team_id AS t2_id, t2.team_name AS t2_name, t2.access_code AS t2_code, t2.max_capacity AS t2_cap, t2.ge250_requested AS t2_ge250, " +
                     "c2_u.full_name AS c2_name, c2_u.bilkent_email AS c2_email, c2_u.student_id AS c2_uni, c2_u.profile_pic_url AS c2_pic, c2_s.elo_point AS c2_elo, c2_s.win_rate AS c2_win " +
                     
                     "FROM matches m " +
                     "INNER JOIN sports sp ON m.sport_id = sp.id " +
                     "INNER JOIN teams t1 ON m.team1_id = t1.team_id " +
                     "INNER JOIN users c1_u ON t1.captain_id = c1_u.id " +
                     "INNER JOIN students c1_s ON c1_u.id = c1_s.user_id " +
                     "INNER JOIN teams t2 ON m.team2_id = t2.team_id " +
                     "INNER JOIN users c2_u ON t2.captain_id = c2_u.id " +
                     "INNER JOIN students c2_s ON c2_u.id = c2_s.user_id " +
                     "WHERE m.tournament_id = ? ORDER BY m.match_date ASC";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setObject(1, java.util.UUID.fromString(tournamentId));

            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    
                    // --- 1. TAKIM (Kaptan -> Takım) ---
                    models.Student captain1 = new models.Student(rs.getString("c1_name"), rs.getString("c1_email"), rs.getString("c1_uni"));
                    captain1.setProfilePictureUrl(rs.getString("c1_pic"));
                    captain1.setEloPoint(rs.getInt("c1_elo"));
                    captain1.setWinRate(rs.getDouble("c1_win"));

                    models.Team team1 = new models.Team(
                        rs.getObject("t1_id").toString(), rs.getString("t1_name"), 
                        rs.getString("t1_code"), rs.getInt("t1_cap"), rs.getBoolean("t1_ge250"), captain1
                    );

                    // --- 2. TAKIM (Kaptan -> Takım) ---
                    models.Student captain2 = new models.Student(rs.getString("c2_name"), rs.getString("c2_email"), rs.getString("c2_uni"));
                    captain2.setProfilePictureUrl(rs.getString("c2_pic"));
                    captain2.setEloPoint(rs.getInt("c2_elo"));
                    captain2.setWinRate(rs.getDouble("c2_win"));

                    models.Team team2 = new models.Team(
                        rs.getObject("t2_id").toString(), rs.getString("t2_name"), 
                        rs.getString("t2_code"), rs.getInt("t2_cap"), rs.getBoolean("t2_ge250"), captain2
                    );

                    // --- SPOR TÜRÜ VE TARİH ---
                    models.SportType sType = models.SportType.valueOf(rs.getString("sport_name").trim().toUpperCase().replace(" ", "_"));
                    java.time.LocalDateTime matchDate = rs.getTimestamp("match_date").toLocalDateTime();

                    // --- MAÇ MODELİ ---
                    models.Match match = new models.Match(
                        rs.getObject("match_id").toString(), matchDate, sType, team1, team2
                    );
                    match.setPointChange(rs.getInt("point_change"));

                    // --- KAZANAN TAKIM DURUMU ---
                    String winnerId = rs.getString("winner_team_id");
                    if (winnerId != null) {
                        if (winnerId.equals(team1.getTeamId())) match.setWinner(team1);
                        else if (winnerId.equals(team2.getTeamId())) match.setWinner(team2);
                    } 

                    tournamentMatches.add(match);
                }
            }
        } catch (IllegalArgumentException | java.sql.SQLException e) {
            e.printStackTrace();
        }

        return tournamentMatches;
    }

    /**
     * Deletes a tournament from the database based on the provided tournament ID.
     * Handles potential foreign key constraint violations by catching SQL exceptions and providing informative error messages.
     * @param tournamentId The UUID of the tournament to be deleted
     * @return DbStatus indicating SUCCESS if the tournament was deleted, DATA_NOT_FOUND if no tournament with the given ID exists, or QUERY_ERROR in case of errors (including foreign key violations).
     */
    public DbStatus deleteTournament(String tournamentId) {
        if (tournamentId == null || tournamentId.trim().isEmpty()) {
            return DbStatus.QUERY_ERROR;
        }

        String sql = "DELETE FROM tournaments WHERE tournament_id = ?";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setObject(1, java.util.UUID.fromString(tournamentId));
            
            int rowsDeleted = stmt.executeUpdate();
            
            return (rowsDeleted > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (IllegalArgumentException e) {
            System.err.println("Geçersiz UUID formatı: " + tournamentId);
            return DbStatus.QUERY_ERROR;
        } catch (java.sql.SQLException e) {
            // Foreign Key ihlali (23503) durumu
            if ("23503".equals(e.getSQLState())) {
                System.err.println("Hata: Bu turnuvaya bağlı takımlar veya maçlar olduğu için turnuva silinemiyor!");
            } else {
                e.printStackTrace();
            }
            return DbStatus.QUERY_ERROR;
        }
    }

    /**
     * Retrieves a list of all reservations from the database, including details about the reserved facility, the organizer (student), and the associated sport.
     * Joins multiple tables to fetch comprehensive reservation information and converts database records into Reservation model objects.
     * @return A List of Reservation objects representing all reservations in the database, or an empty list if none found or in case of errors.
     */
    public java.util.ArrayList<models.Reservation> getAllReservations() {
        java.util.ArrayList<models.Reservation> reservations = new java.util.ArrayList<>();

        // SQL Sorgusu: r.reserved_by kolonu şemana uygun olarak JOIN işlemine dahil edildi.
        String sql = "SELECT r.reservation_id, r.reservation_date, r.time_slot, r.is_cancelled, r.has_attended, " +
                     "f.facility_id, f.name AS facility_name, f.campus_location, f.capacity, f.is_under_maintenance, " +
                     "sp.name AS sport_name, " +
                     "u.full_name, u.bilkent_email, u.student_id AS uni_id, u.profile_pic_url, " +
                     "s.elo_point, s.win_rate " +
                     "FROM reservations r " +
                     "INNER JOIN facilities f ON r.facility_id = f.facility_id " +
                     "INNER JOIN sports sp ON f.sport_id = sp.id " +  
                     "INNER JOIN students s ON r.reserved_by = s.user_id " + // fk->public.students.user_id bağlantısı
                     "INNER JOIN users u ON s.user_id = u.id " +
                     "ORDER BY r.reservation_date DESC, r.time_slot ASC";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                
                // --- 1. TESİS (FACILITY) SPOR TÜRÜ ---
                models.SportType fSportType = null;
                try {
                    fSportType = models.SportType.valueOf(rs.getString("sport_name").trim().toUpperCase().replace(" ", "_"));
                } catch (IllegalArgumentException e) {
                    System.err.println("Uyarı: Tesis için veritabanında eşleşmeyen spor türü: " + rs.getString("sport_name"));
                }

                // --- 2. TESİS (FACILITY) OBJESİ ---
                models.Facility facility = new models.Facility(
                    rs.getObject("facility_id").toString(),
                    rs.getString("facility_name"),
                    rs.getString("campus_location"),
                    fSportType,
                    rs.getInt("capacity")
                );
                facility.setUnderMaintenance(rs.getBoolean("is_under_maintenance"));
                
                // --- 3. ORGANİZATÖR (STUDENT) OBJESİ ---
                models.Student organizer = new models.Student(
                    rs.getString("full_name"),
                    rs.getString("bilkent_email"),
                    rs.getString("uni_id")
                );
                organizer.setProfilePictureUrl(rs.getString("profile_pic_url"));
                organizer.setEloPoint(rs.getInt("elo_point"));
                organizer.setWinRate(rs.getDouble("win_rate"));

                // --- 4. TARİH DÖNÜŞÜMÜ ---
                java.time.LocalDate resDate = rs.getDate("reservation_date").toLocalDate();

                // --- 5. REZERVASYON (RESERVATION) OBJESİ ---
                models.Reservation reservation = new models.Reservation(
                    rs.getObject("reservation_id").toString(),
                    facility,
                    resDate,
                    rs.getString("time_slot")
                );

                reservation.setCancelled(rs.getBoolean("is_cancelled"));
                reservation.setHasAttended(rs.getBoolean("has_attended"));
                reservation.setOrganizer(organizer);

                reservations.add(reservation);
            }

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        return reservations;
    }

    /**
     * Removes a participant (student) from a duello reservation by deleting the corresponding record from the 'reservation_attendees' table.
     * Uses a SQL DELETE statement with a subquery to identify the student ID based on the provided email and reservation ID.
     * @param reservationId The UUID of the reservation from which to remove the participant
     * @param studentEmail The email of the student to be removed from the reservation
     * @return DbStatus indicating SUCCESS if the participant was removed, DATA_NOT_FOUND if no matching record was found, or QUERY_ERROR in case of errors (including invalid input).
     */
    public DbStatus removeDuelloParticipant(String reservationId, String studentEmail) {
        
        if (reservationId == null || reservationId.trim().isEmpty() || 
            studentEmail == null || studentEmail.trim().isEmpty()) {
            return DbStatus.QUERY_ERROR;
        }

        // reservation_attendees tablosuna ve şemana tam uyumlu SQL sorgusu
        String sql = "DELETE FROM reservation_attendees " +
                     "WHERE reservation_id = ? AND student_id = (SELECT id FROM users WHERE bilkent_email = ?)";

        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            // 1. Parametre: Rezervasyon UUID'si
            stmt.setObject(1, java.util.UUID.fromString(reservationId));
            
            // 2. Parametre: Kullanıcının e-postası (Alt sorgu için)
            stmt.setString(2, studentEmail);
            
            int rowsDeleted = stmt.executeUpdate();
            
            return (rowsDeleted > 0) ? DbStatus.SUCCESS : DbStatus.DATA_NOT_FOUND;

        } catch (IllegalArgumentException e) {
            System.err.println("Geçersiz UUID formatı: " + reservationId);
            return DbStatus.QUERY_ERROR;
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                return DbStatus.CONNECTION_ERROR;
            }
            return DbStatus.QUERY_ERROR;
        }
    }
}