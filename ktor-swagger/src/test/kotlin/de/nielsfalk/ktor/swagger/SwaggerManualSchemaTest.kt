package de.nielsfalk.ktor.swagger

import com.winterbe.expekt.should
import de.nielsfalk.ktor.swagger.version.shared.ModelOrModelReference
import de.nielsfalk.ktor.swagger.version.shared.ParameterInputType
import de.nielsfalk.ktor.swagger.version.v2.Swagger
import de.nielsfalk.ktor.swagger.version.v3.OpenApi
import io.ktor.resources.Resource
import io.ktor.server.resources.Resources
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.KtorDsl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import de.nielsfalk.ktor.swagger.version.v2.Operation as OperationV2
import de.nielsfalk.ktor.swagger.version.v2.Parameter as ParameterV2
import de.nielsfalk.ktor.swagger.version.v2.Response as ResponseV2
import de.nielsfalk.ktor.swagger.version.v3.Operation as OperationV3
import de.nielsfalk.ktor.swagger.version.v3.Response as ResponseV3

const val rectanglesLocation = "/rectangles"

@Resource(rectanglesLocation)
class rectangles

const val ref = "${'$'}ref"

val sizeSchemaMap = mapOf(
    "type" to "number",
    "minimum" to 0
)

fun rectangleSchemaMap(refBase: String) = mapOf(
    "type" to "object",
    "properties" to mapOf(
        "a" to mapOf("${'$'}ref" to "$refBase/size"),
        "b" to mapOf("${'$'}ref" to "$refBase/size")
    )
)
val rectangleSwagger = rectangleSchemaMap("#/definitions")
val rectangleOpenApi = rectangleSchemaMap("#/components/schemas")

fun rectanglesSchemaMap(refBase: String) = mapOf(
    "type" to "array",
    "items" to mapOf(
        "description" to "Rectangles",
        ref to "$refBase/Rectangle"
    )
)

val rectanglesSwagger = rectanglesSchemaMap("#/definitions")
val rectanglesOpenApi = rectanglesSchemaMap("#/components/schemas")

data class Rectangle(
    val a: Int,
    val b: Int
)

class SwaggerManualSchemaTest {
    private lateinit var swagger: Swagger
    private lateinit var openApi: OpenApi

    @KtorDsl
    private fun applicationCustomRoute(configuration: Route.() -> Unit) {
        testApplication {
            install(Resources)
            install(SwaggerSupport) {
                swagger = Swagger().apply {
                    definitions["size"] = sizeSchemaMap
                    definitions["Rectangle"] = rectangleSwagger
                    definitions["Rectangles"] = rectanglesSwagger
                }
                openApi = OpenApi().apply {
                    components.schemas["size"] = sizeSchemaMap
                    components.schemas["Rectangle"] = rectangleOpenApi
                    components.schemas["Rectangles"] = rectanglesOpenApi
                }

                this@SwaggerManualSchemaTest.swagger = swagger!!
                this@SwaggerManualSchemaTest.openApi = openApi!!
            }

            routing(configuration)
        }
    }

    @Test
    fun `custom ok return type`() {
        applicationCustomRoute {
            get<rectangles>("all".responds(ok("Rectangle"))) { }
        }
        swagger.definitions["size"].should.equal(sizeSchemaMap)
        openApi.components.schemas["size"].should.equal(sizeSchemaMap)
        swagger.definitions["Rectangle"].should.equal(rectangleSwagger)
        openApi.components.schemas["Rectangle"].should.equal(rectangleOpenApi)
        swagger.definitions["Rectangles"].should.equal(rectanglesSwagger)
        openApi.components.schemas["Rectangles"].should.equal(rectanglesOpenApi)

        openApi.paths[rectanglesLocation]?.get("get").apply {
            should.not.be.`null`
        }.also { operation ->
            operation as OperationV3

            operation.summary.should.equal("all")
            operation.responses.keys.should.contain("200")
            operation.requestBody.should.be.`null`
        }
    }

    @Test
    fun `custom put schema`() {
        applicationCustomRoute {
            put<rectangles, Rectangle>(
                "create".noReflectionBody().responds(
                    created("Rectangles")
                )
            ) { _, _ ->
            }
        }
        swagger.definitions["Rectangle"].should.equal(rectangleSwagger)
        openApi.components.schemas["Rectangle"].should.equal(rectangleOpenApi)
        swagger.definitions["Rectangles"].should.equal(rectanglesSwagger)
        openApi.components.schemas["Rectangles"].should.equal(rectanglesOpenApi)
        swagger.paths[rectanglesLocation]?.get("put").apply {
            should.not.be.`null`
        }?.also { operation ->
            operation as OperationV2

            operation.summary.should.equal("create")
            operation.responses.keys.should.contain("201")
            ((operation.parameters.find { it.`in` == ParameterInputType.body } as ParameterV2).schema as ModelOrModelReference)
                .`$ref`.should.equal("#/definitions/Rectangle")
            (operation.responses["201"] as ResponseV2).schema?.`$ref`.should.equal("#/definitions/Rectangles")
        }
        openApi.paths[rectanglesLocation]?.get("put").apply {
            should.not.be.`null`
        }?.also { operation ->
            operation as OperationV3

            operation.summary.should.equal("create")
            operation.responses.keys.should.contain("201")
            operation.requestBody.should.not.be.`null`
            operation.requestBody?.content?.get("application/json")
                ?.schema?.`$ref`.should.equals("#/components/schemas/Rectangle")
            (operation.responses["201"] as ResponseV3)
                .content?.get("application/json")?.schema?.`$ref`.should.equal("#/components/schemas/Rectangles")
        }
    }

    @Test
    fun `custom schema name on the receive type`() {
        val customName = "CustomName"
        applicationCustomRoute {
            post<rectangles, Rectangle>(
                "create".noReflectionBody(customName).responds(
                    created("Rectangles")
                )
            ) { _, _ ->
            }
        }
        swagger.paths[rectanglesLocation]?.get("post").apply {
            should.not.be.`null`
        }?.also { operation ->
            operation as OperationV2
            (operation.parameters.single { it.`in` == ParameterInputType.body } as ParameterV2).schema?.`$ref`
            .should.equal("#/definitions/$customName")
        }

        openApi.paths[rectanglesLocation]?.get("post").apply {
            should.not.be.`null`
        }?.also { operation ->
            operation as OperationV3
            operation.requestBody?.content?.entries?.single()
                ?.value?.schema?.`$ref`
                .should.equal("#/components/schemas/$customName")
        }
    }

    @Test
    fun `body in get throws exception`() {
        assertThrows<IllegalArgumentException> {
            applicationCustomRoute {
                get<rectangles>("Get All".noReflectionBody()) {}
            }
        }
    }

    @Test
    fun `body in delete throws exception`() {
        assertThrows<IllegalArgumentException> {
            applicationCustomRoute {
                delete<rectangles>("Delete All".noReflectionBody()) {}
            }
        }
    }
}
