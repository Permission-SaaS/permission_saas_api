package com.saas.permissions.project.application;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.domain.route.Route;

import lombok.RequiredArgsConstructor;

/**
 * Lista as rotas de um projeto, opcionalmente filtradas pelo metodo HTTP e
 * sempre ordenadas por caminho. Reaproveita FindProjectByIdUseCase para herdar
 * o tratamento de projeto inexistente.
 */
@Service
@RequiredArgsConstructor
public class FindProjectRoutesUseCase {

    private final FindProjectByIdUseCase findProjectByIdUseCase;

    public List<Route> execute(UUID projectId, String httpMethod) {
        return findProjectByIdUseCase.execute(projectId).getRoutes().stream()
                .filter(route -> httpMethod == null || httpMethod.isBlank()
                        || route.getHttpMethod().equalsIgnoreCase(httpMethod))
                .sorted(Comparator.comparing(Route::getPath, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
