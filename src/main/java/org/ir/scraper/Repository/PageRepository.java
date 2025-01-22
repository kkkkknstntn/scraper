package org.ir.scraper.Repository;

import org.ir.scraper.Entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageRepository extends JpaRepository<Page, Long> {
}