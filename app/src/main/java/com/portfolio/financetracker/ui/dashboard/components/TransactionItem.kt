package com.portfolio.financetracker.ui.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.financetracker.R
import com.portfolio.financetracker.core.util.CurrencyHelper
import com.portfolio.financetracker.core.util.LocalCurrencyCode
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private data class CategoryMeta(
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color
)

@Composable
private fun categoryMeta(key: String): CategoryMeta = when (key.lowercase()) {
    "food", "cat_food"             -> CategoryMeta(Icons.Default.Restaurant,     CatFood,       CatFoodBg)
    "transport", "cat_transport"   -> CategoryMeta(Icons.Default.DirectionsCar,  CatTransport,  CatTransportBg)
    "shopping", "cat_shopping"     -> CategoryMeta(Icons.Default.ShoppingBag,    CatShopping,   CatShoppingBg)
    "housing", "cat_housing"       -> CategoryMeta(Icons.Default.Home,           CatHousing,    CatHousingBg)
    "utilities", "cat_utilities"   -> CategoryMeta(Icons.Default.Bolt,           CatUtilities,  CatUtilitiesBg)
    "salary", "cat_salary"         -> CategoryMeta(Icons.Default.AccountBalance, CatSalary,     CatSalaryBg)
    "freelance", "cat_freelance"   -> CategoryMeta(Icons.Default.Laptop,         CatFreelance,  CatFreelanceBg)
    "investment", "cat_investment" -> CategoryMeta(Icons.Default.TrendingUp,     CatInvestment, CatInvestmentBg)
    else                           -> CategoryMeta(Icons.Default.Category,       CatOther,      CatOtherBg)
}

@Composable
fun getCategoryStringOrFallback(key: String): String = when (key.lowercase()) {
    "food", "cat_food"             -> stringResource(R.string.cat_food)
    "transport", "cat_transport"   -> stringResource(R.string.cat_transport)
    "shopping", "cat_shopping"     -> stringResource(R.string.cat_shopping)
    "housing", "cat_housing"       -> stringResource(R.string.cat_housing)
    "utilities", "cat_utilities"   -> stringResource(R.string.cat_utilities)
    "salary", "cat_salary"         -> stringResource(R.string.cat_salary)
    "freelance", "cat_freelance"   -> stringResource(R.string.cat_freelance)
    "investment", "cat_investment" -> stringResource(R.string.cat_investment)
    "other", "cat_other"           -> stringResource(R.string.cat_other)
    else                           -> key
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyCode = LocalCurrencyCode.current
    val dateFormat   = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val isIncome     = transaction.type == TransactionType.INCOME
    val amountColor  = if (isIncome) MaterialTheme.financeColors.income
                       else MaterialTheme.financeColors.expense
    val amountPrefix = if (isIncome) "+" else "-"
    val meta         = categoryMeta(transaction.category)

    // Spring press animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue    = if (isPressed) 0.97f else 1f,
        animationSpec  = spring(dampingRatio = 0.6f, stiffness = 400f),
        label          = "item_scale"
    )

    // Use theme surface color — adapts to dark/light automatically
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon — vivid color, 15% opacity background
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(meta.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = null,
                    tint = meta.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getCategoryStringOrFallback(transaction.category),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CurrencyHelper.formatAmount(transaction.amount, currencyCode)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
