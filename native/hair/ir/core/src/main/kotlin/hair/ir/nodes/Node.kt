package hair.ir.nodes

interface Node {
    // structure
    val id: Int
    val form: Form
    val session get() = form.session
    val args: ArgsList

    // derived
    val uses: MutableList<Node>

    val registered: Boolean

    fun ensureUnique(): Node = form.ensureUnique(this)

    fun addUse(use: Node) {
        if (use !in uses) uses += use // FIXME linear search
    }


    // FIXME careful!!
    fun removeUse(use: Node) {
        uses.remove(use)
        if (uses.isEmpty() && this !is ControlFlow) {
            deregister()
        }
    }

    fun <R> accept(visitor: BuiltinNodeVisitor<R>): R

    fun paramName(index: Int): String
}

abstract class NodeBase(final override val form: Form, args: List<Node?>) : Node {
    // TODO var?
    final override var id: Int = - session.nextNodeId.also { session.nextNodeId = it.inc() }

    final override val args: ArgsList = ArgsList(this, args)

    final override val uses = mutableListOf<Node>()

    final override fun toString() = "$id = ${form.fullName}(${args.joinToString(", ") { it?.id.toString() }})"

    final override val registered: Boolean
        get() = id > 0
}

