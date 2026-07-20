package org.example.projecttcg.controller;

import lombok.RequiredArgsConstructor;
import org.example.projecttcg.model.User;
import org.example.projecttcg.service.UserService;
import org.example.projecttcg.service.EmailService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final EmailService emailService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return userService.findByEmail(auth.getName());
        }
        return null;
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", currentUser);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam String displayName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String avatarUrl,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            userService.updateProfile(currentUser.getId(), displayName, phone, address, avatarUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cá nhân thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/profile/send-otp")
    @ResponseBody
    public Map<String, Object> sendOtp(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để nhận mã OTP.");
            return response;
        }

        String otp = emailService.generateOtp();
        session.setAttribute("PASSWORD_RESET_OTP", otp);
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));

        emailService.sendOtpEmail(currentUser.getEmail(), otp);

        response.put("success", true);
        response.put("message", "Mã OTP đã được gửi thành công tới Gmail của bạn (và hiển thị tại Console log)!");
        return response;
    }

    @PostMapping("/profile/change-password")
    public String changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @RequestParam String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        String sessionOtp = (String) session.getAttribute("PASSWORD_RESET_OTP");
        LocalDateTime expiry = (LocalDateTime) session.getAttribute("OTP_EXPIRY");

        if (sessionOtp == null || expiry == null || expiry.isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP đã hết hạn hoặc chưa được gửi. Vui lòng yêu cầu lại mã mới.");
            return "redirect:/profile";
        }

        if (!sessionOtp.equals(otp.trim())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP nhập vào không chính xác.");
            return "redirect:/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xác nhận mật khẩu mới không khớp.");
            return "redirect:/profile";
        }

        try {
            userService.changePassword(currentUser.getId(), oldPassword, newPassword);
            session.removeAttribute("PASSWORD_RESET_OTP");
            session.removeAttribute("OTP_EXPIRY");
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi mật khẩu thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/profile";
    }
}
