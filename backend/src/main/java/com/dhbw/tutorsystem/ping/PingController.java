package com.dhbw.tutorsystem.ping;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@RestController
@SecurityScheme(name = "jwt-auth", type = SecuritySchemeType.HTTP, scheme = "bearer")
public class PingController {

    @Operation(summary = "Ping backend ", tags = { "ping" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The backend is reachable"),
    })
    @GetMapping("/ping")
    // a ping to test if backend is alive
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Pong");
    }

    @Operation(summary = "Ping backend for admin students", tags = { "ping" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The user is allowed to access routes only available to students"),
            @ApiResponse(responseCode = "401", description = "The user is not allowed to access routes only available to students")
    })
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @GetMapping("/ping/auth-student")
    // a ping to test if backend is alive (only available for students)
    public ResponseEntity<String> pingAuthStudent() {
        return ResponseEntity.ok("Pong Student");
    }

    @Operation(summary = "Ping backend for admin routes", tags = {
            "ping" }, security = @SecurityRequirement(name = "jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The user is allowed to access routes only available to admins"),
            @ApiResponse(responseCode = "401", description = "The user is not allowed to access routes only available to admins")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/ping/auth-admin")
    // a ping to test if backend is alive (only available for admins)
    public ResponseEntity<String> pingAuthAdmin() {
        return ResponseEntity.ok("Pong Admin");
    }

    @Operation(summary = "Ping backend for admin directors", tags = {
            "ping" }, security = @SecurityRequirement(name = "jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The user is allowed to access routes only available to directors"),
            @ApiResponse(responseCode = "401", description = "The user is not allowed to access routes only available to directors")
    })
    @PreAuthorize("hasRole('ROLE_DIRECTOR')")
    @GetMapping("/ping/auth-director")
    // a ping to test if backend is alive (only available for directors)
    public ResponseEntity<String> pingAuthDirector() {
        return ResponseEntity.ok("Pong Director");
    }
}
