package org.example.cinemamanagement.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PickSeatDTO {
    private UUID id;
    @JsonProperty("account_id")
    private UUID accountID;
    @JsonProperty("perform_id")
    private UUID performID;
    @JsonProperty("layout_seat_id")
    private UUID seatID;
    private String code;
}
