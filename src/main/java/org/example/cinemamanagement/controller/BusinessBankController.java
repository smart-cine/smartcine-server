package org.example.cinemamanagement.controller;

import org.example.cinemamanagement.dto.payment.BusinessBankDTO;
import org.example.cinemamanagement.dto.payment.BusinessBankDTOItem;
import org.example.cinemamanagement.model.BusinessBank;
import org.example.cinemamanagement.payload.response.DataResponse;
import org.example.cinemamanagement.service.BusinessBankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/business-bank")
public class BusinessBankController {
    @Autowired
    private BusinessBankService businessBankService;

    @GetMapping
    public ResponseEntity<DataResponse<List<BusinessBankDTO>>> getBusinesBank() {
        return ResponseEntity.ok(DataResponse.<List<BusinessBankDTO>>builder()
                .success(true)
                .message("Business banks retrieved successfully")
                .data(businessBankService.getAllBusinessBanks())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<BusinessBankDTOItem>> getBusinessBankById(@PathVariable UUID id) {
        return ResponseEntity.ok(DataResponse.<BusinessBankDTOItem>builder()
                .success(true)
                .message("Business bank retrieved successfully")
                .data(businessBankService.getBusinessBankById(id))
                .build());
    }

    @PostMapping
    public ResponseEntity<DataResponse<BusinessBankDTO>> createBusinessBank(@RequestBody(required = true) BusinessBankDTO businessBankDTO) {
        return ResponseEntity.ok(DataResponse.<BusinessBankDTO>builder()
                .success(true)
                .message("Business bank created successfully")
                .data(businessBankService.saveBusinessBank(businessBankDTO))
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<?>> deleteBusinessBank(@PathVariable UUID id) {
        businessBankService.deleteBusinessBank(id);
        return ResponseEntity.ok(DataResponse.builder()
                .success(true)
                .message("Business bank deleted successfully")
                .build());
    }


}
