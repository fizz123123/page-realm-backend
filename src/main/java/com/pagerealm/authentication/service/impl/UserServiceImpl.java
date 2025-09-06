package com.pagerealm.authentication.service.impl;

import com.pagerealm.authentication.dto.UserDTO;
import com.pagerealm.authentication.dto.request.EmailVerifyRequest;
import com.pagerealm.authentication.entity.PasswordResetToken;
import com.pagerealm.authentication.entity.Role;
import com.pagerealm.authentication.entity.User;
import com.pagerealm.authentication.repository.PasswordResetTokenRepository;
import com.pagerealm.authentication.repository.RoleRepository;
import com.pagerealm.authentication.repository.UserRepository;
import com.pagerealm.authentication.s3.S3Buckets;
import com.pagerealm.authentication.s3.S3Service;
import com.pagerealm.authentication.service.TotpService;
import com.pagerealm.authentication.service.UserService;
import com.pagerealm.authentication.utils.EmailService;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.mail.MessagingException;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Value("${frontend.url}")
    String frontendUrl;

    @Value("${aws.s3.default-avatar-key}")
    String defaultAvatarKey;

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    PasswordResetTokenRepository passwordResetTokenRepository;
    EmailService emailService;
    TotpService totpService;
    // S3
    private final S3Service s3Service;
    private final S3Buckets s3Buckets;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, PasswordResetTokenRepository passwordResetTokenRepository, EmailService emailService, TotpService totpService, S3Service s3Service, S3Buckets s3Buckets) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.totpService = totpService;
        this.s3Service = s3Service;
        this.s3Buckets = s3Buckets;
    }

    //----------------------------------------

    @Override
    public List<User> getAllUsers() {

        return userRepository.findAll();
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        return convertToDto(user);
    }

    @Override
    public User findByUsername(String username) {
        User user = userRepository.findByUserName(username).orElseThrow();
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    @Override
    public void deleteByEmail(String email) {
        userRepository.deleteByEmail(email);
    }

    // Mapping User to UserDTO
    private UserDTO convertToDto(User user) {

        // 取出等級並轉為顯示字串
        String tierLabel = (user.getMembershipTier() == null)
                ? "會員等級1"
                : user.getMembershipTier().displayName();

        return new UserDTO(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getGender(),
                user.getBirthdate(),
                tierLabel,
                user.getAvatarUrl(),
                user.getVerificationCode(),
                user.getVerificationCodeExpiresAt(),
                user.isEnabled(),
                user.isTotpEnabled(),
                user.getTotpSecret(),
                user.getSignedMethod(),
                user.getRole(),
                user.getCreatedDate(),
                user.getUpdatedDate()
        );
    }

    //------AdminController-----
    @Override
    public void updateAccountEnabledStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }


    //-------Forgot Password------
    @Override
    public void generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(24, ChronoUnit.HOURS);
        PasswordResetToken resetToken = new PasswordResetToken(token, expiryDate, user);
        passwordResetTokenRepository.save(resetToken);

        // Password Reset URL format:
        // https://www.example.com/reset-password?token=123e4567-e89b-12d3-a456-4283
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
    }

    @Override
    public void resetPassword(String token, String newPassword, String checkPassword) {

        if (newPassword.equals(checkPassword)) {

            PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                    .orElseThrow(() -> new RuntimeException("無效的Token"));

            if (resetToken.isUsed()) throw new RuntimeException("Token已被使用");
            if (resetToken.getExpiryDate().isBefore(Instant.now())) throw new RuntimeException("Token已過期");

            User user = resetToken.getUser();
            if (passwordEncoder.matches(newPassword, user.getPassword()))
                throw new RuntimeException("新密碼不可與舊密碼相同");

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);
        }else {
            throw new RuntimeException("密碼需一致");
        }
    }

    //------Email Verification------

    @Override
    public void buildAndSendVerificationEmail(String rawCode, String email) {
        String subject = "Page Realm 註冊驗證碼";
        String verificationCode = rawCode;
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to Page Realm!</h2>"
                + "<p style=\"font-size: 16px;\">請輸入下方驗證碼以完成註冊:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
        try {
            emailService.sendVerificationEmail(email, subject, htmlMessage);
        } catch (MessagingException e) {
            //for debug
            e.printStackTrace();
        }
    }

    @Override
    public void verifyUser(EmailVerifyRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("驗證失敗，請確認驗證碼是否正確"));

        if (user.getVerificationCode() == null || user.getVerificationCodeExpiresAt() == null) {
            throw new RuntimeException("驗證碼無效，請重新申請驗證");
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("驗證碼已過期，請重新申請驗證");
        }

        String inputCodeHash = DigestUtils.sha256Hex(request.getVerificationCode());

        if (inputCodeHash.equals(user.getVerificationCode())) {
            user.setEnabled(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            userRepository.save(user);
        } else {
            throw new RuntimeException("驗證失敗，請確認驗證碼是否正確");
        }
    }

    @Override
    public void resendVerificationCode(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            if (user.isEnabled()) {
                throw new RuntimeException("帳號已通過驗證");
            }
            String rawCode = generateVerificationCode();
            String hashCode = DigestUtils.sha256Hex(rawCode);

            buildAndSendVerificationEmail(rawCode, email);

            user.setVerificationCode(hashCode);
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    //產生隨機6位數驗證碼
    @Override
    public String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    //----------OAuth2-----------

    /**
     * @descriptiotn : OAuth2認證成功後，若DB中沒有該email，直接註冊(跳過email註冊認證)
     */
    @Override
    public User registerOAuth2User(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        applyDefaultAvatarIfAbsent(user);
        return userRepository.save(user);
    }


    //----------TOTP-----------
    @Override
    public GoogleAuthenticatorKey generateTotpSecret(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        GoogleAuthenticatorKey key = totpService.generateSecret();
        user.setTotpSecret(key.getKey());
        userRepository.save(user);
        return key;
    }

    @Override
    public boolean validateTotpCode(Long userId, int code) {

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        return totpService.verifyCode(user.getTotpSecret(), code);
    }

    @Override
    public void enableTotp(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setTotpEnabled(true);
        userRepository.save(user);
    }

    @Override
    public void disableTotp(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setTotpEnabled(false);
        userRepository.save(user);
    }

    //User Controller
    @Override
    public void updatePassword(Long userId, String newPassword, String checkPassword) {

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (!newPassword.equals(checkPassword)) {
            throw new RuntimeException("兩次輸入的密碼不一致");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("新密碼不可與舊密碼相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void updateBirthdate(Long userId, LocalDate birthdate) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        user.setBirthdate(birthdate);
        userRepository.save(user);
    }

    @Override
    public void updateGender(Long userId, String gender) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        user.setGender(gender);
        userRepository.save(user);
    }

    @Override
    public void updateUsername(Long userId, String username) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        user.setUserName(username);
        userRepository.save(user);
    }

    public String uploadAvatar(Long userId, MultipartFile file){
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("檔案不可為空");
        }

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        String ext;
        switch (contentType) {
            case "image/jpeg": ext = "jpg"; break;
            case "image/png":  ext = "png"; break;
            case "image/gif":  ext = "gif"; break;
            case "image/webp": ext = "webp"; break;
            default: throw new RuntimeException("不支援的檔案格式");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 以 email 產生安全檔名
        String safeEmail = user.getEmail().toLowerCase().replaceAll("[^a-z0-9]", "_");
        String key = "avatars/" + safeEmail + "_avatar." + ext;

        try {
            byte[] bytes = file.getBytes();

            // 上傳到 S3 並設為公開讀取
            s3Service.putObjectWithContentType(s3Buckets.getUser(), key, bytes, contentType);

            // 建立公開 URL
            String url = s3Service.buildPublicUrl(s3Buckets.getUser(), key);

            // 更新使用者頭像 URL
            user.setAvatarUrl(url);
            userRepository.save(user);

            return url;
        } catch (IOException e) {
            throw new RuntimeException("上傳失敗，請稍後再試");
        }
    }

    private void applyDefaultAvatarIfAbsent(User user) {
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            String url = s3Service.buildPublicUrl(s3Buckets.getUser(), defaultAvatarKey);
            user.setAvatarUrl(url);
        }
    }
}
