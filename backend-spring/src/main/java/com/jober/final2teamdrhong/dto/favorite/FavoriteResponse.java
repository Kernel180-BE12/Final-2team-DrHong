package com.jober.final2teamdrhong.dto.favorite;

import com.jober.final2teamdrhong.entity.Favorite;
import com.jober.final2teamdrhong.entity.IndividualTemplate;
import com.jober.final2teamdrhong.entity.PublicTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FavoriteResponse {

    private final Integer favoriteId;
    private final String templateType;
    private final Integer templateId;
    private final String templateTitle;
    private final String templateContent;
    private final String buttonTitle;
    private final Integer viewCount;
    private final Integer shareCount;

    public static FavoriteResponse fromPublicTemplate(Favorite favorite) {
        PublicTemplate pt = favorite.getPublicTemplate();
        if (pt == null) {
            throw new IllegalArgumentException("Favorite does not refer to a PublicTemplate.");
        }
        return FavoriteResponse.builder()
                .favoriteId(favorite.getFavoriteId())
                .templateType("PUBLIC")
                .templateId(pt.getPublicTemplateId())
                .templateTitle(pt.getPublicTemplateTitle())
                .templateContent(pt.getPublicTemplateContent())
                .buttonTitle(pt.getButtonTitle())
                .viewCount(pt.getViewCount())
                .shareCount(pt.getShareCount())
                .build();
    }

    public static FavoriteResponse fromIndividualTemplate(Favorite favorite) {
        IndividualTemplate it = favorite.getIndividualTemplate();
        if (it == null) {
            throw new IllegalArgumentException("Favorite does not refer to an IndividualTemplate.");
        }
        return FavoriteResponse.builder()
                .favoriteId(favorite.getFavoriteId())
                .templateType("INDIVIDUAL")
                .templateId(it.getIndividualTemplateId())
                .templateTitle(it.getIndividualTemplateTitle())
                .templateContent(it.getIndividualTemplateContent())
                .buttonTitle(it.getButtonTitle())
                .build();
    }

    public static FavoriteResponse convertToFavoriteResponse(Favorite favorite) {
        if (favorite.getPublicTemplate() != null) {
            return FavoriteResponse.fromPublicTemplate(favorite);
        }
        return FavoriteResponse.fromIndividualTemplate(favorite);
    }
}
