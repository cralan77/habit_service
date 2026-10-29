package com.habit.infrastructure.persistence

import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class UserIdentifierResolver(
    private val jdbcTemplate: JdbcTemplate? = null
) {
    private val logger = LoggerFactory.getLogger(UserIdentifierResolver::class.java)

    /**
     * Resolve todos os identificadores equivalentes para um dado userId ou username (UUID e username).
     */
    fun resolveAllCandidateIds(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        if (jdbcTemplate == null) return listOf(input)

        val identifiers = linkedSetOf(input)

        try {
            val username = jdbcTemplate.queryForObject(
                "SELECT username FROM users WHERE id::text = ? LIMIT 1",
                String::class.java,
                input
            )
            if (!username.isNullOrBlank()) {
                identifiers.add(username)
            }
        } catch (_: EmptyResultDataAccessException) {
        } catch (e: Exception) {
            logger.debug("Falha ao resolver username a partir do id '{}': {}", input, e.message)
        }

        try {
            val uuid = jdbcTemplate.queryForObject(
                "SELECT id::text FROM users WHERE username = ? LIMIT 1",
                String::class.java,
                input
            )
            if (!uuid.isNullOrBlank()) {
                identifiers.add(uuid)
            }
        } catch (_: EmptyResultDataAccessException) {
        } catch (e: Exception) {
            logger.debug("Falha ao resolver id a partir do username '{}': {}", input, e.message)
        }

        if (input.startsWith("user_")) {
            val stripped = input.removePrefix("user_")
            identifiers.addAll(resolveAllCandidateIds(stripped))
        }

        return identifiers.toList()
    }

    /**
     * Resolve o userId canônico (prefere UUID se existir na tabela users, senão usa o input).
     */
    fun resolveCanonicalUserId(input: String): String {
        if (input.isBlank() || jdbcTemplate == null) return input

        try {
            val uuid = jdbcTemplate.queryForObject(
                "SELECT id::text FROM users WHERE username = ? LIMIT 1",
                String::class.java,
                input
            )
            if (!uuid.isNullOrBlank()) {
                return uuid
            }
        } catch (_: Exception) {
        }

        if (input.startsWith("user_")) {
            val stripped = input.removePrefix("user_")
            return resolveCanonicalUserId(stripped)
        }

        return input
    }
}
