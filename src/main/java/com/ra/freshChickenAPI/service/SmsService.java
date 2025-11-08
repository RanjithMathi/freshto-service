package com.ra.freshChickenAPI.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SmsService {
    
    // For production, integrate with SMS providers like:
    // - Twilio
    // - AWS SNS
    // - MSG91
    // - Fast2SMS
    // - TextLocal
    
    public boolean sendOtp(String phoneNumber, String otp) {
        try {
            // TODO: Implement actual SMS sending logic
            // Example with Twilio:
            // Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            // Message message = Message.creator(
            //     new PhoneNumber("+91" + phoneNumber),
            //     new PhoneNumber(FROM_NUMBER),
            //     "Your OTP is: " + otp + ". Valid for 5 minutes."
            // ).create();
            
            // For development/testing: Just log the OTP
            log.info("Sending OTP to {}: {}", phoneNumber, otp);
            log.info("=".repeat(50));
            log.info("SMS: Your Fresh Chicken verification code is: {}", otp);
            log.info("Valid for 5 minutes. Do not share with anyone.");
            log.info("=".repeat(50));
            
            return true;
        } catch (Exception e) {
            log.error("Failed to send OTP to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}