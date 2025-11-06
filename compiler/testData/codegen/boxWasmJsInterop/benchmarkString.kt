/*
 * Copyright 2023 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package microBenchmarks


inline fun Array<Value>.cnt(predicate: (Value) -> Boolean): Int {
    var count = 0
    for (element in this) {
        if (predicate(element))
            count++
    }
    return count
}

fun classValues(size: Int): Iterable<Value> {
    return intValues(size).map { Value(it) }
}

fun intValues(size: Int): Iterable<Int> {
    return 1..size
}

open class Value(var value: Int) {
    val text = value.toString().reversed()
}

fun filterLoad(v: Value): Boolean {
    return v.value.toString() in v.text
}

fun mapLoad(v: Value): String = v.text.reversed()

class ClassArrayBenchmark {
    private lateinit var data: Array<Value>
    val BENCHMARK_SIZE = 10_000

    fun setup() {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (n in classValues(BENCHMARK_SIZE)) {
            list.add(n)
        }
        data = list.toTypedArray()
    }

    fun copy(): List<Value> {
        return data.toList()
    }

    fun copyManual(): List<Value> {
        val data = data
        val list = ArrayList<Value>(data.size)
        for (item in data) {
            list.add(item)
        }
        return list
    }

    fun filterAndCount(): Int {
        return data.filter { filterLoad(it) }.count()
    }

    fun filterAndMap(): List<String> {
        return data.filter { filterLoad(it) }.map { mapLoad(it) }
    }

    fun filterAndMapManual(): ArrayList<String> {
        val list = ArrayList<String>()
        for (it in data) {
            if (filterLoad(it)) {
                val value = mapLoad(it)
                list.add(value)
            }
        }
        return list
    }

    fun filter(): List<Value> {
        return data.filter { filterLoad(it) }
    }

    fun filterManual(): List<Value> {
        val list = ArrayList<Value>()
        for (it in data) {
            if (filterLoad(it))
                list.add(it)
        }
        return list
    }

    fun countFilteredManual(): Int {
        var count = 0
        for (it in data) {
            if (filterLoad(it))
                count++
        }
        return count
    }

    fun countFiltered(): Int {
        return data.count { filterLoad(it) }
    }

    fun countFilteredLocal(): Int {
        return data.cnt { filterLoad(it) }
    }

    fun Any.isPalindrome() = toString() == toString().reversed()

    fun problem4(): Long {
        val s: Long = BENCHMARK_SIZE.toLong()
        val maxLimit = (s-1)*(s-1)
        val minLimit = (s/10)*(s/10)
        val maxDiv = BENCHMARK_SIZE-1
        val minDiv = BENCHMARK_SIZE/10
        for (i in maxLimit downTo minLimit) {
            if (!i.isPalindrome()) continue;
            for (j in minDiv..maxDiv) {
                if (i % j == 0L) {
                    val res = i / j
                    if (res in minDiv.toLong()..maxDiv.toLong()) {
                        return i
                    }
                }
            }
        }
        return -1
    }
}

fun evalBenches(benchmarks: ClassArrayBenchmark) {
    var sum = 0
    for (i in 0..9) {
        sum += benchmarks.countFiltered()
    }
    if (sum % 10 != 0) {
        throw Exception("Fail 1 $sum")
    }
    sum = 0
    for (i in 0..9) {
        sum += benchmarks.filterAndCount()
    }
    if (sum % 10 != 0) {
        throw Exception("Fail 2 $sum")
    }
    sum = 0
    for (i in 0..9) {
        sum += benchmarks.filterManual().size
    }
    if (sum % 10 != 0) {
        throw Exception("Fail 3 $sum")
    }
    var sum1 = 0L
    for (i in 0..9) {
        sum1 += benchmarks.problem4()
    }
    if (sum1 % 10 != 0L) {
        throw Exception("Fail 4 $sum")
    }
}

fun warmUp(benchmarks: ClassArrayBenchmark) {
    evalBenches(benchmarks)
}

fun box(): String {
    val benchmarks = ClassArrayBenchmark()
    benchmarks.setup()
    warmUp(benchmarks)

    evalBenches(benchmarks)
    evalBenches(benchmarks)
    evalBenches(benchmarks)
    return "OK"
}