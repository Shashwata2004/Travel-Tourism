package com.travel.loginregistration.controller;

import com.travel.loginregistration.dto.PackageDetails;
import com.travel.loginregistration.dto.PackageSummary;
import com.travel.loginregistration.service.PackageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/*
 *   Handles package-related API requests from frontend, delegating to PackageService.
 *   list all active packages and get details of specific package by ID from PackageService.
 *   the packageservice interacts with the database to fetch data.
 */

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

