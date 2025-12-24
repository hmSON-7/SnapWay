package com.snapway.model.service;

import java.sql.SQLException;

import com.snapway.model.dto.AuthDto;

public interface AuthService {
	
	void sendVerificationCode(String email) throws SQLException;
	
	String verifyCode(String email, String code);
	
	void resetPassword(AuthDto.PasswordResetRequest request) throws SQLException;
	
}
