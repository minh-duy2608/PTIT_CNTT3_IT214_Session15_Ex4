package com.example.bai4_it214_ss15.service;

import com.example.bai4_it214_ss15.model.BookingTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai nghiệp vụ Concert Reservation Service (Activity trong Saga).
 * Tuân thủ nguyên tắc: Không để State Machine tự gán ghế hay kiểm tra tồn kho.
 */
@Service
@Slf4j
public class ReservationOrchestrationServiceImpl implements ReservationOrchestrationService {

    @Override
    public boolean reserveSeats(BookingTransaction transaction) {
        log.info("[ReservationService] Reserving {} seats for concert {} (booking: {})",
                transaction.getTicketQuantity(), transaction.getConcertCode(), transaction.getBookingId());

        if (transaction.isSimulateReservationFailure()) {
            log.warn("[ReservationService] Seat reservation failed for booking {}: No available seats",
                    transaction.getBookingId());
            transaction.setReservationSuccessful(false);
            transaction.setFailureReason("No available seats for concert " + transaction.getConcertCode());
            return false;
        }

        List<String> seats = new ArrayList<>();
        for (int i = 1; i <= transaction.getTicketQuantity(); i++) {
            seats.add("VIP-ZONE-" + i);
        }
        transaction.setAssignedSeats(seats);
        transaction.setReservationSuccessful(true);

        log.info("[ReservationService] Successfully reserved seats {} for booking {}",
                seats, transaction.getBookingId());
        return true;
    }

    @Override
    public void cancelReservation(BookingTransaction transaction) {
        log.info("[ReservationService] Cancelling seat reservation for booking {}", transaction.getBookingId());
        transaction.getAssignedSeats().clear();
        transaction.setReservationSuccessful(false);
    }
}
