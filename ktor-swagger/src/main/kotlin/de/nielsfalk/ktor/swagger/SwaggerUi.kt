package de.nielsfalk.ktor.swagger

import io.ktor.server.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Image.PNG
import io.ktor.http.ContentType.Text.CSS
import io.ktor.http.ContentType.Text.Html
import io.ktor.http.ContentType.Text.JavaScript
import io.ktor.http.content.OutgoingContent
import io.ktor.http.withCharset
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import java.net.URL

class SwaggerUi(private val defaultJsonFile: String, private val nonce: (ApplicationCall) -> String?) {
    private val notFound = mutableListOf<String>()
    private val content = mutableMapOf<String, ResourceContent>()
    suspend fun serve(filename: String?, call: ApplicationCall) {
        when (filename) {
            in notFound -> return
            null -> return
            else -> {
                val resource = this::class.java.getResource("/META-INF/resources/webjars/swagger-ui/5.18.2/$filename")
                if (resource == null) {
                    notFound.add(filename)
                    return
                }

                if (filename == "swagger-initializer.js") {
                    val originalBody = resource.readText()
                    val newBody = originalBody.replace(
                        "https://petstore.swagger.io/v2/swagger.json",
                        defaultJsonFile
                    )

                    call.respondText(newBody, JavaScript)
                } else if (filename == "index.html") {
                    val originalBody = resource.readText()
                    val nonceValue = nonce(call)

                    val newBody = if (nonceValue != null) {
                        originalBody.replace(
                            Regex("(rel=\"stylesheet\"|script src=\"[^\"]*\")"),
                        ) {
                            (it.groups[0]?.value ?: "") + " nonce=\"$nonceValue\""
                        }
                    } else {
                        originalBody
                    }

                    call.respondText(newBody, Html)
                } else {
                    call.respond(content.getOrPut(filename) { ResourceContent(resource) })
                }
            }
        }
    }
}

private val contentTypes = mapOf(
        "html" to Html,
        "css" to CSS,
        "js" to JavaScript,
        "json" to ContentType.Application.Json.withCharset(Charsets.UTF_8),
        "png" to PNG)

private class ResourceContent(val resource: URL) : OutgoingContent.ByteArrayContent() {
    private val bytes by lazy { resource.readBytes() }

    override val contentType: ContentType? by lazy {
        val extension = resource.file.substring(resource.file.lastIndexOf('.') + 1)
        contentTypes[extension] ?: Html
    }

    override val contentLength: Long? by lazy {
        bytes.size.toLong()
    }

    override fun bytes(): ByteArray = bytes
    override fun toString() = "ResourceContent \"$resource\""
}
