package com.example.bai4_it214_ss15;

import com.example.bai4_it214_ss15.listener.StateChangeListener;
import com.example.bai4_it214_ss15.machine.ConcertBookingStateMachine;
import com.example.bai4_it214_ss15.machine.ConcertBookingStateMachineImpl;
import com.example.bai4_it214_ss15.model.BookingState;
import com.example.bai4_it214_ss15.model.BookingTransaction;
import com.example.bai4_it214_ss15.model.RetryPolicy;
import com.example.bai4_it214_ss15.service.PaymentOrchestrationService;
import com.example.bai4_it214_ss15.service.ReservationOrchestrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class Bai4It214Ss15ApplicationTests {

    @Autowired
    private ConcertBookingStateMachine stateMachine;

    @Autowired
    private PaymentOrchestrationService paymentService;

    @Autowired
    private ReservationOrchestrationService reservationService;

    @Autowired
    private StateChangeListener stateChangeListener;

    @Test
    @DisplayName("Kịch bản 1: Đặt vé sự kiện âm nhạc thành công hoàn toàn (CONCERT-2026-088)")
    void testSuccessfulConcertBookingFlow() {
        BookingTransaction transaction = BookingTransaction.builder()
                .bookingId("CONCERT-2026-088")
                .concertCode("LIVE-HCM-2026-ULTRA")
                .customerId("VIP-2024")
                .customerEmail("rika@email.com")
                .ticketQuantity(3)
                .amount(5500000.0)
                .build();

        stateMachine.process(transaction);

        // Kiểm tra trạng thái cuối cùng
        assertEquals(BookingState.BOOKING_CONFIRMED, transaction.getCurrentState());
        assertEquals(BookingState.BOOKING_CONFIRMED, stateMachine.getCurrentState("CONCERT-2026-088"));
        assertTrue(transaction.isPaymentSuccessful());
        assertTrue(transaction.isReservationSuccessful());
        assertFalse(transaction.isRefunded());
        assertEquals(3, transaction.getAssignedSeats().size());

        // Kiểm tra lịch sử log chuyển trạng thái khớp hoàn toàn kỳ vọng
        List<String> history = transaction.getHistory();
        assertNotNull(history);

        assertTrue(history.contains("[Orchestrator] State: INITIATED -> Event: PROCESS_PAYMENT -> New State: PAYMENT_PENDING"));
        assertTrue(history.contains("[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt 1/3"));
        assertTrue(history.contains("[Orchestrator] State: PAYMENT_PENDING -> Event: PAYMENT_SUCCESS -> New State: PAYMENT_COMPLETED"));
        assertTrue(history.contains("[Orchestrator] State: PAYMENT_COMPLETED -> Event: RESERVE_SEATS -> New State: SEAT_RESERVING"));
        assertTrue(history.contains("[Orchestrator] State: SEAT_RESERVING -> Event: RESERVATION_SUCCESS -> New State: BOOKING_CONFIRMED"));
        assertTrue(history.contains("[Orchestrator] Final State: BOOKING_CONFIRMED for booking CONCERT-2026-088"));
    }

    @Test
    @DisplayName("Kịch bản 2: Retry Policy xử lý timeout 2 lần và thành công ở lần thứ 3")
    void testPaymentRetryPolicyTransientTimeoutSuccess() {
        // Sử dụng RetryPolicy với delay 10ms để test nhanh
        ConcertBookingStateMachine fastStateMachine = new ConcertBookingStateMachineImpl(
                paymentService, reservationService, stateChangeListener, new RetryPolicy(3, 10));

        BookingTransaction transaction = BookingTransaction.builder()
                .bookingId("CONCERT-RETRY-001")
                .concertCode("LIVE-HCM-2026-ULTRA")
                .customerId("VIP-2025")
                .customerEmail("vip2025@email.com")
                .ticketQuantity(2)
                .amount(3000000.0)
                .simulatePaymentTimeoutAttempts(2) // Lần 1 và 2 timeout, lần 3 thành công
                .build();

        fastStateMachine.process(transaction);

        assertEquals(BookingState.BOOKING_CONFIRMED, transaction.getCurrentState());
        assertTrue(transaction.isPaymentSuccessful());
        assertEquals(3, transaction.getPaymentAttemptCount());

        List<String> history = transaction.getHistory();
        assertTrue(history.contains("[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt 1/3"));
        assertTrue(history.contains("[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt 2/3"));
        assertTrue(history.contains("[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt 3/3"));
    }

    @Test
    @DisplayName("Kịch bản 3: Thanh toán timeout vượt quá 3 lần -> Chuyển CANCELLED")
    void testPaymentFailedAfterRetryExhausted() {
        ConcertBookingStateMachine fastStateMachine = new ConcertBookingStateMachineImpl(
                paymentService, reservationService, stateChangeListener, new RetryPolicy(3, 10));

        BookingTransaction transaction = BookingTransaction.builder()
                .bookingId("CONCERT-FAIL-PAY")
                .concertCode("LIVE-HCM-2026-ULTRA")
                .customerId("VIP-FAIL")
                .customerEmail("fail@email.com")
                .ticketQuantity(1)
                .amount(1000000.0)
                .simulatePaymentTimeoutAttempts(3) // Timeout cả 3 lần
                .build();

        fastStateMachine.process(transaction);

        assertEquals(BookingState.CANCELLED, transaction.getCurrentState());
        assertFalse(transaction.isPaymentSuccessful());
        assertFalse(transaction.isReservationSuccessful());

        List<String> history = transaction.getHistory();
        assertTrue(history.contains("[Orchestrator] State: PAYMENT_PENDING -> Event: PAYMENT_FAILED -> New State: CANCELLED"));
        assertTrue(history.contains("[Orchestrator] Compensation triggered for booking: CONCERT-FAIL-PAY"));
    }

    @Test
    @DisplayName("Kịch bản 4: Giữ chỗ thất bại -> Kích hoạt Bù trừ hoàn tiền (Compensation Refund)")
    void testReservationFailedTriggersCompensationRefund() {
        BookingTransaction transaction = BookingTransaction.builder()
                .bookingId("CONCERT-FAIL-SEAT")
                .concertCode("LIVE-HCM-2026-ULTRA")
                .customerId("VIP-SEAT-FAIL")
                .customerEmail("seatfail@email.com")
                .ticketQuantity(4)
                .amount(8000000.0)
                .simulateReservationFailure(true) // Giữ chỗ thất bại
                .build();

        stateMachine.process(transaction);

        assertEquals(BookingState.CANCELLED, transaction.getCurrentState());
        assertFalse(transaction.isReservationSuccessful());
        // Do bước giữ chỗ thất bại sau khi đã thanh toán, Orchestrator kích hoạt hoàn tiền
        assertTrue(transaction.isRefunded());

        List<String> history = transaction.getHistory();
        assertTrue(history.contains("[Orchestrator] State: SEAT_RESERVING -> Event: RESERVATION_FAILED -> New State: CANCELLED"));
        assertTrue(history.contains("[Orchestrator] Compensation triggered for booking: CONCERT-FAIL-SEAT"));
    }
}
