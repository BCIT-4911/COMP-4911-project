package com.corejsf.Service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.corejsf.Entity.SystemRole;

public class RebacServiceTest {

    private final RebacService rebacService = new RebacService();

    @Test
    void operationsManagerCanCreateProject() {
        assertTrue(rebacService.canCreateProject(SystemRole.OPERATIONS_MANAGER));
    }
}