package back.domain.info.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.info.dto.response.FamilyDetailResponse;
import back.domain.info.dto.response.FamilySummaryResponse;
import back.domain.info.dto.response.VendorInfoResponse;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.AiVendorRepository;
import back.global.exception.ServiceException;

class InfoCatalogQueryServiceImplTest {

    private AiVendorRepository aiVendorRepository;
    private AiModelFamilyRepository aiModelFamilyRepository;
    private InfoCatalogQueryService infoCatalogQueryService;

    @BeforeEach
    void setUp() {
        aiVendorRepository = mock(AiVendorRepository.class);
        aiModelFamilyRepository = mock(AiModelFamilyRepository.class);
        infoCatalogQueryService = new InfoCatalogQueryServiceImpl(aiVendorRepository, aiModelFamilyRepository);
    }

    @Test
    void getVendors_returnsMappedResult() {
        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://openai.com")
                .isActive(true)
                .isDeprecated(false)
                .build();
        ReflectionTestUtils.setField(vendor, "id", 1L);
        when(aiVendorRepository.findAllByOrderByNameAsc()).thenReturn(List.of(vendor));

        List<VendorInfoResponse> result = infoCatalogQueryService.getVendors();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.getFirst().name()).isEqualTo("OpenAI");
    }

    @Test
    void getFamiliesByVendorId_returnsMappedResult() {
        AiVendor vendor = AiVendor.builder().name("OpenAI").isActive(true).isDeprecated(false).build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT-5.4")
                .commonDescription("desc")
                .build();
        ReflectionTestUtils.setField(family, "id", 10L);
        when(aiModelFamilyRepository.findAllByVendorIdOrderByFamilyNameAsc(1L)).thenReturn(List.of(family));

        List<FamilySummaryResponse> result = infoCatalogQueryService.getFamiliesByVendorId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(10L);
        assertThat(result.getFirst().familyName()).isEqualTo("GPT-5.4");
    }

    @Test
    void getFamilyDetail_returnsMappedResult() {
        AiVendor vendor = AiVendor.builder().name("OpenAI").isActive(true).isDeprecated(false).build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT-5.4")
                .commonDescription("desc")
                .inputTypes(new String[] {"text"})
                .outputTypes(new String[] {"text"})
                .build();
        ReflectionTestUtils.setField(family, "id", 20L);
        when(aiModelFamilyRepository.findWithVendorById(20L)).thenReturn(Optional.of(family));

        FamilyDetailResponse result = infoCatalogQueryService.getFamilyDetail(20L);

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.vendorName()).isEqualTo("OpenAI");
        assertThat(result.familyName()).isEqualTo("GPT-5.4");
        assertThat(result.inputTypes()).containsExactly("text");
        assertThat(result.outputTypes()).containsExactly("text");
    }

    @Test
    void getFamilyDetail_whenNotFound_throwsServiceException() {
        when(aiModelFamilyRepository.findWithVendorById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> infoCatalogQueryService.getFamilyDetail(999L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("getFamilyDetail");
    }
}
