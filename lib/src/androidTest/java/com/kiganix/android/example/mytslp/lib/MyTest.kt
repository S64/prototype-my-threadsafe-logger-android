package com.kiganix.android.example.mytslp.lib

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(AndroidJUnit4::class)
class MyTest {

    private fun applicationContext() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var target: MyLogger

    @Before
    fun setUp() {
        target = MyLogger(applicationContext()).apply {
            clear()
        }
    }

    @Test
    fun singleThread_processAll() {
        val entities = listOf<HogeEntity>(
            HogeEntity(a = "11", b = "111", c = Random.nextInt()),
            HogeEntity(a = "22", b = "222", c = Random.nextInt()),
            HogeEntity(a = "33", b = "333", c = Random.nextInt()),
        )

        entities.forEach {
            target.record(it)
        }

        target.takeRecords {
            assertEquals(entities.size, it.size)

            it.forEachIndexed { idx, record ->
                println("$idx: $record")
                val decoded = Json.decodeFromString<HogeEntity>(record)
                assertEquals(entities[idx], decoded)
            }

            return@takeRecords emptyList()
        }

        target.takeRecords {
            assertNotEquals(entities.size, it.size)
            assertEquals(0, it.size)

            return@takeRecords emptyList()
        }
    }

    @Test
    fun singleThread_processPartially() {
        val entities = listOf<HogeEntity>(
            HogeEntity(a = "11", b = "111", c = Random.nextInt()),
            HogeEntity(a = "22", b = "222", c = Random.nextInt()),
            HogeEntity(a = "33", b = "333", c = Random.nextInt()),
        )

        entities.forEach {
            target.record(it)
        }

        repeat(entities.size) { idx ->
            target.takeRecords { records ->
                val item = records[0]
                println("$idx: $item")
                val decoded = Json.decodeFromString<HogeEntity>(item)
                assertEquals(entities[idx], decoded)

                return@takeRecords records.subList(1, records.size)
            }
        }

        target.takeRecords { records ->
            assertEquals(0, records.size)
            return@takeRecords emptyList()
        }
    }

    @Test
    fun multiThread_processPartially(): Unit = runBlocking {
        val entities = mutableListOf<HogeEntity>()

        (0..20).map {
            async(newSingleThreadContext("thread${it}")) {
                println("thread${it}: start")
                delay(Random.nextLong(0, 500))
                println("thread${it}: exec")
                HogeEntity(a = "${it}", b = "${it}${it}", c = Random.nextInt()).let {
                    target.record(it) { entities.add(it) }
                }
                println("thread${it}: finish")
            }
        }.awaitAll()

        (0 until entities.size).map {
            async(newSingleThreadContext("thread${it}")) {
                delay(it * 50L)
                target.takeRecords { records ->
                    println("takeRecords: start ${it}")
                    val result = Json.decodeFromString<HogeEntity>(records[0])
                    assertEquals(entities[it], result)
                    println("takeRecords: finish ${it}")
                    return@takeRecords records.subList(1, records.size)
                }
            }
        }.awaitAll()
    }

}
