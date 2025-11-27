package com.travel.loginregistration.service;

import com.travel.loginregistration.dto.DestinationCard;
import com.travel.loginregistration.dto.DestinationRequest;
import com.travel.loginregistration.dto.HotelDetails;
import com.travel.loginregistration.dto.HotelSummary;
import com.travel.loginregistration.model.Destination;
import com.travel.loginregistration.model.Hotel;
import com.travel.loginregistration.repository.DestinationRepository;
import com.travel.loginregistration.repository.TravelPackageRepository;
import com.travel.loginregistration.repository.HotelRoomRepository;
import com.travel.loginregistration.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DestinationService {
    private final DestinationRepository destinationRepository;
    private final TravelPackageRepository travelPackageRepository;
    private final HotelRepository hotelRepository;
    private final HotelRoomRepository roomRepository;

    public DestinationService(DestinationRepository destinationRepository,
                              TravelPackageRepository travelPackageRepository,
                              HotelRepository hotelRepository,
                              HotelRoomRepository roomRepository) {
        this.destinationRepository = destinationRepository;
        this.travelPackageRepository = travelPackageRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    public List<DestinationCard> list(String search) {
        List<Destination> items = StringUtils.hasText(search)
                ? destinationRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(search.trim())
                : destinationRepository.findByActiveTrueOrderByNameAsc();
        return items.stream().map(this::toCard).collect(Collectors.toList());
    }

    public DestinationCard create(DestinationRequest req) {
        Destination dest = new Destination();
        apply(dest, req);
        Destination saved = destinationRepository.save(dest);
        return toCard(saved);
    }

    public DestinationCard update(UUID id, DestinationRequest req) {
        Destination dest = destinationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("DEST_NOT_FOUND"));
        apply(dest, req);
        Destination saved = destinationRepository.save(dest);
        return toCard(saved);
    }

    public void delete(UUID id) {
        if (destinationRepository.existsById(id)) {
            destinationRepository.deleteById(id);
        }
    }

    public long countHotels(UUID destinationId) {
        return hotelRepository.countByDestinationId(destinationId);
    }

    public List<HotelSummary> listHotels(UUID destinationId) {
        return hotelRepository.findByDestinationIdOrderByNameAsc(destinationId).stream()
                .map(this::toHotelSummary)
                .collect(Collectors.toList());
    }

    public HotelDetails getHotelDetails(UUID hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new IllegalArgumentException("HOTEL_NOT_FOUND"));
        HotelDetails d = new HotelDetails();
        d.id = hotel.getId();
        d.name = hotel.getName();
        d.rating = hotel.getRating();
        d.location = hotel.getLocation();
        d.images = java.util.stream.Stream.of(hotel.getImage1(), hotel.getImage2(), hotel.getImage3(), hotel.getImage4(), hotel.getImage5())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
        String nearby = hotel.getNearby();
        d.nearby = nearby == null || nearby.isBlank() ? List.of()
                : java.util.Arrays.stream(nearby.split("\\."))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.matches("\\d+")) // drop stray numeric-only entries like "0" or "23"
                    .collect(Collectors.toList());
        String fac = hotel.getFacilities();
        d.facilities = fac == null || fac.isBlank()
                ? java.util.List.of()
                : java.util.Arrays.stream(fac.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        return d;
    }

    private HotelSummary toHotelSummary(Hotel h) {
        HotelSummary hs = new HotelSummary();
        hs.id = h.getId();
        hs.name = h.getName();
        hs.rating = h.getRating();
        hs.location = h.getLocation();
        hs.image = h.getImage1();
        hs.realPrice = h.getRealPrice();
        hs.currentPrice = h.getCurrentPrice();
        hs.availableRooms = roomRepository.sumAvailableByHotel(h.getId());
        String fac = h.getFacilities();
        hs.facilities = fac == null || fac.isBlank()
                ? java.util.List.of()
                : java.util.Arrays.stream(fac.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        return hs;
    }

    private DestinationCard toCard(Destination dest) {
        DestinationCard card = new DestinationCard();
        card.setId(dest.getId());
        card.setName(dest.getName());
        card.setRegion(dest.getRegion());
        card.setTags(dest.getTags());
        card.setBestSeason(dest.getBestSeason());
        card.setImageUrl(dest.getImageUrl());
        card.setHotelsCount((int) hotelRepository.countByDestinationId(dest.getId()));
        Optional<com.travel.loginregistration.model.TravelPackage> match =
                travelPackageRepository.findFirstByLocationIgnoreCaseAndActiveTrueOrderByNameAsc(dest.getName());
        card.setPackageAvailable(match.isPresent());
        match.ifPresent(pkg -> card.setPackageId(pkg.getId()));
        card.setActive(dest.isActive());
        return card;
    }

    private void apply(Destination dest, DestinationRequest req) {
        if (!StringUtils.hasText(req.getName())) throw new IllegalArgumentException("NAME_REQUIRED");
        if (!StringUtils.hasText(req.getRegion())) throw new IllegalArgumentException("REGION_REQUIRED");
        dest.setName(req.getName().trim());
        dest.setRegion(req.getRegion().trim());
        dest.setTags(req.getTags());
        dest.setBestSeason(req.getBestSeason());
        dest.setImageUrl(req.getImageUrl());
        if (req.getActive() != null) dest.setActive(req.getActive());
    }
}
