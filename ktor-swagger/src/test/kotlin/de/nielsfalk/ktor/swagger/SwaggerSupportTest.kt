package de.nielsfalk.ktor.swagger

import com.winterbe.expekt.should
import de.nielsfalk.ktor.swagger.version.v2.Swagger
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.resources.Resource
import io.ktor.serialization.gson.GsonConverter
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

class SwaggerSupportTest {
    @Test
    fun `installed apidocs`(): Unit = testApplication {
        // when
        install(SwaggerSupport) {
            forwardRoot = true
            swagger = Swagger()
        }

        val client = createClient {
            followRedirects = false
        }

        client.get("/").let { response ->
            response.headers["Location"].should.equal("/apidocs/index.html")
        }
        client.get("/apidocs").let { response ->
            response.headers["Location"].should.equal("/apidocs/index.html")
        }
        client.get("/apidocs/").let { response ->
            response.headers["Location"].should.equal("/apidocs/index.html")
        }
    }

    @Test
    fun `provide webjar`(): Unit = testApplication {
        // when
        install(SwaggerSupport) {
            forwardRoot = true
            swagger = Swagger()
        }

        val client = createClient {
            followRedirects = false
        }

        client.get("/apidocs/index.html")
            .bodyAsText().should.contain("<title>Swagger UI</title>")
    }

    @Test
    fun `provide swaggerJson`(): Unit = testApplication {
        // when
        install(ContentNegotiation) {
            register(ContentType.Application.Json, GsonConverter())
        }
        install(SwaggerSupport) {
            forwardRoot = true
            swagger = Swagger()
        }

        // then
        val client = createClient {
            followRedirects = false
        }

        client.get("/apidocs/swagger.json") {
            accept(ContentType.Application.Json)
        }.bodyAsText().should.contain("\"swagger\":\"2.0\"")
    }

    @Resource("/model")
    private class modelRoute

    private class Model(val value: String)

    @Test
    fun `provide swaggerJson when a custom schema is provided`(): Unit = testApplication {
        // when
        install(ContentNegotiation) {
            register(ContentType.Application.Json, GsonConverter())
        }
        install(SwaggerSupport) {
            forwardRoot = true
            swagger = Swagger().apply {
                definitions["Model"] = mapOf("type" to "object")
            }
        }
        install(Resources)

        routing {
            put<modelRoute, Model>(noReflectionBody()) { _, _ -> }
        }

        // then
        val client = createClient {
            followRedirects = false
        }

        client.get("/apidocs/swagger.json") {
            accept(ContentType.Application.Json)
        }.bodyAsText().should.contain("\"swagger\":\"2.0\"").and.contain("\"type\":\"object\"")
    }
}
