package com.sparta.showmethecode.api;

import com.google.common.net.HttpHeaders;
import com.sparta.showmethecode.domain.*;
import com.sparta.showmethecode.repository.ReviewAnswerRepository;
import com.sparta.showmethecode.repository.ReviewRequestCommentRepository;
import com.sparta.showmethecode.repository.ReviewRequestRepository;
import com.sparta.showmethecode.repository.UserRepository;
import com.sparta.showmethecode.security.JwtUtils;
import com.sparta.showmethecode.security.UserDetailsImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.Arrays;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@SpringBootTest(properties = "spring.config.location=" +
        "classpath:/application-test.yml")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class UserControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ReviewRequestRepository reviewRequestRepository;
    @Autowired
    ReviewRequestCommentRepository reviewRequestCommentRepository;
    @Autowired
    ReviewAnswerRepository reviewAnswerRepository;
    @Autowired
    JwtUtils jwtUtils;
    @Autowired
    PasswordEncoder passwordEncoder;

    final String TOKEN_PREFIX = "Bearer ";

    User user;
    User reviewer;
    User newReviewer;
    ReviewRequest reviewRequest;
    ReviewAnswer reviewAnswer;
    String token;

    @BeforeAll
    void init() {
        user = new User("user", passwordEncoder.encode("password"), "?????????_?????????", UserRole.ROLE_USER, 0, 0, 0.0);
        reviewer = new User("reviewer", passwordEncoder.encode("password"), "?????????_?????????1", UserRole.ROLE_REVIEWER, 0, 0, 0.0, Arrays.asList(new Language("JAVA")));
        newReviewer = new User("newReviewer", passwordEncoder.encode("password"), "?????????_?????????2", UserRole.ROLE_REVIEWER, 0, 0, 0.0, Arrays.asList(new Language("JAVA")));
        User reviewer2 = new User("newReviewer2", passwordEncoder.encode("password"), "?????????_?????????3", UserRole.ROLE_REVIEWER, 0, 0, 0.0, Arrays.asList(new Language("Python")));

        userRepository.saveAll(Arrays.asList(user, reviewer, newReviewer));

        reviewRequest = new ReviewRequest(user, reviewer, "??????", "??????", ReviewRequestStatus.UNSOLVE, "JAVA");
        reviewRequestRepository.save(reviewRequest);

        ReviewRequestComment reviewRequestComment1 = new ReviewRequestComment("??????1", user);
        ReviewRequestComment reviewRequestComment2 = new ReviewRequestComment("??????2", reviewer);
        reviewRequestCommentRepository.saveAll(Arrays.asList(reviewRequestComment1, reviewRequestComment2));

        reviewAnswer = new ReviewAnswer("????????????", 4.5, reviewer, reviewRequest);
        reviewAnswerRepository.save(reviewAnswer);

        reviewRequest.addComment(reviewRequestComment1);
        reviewRequest.addComment(reviewRequestComment2);
        reviewRequest.setReviewAnswer(reviewAnswer);

        reviewRequestRepository.save(reviewRequest);

    }

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(MockMvcResultHandlers.print())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @DisplayName("1. ?????????????????? ????????? ?????? API ?????????")
    @Order(1)
    @Test
    public void ????????????() throws Exception {
        final String testLanguageName = "JAVA";
        mockMvc.perform(get("/user/language")
                        .param("language", testLanguageName)
                ).andExpect(status().isOk())
                .andDo(document("get-user-searchByLanguageName",
                                requestParameters(
                                        parameterWithName("language").description("????????????")
                                ),
                                responseFields(
                                        fieldWithPath("[].id").description("?????????_ID").type(JsonFieldType.NUMBER),
                                        fieldWithPath("[].username").description("?????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("[].nickname").description("?????????_?????????").type(JsonFieldType.STRING),
                                        fieldWithPath("[].languages").description("????????????_??????").type(JsonFieldType.ARRAY),
                                        fieldWithPath("[].answerCount").description("?????????_?????????").type(JsonFieldType.NUMBER),
                                        fieldWithPath("[].point").description("?????????_??????_?????????").type(JsonFieldType.NUMBER)

                                )
                        )
                );
    }

    @DisplayName("2. ?????? ????????? ?????????????????? ?????? API ?????????")
    @Order(2)
    @Test
    public void ????????????????????????() throws Exception {
        String token = createTokenAndSpringSecuritySetting(user);

        mockMvc.perform(get("/user/requests")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + token)
                        .param("page", "1")
                        .param("size", "10")
                        .param("isAsc", "true")
                        .param("sortBy", "createdAt")
                        .param("status", ReviewRequestStatus.UNSOLVE.toString())
                ).andExpect(status().isOk())
                .andDo(document("get-request-reviewList",
                                requestParameters(
                                        parameterWithName("page").description("??????_?????????_??????").optional(),
                                        parameterWithName("size").description("?????????_???_?????????").optional(),
                                        parameterWithName("sortBy").description("????????????_??????_??????").optional(),
                                        parameterWithName("isAsc").description("????????????").optional(),
                                        parameterWithName("status").description("????????????_????????????").optional()
                                ), responseFields(
                                        fieldWithPath("totalPage").description("?????? ????????????").type(JsonFieldType.NUMBER),
                                        fieldWithPath("totalElements").description("?????? ?????????").type(JsonFieldType.NUMBER),
                                        fieldWithPath("page").description("??????????????? ??????").type(JsonFieldType.NUMBER),
                                        fieldWithPath("size").description("????????? ??? ?????????").type(JsonFieldType.NUMBER),

                                        subsectionWithPath("data").description("????????????_?????????"),
                                        fieldWithPath("data.[].reviewRequestId").description("????????????_ID").type(JsonFieldType.NUMBER),
                                        fieldWithPath("data.[].username").description("???????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].nickname").description("???????????????_?????????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].title").description("????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].content").description("????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].languageName").description("????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].status").description("????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].createdAt").description("????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].commentCount").description("????????????_?????????").type(JsonFieldType.NUMBER)
                                )

                        )
                );
    }

    @DisplayName("3.????????? ????????? ???????????? ?????? API ?????????")
    @Order(3)
    @Test
    public void ?????????_????????????_??????() throws Exception {
        String token = createTokenAndSpringSecuritySetting(reviewer);

        mockMvc.perform(get("/user/received")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + token)
                        .param("page", "1")
                        .param("size", "10")
                        .param("isAsc", "true")
                        .param("sortBy", "createdAt")
                        .param("status", ReviewRequestStatus.UNSOLVE.toString())
                ).andExpect(status().isOk())
                .andDo(document("get-received-reviewList",
                                requestParameters(
                                        parameterWithName("page").description("??????_?????????_??????").optional(),
                                        parameterWithName("size").description("?????????_???_?????????").optional(),
                                        parameterWithName("sortBy").description("????????????_??????_??????").optional(),
                                        parameterWithName("isAsc").description("????????????").optional(),
                                        parameterWithName("status").description("????????????_????????????").optional()
                                ), responseFields(
                                        fieldWithPath("totalPage").description("?????? ????????????").type(JsonFieldType.NUMBER),
                                        fieldWithPath("totalElements").description("?????? ?????????").type(JsonFieldType.NUMBER),
                                        fieldWithPath("page").description("??????????????? ??????").type(JsonFieldType.NUMBER),
                                        fieldWithPath("size").description("????????? ??? ?????????").type(JsonFieldType.NUMBER),

                                        subsectionWithPath("data").description("????????????_?????????"),
                                        fieldWithPath("data.[].reviewRequestId").description("????????????_ID").type(JsonFieldType.NUMBER),
                                        fieldWithPath("data.[].username").description("???????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].nickname").description("???????????????_?????????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].title").description("????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].content").description("????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].languageName").description("????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].status").description("????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].createdAt").description("????????????_??????").type(JsonFieldType.STRING),
                                        fieldWithPath("data.[].commentCount").description("????????????_?????????").type(JsonFieldType.NUMBER)
                                )
                        )
                );
    }

    private String createTokenAndSpringSecuritySetting(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        return token = jwtUtils.createToken(userDetails.getUsername());
    }

}
