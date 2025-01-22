package org.ir.scraper.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2048)
    private String url; // Ссылка на изображение

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private Page page; // Внешний ключ на таблицу страниц
}
