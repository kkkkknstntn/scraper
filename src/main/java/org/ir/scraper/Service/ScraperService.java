package org.ir.scraper.Service;

import lombok.RequiredArgsConstructor;
import org.ir.scraper.Entity.Image;
import org.ir.scraper.Entity.Page;
import org.ir.scraper.Repository.ImageRepository;
import org.ir.scraper.Repository.PageRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            String normalizedUrl = normalizeUrl(url);

            if (!normalizedUrl.startsWith("https://extinct-animals.fandom.com/ru/wiki")) {
                continue;
            }

            if (!normalizedUrl.startsWith("https://extinct-animals.fandom.com/ru/wiki/Служебная")) {
                normalizedUrl = url;
            }


            if (normalizedUrl.contains(".jpg")){
                continue;
            }



            if (visited.contains(normalizedUrl)) {
                continue;
            }

            try {
                Document document = Jsoup.connect(url).get();
                String htmlContent = document.html();
                String title = document.title();

                List<String> targetSections = List.of("Описание", "Определение", "Современные представления");
                String description = extractDescription(document, targetSections);

                List<String> excludeSections = List.of("Описание", "Определение", "Современные представления");
                List<String> additionalTexts = extractAdditionalSections(document, excludeSections, 3);

                // Извлечение "Классификация"
                String classification = extractClassification(document);

                Page page = new Page();
                page.setUrl(url);
                page.setTitle(title);
                page.setDescription(description);
                page.setClassification(classification); // Устанавливаем значение "Классификация"
                page.setAdditionalText1(additionalTexts.size() > 0 ? additionalTexts.get(0) : null);
                page.setAdditionalText2(additionalTexts.size() > 1 ? additionalTexts.get(1) : null);
                page.setAdditionalText3(additionalTexts.size() > 2 ? additionalTexts.get(2) : null);
                List<String> categories = extractCategories(document);
                page.setCategories(String.join(", ", categories));
                Page savedPage = pageRepository.save(page);

                // Извлечение ссылок на изображения
                List<Image> images = new ArrayList<>();
                for (Element img : document.select("img")) {
                    String imgUrl = img.absUrl("src");
                    if (!imgUrl.isEmpty() && imgUrl.length() <= 2048 && !imageRepository.existsByUrl(imgUrl)) {
                        Image image = new Image();
                        image.setUrl(imgUrl);
                        image.setPage(savedPage);
                        images.add(image);
                    }
                }

                if (!images.isEmpty()) {
                    imageRepository.saveAll(images);
                }

                Elements links = document.select("a[href]");
                for (Element link : links) {
                    String absUrl = link.attr("abs:href");
                    if (absUrl.startsWith("https://extinct-animals.fandom.com/ru/wiki/Служебная")) {
                        queue.add(absUrl);
                    }
                    else if (!visited.contains(normalizeUrl(absUrl))) {
                        queue.add(normalizeUrl(absUrl));
                    }
                }
                count++;

            } catch (IOException e) {
                e.printStackTrace();
            }

            visited.add(normalizedUrl);
        }
    }

    // Метод для извлечения "Классификация"
    private String extractClassification(Document document) {
        StringBuilder classification = new StringBuilder();

        // Блок "Классификация" идентифицируется через CSS-селектор
        Element classificationBlock = document.selectFirst("section.pi-item.pi-group.pi-border-color");
        if (classificationBlock != null) {
            // Извлечение подпунктов
            String[] words = classificationBlock.text().split(" ");
            StringBuilder result = new StringBuilder();

            // Добавляем каждое второе слово (начиная с индекса 1)
            if (Objects.equals(words[2], "Животные")) {
                for (int i = 2; i < words.length; i += 2) {
                    result.append(words[i]).append(" ");
                }
            }

            // Удаляем последний лишний пробел и возвращаем результат
            return result.toString().trim();
        }

        return classification.toString().trim(); // Возвращаем текст без лишних пробелов
    }

    private List<String> extractCategories(Document document) {
        List<String> categories = new ArrayList<>();
        String documentContent = document.toString();
        Pattern pattern = Pattern.compile("\"wgCategories\":\\[(.*?)]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(documentContent);

        if (matcher.find()) {
            String categoriesString = matcher.group(1);
            String[] categoriesArray = categoriesString.split(",");
            for (String category : categoriesArray) {
                categories.add(category.replaceAll("^\"|\"$", "").trim());
            }
        }

        return categories;
    }

    private List<String> extractAdditionalSections(Document document, List<String> excludeSections, int limit) {
        List<String> results = new ArrayList<>();
        for (Element header : document.select("h2")) {
            Element span = header.selectFirst("span");
            if (span != null && excludeSections.stream().noneMatch(span.id()::contains)) {
                Element sibling = header.nextElementSibling();
                StringBuilder sectionText = new StringBuilder();
                while (sibling != null && !sibling.tagName().equals("h2")) {
                    sectionText.append(sibling.text()).append("\n");
                    sibling = sibling.nextElementSibling();
                }
                results.add(Jsoup.parse(sectionText.toString()).text());
                if (results.size() == limit) break;
            }
        }
        return results;
    }

    private String extractDescription(Document document, List<String> targetSections) {
        for (Element header : document.select("h2")) {
            boolean flag = false;
            for (String sectionName : targetSections) {
                if (header.toString().contains(sectionName)) flag = true;
            }

            Element span = header.selectFirst("span");
            if (span != null && flag) {
                Element sibling = header.nextElementSibling();
                StringBuilder description = new StringBuilder();
                while (sibling != null && !sibling.tagName().equals("h2")) {
                    description.append(sibling.text()).append("\n");
                    sibling = sibling.nextElementSibling();
                }
                return Jsoup.parse(description.toString()).text();
            }
        }
        return null;
    }

    private String normalizeUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String path = parsedUrl.getPath();

            if (path.contains("/wiki/")) {
                int wikiIndex = path.indexOf("/wiki/") + 6;
                String prefix = path.substring(0, wikiIndex);
                String pageName = path.substring(wikiIndex);

                int colonIndex = pageName.indexOf(":");
                int hashIndex = pageName.indexOf("#");

                int cutIndex = pageName.length();
                if (colonIndex != -1) cutIndex = colonIndex;
                if (hashIndex != -1) cutIndex = Math.min(cutIndex, hashIndex);

                pageName = pageName.substring(0, cutIndex);
                path = prefix + pageName;
            }

            return new URL(parsedUrl.getProtocol(), parsedUrl.getHost(), path).toString();
        } catch (MalformedURLException e) {
            System.err.println("Invalid URL format: " + url);
            return url;
        }
    }
}

