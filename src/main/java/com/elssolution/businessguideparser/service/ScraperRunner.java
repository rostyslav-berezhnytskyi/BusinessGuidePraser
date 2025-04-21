package com.elssolution.businessguideparser.service;


import com.elssolution.businessguideparser.model.Company;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class ScraperRunner implements CommandLineRunner {

    private final CompanyScraperService companyScraperService;
    private final ExcelWriterService excelWriterService;

    // Inject services
    public ScraperRunner(CompanyScraperService companyScraperService, ExcelWriterService excelWriterService) {
        this.companyScraperService = companyScraperService;
        this.excelWriterService = excelWriterService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting scraper...");
        List<Company> allCompanies = new ArrayList<>();
        Set<String> processedUrls = new HashSet<>(); // Щоб уникнути дублів
        int page = 1;
        boolean morePages = true;
        String baseCatalogUrl = "https://business-guide.com.ua/enterprises?q=&v=1&o=&import_mva=&g=&page=";

        while(morePages) {
            String catalogUrl = baseCatalogUrl + page;
            System.out.println("Processing catalog page: " + catalogUrl);
            List<String> companyUrlsOnPage = new ArrayList<>();

            try {
                Document catalogDoc = Jsoup.connect(catalogUrl)
                        .userAgent("Mozilla/5.0") // Be a good citizen
                        .timeout(15000)
                        .get();

                // Знайдіть правильний селектор для посилань на компанії!
                // Це просто приклад, вам потрібно дослідити HTML сторінки каталогу.
                // Припустимо, посилання знаходяться всередині <div class="company-item"> -> <a>
                Elements links = catalogDoc.select("div.firm_search_string a[href^='https://']"); // !!! УТОЧНІТЬ СЕЛЕКТОР !!!

                if (links.isEmpty()) {
                    System.out.println("No company links found on page " + page + ". Assuming end of catalog.");
                    morePages = false;
                } else {
                    for (Element link : links) {
                        String companyUrl = link.absUrl("href"); // Отримати абсолютний URL
                        // Перевірити, чи це URL компанії (можливо, за форматом)
                        if (companyUrl != null && companyUrl.contains(".business-guide.com.ua") && !companyUrl.startsWith("https://business-guide.com.ua")) {
                            if (processedUrls.add(companyUrl)) { // Додати, якщо ще не обробляли
                                companyUrlsOnPage.add(companyUrl);
                            }
                        }
                    }
                    System.out.println("Found " + companyUrlsOnPage.size() + " new company URLs on page " + page);
                }

            } catch (IOException e) {
                System.err.println("Failed to process catalog page " + catalogUrl + ": " + e.getMessage());
                morePages = false; // Зупинитись у разі помилки на сторінці каталогу
            }

            // Скрапінг компаній з поточної сторінки
            for (String companyUrl : companyUrlsOnPage) {
                System.out.println("Scraping: " + companyUrl);
                Company company = companyScraperService.scrapeCompanyPage(companyUrl);
                if (company != null) {
                    allCompanies.add(company);
                }
                // !!! ВАЖЛИВО: Пауза між запитами !!!
                // Щоб не перевантажувати сайт і не бути заблокованим.
                try {
                    TimeUnit.SECONDS.sleep(2); // Пауза 2 секунди
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Scraping interrupted.");
                    break; // Вийти з циклу обробки компаній
                }
            }
            if (!morePages) break; // Вийти з головного циклу пагінації

            page++;
            // Можливо, додати обмеження на кількість сторінок для тестування
            // if (page > 5) morePages = false;

            // Додаткова пауза між обробкою сторінок каталогу
            try {
                TimeUnit.SECONDS.sleep(5); // Пауза 5 секунд
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Scraping interrupted.");
                break;
            }
        }


        // Запис у файл Excel
        if (!allCompanies.isEmpty()) {
            excelWriterService.writeCompaniesToExcel(allCompanies, "business_guide_companies.xlsx");
        } else {
            System.out.println("No companies were scraped successfully.");
        }

        System.out.println("Scraper finished.");
    }
}
