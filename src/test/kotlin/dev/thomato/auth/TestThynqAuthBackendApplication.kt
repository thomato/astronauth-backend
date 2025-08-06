package dev.thomato.auth

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<ThynqAuthBackendApplication>().with(TestcontainersConfiguration::class).run(*args)
}
