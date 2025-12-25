package com.snapway.security;

import java.sql.SQLException;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.snapway.model.mapper.MemberMapper;
import com.snapway.security.CustomUserDetails;
import com.snapway.model.dto.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [보관용] JWT 도입 시 사용될 UserDetailsService
 * 현재는 세션 기반 로그인으로 전환되어 빈 등록(@Service)을 해제함.
 * 추후 JWT 도입 시 주석 해제하여 사용.
 */
@Deprecated
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberMapper memberMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Security: loadUserByUsername execution - email: {}", email);
        
        Member member = null;
        try {
            member = memberMapper.loginMember(email);
        } catch (SQLException e) {
            throw new RuntimeException("DB Error during loading user", e);
        }

        if (member == null) {
            throw new UsernameNotFoundException("해당 이메일의 유저를 찾을 수 없습니다: " + email);
        }

        // Member 객체를 UserDetails 구현체로 감싸서 반환
        return new CustomUserDetails(member);
    }
}