package com.jober.final2teamdrhong.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", columnDefinition = "TIMESTAMP", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 엔티티의 수정 시각(updatedAt)을 현재 시간으로 갱신합니다.
     * JPA의 Dirty Checking에 의해 DB에 반영됩니다.
     * 수정 시간은 Asia/Seoul 시간대를 기준으로 기록됩니다.
     */
    public void update() {
        // Java의 ZoneId를 사용하여 서울 시간대의 현재 시간을 가져옵니다.
        this.updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }

    /**
     * 엔티티를 소프트 삭제 상태로 변경합니다.
     * 삭제 시간은 Asia/Seoul 시간대를 기준으로 기록됩니다.
     */
    public void softDelete() {
        this.isDeleted = true;
        // Java의 ZoneId를 사용하여 서울 시간대의 현재 시간을 가져옵니다.
        this.updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDateTime();
        this.deletedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }

    /**
     * JPA 엔티티가 저장(@PrePersist)되거나 업데이트(@PreUpdate)되기 직전에 호출되어,
     * 영속화에 필요한 사전 처리 작업을 수행합니다.
     * <p>
     * - 모든 String 타입 필드의 앞뒤 공백을 제거(trim)합니다.
     * <p>
     * - 생성 및 수정 시각(LocalDateTime)의 정밀도를 DB와 맞추기 위해 초(second) 단위에서 잘라냅니다.
     */
    @PrePersist
    @PreUpdate
    public void onPrePersistOrUpdate() {
        // 1. 시간 정밀도 맞추기: AuditingEntityListener가 값을 설정한 후, DB에 저장되기 전에 정밀도를 조정합니다.
        if (this.createdAt != null) {
            this.createdAt = this.createdAt.truncatedTo(ChronoUnit.SECONDS);
        }
        // updatedAt은 AuditingEntityListener 또는 엔티티의 update/softDelete 메서드에 의해 항상 갱신됩니다.
        if (this.updatedAt != null) {
            this.updatedAt = this.updatedAt.truncatedTo(ChronoUnit.SECONDS);
        }
        if (this.deletedAt != null) {
            this.deletedAt = this.deletedAt.truncatedTo(ChronoUnit.SECONDS);
        }

        // 2. 리플렉션을 사용하여 엔티티 내의 모든 String 필드의 공백을 제거합니다.
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType().equals(String.class)) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    if (value != null) {
                        field.set(this, ((String) value).trim());
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to trim string fields", e);
                }
            }
        }
    }
}
