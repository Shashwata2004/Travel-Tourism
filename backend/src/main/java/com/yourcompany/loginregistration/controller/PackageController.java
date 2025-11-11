package com.yourcompany.loginregistration.controller;

import com.yourcompany.loginregistration.dto.PackageDetails;
import com.yourcompany.loginregistration.dto.PackageSummary;
import com.yourcompany.loginregistration.service.PackageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/packages")
public class PackageController {
    private final PackageService service;

    public PackageController(PackageService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PackageSummary>> list() {
        return ResponseEntity.ok(service.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackageDetails> details(@PathVariable UUID id) {
        return ResponseEntity.ok(service.details(id));
    }
}

