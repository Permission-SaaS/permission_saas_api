package com.saas.permissions.project.infrastructure.route.batch;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.saas.permissions.project.application.route.AddRouteToProjectUseCase;
import com.saas.permissions.project.application.route.command.AddRouteToProjectCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Grava um chunk de rotas pelo mesmo caso de uso do POST /projects/{id}/routes: a regra
 * de criação de rota continua num lugar só. O chunk inteiro roda numa transação, aberta
 * pelo Spring Batch; se uma rota falhar, nenhuma daquele chunk fica gravada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteImportWriter implements ItemWriter<AddRouteToProjectCommand> {

    private final AddRouteToProjectUseCase addRouteToProjectUseCase;

    @Override
    public void write(Chunk<? extends AddRouteToProjectCommand> chunk) {
        for (AddRouteToProjectCommand command : chunk) {
            addRouteToProjectUseCase.execute(command);
        }
        log.info("Route import chunk written: {} routes", chunk.size());
    }

}
