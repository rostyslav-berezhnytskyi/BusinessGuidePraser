package com.elssolution.businessguideparser.service;

import com.elssolution.businessguideparser.formater.Formater;
import com.elssolution.businessguideparser.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.stream.Collectors;

@Service
public class CompanyScraperService {
    private final Formater formater;

    @Autowired
    public CompanyScraperService(Formater formater) {
        this.formater = formater;
    }

    public Company scrapeCompanyPage(String url) {
        try {
            // Завантажуємо HTML
            // Додайте userAgent та timeout для більшої надійності
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(5000) // 5 секунд
                    .get();

            // Витягуємо дані (приклади селекторів, потребують уточнення та тестування)
            String companyName = extractCompanyName(doc); // Або знайти h1/h2 з назвою
            String address = safeGetText(doc, "p[itemprop='address']");
            String postalAddress = findTextAfterHeader(doc, "Поштова адреса:");
            String phone = safeGetText(doc, "p[itemprop='telephone']").replace("<br>", ", "); // Може бути кілька телефонів
            String contactPerson = findTextAfterHeader(doc, "Контактна особа :");
            String director = findTextAfterHeader(doc, "Керівник:");
            String accountant = findTextAfterHeader(doc, "Бухгалтер:");
            String accountantPhone = findTextAfterHeader(doc, "Телефон бухгалтера:");
            String registrationNumber = findTextAfterHeader(doc, "Реєстраційний номер підприємства");
            String foundationYear = safeGetText(doc, "span[itemprop='foundingDate']");
            String employeeCount = extractValueFromLabelParagraph(doc, "Кількість працівників: "); // Ensure exact label with space
            String tin = extractValueFromLabelParagraph(doc, "ІПН: "); // Ensure exact label with space
            String certificateNumber = extractValueFromLabelParagraph(doc, "Номер свідоцтва: "); // Ensure exact label with space
            String websiteUrl = safeGetAttr(doc, "a[itemprop='url']", "href");
            String descriptionText = extractDescription(doc);

            // Очищення даних (видалення зайвих пробілів, тексту заголовків)
            director = cleanData(director, "- генеральний директор"); // Приклад очищення
            employeeCount = cleanData(employeeCount, "Кількість працівників:").trim();
            // ... додати інші очищення за потребою

            return Company.builder()
                    .companyName(companyName)
                    .address(address)
                    .postalAddress(postalAddress)
                    .phone(phone)
                    .contactPerson(contactPerson)
                    .director(director)
                    .accountant(accountant)
                    .accountantPhone(accountantPhone)
                    .registrationNumber(registrationNumber)
                    .foundationYear(foundationYear)
                    .employeeCount(employeeCount)
                    .tin(tin)
                    .certificateNumber(certificateNumber)
                    .website(websiteUrl)
                    .emailLink(null) // Збережіть повне посилання, якщо потрібно
                    .sourceUrl(url)
                    .description(descriptionText)
                    .build();

        } catch (IOException e) {
            System.err.println("Error scraping URL " + url + ": " + e.getMessage());
            // Повернути null або порожній об'єкт, або кинути виняток
            return null;
        }
    }

    private String extractDescription(Document doc) {
        String descriptionText = "";
        // Selector targets the <p> tag directly inside the <div class="kartkaBlockTxt">
        Element descriptionElement = doc.selectFirst("div.kartkaBlockTxt > p");

        if (descriptionElement != null) {
            // 1. Get HTML content to preserve <br> tags
            String descriptionHtml = descriptionElement.html();
            // 2. Replace <br> tags (case-insensitive) with a newline character (\n)
            String textWithNewlines = descriptionHtml.replaceAll("(?i)<br\\s*/?>", "\n");
            // 3. Parse the fragment and get clean text (handles entities, preserves \n)
            descriptionText = Jsoup.parse(textWithNewlines).text();
        } else {
            // Log a warning if the element wasn't found (optional but helpful)
            System.err.println("WARN: Could not find description element 'div.kartkaBlockTxt > p'");
            // descriptionText remains ""
        }
        return descriptionText;
    }

    // Допоміжна функція для безпечного отримання тексту
    private String safeGetText(Element doc, String cssSelector) {
        Elements elements = doc.select(cssSelector);
        if (elements.isEmpty()) {
            return ""; // Повернути порожній рядок, якщо елемент не знайдено
        }
        // Об'єднати текст з усіх знайдених елементів (на випадок кількох)
        return elements.stream().map(Element::text).collect(Collectors.joining(", ")).trim();
    }

    // Допоміжна функція для безпечного отримання атрибуту
    private String safeGetAttr(Element doc, String cssSelector, String attributeKey) {
        Element element = doc.selectFirst(cssSelector);
        return (element != null) ? element.attr(attributeKey) : "";
    }

