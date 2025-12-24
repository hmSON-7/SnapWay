package com.snapway.model.service;

public interface EmailService {
	
	String createAuthCode();
	
	void sendEmail(String toEmail, String  authCode);
	
}
