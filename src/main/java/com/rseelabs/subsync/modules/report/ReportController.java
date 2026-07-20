package com.rseelabs.subsync.modules.report;

import com.rseelabs.subsync.modules.subscription.Subscription;
import com.rseelabs.subsync.modules.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/tax/csv")
    public ResponseEntity<String> downloadTaxReportCsv(@AuthenticationPrincipal User user) {
        
        // TODO: In a real implementation, you would inject a SubscriptionRepository
        // and fetch subscriptions by userId and type=BUSINESS
        List<Subscription> businessSubscriptions = Collections.emptyList(); 
        
        String csv = reportService.generateCsvReport(user, businessSubscriptions);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "tax_report.csv");
        
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    @GetMapping("/tax/pdf")
    public ResponseEntity<byte[]> downloadTaxReportPdf(@AuthenticationPrincipal User user) {
        
        // TODO: Same as above, fetch real subscriptions
        List<Subscription> businessSubscriptions = Collections.emptyList(); 
        
        byte[] pdfBytes = reportService.generatePdfReport(user, businessSubscriptions);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "tax_report.pdf");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
