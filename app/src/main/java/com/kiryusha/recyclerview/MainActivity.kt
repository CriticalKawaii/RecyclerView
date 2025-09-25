package com.kiryusha.recyclerview

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShoppingAdapter
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var sharedPreferences: SharedPreferences
    private val shoppingItems = mutableListOf<ShoppingItem>()
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "shopping_list_prefs"
        private const val ITEMS_KEY = "shopping_items"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupFab()
        loadItems()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        fabAdd = findViewById(R.id.fabAdd)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun setupRecyclerView() {
        adapter = ShoppingAdapter(
            items = shoppingItems,
            onEditClick = { position -> showEditDialog(position) },
            onDeleteClick = { position -> deleteItem(position) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                showDeleteConfirmation(position)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun setupFab() {
        fabAdd.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val editName = dialogView.findViewById<EditText>(R.id.editTextName)
        val editQuantity = dialogView.findViewById<EditText>(R.id.editTextQuantity)

        AlertDialog.Builder(this)
            .setTitle("Добавить товар")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = editName.text.toString().trim()
                val quantityStr = editQuantity.text.toString().trim()

                if (name.isNotEmpty() && quantityStr.isNotEmpty()) {
                    try {
                        val quantity = quantityStr.toInt()
                        if (quantity > 0) {
                            addItem(ShoppingItem(name, quantity))
                        } else {
                            Toast.makeText(this, "Количество должно быть больше 0", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Введите корректное количество", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditDialog(position: Int) {
        val item = shoppingItems[position]
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val editName = dialogView.findViewById<EditText>(R.id.editTextName)
        val editQuantity = dialogView.findViewById<EditText>(R.id.editTextQuantity)

        editName.setText(item.name)
        editQuantity.setText(item.quantity.toString())

        AlertDialog.Builder(this)
            .setTitle("Редактировать товар")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = editName.text.toString().trim()
                val quantityStr = editQuantity.text.toString().trim()

                if (name.isNotEmpty() && quantityStr.isNotEmpty()) {
                    try {
                        val quantity = quantityStr.toInt()
                        if (quantity > 0) {
                            editItem(position, ShoppingItem(name, quantity))
                        } else {
                            Toast.makeText(this, "Количество должно быть больше 0", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Введите корректное количество", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteConfirmation(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить товар")
            .setMessage("Вы уверены, что хотите удалить этот товар?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteItem(position)
            }
            .setNegativeButton("Отмена") { _, _ ->
                adapter.notifyItemChanged(position)
            }
            .show()
    }

    private fun addItem(item: ShoppingItem) {
        shoppingItems.add(item)
        adapter.notifyItemInserted(shoppingItems.size - 1)
        saveItems()
        Toast.makeText(this, "Товар добавлен", Toast.LENGTH_SHORT).show()
    }

    private fun editItem(position: Int, newItem: ShoppingItem) {
        shoppingItems[position] = newItem
        adapter.notifyItemChanged(position)
        saveItems()
        Toast.makeText(this, "Товар обновлен", Toast.LENGTH_SHORT).show()
    }

    private fun deleteItem(position: Int) {
        val itemName = shoppingItems[position].name
        shoppingItems.removeAt(position)
        adapter.notifyItemRemoved(position)
        saveItems()
        Toast.makeText(this, "$itemName удален", Toast.LENGTH_SHORT).show()
    }

    private fun saveItems() {
        val json = gson.toJson(shoppingItems)
        sharedPreferences.edit()
            .putString(ITEMS_KEY, json)
            .apply()
    }

    private fun loadItems() {
        val json = sharedPreferences.getString(ITEMS_KEY, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<ShoppingItem>>() {}.type
            val loadedItems = gson.fromJson<MutableList<ShoppingItem>>(json, type)
            shoppingItems.clear()
            shoppingItems.addAll(loadedItems)
            adapter.notifyDataSetChanged()
        }
    }
}