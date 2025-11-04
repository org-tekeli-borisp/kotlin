plugins {
    kotlin("jvm")
    application
    id("project-tests-convention")
}

dependencies {
    implementation(project(":generators"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
    testImplementation(kotlinTest("junit5"))
    testImplementation(platform(libs.junit.bom))
}

application {
    mainClass.set("org.jetbrains.kotlin.gradle.generators.version.MainKt")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        useJUnitPlatform()
    }
}