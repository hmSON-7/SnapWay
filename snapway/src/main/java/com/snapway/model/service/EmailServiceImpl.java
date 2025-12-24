package com.snapway.model.service;

import java.util.Random;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	private final JavaMailSender jms;
	
	// 랜덤 인증 코드 생성기
	@Override
	public String createAuthCode() {
		Random random = new Random();
		StringBuilder key = new StringBuilder();
		
		for(int i=0; i<6; i++) {
			key.append(random.nextInt(10));
		}
		return key.toString();
	}
	
	@Override
	public void sendEmail(String toEmail, String authCode) {
		MimeMessage mimeMessage = jms.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
			helper.setTo(toEmail);
			helper.setSubject("[Snapway] 비밀번호 변경 인증번호입니다.");
			helper.setText(buildEmailContent(authCode), true);
			
			jms.send(mimeMessage);
			log.info("이메일 발송 성공: {}", toEmail);
			
		} catch(MessagingException e) {
			log.error("이메일 발송 실패 : {}", e.getMessage());
			throw new RuntimeException ("이메일 발송 중 오류가 발생했습니다.");
		}
	}
	
	private String buildEmailContent(String authCode) {
		return "<div style='margin:20px;'>" +
	               "<h1>안녕하세요 Snapway입니다.</h1>" +
	               "<br>" +
	               "<p>비밀번호 찾기를 위한 인증번호입니다.<p>" +
	               "<br>" +
	               "<div style='font-size:130%'>" +
	               "인증번호 : <strong>" + authCode + "</strong>" +
	               "</div>" +
	               "<br>" +
	               "<p>해당 인증번호를 인증번호 확인란에 기입하여 주세요.</p>" +
	               "</div>";
	}
	
}
