package com.example.myapplication.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "bill_data_1"
                ).allowMainThreadQueries().build()

                val billItemDao = db.billItemDao()

                // Use the BillList composable

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // GreetingView(Greeting().greet())
//                    BillList(billItems)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 第一个区域：汇总展示账单列表中的金额
                        val sumAmount = billItemDao.getSumAmount()
                        val totalAmount = sumAmount
                        Text(
                            text = "总金额: ¥%.2f".format(totalAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(16.dp)
                        )

                        // 第二个区域：展示账单列表
                        var all = billItemDao.getAll()
                        BillList(all)

                        // 第三个区域：添加新账单的按钮
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
                                            val maxId = billItemDao.getMaxId()
                                            val billItem = BillItem(
                                                amount,
                                                newItemDescription
                                            )
                                            val billItems = billItemDao.getAll()
                                            all =
                                                all + billItem
                                            showDialog = false
                                            newItemDescription = ""
                                            newItemAmount = ""
                                            billItemDao.insertAll(billItem)
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
fun BillList(items: List<BillItem>) {
    // Create a composable function for the bill list
    androidx.compose.foundation.lazy.LazyColumn {
        items(items) { item ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(text = item.description)
                Text(text = "¥%.2f".format(item.amount))
            }
        }
    }
}

@Database(entities = [BillItem::class], version = 1)
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


