package com.footballdata.football_stats_predictions.rest

import com.footballdata.football_stats_predictions.service.JwtUtilService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.web.bind.annotation.*
import pe.itana.springsecurityjwtdemo.model.AuthenticationReq
import pe.itana.springsecurityjwtdemo.model.TokenInfo

@RestController
@RequestMapping
class DemoRest {
    @Autowired
    private val authenticationManager: AuthenticationManager? = null

    @Autowired
    var usuarioDetailsService: UserDetailsService? = null

    @Autowired
    private val jwtUtilService: JwtUtilService? = null

    @get:GetMapping("/mensaje")
    val mensaje: ResponseEntity<*>
        get() {
            logger.info("Obteniendo el mensaje")

            val auth = SecurityContextHolder.getContext().getAuthentication()
            logger.info("Datos del Usuario: {}", auth.getPrincipal())
            logger.info("Datos de los Roles {}", auth.getAuthorities())
            logger.info("Esta autenticado {}", auth.isAuthenticated())

            val mensaje: MutableMap<String?, String?> = HashMap<String?, String?>()
            mensaje.put("contenido", "Hola Peru")
            return ResponseEntity.ok<MutableMap<String?, String?>?>(mensaje)
        }

    @get:GetMapping("/admin")
    val mensajeAdmin: ResponseEntity<*>
        get() {
            val auth = SecurityContextHolder.getContext().getAuthentication()
            logger.info("Datos del Usuario: {}", auth.getPrincipal())
            logger.info("Datos de los Permisos {}", auth.getAuthorities())
            logger.info("Esta autenticado {}", auth.isAuthenticated())

            val mensaje: MutableMap<String?, String?> = HashMap<String?, String?>()
            mensaje.put("contenido", "Hola Admin")
            return ResponseEntity.ok<MutableMap<String?, String?>?>(mensaje)
        }

    @get:GetMapping("/publico")
    val mensajePublico: ResponseEntity<*>
        get() {
            val auth = SecurityContextHolder.getContext().getAuthentication()
            logger.info("Datos del Usuario: {}", auth.getPrincipal())
            logger.info("Datos de los Permisos {}", auth.getAuthorities())
            logger.info("Esta autenticado {}", auth.isAuthenticated())

            val mensaje: MutableMap<String?, String?> = HashMap<String?, String?>()
            mensaje.put("contenido", "Hola. esto es publico")
            return ResponseEntity.ok<MutableMap<String?, String?>?>(mensaje)
        }


    @PostMapping("/publico/authenticate")
    fun authenticate(@RequestBody authenticationReq: AuthenticationReq): ResponseEntity<TokenInfo?> {
        logger.info("Autenticando al usuario {}", authenticationReq.getUsuario())

        authenticationManager!!.authenticate(
            UsernamePasswordAuthenticationToken(
                authenticationReq.getUsuario(),
                authenticationReq.getClave()
            )
        )

        val userDetails = usuarioDetailsService!!.loadUserByUsername(
            authenticationReq.getUsuario()
        )

        val jwt: String? = jwtUtilService.generateToken(userDetails)

        return ResponseEntity.ok<TokenInfo?>(TokenInfo(jwt))
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DemoRest::class.java)
    }
}