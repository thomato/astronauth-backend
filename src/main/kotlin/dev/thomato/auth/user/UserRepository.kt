package dev.thomato.auth.user

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : CoroutineCrudRepository<User, UUID>
