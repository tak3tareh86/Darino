package com.financemanager.backend.security

import com.financemanager.backend.domain.User
import com.financemanager.backend.repository.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

data class UserPrincipal(
    val id: String,
    val usernameVal: String,
    val phoneNumberVal: String?,
    val isPhoneVerifiedVal: Boolean,
    val passwordVal: String,
    val authoritiesVal: Collection<GrantedAuthority>
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = authoritiesVal
    override fun getPassword(): String = passwordVal
    override fun getUsername(): String = usernameVal
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    companion object {
        fun create(user: User): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority(user.role))
            return UserPrincipal(
                id = user.id,
                usernameVal = user.username,
                phoneNumberVal = user.phoneNumber,
                isPhoneVerifiedVal = user.phoneVerified,
                passwordVal = user.passwordHash,
                authoritiesVal = authorities
            )
        }
    }
}

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            .orElseGet {
                userRepository.findByPhoneNumber(username)
                    .orElseThrow { UsernameNotFoundException("User not found with username or phone: $username") }
            }
        return UserPrincipal.create(user)
    }

    fun loadUserById(id: String): UserPrincipal {
        val user = userRepository.findById(id)
            .orElseThrow { UsernameNotFoundException("User not found with id: $id") }
        return UserPrincipal.create(user)
    }
}
