package com.example.bai4_it214_ss15.listener;

import com.example.bai4_it214_ss15.model.BookingEvent;
import com.example.bai4_it214_ss15.model.BookingState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Triển khai mặc định của StateChangeListener để ghi nhận và giám sát chuyển đổi trạng thái.
 */
@Component
@Slf4j
public class DefaultStateChangeListener implements StateChangeListener {

    @Override
    public void onStateChanged(String bookingId, BookingState oldState, BookingState newState, BookingEvent event) {
        log.debug("[StateChangeListener] Booking {} transitioned from {} to {} on event {}",
                bookingId, oldState, newState, event);
    }
}
