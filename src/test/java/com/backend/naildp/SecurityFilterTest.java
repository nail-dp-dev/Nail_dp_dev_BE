package com.backend.naildp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.backend.naildp.oauth2.impl.UserDetailsServiceImpl;
import com.backend.naildp.oauth2.jwt.JwtUtil;
import com.backend.naildp.service.AuthService;

@SpringBootTest(classes = NaildpApplication.class)
@AutoConfigureMockMvc
public class SecurityFilterTest {

	private static final Logger logger = LoggerFactory.getLogger(JwtUtilTest.class);

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OAuth2AuthorizedClientService authorizedClientService;

	@MockBean
	private ClientRegistrationRepository clientRegistrationRepository;

	@MockBean
	private AuthService authService;

	@MockBean
	private JwtUtil jwtUtil;

	@MockBean
	private UserDetailsServiceImpl userDetailsService;

	@BeforeEach
	public void setUp() throws Exception {
		// secretKey 값을 수동으로 설정
		ReflectionTestUtils.setField(jwtUtil, "secretKey",
			"d42ad6111848f136a3de63282954ec1fb581f7cc9c8cc3a6e63fd34547ec7a29d2362ca9f9ba99af7bb7c56fa5af71fd862ad6c44591172357fe0b52448cfa29");
		jwtUtil.init();
		logger.info("setup completed");

	}

	// @Test
	// @DisplayName("허용된 엔드포인트 테스트")
	// public void securityTest1() throws Exception {
	// 	LoginRequestDto loginRequestDto = new LoginRequestDto();
	// 	loginRequestDto.setNickname("testuser");
	// 	loginRequestDto.setPhone_number("01012345678");
	//
	// 	when(authService.signupUser(any(LoginRequestDto.class),))
	// 		.thenReturn(ApiResponse.successWithMessage(HttpStatus.OK, "회원가입 성공"));
	//
	// 	mockMvc.perform(post("/auth/signup")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(
	// 				"{\"nickname\":\"testuser\", \"phone_number\":\"01012345678\"}"))
	// 		.andExpect(status().isOk()) // 200
	// 		.andDo(print());
	// }

	@Test
	@DisplayName("보호된 엔드포인트 테스트 - 인증 없이 접근 시")
	public void securityTest2() throws Exception {
		mockMvc.perform(get("/api/protected"))
			.andExpect(status().isUnauthorized()) // 401
			.andDo(print());
	}

	@Test
	@DisplayName("보호된 엔드포인트 테스트 - JWT 인증 후 접근")
	@WithMockUser(username = "testuser", roles = {"USER"})
	public void securityTest3() throws Exception {

		mockMvc.perform(get("/api/protected"))
			.andExpect(status().isOk()) // 인증된 경우 200 OK 예상
			.andDo(print());

	}
}