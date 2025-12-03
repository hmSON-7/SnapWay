package com.snapway.model.service;

import com.snapway.model.dto.Member;

public interface MemberService {
    
    /**
     * 회원 가입
     * @param member 가입할 회원 정보
     * @return 가입 성공 여부 (1: 성공, 0: 실패)
     */
    int registMember(Member member) throws Exception;

    /**
     * 로그인
     * @param email 이메일
     * @param password 비밀번호 (평문)
     * @return 로그인 성공한 회원 정보 (실패 시 null)
     */
    Member loginMember(String email, String password) throws Exception;

    /**
     * 이메일 중복 체크
     * @param email 체크할 이메일
     * @return 중복 여부 (true: 중복, false: 사용 가능)
     */
    boolean idCheck(String email) throws Exception;
}