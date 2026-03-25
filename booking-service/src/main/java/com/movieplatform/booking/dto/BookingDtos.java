package com.movieplatform.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.UUID;

public class BookingDtos {

    @Data
    public static class BookingRequest {
        @NotNull  private UUID showId;
        @NotEmpty private List<UUID> seatLayoutIds;
        private String offerCode;
        private String idempotencyKey; // client provides for dedup
    }

    @Data
    public static class BulkBookingRequest {
        @NotNull  private UUID showId;
        @NotEmpty private List<SingleBooking> bookings;

        @Data
        public static class SingleBooking {
            @NotEmpty private List<UUID> seatLayoutIds;
            @NotNull  private UUID userId;
        }
    }
}
