package com.eresto.captain.ui.tables


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.eresto.captain.model.GetTables
import com.eresto.captain.utils.DBHelper
import com.eresto.captain.utils.Preferences

class TabViewModel(private val repository: TabRepository, private val dbHelper: DBHelper) :
    ViewModel() {

    private val _tableList = MutableLiveData<List<GetTables>>()
    val tableList: LiveData<List<GetTables>> = _tableList

    fun fetchTables(pref: Preferences) {
        repository.startFetchingTables(pref) { json ->
            _tableList.postValue(dbHelper.GetTables(1)) // Update LiveData
        }
    }

    fun stopFetchingTables() {
        repository.stopFetchingTables() // Stop polling
    }
}
