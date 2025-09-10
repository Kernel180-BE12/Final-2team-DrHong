-- 테스트 데이터 정리 스크립트
-- 외래 키 제약 조건 순서에 맞춰 정리

-- 1단계: 자식 테이블 데이터 삭제
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM template_modified_history;
DELETE FROM favorite;
DELETE FROM group_mapping;
DELETE FROM phone_book;
DELETE FROM recipient;
DELETE FROM individual_template;
DELETE FROM public_template;
DELETE FROM chat_message;
DELETE FROM chat_session;
DELETE FROM workspace;
DELETE FROM users_auth;
DELETE FROM users;
DELETE FROM email_verification;

SET FOREIGN_KEY_CHECKS = 1;