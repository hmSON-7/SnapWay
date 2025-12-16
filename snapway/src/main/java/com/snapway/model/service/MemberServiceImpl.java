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
    private final AuthenticationManager authenticationManager; 

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
}