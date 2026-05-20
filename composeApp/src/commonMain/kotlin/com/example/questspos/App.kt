package com.example.questspos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.model.PosTotals
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    MaterialTheme {
        val vm: PosViewModel = koinViewModel()
        val catalog by vm.catalog.collectAsState()
        val cart by vm.cart.collectAsState()
        val totals by vm.totals.collectAsState()
        val past by vm.pastOrders.collectAsState()
        val discount by vm.discountPercent.collectAsState()
        val last by vm.lastCheckout.collectAsState()
        val banner by vm.syncMessage.collectAsState()
        var discountInput by remember { mutableStateOf(discount.toString()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Sahm Food — Mini POS (offline)", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Catalog, cart, tax, discount, SQLite persistence, sync outbox, mock payment + receipt.",
                style = MaterialTheme.typography.bodyMedium,
            )

            banner?.let {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(it, modifier = Modifier.padding(12.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it },
                    label = { Text("Discount %") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val parsed = discountInput.toDoubleOrNull() ?: 0.0
                        vm.setDiscountPercent(parsed)
                    },
                ) {
                    Text("Apply")
                }
            }

            TotalsRow(totals)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.checkout() }) { Text("Pay & save") }
                OutlinedButton(onClick = { vm.syncNow() }) { Text("Sync pending") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { vm.applyDemoBarcode("cf001") }, label = { Text("Scan cf001") })
                AssistChip(onClick = { vm.applyDemoBarcode("cf003") }, label = { Text("Scan cf003") })
            }

            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Menu", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(catalog, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.addProduct(item.id) },
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                                    Text("${formatMoney(item.price)} · ${item.sku}")
                                }
                            }
                        }
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text("Cart", style = MaterialTheme.typography.titleMedium)
                    if (cart.isEmpty()) {
                        Text("Tap a menu item to add.")
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cart, key = { it.lineId }) { line ->
                            Card {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(line.title, style = MaterialTheme.typography.titleSmall)
                                    Text("Qty ${line.quantity} · ${formatMoney(line.unitPrice)} each")
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { vm.decrement(line.lineId) }) { Text("-") }
                                        OutlinedButton(onClick = { vm.increment(line.lineId) }) { Text("+") }
                                        OutlinedButton(onClick = { vm.remove(line.lineId) }) { Text("Remove") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Recent orders (local)", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.height(160.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(past, key = { it.id }) { order ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(order.id.take(10) + "…")
                        Text(formatMoney(order.total))
                        Text(if (order.synced) "Synced" else "Pending")
                    }
                }
            }

            last?.let { result ->
                HorizontalDivider()
                Text("Last receipt", style = MaterialTheme.typography.titleMedium)
                Text(result.receiptText, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TotalsRow(totals: PosTotals) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Totals", style = MaterialTheme.typography.titleMedium)
            Text("Subtotal: ${formatMoney(totals.subtotal)}")
            Text("Discount: ${formatMoney(totals.discount)}")
            Text("Tax (8.25%): ${formatMoney(totals.tax)}")
            Text("Total: ${formatMoney(totals.total)}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatMoney(value: Double): String {
    val cents = (value * 100).toInt().coerceAtLeast(0)
    val dollars = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "$" + dollars + "." + fraction
}
