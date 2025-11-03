interface Base {
    fun foo()
}

class Delegate : Base {
    override fun foo() {}
}

class Derived(val d: Delegate) : Base by d

// class: Derived
