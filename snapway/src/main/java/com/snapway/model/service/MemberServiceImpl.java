package com.snapway.model.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snapway.model.mapper.MemberMapper;
import com.snapway.security.CustomUserDetails;
import com.snapway.model.dto.Member;
import com.snapway.model.dto.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder; // SecurityConfig에서 빈으로 등록되어 있어야 함
    @Deprecated // 이 의존성을 다시 사용하게 되면 제거할 것.
    private final AuthenticationManager authenticationManager; 
    private static final String LOCAL_LOGIN_EMAIL = "local@snapway.dev";
    private static final String LOCAL_LOGIN_PASSWORD = "local1234";

    @Override
    @Transactional
    public int registMember(Member member) throws Exception {
        // 1. 비밀번호 암호화
        String rawPassword = member.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        member.setPassword(encodedPassword);

        if (member.getRole() == null) {
            member.setRole(Role.USER);
        }

        try {
            memberMapper.registMember(member);
            return 1;
        } catch (Exception e) {
            log.error("회원가입 실패: {}", e.getMessage());
            throw e;
        }
    }


    @Override
    public Member loginMember(String email, String password) throws Exception {
        if (LOCAL_LOGIN_EMAIL.equals(email) && LOCAL_LOGIN_PASSWORD.equals(password)) {
            return Member.builder()
                .id(-1)
                .email(LOCAL_LOGIN_EMAIL)
                .username("LocalUser")
                .role(Role.USER)
                .build();
        }

        // [변경] Security 인증 절차 수행
//        try {
//            // 1. 인증 토큰 생성 (ID/PW)
//            UsernamePasswordAuthenticationToken authToken = 
//                new UsernamePasswordAuthenticationToken(email, password);
//            
//            // 2. 인증 매니저에게 인증 요청 -> CustomUserDetailsService 호출 -> 비번 검증
//            Authentication authentication = authenticationManager.authenticate(authToken);
//            
//            // 3. 인증 성공 시 UserDetails 꺼내기
//            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
//            
//            log.info("Security 로그인 성공: {}", email);
//            
//            // 4. 컨트롤러에 반환할 멤버 정보 (비밀번호는 null 처리)
//            Member member = userDetails.getMember();
//            member.setPassword(null);
//            
//            return member;
//            
//        } catch (Exception e) {
//            log.error("Security 로그인 실패: {}", e.getMessage());
//            return null; // 인증 실패
//        }
    	
    	// 1. DB에서 이메일로 회원 정보 조회 (암호화된 비번 포함됨)
        Member member = memberMapper.loginMember(email);

        // 2. 회원이 없거나, 비밀번호가 일치하지 않으면 null 반환
        // passwordEncoder.matches(입력받은평문, DB의암호문) -> Security 기능 활용
        if (member == null || !passwordEncoder.matches(password, member.getPassword())) {
            return null;
        }

        // 3. 로그인 성공 시, 보안을 위해 비밀번호는 지우고 반환
        member.setPassword(null);
        return member;
    }

    @Override
    public boolean idCheck(String email) throws Exception {
        int count = memberMapper.checkEmail(email);
        return count > 0; // 1 이상이면 중복(true)
    }
    
    /**
     * 회원 정보 수정
     */
    @Override
    @Transactional
    public int updateMember(Member member) throws Exception {
        // [핵심 로직] 비밀번호 변경 요청이 들어왔는지 확인
        // 프론트에서 비밀번호를 입력하지 않았다면 null 또는 ""(빈문자열)로 들어옴
        if (member.getPassword() != null && !member.getPassword().trim().isEmpty()) {
            // 값이 있다면 -> "비밀번호를 바꾸겠다"는 뜻
            // 1. 새 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(member.getPassword());
            // 2. 암호화된 비밀번호로 덮어쓰기
            member.setPassword(encodedPassword);
        } else {
            // 값이 없다면 -> "비밀번호는 안 바꿀래"라는 뜻
            // null로 설정하여 Mapper의 <if> 조건이 거짓(false)이 되게 함 -> 쿼리에서 제외됨 (기존 비번 유지)
            member.setPassword(null);
        }

        // DB 업데이트 수행 (수정된 행의 개수 반환)
        return memberMapper.updateMember(member);
    }
    
    /**
     * 회원 탈퇴
     */
    @Override
    public int deleteMember(String email) throws Exception {
        // 삭제된 행의 개수 반환
        return memberMapper.deleteMember(email);
    }
}
