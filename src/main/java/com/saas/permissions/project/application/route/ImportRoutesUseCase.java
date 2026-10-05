package com.saas.permissions.project.application.route;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.application.project.FindProjectByIdUseCase;
import com.saas.permissions.project.domain.route.RouteImportResult;
import com.saas.permissions.project.domain.route.RouteImporter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportRoutesUseCase {

    private final FindProjectByIdUseCase findProjectByIdUseCase;
    private final RouteImporter routeImporter;

    public RouteImportResult execute(UUID projectId, InputStream csv) {
        findProjectByIdUseCase.execute(projectId);

        return routeImporter.importRoutes(projectId, csv);
    }
}
