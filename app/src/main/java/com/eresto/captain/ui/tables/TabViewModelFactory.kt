package com.eresto.captain.ui.tables


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.eresto.captain.utils.DBHelper

class TabViewModelFactory(
    private val repository: TabRepository,
    private val dbHelper: DBHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TabViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TabViewModel(repository, dbHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
