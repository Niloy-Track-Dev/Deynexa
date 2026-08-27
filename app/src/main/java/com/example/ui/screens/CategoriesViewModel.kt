package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Category
import com.example.domain.model.Task
import com.example.domain.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryWithCount(
    val category: Category,
    val taskCount: Int
)

data class CategoriesUiState(
    val categories: List<CategoryWithCount> = emptyList(),
    val isLoading: Boolean = true
)

class CategoriesViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = combine(
        repository.getCategories(),
        repository.getTasks()
    ) { categories, tasks ->
        val list = categories.map { cat ->
            val count = tasks.count { it.categoryId == cat.id }
            CategoryWithCount(cat, count)
        }
        CategoriesUiState(categories = list, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState()
    )

    fun createCategory(name: String, icon: String, color: Int) {
        viewModelScope.launch {
            repository.saveCategory(Category(name = name, icon = icon, color = color))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.saveCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    class Factory(
        private val repository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoriesViewModel(repository) as T
        }
    }
}
