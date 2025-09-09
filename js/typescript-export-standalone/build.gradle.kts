plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

description = "Standalone Runner for TypeScript Export"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":core:compiler.common.js"))

    implementation(project(":analysis:analysis-api-standalone"))
    implementation(project(":libraries:tools:analysis-api-based-klib-reader"))

    implementation(project(":js:js.ast"))
    implementation(project(":js:typescript-export-model"))
    implementation(project(":js:typescript-printer"))

    api(project(":js:js.config"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
