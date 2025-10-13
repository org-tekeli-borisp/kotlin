import kotlin.native.ObjCEnum
import kotlin.experimental.ExperimentalObjCEnum

// @file:OptIn(ExperimentalObjCEnum::class)

// package nativeEnum

@OptIn(kotlin.experimental.ExperimentalObjCEnum::class)
@ObjCEnum("OBJCFoo")
enum class MyKotlinEnum {
    A, B, C
}