package com.example.myapplication.android

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "bill_data_1"
                ).allowMainThreadQueries().build()

                val billItemDao = db.billItemDao()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val sumAmount = billItemDao.getSumAmount()
                        val totalAmount = sumAmount
                        Text(
                            text = "总金额: ¥%.2f".format(totalAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(16.dp)
                        )

                        var all by remember { mutableStateOf(billItemDao.getAll()) }
                        BillList(all) { item ->
                            billItemDao.delete(item)
                            all = billItemDao.getAll()
                        }

                        var showDialog by remember { mutableStateOf(false) }
                        var newItemDescription by remember { mutableStateOf("") }
                        var newItemAmount by remember { mutableStateOf("") }

                        Button(
                            onClick = { showDialog = true },
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Text("添加新账单")
                        }

                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = { showDialog = false },
                                title = { Text("添加新账单") },
                                text = {
                                    Column {
                                        TextField(
                                            value = newItemDescription,
                                            onValueChange = { newItemDescription = it },
                                            label = { Text("事项名称") }
                                        )
                                        TextField(
                                            value = newItemAmount,
                                            onValueChange = { newItemAmount = it },
                                            label = { Text("金额") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        val amount = newItemAmount.toDoubleOrNull()
                                        if (amount != null && newItemDescription.isNotBlank()) {
                                            val billItem = BillItem(
                                                amount,
                                                newItemDescription
                                            )
                                            billItemDao.insertAll(billItem)
                                            all = billItemDao.getAll()
                                            showDialog = false
                                            newItemDescription = ""
                                            newItemAmount = ""
                                        }
                                    }) {
                                        Text("确定")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = { showDialog = false }) {
                                        Text("取消")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        createNotificationChannel()
        setAlarm(this, System.currentTimeMillis() + 15000)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setAlarm(context: Context, triggerAtMillis: Long) {
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent,
            PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canScheduleExactAlarms = alarmManager.canScheduleExactAlarms()
        if (canScheduleExactAlarms) alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Channel Name"
            val descriptionText = "Channel Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("your_channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun GreetingView(text: String) {
    return Text(text = text + "等我")
}

@Entity
data class BillItem(
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "description") val description: String
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}

@Dao
interface BillItemDao {
    @Query("SELECT * FROM billitem")
    fun getAll(): List<BillItem>

    @Query("SELECT * FROM billitem WHERE id IN (:idList)")
    fun loadById(idList: List<Int>): List<BillItem>

    @Query("SELECT MAX(id) FROM billitem")
    fun getMaxId(): Int

    @Query("SELECT SUM(amount) FROM billitem")
    fun getSumAmount(): Double

    @Insert
    fun insertAll(vararg billItem: BillItem)

    @Insert
    fun insertAllByList(billItems: List<BillItem>)

    @Delete
    fun delete(billItem: BillItem)
}

@Composable
fun BillList(items: List<BillItem>, onDelete: (BillItem) -> Unit) {
    androidx.compose.foundation.lazy.LazyColumn {
        items(items) { item ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.description)
                Text(text = "¥%.2f".format(item.amount))
                IconButton(onClick = { onDelete(item) }) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        }
    }
}

@Database(entities = [BillItem::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billItemDao(): BillItemDao
}

@Preview
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        GreetingView("Hello, Android!")
    }
}
