package com.example.bai4_it214_ss15.listener;

import com.example.bai4_it214_ss15.model.BookingEvent;
import com.example.bai4_it214_ss15.model.BookingState;

/**
 * Listener theo dõi và xử lý các sự kiện chuyển trạng thái trong State Machine.
 */
public interface StateChangeListener {
    void onStateChanged(String bookingId, BookingState oldState, BookingState newState, BookingEvent event);
}
