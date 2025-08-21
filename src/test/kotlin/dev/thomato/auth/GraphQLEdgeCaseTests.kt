package dev.thomato.auth

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import java.time.Duration
import java.time.Instant

@GraphQlTest(SystemController::class)
@Import(TestcontainersConfiguration::class)
class GraphQLEdgeCaseTests {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @Test
    fun `echo with very long string should work`() {
        val longString = "A".repeat(1000)

        graphQlTester
            .document(
                """
                query {
                    echo(message: "$longString") {
                        original
                        reversed
                        length
                    }
                }
            """,
            )
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo(longString)
            .path("echo.reversed").entity(String::class.java).isEqualTo(longString)
            .path("echo.length").entity(Int::class.java).isEqualTo(1000)
    }

    @Test
    fun `echo with newlines and tabs should preserve formatting`() {
        val multilineString = "Line 1\\nLine 2\\tTabbed\\rCarriage Return"

        graphQlTester
            .document(
                """
                query {
                    echo(message: "$multilineString") {
                        original
                        reversed
                        length
                    }
                }
            """,
            )
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo("Line 1\nLine 2\tTabbed\rCarriage Return")
            .path("echo.length").entity(Int::class.java).isEqualTo("Line 1\nLine 2\tTabbed\rCarriage Return".length)
    }

    @Test
    fun `echo with escaped quotes should work`() {
        val quotedString = "He said \"Hello\" to me"

        graphQlTester
            .document(
                """
                query {
                    echo(message: "He said \"Hello\" to me") {
                        original
                        reversed
                        length
                    }
                }
            """,
            )
            .execute()
            .path("echo.original").entity(String::class.java).isEqualTo(quotedString)
            .path("echo.reversed").entity(String::class.java).isEqualTo(quotedString.reversed())
    }

    @Test
    fun `timestamp should be valid ISO-8601 format`() {
        graphQlTester
            .document(
                """
                query {
                    echo(message: "test") {
                        timestamp
                    }
                    ping {
                        timestamp
                    }
                }
            """,
            )
            .execute()
            .path("echo.timestamp").entity(String::class.java).satisfies { timestamp ->
                val instant = Instant.parse(timestamp)
                assert(instant.isBefore(Instant.now().plusSeconds(1))) { "Timestamp should be recent" }
                assert(instant.isAfter(Instant.now().minusSeconds(5))) { "Timestamp should be very recent" }
            }
            .path("ping.timestamp").entity(String::class.java).satisfies { timestamp ->
                Instant.parse(timestamp) // Should not throw exception
            }
    }

    @Test
    fun `ping latency should be realistic`() {
        graphQlTester
            .document(
                """
                query {
                    ping {
                        latency
                    }
                }
            """,
            )
            .execute()
            .path("ping.latency").entity(Float::class.java).satisfies { latency ->
                assert(latency >= 0.0f) { "Latency should be non-negative" }
                assert(latency < 1.0f) { "Latency should be less than 1 second for local processing" }
            }
    }

    @Test
    fun `query should complete within reasonable time`() {
        assertTimeout(Duration.ofSeconds(1)) {
            graphQlTester
                .document(
                    """
                    query {
                        echo(message: "${"X".repeat(10000)}") {
                            original
                            reversed
                            length
                        }
                    }
                """,
                )
                .execute()
                .path("echo.length").entity(Int::class.java).isEqualTo(10000)
        }
    }

    @Test
    fun `concurrent queries should work`() {
        val queries =
            (1..5).map { i ->
                """
                alias$i: echo(message: "Message $i") {
                    original
                }
            """
            }.joinToString("\n")

        graphQlTester
            .document(
                """
                query {
                    $queries
                }
            """,
            )
            .execute()
            .apply {
                (1..5).forEach { i ->
                    path("alias$i.original").entity(String::class.java).isEqualTo("Message $i")
                }
            }
    }

    @Test
    fun `deeply nested query selection should work`() {
        graphQlTester
            .document(
                """
                query {
                    echo(message: "nested") {
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
            .path("echo").entity(Map::class.java).satisfies { echoMap ->
                assert(echoMap.size == 4) { "Should have exactly 4 fields" }
                assert(echoMap.containsKey("original"))
                assert(echoMap.containsKey("reversed"))
                assert(echoMap.containsKey("length"))
                assert(echoMap.containsKey("timestamp"))
            }
    }

    @Test
    fun `query with null variable should fail gracefully`() {
        graphQlTester
            .document(
                """
                query TestEcho(${"$"}msg: String!) {
                    echo(message: ${"$"}msg) {
                        original
                    }
                }
            """,
            )
            .variable("msg", null)
            .execute()
            .errors()
            .satisfy { errors ->
                assert(errors.isNotEmpty()) { "Should have errors for null non-nullable variable" }
            }
    }

    @Test
    fun `query with wrong type variable should fail`() {
        graphQlTester
            .document(
                """
                query TestEcho(${"$"}msg: String!) {
                    echo(message: ${"$"}msg) {
                        original
                    }
                }
            """,
            )
            .variable("msg", 123) // Number instead of String
            .execute()
            .errors()
            .satisfy { errors ->
                assert(errors.isNotEmpty()) { "Should have errors for wrong type variable" }
            }
    }
}
