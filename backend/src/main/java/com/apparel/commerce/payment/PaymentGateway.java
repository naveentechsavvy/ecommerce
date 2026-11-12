package com.apparel.commerce.payment;

import java.math.BigDecimal;

/**
 * Port to a payment service provider (Razorpay / Stripe / PayU / Cashfree ...).
 * Card data never reaches this backend: the storefront tokenises it with the
 * PSP's hosted fields / SDK and sends only the token (keeps PCI scope at SAQ-A).
 */
public interface PaymentGateway {

    enum Method { CREDIT_CARD, DEBIT_CARD, WALLET, UPI }

    /** What the storefront sends. Exactly one of token / upiId / walletProvider is relevant per method. */
    record PaymentRequest(Method method, String token, String upiId, String walletProvider) {}

    record ChargeCommand(String orderNumber, BigDecimal amount, String currency, String email, PaymentRequest instrument) {}

    record ChargeResult(boolean success, String provider, String providerRef, String maskedInstrument, String failureReason) {}

    ChargeResult charge(ChargeCommand command);

    /** Refund (full) of a previous successful charge. */
    boolean refund(String providerRef, BigDecimal amount);
}
