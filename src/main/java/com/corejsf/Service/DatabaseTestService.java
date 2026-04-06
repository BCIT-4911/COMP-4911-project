package com.corejsf.Service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class DatabaseTestService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    public String testConnection() {
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(w) FROM WorkPackage w",
                    Long.class
            ).getSingleResult();

            System.out.println("DB CONNECTED SUCCESS");
            System.out.println("WorkPackage count = " + count);

            return "SUCCESS: Connected to database. WorkPackage count = " + count;
        } catch (Exception e) {
            e.printStackTrace();
            return "FAILED: " + e.getMessage();
        }
    }
}