package org.ir.scraper.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "new_pages_index")
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class Page {
    @Id
    private Long id;

    @Field(type = FieldType.Text, name = "url")
    private String url;

    @Field(type = FieldType.Text, name = "title")
    private String title;

    @Field(type = FieldType.Text, name = "description")
    private String description;

    @Field(type = FieldType.Text, name = "additional_text1")
    private String additionalText1;

    @Field(type = FieldType.Text, name = "additional_text2")
    private String additionalText2;

    @Field(type = FieldType.Text, name = "additional_text3")
    private String additionalText3;

    @Field(type = FieldType.Text, name = "categories")
    private String categories;

    @Field(type = FieldType.Text, name = "classification")
    private String classification;
}
