package back.domain.info.mapper;

import back.domain.info.dto.data.FamilyDto;
import back.domain.info.dto.data.VendorDto;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;

@Component
public class AiModelMapper {
    public AiVendor toVendorEntity(VendorDto dto) {
        AiVendor vendor = AiVendor.builder()
                .name(dto.name())
                .officialUrl(dto.officialUrl())
                .isActive(dto.isActive())
                .isDeprecated(dto.isDeprecated())
                .modelFamilies(new ArrayList<>())
                .build();

        if (dto.families() != null) {
            dto.families().stream()
                    .map(f -> toFamilyEntity(f, vendor))
                    .forEach(vendor.getModelFamilies()::add);
        }

        return vendor;
    }

    public AiModelFamily toFamilyEntity(FamilyDto dto, AiVendor vendor) {
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName(dto.familyName())
                .commonDescription(dto.commonDescription())
                .build();

        return family;
    }

}
