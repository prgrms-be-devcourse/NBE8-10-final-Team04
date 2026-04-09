package back.domain.prompt.prompt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import back.domain.prompt.prompt.dto.SkillContentDetail;
import back.domain.prompt.prompt.dto.SkillDetailDto;
import back.domain.prompt.prompt.dto.SkillListItemDto;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;
import back.domain.prompt.prompt.repository.SkillRepository;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SkillReadServiceImplTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillReadServiceImpl skillReadService;

    @Test
    @DisplayName("getSkillContent는 skill과 repository 정보를 매핑해 반환한다")
    void getSkillContent_returnsMappedContent() {
        Skill skill = skill(11L, "alpha", Category.BACKEND, "alpha content", Set.of("java"));
        when(skillRepository.findByIdWithRepository(11L)).thenReturn(Optional.of(skill));

        SkillContentDetail result = skillReadService.getSkillContent(11L);

        assertThat(result.skillId()).isEqualTo(11L);
        assertThat(result.category()).isEqualTo("backend");
        assertThat(result.sourceRepo()).isEqualTo("owner/repo");
        assertThat(result.contentMd()).isEqualTo("alpha content");
    }

    @Test
    @DisplayName("getSkillContent는 skill이 없으면 NOT_FOUND 예외를 던진다")
    void getSkillContent_throwsWhenSkillNotFound() {
        when(skillRepository.findByIdWithRepository(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillReadService.getSkillContent(99L))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("getSkills는 category가 null이면 전체 조회를 사용한다")
    void getSkills_usesFindAllWhenCategoryIsNull() {
        Pageable pageable = PageRequest.of(0, 5);
        when(skillRepository.findAllWithRepository(pageable)).thenReturn(new PageImpl<>(List.of(
                skill(1L, "alpha", Category.BACKEND, "A", Set.of("java")),
                skill(2L, "beta", Category.FRONTEND, "B", Set.of("react"))
        ), pageable, 2));

        Page<SkillListItemDto> result = skillReadService.getSkills(pageable, null);

        verify(skillRepository).findAllWithRepository(pageable);
        assertThat(result.getContent()).extracting(SkillListItemDto::id).containsExactly(1L, 2L);
        assertThat(result.getContent()).extracting(SkillListItemDto::category).containsExactly("BACKEND", "FRONTEND");
    }

    @Test
    @DisplayName("getSkills는 category가 있으면 카테고리 조회를 사용한다")
    void getSkills_usesFindByCategoryWhenCategoryProvided() {
        Pageable pageable = PageRequest.of(0, 5);
        when(skillRepository.findByCategoryWithRepository(Category.BACKEND, pageable))
                .thenReturn(new PageImpl<>(List.of(
                        skill(3L, "gamma", Category.BACKEND, "G", Set.of("spring"))
                ), pageable, 1));

        Page<SkillListItemDto> result = skillReadService.getSkills(pageable, Category.BACKEND);

        verify(skillRepository).findByCategoryWithRepository(Category.BACKEND, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("gamma");
    }

    @Test
    @DisplayName("searchSkills는 keyword가 비어 있으면 전체 조회를 사용한다")
    void searchSkills_returnsAllWhenKeywordBlank() {
        Pageable pageable = PageRequest.of(0, 5);
        when(skillRepository.findAllWithRepository(pageable)).thenReturn(new PageImpl<>(List.of(
                skill(10L, "alpha", Category.BACKEND, "A", Set.of("java"))
        ), pageable, 1));

        Page<SkillListItemDto> result = skillReadService.searchSkills("   ", pageable);

        verify(skillRepository).findAllWithRepository(pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("searchSkills는 검색 결과 id가 없으면 빈 페이지를 반환한다")
    void searchSkills_returnsEmptyPageWhenNoIds() {
        Pageable pageable = PageRequest.of(0, 2);
        when(skillRepository.searchIdsByKeyword("%spring%")).thenReturn(List.of());

        Page<SkillListItemDto> result = skillReadService.searchSkills("spring", pageable);

        assertThat(result.getContent()).isEmpty();
        verify(skillRepository, never()).findAllByIdsWithRepository(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("searchSkills는 검색 id 순서를 유지하고 수동 페이징한다")
    void searchSkills_keepsSearchOrderAndPaginates() {
        Pageable pageable = PageRequest.of(1, 2); // offset=2
        when(skillRepository.searchIdsByKeyword("%keyword%")).thenReturn(List.of(5L, 3L, 7L, 2L));
        when(skillRepository.findAllByIdsWithRepository(List.of(7L, 2L))).thenReturn(List.of(
                skill(2L, "two", Category.FRONTEND, "C2", Set.of("tag2")),
                skill(7L, "seven", Category.BACKEND, "C7", Set.of("tag7"))
        ));

        Page<SkillListItemDto> result = skillReadService.searchSkills(" keyword ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent()).extracting(SkillListItemDto::id).containsExactly(7L, 2L);
        assertThat(result.getContent()).extracting(SkillListItemDto::name).containsExactly("seven", "two");
    }

    @Test
    @DisplayName("getSkillDetail은 SkillDetailDto로 매핑한다")
    void getSkillDetail_returnsMappedDto() {
        Skill skill = skill(21L, "delta", Category.SECURITY, "secure", Set.of("owasp"));
        when(skillRepository.findByIdWithRepository(21L)).thenReturn(Optional.of(skill));

        SkillDetailDto result = skillReadService.getSkillDetail(21L);

        assertThat(result.id()).isEqualTo(21L);
        assertThat(result.name()).isEqualTo("delta");
        assertThat(result.category()).isEqualTo("SECURITY");
        assertThat(result.repositoryName()).isEqualTo("demo-repo");
    }

    @Test
    @DisplayName("getSkillDetail은 skill이 없으면 NOT_FOUND 예외를 던진다")
    void getSkillDetail_throwsWhenSkillNotFound() {
        when(skillRepository.findByIdWithRepository(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillReadService.getSkillDetail(404L))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.NOT_FOUND);
    }

    private Skill skill(Long id, String name, Category category, String contentMd, Set<String> tags) {
        Skill skill = Skill.builder()
                .repository(repository())
                .name(name)
                .contentMd(contentMd)
                .contentHash(name + "-hash")
                .filePath("skills/" + name + ".md")
                .category(category)
                .tagsJson(tags)
                .build();
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }

    private Repository repository() {
        Repository repository = Repository.builder()
                .githubId(100L)
                .name("demo-repo")
                .sourceRepo("owner/repo")
                .sourceUri("https://example.com/owner/repo")
                .summary("demo summary")
                .starCount(10)
                .forkCount(3)
                .size(50)
                .languageStats(Map.of("Java", 90))
                .license("MIT")
                .homepage("https://example.com")
                .ownerAvatarUrl("https://example.com/avatar.png")
                .ownerType(OwnerType.USER)
                .isOfficial(true)
                .defaultBranch("main")
                .etag("etag-1")
                .sourceUpdatedAt(LocalDateTime.parse("2026-03-26T00:00:00"))
                .active(true)
                .rawMetadata(Map.of("category", "demo"))
                .build();
        ReflectionTestUtils.setField(repository, "id", 1000L);
        return repository;
    }
}
