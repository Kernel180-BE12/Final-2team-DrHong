package com.jober.final2teamdrhong.util;

/**
 * 로깅 시 민감한 정보를 마스킹하는 유틸리티 클래스
 * 개인정보 보호를 위해 이메일, IP, 전화번호 등을 마스킹 처리
 */
public class LogMaskingUtil {
    
    private static final String MASK = "***";
    
    /**
     * 이메일 주소 마스킹
     * 예: user@example.com → u***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            // @ 기호가 없거나 첫 번째 문자면 전체 마스킹
            return MASK;
        }
        
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        
        if (localPart.length() <= 1) {
            return MASK + domainPart;
        }
        
        // 첫 글자만 표시하고 나머지는 마스킹
        return localPart.charAt(0) + MASK + domainPart;
    }
    
    /**
     * IP 주소 마스킹
     * 예: 192.168.1.100 → 192.168.***.***
     */
    public static String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return ipAddress;
        }
        
        // IPv4 주소인지 확인
        String[] parts = ipAddress.split("\\.");
        if (parts.length == 4) {
            // 앞의 두 옥텟만 표시
            return parts[0] + "." + parts[1] + ".***" + ".***";
        }
        
        // IPv6 또는 기타 형식은 전체 마스킹
        return "***";
    }
    
    /**
     * 전화번호 마스킹
     * 예: 010-1234-5678 → 010-***-5678
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }
        
        // 전화번호 패턴 확인 (010-1234-5678)
        if (phoneNumber.matches("\\d{3}-\\d{4}-\\d{4}")) {
            String[] parts = phoneNumber.split("-");
            return parts[0] + "-***-" + parts[2];
        }
        
        // 다른 형식은 부분 마스킹
        if (phoneNumber.length() > 4) {
            return phoneNumber.substring(0, 3) + "***" + phoneNumber.substring(phoneNumber.length() - 4);
        }
        
        return MASK;
    }
    
    /**
     * 사용자 ID 마스킹 (Long 타입)
     * 디버깅 목적으로 마지막 3자리만 표시
     */
    public static String maskUserId(Long userId) {
        if (userId == null) {
            return "null";
        }
        
        String userIdStr = userId.toString();
        if (userIdStr.length() <= 3) {
            return "***";
        }
        
        return "***" + userIdStr.substring(userIdStr.length() - 3);
    }
    
    /**
     * 토큰 마스킹 (JWT 등)
     * 처음 10자와 마지막 4자만 표시
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }
        
        if (token.length() <= 14) {
            return MASK;
        }
        
        return token.substring(0, 10) + "***" + token.substring(token.length() - 4);
    }
}