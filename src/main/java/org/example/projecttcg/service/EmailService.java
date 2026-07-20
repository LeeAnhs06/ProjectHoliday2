package org.example.projecttcg.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public void sendOtpEmail(String toEmail, String otp) {
        // ALWAYS log the OTP to the console first so that it is easily testable
        log.info("=================================================");
        log.info("[OTP ĐỔI MẬT KHẨU] Tài khoản: {} | Mã OTP: {}", toEmail, otp);
        log.info("=================================================");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[TCG Market] Mã OTP xác nhận đổi mật khẩu");
            message.setText("Xin chào,\n\nBạn đang thực hiện yêu cầu đổi mật khẩu tài khoản TCG Market.\nMã OTP xác nhận của bạn là: " + otp + "\nMã OTP này có hiệu lực trong vòng 5 phút.\n\nNếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\nTrân trọng,\nTCG Market Team.");
            mailSender.send(message);
            log.info("Email OTP sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.warn("Không thể gửi email OTP (Có thể bạn chưa cấu hình Gmail SMTP trong application.properties): {}", e.getMessage());
        }
    }
}
