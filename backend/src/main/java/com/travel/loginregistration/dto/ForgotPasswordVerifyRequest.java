package com.travel.loginregistration.dto;

/*
 * Request payload for verifying identity before password reset.
 */
public class ForgotPasswordVerifyRequest {
    public String email;
    public String idType;
    public String idNumber;
}
