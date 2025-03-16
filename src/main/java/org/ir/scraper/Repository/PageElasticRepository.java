package org.ir.scraper.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.ir.scraper.Document.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PageElasticRepository {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    public List<Page> searchByQuery(String query) throws IOException {
        // Создаем multi_match запрос
        MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .fields("title^4", "classification^3", "categories^2", "description^1.5", "additionalText1", "additionalText2", "additionalText3")
                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.MostFields)
                .fuzziness("AUTO")
                .prefixLength(2)
                .maxExpansions(10)
        );

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index("new_pages_index") // Укажите имя вашего индекса
                .query(Query.of(q -> q.multiMatch(multiMatchQuery)))
                .from(0)
                .size(10)
        );

        SearchResponse<Page> searchResponse = elasticsearchClient.search(searchRequest, Page.class);

        return searchResponse.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }
}