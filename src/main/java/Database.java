package java;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Database {
    private Connection conn;
    private String url;
    private String user;
    private String password;

    public Database() {
        loadConfig();
    }

    private void loadConfig() {
        // Properties nesnesi oluşturuluyor
        Properties prop = new Properties();
        
        // try-with-resources kullanarak dosyayı okuyoruz
        try (InputStream input = new FileInputStream("db.properties")) {
            // Dosyayı yükle
            prop.load(input);

            // Değerleri değişkenlere ata
            this.url = prop.getProperty("db.url");
            this.user = prop.getProperty("db.user");
            this.password = prop.getProperty("db.password");
            
            System.out.println("Yapılandırma başarıyla yüklendi.");
        } catch (IOException ex) {
            System.out.println("Hata: db.properties dosyası bulunamadı!");
            ex.printStackTrace();
        }
    }

    public DbStatus connect() {
        try {
            conn = DriverManager.getConnection(url, user, password);
            return DbStatus.SUCCESS;
        } catch (SQLException e) {
            return DbStatus.CONNECTION_ERROR;
        }
    }

    public DbStatus disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                return DbStatus.SUCCESS;
            }
            else{
                return DbStatus.CONNECTION_ERROR;
            }
        } catch (SQLException e) {
            return DbStatus.CONNECTION_ERROR;
        }
    }

    public ResultSet getAllUsers(){

        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            return rs;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public DbStatus addUser(String name, int age) {
        String sql = "INSERT INTO users (name, age) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setInt(2, age);
            
            // int affectedRows = pstmt.executeUpdate();
            return DbStatus.SUCCESS;
        } catch (SQLException e) {
            System.out.println("Ekleme hatası: " + e.getMessage());
            return DbStatus.QUERY_ERROR;
        }
    }

    public DbStatus deleteUser(String name) {
        String sql = "DELETE FROM users WHERE name = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            
            // int affectedRows = pstmt.executeUpdate();
            return DbStatus.SUCCESS;
        } catch (SQLException e) {
            System.out.println("Silme hatası: " + e.getMessage());
            return DbStatus.QUERY_ERROR;
        }
    }
}
