package com.snapway.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.snapway.model.dto.Member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [보관용] JWT 도입 시 사용될 UserDetails 구현체
 * 현재는 세션 기반 로그인(MemberController 직접 제어)을 사용하므로 사용하지 않음.
 * 추후 JWT 및 Security 정석 인증 전환 시 활성화 예정.
 */
@Deprecated
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Member member;
    

    // 권한 목록 반환 (ROLE_USER, ROLE_ADMIN 등)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // Enum Role이 있다면 String으로 변환하여 등록
        if (member.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getEmail(); // 우리는 이메일을 ID로 사용
    }

    // 계정 만료 여부 (true: 만료 안됨)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정 잠김 여부 (true: 잠기지 않음)
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // 비밀번호 만료 여부 (true: 만료 안됨)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 계정 활성화 여부 (true: 활성화)
    @Override
    public boolean isEnabled() {
        return true;
    }
}