package com.example.backend.security

import com.example.backend.domain.User
import com.example.backend.repository.UserRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.security.Key
import java.util.*

class UserPrincipal(
    val id: UUID,
    val email: String?,
    val phoneNumber: String?,
    val phoneVerified: Boolean,
    private val passwordHash: String,
    private val authorities: Collection<GrantedAuthority>
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = authorities
    override fun getPassword(): String = passwordHash
    override fun getUsername(): String = email ?: phoneNumber ?: id.toString()
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    companion object {
        fun fromUser(user: User): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
            return UserPrincipal(
                id = user.id!!,
                email = user.email,
                phoneNumber = user.phoneNumber,
                phoneVerified = user.phoneVerified,
                passwordHash = user.passwordHash,
                authorities = authorities
            )
        }
    }
}

@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}") private val jwtSecret: String,
    @Value("\${app.jwt.access-expiration-ms}") val accessExpirationMs: Long,
    @Value("\${app.jwt.refresh-expiration-ms}") val refreshExpirationMs: Long
) {
    private val log = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    private val key: Key by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }

    fun generateAccessToken(user: User): String {
        val now = Date()
        val expiryDate = Date(now.time + accessExpirationMs)

        return Jwts.builder()
            .setSubject(user.id.toString())
            .claim("email", user.email)
            .claim("phone", user.phoneNumber)
            .claim("phoneVerified", user.phoneVerified)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims: Claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        return UUID.fromString(claims.subject)
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
            true
        } catch (ex: Exception) {
            log.warn("Invalid JWT Token: ${ex.message}")
            false
        }
    }
}

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val jwt = getJwtFromRequest(request)
            if (!jwt.isNullOrBlank() && jwtTokenProvider.validateToken(jwt)) {
                val userId = jwtTokenProvider.getUserIdFromToken(jwt)
                val user = userRepository.findById(userId).orElse(null)
                if (user != null && user.isActive) {
                    val principal = UserPrincipal.fromUser(user)
                    val authentication = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        } catch (ex: Exception) {
            logger.error("Could not set user authentication in security context", ex)
        }

        filterChain.doFilter(request, response)
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (!bearerToken.isNullOrBlank() && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        "/api/v1/auth/**",
                        "/api/v1/phone/**",
                        "/api/v1/webhooks/**",
                        "/actuator/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**"
                    ).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
