package dev.thomato.auth

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.time.Instant

@Controller
class SystemController {
    @QueryMapping
    fun echo(
        @Argument message: String,
    ) = EchoResponse(
        original = message,
        reversed = message.reversed(),
        length = message.length,
        timestamp = Instant.now().toString(),
    )

    @QueryMapping
    fun ping() =
        PingResponse(
            status = "pong",
            latency = 0.001f,
            timestamp = Instant.now().toString(),
        )
}

data class EchoResponse(
    val original: String,
    val reversed: String,
    val length: Int,
    val timestamp: String,
)

data class PingResponse(
    val status: String,
    val latency: Float,
    val timestamp: String,
)
