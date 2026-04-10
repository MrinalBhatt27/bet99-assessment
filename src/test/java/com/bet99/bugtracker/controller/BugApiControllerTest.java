package com.bet99.bugtracker.controller;

import com.bet99.bugtracker.dto.BugResponse;
import com.bet99.bugtracker.dto.CreateBugRequest;
import com.bet99.bugtracker.dto.UpdateBugRequest;
import com.bet99.bugtracker.exception.BugNotFoundException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .thenReturn(makeResponse(1L, "Login broken", Severity.HIGH, BugStatus.OPEN));

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
    public void create_titleTooLong_returns400() throws Exception {
        String longTitle = new String(new char[256]).replace('\0', 'A');
        mockMvc.perform(post("/api/bugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBugJson(longTitle, "Some description", "LOW", "OPEN")))
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
        when(bugService.list(Optional.empty(), Optional.empty())).thenReturn(Arrays.asList(
                makeResponse(1L, "Bug A", Severity.HIGH, BugStatus.OPEN),
                makeResponse(2L, "Bug B", Severity.LOW, BugStatus.RESOLVED)
        ));

        mockMvc.perform(get("/api/bugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bugTitle").value("Bug A"))
                .andExpect(jsonPath("$[1].bugTitle").value("Bug B"));
    }

    @Test
    public void list_noFilter_returnsEmptyArrayWhenNoBugs() throws Exception {
        when(bugService.list(Optional.empty(), Optional.empty())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/bugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void list_withSeverityFilter_returnsFilteredBugs() throws Exception {
        when(bugService.list(Optional.of(Severity.CRITICAL), Optional.empty())).thenReturn(Collections.singletonList(
                makeResponse(1L, "Critical bug", Severity.CRITICAL, BugStatus.OPEN)
        ));

        mockMvc.perform(get("/api/bugs").param("severity", "CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));
    }

    @Test
    public void list_withStatusFilter_returnsFilteredBugs() throws Exception {
        when(bugService.list(Optional.empty(), Optional.of(BugStatus.OPEN))).thenReturn(Collections.singletonList(
                makeResponse(1L, "Open bug", Severity.HIGH, BugStatus.OPEN)
        ));

        mockMvc.perform(get("/api/bugs").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    public void list_withBothFilters_returnsFilteredBugs() throws Exception {
        when(bugService.list(Optional.of(Severity.HIGH), Optional.of(BugStatus.OPEN))).thenReturn(Collections.singletonList(
                makeResponse(1L, "High open bug", Severity.HIGH, BugStatus.OPEN)
        ));

        mockMvc.perform(get("/api/bugs").param("severity", "HIGH").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    public void list_withInvalidSeverity_returns400WithMessage() throws Exception {
        mockMvc.perform(get("/api/bugs").param("severity", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void list_withInvalidStatus_returns400WithMessage() throws Exception {
        mockMvc.perform(get("/api/bugs").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── PATCH /api/bugs/{id}/status ───────────────────────────────────────────

    @Test
    public void updateStatus_validRequest_returns200() throws Exception {
        BugResponse updated = makeResponse(1L, "Login broken", Severity.HIGH, BugStatus.RESOLVED);
        when(bugService.updateStatus(eq(1L), eq(BugStatus.RESOLVED))).thenReturn(updated);

        mockMvc.perform(patch("/api/bugs/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    public void updateStatus_bugNotFound_returns404() throws Exception {
        when(bugService.updateStatus(eq(99L), any(BugStatus.class)))
                .thenThrow(new BugNotFoundException(99L));

        mockMvc.perform(patch("/api/bugs/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void updateStatus_missingStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/bugs/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void updateStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/bugs/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/bugs/{id} ───────────────────────────────────────────────────

    @Test
    public void update_validRequest_returns200() throws Exception {
        BugResponse updated = makeResponse(1L, "Updated title", Severity.CRITICAL, BugStatus.IN_PROGRESS);
        when(bugService.update(eq(1L), any(UpdateBugRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/bugs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBugJson("Updated title", "Updated desc", "CRITICAL", "IN_PROGRESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bugTitle").value("Updated title"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    public void update_blankTitle_returns400() throws Exception {
        mockMvc.perform(put("/api/bugs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBugJson("", "Some description", "LOW", "OPEN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void update_bugNotFound_returns404() throws Exception {
        when(bugService.update(eq(99L), any(UpdateBugRequest.class)))
                .thenThrow(new BugNotFoundException(99L));

        mockMvc.perform(put("/api/bugs/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBugJson("Title", "Desc", "LOW", "OPEN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── DELETE /api/bugs/{id} ─────────────────────────────────────────────────

    @Test
    public void delete_existingBug_returns204() throws Exception {
        doNothing().when(bugService).delete(1L);

        mockMvc.perform(delete("/api/bugs/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void delete_notFound_returns404() throws Exception {
        doThrow(new BugNotFoundException(99L)).when(bugService).delete(99L);

        mockMvc.perform(delete("/api/bugs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private BugResponse makeResponse(Long id, String title, Severity severity, BugStatus status) {
        BugResponse r = new BugResponse();
        r.setId(id);
        r.setBugTitle(title);
        r.setDescription("Some description");
        r.setSeverity(severity);
        r.setStatus(status);
        r.setCreatedAt("2026-04-09T00:00:00Z");
        return r;
    }

    private String validBugJson(String title, String description, String severity, String status) {
        return String.format(
                "{\"bugTitle\":\"%s\",\"description\":\"%s\",\"severity\":\"%s\",\"status\":\"%s\"}",
                title, description, severity, status);
    }
}
