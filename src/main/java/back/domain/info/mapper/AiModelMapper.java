package back.domain.info.mapper;

import back.domain.info.dto.FamilyDto;
import back.domain.info.dto.ModelDto;
import back.domain.info.dto.VendorDto;
import back.domain.info.entity.AiModel;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;

@Component
public class AiModelMapper {
    public AiVendor toVendorEntity(VendorDto dto) {
        AiVendor vendor = AiVendor.builder()
                .name(dto.getName())
                .officialUrl(dto.getOfficialUrl())
                .isActive(dto.getIsActive())
                .isDeprecated(dto.getIsDeprecated())
                .modelFamilies(new ArrayList<>())
                .build();

        if (dto.getFamilies() != null) {
            dto.getFamilies().stream()
                    .map(f -> toFamilyEntity(f, vendor))
                    .forEach(vendor.getModelFamilies()::add);
        }

        return vendor;
    }

    public AiModelFamily toFamilyEntity(FamilyDto dto, AiVendor vendor) {
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName(dto.getFamilyName())
                .commonDescription(dto.getCommonDescription())
                .models(new ArrayList<>())
                .build();

        if (dto.getModels() != null) {
            dto.getModels().stream()
                    .map(m -> toModelEntity(m, family))
                    .forEach(family.getModels()::add);
        }

        return family;
    }

    public AiModel toModelEntity(ModelDto dto, AiModelFamily family) {
        return AiModel.builder()
                .family(family)
                .modelName(dto.getModelName())
                .apiId(dto.getApiId())
                .contextWindow(dto.getContextWindow())
                .maxOutputTokens(dto.getMaxOutputTokens())
                .releaseDate(dto.getReleaseDate())
                .isPreview(dto.getIsPreview())
                .modelImageUrl(dto.getModelImageUrl())
                .inputPrice(dto.getInputPrice())
                .outputPrice(dto.getOutputPrice())
                .inputModalities(dto.getInputModalities() != null ? dto.getInputModalities() : new ArrayList<>())
                .outputModalities(dto.getOutputModalities() != null ? dto.getOutputModalities() : new ArrayList<>())
                .build();
    }
}
