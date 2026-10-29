package com.habit.interfaces.rest.security

import com.habit.infrastructure.persistence.UserIdentifierResolver
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenValidator: JwtTokenValidator,
    private val userIdentifierResolver: UserIdentifierResolver
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        val customUserId = request.getHeader("X-User-Id")

        var principal: HabitUserPrincipal? = null

        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ", ignoreCase = true)) {
            val token = authHeader.substring(7).trim()
            principal = jwtTokenValidator.validate(token)?.let {
                it.copy(userId = userIdentifierResolver.resolveCanonicalUserId(it.userId))
            }
        }

        if (principal == null && !customUserId.isNullOrBlank()) {
            val resolvedUserId = userIdentifierResolver.resolveCanonicalUserId(customUserId)
            principal = HabitUserPrincipal(userId = resolvedUserId)
        }

        if (principal == null && request.requestURI.startsWith("/api/")) {
            principal = HabitUserPrincipal(userId = "default_mobile_user")
        }

        if (principal != null) {
            val authorities = principal.roles.map { SimpleGrantedAuthority(it) }
            val auth = UsernamePasswordAuthenticationToken(principal, null, authorities)
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}
