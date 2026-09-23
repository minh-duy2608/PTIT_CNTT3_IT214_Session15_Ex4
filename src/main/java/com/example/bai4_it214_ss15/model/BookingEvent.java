package com.example.bai4_it214_ss15.model;

/**
 * Các sự kiện (Events) kích hoạt chuyển trạng thái trong Saga State Machine.
 */
public enum BookingEvent {
    PROCESS_PAYMENT,        // Bắt đầu xử lý thanh toán
    PAYMENT_SUCCESS,        // Thanh toán thành công
    PAYMENT_FAILED,         // Thanh toán thất bại
    RESERVE_SEATS,          // Bắt đầu giữ chỗ
    RESERVATION_SUCCESS,    // Giữ chỗ thành công
    RESERVATION_FAILED      // Giữ chỗ thất bại
}
