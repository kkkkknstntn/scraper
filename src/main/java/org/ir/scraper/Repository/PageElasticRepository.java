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
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PageElasticRepository {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    public List<Page> searchByQuery(String query) throws IOException {
        // Разбиваем запрос на первое слово и остальную часть
        String[] words = query.split("\\s+");
        String firstWord = words.length > 0 ? words[0] : "";
        String remainingQuery = words.length > 1
                ? String.join(" ", Arrays.copyOfRange(words, 1, words.length))
                : "";

        // Базовые запросы для полного запроса
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
                .field("title.keyword")
                .boost(3.0f)
        )._toQuery();

        // Условия для первого слова с повышенным приоритетом
        List<Query> shouldQueries = new ArrayList<>();
        shouldQueries.add(phraseQuery);
        shouldQueries.add(multiMatchQuery);
        shouldQueries.add(synonymQuery);

        if (!firstWord.isEmpty()) {
            // N-gram запрос для первого слова
            Query ngramQuery = MultiMatchQuery.of(m -> m
                    .query(firstWord)
                    .fields(
                            "title.ngram^20",    // повышенный boost для n-gram
                            "classification.ngram^18",
                            "categories.ngram^15",
                            "description.ngram^12",
                            "additionalText1.ngram^10",
                            "additionalText2.ngram^10",
                            "additionalText3.ngram^10"
                    )
                    .type(TextQueryType.BestFields)
                    .boost(10.0f) // общий boost
            )._toQuery();

            // Высокоприоритетный запрос по первому слову
            Query firstWordBoostQuery = MultiMatchQuery.of(m -> m
                    .query(firstWord)
                    .fields(
                            "title^15",    // Повышенный boost
                            "classification^12",
                            "categories^9",
                            "description^6",
                            "additionalText1^3",
                            "additionalText2^3",
                            "additionalText3^3"
                    )
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO:4,7")
                    .boost(8.0f)
            )._toQuery();

            // Точное совпадение первого слова
            Query exactFirstWordQuery = MatchQuery.of(m -> m
                    .query(firstWord)
                    .field("title.keyword")
                    .boost(12.0f)
            )._toQuery();

            shouldQueries.add(ngramQuery);
            shouldQueries.add(firstWordBoostQuery);
            shouldQueries.add(exactFirstWordQuery);
        }

        // Дополнительные условия для оставшейся части запроса
        if (!remainingQuery.isEmpty()) {
            Query remainingPhraseQuery = MatchPhraseQuery.of(mp -> mp
                    .query(remainingQuery)
                    .slop(3)
                    .boost(1.5f)
                    .field("title")
            )._toQuery();

            Query remainingMultiMatch = MultiMatchQuery.of(m -> m
                    .query(remainingQuery)
                    .fields(
                            "title^5",
                            "classification^4",
                            "categories^3",
                            "description^2",
                            "additionalText1",
                            "additionalText2",
                            "additionalText3"
                    )
                    .boost(0.7f)
            )._toQuery();

            shouldQueries.add(remainingPhraseQuery);
            shouldQueries.add(remainingMultiMatch);
        }

        // Объединяем все условия
        Query combinedQuery = BoolQuery.of(b -> b
                .should(shouldQueries)
        )._toQuery();

        // Выполняем поиск
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