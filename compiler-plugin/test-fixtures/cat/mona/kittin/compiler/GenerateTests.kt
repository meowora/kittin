package cat.mona.kittin.compiler

import cat.mona.kittin.compiler.runners.AbstractJvmBoxTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "compiler-plugin/testData", testsRoot = "compiler-plugin/test-gen") {

            testClass<AbstractJvmBoxTest> {
                model("box")
            }
        }
    }
}
