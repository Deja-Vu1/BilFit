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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import models.SportType;
import models.Student;
import models.Facility;
import models.Reservation;
import java.util.List;
import java.util.ArrayList;

public class Database {

    // 1. Uygulama boyunca yaşayacak TEK ortak nesne
    private static Database instance;

    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String salt;
    private Connection conn;

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
     * student_id is implicitly set to NULL and role is set to 'admin'.
     * @param name Admin's full name
     * @param email Admin's email address
     * @param passwordHash Admin's hashed password
     * @param adminActivationCode The special code required to register as an admin
     * @return DbStatus indicating SUCCESS, EMAIL_ALREADY_EXISTS, INVALID_CODE, QUERY_ERROR, etc.
     */
    public DbStatus registerAdmin(String name, String email, String passwordHash, String adminActivationCode) {
        
        String gcSql = "DELETE FROM users WHERE is_activated = FALSE AND created_at < NOW() - INTERVAL '30 minutes'";
        String checkEmailSql = "SELECT bilkent_email FROM users WHERE bilkent_email = ?";
        
        String checkAdminCodeSql = "SELECT id FROM admin_activation WHERE activation_code = ?";
        String deleteAdminCodeSql = "DELETE FROM admin_activation WHERE activation_code = ?";
        
        String insertSql = "INSERT INTO users (full_name, bilkent_email, student_id, password_hash, role, is_activated) VALUES (?, ?, NULL, ?, 'admin', FALSE)";

        try {

            try (PreparedStatement gcStmt = getConnection().prepareStatement(gcSql)) {
                gcStmt.executeUpdate();
            }

            try (PreparedStatement checkStmt = getConnection().prepareStatement(checkEmailSql)) {
                checkStmt.setString(1, email);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return DbStatus.EMAIL_ALREADY_EXISTS; 
                    }
                }
            }

            try (PreparedStatement checkCodeStmt = getConnection().prepareStatement(checkAdminCodeSql)) {
                
                checkCodeStmt.setString(1, adminActivationCode); 

                try (ResultSet rs = checkCodeStmt.executeQuery()) {
                    if (!rs.next()) {
                        return DbStatus.INVALID_CODE; 
                    }
                }
            }

