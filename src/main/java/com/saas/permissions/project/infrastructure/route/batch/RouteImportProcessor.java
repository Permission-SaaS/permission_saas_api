package com.saas.permissions.project.infrastructure.route.batch;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.saas.permissions.project.application.route.FindProjectRoutesUseCase;
import com.saas.permissions.project.application.route.command.AddRouteToProjectCommand;
import com.saas.permissions.project.domain.route.Route;

import lombok.extern.slf4j.Slf4j;

/**
 * A regra da importação: recebe uma linha crua do CSV e devolve o comando para criar a
 * rota, já normalizado, ou {@code null} para descartar a linha. O Spring Batch conta
 * cada {@code null} como item filtrado e segue para a próxima linha.
 *
 * <p>{@code @StepScope}: um processor novo a cada execução, porque ele guarda estado (as
 * rotas que já viu) e recebe o projeto pelos parâmetros daquela execução.
 */
@Slf4j
@Component
@StepScope
public class RouteImportProcessor implements ItemProcessor<RouteCsvLine, AddRouteToProjectCommand> {

    private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final FindProjectRoutesUseCase findProjectRoutesUseCase;
    private final UUID projectId;

    /** Método + path de toda rota que já existe no projeto ou já apareceu no arquivo. */
    private Set<String> knownRoutes;

    // Construtor explícito: o @RequiredArgsConstructor do Lombok não levaria o @Value
    // para o parâmetro, e o projectId chegaria nulo.
    public RouteImportProcessor(FindProjectRoutesUseCase findProjectRoutesUseCase,
            @Value("#{jobParameters['projectId']}") String projectId) {
        this.findProjectRoutesUseCase = findProjectRoutesUseCase;
        this.projectId = UUID.fromString(projectId);
    }

    @Override
    public AddRouteToProjectCommand process(RouteCsvLine line) {
        String name = trimToNull(line.name());
        String httpMethod = normalizeHttpMethod(line.httpMethod());
        String path = normalizePath(line.path());

        if (name == null) {
            return discard(line, "name is blank");
        }
        if (httpMethod == null) {
            return discard(line, "HTTP method is not one of " + HTTP_METHODS);
        }
        if (path == null) {
            return discard(line, "path is blank");
        }
        if (!knownRoutes().add(key(httpMethod, path))) {
            return discard(line, "route already exists in the project or earlier in the file");
        }

        return new AddRouteToProjectCommand(projectId, name, path, httpMethod, trimToNull(line.description()));
    }

    /** Carregado só na primeira linha: uma consulta ao projeto por importação, não uma por linha. */
    private Set<String> knownRoutes() {
        if (knownRoutes == null) {
            knownRoutes = new HashSet<>();
            for (Route route : findProjectRoutesUseCase.execute(projectId, null)) {
                knownRoutes.add(key(route.getHttpMethod(), route.getPath()));
            }
        }
        return knownRoutes;
    }

    /** Mesmo critério do Project.addRoute: método e path sem diferenciar maiúsculas. */
    private static String key(String httpMethod, String path) {
        return httpMethod.toUpperCase(Locale.ROOT) + " " + path.toLowerCase(Locale.ROOT);
    }

    private static String normalizeHttpMethod(String httpMethod) {
        String method = trimToNull(httpMethod);
        if (method == null) {
            return null;
        }
        method = method.toUpperCase(Locale.ROOT);
        return HTTP_METHODS.contains(method) ? method : null;
    }

    /** {@code orders/{id}/} vira {@code /orders/{id}}: barra no começo, nenhuma no fim. */
    private static String normalizePath(String path) {
        String normalized = trimToNull(path);
        if (normalized == null) {
            return null;
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static AddRouteToProjectCommand discard(RouteCsvLine line, String reason) {
        log.info("Route import line discarded ({}): {}", reason, line);
        return null;
    }

}
