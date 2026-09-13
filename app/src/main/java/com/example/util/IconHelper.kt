package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object IconHelper {
    val availableIcons = listOf(
        "restaurant" to Icons.Default.Restaurant,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "directions_bus" to Icons.Default.DirectionsBus,
        "directions_car" to Icons.Default.DirectionsCar,
        "local_gas_station" to Icons.Default.LocalGasStation,
        "home" to Icons.Default.Home,
        "bolt" to Icons.Default.Bolt,
        "water_drop" to Icons.Default.WaterDrop,
        "wifi" to Icons.Default.Wifi,
        "smartphone" to Icons.Default.Smartphone,
        "account_balance" to Icons.Default.AccountBalance,
        "account_balance_wallet" to Icons.Default.AccountBalanceWallet,
        "verified_user" to Icons.Default.VerifiedUser,
        "favorite" to Icons.Default.Favorite,
        "medication" to Icons.Default.Medication,
        "school" to Icons.Default.School,
        "movie" to Icons.Default.Movie,
        "flight" to Icons.Default.Flight,
        "subscriptions" to Icons.Default.Subscriptions,
        "spa" to Icons.Default.Spa,
        "more_horiz" to Icons.Default.MoreHoriz,
        "payments" to Icons.Default.Payments,
        "storefront" to Icons.Default.Storefront,
        "laptop" to Icons.Default.Laptop,
        "trending_up" to Icons.Default.TrendingUp,
        "card_giftcard" to Icons.Default.CardGiftcard,
        "attach_money" to Icons.Default.AttachMoney,
        "receipt" to Icons.Default.Receipt,
        "fitness_center" to Icons.Default.FitnessCenter,
        "coffee" to Icons.Default.Coffee,
        "work" to Icons.Default.Work,
        "credit_card" to Icons.Default.CreditCard
    )

    private val iconMap = availableIcons.toMap()

    fun getIcon(name: String): ImageVector {
        return iconMap[name] ?: Icons.Default.Receipt
    }
}
