package org.ir.scraper.сonfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

//@Configuration
//public class MyClientConfig extends ElasticsearchConfiguration {
//    @Value("${spring.elasticsearch.uris}")
//    private String elasticsearchUrl;
//
//    @Value("${spring.elasticsearch.username}")
//    private String username;
//
//    @Value("${spring.elasticsearch.password}")
//    private String password;
//
//    @Override
//    public ClientConfiguration clientConfiguration() {
//        return ClientConfiguration.builder()
//                .connectedTo(elasticsearchUrl)
//                .withBasicAuth(username, password)
//                .build();
//    }
//}