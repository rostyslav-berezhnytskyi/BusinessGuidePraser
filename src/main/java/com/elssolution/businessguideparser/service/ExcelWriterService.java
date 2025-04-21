package com.elssolution.businessguideparser.service;

import com.elssolution.businessguideparser.model.Company;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelWriterService {

    public void writeCompaniesToExcel(List<Company> companies, String filePath) {
        if (companies == null || companies.isEmpty()) {
            System.out.println("No companies to write to Excel.");
            return;
        }

        // Створюємо нову книгу Excel
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(filePath)) {
            // Створюємо аркуш
            Sheet sheet = workbook.createSheet("Companies");

            // Створюємо стиль для заголовків
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            // Створюємо рядок заголовків
            String[] headers = {"Company Name", "Address", "Postal Address", "Phone", "Contact Person", "Director", "Accountant", "Accountant Phone", "Reg Number", "Foundation Year", "Employees", "TIN", "Cert Number", "Website", "Email Link", "Source URL", "Description"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Заповнюємо дані
            int rowNum = 1;
            for (Company company : companies) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(company.getCompanyName());
                row.createCell(1).setCellValue(company.getAddress());
                row.createCell(2).setCellValue(company.getPostalAddress());
                row.createCell(3).setCellValue(company.getPhone());
                row.createCell(4).setCellValue(company.getContactPerson());
                row.createCell(5).setCellValue(company.getDirector());
                row.createCell(6).setCellValue(company.getAccountant());
                row.createCell(7).setCellValue(company.getAccountantPhone());
                row.createCell(8).setCellValue(company.getRegistrationNumber());
                row.createCell(9).setCellValue(company.getFoundationYear());
                row.createCell(10).setCellValue(company.getEmployeeCount());
                row.createCell(11).setCellValue(company.getTin());
                row.createCell(12).setCellValue(company.getCertificateNumber());
                row.createCell(13).setCellValue(company.getWebsite());
                row.createCell(14).setCellValue(company.getEmailLink());
                row.createCell(15).setCellValue(company.getSourceUrl());
                row.createCell(16).setCellValue(company.getDescription());
            }

            // Автоматично підлаштовуємо ширину колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Записуємо книгу у файл
            workbook.write(fileOut);
            System.out.println("Successfully written companies to " + filePath);

        } catch (IOException e) {
            System.err.println("Error writing to Excel file " + filePath + ": " + e.getMessage());
            // Обробити помилку запису
        }
    }
}
