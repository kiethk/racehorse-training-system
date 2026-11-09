package com.rtms.backend.audit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditActivityResolverTest {

    private AuditActivityResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuditActivityResolver();
    }

    @Test
    void testResolveDeleteBarnArea() {
        AuditActivityResolver.ResolvedActivity activity = resolver.resolve("DELETE", "/api/areas/12", 200);

        assertEquals("Remove Barn Area", activity.action());
        assertEquals("Area", activity.entityName());
        assertEquals(12L, activity.entityId());
        assertEquals("Manager removed barn area #12", activity.details());
    }

    @Test
    void testResolveCreateBarnArea() {
        AuditActivityResolver.ResolvedActivity activity = resolver.resolve("POST", "/api/areas", 201);

        assertEquals("Create Barn Area", activity.action());
        assertEquals("Area", activity.entityName());
        assertNull(activity.entityId());
        assertEquals("Manager created a new stable area", activity.details());
    }

    @Test
    void testResolveUpdateBarnArea() {
        AuditActivityResolver.ResolvedActivity activity = resolver.resolve("PUT", "/api/areas/5", 200);

        assertEquals("Update Barn Area", activity.action());
        assertEquals("Area", activity.entityName());
        assertEquals(5L, activity.entityId());
        assertEquals("Manager updated barn area #5", activity.details());
    }

    @Test
    void testResolveCreateAccountFailed() {
        AuditActivityResolver.ResolvedActivity activity = resolver.resolve("POST", "/api/accounts", 409);

        assertEquals("Create Account", activity.action());
        assertEquals("User", activity.entityName());
        assertEquals("Manager created a new account (Attempt failed)", activity.details());
    }
}
