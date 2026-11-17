package com.example.group2mts

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.group2mts.data.HydrationRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LogDrinkActivity : AppCompatActivity() {

    private data class HardcodedDrink(
        val name: String,
        val category: String,
        val factorPct: Int,
        val colorHex: String
    )

    private val drinks = listOf(
        HardcodedDrink("Water", "Water", 100, "#1FB6D5"),
        HardcodedDrink("Green tea", "Tea", 90, "#7FB685"),
        HardcodedDrink("Coffee", "Coffee", 80, "#A68A7B"),
        HardcodedDrink("Juice", "Juice", 95, "#F3B562"),
        HardcodedDrink("Milk", "Milk", 90, "#9AD1EA"),
        HardcodedDrink("Soda", "Soda", 80, "#E598A8")
    )

    private val repo = HydrationRepository()
    private var selectedDrink = drinks[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_log_drink)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val drinkButtons = listOf(
            findViewById<MaterialButton>(R.id.btnDrinkWater),
            findViewById<MaterialButton>(R.id.btnDrinkTea),
            findViewById<MaterialButton>(R.id.btnDrinkCoffee),
            findViewById<MaterialButton>(R.id.btnDrinkJuice),
            findViewById<MaterialButton>(R.id.btnDrinkMilk),
            findViewById<MaterialButton>(R.id.btnDrinkSoda)
        )

        val etAmount = findViewById<TextInputEditText>(R.id.etAmountMl)
        val tvPreview = findViewById<TextView>(R.id.tvCreditPreview)

        fun updatePreview() {
            val rawMl = etAmount.text.toString().toIntOrNull() ?: 0
            val credited = (rawMl * selectedDrink.factorPct / 100.0).roundToInt()
            tvPreview.text = "Credited hydration: $credited ml (${selectedDrink.name} · ${selectedDrink.factorPct}%)"
        }

        fun highlightDrink(selectedIdx: Int) {
            drinkButtons.forEachIndexed { idx, btn ->
                val colorRes = if (idx == selectedIdx) R.color.primary else R.color.surface
                btn.backgroundTintList = ColorStateList.valueOf(getColor(colorRes))
            }
            updatePreview()
        }

        drinkButtons.forEachIndexed { idx, btn ->
            btn.setOnClickListener {
                selectedDrink = drinks[idx]
                highlightDrink(idx)
            }
        }

        mapOf(
            R.id.btnAmt150 to 150,
            R.id.btnAmt250 to 250,
            R.id.btnAmt350 to 350,
            R.id.btnAmt500 to 500
        ).forEach { (btnId, ml) ->
            findViewById<MaterialButton>(btnId).setOnClickListener {
                etAmount.setText(ml.toString())
                updatePreview()
            }
        }

        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updatePreview()
            override fun afterTextChanged(s: Editable?) {}
        })

        val btnSave = findViewById<MaterialButton>(R.id.btnSaveDrink)
        btnSave.setOnClickListener {
            val consumedMl = etAmount.text.toString().toIntOrNull() ?: 0
            if (consumedMl <= 0) {
                toast("Enter a valid amount in ml")
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            lifecycleScope.launch {
                try {
                    repo.logDrink(
                        name = selectedDrink.name,
                        category = selectedDrink.category,
                        colorHex = selectedDrink.colorHex,
                        consumedMl = consumedMl,
                        factorPct = selectedDrink.factorPct
                    )
                    toast("Logged $consumedMl ml of ${selectedDrink.name}!")
                    finish()
                } catch (e: Exception) {
                    toast(e.message ?: "Failed to log drink")
                    btnSave.isEnabled = true
                }
            }
        }

        highlightDrink(0)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}