package com.hanserdev.interview.constants;

public final class RedisConstants {

    private static final String PHONE_SMS_CODE_PREFIX = "login:phone:sms:";
    private static final String EMAIL_CODE_KEY_PREFIX = "login:email:code:";

    private RedisConstants() {
    }

    public static String buildPhoneLoginCodeKey(String phone) {
        return PHONE_SMS_CODE_PREFIX + phone;
    }

    public static String buildEmailCodeKey(String scene, String email) {
        return EMAIL_CODE_KEY_PREFIX + scene + ":" + email;
    }
}
