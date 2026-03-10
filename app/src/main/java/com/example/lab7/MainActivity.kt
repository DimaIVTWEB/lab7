package com.example.lab7

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnRefresh: Button
    private lateinit var requestQueue: RequestQueue

    private val currencyAdapter = CurrencyAdapter()

    // Используем бесплатный API (не требует ключа)
    private val apiUrl = "https://api.exchangerate-api.com/v4/latest/RUB"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()

        // Инициализация Volley
        requestQueue = Volley.newRequestQueue(this)

        // Загрузка данных при старте
        loadExchangeRates()

        // Обработчик кнопки обновления
        btnRefresh.setOnClickListener {
            loadExchangeRates()
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        tvError = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)
        btnRefresh = findViewById(R.id.btnRefresh)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = currencyAdapter
    }

    private fun loadExchangeRates() {
        showLoading(true)
        tvError.visibility = View.GONE

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET,
            apiUrl,
            null,
            { response ->
                parseResponse(response)
                showLoading(false)
            },
            { error ->
                showError("Ошибка: ${error.message}")
                showLoading(false)
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun parseResponse(response: JSONObject) {
        try {
            val baseCurrency = response.getString("base")
            val ratesObject = response.getJSONObject("rates")

            val currencyList = mutableListOf<CurrencyItem>()
            val keys = ratesObject.keys()

            while (keys.hasNext()) {
                val code = keys.next()
                val rate = ratesObject.getDouble(code)
                currencyList.add(CurrencyItem(code, rate))
            }

            // Сортировка по коду валюты
            currencyList.sortBy { it.code }

            currencyAdapter.submitList(currencyList, baseCurrency)

        } catch (e: Exception) {
            showError("Ошибка парсинга: ${e.message}")
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Data class для валюты
    data class CurrencyItem(val code: String, val rate: Double)

    // Adapter для RecyclerView
    inner class CurrencyAdapter : RecyclerView.Adapter<CurrencyAdapter.ViewHolder>() {

        private var items: List<CurrencyItem> = emptyList()
        private var baseCurrency: String = "USD"

        fun submitList(newItems: List<CurrencyItem>, base: String) {
            items = newItems
            baseCurrency = base
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvCode: TextView = itemView.findViewById(R.id.tvCurrencyCode)
            val tvRate: TextView = itemView.findViewById(R.id.tvCurrencyRate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_currency, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvCode.text = item.code

            // Форматирование числа
            val format = NumberFormat.getNumberInstance(Locale.US)
            format.maximumFractionDigits = 4
            format.minimumFractionDigits = 2
            holder.tvRate.text = "${format.format(item.rate)} $baseCurrency"
        }

        override fun getItemCount() = items.size
    }
}