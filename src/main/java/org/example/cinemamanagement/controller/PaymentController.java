package org.example.cinemamanagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.cinemamanagement.model.Payment;
import org.example.cinemamanagement.payload.request.OrderRequestDTO;
import org.example.cinemamanagement.payload.response.DataResponse;
import org.example.cinemamanagement.repository.PaymentRepository;
import org.example.cinemamanagement.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/{id}")
    public ResponseEntity<?> getPayment(@PathVariable UUID id) {
        DataResponse dataResponse = DataResponse.builder()
                .message("Get payment successfully")
                .data(paymentService.getPayment(id))
                .success(true)
                .build();

        return ResponseEntity.ok(dataResponse);
    }

    @PostMapping("/{type}")
    public ResponseEntity<DataResponse<Map<String, Object>>> createPayment(
            HttpServletRequest request,
            @RequestBody OrderRequestDTO req, @PathVariable String type) throws Exception {

        type = type.toUpperCase();
        switch (type) {
            case "VNPAY":
                DataResponse<Map<String, Object>> dataResponse = DataResponse.<Map<String, Object>>builder()
                        .message("Add payment successfully").success(true)
                        .data(paymentService.createOrder(request, req))
                        .build();

                return ResponseEntity.ok(dataResponse);
            case "MOMO":
                break;

            case "ZALOPAY":
                break;

            default:
                throw new Exception("Don't have this type payment");

        }

        return null;
    }

    @GetMapping("/ipn")
    public ResponseEntity<?> handlePayment(@RequestParam Map<String, String> params) {
        paymentService.handlePayment(params);
        return ResponseEntity.ok(DataResponse.builder()
                .message("Payment handled successfully")
                .success(true)
                .build());
    }

    @PostMapping("/testing")
    public ResponseEntity<?> test(@RequestBody Map<String, String> body) {

        paymentRepository.save(Payment.builder()
                .build());
        return null;
    }

}