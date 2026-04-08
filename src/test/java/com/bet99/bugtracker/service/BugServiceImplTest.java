package com.bet99.bugtracker.service;

import com.bet99.bugtracker.dto.CreateBugRequest;
import com.bet99.bugtracker.model.Bug;
import com.bet99.bugtracker.model.BugStatus;
import com.bet99.bugtracker.model.Severity;
import com.bet99.bugtracker.repository.BugRepository;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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

        Bug saved = new Bug();
        saved.setBugTitle("t");
        saved.setDescription("d");
        saved.setSeverity(Severity.HIGH);
        saved.setStatus(BugStatus.OPEN);

        when(repo.save(any(Bug.class))).thenAnswer(invocation -> {
            Bug b = invocation.getArgument(0);
            return b;
        });

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
}

