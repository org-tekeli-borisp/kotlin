import Kt


private func testNativeEnumValues() throws {
    let ktEnum = MyKotlinEnum.a
    let nsEnum = ktEnum.toNSEnum()

    switch(nsEnum) {
        case .A: try assertEquals(actual: nsEnum, expected: ktEnum.toNSEnum())
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
