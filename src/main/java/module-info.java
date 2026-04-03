module bilfit {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql; // Database kullandığın için
    requires org.postgresql.jdbc;
    requires jakarta.mail;
    requires javafx.graphics;
    requires javafx.base;
    requires org.controlsfx.controls;

    opens core to javafx.graphics, javafx.fxml;
    opens controllers to javafx.fxml;
    opens database to java.sql;
    opens models to javafx.base;
    
    exports core;
    exports controllers;
    exports models;
    exports managers;
}