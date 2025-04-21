package com.elssolution.businessguideparser;

import com.elssolution.businessguideparser.model.Company;
import com.elssolution.businessguideparser.service.CompanyScraperService;
import com.elssolution.businessguideparser.service.ExcelWriterService;
import com.sun.tools.javac.Main;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class BusinessGuideParserApplication implements CommandLineRunner {
    @Autowired
    private CompanyScraperService companyScraperService;
    @Autowired // Inject the ExcelWriterService
    private ExcelWriterService excelWriterService;


    public static void main(String[] args) {
        SpringApplication.run(BusinessGuideParserApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("CommandLineRunner starting... Testing scraper and Excel writer.");

        String testUrl = "https://stepnoy.business-guide.com.ua/";
        Company scrapedCompany = null; // Variable to hold the scraped result

        System.out.println("Attempting to scrape: " + testUrl);
        try {
            // 1. Scrape the single company
            scrapedCompany = companyScraperService.scrapeCompanyPage(testUrl);

            // 2. Check if scraping was successful
            if (scrapedCompany != null) {
                System.out.println("--- Scraped Company Data ---");
                System.out.println(scrapedCompany); // Print to console as before
                System.out.println("----------------------------");

                // 3. Prepare the data for the Excel writer (needs a List)
                List<Company> companyList = new ArrayList<>();
                companyList.add(scrapedCompany); // Add the single successful scrape to the list

                // 4. Define the output file path (relative path -> project root)
                String outputFilePath = "scraped_company_test.xlsx";
                System.out.println("Attempting to write data to: " + outputFilePath);

                // 5. Call the Excel writer service
                // The try-catch inside writeCompaniesToExcel handles file writing errors
                excelWriterService.writeCompaniesToExcel(companyList, outputFilePath);

            } else {
                System.out.println("Scraping returned null for URL: " + testUrl + ". Cannot write to Excel.");
            }
        } catch (Exception e) {
            // Catch potential errors during scraping itself
            System.err.println("An error occurred during the scraping process: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("CommandLineRunner finished.");
        // Note: The catalog processing logic is not included here,
        // as we are just testing the single scrape + write.
    }

}
