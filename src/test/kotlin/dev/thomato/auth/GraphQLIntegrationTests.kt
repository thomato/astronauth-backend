package dev.thomato.auth

import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
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

    @Test
    fun `echo query via HTTP should return correct response`() {
        val query =
            """
            {
                "query": "query { echo(message: \"Hello World\") { original reversed length timestamp } }"
            }
            """.trimIndent()

        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query),
        )
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

        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query),
        )
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

        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query),
        )
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

        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query),
        )
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

        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query),
        )
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

        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson),
        )
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

        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query),
        )
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

        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query),
        )
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

        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query),
        )
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

        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.__type.name").value("EchoResponse"))
            .andExpect(jsonPath("$.data.__type.fields[?(@.name == 'original')]").exists())
            .andExpect(jsonPath("$.data.__type.fields[?(@.name == 'reversed')]").exists())
            .andExpect(jsonPath("$.data.__type.fields[?(@.name == 'length')]").exists())
            .andExpect(jsonPath("$.data.__type.fields[?(@.name == 'timestamp')]").exists())
    }
}
