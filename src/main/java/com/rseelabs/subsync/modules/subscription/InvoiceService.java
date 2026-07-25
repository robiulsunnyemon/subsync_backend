package com.rseelabs.subsync.modules.subscription;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.rseelabs.subsync.modules.user.User;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {

    public byte[] generateInvoicePdf(Subscription subscription, User user) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Colors
            Color primaryColor = new Color(30, 27, 75); // Dark Navy Blue (#1E1B4B)
            Color accentColor = new Color(16, 185, 129); // Emerald Green (#10B981)
            Color lightBgColor = new Color(248, 250, 252); // Soft Light Blue Gray (#F8FAFC)
            Color borderColor = new Color(226, 232, 240); // Border Gray (#E2E8F0)
            Color textColor = new Color(51, 65, 85); // Slate Text (#334155)

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, primaryColor);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, textColor);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textColor);
            Font boldBodyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, textColor);
            Font badgeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(148, 163, 184));

            // --- 1. Header Table (App Name & Invoice Title) ---
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            Paragraph logoPara = new Paragraph("SubSync", titleFont);
            Paragraph logoSub = new Paragraph("Subscription & Expense Management", FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(100, 116, 139)));
            logoCell.addElement(logoPara);
            logoCell.addElement(logoSub);

            PdfPCell invoiceMetaCell = new PdfPCell();
            invoiceMetaCell.setBorder(Rectangle.NO_BORDER);
            invoiceMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            String subIdStr = subscription.getId().toString().replaceAll("-", "").toUpperCase();
            String invoiceNo = "INV-" + LocalDate.now().getYear() + "-" + subIdStr.substring(0, Math.min(8, subIdStr.length()));
            
            Paragraph invTitle = new Paragraph("TAX INVOICE", headerFont);
            invTitle.setAlignment(Element.ALIGN_RIGHT);
            Paragraph invNoPara = new Paragraph("Invoice No: " + invoiceNo, subHeaderFont);
            invNoPara.setAlignment(Element.ALIGN_RIGHT);
            
            String issueDate = subscription.getNextBillingDate() != null 
                    ? subscription.getNextBillingDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    : LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            Paragraph invDatePara = new Paragraph("Date: " + issueDate, bodyFont);
            invDatePara.setAlignment(Element.ALIGN_RIGHT);

            invoiceMetaCell.addElement(invTitle);
            invoiceMetaCell.addElement(invNoPara);
            invoiceMetaCell.addElement(invDatePara);

            headerTable.addCell(logoCell);
            headerTable.addCell(invoiceMetaCell);
            document.add(headerTable);

            document.add(new Paragraph(" "));

            // --- 2. Customer & Status Section ---
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50, 50});

            // Billed To Box
            PdfPCell billedToCell = new PdfPCell();
            billedToCell.setBackgroundColor(lightBgColor);
            billedToCell.setBorderColor(borderColor);
            billedToCell.setPadding(12);

            billedToCell.addElement(new Paragraph("BILLED TO:", subHeaderFont));
            billedToCell.addElement(new Paragraph(user.getFullName() != null ? user.getFullName() : "Valued Customer", boldBodyFont));
            billedToCell.addElement(new Paragraph("Email: " + user.getEmail(), bodyFont));
            if (user.getBusinessName() != null && !user.getBusinessName().isBlank()) {
                billedToCell.addElement(new Paragraph("Company: " + user.getBusinessName(), boldBodyFont));
            }
            if (user.getVatNumber() != null && !user.getVatNumber().isBlank()) {
                billedToCell.addElement(new Paragraph("VAT No: " + user.getVatNumber(), boldBodyFont));
            }
            billedToCell.addElement(new Paragraph("Account: Verified User", bodyFont));

            // Status & Payment Details Box
            PdfPCell statusCell = new PdfPCell();
            statusCell.setBackgroundColor(lightBgColor);
            statusCell.setBorderColor(borderColor);
            statusCell.setPadding(12);

            statusCell.addElement(new Paragraph("PAYMENT STATUS:", subHeaderFont));
            
            // Paid Badge
            PdfPTable badgeTable = new PdfPTable(1);
            badgeTable.setWidthPercentage(40);
            badgeTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            PdfPCell badgeInnerCell = new PdfPCell(new Phrase("PAID", badgeFont));
            badgeInnerCell.setBackgroundColor(accentColor);
            badgeInnerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeInnerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            badgeInnerCell.setPadding(4);
            badgeInnerCell.setBorder(Rectangle.NO_BORDER);
            badgeTable.addCell(badgeInnerCell);
            
            statusCell.addElement(badgeTable);
            statusCell.addElement(new Paragraph("Cycle: " + subscription.getCycle(), bodyFont));
            statusCell.addElement(new Paragraph("Category: " + subscription.getType(), bodyFont));

            infoTable.addCell(billedToCell);
            infoTable.addCell(statusCell);
            document.add(infoTable);

            document.add(new Paragraph(" "));

            // --- 3. Items Table ---
            PdfPTable itemTable = new PdfPTable(4);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{40, 20, 20, 20});

            // Table Header
            String[] headers = {"Description / Merchant", "Billing Cycle", "Category", "Amount"};
            for (String headerText : headers) {
                PdfPCell thCell = new PdfPCell(new Phrase(headerText, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
                thCell.setBackgroundColor(primaryColor);
                thCell.setPadding(8);
                thCell.setBorderColor(primaryColor);
                if (headerText.equals("Amount")) {
                    thCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                itemTable.addCell(thCell);
            }

            // Table Row
            PdfPCell itemDesc = new PdfPCell(new Phrase(subscription.getMerchantName(), boldBodyFont));
            itemDesc.setPadding(10);
            itemDesc.setBorderColor(borderColor);

            PdfPCell itemCycle = new PdfPCell(new Phrase(subscription.getCycle().toString(), bodyFont));
            itemCycle.setPadding(10);
            itemCycle.setBorderColor(borderColor);

            PdfPCell itemCat = new PdfPCell(new Phrase(subscription.getType().toString(), bodyFont));
            itemCat.setPadding(10);
            itemCat.setBorderColor(borderColor);

            String currencySymbol = "€".equals(subscription.getCurrency()) || "EUR".equals(subscription.getCurrency()) ? "€" : "$";
            BigDecimal totalAmount = subscription.getAmount() != null ? subscription.getAmount() : BigDecimal.ZERO;
            PdfPCell itemAmt = new PdfPCell(new Phrase(currencySymbol + totalAmount.setScale(2, RoundingMode.HALF_UP), boldBodyFont));
            itemAmt.setPadding(10);
            itemAmt.setHorizontalAlignment(Element.ALIGN_RIGHT);
            itemAmt.setBorderColor(borderColor);

            itemTable.addCell(itemDesc);
            itemTable.addCell(itemCycle);
            itemTable.addCell(itemCat);
            itemTable.addCell(itemAmt);

            document.add(itemTable);

            document.add(new Paragraph(" "));

            // --- 4. Summary Table ---
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.setWidths(new float[]{60, 40});

            BigDecimal vatRate = new BigDecimal("0.20"); // 20% VAT
            BigDecimal netAmount = totalAmount.divide(BigDecimal.ONE.add(vatRate), 2, RoundingMode.HALF_UP);
            BigDecimal vatAmount = totalAmount.subtract(netAmount);

            addSummaryRow(summaryTable, "Subtotal (Excl. VAT):", currencySymbol + netAmount, bodyFont, borderColor);
            addSummaryRow(summaryTable, "Estimated VAT (20%):", currencySymbol + vatAmount, bodyFont, borderColor);
            addSummaryRow(summaryTable, "Total Paid:", currencySymbol + totalAmount.setScale(2, RoundingMode.HALF_UP), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, primaryColor), borderColor);

            document.add(summaryTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // --- 5. Legal Footer ---
            Paragraph footerText = new Paragraph("This official tax receipt was generated automatically by SubSync for accounting, tax deduction, and expense claim compliance. For questions, contact support@subsync.app.", footerFont);
            footerText.setAlignment(Element.ALIGN_CENTER);
            document.add(footerText);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font, Color borderColor) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorderColor(borderColor);
        labelCell.setPadding(6);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorderColor(borderColor);
        valueCell.setPadding(6);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
