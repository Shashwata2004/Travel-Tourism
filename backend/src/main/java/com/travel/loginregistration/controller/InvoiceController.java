package com.travel.loginregistration.controller;

import com.travel.loginregistration.service.InvoiceService;
import com.travel.loginregistration.service.InvoiceService.BookingKind;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/history")
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/invoice/{kind}/{id}")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable("kind") String kind,
                                                  @PathVariable("id") UUID id,
                                                  Authentication auth) {
        String email = auth == null ? null : (String) auth.getPrincipal();
        BookingKind bookingKind = BookingKind.from(kind);
        byte[] pdf = invoiceService.createInvoice(bookingKind, id, email);
        String filename = "Travel-Tourism-Invoice-" + id.toString().replace("-", "").substring(0, Math.min(8, id.toString().length())) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
