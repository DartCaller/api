package com.dartcaller.routes.ws

import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.ConfigFactory
import io.ktor.http.cio.websocket.*
import java.security.interfaces.RSAPublicKey

fun verifyToken(token: String): DecodedJWT {
    val jwkProvider = UrlJwkProvider(ConfigFactory.load().getString("auth0.issuer"))

    val jwt = JWT.decode(token)
    val jwk = jwkProvider.get(jwt.keyId)

    val publicKey = jwk.publicKey as? RSAPublicKey ?: throw Exception("Invalid key type") // safe

    val algorithm = when (jwk.algorithm) {
        "RS256" -> Algorithm.RSA256(publicKey, null)
        else -> throw Exception("Unsupported algorithm")
    }

    val verifier = JWT.require(algorithm) // signature
        .withIssuer(ConfigFactory.load().getString("auth0.issuer")) // iss
        .withAudience(ConfigFactory.load().getString("auth0.audience")) // aud
        .build()

    return verifier.verify(token)
}

class AuthenticateEvent(
    val accessToken: String,
) : WsEvent("Authenticate")

class AuthenticatedResult(
    val authenticated: Boolean
) {
    fun toJson(): String {
        return jacksonObjectMapper().writeValueAsString(this)
    }
}

suspend fun authenticateWs(event: AuthenticateEvent, socket: Connection) {
    println("Not yet authenticated: ${socket.name}")
    try {
        verifyToken(event.accessToken)
        socket.authenticated = true
        println("Authenticated: ${socket.name}")
        socket.session.outgoing.send(
            Frame.Text(
                AuthenticatedResult(true).toJson()
            )
        )
    } catch (e: Exception) {
        println(e)
    }
}
