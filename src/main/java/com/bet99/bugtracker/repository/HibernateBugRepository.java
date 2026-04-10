package com.bet99.bugtracker.repository;

import com.bet99.bugtracker.model.Bug;
import com.bet99.bugtracker.model.BugStatus;
import com.bet99.bugtracker.model.Severity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class HibernateBugRepository implements BugRepository {

    private final SessionFactory sessionFactory;

    public HibernateBugRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public Bug save(Bug bug) {
        session().save(bug);
        return bug;
    }

    @Override
    public Optional<Bug> findById(Long id) {
        return Optional.ofNullable(session().get(Bug.class, id));
    }

    @Override
    public List<Bug> findAll() {
        return session()
                .createQuery("from Bug b order by b.id desc", Bug.class)
                .getResultList();
    }

    @Override
    public List<Bug> findBySeverity(Severity severity) {
        return session()
                .createQuery("from Bug b where b.severity = :sev order by b.id desc", Bug.class)
                .setParameter("sev", severity)
                .getResultList();
    }

    @Override
    public List<Bug> findByStatus(BugStatus status) {
        return session()
                .createQuery("from Bug b where b.status = :status order by b.id desc", Bug.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Bug> findBySeverityAndStatus(Severity severity, BugStatus status) {
        return session()
                .createQuery("from Bug b where b.severity = :sev and b.status = :status order by b.id desc", Bug.class)
                .setParameter("sev", severity)
                .setParameter("status", status)
                .getResultList();
    }
}
