package com.movieplatform.theatre.service;

import com.movieplatform.theatre.dto.TheatreDtos.*;
import com.movieplatform.theatre.entity.*;
import com.movieplatform.theatre.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Theatre Service — B2B partner operations.
 *
 * Implements WRITE scenarios:
 *  - "Theatres can create, update, and delete shows for the day."
 *  - "Theatres can allocate seat inventory and update them for the show."
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TheatreService {

    private final TheatreRepository      theatreRepo;
    private final ScreenRepository       screenRepo;
    private final SeatLayoutRepository   seatRepo;
    private final TheatreShowRepository  showRepo;

    // ── Onboarding ──────────────────────────────────────────────────────

    @Transactional
    public TheatrePartner onboardTheatre(UUID userId, OnboardRequest req) {
        TheatrePartner theatre = TheatrePartner.builder()
            .userId(userId)
            .theatreName(req.getTheatreName())
            .address(req.getAddress())
            .city(req.getCity())
            .state(req.getState())
            .country(req.getCountry())
            .gstNumber(req.getGstNumber())
            .status(TheatrePartner.OnboardingStatus.PENDING_VERIFICATION)
            .build();
        TheatrePartner saved = theatreRepo.save(theatre);
        log.info("Theatre onboarded: id={} name={} userId={}", saved.getId(), saved.getTheatreName(), userId);
        return saved;
    }

    @Transactional
    public Screen addScreen(UUID theatreId, String screenName, int totalSeats, String screenType) {
        TheatrePartner theatre = getTheatreById(theatreId);
        Screen screen = Screen.builder()
            .theatre(theatre)
            .screenName(screenName)
            .totalSeats(totalSeats)
            .screenType(Screen.ScreenType.valueOf(screenType.toUpperCase()))
            .build();
        return screenRepo.save(screen);
    }

    // ── Seat Inventory (WRITE SCENARIO) ─────────────────────────────────

    @Transactional
    public List<SeatLayout> allocateSeats(UUID screenId, AllocateSeatsRequest req) {
        Screen screen = screenRepo.findById(screenId)
            .orElseThrow(() -> new RuntimeException("Screen not found: " + screenId));

        List<SeatLayout> seats = req.getSeats().stream().map(s ->
            SeatLayout.builder()
                .screen(screen)
                .rowLabel(s.getRowLabel())
                .seatNumber(s.getSeatNumber())
                .seatCategory(SeatLayout.SeatCategory.valueOf(s.getSeatCategory().toUpperCase()))
                .basePrice(s.getBasePrice())
                .build()
        ).collect(Collectors.toList());

        List<SeatLayout> saved = seatRepo.saveAll(seats);
        log.info("Allocated {} seats for screenId={}", saved.size(), screenId);
        return saved;
    }

    // ── Show Management (WRITE SCENARIO) ────────────────────────────────

    @Transactional
    public TheatreShow createShow(UUID theatreId, CreateShowRequest req) {
        Screen screen = screenRepo.findById(req.getScreenId())
            .orElseThrow(() -> new RuntimeException("Screen not found"));

        int totalSeats = (int) seatRepo.countByScreenId(req.getScreenId());

        TheatreShow show = TheatreShow.builder()
            .theatreId(theatreId)
            .screenId(req.getScreenId())
            .movieId(req.getMovieId())
            .showTime(req.getShowTime())
            .showType(TheatreShow.ShowType.valueOf(req.getShowType().toUpperCase()))
            .priceMultiplier(req.getPriceMultiplier())
            .totalSeats(totalSeats)
            .availableSeats(totalSeats)
            .build();

        TheatreShow saved = showRepo.save(show);
        log.info("Show created: showId={} theatreId={} movieId={} time={}",
            saved.getId(), theatreId, req.getMovieId(), req.getShowTime());
        return saved;
    }

    @Transactional
    public TheatreShow updateShow(UUID theatreId, UUID showId, UpdateShowRequest req) {
        TheatreShow show = showRepo.findById(showId)
            .orElseThrow(() -> new RuntimeException("Show not found: " + showId));

        if (!show.getTheatreId().equals(theatreId)) {
            throw new RuntimeException("Show does not belong to this theatre");
        }

        if (req.getShowTime()       != null) show.setShowTime(req.getShowTime());
        if (req.getIsActive()       != null) show.setActive(req.getIsActive());
        if (req.getPriceMultiplier()!= null) show.setPriceMultiplier(req.getPriceMultiplier());

        TheatreShow updated = showRepo.save(show);
        log.info("Show updated: showId={}", showId);
        return updated;
    }

    @Transactional
    public void deleteShow(UUID theatreId, UUID showId) {
        TheatreShow show = showRepo.findById(showId)
            .orElseThrow(() -> new RuntimeException("Show not found: " + showId));

        if (!show.getTheatreId().equals(theatreId)) {
            throw new RuntimeException("Show does not belong to this theatre");
        }
        show.setActive(false);  // soft delete
        showRepo.save(show);
        log.info("Show soft-deleted: showId={}", showId);
    }

    @Transactional(readOnly = true)
    public List<TheatreShow> getShowsForTheatre(UUID theatreId) {
        return showRepo.findByTheatreIdAndActiveTrue(theatreId);
    }

    @Transactional(readOnly = true)
    public List<TheatrePartner> getTheatresByCity(String city) {
        return theatreRepo.findByCityIgnoreCaseAndActiveTrue(city);
    }

    @Transactional(readOnly = true)
    public TheatrePartner getTheatreById(UUID theatreId) {
        return theatreRepo.findById(theatreId)
            .orElseThrow(() -> new RuntimeException("Theatre not found: " + theatreId));
    }

    @Transactional(readOnly = true)
    public List<SeatLayout> getSeatsForScreen(UUID screenId) {
        return seatRepo.findByScreenId(screenId);
    }
}
