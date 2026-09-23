package com.example.bai4_it214_ss15.service;

import com.example.bai4_it214_ss15.model.BookingTransaction;

/**
 * Service xử lý thanh toán và hoàn tiền cho giao dịch đặt vé.
 */
public interface PaymentOrchestrationService {
    boolean processPayment(BookingTransaction transaction);
    void refund(BookingTransaction transaction);
}
