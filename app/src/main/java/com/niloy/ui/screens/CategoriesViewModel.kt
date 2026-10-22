package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import com.niloy.domain.repository.TaskRepository
import com.niloy.ui.util.SideEffectEffectHandler
import com.niloy.ui.util.UiIntent
import com.niloy.ui.util.UiSideEffect
import com.niloy.ui.util.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryWithCount(
    val category: Category,
    val taskCount: Int
)

data class CategoriesUiState(
    val categories: List<CategoryWithCount> = emptyList(),
    val isLoading: Boolean = true
) : UiState

sealed interface CategoriesIntent : UiIntent {
    data class CreateCategory(val name: String, val icon: String, val color: Int) : CategoriesIntent
    data class UpdateCategory(val category: Category) : CategoriesIntent
    data class DeleteCategory(val category: Category) : CategoriesIntent
}

sealed interface CategoriesSideEffect : UiSideEffect {
    data class ShowToast(val message: String) : CategoriesSideEffect
}

class CategoriesViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val sideEffectHandler = SideEffectEffectHandler<CategoriesSideEffect>()
    val sideEffect: Flow<CategoriesSideEffect> = sideEffectHandler.effectFlow

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

    fun onIntent(intent: CategoriesIntent) {
        when (intent) {
            is CategoriesIntent.CreateCategory -> createCategory(intent.name, intent.icon, intent.color)
            is CategoriesIntent.UpdateCategory -> updateCategory(intent.category)
            is CategoriesIntent.DeleteCategory -> deleteCategory(intent.category)
        }
    }

    fun createCategory(name: String, icon: String, color: Int) {
        viewModelScope.launch {
            repository.saveCategory(Category(name = name, icon = icon, color = color))
            sideEffectHandler.trySendEffect(CategoriesSideEffect.ShowToast("Category created"))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.saveCategory(category)
            sideEffectHandler.trySendEffect(CategoriesSideEffect.ShowToast("Category updated"))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            sideEffectHandler.trySendEffect(CategoriesSideEffect.ShowToast("Category deleted"))
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
