package com.travel.loginregistration.service;

import com.travel.loginregistration.dto.DestinationCard;
import com.travel.loginregistration.dto.DestinationRequest;
import com.travel.loginregistration.model.Destination;
import com.travel.loginregistration.repository.DestinationRepository;
import com.travel.loginregistration.repository.TravelPackageRepository;
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

    public DestinationService(DestinationRepository destinationRepository,
                              TravelPackageRepository travelPackageRepository) {
        this.destinationRepository = destinationRepository;
        this.travelPackageRepository = travelPackageRepository;
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

    private DestinationCard toCard(Destination dest) {
        DestinationCard card = new DestinationCard();
        card.setId(dest.getId());
        card.setName(dest.getName());
        card.setRegion(dest.getRegion());
        card.setTags(dest.getTags());
        card.setBestSeason(dest.getBestSeason());
        card.setImageUrl(dest.getImageUrl());
        card.setHotelsCount(dest.getHotelsCount() == null ? 0 : dest.getHotelsCount());
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
        dest.setHotelsCount(req.getHotelsCount() != null ? req.getHotelsCount() : 0);
        if (req.getActive() != null) dest.setActive(req.getActive());
    }
}
