package com.saas.permissions;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulesIntegrationTests {

    ApplicationModules modules = ApplicationModules.of(PermissionSaasApplication.class);

    @Test
    void printsModuleStructure() {
        modules.forEach(System.out::println);
    }

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}