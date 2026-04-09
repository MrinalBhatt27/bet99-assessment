package com.bet99.bugtracker.service;

import com.bet99.bugtracker.dto.BugResponse;
import com.bet99.bugtracker.dto.CreateBugRequest;
import com.bet99.bugtracker.model.Bug;
import com.bet99.bugtracker.model.BugStatus;
import com.bet99.bugtracker.model.Severity;
import com.bet99.bugtracker.repository.BugRepository;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BugServiceImplTest {

    @Test
    public void create_mapsAndSavesBug() {
        BugRepository repo = mock(BugRepository.class);

        when(repo.save(any(Bug.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BugServiceImpl service = new BugServiceImpl(repo);

        CreateBugRequest req = new CreateBugRequest();
        req.setBugTitle("Broken link");
        req.setDescription("Home page link is broken");
        req.setSeverity(Severity.HIGH);
        req.setStatus(BugStatus.OPEN);

        service.create(req);

        ArgumentCaptor<Bug> captor = ArgumentCaptor.forClass(Bug.class);
        verify(repo).save(captor.capture());

        Bug bug = captor.getValue();
        assertEquals("Broken link", bug.getBugTitle());
        assertEquals("Home page link is broken", bug.getDescription());
        assertEquals(Severity.HIGH, bug.getSeverity());
        assertEquals(BugStatus.OPEN, bug.getStatus());
        assertNotNull(bug.getCreatedAt());
    }

    @Test
    public void list_withNoFilter_returnsAllBugs() {
        BugRepository repo = mock(BugRepository.class);

        Bug b1 = makeBug("Login broken", "Cannot log in", Severity.HIGH, BugStatus.OPEN);
        Bug b2 = makeBug("Typo on homepage", "Missing word", Severity.LOW, BugStatus.RESOLVED);
        when(repo.findAll()).thenReturn(Arrays.asList(b1, b2));

        BugServiceImpl service = new BugServiceImpl(repo);

        List<BugResponse> result = service.list(Optional.empty());

        assertEquals(2, result.size());
        assertEquals("Login broken", result.get(0).getBugTitle());
        assertEquals(Severity.HIGH, result.get(0).getSeverity());
        assertEquals("Typo on homepage", result.get(1).getBugTitle());
        assertEquals(Severity.LOW, result.get(1).getSeverity());
        verify(repo).findAll();
    }

    @Test
    public void list_withSeverityFilter_delegatesToFindBySeverity() {
        BugRepository repo = mock(BugRepository.class);

        Bug b = makeBug("Critical crash", "App crashes on load", Severity.CRITICAL, BugStatus.OPEN);
        when(repo.findBySeverity(Severity.CRITICAL)).thenReturn(Collections.singletonList(b));

        BugServiceImpl service = new BugServiceImpl(repo);

        List<BugResponse> result = service.list(Optional.of(Severity.CRITICAL));

        assertEquals(1, result.size());
        assertEquals(Severity.CRITICAL, result.get(0).getSeverity());
        assertEquals("Critical crash", result.get(0).getBugTitle());
        assertNotNull(result.get(0).getCreatedAt());
        verify(repo).findBySeverity(Severity.CRITICAL);
    }

    @Test
    public void list_withSeverityFilter_returnsEmptyWhenNoneMatch() {
        BugRepository repo = mock(BugRepository.class);
        when(repo.findBySeverity(Severity.MEDIUM)).thenReturn(Collections.emptyList());

        BugServiceImpl service = new BugServiceImpl(repo);

        List<BugResponse> result = service.list(Optional.of(Severity.MEDIUM));

        assertEquals(0, result.size());
    }

    // ---- helpers ----

    private Bug makeBug(String title, String description, Severity severity, BugStatus status) {
        Bug b = new Bug();
        b.setBugTitle(title);
        b.setDescription(description);
        b.setSeverity(severity);
        b.setStatus(status);
        return b;
    }
}

