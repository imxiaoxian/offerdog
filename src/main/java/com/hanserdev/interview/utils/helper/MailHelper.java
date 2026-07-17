package com.hanserdev.interview.utils.helper;

import com.hanserdev.interview.constants.RedisConstants;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MailHelper {

    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(5);
    private static final int VERIFICATION_CODE_LENGTH = 6;
    @Resource
    private JavaMailSender mailSender;

    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.mail.username}")
    private String username;

    public boolean sendRegistrationCode(String username, String email, HttpSession session) {
        return sendEmailCode(username, email, VerificationScene.REGISTER);
    }

    public boolean sendLoginCode(String email, HttpSession session) {
        return sendEmailCode(null, email, VerificationScene.LOGIN);
    }

    public boolean verifyRegistrationCode(String email, String inputCode, HttpSession session) {
        return verifyEmailCode(email, inputCode, VerificationScene.REGISTER);
    }

    public boolean verifyLoginCode(String email, String inputCode, HttpSession session) {
        return verifyEmailCode(email, inputCode, VerificationScene.LOGIN);
    }

    private boolean sendEmailCode(String username, String email, VerificationScene scene) {
        if (StringUtils.isBlank(email)) {
            log.warn("{}邮箱不能为空", scene);
            return false;
        }
        String verificationCode = generateVerificationCode();
        storeVerificationState(email, verificationCode, scene);
        String htmlContent = renderMailTemplate(username, verificationCode);
        return sendMimeMail(scene.getSubject(), email, htmlContent);
    }

    private boolean verifyEmailCode(String email, String inputCode, VerificationScene scene) {
        if (StringUtils.isAnyBlank(email, inputCode)) {
            return false;
        }
        String redisKey = buildCacheKey(email, scene);
        Object cachedCode = redisTemplate.opsForValue().get(redisKey);
        if (!(cachedCode instanceof String cached)) {
            return false;
        }
        boolean match = Strings.CI.equals(cached, inputCode);
        if (match) {
            redisTemplate.delete(redisKey);
        }
        return match;
    }

    private boolean sendMimeMail(String subject, String email, String htmlContent) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            helper.setSubject(subject);
            helper.setFrom(username);
            helper.setTo(email);
            helper.setSentDate(new Date());
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("发送邮件验证码成功, scene={}, email={}", subject, email);
            return true;
        } catch (MessagingException | MailException ex) {
            log.error("发送邮件验证码失败, scene={}, email={}, error={}", subject, email, ex.getMessage(), ex);
            return false;
        }
    }

    private String renderMailTemplate(String username, String verificationCode) {
        Context context = new Context(Locale.CHINA);
        context.setVariable("verifyCode", Arrays.asList(verificationCode.split("")));
        context.setVariable("username", StringUtils.defaultIfBlank(username, "同学"));
        context.setVariable("expiredMinutes", VERIFICATION_CODE_TTL.toMinutes());
        try {
            return templateEngine.process("EmailVerificationCode.html", context);
        } catch (Exception ex) {
            log.error("渲染邮箱验证码模板失败", ex);
            throw ex;
        }
    }

    private void storeVerificationState(String email, String code, VerificationScene scene) {
        String redisKey = buildCacheKey(email, scene);
        redisTemplate.opsForValue().set(redisKey, code, VERIFICATION_CODE_TTL.getSeconds(), TimeUnit.SECONDS);
    }

    private String generateVerificationCode() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder builder = new StringBuilder(VERIFICATION_CODE_LENGTH);
        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
            builder.append(random.nextInt(0, 10));
        }
        return builder.toString();
    }

    private String buildCacheKey(String email, VerificationScene scene) {
        return RedisConstants.buildEmailCodeKey(scene.getScene(), email);
    }

    private enum VerificationScene {
        REGISTER("register", "【OfferDog】邮箱注册验证码"),
        LOGIN("login", "【OfferDog】邮箱登录验证码");

        private final String scene;
        private final String subject;

        VerificationScene(String scene, String subject) {
            this.scene = scene;
            this.subject = subject;
        }

        String getScene() {
            return scene;
        }

        String getSubject() {
            return subject;
        }
    }
}
