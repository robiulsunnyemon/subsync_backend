package com.rseelabs.subsync.modules.report;

import com.rseelabs.subsync.modules.subscription.Subscription;
import com.rseelabs.subsync.modules.user.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReportService {

    public String generateCsvReport(User user, List<Subscription> businessSubscriptions) {
        StringBuilder csv = new StringBuilder();
        csv.append("Merchant,Amount,Currency,Cycle,Next_Billing_Date,Status\n");
        
        BigDecimal total = BigDecimal.ZERO;

        for (Subscription sub : businessSubscriptions) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(sub.getMerchantName()),
                    sub.getAmount().toPlainString(),
                    sub.getCurrency(),
                    sub.getCycle().name(),
                    sub.getNextBillingDate().toString(),
                    sub.getStatus().name()
            ));
            total = total.add(sub.getAmount());
        }

        csv.append("\nTOTAL BUSINESS DEDUCTIONS,,,").append(total.toPlainString()).append("\n");
        return csv.toString();
    }

    public byte[] generatePdfReport(User user, List<Subscription> businessSubscriptions) {
        // TODO: In a real implementation, use iText, OpenPDF, or JasperReports
        // to generate a formatted PDF based on the Figma design.
        // For now, we return a simple byte array stub.
        String stubText = "PDF Report Stub\nTotal Subscriptions: " + businessSubscriptions.size();
        return stubText.getBytes();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