            try (PreparedStatement insertStmt = getConnection().prepareStatement(insertSql)) {
                
                passwordHash = hashPassword(passwordHash); 
                
                insertStmt.setString(1, name);           // full_name
                insertStmt.setString(2, email);          // bilkent_email
                insertStmt.setString(3, passwordHash);   // password_hash

                int insertedRows = insertStmt.executeUpdate();
                
                if (insertedRows > 0) {
                    
                    try (PreparedStatement deleteCodeStmt = getConnection().prepareStatement(deleteAdminCodeSql)) {
                        deleteCodeStmt.setString(1, adminActivationCode);
                        deleteCodeStmt.executeUpdate();
                    }

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
     * If authentication is successful, updates the 'last_seen' timestamp in the students table.
     * @param email Student's Bilkent email address
     * @param plainPassword The raw password entered by the user
     * @return DbStatus indicating SUCCESS, ACCOUNT_NOT_ACTIVATED, INVALID_CREDENTIALS, etc.
     */
    public DbStatus loginStudent(String email, String plainPassword) {

        String selectSql = "SELECT id, password_hash, role, is_activated FROM users WHERE bilkent_email = ?";
        
        String updateLastSeenSql = "UPDATE students SET last_seen = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(selectSql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.util.UUID userId = rs.getObject("id", java.util.UUID.class);
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
     * @param email User's Bilkent email address
     * @param newPassword The new raw password to be set
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, CONNECTION_ERROR, or QUERY_ERROR
     */
    public DbStatus updatePassword(String email, String newPassword) {
        String updateSql = "UPDATE users SET password_hash = ? WHERE bilkent_email = ?";

        try (PreparedStatement updateStmt = getConnection().prepareStatement(updateSql)) {
            
            String newPasswordHash = hashPassword(newPassword);
            
            updateStmt.setString(1, newPasswordHash);
            updateStmt.setString(2, email);

            int updatedRows = updateStmt.executeUpdate();
            
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
     * including their sport interests, and updates the provided Student object.
     * @param student The existing Student object to be updated
     * @param email Student's Bilkent email address to query the database
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus fillStudentDataByEmail(Student student, String email) {
        
        if (student == null) {
            return DbStatus.QUERY_ERROR;
        }

        String sql = "SELECT u.full_name, u.bilkent_email, u.student_id AS uni_id, u.password_hash, " +
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
     * Accepts a duello request and adds the student as an official participant.
     * Updates request status, decrements empty slots, and manages matching status.
     * @param reservationId The UUID of the duello/reservation
     * @param studentEmail The Bilkent email of the student being accepted
     * @return DbStatus indicating SUCCESS, DATA_NOT_FOUND, or errors.
     */
    public DbStatus updateDuelloParticipant(String reservationId, String studentEmail) {
        
        String updateRequestSql = "UPDATE duello_requests SET status = 'Accepted' " +
                                  "WHERE reservation_id = ? AND requester_id = (SELECT id FROM users WHERE bilkent_email = ?) " +
                                  "AND status = 'Pending'";

        String updateDuelloSql = "UPDATE duellos SET empty_slots = empty_slots - 1, " +
                                 "is_matched = CASE WHEN empty_slots - 1 = 0 THEN TRUE ELSE FALSE END " +
                                 "WHERE reservation_id = ? AND empty_slots > 0";

        String insertAttendeeSql = "INSERT INTO reservation_attendees (reservation_id, student_id) " +
                                   "SELECT ?, id FROM users WHERE bilkent_email = ?";

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

            conn.commit();
            return DbStatus.SUCCESS;

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
     * Finds up to 5 potential opponents for a solo match based on sport type and ELO proximity.
     * Searches for other students who have 'is_elo_matching_enabled' set to TRUE,
     * share the same sport interest, and are ordered by how close their ELO is to the current student.
     * @param currentStudent The student looking for a match
     * @param sportName The name of the sport (e.g., "TENNIS" or "TABLE TENNIS")
     * @return An ArrayList containing a maximum of 5 matching Student objects (closest ELO first).
     */
    public ArrayList<Student> findOpponentForMatch(Student currentStudent, String sportName) {
        
        ArrayList<Student> potentialOpponents = new ArrayList<>();

        if (currentStudent == null || sportName == null || sportName.trim().isEmpty()) {
            return potentialOpponents; 
        }

        String formattedSportName = sportName.trim().toUpperCase().replace(" ", "_");

        String sql = "SELECT u.full_name, u.bilkent_email, u.student_id AS uni_id, " +
                     "s.elo_point, s.penalty_points, s.reliability_score, s.matches_played, s.win_rate " +
                     "FROM users u " +
                     "INNER JOIN students s ON u.id = s.user_id " +
                     "INNER JOIN student_interests si ON u.id = si.student_id " +
                     "INNER JOIN sports sp ON si.sport_id = sp.id " +
                     "WHERE u.bilkent_email != ? " + // Kendisi hariç
                     "  AND u.role = 'student' " +
                     "  AND s.is_elo_matching_enabled = TRUE " + // Eşleştirmeyi açanlar
                     "  AND UPPER(REPLACE(sp.name, ' ', '_')) = ? " + // Aynı sporu arayanlar
                     "ORDER BY ABS(s.elo_point - ?) ASC " + // Puanı en yakın olanlar ilk gelsin
                     "LIMIT 5"; // MAKSİMUM 5 KİŞİ GETİR

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            
            stmt.setString(1, currentStudent.getBilkentEmail());
            stmt.setString(2, formattedSportName);
            stmt.setInt(3, currentStudent.getEloPoint()); // Senin ELO'nu referans alıyoruz

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Student opponent = new Student(
                        rs.getString("full_name"), 
                        rs.getString("bilkent_email"), 
                        rs.getString("uni_id")
                    );
                    
                    opponent.setEloPoint(rs.getInt("elo_point"));
                    opponent.setPenaltyPoints(rs.getInt("penalty_points"));
                    opponent.setReliabilityScore(rs.getDouble("reliability_score"));
                    opponent.setMatchesPlayed(rs.getInt("matches_played"));
                    opponent.setWinRate(rs.getDouble("win_rate"));
                    
                    int matchesWon = (int) Math.round(rs.getInt("matches_played") * rs.getDouble("win_rate"));
                    opponent.setMatchesWon(matchesWon);

                    potentialOpponents.add(opponent);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return potentialOpponents;
    }
}