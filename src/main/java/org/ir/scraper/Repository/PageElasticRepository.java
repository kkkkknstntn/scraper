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
        String[] words = query.split("\\s+");
        String firstWord = words.length > 0 ? words[0] : "";
        String secondWord = words.length > 1 ? words[1] : "";
        String firstTwoWords = (firstWord + " " + secondWord).trim();
        String remainingQuery = words.length > 2
                ? String.join(" ", Arrays.copyOfRange(words, 2, words.length))
                : "";

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

        List<Query> shouldQueries = new ArrayList<>();
        if (!firstTwoWords.isEmpty()) {
            Query firstTwoWordsPhraseQuery = MatchPhraseQuery.of(mp -> mp
                    .query(firstTwoWords)
                    .slop(0)
                    .boost(40.0f)
                    .field("title")
            )._toQuery();

            Query firstTwoWordsSlopQuery = MatchPhraseQuery.of(mp -> mp
                    .query(firstTwoWords)
                    .slop(2)
                    .boost(30.0f)
                    .field("title")
            )._toQuery();

            shouldQueries.add(firstTwoWordsPhraseQuery);
            shouldQueries.add(firstTwoWordsSlopQuery);
        }

        if (!firstWord.isEmpty()) {
            Query ngramQuery = MultiMatchQuery.of(m -> m
                    .query(firstWord)
                    .fields(
                            "title.ngram^20",
                            "classification.ngram^18",
                            "categories.ngram^15",
                            "description.ngram^12",
                            "additionalText1.ngram^10",
                            "additionalText2.ngram^10",
                            "additionalText3.ngram^10"
                    )
                    .type(TextQueryType.BestFields)
                    .boost(10.0f)
            )._toQuery();

            Query firstWordBoostQuery = MultiMatchQuery.of(m -> m
                    .query(firstWord)
                    .fields(
                            "title^15",
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

            Query exactFirstWordQuery = MatchQuery.of(m -> m
                    .query(firstWord)
                    .field("title.keyword")
                    .boost(12.0f)
            )._toQuery();

            shouldQueries.add(ngramQuery);
            shouldQueries.add(firstWordBoostQuery);
            shouldQueries.add(exactFirstWordQuery);
        }

        if (!secondWord.isEmpty()) {
//            Query secondWordNgramQuery = MultiMatchQuery.of(m -> m
//                    .query(secondWord)
//                    .fields(
//                            "title.ngram^3",
//                            "classification.ngram^4",
//                            "categories.ngram^4",
//                            "description.ngram^3",
//                            "additionalText1.ngram^3",
//                            "additionalText2.ngram^2",
//                            "additionalText3.ngram^2"
//                    )
//                    .type(TextQueryType.BestFields)
//                    .boost(5.0f)
//            )._toQuery();

            Query secondWordBoostQuery = MultiMatchQuery.of(m -> m
                    .query(secondWord)
                    .fields(
                            "title^5",
                            "classification^3",
                            "categories^4",
                            "description^10",
                            "additionalText1^3",
                            "additionalText2^2",
                            "additionalText3^2"
                    )
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO:4,7")
                    .boost(4.0f)
            )._toQuery();

            Query exactSecondWordQuery = MatchQuery.of(m -> m
                    .query(secondWord)
                    .field("title.keyword")
                    .boost(6.0f)
            )._toQuery();

//            shouldQueries.add(secondWordNgramQuery);
            shouldQueries.add(secondWordBoostQuery);
            shouldQueries.add(exactSecondWordQuery);
        }

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

//        shouldQueries.add(synonymQuery);
//        shouldQueries.add(multiMatchQuery);
//        shouldQueries.add(phraseQuery);

        Query combinedQuery = BoolQuery.of(b -> b
                .should(shouldQueries)
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