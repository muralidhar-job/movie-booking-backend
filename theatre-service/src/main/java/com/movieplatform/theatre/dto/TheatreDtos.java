package com.movieplatform.theatre.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TheatreDtos {

    @Data
    public static class OnboardRequest {
        @NotBlank private String theatreName;
        @NotBlank private String address;
        @NotBlank private String city;
        @NotBlank private String state;
        private String country = "India";
        private String gstNumber;
    }

    @Data
    public static class CreateShowRequest {
        @NotNull  private UUID movieId;
        @NotNull  private UUID screenId;
        @NotNull  private LocalDateTime showTime;
        private String showType = "REGULAR";
        private BigDecimal priceMultiplier = BigDecimal.ONE;
    }

    @Data
    public static class UpdateShowRequest {
        private LocalDateTime showTime;
        private Boolean isActive;
        private BigDecimal priceMultiplier;
    }

    @Data
    public static class SeatRequest {
        @NotBlank private String rowLabel;
        @NotNull  private Integer seatNumber;
        private String seatCategory = "REGULAR";
        @NotNull  private BigDecimal basePrice;
    }

    @Data
    public static class AllocateSeatsRequest {
        @NotNull private List<SeatRequest> seats;
    }
}
