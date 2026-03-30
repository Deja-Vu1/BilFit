package database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class Email {

    private static String smtpHost;
    private static String smtpPort;
    private static String senderEmail;
    private static String senderPassword;

    // Sınıf yüklendiğinde db.properties dosyasını okuyacak statik blok
    static {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("db.properties")) {
            // Dosyayı yükle
            prop.load(input);

            // Properties dosyasındaki anahtarları (key) kullanarak değerleri atıyoruz
            smtpHost = prop.getProperty("mail.smtp.host", "smtp.gmail.com"); // Bulamazsa varsayılanı kullanır
            smtpPort = prop.getProperty("mail.smtp.port", "587");
            senderEmail = prop.getProperty("mail.sender.email");
            senderPassword = prop.getProperty("mail.sender.password");
        } catch (IOException ex) {
            System.err.println("db.properties okunurken bir hata olustu: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Belirtilen e-posta adresine düz metin (String) formatında mail gönderir.
     * * @param toEmail Alıcının e-posta adresi
     * @param subject E-postanın konusu
     * @param body    E-postanın içeriği (Düz metin)
     * @return Başarılıysa true, hata oluşursa false döner.
     */
    public static boolean sendEmail(String toEmail, String subject, String body) {
        
        // Gerekli ayarların yüklendiğinden emin olalım
        if (senderEmail == null || senderPassword == null) {
            System.err.println("E-posta gonderilemedi: Gonderici bilgileri db.properties dosyasindan okunamadi.");
            return false;
        }

        // SMTP Sunucu ayarlarını yapılandırıyoruz
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // Güvenli bağlantı (TLS)
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);

        // Oturum (Session) oluşturuyoruz ve kimlik doğrulama yapıyoruz
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            // E-posta mesajını oluşturuyoruz
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8"); // İçeriği HTML olarak atıyoruz

            // E-postayı gönder
            Transport.send(message);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}