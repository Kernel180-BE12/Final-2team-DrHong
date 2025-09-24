package com.jober.final2teamdrhong.dto.individualtemplate;

import com.jober.final2teamdrhong.entity.IndividualTemplate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualTemplateResponse {
    private Integer individualTemplateId;
    private String individualTemplateTitle;     // null 가능
    private String individualTemplateContent;   // null 가능
    private String buttonTitle;                 // null 가능
    private Integer workspaceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private IndividualTemplate.Status status;


    public static IndividualTemplateResponse toResponse(IndividualTemplate entity) {
        return new IndividualTemplateResponse(
                entity.getIndividualTemplateId(),
                entity.getIndividualTemplateTitle(),
                entity.getIndividualTemplateContent(),
                entity.getButtonTitle(),
                entity.getWorkspace().getWorkspaceId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getIsDeleted(),
                entity.getStatus()
        );
    }
}
