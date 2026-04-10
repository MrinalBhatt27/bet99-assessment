package com.bet99.bugtracker.service;

import com.bet99.bugtracker.dto.BugResponse;
import com.bet99.bugtracker.dto.CreateBugRequest;
import com.bet99.bugtracker.dto.UpdateBugRequest;
import com.bet99.bugtracker.exception.BugNotFoundException;
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
import static org.mockito.Mockito.never;
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

        List<BugResponse> result = service.list(Optional.<Severity>empty(), Optional.<BugStatus>empty());

        assertEquals(2, result.size());
        assertEquals("Login broken", result.get(0).getBugTitle());
        assertEquals(Severity.HIGH, result.get(0).getSeverity());
        assertEquals("Typo on homepage", result.get(1).getBugTitle());
        verify(repo).findAll();
    }

    @Test
    public void list_withSeverityFilter_delegatesToFindBySeverity() {
        BugRepository repo = mock(BugRepository.class);

        Bug b = makeBug("Critical crash", "App crashes on load", Severity.CRITICAL, BugStatus.OPEN);
        when(repo.findBySeverity(Severity.CRITICAL)).thenReturn(Collections.singletonList(b));

        BugServiceImpl service = new BugServiceImpl(repo);

        List<BugResponse> result = service.list(Optional.of(Severity.CRITICAL), Optional.<BugStatus>empty());

        assertEquals(1, result.size());
        assertEquals(Severity.CRITICAL, result.get(0).getSeverity());
        verify(repo).findBySeverity(Severity.CRITICAL);
    }

    @Test
    public void list_withSeverityFilter_returnsEmptyWhenNoneMatch() {
        BugRepository repo = mock(BugRepository.class);
        when(repo.findBySeverity(Severity.MEDIUM)).thenReturn(Collections.<Bug>emptyList());

        BugServiceImpl service = new BugServiceImpl(repo);

        List<BugResponse> result = service.list(Optional.of(Severity.MEDIUM), Optional.<BugStatus>empty());

        assertEquals(0, result.size());
    }

    @Test
    public void list_withStatusFilter_delegatesToFindByStatus() {
        BugRepository repo = mock(BugRepository.class);

        Bug b = makeBug("Open bug", "Still open", Severity.LOW, BugStatus.OPEN);
        when(repo.findByStatus(BugStatus.OPEN)).thenReturn(Collections.singletonList(b));

        BugServiceImpl service = new BugServiceImpl(repo);

        List<BugResponse> result = service.list(Optional.<Severity>empty(), Optional.of(BugStatus.OPEN));

        assertEquals(1, result.size());
        assertEquals(BugStatus.OPEN, result.get(0).getStatus());
        verify(repo).findByStatus(BugStatus.OPEN);
    }

    @Test
    public void list_withBothFilters_delegatesToFindBySeverityAndStatus() {
        BugRepository repo = mock(BugRepository.class);

        Bug b = makeBug("High open bug", "Needs attention", Severity.HIGH, BugStatus.OPEN);
        when(repo.findBySeverityAndStatus(Severity.HIGH, BugStatus.OPEN)).thenReturn(Collections.singletonList(b));

        BugServiceImpl service = new BugServiceImpl(repo);

        List<BugResponse> result = service.list(Optional.of(Severity.HIGH), Optional.of(BugStatus.OPEN));

        assertEquals(1, result.size());
        assertEquals(Severity.HIGH, result.get(0).getSeverity());
        assertEquals(BugStatus.OPEN, result.get(0).getStatus());
        verify(repo).findBySeverityAndStatus(Severity.HIGH, BugStatus.OPEN);
    }

    @Test
    public void updateStatus_existingBug_updatesStatusAndReturns() {
        BugRepository repo = mock(BugRepository.class);

        Bug bug = makeBug("Login broken", "Cannot log in", Severity.HIGH, BugStatus.OPEN);
        when(repo.findById(1L)).thenReturn(Optional.of(bug));

        BugServiceImpl service = new BugServiceImpl(repo);

        BugResponse result = service.updateStatus(1L, BugStatus.RESOLVED);

        assertEquals(BugStatus.RESOLVED, result.getStatus());
        assertEquals(BugStatus.RESOLVED, bug.getStatus()); // entity mutated in place
    }

    @Test(expected = BugNotFoundException.class)
    public void updateStatus_nonExistentBug_throwsBugNotFoundException() {
        BugRepository repo = mock(BugRepository.class);
        when(repo.findById(99L)).thenReturn(Optional.<Bug>empty());

        BugServiceImpl service = new BugServiceImpl(repo);
        service.updateStatus(99L, BugStatus.RESOLVED);
    }

    @Test
    public void update_existingBug_updatesAllFieldsAndReturns() {
        BugRepository repo = mock(BugRepository.class);
        Bug bug = makeBug("Old title", "Old desc", Severity.LOW, BugStatus.OPEN);
        when(repo.findById(1L)).thenReturn(Optional.of(bug));

        UpdateBugRequest req = new UpdateBugRequest();
        req.setBugTitle("New title");
        req.setDescription("New desc");
        req.setSeverity(Severity.CRITICAL);
        req.setStatus(BugStatus.RESOLVED);

        BugServiceImpl service = new BugServiceImpl(repo);
        BugResponse result = service.update(1L, req);

        assertEquals("New title", result.getBugTitle());
        assertEquals("New desc", result.getDescription());
        assertEquals(Severity.CRITICAL, result.getSeverity());
        assertEquals(BugStatus.RESOLVED, result.getStatus());
        // Verify entity was mutated
        assertEquals("New title", bug.getBugTitle());
        assertEquals(Severity.CRITICAL, bug.getSeverity());
    }

    @Test(expected = BugNotFoundException.class)
    public void update_nonExistentBug_throwsBugNotFoundException() {
        BugRepository repo = mock(BugRepository.class);
        when(repo.findById(99L)).thenReturn(Optional.<Bug>empty());

        BugServiceImpl service = new BugServiceImpl(repo);
        service.update(99L, new UpdateBugRequest());
    }

    @Test
    public void delete_existingBug_callsRepository() {
        BugRepository repo = mock(BugRepository.class);
        Bug bug = makeBug("Old bug", "To be deleted", Severity.LOW, BugStatus.OPEN);
        when(repo.findById(1L)).thenReturn(Optional.of(bug));

        BugServiceImpl service = new BugServiceImpl(repo);
        service.delete(1L);

        verify(repo).deleteById(1L);
    }

    @Test(expected = BugNotFoundException.class)
    public void delete_nonExistentBug_throwsBugNotFoundException() {
        BugRepository repo = mock(BugRepository.class);
        when(repo.findById(99L)).thenReturn(Optional.<Bug>empty());

        BugServiceImpl service = new BugServiceImpl(repo);
        service.delete(99L);

        verify(repo, never()).deleteById(99L);
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
