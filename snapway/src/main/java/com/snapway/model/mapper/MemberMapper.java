package com.snapway.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.snapway.model.dto.Member;

import java.sql.SQLException;

@Mapper
public interface MemberMapper {
    // 회원가입
    void registMember(Member member) throws SQLException;
    
    // 로그인 (이메일로 조회)
    Member loginMember(String email) throws SQLException;
    
    // 이메일 중복 체크
    int checkEmail(String email) throws SQLException;
}