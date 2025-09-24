package com.jober.final2teamdrhong.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "favorite", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"workspace_id", "public_template_id", "individual_template_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@SQLRestriction("is_deleted = false")
public class Favorite extends BaseEntity {

    public enum TemplateType {
        INDIVIDUAL,
        PUBLIC
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Integer favoriteId;

    // ===== 관계 필드 =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_template_id")
    private PublicTemplate publicTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "individual_template_id")
    private IndividualTemplate individualTemplate;
}
