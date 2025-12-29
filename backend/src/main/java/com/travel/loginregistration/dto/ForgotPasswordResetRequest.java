package com.travel.loginregistration.dto;

/*
 * Request payload for resetting password after identity verification.
 */
public class ForgotPasswordResetRequest {
    public String email;
    public String idType;
    public String idNumber;
    public String newPassword;
}
