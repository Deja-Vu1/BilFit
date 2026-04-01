module bilfit {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql; // Database için
    requires org.postgresql.jdbc;
    requires jakarta.mail;
    requires javafx.graphics;

    opens core to javafx.graphics, javafx.fxml;
    opens controllers to javafx.fxml;
    opens database to java.sql;
    
    exports core;
}