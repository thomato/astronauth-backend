package dev.thomato.auth.account

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AccountIntegrationTest
class CsrfTests(
    @Autowired private val mockMvc: MockMvc,
) {
    private val body = """{"query": "query { ping { status } }"}"""

    @Test
    fun `a GraphQL request without the CSRF token is refused`() {
        mockMvc
            .perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `a GraphQL request whose header does not match the cookie is refused`() {
        mockMvc
            .perform(
                post("/graphql")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .cookie(jakarta.servlet.http.Cookie("XSRF-TOKEN", CSRF_TOKEN))
                    .header("X-XSRF-TOKEN", "something else"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `loading a page hands the SPA its CSRF token`() {
        mockMvc
            .perform(get("/register"))
            .andExpect(status().isOk)
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
    }
}
