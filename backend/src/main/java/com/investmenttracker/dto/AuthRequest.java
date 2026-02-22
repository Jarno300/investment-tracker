package com.investmenttracker.dto;

public record AuthRequest(
    String email,
    String password
) {
}
