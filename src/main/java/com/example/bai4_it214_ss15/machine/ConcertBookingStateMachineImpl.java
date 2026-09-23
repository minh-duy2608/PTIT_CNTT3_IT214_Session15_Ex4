package com.example.bai4_it214_ss15.machine;

import com.example.bai4_it214_ss15.listener.StateChangeListener;
import com.example.bai4_it214_ss15.model.BookingEvent;
import com.example.bai4_it214_ss15.model.BookingState;
import com.example.bai4_it214_ss15.model.BookingTransaction;
import com.example.bai4_it214_ss15.model.RetryPolicy;
import com.example.bai4_it214_ss15.service.PaymentOrchestrationService;
import com.example.bai4_it214_ss15.service.ReservationOrchestrationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Nhạc trưởng" Orchestrator Saga với State Machine.
 * Đóng vai trò quản lý trạng thái tập trung và điều phối luồng giao dịch,
 * KHÔNG chứa logic nghiệp vụ thanh toán hay kiểm tra chỗ ngồi.
 */
@Service
@Slf4j
public class ConcertBookingStateMachineImpl implements ConcertBookingStateMachine {

    private final PaymentOrchestrationService paymentService;
    private final ReservationOrchestrationService reservationService;
    private final StateChangeListener stateChangeListener;
    @Getter
    private final RetryPolicy retryPolicy;

    // Lưu trạng thái của từng transaction
    private final Map<String, BookingState> transactionStates = new ConcurrentHashMap<>();
    private final Map<String, BookingTransaction> transactions = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public ConcertBookingStateMachineImpl(PaymentOrchestrationService paymentService,
                                          ReservationOrchestrationService reservationService,
                                          StateChangeListener stateChangeListener) {
        this(paymentService, reservationService, stateChangeListener, new RetryPolicy(3, 2000));
    }

    public ConcertBookingStateMachineImpl(PaymentOrchestrationService paymentService,
                                          ReservationOrchestrationService reservationService,
                                          StateChangeListener stateChangeListener,
                                          RetryPolicy retryPolicy) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
        this.stateChangeListener = stateChangeListener;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public void process(BookingTransaction transaction) {
        String bookingId = transaction.getBookingId();

        // Khởi tạo trạng thái INITIATED nếu chưa có trong bảng quản lý
        if (!transactionStates.containsKey(bookingId)) {
            transactionStates.put(bookingId, BookingState.INITIATED);
            transaction.setCurrentState(BookingState.INITIATED);
            transactions.put(bookingId, transaction);
        }

        BookingState currentState = transaction.getCurrentState();

        // Xác định hành động dựa trên trạng thái hiện tại
        switch (currentState) {
            case INITIATED:
                // Chuyển sang PAYMENT_PENDING và gọi thanh toán
                transition(bookingId, BookingState.PAYMENT_PENDING, BookingEvent.PROCESS_PAYMENT);
                processPayment(transaction);
                break;

            case PAYMENT_PENDING:
                // Nếu đang ở PAYMENT_PENDING, chờ kết quả từ payment service
                // (sẽ được xử lý bởi callback hoặc listener)
                break;

            case PAYMENT_COMPLETED:
                // Chuyển sang SEAT_RESERVING và gọi giữ chỗ
                transition(bookingId, BookingState.SEAT_RESERVING, BookingEvent.RESERVE_SEATS);
                processReservation(transaction);
                break;

            case SEAT_RESERVING:
                // Chờ kết quả từ reservation service
                break;

            case BOOKING_CONFIRMED:
                log.info("[Orchestrator] Final State: BOOKING_CONFIRMED for booking {}", bookingId);
                transaction.addHistory("[Orchestrator] Final State: BOOKING_CONFIRMED for booking " + bookingId);
                break;

            case CANCELLED:
                log.info("[Orchestrator] Transaction {} is CANCELLED", bookingId);
                transaction.addHistory("[Orchestrator] Transaction " + bookingId + " is CANCELLED");
                break;

            default:
                log.error("Unknown state: {}", currentState);
        }
    }

    private void transition(String bookingId, BookingState newState, BookingEvent event) {
        BookingState oldState = transactionStates.get(bookingId);
        transactionStates.put(bookingId, newState);

        BookingTransaction tx = transactions.get(bookingId);
        if (tx != null) {
            tx.setCurrentState(newState);
            tx.addHistory("[Orchestrator] State: " + oldState + " -> Event: " + event + " -> New State: " + newState);
        }

        stateChangeListener.onStateChanged(bookingId, oldState, newState, event);
        log.info("[Orchestrator] State: {} -> Event: {} -> New State: {}",
                oldState, event, newState);
    }

    private void processPayment(BookingTransaction transaction) {
        String bookingId = transaction.getBookingId();
        // Thực hiện gọi PaymentService với Retry
        for (int attempt = 1; attempt <= retryPolicy.getMaxAttempts(); attempt++) {
            try {
                log.info("[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt {}/{}",
                        attempt, retryPolicy.getMaxAttempts());
                transaction.addHistory("[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt "
                        + attempt + "/" + retryPolicy.getMaxAttempts());

                boolean success = paymentService.processPayment(transaction);
                if (success) {
                    // Chuyển sang PAYMENT_COMPLETED
                    transition(bookingId, BookingState.PAYMENT_COMPLETED, BookingEvent.PAYMENT_SUCCESS);
                    // Tiếp tục xử lý (gọi process lại để đi tiếp)
                    process(transaction);
                    return;
                } else {
                    transition(bookingId, BookingState.CANCELLED, BookingEvent.PAYMENT_FAILED);
                    compensationTransaction(transaction);
                    return;
                }
            } catch (Exception e) {
                if (attempt == retryPolicy.getMaxAttempts()) {
                    // Thất bại sau 3 lần -> chuyển sang CANCELLED
                    transition(bookingId, BookingState.CANCELLED, BookingEvent.PAYMENT_FAILED);
                    // Kích hoạt bù trừ (nếu cần)
                    compensationTransaction(transaction);
                    return;
                }
                try {
                    Thread.sleep(retryPolicy.getDelayMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    transition(bookingId, BookingState.CANCELLED, BookingEvent.PAYMENT_FAILED);
                    compensationTransaction(transaction);
                    return;
                }
            }
        }
    }

    private void processReservation(BookingTransaction transaction) {
        String bookingId = transaction.getBookingId();
        try {
            boolean success = reservationService.reserveSeats(transaction);
            if (success) {
                transition(bookingId, BookingState.BOOKING_CONFIRMED, BookingEvent.RESERVATION_SUCCESS);
                process(transaction);
            } else {
                transition(bookingId, BookingState.CANCELLED, BookingEvent.RESERVATION_FAILED);
                compensationTransaction(transaction);
            }
        } catch (Exception e) {
            transition(bookingId, BookingState.CANCELLED, BookingEvent.RESERVATION_FAILED);
            compensationTransaction(transaction);
        }
    }

    private void compensationTransaction(BookingTransaction transaction) {
        log.info("[Orchestrator] Compensation triggered for booking: {}", transaction.getBookingId());
        transaction.addHistory("[Orchestrator] Compensation triggered for booking: " + transaction.getBookingId());
        // Gọi hoàn tiền nếu đã thanh toán
        if (transaction.isPaymentSuccessful()) {
            paymentService.refund(transaction);
        }
    }

    @Override
    public BookingState getCurrentState(String bookingId) {
        return transactionStates.get(bookingId);
    }

    @Override
    public BookingTransaction getTransaction(String bookingId) {
        return transactions.get(bookingId);
    }
}
