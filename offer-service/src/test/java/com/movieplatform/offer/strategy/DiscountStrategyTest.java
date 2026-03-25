package com.movieplatform.offer.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Strategy Pattern unit tests.
 * Tests each discount strategy independently — pure unit tests, no Spring context.
 */
@DisplayName("Discount Strategy tests")
class DiscountStrategyTest {

    private final ThirdTicketDiscountStrategy   thirdTicket  = new ThirdTicketDiscountStrategy();
    private final AfternoonShowDiscountStrategy afternoon    = new AfternoonShowDiscountStrategy();
    private final NoDiscountStrategy            noDiscount   = new NoDiscountStrategy();
    private static final BigDecimal BASE_PRICE = new BigDecimal("250.00");

    // ── THIRD50 strategy ─────────────────────────────────────────────────

    @Test
    @DisplayName("THIRD50: should give ₹125 discount for 3 tickets at ₹250 each")
    void thirdTicket_shouldGiveHalfPriceOnThirdTicket() {
        BigDecimal discount = thirdTicket.calculate(3, BASE_PRICE, null, List.of());
        assertThat(discount).isEqualByComparingTo("125.00"); // 50% of ₹250
    }

    @Test
    @DisplayName("THIRD50: should give ₹125 even for 5 tickets — only 3rd ticket discounted")
    void thirdTicket_shouldStillApplyForMoreThanThreeTickets() {
        BigDecimal discount = thirdTicket.calculate(5, BASE_PRICE, null, List.of());
        assertThat(discount).isEqualByComparingTo("125.00");
    }

    @Test
    @DisplayName("THIRD50: should give zero discount for fewer than 3 tickets")
    void thirdTicket_shouldNotApplyForLessThanThreeTickets() {
        assertThat(thirdTicket.calculate(1, BASE_PRICE, null, List.of()))
            .isEqualByComparingTo("0.00");
        assertThat(thirdTicket.calculate(2, BASE_PRICE, null, List.of()))
            .isEqualByComparingTo("0.00");
    }

    // ── AFTERNOON20 strategy ─────────────────────────────────────────────

    @ParameterizedTest(name = "hour={0} → isAfternoon={1}")
    @CsvSource({ "12,true", "14,true", "16,true", "11,false", "17,false", "20,false", "9,false" })
    @DisplayName("AFTERNOON20: should apply only within 12:00-16:59")
    void afternoon_shouldApplyOnlyInAfternoonWindow(int hour, boolean expectDiscount) {
        LocalDateTime showTime = LocalDateTime.of(2025, 3, 25, hour, 0);
        BigDecimal discount = afternoon.calculate(2, BASE_PRICE, showTime, List.of());

        if (expectDiscount) {
            // 20% of (2 × ₹250) = 20% of ₹500 = ₹100
            assertThat(discount).isEqualByComparingTo("100.00");
        } else {
            assertThat(discount).isEqualByComparingTo("0.00");
        }
    }

    @Test
    @DisplayName("AFTERNOON20: should return zero when showTime is null")
    void afternoon_shouldReturnZeroWhenShowTimeNull() {
        BigDecimal discount = afternoon.calculate(2, BASE_PRICE, null, List.of());
        assertThat(discount).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("AFTERNOON20: discount scales with ticket count")
    void afternoon_discountScalesWithTicketCount() {
        LocalDateTime afternoonShow = LocalDateTime.of(2025, 3, 25, 14, 0);
        BigDecimal oneTicket  = afternoon.calculate(1, BASE_PRICE, afternoonShow, List.of());
        BigDecimal fourTickets = afternoon.calculate(4, BASE_PRICE, afternoonShow, List.of());
        assertThat(oneTicket).isEqualByComparingTo("50.00");   // 20% of ₹250
        assertThat(fourTickets).isEqualByComparingTo("200.00"); // 20% of ₹1000
    }

    // ── NoDiscount strategy (Null Object) ────────────────────────────────

    @Test
    @DisplayName("NoDiscount: should always return zero")
    void noDiscount_shouldAlwaysReturnZero() {
        assertThat(noDiscount.calculate(10, new BigDecimal("999"), LocalDateTime.now(), List.of()))
            .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("NoDiscount: supports null and blank offer codes")
    void noDiscount_supportNullAndBlank() {
        assertThat(noDiscount.supports(null)).isTrue();
        assertThat(noDiscount.supports("")).isTrue();
        assertThat(noDiscount.supports("  ")).isTrue();
        assertThat(noDiscount.supports("THIRD50")).isFalse();
    }

    // ── Factory resolution ───────────────────────────────────────────────

    @Test
    @DisplayName("Factory: should resolve correct strategy for each offer code")
    void factory_shouldResolveCorrectStrategy() {
        DiscountStrategyFactory factory = new DiscountStrategyFactory(
            List.of(thirdTicket, afternoon, noDiscount), noDiscount);

        assertThat(factory.resolve("THIRD50").getOfferCode()).isEqualTo("THIRD50");
        assertThat(factory.resolve("AFTERNOON20").getOfferCode()).isEqualTo("AFTERNOON20");
        assertThat(factory.resolve("UNKNOWN").getStrategyName()).isEqualTo("NO_DISCOUNT");
        assertThat(factory.resolve(null).getStrategyName()).isEqualTo("NO_DISCOUNT");
    }
}
