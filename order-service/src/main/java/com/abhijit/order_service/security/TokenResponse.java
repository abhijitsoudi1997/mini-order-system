package com.abhijit.order_service.security;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) { }