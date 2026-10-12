package com.example.smstracker

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stores transactions in a CSV file (opens directly in Excel / Google Sheets). */
object TransactionStore {
    const val FILE_NAME = "transactions.csv"
    private const val HEADER = "Date,Time,Type,Amount,Merchant,Account,Balance,Sender,SMS,Id"
    private val lock = Any()

    fun file(ctx: Context) = File(ctx.filesDir, FILE_NAME)

    private fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    private fun idOf(t: Txn) = "${t.timestamp}_${t.body.hashCode()}"

    /** Returns true if a new row was added (false if duplicate). */
    fun add(ctx: Context, t: Txn): Boolean = synchronized(lock) {
        val f = file(ctx)
        val id = idOf(t)
        if (!f.exists()) {
            f.writeText("\uFEFF$HEADER\n", Charsets.UTF_8) // BOM so Excel shows ₹ and unicode correctly
        } else if (f.readText().contains(id)) {
            return false
        }
        val d = Date(t.timestamp)
        val row = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d),
            SimpleDateFormat("HH:mm", Locale.US).format(d),
            t.type,
            String.format(Locale.US, "%.2f", t.amount),
            esc(t.merchant), esc(t.account), t.balance, esc(t.sender), esc(t.body), id
        ).joinToString(",")
        f.appendText(row + "\n", Charsets.UTF_8)
        true
    }

    /** Last [limit] rows as short display lines plus (debit total, credit total). */
    fun summary(ctx: Context, limit: Int = 50): Triple<List<String>, Double, Double> {
        val f = file(ctx)
        if (!f.exists()) return Triple(emptyList(), 0.0, 0.0)
        val lines = f.readLines(Charsets.UTF_8).drop(1).filter { it.isNotBlank() }
        var debit = 0.0
        var credit = 0.0
        val items = lines.map { line ->
            val cols = splitCsv(line)
            val amt = cols.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            val type = cols.getOrNull(2) ?: ""
            if (type == "DEBIT") debit += amt else credit += amt
            val sign = if (type == "DEBIT") "−" else "+"
            "${cols.getOrNull(0)}  $sign₹${cols.getOrNull(3)}  ${cols.getOrNull(4)}"
        }
        return Triple(items.takeLast(limit).reversed(), debit, credit)
    }

    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var q = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && q && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                ch == '"' -> q = !q
                ch == ',' && !q -> { out.add(sb.toString()); sb.clear() }
                else -> sb.append(ch)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}
