package dev.thomato.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ThynqAuthBackendApplication

fun main(args: Array<String>) {
    runApplication<ThynqAuthBackendApplication>(*args)
}
