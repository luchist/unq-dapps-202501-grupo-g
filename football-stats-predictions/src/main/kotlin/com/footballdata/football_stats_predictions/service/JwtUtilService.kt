package com.footballdata.football_stats_predictions.service

import com.sun.org.apache.xml.internal.security.algorithms.SignatureAlgorithm
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

@Service
class JwtUtilService {
    fun extractUsername(token: String?): String {
        return extractClaim<String?>(token, Claims::getSubject)!!
    }

    fun extractExpiration(token: String?): Date? {
        return extractClaim<Date?>(token, Claims::getExpiration)
    }

    fun <T> extractClaim(token: String?, claimsResolver: Function<Claims?, T?>): T? {
        return claimsResolver.apply(extractAllClaims(token))
    }

    private fun extractAllClaims(token: String?): Claims {
        return Jwts.parser().setSigningKey(JWT_SECRET_KEY).parseClaimsJws(token).getBody()
    }

    private fun isTokenExpired(token: String?): Boolean {
        return extractExpiration(token)!!.before(Date())
    }

    fun generateToken(userDetails: UserDetails): String {
        val claims: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        // Agregando informacion adicional como "claim"
        val rol = userDetails.getAuthorities().stream().collect(Collectors.toList()).get(0)
        claims.put("rol", rol)
        return createToken(claims, userDetails.getUsername())
    }

    private fun createToken(claims: MutableMap<String?, Any?>?, subject: String?): String {
        return Jwts
            .builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
            .signWith(SignatureAlgorithm.HS256, JWT_SECRET_KEY)
            .compact()
    }

    fun validateToken(token: String?, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return (username == userDetails.getUsername() && !isTokenExpired(token))
    }

    companion object {
        // LLAVE_MUY_SECRETA => [Base64] => TExBVkVfTVVZX1NFQ1JFVEE=
        private const val JWT_SECRET_KEY = "TExBVkVfTVVZX1NFQ1JFVEE="

        val JWT_TOKEN_VALIDITY: Long = 1000 * 60 * 60 * 8L // 8 Horas
    }
}