package com.example.inventory.ui.item

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.ItemsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel to retrieve, update, and delete an item from the [ItemsRepository]'s data source.
 */
class ItemDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val itemsRepository: ItemsRepository,
) : ViewModel() {

    private val itemId: Int = savedStateHandle[ItemDetailsDestination.itemIdArg] ?: error("Item ID is missing")

    /**
     * Holds the item details UI state. The data is retrieved from [ItemsRepository] and mapped to
     * the UI state.
     */
    val uiState: StateFlow<ItemDetailsUiState> =
        itemsRepository.getItemsStream(itemId)
            .filterNotNull()
            .map { item ->
                ItemDetailsUiState(
                    outOfStock = item.quantity <= 0,
                    itemDetails = item.toItemDetails()
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = ItemDetailsUiState()
            )

    /**
     * Reduces the item quantity by one and updates the [ItemsRepository]'s data source.
     */
    fun reduceQuantityByOne() {
        viewModelScope.launch {
            uiState.value.itemDetails.toItem().let { currentItem ->
                if (currentItem.quantity > 0) {
                    itemsRepository.updateItem(currentItem.copy(quantity = currentItem.quantity - 1))
                }
            }
        }
    }

    /**
     * Deletes the item from the [ItemsRepository]'s data source.
     */
    suspend fun deleteItem() {
        itemsRepository.deleteItem(uiState.value.itemDetails.toItem())
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * UI state for ItemDetailsScreen.
 */
data class ItemDetailsUiState(
    val outOfStock: Boolean = true,
    val itemDetails: ItemDetails = ItemDetails()
)
