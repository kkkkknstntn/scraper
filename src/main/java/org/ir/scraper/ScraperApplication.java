package org.ir.scraper;

import lombok.RequiredArgsConstructor;
import org.ir.scraper.Service.ScraperService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@RequiredArgsConstructor
public class ScraperApplication implements CommandLineRunner {


    private final ScraperService scraperService;

    public static void main(String[] args) {
        SpringApplication.run(ScraperApplication.class, args);
    }

    @Override
    public void run(String... args) {
        String startUrl = "https://extinct-animals.fandom.com/ru/wiki/«Futabasaurus»"; // Replace with your starting URL
        int maxPages = 50;
        scraperService.scrape(startUrl, maxPages);
    }
}