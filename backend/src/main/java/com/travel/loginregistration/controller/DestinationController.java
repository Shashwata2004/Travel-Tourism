package com.travel.loginregistration.controller;

import com.travel.loginregistration.dto.DestinationCard;
import com.travel.loginregistration.dto.DestinationRequest;
import com.travel.loginregistration.service.DestinationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationService service;

    public DestinationController(DestinationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DestinationCard>> list(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.list(search));
    }

    @PostMapping
    public ResponseEntity<DestinationCard> create(@RequestBody DestinationRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DestinationCard> update(@PathVariable UUID id, @RequestBody DestinationRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/hotels/count")
    public ResponseEntity<Long> hotelCount(@PathVariable UUID id) {
        return ResponseEntity.ok(service.countHotels(id));
    }

    @GetMapping("/{id}/hotels")
    public ResponseEntity<List<com.travel.loginregistration.dto.HotelSummary>> hotels(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listHotels(id));
    }

    @GetMapping("/hotels/{hotelId}")
    public ResponseEntity<com.travel.loginregistration.dto.HotelDetails> hotel(@PathVariable UUID hotelId) {
        return ResponseEntity.ok(service.getHotelDetails(hotelId));
    }
}
