package com.habit.interfaces.rest.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.Key

data class HabitUserPrincipal(
    val userId: String,
    val roles: List<String> = listOf("ROLE_USER")
)

@Component
class JwtTokenValidator(
    @param:Value("\${jwt.secret}") private val secret: String
) {

    private val signingKey: Key by lazy {
        val keyBytes = try {
            java.util.Base64.getDecoder().decode(secret)
        } catch (e: Exception) {
            secret.toByteArray(StandardCharsets.UTF_8)
        }
        Keys.hmacShaKeyFor(keyBytes)
    }

    fun validate(token: String): HabitUserPrincipal? {
        return try {
            val claims: Claims = Jwts.parser()
                .verifyWith(signingKey as javax.crypto.SecretKey)
                .build()
                .parseSignedClaims(token)
                .payload

            val userId = claims.subject ?: claims["userId"]?.toString() ?: return null
            val roles = (claims["roles"] as? List<*>)?.mapNotNull { it?.toString() } ?: listOf("ROLE_USER")

            HabitUserPrincipal(userId = userId, roles = roles)
        } catch (e: Exception) {
            null
        }
    }
}
