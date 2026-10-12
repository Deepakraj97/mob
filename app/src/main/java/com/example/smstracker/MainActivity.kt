package com.example.smstracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider

class MainActivity : Activity() {
    private lateinit var totals: TextView
    private lateinit var list: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }

        totals = TextView(this).apply { textSize = 16f; gravity = Gravity.CENTER_HORIZONTAL }
        val scan = Button(this).apply { text = "Scan past SMS (last 90 days)"; setOnClickListener { scanInbox() } }
        val share = Button(this).apply { text = "Open / share spreadsheet (CSV)"; setOnClickListener { shareCsv() } }
        list = ListView(this)

        root.addView(totals); root.addView(scan); root.addView(share)
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        if (!hasPerms()) requestPermissions(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS), 1)
        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (!hasPerms()) Toast.makeText(this, "SMS permission is required", Toast.LENGTH_LONG).show()
    }

    private fun hasPerms() =
        checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    private fun refresh() {
        val (items, debit, credit) = TransactionStore.summary(this)
        totals.text = "Spent ₹%.2f   |   Received ₹%.2f".format(debit, credit)
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }

    private fun scanInbox() {
        if (!hasPerms()) { requestPermissions(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS), 1); return }
        Toast.makeText(this, "Scanning…", Toast.LENGTH_SHORT).show()
        Thread {
            var added = 0
            val since = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
            contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                "date >= ?", arrayOf(since.toString()), "date ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val txn = SmsParser.parse(c.getString(0) ?: "", c.getString(1) ?: "", c.getLong(2))
                    if (txn != null && TransactionStore.add(this, txn)) added++
                }
            }
            runOnUiThread {
                Toast.makeText(this, "Added $added new transactions", Toast.LENGTH_LONG).show()
                refresh()
            }
        }.start()
    }

    private fun shareCsv() {
        val f = TransactionStore.file(this)
        if (!f.exists()) { Toast.makeText(this, "No transactions yet", Toast.LENGTH_SHORT).show(); return }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "Open / share with…"))
    }
}
