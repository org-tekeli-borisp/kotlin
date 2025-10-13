import Kt


private func testNativeEnumValues() throws {
    let ktEnum = MyKotlinEnum.A
    let nsEnum = ktEnum.toNSEnum()

    switch(nsEnum) {
        case .A: try assertEquals(nsEnum, ktEnum.toNSEnum())
        case .B: try fail()
        case .C: try fail()
    }
}

class NativeEnumTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestNativeEnumValues", testNativeEnumValues)
     }
}
