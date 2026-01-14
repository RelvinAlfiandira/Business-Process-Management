package com.uncal.bpm_backend.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Mengirim email selamat datang setelah registrasi berhasil
     */
    public void sendWelcomeEmail(String toEmail, String username, String password) {
        try {
            log.info("Mencoba mengirim email ke: {}", toEmail);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("mailbpmsystem@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Selamat Datang di Business Process Management");
            message.setText(
                "Halo,\n\n" +
                "Selamat! Akun Anda telah berhasil didaftarkan.\n\n" +
                "Username: " + username + "\n" +
                "Password: " + password + "\n\n" +
                "Silakan login untuk mulai menggunakan sistem Business Process Management.\n\n" +
                "Terima kasih."
            );
            
            mailSender.send(message);
            log.info("✅ Email berhasil dikirim ke: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Gagal mengirim email ke {}: {}", toEmail, e.getMessage());
            e.printStackTrace();
            // Tidak throw exception agar proses registrasi tetap berhasil meski email gagal
        }
    }
}
