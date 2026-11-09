package com.ashis.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- 1. ROOM DATABASE SETUP ---
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long = System.currentTimeMillis()
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert
    suspend fun insert(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)
}

@Database(entities = [Expense::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}

// --- 2. MAIN ACTIVITY & UI ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "expense-db").build()
        val dao = db.expenseDao()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0E17),
                    surface = Color(0xFF141B2D),
                    primary = Color(0xFF00F0FF)
                )
            ) {
                ExpenseTrackerScreen(dao)
            }
        }
    }
}

@Composable
fun ExpenseTrackerScreen(dao: ExpenseDao) {
    val expenses by dao.getAllExpenses().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val categories = listOf("Food", "Travel", "Recharge", "Study", "Other")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    val totalExpense = expenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .padding(16.dp)
    ) {
        // Header
        Text("CYBER VAULT // EXPENSES", color = Color(0xFF00F0FF), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Monthly / Total Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141B2D)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("TOTAL SPENT", color = Color.Gray, fontSize = 12.sp)
                Text("₹${String.format("%.2f", totalExpense)}", color = Color(0xFF00F0FF), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(12.dp))

                // Category-wise mini visual bar breakdown
                if (totalExpense > 0) {
                    categories.forEach { cat ->
                        val catTotal = expenses.filter { it.category == cat }.sumOf { it.amount }
                        if (catTotal > 0) {
                            val progress = (catTotal / totalExpense).toFloat()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                LinearProgressIndicator(
                                    progress = progress,
                                    color = Color(0xFF00F0FF),
                                    trackColor = Color(0xFF232F46),
                                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Text(" ₹${catTotal.toInt()}", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Expense Inputs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Kharcha (e.g. Momo)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("₹ Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Selector Chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            categories.forEach { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF00F0FF) else Color(0xFF141B2D))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val amt = amount.toDoubleOrNull()
                if (title.isNotBlank() && amt != null && amt > 0) {
                    scope.launch {
                        dao.insert(Expense(title = title.trim(), amount = amt, category = selectedCategory))
                        title = ""
                        amount = ""
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ ADD EXPENSE", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("RECENT LOGS", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Expense History List with Delete Icon Button
        val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(expenses, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B2D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${item.category} • ${dateFormat.format(Date(item.date))}", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text("₹${item.amount}", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            scope.launch { dao.delete(item) }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Expense",
                                tint = Color(0xFFFF5555) // Cyber red tint for delete icon
                            )
                        }
                    }
                }
            }
        }
    }
}
