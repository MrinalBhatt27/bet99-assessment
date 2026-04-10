package com.bet99.bugtracker.controller;

import com.bet99.bugtracker.dto.BugResponse;
import com.bet99.bugtracker.dto.CreateBugRequest;
import com.bet99.bugtracker.model.BugStatus;
import com.bet99.bugtracker.model.Severity;
import com.bet99.bugtracker.service.BugService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BugApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BugService bugService;

    private AutoCloseable mocks;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BugApiController(bugService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // ── POST /api/bugs ────────────────────────────────────────────────────────

    @Test
    public void create_validRequest_returns200WithBody() throws Exception {
        when(bugService.create(any(CreateBugRequest.class)))
                .thenReturn(makeResponse(1L, "Login broken", Severity.HIGH));

        mockMvc.perform(post("/api/bugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBugJson("Login broken", "Cannot log in", "HIGH", "OPEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bugTitle").value("Login broken"))
                .andExpect(jsonPath("$.severity").value("HIGH"));
    }

    @Test
    public void create_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/bugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBugJson("   ", "Some description", "LOW", "OPEN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void create_blankDescription_returns400() throws Exception {
        mockMvc.perform(post("/api/bugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBugJson("Valid title", "", "MEDIUM", "OPEN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void create_missingSeverity_returns400() throws Exception {
        String json = "{\"bugTitle\":\"No severity\",\"description\":\"Missing field\",\"status\":\"OPEN\"}";
        mockMvc.perform(post("/api/bugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void create_invalidJson_returns400() throws Exception {
        mockMvc.perform(post("/api/bugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/bugs ─────────────────────────────────────────────────────────

    @Test
    public void list_noFilter_returnsAllBugs() throws Exception {
        when(bugService.list(Optional.empty())).thenReturn(Arrays.asList(
                makeResponse(1L, "Bug A", Severity.HIGH),
                makeResponse(2L, "Bug B", Severity.LOW)
        ));

        mockMvc.perform(get("/api/bugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bugTitle").value("Bug A"))
                .andExpect(jsonPath("$[1].bugTitle").value("Bug B"));
    }

    @Test
    public void list_noFilter_returnsEmptyArrayWhenNoBugs() throws Exception {
        when(bugService.list(Optional.empty())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/bugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void list_withSeverityFilter_returnsFilteredBugs() throws Exception {
        when(bugService.list(Optional.of(Severity.CRITICAL))).thenReturn(Collections.singletonList(
                makeResponse(1L, "Critical bug", Severity.CRITICAL)
        ));

        mockMvc.perform(get("/api/bugs").param("severity", "CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));
    }

    @Test
    public void list_withInvalidSeverity_returns400WithMessage() throws Exception {
        mockMvc.perform(get("/api/bugs").param("severity", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private BugResponse makeResponse(Long id, String title, Severity severity) {
        BugResponse r = new BugResponse();
        r.setId(id);
        r.setBugTitle(title);
        r.setDescription("Some description");
        r.setSeverity(severity);
        r.setStatus(BugStatus.OPEN);
        r.setCreatedAt("2026-04-09T00:00:00");
        return r;
    }

    private String validBugJson(String title, String description, String severity, String status) {
        return String.format(
                "{\"bugTitle\":\"%s\",\"description\":\"%s\",\"severity\":\"%s\",\"status\":\"%s\"}",
                title, description, severity, status);
    }
}
