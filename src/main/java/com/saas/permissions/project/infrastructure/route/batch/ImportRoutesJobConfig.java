package com.saas.permissions.project.infrastructure.route.batch;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.saas.permissions.project.application.route.command.AddRouteToProjectCommand;

/**
 * A montagem do job de importação de rotas:
 *
 * <pre>
 * CSV ─▶ routeCsvReader ─▶ RouteImportProcessor ─▶ RouteImportWriter ─▶ banco
 *        uma linha por vez   normaliza ou descarta   grava de 10 em 10
 * </pre>
 *
 * Quem dispara é o {@link BatchRouteImporter}, com os parâmetros {@code projectId},
 * {@code file} e {@code delimiter}.
 */
@Configuration
public class ImportRoutesJobConfig {

    /** Quantas linhas formam um chunk: cada chunk é gravado numa transação. */
    static final int CHUNK_SIZE = 10;

    /** Quantas linhas com o número errado de colunas a importação tolera antes de falhar. */
    static final int MALFORMED_LINES_LIMIT = 10;

    /**
     * Lê o CSV uma linha por vez, pulando o cabeçalho, com o separador ({@code ,} ou {@code ;})
     * que o {@link BatchRouteImporter} encontrou. O método devolve o tipo concreto
     * (FlatFileItemReader) e não a interface ItemReader: com {@code @StepScope} o Spring
     * cria um proxy a partir do tipo declarado, e o step precisa ver que o reader abre e
     * fecha o arquivo.
     */
    @Bean
    @StepScope
    FlatFileItemReader<RouteCsvLine> routeCsvReader(@Value("#{jobParameters['file']}") String file,
            @Value("#{jobParameters['delimiter']}") String delimiter) {
        return new FlatFileItemReaderBuilder<RouteCsvLine>()
                .name("routeCsvReader")
                .resource(new FileSystemResource(file))
                .linesToSkip(1)
                .delimited()
                .delimiter(delimiter)
                .names("name", "httpMethod", "path", "description")
                .targetType(RouteCsvLine.class)
                .build();
    }

    @Bean
    Step importRoutesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            FlatFileItemReader<RouteCsvLine> routeCsvReader, RouteImportProcessor routeImportProcessor,
            RouteImportWriter routeImportWriter) {
        return new StepBuilder("importRoutesStep", jobRepository)
                .<RouteCsvLine, AddRouteToProjectCommand>chunk(CHUNK_SIZE)
                .reader(routeCsvReader)
                .processor(routeImportProcessor)
                .writer(routeImportWriter)
                .transactionManager(transactionManager)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skipLimit(MALFORMED_LINES_LIMIT)
                .build();
    }

    @Bean
    Job importRoutesJob(JobRepository jobRepository, Step importRoutesStep) {
        return new JobBuilder("importRoutesJob", jobRepository)
                .start(importRoutesStep)
                .build();
    }

}
