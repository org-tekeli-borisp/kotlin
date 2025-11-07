// LL_FIR_DIVERGENCE
// AA doesn't run the compilation, so no metadata is present
// See KmpCompilationMode.LOW_LEVEL_API
// LL_FIR_DIVERGENCE

// RUN_PIPELINE_TILL: KLIB
// LANGUAGE: +MultiPlatformProjects
// MODULE: commonjs
// FILE: commonjs.kt

expect interface ExternalInterface

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>ExternalClass<!>: ExternalInterface

// MODULE: js()()(commonjs)
// FILE: js.kt

actual external interface ExternalInterface
