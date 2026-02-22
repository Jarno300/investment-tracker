package com.investmenttracker.dto;

public record RegisterRequest(
    String email,
    String password
) {
}
