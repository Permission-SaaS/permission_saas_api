package com.saas.permissions.project.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.dto.SearchProjectsRequest;
import com.saas.permissions.project.application.command.SearchProjectsQuery;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class SearchProjectsMapper implements Mapper<SearchProjectsRequest, SearchProjectsQuery> {

    @Override
    public SearchProjectsQuery map(SearchProjectsRequest request) {
        return new SearchProjectsQuery(
                request.name(),
                request.onlyActive());
    }
}
