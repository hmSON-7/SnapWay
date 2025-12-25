package com.snapway.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.snapway.security.CustomUserDetails;
import com.snapway.model.dto.Member;
import com.snapway.model.mapper.MemberMapper;
import com.snapway.model.service.MemberServiceImpl;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberServiceImpl memberService; // 테스트 대상

    @Mock
    private MemberMapper memberMapper; // 가짜 DB 매퍼

    @Mock
    private PasswordEncoder passwordEncoder; // 가짜 암호화기

    @Mock
    private AuthenticationManager authenticationManager; // 가짜 인증 매니저

    @Mock
    private Authentication authentication; // 인증 결과 객체

    @Test
    @DisplayName("회원가입 시 비밀번호가 암호화되어 DB에 저장되는지 확인")
    void registMemberTest() throws Exception {
        // given
        Member member = Member.builder()
                .email("new@test.com")
                .password("rawPassword")
                .build();
        
        given(passwordEncoder.encode("rawPassword")).willReturn("encodedPassword");

        // when
        memberService.registMember(member);

        // then
        // 1. 비밀번호가 암호화되었는지 확인
        assertEquals("encodedPassword", member.getPassword());
        // 2. 매퍼(DB)의 registMember가 호출되었는지 확인
        verify(memberMapper).registMember(any(Member.class));
    }

    @Test
    @DisplayName("로그인 시 Security 인증 절차를 거치는지 확인")
    void loginMemberTest() throws Exception {
        // given
        String email = "test@test.com";
        String password = "password";
        
        Member dbMember = Member.builder().email(email).username("user").build();
        CustomUserDetails userDetails = new CustomUserDetails(dbMember);

        // AuthenticationManager가 인증 성공 시 userDetails를 담은 인증 객체를 반환한다고 가정
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userDetails);

        // when
        Member result = memberService.loginMember(email, password);

        // then
        // 1. 반환된 멤버가 기대한 멤버인지 확인
        assertEquals(email, result.getEmail());
        // 2. 인증 매니저의 authenticate 메소드가 실제로 호출되었는지 확인 (핵심)
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}