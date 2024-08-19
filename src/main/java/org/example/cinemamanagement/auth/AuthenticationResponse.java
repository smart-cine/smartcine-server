package org.example.cinemamanagement.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.cinemamanagement.common.Role;
import org.example.cinemamanagement.dto.UserDTO;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class AuthenticationResponse {
    private String token;
    private Role role;
    private String name;
    private String email;
}
