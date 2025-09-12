package com.eresto.captain.ui.tables

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eresto.captain.model.GetTables
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TabViewModel(
    private val repository: TabRepository,
    private val dbHelper: DBHelper
) : ViewModel() {

    private val _tableList = MutableLiveData<List<GetTables>>()
    val tableList: LiveData<List<GetTables>> = _tableList

    /**
     * Tells the repository to send a single network request for fresh table data.
     */
    fun requestFreshTableData(pref: Preferences) {
        repository.requestTableUpdate(pref)
    }

    /**
     * This function is called after a network response is received and the
     * local database has been updated. It re-queries the database on a background
     * thread and posts the result to the LiveData for the UI to observe.
     * We assume type=1 is for Dine-In tables.
     */
    fun refreshTablesFromDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val tables = dbHelper.GetTables(1) // Assuming type 1 for Dine-In
            _tableList.postValue(tables)
        }
    }
}

/**
 * ViewModel Factory is required for ViewModels that have constructor arguments.
 * It tells the system how to create an instance of our TabViewModel.
 */
