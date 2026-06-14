package com.nqt.identity_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.common_starter.util.ExcelUtil;
import com.nqt.common_starter.util.PageUtil;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.dto.request.skill.SkillRequest;
import com.nqt.identity_service.dto.request.skill.SkillSuggestionRequest;
import com.nqt.identity_service.dto.response.skill.SkillResponse;
import com.nqt.identity_service.dto.response.skill.SkillSuggestionResult;
import com.nqt.identity_service.entity.Skill;
import com.nqt.identity_service.entity.User;
import com.nqt.identity_service.mapper.SkillMapper;
import com.nqt.identity_service.repository.SkillRepository;
import com.nqt.identity_service.repository.UserRepository;
import com.nqt.identity_service.service.impl.ISkill;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SkillService implements ISkill {
    SkillRepository skillRepository;
    SkillMapper skillMapper;
    UserRepository userRepository;
    VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    ChatClient chatClient;
    @Override
    public SkillResponse createSkill(SkillRequest request) {
        //check validation

        if (skillRepository.existsBySkillNameIgnoreCase(request.getSkillName())) {
            throw new GlobalException(ErrorCode.IS_EXISTED.getMessage());
        }
        Skill skill = skillMapper.toEntity(request);
        Skill entity = skillRepository.save(skill);
        //lưu skill vào vectorDB
        saveSkillToVectorDb(entity);
        return skillMapper.toResponse(entity);
    }


    private Skill mapRowToSkill(
            Row row,
            DataFormatter formatter,
            Map<String, Integer> headerMap
    ) {
        String skillName = ExcelUtil.getCellValue(row, formatter, headerMap, "skillName");
        String skillDescription = ExcelUtil.getCellValue(row, formatter, headerMap, "skillDescription");

        if (skillName.isBlank()) {
            return null;
        }

        return Skill.builder()
                .skillName(skillName)
                .skillDescription(skillDescription)
                .isDeleted(false)
                .build();
    }

    @Override
    public void createSkillsByFile(MultipartFile file) {
        List<Skill> skillsFromFile = ExcelUtil.readExcel(file, this::mapRowToSkill);

        Set<String> skillNamesInFile = new HashSet<>();
        List<Skill> skills = new ArrayList<>();

        for (Skill skill : skillsFromFile) {
            String normalizedSkillName = ExcelUtil.normalizeText(skill.getSkillName());

            if (!skillNamesInFile.add(normalizedSkillName)) {
                continue;
            }

            if (skillRepository.existsBySkillNameIgnoreCase(skill.getSkillName())) {
                continue;
            }

            skills.add(skill);
        }
        List<Skill> entities = skillRepository.saveAll(skills);
        entities.forEach(this::saveSkillToVectorDb);
    }
    @Override
    public PageResponse<SkillResponse> getAllSkills(PageRequest pageRequest) {
        Pageable pageable = PageUtil.createPageable(pageRequest);
        Page<Skill> skillPage =skillRepository.findBySkillNameContainingIgnoreCaseAndDeletedFalse(pageRequest.getSearch(), pageable); // Lấy trang đầu tiên với kích thước 10

        return PageResponse.fromPage(skillPage.map(skillMapper::toResponse));
    }

    @Override
    public PageResponse<SkillResponse> getAllSkillsForAdmin(PageRequest pageRequest) {
        Pageable pageable = PageUtil.createPageable(pageRequest);
        Page<Skill> skillPage = skillRepository.findBySkillNameContainingIgnoreCase(pageRequest.getSearch(),pageable); // Lấy trang đầu tiên với kích thước 10

        return PageResponse.fromPage(skillPage.map(skillMapper::toResponse));
    }

    @Override
    public SkillResponse getSkillById(long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND.getCode(),ErrorCode.NOT_FOUND.getMessage()));
        return skillMapper.toResponse(skill);
    }

    @Override
    public SkillResponse updateSkill(long id, SkillRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND.getCode(),ErrorCode.NOT_FOUND.getMessage()));

        skillMapper.toUpdate(skill, request);

        return skillMapper.toResponse(skillRepository.save(skill));
    }

    @Override
    public void toggleSkill(long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND.getCode(),ErrorCode.NOT_FOUND.getMessage()));

        skill.setDeleted(!skill.isDeleted());
        skillRepository.save(skill);
    }

    @Override
    public List<SkillResponse> getAllSkillsByExpertCode(String expertCode) {
        User expert = userRepository.findByCode(expertCode)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                ));
        if(expert.getRole() != Role.EXPERT) {
            throw new GlobalException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    ErrorCode.UNAUTHORIZED.getMessage()
            );
        }

        return expert.getExpertProfile().getSkills()
                .stream()
                .filter(skill -> !skill.isDeleted())
                .map(skillMapper::toResponse)
                .toList();
    }

    @Override
    public SkillSuggestionResult suggestSkills(SkillSuggestionRequest request) {
        String title = request.getTitle();
        String description = request.getDescription();

        String query = """
                Job title: %s
                Job description: %s
                """.formatted(title, description);

        var relatedSkillDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(10)
                        .build()
        );

        String skillContext = relatedSkillDocs.stream()
                .map(this::toSkillContext)
                .collect(Collectors.joining(",\n"));

        String response = chatClient.prompt()
                .user("""
                        You are a job post skill suggestion assistant.

                        Your task:
                        - Analyze the job title and description.
                        - Choose suitable skills ONLY from the retrieved skill list.
                        - Return only valid JSON.
                        - Do not explain anything.
                        - Do not invent skill ids.
                        - Only use ids from the retrieved skill list.

                        Rules:
                        - requiredSkillIds: skills clearly necessary for this job.
                        - preferredSkillIds: skills useful but not mandatory.
                        - If unsure, put the skill in preferredSkillIds.
                        - If no suitable skill exists, return empty arrays.

                        Output format:
                        {
                          "requiredSkillIds": [],
                          "preferredSkillIds": []
                        }

                        Retrieved skill list:
                        [
                        %s
                        ]

                        Job title:
                        %s

                        Job description:
                        %s
                        """.formatted(skillContext, title, description))
                .call()
                .content();

        try {
            String cleanJson = cleanJson(response);
            return objectMapper.readValue(cleanJson, SkillSuggestionResult.class);
        } catch (JsonProcessingException e) {
            return SkillSuggestionResult.builder()
                    .requiredSkillIds(Set.of())
                    .preferredSkillIds(Set.of())
                    .build();
        }
    }

    private String toSkillContext(Document document) {
        return """
                {
                  "id": %s,
                  "name": "%s",
                  "content": "%s"
                }
                """.formatted(
                document.getMetadata().get("skillId"),
                document.getMetadata().get("skillName"),
                document.getText().replace("\"", "'")
        );
    }

    private String cleanJson(String response) {
        return response
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private void saveSkillToVectorDb(Skill skill) {
        String skillText = buildSkillText(skill);
        vectorStore.add(List.of(
                new Document(
                        skillText,
                        // Metadata: thông tin phụ đi kèm document
                        java.util.Map.of(
                                "skillId", String.valueOf(skill.getId()),
                                "skillName", skill.getSkillName()
                        )
                )
        ));
    }

    private String buildSkillText(Skill skill) {
        return """
                Skill name: %s
                Description: %s
                """.formatted(
                skill.getSkillName(),
                skill.getSkillDescription()
        );
    }
}