    private String extractCompanyName(Document doc) {
        String h1Text = "";
        String h4Text = "";

        // 1. Select the specific container TD for the name elements
        Element nameContainer = doc.selectFirst("td.enterpriseNazva#ent_title");

        if (nameContainer != null) {
            // 2. Select H1 and H4 within the container
            Element h1Element = nameContainer.selectFirst("h1[itemprop=name]");
            Element h4Element = nameContainer.selectFirst("h4");

            // 3. Safely get text from H1 and H4
            if (h1Element != null) {
                h1Text = h1Element.text().trim();
            }
            if (h4Element != null) {
                h4Text = h4Element.text().trim(); // This will include the parentheses "(...)"
            }
        } else {
            System.err.println("WARN: Could not find name container 'td.enterpriseNazva#ent_title'. Searching globally for H1/H4.");
            // Fallback: Search globally if specific container not found (less reliable)
            Element h1Element = doc.selectFirst("h1[itemprop=name]");
            Element h4Element = doc.selectFirst("h4"); // Might grab the wrong h4 if others exist
            if (h1Element != null) h1Text = h1Element.text().trim();
            if (h4Element != null) h4Text = h4Element.text().trim();
        }


        // 4. Combine the extracted text
        StringBuilder combinedName = new StringBuilder();
        if (!h1Text.isEmpty()) {
            combinedName.append(h1Text);
        }
        if (!h4Text.isEmpty()) {
            if (combinedName.length() > 0) {
                combinedName.append(" "); // Add a space separator only if H1 text exists
            }
            combinedName.append(h4Text); // Append the H4 text (e.g., "(ПРАТ ПЛЕМЗАВОД СТЕПНОЙ)")
        }

        // 5. Final check and fallback to document title
        if (combinedName.length() > 0) {
            return combinedName.toString();
        } else {
            System.err.println("WARN: Failed to extract company name from H1/H4. Using document title as fallback.");
            return doc.title().trim(); // Use title as a last resort
        }
    }


    // Допоміжна функція для пошуку тексту ПІСЛЯ певного заголовка
    private String findTextAfterHeader(Document doc, String headerText) {
        // 1. Find a distinct element (p, h2, span) containing *only* the header text.
        //    This is key - it looks for label-only elements.
        Element headerElement = doc.select(
                "p:containsOwn(" + headerText + "), " +
                        "h2:containsOwn(" + headerText + "), " +
                        "span:containsOwn(" + headerText + ")"
        ).first(); // Get the first matching distinct label element

        // 2. If such a distinct header element was found...
        if (headerElement != null) {

            // Pattern A: Check the next sibling element, looking specifically for a <p> tag.
            Element sibling = headerElement.nextElementSibling();
            while (sibling != null && !sibling.tagName().equals("p")) { // Skip non-<p> siblings
                sibling = sibling.nextElementSibling();
            }
            if (sibling != null) {
                // Found the value in the next <p> sibling.
                return sibling.text().trim();
            }

            // Pattern B: Check if the header was a <span> inside a <p>,
            // and extract the text following the span within that parent <p>.
            if (headerElement.tagName().equals("span") && headerElement.parent() != null && headerElement.parent().tagName().equals("p")) {
                String parentText = headerElement.parent().text(); // Text of the parent <p>
                int index = parentText.indexOf(headerText);
                if (index != -1) {
                    // Extract substring after the header text.
                    String value = parentText.substring(index + headerText.length()).trim();
                    // Clean common leading colon often used after span labels
                    if (value.startsWith(":")) {
                        value = value.substring(1).trim();
                    }
                    return value;
                }
            }
            // If headerElement was found, but neither pattern A nor B matched the structure.
        }

        // 3. If no distinct header element was found OR if found but structure didn't match A or B.
        return "";
    }

    private String extractValueFromLabelParagraph(Document doc, String labelTextWithSpacing) {
        // Select all paragraph elements
        Elements paragraphs = doc.select("p");

        for (Element pElement : paragraphs) {
            String pText = pElement.text(); // Get the full text content of the <p> tag

            // Check if this specific paragraph's text starts with the exact label provided
            if (pText != null && pText.startsWith(labelTextWithSpacing)) {
                // If it matches, extract the substring immediately after the label text
                String value = pText.substring(labelTextWithSpacing.length()).trim();
                // Return the value found in THIS paragraph
                return value;
            }
        }

        // If loop finishes without finding a matching paragraph
        System.err.println("WARN: Could not find <p> starting with: '" + labelTextWithSpacing + "'");
        return "";
    }


    // Допоміжна функція для очищення даних
    private String cleanData(String data, String textToRemove) {
        if (data != null && textToRemove != null) {
            return data.replace(textToRemove, "").trim();
        }
        return data;
    }
}
