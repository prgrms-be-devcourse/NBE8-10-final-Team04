package back.domain.prompt.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DemoSkillBatchRequestDto(

        @JsonProperty("skills")
        @NotEmpty
        List<@Valid DemoSkillRequestDto> skills

) {
    public DemoSkillBatchRequestDto {
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    @Override
    public List<DemoSkillRequestDto> skills() {
        return List.copyOf(skills);
    }
}
