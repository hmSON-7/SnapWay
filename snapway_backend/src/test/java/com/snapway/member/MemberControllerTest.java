package com.snapway.member;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapway.security.SecurityConfig;
import com.snapway.model.service.MemberService;
import com.snapway.controller.MemberController;
import com.snapway.model.dto.Member;

@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class) // Security 설정 적용하여 403 에러 방지
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean // 실제 Service 대신 가짜(Mock) 객체 주입
    private MemberService memberService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 성공 테스트")
    @WithMockUser // 가상 유저로 요청 수행
    void registSuccess() throws Exception {
        // given: 프론트에서 보낼 데이터 준비
        Member member = Member.builder()
                .email("test@snapway.com")
                .password("password123")
                .username("테스터")
                .build();

        // Service가 정상적으로 1(성공)을 반환한다고 가정
        given(memberService.registMember(any(Member.class))).willReturn(1);

        // when & then: API 호출 및 검증
        mockMvc.perform(post("/api/member/regist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(member))) // JSON 변환 전송
                .andExpect(status().isCreated()) // 201 Created 기대
                .andExpect(jsonPath("$.message").value("success"))
                .andDo(print());
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    @WithMockUser
    void loginSuccess() throws Exception {
        // given
        Map<String, String> loginMap = new HashMap<>();
        loginMap.put("email", "test@snapway.com");
        loginMap.put("password", "password123");

        // Service가 정상 로그인된 Member 객체를 반환한다고 가정
        Member loginMember = Member.builder().email("test@snapway.com").username("테스터").build();
        given(memberService.loginMember("test@snapway.com", "password123")).willReturn(loginMember);

        // when & then
        mockMvc.perform(post("/api/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginMap)))
                .andExpect(status().isOk()) // 200 OK 기대
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.userInfo.username").value("테스터"))
                .andDo(print());
    }

    @Test
    @DisplayName("로그인 실패 테스트 (비밀번호 불일치 등)")
    @WithMockUser
    void loginFail() throws Exception {
        // given
        Map<String, String> loginMap = new HashMap<>();
        loginMap.put("email", "test@snapway.com");
        loginMap.put("password", "wrongPw");

        // Service가 null(실패)을 반환한다고 가정
        given(memberService.loginMember("test@snapway.com", "wrongPw")).willReturn(null);

        // when & then
        mockMvc.perform(post("/api/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginMap)))
                .andExpect(status().isUnauthorized()) // 401 Unauthorized 기대
                .andExpect(jsonPath("$.message").value("fail"))
                .andDo(print());
    }
    
    @Test
    void forTest() throws Exception {
    	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    	System.out.println(encoder.encode("1234"));

    }
}