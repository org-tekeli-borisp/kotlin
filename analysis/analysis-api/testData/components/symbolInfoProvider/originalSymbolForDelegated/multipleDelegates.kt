interface Base1 {
    fun foo()
}

interface Base2 {
    fun bar()
}

class Delegate1 : Base1 {
    override fun foo() {}
}

class Delegate2 : Base2 {
    override fun bar() {}
}

class Derived(val d1: Delegate1, val d2: Delegate2) : Base1 by d1, Base2 by d2

// class: Derived
