package com.example.bai4_it214_ss15.service;

import com.example.bai4_it214_ss15.model.BookingTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeoutException;

/**
 * Triển khai nghiệp vụ Payment Service (Activity trong Saga).
 * Tuân thủ nguyên tắc: Logic nghiệp vụ thanh toán nằm ở Service riêng biệt, không nằm trong State Machine.
 */
@Service
@Slf4j
public class PaymentOrchestrationServiceImpl implements PaymentOrchestrationService {

    @Override
    public boolean processPayment(BookingTransaction transaction) {
        int attempt = transaction.getPaymentAttemptCount() + 1;
        transaction.setPaymentAttemptCount(attempt);

        log.info("[PaymentService] Processing payment of {} VND for booking {} (customer: {}) - attempt {}",
                transaction.getAmount(), transaction.getBookingId(), transaction.getCustomerId(), attempt);

        // Mô phỏng lỗi timeout để kiểm thử Retry Policy
        if (transaction.getSimulatePaymentTimeoutAttempts() > 0 
                && attempt <= transaction.getSimulatePaymentTimeoutAttempts()) {
            log.warn("[PaymentService] Simulated network timeout during payment processing for booking {}",
                    transaction.getBookingId());
            throw new RuntimeException(new TimeoutException("Simulated payment gateway timeout"));
        }

        // Mô phỏng thanh toán thất bại
        if (transaction.isSimulatePaymentFailure()) {
            log.warn("[PaymentService] Payment failed for booking {}", transaction.getBookingId());
            transaction.setPaymentSuccessful(false);
            transaction.setFailureReason("Payment processing failed");
            return false;
        }

        transaction.setPaymentSuccessful(true);
        log.info("[PaymentService] Payment successful for booking {}", transaction.getBookingId());
        return true;
    }

    @Override
    public void refund(BookingTransaction transaction) {
        log.info("[PaymentService] Refunding {} VND for booking {} to customer {}",
                transaction.getAmount(), transaction.getBookingId(), transaction.getCustomerId());
        transaction.setRefunded(true);
        transaction.setPaymentSuccessful(false);
        log.info("[PaymentService] Refund completed successfully for booking {}", transaction.getBookingId());
    }
}
