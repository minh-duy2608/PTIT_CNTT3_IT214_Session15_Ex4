package com.example.bai4_it214_ss15.controller;

import com.example.bai4_it214_ss15.dto.BookingRequest;
import com.example.bai4_it214_ss15.machine.ConcertBookingStateMachine;
import com.example.bai4_it214_ss15.model.BookingState;
import com.example.bai4_it214_ss15.model.BookingTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller tiếp nhận yêu cầu đặt vé và truy vấn trạng thái từ Orchestrator State Machine.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final ConcertBookingStateMachine stateMachine;

    @PostMapping("/process")
    public ResponseEntity<BookingTransaction> processBooking(@RequestBody BookingRequest request) {
        String bookingId = (request.getBookingId() != null && !request.getBookingId().isBlank())
                ? request.getBookingId()
                : "CONCERT-2026-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        BookingTransaction transaction = BookingTransaction.builder()
                .bookingId(bookingId)
                .concertCode(request.getConcertCode() != null ? request.getConcertCode() : "LIVE-HCM-2026-ULTRA")
                .customerId(request.getCustomerId() != null ? request.getCustomerId() : "VIP-2024")
                .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail() : "rika@email.com")
                .ticketQuantity(request.getTicketQuantity() != null ? request.getTicketQuantity() : 1)
                .amount(request.getAmount() != null ? request.getAmount() : 0.0)
                .simulatePaymentTimeoutAttempts(request.getSimulatePaymentTimeoutAttempts() != null ? request.getSimulatePaymentTimeoutAttempts() : 0)
                .simulatePaymentFailure(Boolean.TRUE.equals(request.getSimulatePaymentFailure()))
                .simulateReservationFailure(Boolean.TRUE.equals(request.getSimulateReservationFailure()))
                .build();

        stateMachine.process(transaction);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingTransaction> getBooking(@PathVariable String bookingId) {
        BookingTransaction transaction = stateMachine.getTransaction(bookingId);
        if (transaction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/{bookingId}/state")
    public ResponseEntity<BookingState> getBookingState(@PathVariable String bookingId) {
        BookingState state = stateMachine.getCurrentState(bookingId);
        if (state == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(state);
    }
}
