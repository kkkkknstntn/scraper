package org.ir.scraper.Service;

import lombok.RequiredArgsConstructor;
import org.ir.scraper.Entity.Image;
import org.ir.scraper.Entity.Page;
import org.ir.scraper.Repository.ImageRepository;
import org.ir.scraper.Repository.PageRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Service
@RequiredArgsConstructor
public class ScraperService {

    private final PageRepository pageRepository;
    private final ImageRepository imageRepository;

    public void scrape(String startUrl, int maxPages) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startUrl);

        int count = 0;

        while (!queue.isEmpty() && count < maxPages) {
            String url = queue.poll();
            if (!url.startsWith("https://extinct-animals.fandom.com/ru/wiki")) {
                System.out.println("URL не соответствует нужному формату: " + url);
                continue;
            }


            if (visited.contains(url)) {
                continue;
            }

            try {
                System.out.println("Processing: " + url);

                // Парсинг страницы
                Document document = Jsoup.connect(url).get();
                String htmlContent = document.html();
                String title = document.title(); // Извлечение заголовка

                // Создание и сохранение страницы
                Page page = new Page();
                page.setUrl(url);
                page.setContent(htmlContent);
                page.setTitle(title);
                Page savedPage = pageRepository.save(page);

                // Извлечение ссылок на изображения
                List<Image> images = new ArrayList<>();
                for (Element img : document.select("img")) {
                    String imgUrl = img.absUrl("src");
                    if (!imgUrl.isEmpty() && imgUrl.length() <= 2048 && !imageRepository.existsByUrl(imgUrl)) { // Проверка уникальности
                        Image image = new Image();
                        image.setUrl(imgUrl);
                        image.setPage(savedPage);
                        images.add(image);
                    } else {
                        System.out.println("Дублирующийся или некорректный URL: " + imgUrl);
                    }
                }

                if (!images.isEmpty()) {
                    imageRepository.saveAll(images);
                }

                System.out.println("Saved: " + url);

                // Extract links and add to the queue
                Elements links = document.select("a[href]");
                for (Element link : links) {
                    String absUrl = link.attr("abs:href");
                    if (!visited.contains(absUrl)) {
                        queue.add(absUrl);
                    }
                }

                visited.add(url);
                count++;

            } catch (IOException e) {
                System.err.println("Failed to process: " + url);
                e.printStackTrace();
            }
        }
    }
}