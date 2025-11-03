interface Base {
    val value: Int
}

class Delegate : Base {
    override val value: Int = 42
}

class Derived(val d: Delegate) : Base by d

// class: Derived
