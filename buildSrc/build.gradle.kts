plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

allprojects {
    repositories {
        maven { setUrl("https://jitpack.io") }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.1")
    implementation("com.parmet:buf-gradle-plugin:0.8.5")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.9.4")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.0.0")
    implementation("com.github.node-gradle:gradle-node-plugin:7.0.2")
    implementation("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.4.3")
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.4.0")
    implementation(gradleApi())
    implementation(localGroovy())
}
