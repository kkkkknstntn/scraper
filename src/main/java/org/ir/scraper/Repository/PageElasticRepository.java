package org.ir.scraper.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.ir.scraper.Document.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class PageElasticRepository {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    public List<Page> searchByQuery(String query) throws IOException {
        Query phraseQuery = MatchPhraseQuery.of(mp -> mp
                .query(query)
                .slop(3)
                .boost(2.5f)
                .field("title")
        )._toQuery();

        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .fields(
                        "title^5",
                        "classification^4",
                        "categories^3",
                        "description^2",
                        "additionalText1",
                        "additionalText2",
                        "additionalText3"
                )
                .type(TextQueryType.BestFields)
                .fuzziness("AUTO:2,5")
                .prefixLength(2)
                .maxExpansions(10)
                .operator(Operator.And)
        )._toQuery();

        Query synonymQuery = MatchQuery.of(m -> m
                .query(query)
                .field("title.keyword") // Поиск по полному соответствию
                .boost(3.0f)
        )._toQuery();

        Query combinedQuery = BoolQuery.of(b -> b
                .should(phraseQuery)
                .should(multiMatchQuery)
                .should(synonymQuery)
        )._toQuery();


        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index("new_pages_index")
                .query(combinedQuery)
                .from(0)
                .size(10)
        );

        SearchResponse<Page> response = elasticsearchClient.search(searchRequest, Page.class);

        return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}