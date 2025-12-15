package com.organizadorfinancas.ui.import

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizadorfinancas.domain.model.Category
import com.organizadorfinancas.domain.model.StatementItem
import com.organizadorfinancas.domain.repository.CategoryRepository
import com.organizadorfinancas.domain.usecase.ImportStatementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportStatementUiState(
    val isLoading: Boolean = false,
    val items: List<StatementItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isImportComplete: Boolean = false
)

@HiltViewModel
class ImportStatementViewModel @Inject constructor(
    private val importStatementUseCase: ImportStatementUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportStatementUiState())
    val uiState: StateFlow<ImportStatementUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    fun parseFile(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val result = importStatementUseCase.parseFile(inputStream, fileName)

                    result.fold(
                        onSuccess = { items ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                items = items
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to parse file"
                            )
                        }
                    )
                    inputStream.close()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Unable to open file"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun toggleItemSelection(index: Int) {
        val currentItems = _uiState.value.items.toMutableList()
        if (index in currentItems.indices) {
            currentItems[index] = currentItems[index].copy(
                isSelected = !currentItems[index].isSelected
            )
            _uiState.value = _uiState.value.copy(items = currentItems)
        }
    }

    fun selectAllItems() {
        val updatedItems = _uiState.value.items.map { it.copy(isSelected = true) }
        _uiState.value = _uiState.value.copy(items = updatedItems)
    }

    fun deselectAllItems() {
        val updatedItems = _uiState.value.items.map { it.copy(isSelected = false) }
        _uiState.value = _uiState.value.copy(items = updatedItems)
    }

    fun setCategory(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun importSelectedItems(creditCardId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = importStatementUseCase.importItems(
                creditCardId = creditCardId,
                items = _uiState.value.items,
                categoryId = _uiState.value.selectedCategoryId
            )

            result.fold(
                onSuccess = { count ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "$count items imported successfully",
                        isImportComplete = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to import items"
                    )
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

