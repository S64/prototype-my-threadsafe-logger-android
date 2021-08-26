package com.kiganix.android.example.mytslp.lib

import android.app.Application
import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.streams.toList

class MyLogger(
    private val applicationContext: Context
) {

    private val dir by lazy {
        File(applicationContext.cacheDir, "my-logger-caches").also {
            assert(it.exists() || it.mkdirs())
            assert(it.isDirectory && it.canRead() && it.canWrite())
        }
    }
    private fun filePath() = File(dir, "logs.txt")

    private val logLock: ReentrantLock = ReentrantLock()

    fun <T> useLog(block: (file: File) -> T): T {
        assert(!logLock.isHeldByCurrentThread)
        return logLock.withLock {
            val file = filePath()

            assert(file.exists() || file.createNewFile())
            assert(file.canRead() && file.canWrite())

            block(file)
        }
    }

    inline fun <reified T> record(entity: T, crossinline callback: () -> Unit = {}): Unit {
        useLog { file ->
            PrintWriter(FileWriter(file, true)).use { writer ->
                writer.println(
                    Json.encodeToString(entity)
                )
            }
            callback()
        }
    }

    fun takeRecords(
        block: (records: List<String>) -> List<String>
    ): Unit {
        useLog { file ->
            val records = BufferedReader(FileReader(file)).use {
                it.lineSequence().toList()
            }
            val result = block(records)

            if (result.isEmpty()) {
                assert(file.delete())
            } else {
                PrintWriter(FileWriter(file, false)).use { writer ->
                    result.forEach {
                        writer.println(it)
                    }
                }
            }
        }
    }

    fun clear() {
        useLog {
            assert(it.delete())
        }
    }

}