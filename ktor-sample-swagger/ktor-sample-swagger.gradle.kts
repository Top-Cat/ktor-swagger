import org.jetbrains.kotlin.contracts.model.structure.UNKNOWN_COMPUTATION.type

plugins {
    `application`
}

fun DependencyHandler.ktor(name: String) =
    create(group = "io.ktor", name = name, version = "2.0.3")

dependencies {
    implementation(project(":ktor-swagger"))
    implementation(ktor("ktor-server-netty"))
    implementation(ktor("ktor-server-content-negotiation"))
    implementation(ktor("ktor-serialization-gson"))
    implementation(group = "com.github.ajalt", name = "clikt", version = "1.3.0")
}

application {
    mainClass = "de.nielsfalk.ktor.swagger.sample.JsonApplicationKt"
}
