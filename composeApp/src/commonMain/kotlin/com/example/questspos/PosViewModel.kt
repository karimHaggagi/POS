package com.example.questspos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.PosRepository
import com.example.model.PosCheckoutResult
import com.example.model.PosTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosViewModel(
    private val repo: PosRepository,
) : ViewModel() {

    private val discount = MutableStateFlow(0.0)
    val discountPercent = discount.asStateFlow()

    val catalog = repo.observeCatalog()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val cart = repo.observeCart()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pastOrders = repo.observePastOrders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totals: StateFlow<PosTotals> = discount
        .flatMapLatest { repo.observeTotals(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, PosTotals(0.0, 0.0, 0.0, 0.0))

    private val _lastCheckout = MutableStateFlow<PosCheckoutResult?>(null)
    val lastCheckout = _lastCheckout.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    fun setDiscountPercent(value: Double) {
        discount.value = value.coerceIn(0.0, 50.0)
    }

    fun addProduct(id: String) = viewModelScope.launch {
        repo.addProductToCart(id, 1)
    }

    fun increment(lineId: String) = viewModelScope.launch {
        repo.incrementQuantity(lineId)
    }

    fun decrement(lineId: String) = viewModelScope.launch {
        repo.decrementQuantity(lineId)
    }

    fun remove(lineId: String) = viewModelScope.launch {
        repo.removeCartLine(lineId)
    }

    fun checkout() = viewModelScope.launch {
        val result = repo.completeSale(discount.value)
        _lastCheckout.value = result.getOrNull()
        if (result.isFailure) {
            _syncMessage.value = result.exceptionOrNull()?.message
        }
    }

    fun syncNow() = viewModelScope.launch {
        val summary = repo.processSyncOutbox()
        _syncMessage.value = summary.detail
    }

    fun applyDemoBarcode(sku: String) = viewModelScope.launch {
        val ok = repo.applyBarcode(sku)
        if (!ok) {
            _syncMessage.value = "Unknown SKU: $sku"
        }
    }
}
