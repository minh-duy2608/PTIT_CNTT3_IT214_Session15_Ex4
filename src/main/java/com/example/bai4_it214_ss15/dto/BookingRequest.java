package com.example.bai4_it214_ss15.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận yêu cầu đặt vé sự kiện âm nhạc từ client / API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    private String bookingId;
    private String concertCode;
    private String customerId;
    private String customerEmail;
    private Integer ticketQuantity;
    private Double amount;

    // Các cờ hỗ trợ mô phỏng kiểm thử
    private Integer simulatePaymentTimeoutAttempts;
    private Boolean simulatePaymentFailure;
    private Boolean simulateReservationFailure;
}
