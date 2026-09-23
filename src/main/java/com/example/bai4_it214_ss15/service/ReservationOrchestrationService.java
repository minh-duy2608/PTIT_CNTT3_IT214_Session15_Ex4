package com.example.bai4_it214_ss15.service;

import com.example.bai4_it214_ss15.model.BookingTransaction;

/**
 * Service xử lý giữ chỗ và gán ghế cho sự kiện âm nhạc.
 */
public interface ReservationOrchestrationService {
    boolean reserveSeats(BookingTransaction transaction);
    void cancelReservation(BookingTransaction transaction);
}
