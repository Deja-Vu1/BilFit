package database;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class EmailService {

    public boolean sendActivationEmail(String toEmail, String activationCode) {
        String htmlContent = "";

        // 1. HTML dosyasını resources klasöründen oku
        try (InputStream is = EmailService.class.getResourceAsStream("/templates/activation.html")) {
            if (is == null) {
                System.err.println("HATA: HTML şablon dosyası bulunamadı!");
                return false;
            }
            // InputStream'i String'e çevir (Java 9 ve sonrası için en temiz yol)
            htmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            System.err.println("HTML dosyası okunurken hata oluştu: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        // 2. HTML içindeki {{ACTIVATION_CODE}} yer tutucusunu gerçek kodla değiştir
        htmlContent = htmlContent.replace("{{ACTIVATION_CODE}}", activationCode);

        // 3. EmailHelper'ı kullanarak maili gönder
        String subject = "BilFit - Activation Code";
        boolean isSent = Email.sendEmail(toEmail, subject, htmlContent);

        return isSent;
    }
}