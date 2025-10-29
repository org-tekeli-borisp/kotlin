package kotlin

fun <T> assertArrayEquals(expected: Array<out T>, actual: Array<out T>, message: String? = null) {
    if (!arraysEqual(expected, actual)) {
        val msg = if (message == null) "" else ", message = '$message'"
        fail("Unexpected array: expected = '$expected', actual = '$actual'$msg")
    }
}

private fun <T> arraysEqual(first: Array<out T>, second: Array<out T>): Boolean {
    if (first === second) return true
    if (first.size != second.size) return false
    for (index in 0..first.size - 1) {
        if (!equal(first[index], second[index])) return false
    }
    return true
}

private fun equal(first: Any?, second: Any?) =
    if (first is Array<*> && second is Array<*>) {
        arraysEqual(first, second)
    }
    else {
        first == second
    }

fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
    if (expected != actual) {
        val msg = if (message == null) "" else ", message = '$message'"
        fail("Unexpected value: expected = '$expected', actual = '$actual'$msg")
    }
}

fun <T> assertNotEquals(illegal: T, actual: T, message: String? = null) {
    if (illegal == actual) {
        val msg = if (message == null) "" else ", message = '$message'"
        fail("Illegal value: illegal = '$illegal', actual = '$actual'$msg")
    }
}

fun <T> assertSame(expected: T, actual: T, message: String? = null) {
    if (expected !== actual) {
        val msg = if (message == null) "" else ", message = '$message'"
        fail("Expected same instances: expected = '$expected', actual = '$actual'$msg")
    }
}

fun assertTrue(actual: Boolean, message: String? = null) = assertEquals(true, actual, message)

fun assertFalse(actual: Boolean, message: String? = null) = assertEquals(false, actual, message)

fun testTrue(f: () -> Boolean) {
    assertTrue(f(), f.toString())
}

fun testFalse(f: () -> Boolean) {
    assertFalse(f(), f.toString())
}

fun assertFails(block: () -> Unit): Throwable {
    try {
        block()
    } catch (t: Throwable) {
        return t
    }
    fail("Expected an exception to be thrown, but was completed successfully.")
}

fun fail(message: String? = null): Nothing {
    throw Throwable(message)
}
