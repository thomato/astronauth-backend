package dev.thomato.auth

import dev.thomato.auth.user.UserRepository
import dev.thomato.auth.user.registration.RegisterUserInput
import dev.thomato.auth.user.registration.UserRegistrationController
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class GraphQLIntegrationTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var controller: UserRegistrationController

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `echo query via HTTP should return correct response`() {
        val query =
            """
            {
                "query": "query { echo(message: \"Hello World\") { original reversed length timestamp } }"
            }
            """.trimIndent()

        performGraphQl(query)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.echo.original").value("Hello World"))
            .andExpect(jsonPath("$.data.echo.reversed").value("dlroW olleH"))
            .andExpect(jsonPath("$.data.echo.length").value(11))
            .andExpect(jsonPath("$.data.echo.timestamp").exists())
    }

    @Test
    fun `ping query via HTTP should return pong`() {
        val query =
            """
            {
                "query": "query { ping { status latency timestamp } }"
            }
            """.trimIndent()

        performGraphQl(query)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.ping.status").value("pong"))
            .andExpect(jsonPath("$.data.ping.latency").value(greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.data.ping.timestamp").exists())
    }

    @Test
    fun `query with variables via HTTP should work`() {
        val query =
            """
            {
                "query": "query TestEcho(${"$"}msg: String!) { echo(message: ${"$"}msg) { original reversed length } }",
                "variables": {
                    "msg": "Variable Test"
                }
            }
            """.trimIndent()

        performGraphQl(query)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.echo.original").value("Variable Test"))
            .andExpect(jsonPath("$.data.echo.reversed").value("tseT elbairaV"))
            .andExpect(jsonPath("$.data.echo.length").value(13))
    }

    @Test
    fun `invalid query should return error`() {
        val query =
            """
            {
                "query": "query { invalidQuery }"
            }
            """.trimIndent()

        performGraphQl(query)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errors").exists())
            .andExpect(jsonPath("$.errors[0].message").exists())
    }

    @Test
    fun `missing required argument should return error`() {
        val query =
            """
            {
                "query": "query { echo { original } }"
            }
            """.trimIndent()

        performGraphQl(query)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errors").exists())
            .andExpect(jsonPath("$.errors[0].message").exists())
    }

    @Test
    fun `malformed JSON should return bad request`() {
        val malformedJson =
            """
            { "query": "query { ping { status } }"
            """.trimIndent()

        performGraphQl(malformedJson)
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `batch queries via HTTP should work`() {
        val query =
            """
            {
                "query": "query { echo1: echo(message: \"First\") { original } echo2: echo(message: \"Second\") { original } ping { status } }"
            }
            """.trimIndent()

        performGraphQl(query)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.echo1.original").value("First"))
            .andExpect(jsonPath("$.data.echo2.original").value("Second"))
            .andExpect(jsonPath("$.data.ping.status").value("pong"))
    }

    @Test
    fun `query with operation name should work`() {
        val query =
            """
            {
                "query": "query GetEcho { echo(message: \"Operation Name Test\") { original } } query GetPing { ping { status } }",
                "operationName": "GetEcho"
            }
            """.trimIndent()

        performGraphQl(query)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.echo.original").value("Operation Name Test"))
            .andExpect(jsonPath("$.data.ping").doesNotExist())
    }

    @Test
    fun `introspection query should work`() {
        val query =
            """
            {
                "query": "{ __schema { types { name } } }"
            }
            """.trimIndent()

        performGraphQl(query)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.__schema.types").isArray)
            .andExpect(jsonPath("$.data.__schema.types[?(@.name == 'Query')]").exists())
            .andExpect(jsonPath("$.data.__schema.types[?(@.name == 'EchoResponse')]").exists())
            .andExpect(jsonPath("$.data.__schema.types[?(@.name == 'PingResponse')]").exists())
    }

    @Test
    fun `type introspection should work`() {
        val query =
            """
            {
                "query": "{ __type(name: \"EchoResponse\") { name fields { name type { name } } } }"
            }
            """.trimIndent()

        performGraphQl(query)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.__type.name").value("EchoResponse"))
            .andExpect(jsonPath("$.data.__type.fields[?(@.name == 'original')]").exists())
            .andExpect(jsonPath("$.data.__type.fields[?(@.name == 'reversed')]").exists())
            .andExpect(jsonPath("$.data.__type.fields[?(@.name == 'length')]").exists())
            .andExpect(jsonPath("$.data.__type.fields[?(@.name == 'timestamp')]").exists())
    }

    @Test
    fun `should register user with valid input`() {
        // Arrange
        val input =
            RegisterUserInput(
                email = "user@example.com",
                password = "SecurePass123!",
                confirmPassword = "SecurePass123!",
            )

        // Act
        val result = controller.registerUser(input)

        // Assert
        // Verify user exists in database with correct email
        val savedUser = userRepository.findAll().find { it.email == "user@example.com" }
        assert(savedUser != null) { "User should exist in database" }
        assert(savedUser!!.email == "user@example.com") { "Email should match" }

        // Verify password is hashed (not plaintext)
        val passwordEncoder = BCryptPasswordEncoder()
        assert(passwordEncoder.matches("SecurePass123!", savedUser.password))
        assert(savedUser.password != "SecurePass123!") { "Password should not be stored as plaintext" }

        // Verify success message contains the user ID
        assert(result.startsWith("User registered successfully with ID:"))
        assert(result.contains(savedUser.id.toString())) { "Result should contain the actual user ID" }
    }

    // GraphQL responses may complete asynchronously (e.g. on virtual threads); wait for them
    private fun performGraphQl(body: String): ResultActions {
        val result =
            mockMvc.perform(
                post("/graphql")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
        val mvcResult = result.andReturn()
        return if (mvcResult.request.isAsyncStarted) mockMvc.perform(asyncDispatch(mvcResult)) else result
    }
}
