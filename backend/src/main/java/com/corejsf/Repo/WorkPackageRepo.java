package com.corejsf.Repo;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.corejsf.Entity.WorkPackage;

@Stateless
public class WorkPackageRepo {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    public WorkPackage findById(String wpId) {
        return em.find(WorkPackage.class, wpId);
    }

    public List<WorkPackage> findDirectChildren(String parentWpId) {
        return em.createQuery(
                "SELECT w FROM WorkPackage w " +
                "WHERE w.parentWorkPackage.wpId = :pid " +
                "ORDER BY w.wpId",
                WorkPackage.class
        ).setParameter("pid", parentWpId)
         .getResultList();
    }
}