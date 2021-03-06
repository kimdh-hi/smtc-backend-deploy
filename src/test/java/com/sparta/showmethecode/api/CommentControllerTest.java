package com.sparta.showmethecode.api;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.sparta.showmethecode.domain.*;
import com.sparta.showmethecode.dto.request.AddCommentDto;
import com.sparta.showmethecode.dto.request.UpdateCommentDto;
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
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@SpringBootTest(properties = "spring.config.location=" +
        "classpath:/application-test.yml")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class CommentControllerTest {

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
    ReviewRequest reviewRequest;
    ReviewRequestComment comment;
    String token;

    @BeforeAll
    public void init() {
        user = new User("user", passwordEncoder.encode("password"), "?????????_?????????", UserRole.ROLE_USER, 0, 0, 0.0);
        reviewer = new User("reviewer", passwordEncoder.encode("password"), "?????????_?????????", UserRole.ROLE_REVIEWER, 0, 0, 0.0, Arrays.asList(new Language("JAVA")));

        userRepository.saveAll(Arrays.asList(user, reviewer));

        reviewRequest = new ReviewRequest(user, reviewer, "??????", "??????", ReviewRequestStatus.UNSOLVE, "JAVA");
        reviewRequestRepository.save(reviewRequest);

        comment = new ReviewRequestComment("??????1", user);
        ReviewRequestComment reviewRequestComment2 = new ReviewRequestComment("??????2", reviewer);
        reviewRequestCommentRepository.saveAll(Arrays.asList(comment, reviewRequestComment2));

        ReviewAnswer reviewAnswer = new ReviewAnswer("????????????", 4.5, reviewer, reviewRequest);
        reviewAnswerRepository.save(reviewAnswer);

        reviewRequest.addComment(comment);
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

    @Order(1)
    @DisplayName("1. ???????????? API ?????????")
    @Test
    public void ????????????() throws Exception {
        String token = createTokenAndSpringSecuritySetting(user);
        AddCommentDto addCommentDto = new AddCommentDto("????????? ???????????????.");
        String dtoJson = new Gson().toJson(addCommentDto);

        mockMvc.perform(RestDocumentationRequestBuilders.post("/question/{questionId}/comment", reviewRequest.getId())
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(dtoJson)
                ).andExpect(status().isOk())
                .andDo(document("post-comment",
                                pathParameters(
                                        parameterWithName("questionId").description("????????????_ID")
                                ),
                                requestFields(
                                        fieldWithPath("content").description("????????????")
                                )
                        )
                );
    }

    @Order(2)
    @DisplayName("2. ???????????? API ?????????")
    @Test
    public void ????????????() throws Exception {
        String token = createTokenAndSpringSecuritySetting(user);

        mockMvc.perform(RestDocumentationRequestBuilders.delete("/question/comment/{commentId}", comment.getId())
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + token)
                ).andExpect(status().isOk())
                .andDo(document("delete-comment",
                                pathParameters(
                                        parameterWithName("commentId").description("??????_ID")
                                )
                        )
                );
    }

    @Order(3)
    @DisplayName("3. ???????????? API ?????????")
    @Test
    public void ????????????() throws Exception {
        String token = createTokenAndSpringSecuritySetting(user);
        UpdateCommentDto updateCommentDto = new UpdateCommentDto("???????????? ?????????");
        String dtoJson = new Gson().toJson(updateCommentDto);

        mockMvc.perform(RestDocumentationRequestBuilders.put("/question/comment/{commentId}", comment.getId())
                        .header(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(dtoJson)
                ).andExpect(status().isOk())
                .andDo(document("put-comment",
                                pathParameters(
                                        parameterWithName("commentId").description("??????_ID")
                                ),
                                requestFields(
                                        fieldWithPath("content").description("????????????")
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
