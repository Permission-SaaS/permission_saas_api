package com.saas.permissions.project.api.route.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

/**
 * Upload do CSV de rotas (name,httpMethod,path,description). Chega como formulário
 * multipart, no campo {@code file}. Um arquivo vazio não é erro: a importação lê
 * zero linhas.
 */
public record RouteImportRequest(@NotNull MultipartFile file) {
}
