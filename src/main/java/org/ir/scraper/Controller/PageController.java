package org.ir.scraper.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ir.scraper.Document.Page;
import org.ir.scraper.Repository.PageElasticRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pages")
@Slf4j
public class PageController {
    private final PageElasticRepository pageElasticRepository;

    @GetMapping
    @Operation
    public List<Page> getPages(@RequestParam String title) throws IOException {
        return this.pageElasticRepository.searchByQuery(title);
    }
}
