package com.example.bai4_it214_ss15.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Cấu hình Retry Policy cho các hoạt động Saga.
 * Mặc định: Thử lại tối đa 03 lần nếu gặp lỗi timeout, khoảng cách 2000ms.
 */
@Getter
@Setter
public class RetryPolicy {
    private final int maxAttempts;
    private final long delayMs;

    public RetryPolicy(int maxAttempts, long delayMs) {
        this.maxAttempts = maxAttempts;
        this.delayMs = delayMs;
    }

    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, 2000);
    }
}
