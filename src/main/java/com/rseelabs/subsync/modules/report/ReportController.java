package com.rseelabs.subsync.modules.report;

import com.rseelabs.subsync.core.exception.ResourceNotFoundException;
import com.rseelabs.subsync.modules.user.User;
import com.rseelabs.subsync.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Reports", description = "Endpoints for downloading tax and expense reports")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    @GetMapping("/tax")
    public ResponseEntity<TaxReportResponse> getTaxReportData(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "2026") int year) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(reportService.getTaxReportData(user, year));
    }

    @GetMapping("/tax/csv")
    public ResponseEntity<String> downloadTaxReportCsv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "2026") int year) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        String csv = reportService.generateCsvReport(user, year);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "tax_report_" + year + ".csv");
        
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/tax/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadTaxReportPdf(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "2026") int year) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        byte[] pdfBytes = reportService.generatePdfReport(user, year);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "tax_report_" + year + ".pdf");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
