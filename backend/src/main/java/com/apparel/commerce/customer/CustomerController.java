package com.apparel.commerce.customer;

import com.apparel.commerce.common.ApiException;
import com.apparel.commerce.common.CurrentUser;
import com.apparel.commerce.common.PageResponse;
import com.apparel.commerce.order.OrderDtos.OrderSummary;
import com.apparel.commerce.order.OrderDtos.OrderView;
import com.apparel.commerce.order.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Signed-in customer self-service ("My account"). */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class CustomerController {

    private static final int MAX_ADDRESSES = 10;

    private final OrderService orders;
    private final CustomerAddressRepository addresses;

    public record AddressRequest(@Size(max = 40) String label, @NotBlank @Size(max = 120) String fullName,
                                 @NotBlank @Pattern(regexp = "^[0-9+ -]{8,20}$") String phone,
                                 @NotBlank @Size(max = 200) String line1, @Size(max = 200) String line2,
                                 @NotBlank @Size(max = 80) String city, @NotBlank @Size(max = 80) String state,
                                 @NotBlank @Pattern(regexp = "^[0-9A-Za-z -]{3,12}$") String postalCode,
                                 @NotBlank @Size(min = 2, max = 2) String country, boolean isDefault) {}

    public record AddressView(Long id, String label, String fullName, String phone, String line1, String line2,
                              String city, String state, String postalCode, String country, boolean isDefault) {}

    @GetMapping
    public CurrentUser profile() {
        return CurrentUser.require();
    }

    @GetMapping("/orders")
    public PageResponse<OrderSummary> orders(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return orders.myOrders(page, size);
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderView order(@PathVariable String orderNumber) {
        return orders.myOrder(orderNumber);
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    public OrderView cancel(@PathVariable String orderNumber) {
        return orders.cancelMyOrder(orderNumber);
    }

    @GetMapping("/addresses")
    @Transactional(readOnly = true)
    public List<AddressView> addresses() {
        return addresses.findByCustomerIdOrderByIsDefaultDescIdAsc(CurrentUser.require().customerId())
            .stream().map(CustomerController::toView).toList();
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AddressView addAddress(@Valid @RequestBody AddressRequest req) {
        String me = CurrentUser.require().customerId();
        if (addresses.countByCustomerId(me) >= MAX_ADDRESSES) {
            throw ApiException.badRequest("ADDRESS_LIMIT", "You can save up to " + MAX_ADDRESSES + " addresses");
        }
        boolean first = addresses.countByCustomerId(me) == 0;
        if (req.isDefault()) addresses.clearDefault(me);
        CustomerAddress a = new CustomerAddress();
        a.setCustomerId(me);
        apply(a, req);
        a.setDefault(req.isDefault() || first);
        return toView(addresses.save(a));
    }

    @PutMapping("/addresses/{id}")
    @Transactional
    public AddressView updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest req) {
        String me = CurrentUser.require().customerId();
        if (req.isDefault()) addresses.clearDefault(me);
        CustomerAddress a = addresses.findByIdAndCustomerId(id, me).orElseThrow(() -> ApiException.notFound("Address", id));
        apply(a, req);
        if (req.isDefault()) a.setDefault(true);
        return toView(addresses.save(a));
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteAddress(@PathVariable Long id) {
        String me = CurrentUser.require().customerId();
        addresses.delete(addresses.findByIdAndCustomerId(id, me).orElseThrow(() -> ApiException.notFound("Address", id)));
    }

    private static void apply(CustomerAddress a, AddressRequest r) {
        a.setLabel(r.label());
        a.setFullName(r.fullName());
        a.setPhone(r.phone());
        a.setLine1(r.line1());
        a.setLine2(r.line2());
        a.setCity(r.city());
        a.setState(r.state());
        a.setPostalCode(r.postalCode());
        a.setCountry(r.country().toUpperCase());
    }

    private static AddressView toView(CustomerAddress a) {
        return new AddressView(a.getId(), a.getLabel(), a.getFullName(), a.getPhone(), a.getLine1(), a.getLine2(),
            a.getCity(), a.getState(), a.getPostalCode(), a.getCountry(), a.isDefault());
    }
}
