package com.nqt.jobPost_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqt.jobPost_service.dto.response.NormalizedJobText;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiService {
    ChatClient chatClient;
    ObjectMapper objectMapper;

    public AiService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    public NormalizedJobText normalize(String title, String description) {
        String response = chatClient.prompt()
                .user("""
                You are a job post text improver.

                Your task:
                - Improve the job post title and description.
                - Make the title clearer, more professional, and more attractive.
                - Rewrite the description to be clearer and easier to understand.
                - Correct spelling and grammar mistakes.
                - Fix obvious technology names.
                - Keep the original meaning.
                - Do not add new skills or technologies that are not mentioned by the user.
                - Do not remove important information.
                - Return only valid JSON.
                - Do not include markdown.

                The JSON format must be:
                {
                  "title": "...",
                  "description": "..."
                }

                Example:
                Input title: Bulid Reac app
                Input description: Need Sping boot, Postgress and authencation

                Output:
                {
                  "title": "Build a React Application",
                  "description": "We are looking for a developer to build a React application with Spring Boot, PostgreSQL, and authentication features."
                }

                Input title:
                %s

                Input description:
                %s
                """.formatted(title, description))
                .call()
                .content();

        try {
            return objectMapper.readValue(response, NormalizedJobText.class);
        } catch (JsonProcessingException e) {
            return new NormalizedJobText(title, description);
        }
    }
}
