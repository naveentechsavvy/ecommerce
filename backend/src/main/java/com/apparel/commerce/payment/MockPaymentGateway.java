package com.apparel.commerce.payment;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a real PSP, good for demos and automated tests.
 * <ul>
 *   <li>Cards: token format {@code tok_<brand>_<last4>}; last4 {@code 0002} is declined.</li>
 *   <li>UPI: VPA must look like {@code name@bank}; {@code fail@upi} is declined.</li>
 *   <li>Wallets: PAYTM, PHONEPE, AMAZONPAY, MOBIKWIK.</li>
 * </ul>
 * Replace with a real adapter (and an async webhook flow) per environment.
 */
@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final Pattern CARD_TOKEN = Pattern.compile("^tok_([a-z]+)_(\\d{4})$");
    private static final Pattern VPA = Pattern.compile("^[a-zA-Z0-9._-]{2,256}@[a-zA-Z]{2,64}$");
    private static final Set<String> WALLETS = Set.of("PAYTM", "PHONEPE", "AMAZONPAY", "MOBIKWIK");

    @Override
    public ChargeResult charge(ChargeCommand cmd) {
        PaymentRequest p = cmd.instrument();
        if (p == null || p.method() == null) return fail("Payment method is required", null);
        String ref = "mockpay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        log.info("Mock charge {} {} {} for order {}", p.method(), cmd.amount(), cmd.currency(), cmd.orderNumber());

        return switch (p.method()) {
            case CREDIT_CARD, DEBIT_CARD -> {
                Matcher m = p.token() == null ? null : CARD_TOKEN.matcher(p.token());
                if (m == null || !m.matches()) yield fail("Invalid card token", null);
                String masked = m.group(1).toUpperCase(Locale.ROOT) + " •••• " + m.group(2);
                yield "0002".equals(m.group(2))
                    ? fail("Card declined by issuer", masked)
                    : new ChargeResult(true, "MOCKPAY", ref, masked, null);
            }
            case UPI -> {
                if (p.upiId() == null || !VPA.matcher(p.upiId()).matches()) yield fail("Invalid UPI ID", p.upiId());
                yield "fail@upi".equalsIgnoreCase(p.upiId())
                    ? fail("UPI collect request declined", p.upiId())
                    : new ChargeResult(true, "MOCKPAY-UPI", ref, p.upiId(), null);
            }
            case WALLET -> {
                String w = p.walletProvider() == null ? "" : p.walletProvider().toUpperCase(Locale.ROOT);
                yield WALLETS.contains(w)
                    ? new ChargeResult(true, "MOCKPAY-" + w, ref, w, null)
                    : fail("Unsupported wallet", w);
            }
        };
    }

    @Override
    public boolean refund(String providerRef, BigDecimal amount) {
        log.info("Mock refund {} of {}", providerRef, amount);
        return true;
    }

    private static ChargeResult fail(String reason, String instrument) {
        return new ChargeResult(false, "MOCKPAY", null, instrument, reason);
    }
}
