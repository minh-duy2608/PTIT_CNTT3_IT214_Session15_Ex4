package com.example.bai4_it214_ss15.model;

/**
 * Các trạng thái (States) của một giao dịch đặt vé sự kiện âm nhạc trong Saga State Machine.
 */
public enum BookingState {
    INITIATED,          // Khởi tạo yêu cầu đặt vé
    PAYMENT_PENDING,    // Đang xử lý thanh toán
    PAYMENT_COMPLETED,  // Thanh toán thành công
    SEAT_RESERVING,     // Đang giữ chỗ
    BOOKING_CONFIRMED,  // Đặt vé hoàn tất
    CANCELLED           // Hủy bỏ (bù trừ)
}
