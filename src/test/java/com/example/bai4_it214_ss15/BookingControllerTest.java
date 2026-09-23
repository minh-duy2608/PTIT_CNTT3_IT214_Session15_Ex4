package com.example.bai4_it214_ss15;

import com.example.bai4_it214_ss15.controller.BookingController;
import com.example.bai4_it214_ss15.dto.BookingRequest;
import com.example.bai4_it214_ss15.model.BookingState;
import com.example.bai4_it214_ss15.model.BookingTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BookingControllerTest {

    @Autowired
    private BookingController bookingController;

    @Test
    @DisplayName("Controller Test: POST /api/bookings/process và GET /api/bookings/{bookingId}")
    void testBookingControllerEndpoints() {
        BookingRequest request = BookingRequest.builder()
                .bookingId("CONCERT-2026-CTRL-01")
                .concertCode("LIVE-HCM-2026-ULTRA")
                .customerId("VIP-2024")
                .customerEmail("rika@email.com")
                .ticketQuantity(3)
                .amount(5500000.0)
                .build();

        // 1. Kiểm tra endpoint xử lý đặt vé
        ResponseEntity<BookingTransaction> postResponse = bookingController.processBooking(request);

        assertEquals(HttpStatus.OK, postResponse.getStatusCode());
        assertNotNull(postResponse.getBody());
        assertEquals("CONCERT-2026-CTRL-01", postResponse.getBody().getBookingId());
        assertEquals(BookingState.BOOKING_CONFIRMED, postResponse.getBody().getCurrentState());
        assertTrue(postResponse.getBody().isPaymentSuccessful());
        assertTrue(postResponse.getBody().isReservationSuccessful());

        // 2. Kiểm tra endpoint truy vấn chi tiết giao dịch
        ResponseEntity<BookingTransaction> getResponse = bookingController.getBooking("CONCERT-2026-CTRL-01");

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals("CONCERT-2026-CTRL-01", getResponse.getBody().getBookingId());
        assertEquals(BookingState.BOOKING_CONFIRMED, getResponse.getBody().getCurrentState());

        // 3. Kiểm tra endpoint tra cứu riêng State
        ResponseEntity<BookingState> stateResponse = bookingController.getBookingState("CONCERT-2026-CTRL-01");

        assertEquals(HttpStatus.OK, stateResponse.getStatusCode());
        assertEquals(BookingState.BOOKING_CONFIRMED, stateResponse.getBody());
    }
}
