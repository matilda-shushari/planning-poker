package com.lufthansa.planning_poker.room.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStoryRequest(
    @NotBlank(message = "Story title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    String title,

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description,

    @Size(max = 500, message = "JIRA link cannot exceed 500 characters")
    String jiraLink
) {}

