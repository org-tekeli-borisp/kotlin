// This test checks that some special well-known keywords and other kinds of reserved names
// can still be used as identifiers in some regular contexts.

package foo

fun box(): String {
    js("""
        var async = function() { this.value = 1; };
        var as = function() { this.value = 1; };
        var from = function() { this.value = 1; };
        var of = function() { this.value = 1; };
        var target = function() { this.value = 1; };
        var meta = function() { this.value = 1; };
    """)
    val obj = js("""({
        async: new async,
        as: new as,
        from: new from,
        of: new of,
        target: new target,
        meta: new meta
    })""")
    assertEquals(obj.`async`, 1)
    assertEquals(obj.`as`, 1)
    assertEquals(obj.`from`, 1)
    assertEquals(obj.`of`, 1)
    assertEquals(obj.`target`, 1)

    return "OK"
}