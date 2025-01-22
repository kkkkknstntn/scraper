package org.ir.scraper.Repository;

import org.ir.scraper.Entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
    boolean existsByUrl(String url);
}