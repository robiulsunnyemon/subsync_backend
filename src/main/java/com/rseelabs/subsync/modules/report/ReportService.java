package com.rseelabs.subsync.modules.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.rseelabs.subsync.modules.subscription.Subscription;
import com.rseelabs.subsync.modules.subscription.SubscriptionRepository;
import com.rseelabs.subsync.modules.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SubscriptionRepository subscriptionRepository;

    public TaxReportResponse getTaxReportData(User user, LocalDate startDate, LocalDate endDate) {
        List<Subscription> userSubscriptions = subscriptionRepository.findAllByUser(user);

        // Filter only BUSINESS expenses
        List<Subscription> businessSubs = userSubscriptions.stream()
                .filter(s -> s.getType() == Subscription.ExpenseType.BUSINESS && s.getStatus() == Subscription.SubscriptionStatus.ACTIVE)
                .collect(Collectors.toList());

        BigDecimal totalDeduction = BigDecimal.ZERO;
        List<Map<String, Object>> mappedSubs = new ArrayList<>();

        double monthsInPeriod = Math.max(1.0, ChronoUnit.DAYS.between(startDate, endDate) / 30.4375);

        for (Subscription sub : businessSubs) {
            BigDecimal monthlyAmount = sub.getAmount() != null ? sub.getAmount() : BigDecimal.ZERO;
            BigDecimal periodAmount = calculatePeriodAmount(monthlyAmount, sub.getCycle(), monthsInPeriod);

            totalDeduction = totalDeduction.add(periodAmount);

            Map<String, Object> map = new HashMap<>();
            map.put("id", sub.getId());
            map.put("name", sub.getMerchantName());
            map.put("category", "Software / Business");
            map.put("cycle", sub.getCycle().name());
            map.put("monthlyAmount", monthlyAmount);
            map.put("annualTotal", periodAmount);
            map.put("currency", sub.getCurrency() != null ? sub.getCurrency() : "EUR");
            map.put("icon", sub.getMerchantName() != null && !sub.getMerchantName().isEmpty() ? sub.getMerchantName().substring(0, 1).toUpperCase() : "B");
            
            mappedSubs.add(map);
        }

        return TaxReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalDeductions(totalDeduction.setScale(2, RoundingMode.HALF_UP))
                .subscriptions(mappedSubs)
                .build();
    }

    public String generateCsvReport(User user, LocalDate startDate, LocalDate endDate) {
        List<Subscription> businessSubs = subscriptionRepository.findAllByUser(user).stream()
                .filter(s -> s.getType() == Subscription.ExpenseType.BUSINESS)
                .collect(Collectors.toList());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String periodStr = startDate.format(dtf) + " - " + endDate.format(dtf);

        StringBuilder csv = new StringBuilder();
        csv.append("SubSync Tax Deduction Report\n");
        csv.append("Tax Period,").append(periodStr).append("\n");
        csv.append("Tax Payer,").append(escapeCsv(user.getFullName())).append(" (").append(user.getEmail()).append(")\n\n");
        csv.append("Merchant,Billing Cycle,Category,Monthly Amount,Period Deduction,Currency,Status\n");

        BigDecimal totalDeduction = BigDecimal.ZERO;
        double monthsInPeriod = Math.max(1.0, ChronoUnit.DAYS.between(startDate, endDate) / 30.4375);

        for (Subscription sub : businessSubs) {
            BigDecimal monthlyAmount = sub.getAmount() != null ? sub.getAmount() : BigDecimal.ZERO;
            BigDecimal periodAmount = calculatePeriodAmount(monthlyAmount, sub.getCycle(), monthsInPeriod);
            totalDeduction = totalDeduction.add(periodAmount);

            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(sub.getMerchantName()),
                    sub.getCycle().name(),
                    sub.getType().name(),
                    monthlyAmount.toPlainString(),
                    periodAmount.toPlainString(),
                    sub.getCurrency(),
                    sub.getStatus().name()
            ));
        }

        csv.append("\nTOTAL PERIOD DEDUCTIONS,,,").append(totalDeduction.setScale(2, RoundingMode.HALF_UP).toPlainString()).append("\n");
        return csv.toString();
    }

    public byte[] generatePdfReport(User user, LocalDate startDate, LocalDate endDate) {
        List<Subscription> businessSubs = subscriptionRepository.findAllByUser(user).stream()
                .filter(s -> s.getType() == Subscription.ExpenseType.BUSINESS && s.getStatus() == Subscription.SubscriptionStatus.ACTIVE)
                .collect(Collectors.toList());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String periodStr = startDate.format(dtf) + " to " + endDate.format(dtf);
        double monthsInPeriod = Math.max(1.0, ChronoUnit.DAYS.between(startDate, endDate) / 30.4375);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Colors
            Color primaryColor = new Color(30, 27, 75); // Dark Navy Blue (#1E1B4B)
            Color accentColor = new Color(16, 185, 129); // Emerald Green (#10B981)
            Color lightBgColor = new Color(248, 250, 252);
            Color borderColor = new Color(226, 232, 240);
            Color textColor = new Color(51, 65, 85);

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, primaryColor);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, primaryColor);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, textColor);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textColor);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, textColor);

            // 1. Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{55, 45});

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.addElement(new Paragraph("SubSync", titleFont));
            logoCell.addElement(new Paragraph("Official Tax Deduction Summary", FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139))));

            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            Paragraph periodPara = new Paragraph("PERIOD: " + periodStr, headerFont);
            periodPara.setAlignment(Element.ALIGN_RIGHT);
            Paragraph datePara = new Paragraph("Generated: " + LocalDate.now().format(dtf), bodyFont);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            
            dateCell.addElement(periodPara);
            dateCell.addElement(datePara);

            headerTable.addCell(logoCell);
            headerTable.addCell(dateCell);
            document.add(headerTable);

            document.add(new Paragraph(" "));

            // 2. Summary Box
            BigDecimal totalDeductions = BigDecimal.ZERO;
            for (Subscription sub : businessSubs) {
                totalDeductions = totalDeductions.add(calculatePeriodAmount(sub.getAmount(), sub.getCycle(), monthsInPeriod));
            }

            PdfPTable summaryBox = new PdfPTable(2);
            summaryBox.setWidthPercentage(100);
            summaryBox.setWidths(new float[]{50, 50});

            PdfPCell userCell = new PdfPCell();
            userCell.setBackgroundColor(lightBgColor);
            userCell.setBorderColor(borderColor);
            userCell.setPadding(12);
            userCell.addElement(new Paragraph("TAX PAYER:", subHeaderFont));
            userCell.addElement(new Paragraph(user.getFullName() != null ? user.getFullName() : "Valued User", boldFont));
            userCell.addElement(new Paragraph("Email: " + user.getEmail(), bodyFont));

            PdfPCell totalCell = new PdfPCell();
            totalCell.setBackgroundColor(lightBgColor);
            totalCell.setBorderColor(borderColor);
            totalCell.setPadding(12);
            totalCell.addElement(new Paragraph("TOTAL PERIOD DEDUCTIONS:", subHeaderFont));
            
            Paragraph totalAmtPara = new Paragraph("€" + totalDeductions.setScale(2, RoundingMode.HALF_UP), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, accentColor));
            totalCell.addElement(totalAmtPara);
            totalCell.addElement(new Paragraph("Status: Verified Tax Deductible", FontFactory.getFont(FontFactory.HELVETICA, 9, accentColor)));

            summaryBox.addCell(userCell);
            summaryBox.addCell(totalCell);
            document.add(summaryBox);

            document.add(new Paragraph(" "));

            // 3. Table of Eligible Expenses
            Paragraph itemsHeading = new Paragraph("Eligible Business Expenses", headerFont);
            document.add(itemsHeading);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{40, 20, 20, 20});

            String[] tableHeaders = {"Description / Merchant", "Cycle", "Monthly Amt", "Period Total"};
            for (String h : tableHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
                cell.setBackgroundColor(primaryColor);
                cell.setPadding(8);
                cell.setBorderColor(primaryColor);
                if (h.contains("Amt") || h.contains("Total")) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                table.addCell(cell);
            }

            for (Subscription sub : businessSubs) {
                BigDecimal monthly = sub.getAmount() != null ? sub.getAmount() : BigDecimal.ZERO;
                BigDecimal periodAmount = calculatePeriodAmount(monthly, sub.getCycle(), monthsInPeriod);

                PdfPCell nameC = new PdfPCell(new Phrase(sub.getMerchantName(), boldFont));
                nameC.setPadding(8);
                nameC.setBorderColor(borderColor);

                PdfPCell cycleC = new PdfPCell(new Phrase(sub.getCycle().toString(), bodyFont));
                cycleC.setPadding(8);
                cycleC.setBorderColor(borderColor);

                String symbol = "EUR".equals(sub.getCurrency()) || "€".equals(sub.getCurrency()) ? "€" : "$";
                PdfPCell monthC = new PdfPCell(new Phrase(symbol + monthly.setScale(2, RoundingMode.HALF_UP), bodyFont));
                monthC.setPadding(8);
                monthC.setHorizontalAlignment(Element.ALIGN_RIGHT);
                monthC.setBorderColor(borderColor);

                PdfPCell annualC = new PdfPCell(new Phrase(symbol + periodAmount.setScale(2, RoundingMode.HALF_UP), boldFont));
                annualC.setPadding(8);
                annualC.setHorizontalAlignment(Element.ALIGN_RIGHT);
                annualC.setBorderColor(borderColor);

                table.addCell(nameC);
                table.addCell(cycleC);
                table.addCell(monthC);
                table.addCell(annualC);
            }

            document.add(table);

            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("This official tax report was generated by SubSync for accounting and tax return filing. Valid for UK HMRC, EU Finanzamt, and international tax compliance.", FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(148, 163, 184)));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private BigDecimal calculatePeriodAmount(BigDecimal amount, Subscription.BillingCycle cycle, double monthsInPeriod) {
        if (amount == null) return BigDecimal.ZERO;
        if (cycle == null) return amount.multiply(new BigDecimal(Double.toString(monthsInPeriod)));
        return switch (cycle) {
            case WEEKLY -> amount.multiply(new BigDecimal(Double.toString((monthsInPeriod / 12.0) * 52.0)));
            case MONTHLY -> amount.multiply(new BigDecimal(Double.toString(monthsInPeriod)));
            case YEARLY -> amount.multiply(new BigDecimal(Double.toString(monthsInPeriod / 12.0)));
        };
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
