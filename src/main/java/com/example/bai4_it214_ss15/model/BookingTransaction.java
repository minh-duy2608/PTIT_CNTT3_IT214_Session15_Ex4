package com.example.bai4_it214_ss15.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity / Model chứa dữ liệu giao dịch đặt vé sự kiện âm nhạc và trạng thái Saga hiện tại.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingTransaction {

    private String bookingId;
    private String concertCode;
    private String customerId;
    private String customerEmail;
    private int ticketQuantity;
    private double amount;

    @Builder.Default
    private BookingState currentState = BookingState.INITIATED;

    @Builder.Default
    private boolean paymentSuccessful = false;

    @Builder.Default
    private boolean reservationSuccessful = false;

    @Builder.Default
    private boolean refunded = false;

    @Builder.Default
    private List<String> assignedSeats = new ArrayList<>();

    private String failureReason;

    // Các cờ hỗ trợ mô phỏng kiểm thử kịch bản timeout/lỗi
    @Builder.Default
    private int simulatePaymentTimeoutAttempts = 0;

    @Builder.Default
    private boolean simulatePaymentFailure = false;

    @Builder.Default
    private boolean simulateReservationFailure = false;

    @Builder.Default
    private int paymentAttemptCount = 0;

    @Builder.Default
    private List<String> history = new ArrayList<>();

    public void addHistory(String entry) {
        if (this.history == null) {
            this.history = new ArrayList<>();
        }
        this.history.add(entry);
    }
}
