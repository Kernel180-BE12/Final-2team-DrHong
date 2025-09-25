package com.jober.final2teamdrhong.dto.individualtemplate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualTemplateUpdateRequest {
    private String individualTemplateTitle;
    private String individualTemplateContent;
    private String buttonTitle;
}
