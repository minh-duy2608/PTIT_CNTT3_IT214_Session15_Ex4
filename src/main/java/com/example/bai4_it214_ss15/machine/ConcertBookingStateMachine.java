package com.example.bai4_it214_ss15.machine;

import com.example.bai4_it214_ss15.model.BookingState;
import com.example.bai4_it214_ss15.model.BookingTransaction;

/**
 * Interface điều phối State Machine cho giao dịch đặt vé sự kiện âm nhạc.
 */
public interface ConcertBookingStateMachine {

    /**
     * Bắt đầu hoặc tiếp tục tiến trình xử lý giao dịch đặt vé.
     */
    void process(BookingTransaction transaction);

    /**
     * Lấy trạng thái hiện tại của một transaction theo bookingId.
     */
    BookingState getCurrentState(String bookingId);

    /**
     * Lấy thông tin giao dịch theo bookingId.
     */
    BookingTransaction getTransaction(String bookingId);
}
