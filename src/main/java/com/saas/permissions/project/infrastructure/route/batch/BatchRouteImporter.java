package com.saas.permissions.project.infrastructure.route.batch;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.saas.permissions.project.domain.route.RouteImportResult;
import com.saas.permissions.project.domain.route.RouteImporter;

/**
 * Dispara o job {@code importRoutesJob} e devolve os contadores do step. O reader do
 * Batch lê de um arquivo em disco, então o CSV recebido é copiado para um arquivo
 * temporário, que some no fim da execução. O separador do CSV sai do cabeçalho:
 * {@code ;}, que é como o Excel em português salva, ou {@code ,}. O job roda na mesma
 * thread da requisição: o arquivo é pequeno e quem importa quer o resumo na resposta.
 */
@Component
public class BatchRouteImporter implements RouteImporter {

    private final JobOperator jobOperator;
    private final Job importRoutesJob;

    public BatchRouteImporter(JobOperator jobOperator, @Qualifier("importRoutesJob") Job importRoutesJob) {
        this.jobOperator = jobOperator;
        this.importRoutesJob = importRoutesJob;
    }

    @Override
    public RouteImportResult importRoutes(UUID projectId, InputStream csv) {
        Path csvFile = copyToTemporaryFile(csv);
        try {
            return run(projectId, csvFile);
        } finally {
            deleteQuietly(csvFile);
        }
    }

    private RouteImportResult run(UUID projectId, Path csvFile) {
        JobParameters parameters = new JobParametersBuilder()
                .addString("projectId", projectId.toString())
                .addString("file", csvFile.toAbsolutePath().toString())
                .addString("delimiter", findDelimiter(csvFile))
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters();

        try {
            JobExecution execution = jobOperator.start(importRoutesJob, parameters);

            long read = 0;
            long imported = 0;
            long discarded = 0;
            for (StepExecution step : execution.getStepExecutions()) {
                read += step.getReadCount();
                imported += step.getWriteCount();
                discarded += step.getFilterCount() + step.getSkipCount();
            }
            return new RouteImportResult(execution.getId(), execution.getStatus().name(), read, imported,
                    discarded);
        } catch (Exception e) {
            throw new IllegalStateException("Could not start the route import job", e);
        }
    }

    private static Path copyToTemporaryFile(InputStream csv) {
        try {
            Path file = Files.createTempFile("routes-import-", ".csv");
            Files.copy(csv, file, StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the uploaded CSV", e);
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // arquivo temporário: o sistema limpa se ficar para trás
        }
    }

    /**
     * Lê só até a primeira linha não vazia, o cabeçalho. A leitura é em ISO-8859-1 porque
     * nunca falha: em UTF-8, um CSV do Excel em Windows-1252 com acento lançaria
     * MalformedInputException. Para achar {@code ;} ou {@code ,}, o encoding não importa.
     */
    private static String findDelimiter(Path csvFile) {
        try (Stream<String> lines = Files.lines(csvFile, StandardCharsets.ISO_8859_1)) {
            String header = lines.filter(line -> !line.isBlank()).findFirst().orElse("");

            return header.contains(";") ? ";" : ",";

        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the CSV header", e);
        }
    }
}
