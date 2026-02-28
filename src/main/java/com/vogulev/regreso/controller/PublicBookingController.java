package com.vogulev.regreso.controller;

import com.vogulev.regreso.dto.request.PublicBookingRequest;
import com.vogulev.regreso.dto.response.AvailableSlotsResponse;
import com.vogulev.regreso.dto.response.PublicBookingConfirmation;
import com.vogulev.regreso.dto.response.PublicBookingPageResponse;
import com.vogulev.regreso.service.PublicBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/booking")
@RequiredArgsConstructor
public class PublicBookingController {

    private final PublicBookingService publicBookingService;

    @GetMapping("/{slug}")
    public ResponseEntity<PublicBookingPageResponse> getBookingPage(@PathVariable String slug) {
        return ResponseEntity.ok(publicBookingService.getBookingPage(slug));
    }

    @GetMapping("/{slug}/slots")
    public ResponseEntity<AvailableSlotsResponse> getAvailableSlots(
            @PathVariable String slug,
            @RequestParam String date) {
        return ResponseEntity.ok(publicBookingService.getAvailableSlots(slug, date));
    }

    @PostMapping("/{slug}")
    public ResponseEntity<PublicBookingConfirmation> createBooking(
            @PathVariable String slug,
            @RequestBody PublicBookingRequest request) {
        return ResponseEntity.ok(publicBookingService.createBooking(slug, request));
    }
}
