package com.bet99.bugtracker.repository;

import com.bet99.bugtracker.model.Bug;
import com.bet99.bugtracker.model.Severity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class HibernateBugRepository implements BugRepository {

    private final SessionFactory sessionFactory;

    public HibernateBugRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Bug save(Bug bug) {
        Session session = sessionFactory.getCurrentSession();
        session.save(bug);
        return bug;
    }

    @Override
    public List<Bug> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("from Bug b order by b.id desc", Bug.class)
                .getResultList();
    }

    @Override
    public List<Bug> findBySeverity(Severity severity) {
        return sessionFactory.getCurrentSession()
                .createQuery("from Bug b where b.severity = :sev order by b.id desc", Bug.class)
                .setParameter("sev", severity)
                .getResultList();
    }
}

