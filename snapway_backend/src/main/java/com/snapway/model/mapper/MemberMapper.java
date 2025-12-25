package com.snapway.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.snapway.model.dto.Member;

import io.lettuce.core.dynamic.annotation.Param;

import java.sql.SQLException;

@Mapper
public interface MemberMapper {
    // 회원가입
    void registMember(Member member) throws SQLException;
    
    // 로그인 (이메일로 조회)
    Member loginMember(String email) throws SQLException;
    
    // ID로 회원 정보 조회 (세션 갱신/프로필 조회용)
    Member selectMemberById(int id) throws SQLException;
    
    // 이메일 중복 체크
    int checkEmail(String email) throws SQLException;
    
    // 회원 정보 수정
    int updateMember(Member member) throws SQLException;
    
    // 회원 탈퇴
    int deleteMember(String email) throws SQLException;
    
    // 비밀번호 변경
    int updatePasswordByEmail(@Param("email")String email, @Param("password")String password) throws SQLException;
}