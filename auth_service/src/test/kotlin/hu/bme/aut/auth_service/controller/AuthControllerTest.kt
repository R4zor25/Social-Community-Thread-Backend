package hu.bme.aut.auth_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.aut.auth_service.domain.AuthRequest
import hu.bme.aut.auth_service.domain.JwtResponse
import hu.bme.aut.auth_service.domain.RefreshTokenRequest
import hu.bme.aut.auth_service.domain.UserRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper
) {

    @Test
    fun testRegisterSuccessful() {
        //given
        val url = "/api/auth/register"
        val userRequest = UserRequest("test", "test", "test")
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userRequest)
        }
        //when
        post
            .andDo { print() }
            //then
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    string("User added successfully!")
                }
            }
    }

    @Test
    fun testRegisterUnsuccessful() {
        //given
        val url = "/api/auth/register"
        val userRequest = UserRequest("test", "test", "test")
        val userRequest1 = UserRequest("test1", "test", "test")
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userRequest)
        }
        val post1 = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userRequest)
        }
        val post2 = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userRequest1)
        }
        //when
        post
            .andDo { print() }
            //then
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    string("User added successfully!")
                }
            }



        post1.andDo { print() }
        //then
        .andExpect {
            status { isBadRequest() }
            content {
                MediaType.APPLICATION_JSON
                string("Username already taken!")
            }
        }

        post2.andDo { print() }
            //then
            .andExpect {
                status { isBadRequest() }
                content {
                    MediaType.APPLICATION_JSON
                    string("Email address already in use!")
                }
            }

    }

    @Test
    fun testLoginSuccessful() {
        //given
        val url = "/api/auth/register"
        val userRequest = UserRequest("test", "test", "test")
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userRequest)
        }

        //when
        post
            .andDo { print() }
            //then
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    string("User added successfully!")
                }
            }

        val loginUrl = "/api/auth/login"
        val authRequest = AuthRequest("test", "test")
        val loginPost = mockMvc.post(loginUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(authRequest)
        }
        loginPost.andDo { print() }.andExpect { status { isOk() } }
    }

    @Test
    fun testLoginUnsuccessful() {
        val url = "/api/auth/login"
        val invalidUserRequest = AuthRequest("test1", "test")
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(invalidUserRequest)
        }

        //when
        post
            .andDo { print() }
            //then
            .andExpect {
                status { isForbidden() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }
    }

    @Test
    fun testRefreshTokenSuccessful() {
        //given
        val url = "/api/auth/register"
        val userRequest = UserRequest("test", "test", "test")
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userRequest)
        }

        //when
        post
            .andDo { print() }
            //then
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    string("User added successfully!")
                }
            }

        val loginUrl = "/api/auth/login"
        val authRequest = AuthRequest("test", "test")
        val loginPost = mockMvc.post(loginUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(authRequest)
        }
        val result = loginPost
            .andDo { print() }
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val token = objectMapper.readValue(result.response.contentAsString, JwtResponse::class.java)
        val refreshTokenUrl = "/api/auth/refreshToken"
        val refreshTokenPost = mockMvc.post(refreshTokenUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RefreshTokenRequest(token.token))
        }

        refreshTokenPost.andDo { print() }.andExpect { status { isOk() } }
    }

    @Test
    fun testRefreshTokenUnsuccessful() {
        val url = "/api/auth/refreshToken"

        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString("Invalid Token")
        }
        post.andDo { print() }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun findAll() {
        val registerUrl = "/api/auth/register"
        val userRequest = UserRequest("test", "test", "test")
        val registerPost = mockMvc.post(registerUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userRequest)
        }

        //when
        registerPost
            .andDo { print() }
            //then
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    string("User added successfully!")
                }
            }

        val url = "/api/auth/users"
        val post = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }
        post
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    jsonPath("$.size()") { value(1) }
                    jsonPath("$[0].userName") { value("test") }
                }
            }
    }

    @Test
    fun findByIdSuccessful() {
        val registerUrl = "/api/auth/register"
        val userRequest = UserRequest("test", "test", "test")
        val registerPost = mockMvc.post(registerUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userRequest)
        }

        //when
        registerPost
            .andDo { print() }
            //then
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    string("User added successfully!")
                }
            }

        val url = "/api/auth/users/${1L}"
        val post = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }
        post
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    jsonPath("$.userName") { value("test") }
                }
            }
    }

    @Test
    fun findByIdUnsuccessful() {
        val url = "/api/auth/users/${1L}"
        val post = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }
        post
            .andDo { print() }
            .andExpect {
                status { isNotFound() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }
    }

    @Test
    fun findByUsernameSuccessful() {
        val registerUrl = "/api/auth/register"
        val userRequest = UserRequest("test", "test", "test")
        val registerPost = mockMvc.post(registerUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userRequest)
        }

        //when
        registerPost
            .andDo { print() }
            //then
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    string("User added successfully!")
                }
            }

        val url = "/api/auth/users/username/test"
        val post = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }
        post
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    jsonPath("$.userName") { value("test") }
                }
            }
    }

    @Test
    fun findByUsernameUnsuccessful() {
        val url = "/api/auth/users/username/test"
        val post = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }
        post
            .andDo { print() }
            .andExpect {
                status { isNotFound() }
                content {
                    MediaType.APPLICATION_JSON
                    string("")
                }
            }
    }

}