package dev.thomato.auth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester

@GraphQlTest(SystemController::class)
@Import(TestcontainersConfiguration::class)
class GraphQLTests {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @Test
    fun `echo query should return original, reversed, length and timestamp`() {
        val testMessage = "Hello GraphQL"

        graphQlTester
            .document(
                """
                query {
                    echo(message: "$testMessage") {
                        original
                        reversed
                        length
                        timestamp
                    }
                }
            """,
            )
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo(testMessage)
            .path("echo.reversed").entity(String::class.java).isEqualTo(testMessage.reversed())
            .path("echo.length").entity(Int::class.java).isEqualTo(testMessage.length)
            .path("echo.timestamp").entity(String::class.java).satisfies { timestamp ->
                assert(timestamp.isNotEmpty()) { "Timestamp should not be empty" }
            }
    }

    @Test
    fun `echo query with empty string should work`() {
        val testMessage = ""

        graphQlTester
            .document(
                """
                query {
                    echo(message: "$testMessage") {
                        original
                        reversed
                        length
                        timestamp
                    }
                }
            """,
            )
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo(testMessage)
            .path("echo.reversed").entity(String::class.java).isEqualTo(testMessage)
            .path("echo.length").entity(Int::class.java).isEqualTo(0)
            .path("echo.timestamp").entity(String::class.java).satisfies { timestamp ->
                assert(timestamp.isNotEmpty()) { "Timestamp should not be empty" }
            }
    }

    @Test
    fun `echo query with special characters should work`() {
        val testMessage = "Hello @#$%^&*() World!"

        graphQlTester
            .document(
                """
                query {
                    echo(message: "$testMessage") {
                        original
                        reversed
                        length
                        timestamp
                    }
                }
            """,
            )
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo(testMessage)
            .path("echo.reversed").entity(String::class.java).isEqualTo(testMessage.reversed())
            .path("echo.length").entity(Int::class.java).isEqualTo(testMessage.length)
            .path("echo.timestamp").entity(String::class.java).satisfies { timestamp ->
                assert(timestamp.isNotEmpty()) { "Timestamp should not be empty" }
            }
    }

    @Test
    fun `echo query with unicode characters should work`() {
        val testMessage = "Hello 世界 🌍"

        graphQlTester
            .document(
                """
                query {
                    echo(message: "$testMessage") {
                        original
                        reversed
                        length
                        timestamp
                    }
                }
            """,
            )
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo(testMessage)
            .path("echo.reversed").entity(String::class.java).isEqualTo(testMessage.reversed())
            .path("echo.length").entity(Int::class.java).isEqualTo(testMessage.length)
            .path("echo.timestamp").entity(String::class.java).satisfies { timestamp ->
                assert(timestamp.isNotEmpty()) { "Timestamp should not be empty" }
            }
    }

    @Test
    fun `ping query should return pong status with latency and timestamp`() {
        graphQlTester
            .document(
                """
                query {
                    ping {
                        status
                        latency
                        timestamp
                    }
                }
            """,
            )
            .execute()
            .path("ping.status").entity(String::class.java).isEqualTo("pong")
            .path("ping.latency").entity(Float::class.java).satisfies { latency ->
                assert(latency >= 0) { "Latency should be non-negative" }
            }
            .path("ping.timestamp").entity(String::class.java).satisfies { timestamp ->
                assert(timestamp.isNotEmpty()) { "Timestamp should not be empty" }
            }
    }

    @Test
    fun `multiple queries in single request should work`() {
        val testMessage = "Test"

        graphQlTester
            .document(
                """
                query {
                    echo(message: "$testMessage") {
                        original
                        reversed
                        length
                        timestamp
                    }
                    ping {
                        status
                        latency
                        timestamp
                    }
                }
            """,
            )
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo(testMessage)
            .path("echo.reversed").entity(String::class.java).isEqualTo(testMessage.reversed())
            .path("echo.length").entity(Int::class.java).isEqualTo(testMessage.length)
            .path("ping.status").entity(String::class.java).isEqualTo("pong")
    }

    @Test
    fun `echo query with alias should work`() {
        val message1 = "First"
        val message2 = "Second"

        graphQlTester
            .document(
                """
                query {
                    first: echo(message: "$message1") {
                        original
                        length
                    }
                    second: echo(message: "$message2") {
                        original
                        length
                    }
                }
            """,
            )
            .execute()
            .path("first.original").entity(String::class.java).isEqualTo(message1)
            .path("first.length").entity(Int::class.java).isEqualTo(message1.length)
            .path("second.original").entity(String::class.java).isEqualTo(message2)
            .path("second.length").entity(Int::class.java).isEqualTo(message2.length)
    }

    @Test
    fun `query with fragments should work`() {
        val testMessage = "Fragment Test"

        graphQlTester
            .document(
                """
                query {
                    echo(message: "$testMessage") {
                        ...echoFields
                    }
                }
                
                fragment echoFields on EchoResponse {
                    original
                    reversed
                    length
                    timestamp
                }
            """,
            )
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo(testMessage)
            .path("echo.reversed").entity(String::class.java).isEqualTo(testMessage.reversed())
            .path("echo.length").entity(Int::class.java).isEqualTo(testMessage.length)
            .path("echo.timestamp").entity(String::class.java).satisfies { timestamp ->
                assert(timestamp.isNotEmpty()) { "Timestamp should not be empty" }
            }
    }

    @Test
    fun `query with variables should work`() {
        val testMessage = "Variable Test"

        graphQlTester
            .document(
                """
                query TestEcho(${"$"}msg: String!) {
                    echo(message: ${"$"}msg) {
                        original
                        reversed
                        length
                        timestamp
                    }
                }
            """,
            )
            .variable("msg", testMessage)
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo(testMessage)
            .path("echo.reversed").entity(String::class.java).isEqualTo(testMessage.reversed())
            .path("echo.length").entity(Int::class.java).isEqualTo(testMessage.length)
    }

    @Test
    fun `partial field selection should work`() {
        val testMessage = "Partial"

        graphQlTester
            .document(
                """
                query {
                    echo(message: "$testMessage") {
                        original
                        length
                    }
                }
            """,
            )
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo(testMessage)
            .path("echo.length").entity(Int::class.java).isEqualTo(testMessage.length)
            .path("echo.reversed").pathDoesNotExist()
            .path("echo.timestamp").pathDoesNotExist()
    }
}
