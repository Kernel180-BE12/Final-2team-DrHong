package com.jober.final2teamdrhong.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "individual_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 생성

public class IndividualTemplate {
    public enum Status {
        DRAFT, PENDING, APPROVED, REJECTED
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "individual_template_id")
    private Integer individualTemplateId;

    @Column(name = "individual_template_title")
    private String individualTemplateTitle;

    @Column(name = "individual_template_content", columnDefinition = "TEXT")
    private String individualTemplateContent;

    @Column(name = "button_title", length = 50)
    private String buttonTitle;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", columnDefinition = "DATETIME")
    private LocalDateTime deletedAt;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TemplateModifiedHistory.Status status = TemplateModifiedHistory.Status.DRAFT;


    @ManyToOne
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @OneToMany(mappedBy = "individualTemplate")
    private List<TemplateModifiedHistory> histories;

    @OneToOne(mappedBy = "individualTemplate")
    private Favorite favorite;

    @Builder
    public IndividualTemplate(Workspace workspaceId,
                              String individualTemplateTitle,
                              String individualTemplateContent,
                              String buttonTitle){
        this.workspace = workspaceId;
        this.individualTemplateTitle = individualTemplateTitle;
        this.individualTemplateContent = individualTemplateContent;
        this.buttonTitle = buttonTitle;
    }
}