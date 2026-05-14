package com.portfolio.financetracker.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  MIDNIGHT DARK — Primary palette
// ─────────────────────────────────────────────────────────────────────────────

/** Deep midnight background — the canvas for everything */
val Midnight        = Color(0xFF0F172A)
val MidnightSurface = Color(0xFF1E293B)
val MidnightCard    = Color(0xFF1E293B)
val MidnightBorder  = Color(0x1AFFFFFF)   // 10% white — glass border

/** Slate text hierarchy */
val SlateWhite      = Color(0xFFF8FAFC)
val Slate200        = Color(0xFFE2E8F0)
val Slate400        = Color(0xFF94A3B8)
val Slate600        = Color(0xFF475569)

// ─────────────────────────────────────────────────────────────────────────────
//  SEMANTIC — Finance tokens
// ─────────────────────────────────────────────────────────────────────────────

/** Emerald Green — income, positive balance, success */
val EmeraldGreen    = Color(0xFF10B981)
val EmeraldLight    = Color(0xFF34D399)
val EmeraldDark     = Color(0xFF059669)
val EmeraldBg       = Color(0x1A10B981)   // 10% opacity background

/** Electric Rose — expense, negative, danger */
val ElectricRose    = Color(0xFFF43F5E)
val RoseLight       = Color(0xFFFB7185)
val RoseDark        = Color(0xFFE11D48)
val RoseBg          = Color(0x1AF43F5E)   // 10% opacity background

// ─────────────────────────────────────────────────────────────────────────────
//  MESH GRADIENT — Hero balance card
// ─────────────────────────────────────────────────────────────────────────────

val GradientBlue    = Color(0xFF3B82F6)
val GradientPurple  = Color(0xFF8B5CF6)
val GradientTeal    = Color(0xFF06B6D4)
val GradientIndigo  = Color(0xFF6366F1)

// ─────────────────────────────────────────────────────────────────────────────
//  CATEGORY ICON COLORS — vivid, each with 15% opacity background
// ─────────────────────────────────────────────────────────────────────────────

val CatFood         = Color(0xFFF97316)   // Orange
val CatFoodBg       = Color(0x26F97316)
val CatTransport    = Color(0xFF3B82F6)   // Blue
val CatTransportBg  = Color(0x263B82F6)
val CatShopping     = Color(0xFFEC4899)   // Pink
val CatShoppingBg   = Color(0x26EC4899)
val CatHousing      = Color(0xFF8B5CF6)   // Purple
val CatHousingBg    = Color(0x268B5CF6)
val CatUtilities    = Color(0xFF06B6D4)   // Cyan
val CatUtilitiesBg  = Color(0x2606B6D4)
val CatSalary       = Color(0xFF10B981)   // Emerald
val CatSalaryBg     = Color(0x2610B981)
val CatFreelance    = Color(0xFFF59E0B)   // Amber
val CatFreelanceBg  = Color(0x26F59E0B)
val CatInvestment   = Color(0xFF6366F1)   // Indigo
val CatInvestmentBg = Color(0x266366F1)
val CatOther        = Color(0xFF94A3B8)   // Slate
val CatOtherBg      = Color(0x2694A3B8)

// ─────────────────────────────────────────────────────────────────────────────
//  LIGHT MODE — kept for completeness (app defaults to dark)
// ─────────────────────────────────────────────────────────────────────────────

val Navy900         = Color(0xFF0B1F3A)
val NavyOnLight     = Color(0xFFFFFFFF)
val Navy200         = Color(0xFFD0E4FF)
val Navy800         = Color(0xFF0D2545)
val SteelBlue       = Color(0xFF3A6EA5)
val SteelBlueOn     = Color(0xFFFFFFFF)
val SteelBlue100    = Color(0xFFDCEAF8)
val SteelBlue900    = Color(0xFF1A3F66)
val IncomeGreen     = Color(0xFF10B981)
val IncomeGreenOn   = Color(0xFFFFFFFF)
val GreenLight      = Color(0xFFD4F5E2)
val GreenDark       = Color(0xFF1A7A44)
val ExpenseRed      = Color(0xFFF43F5E)
val ExpenseRedOn    = Color(0xFFFFFFFF)
val RedLight        = Color(0xFFFFDAD6)
val RedDark         = Color(0xFF93000A)
val OffWhite        = Color(0xFFF7F9FC)
val White           = Color(0xFFFFFFFF)
val LightGrey       = Color(0xFFE8EDF2)
val MidGrey         = Color(0xFF8A9BB0)
val DarkText        = Color(0xFF0B1F3A)

// Dark aliases (used by Theme.kt)
val DarkNavy        = Color(0xFF1E2A38)
val DarkNavyOn      = Color(0xFFD0E4FF)
val DarkNavyCont    = Color(0xFF0D2545)
val DarkNavyContOn  = Color(0xFFD0E4FF)
val DarkSteel       = Color(0xFF4F6D8A)
val DarkSteelOn     = Color(0xFFFFFFFF)
val DarkSteelCont   = Color(0xFF1A3F66)
val DarkSteelContOn = Color(0xFFDCEAF8)
val DarkGreen       = Color(0xFF10B981)
val DarkGreenOn     = Color(0xFFFFFFFF)
val DarkGreenCont   = Color(0xFF0A3D22)
val DarkGreenContOn = Color(0xFFB7F5D0)
val DarkRed         = Color(0xFFF43F5E)
val DarkRedOn       = Color(0xFF690005)
val DarkRedCont     = Color(0xFF93000A)
val DarkRedContOn   = Color(0xFFFFDAD6)
val DarkBg          = Midnight
val DarkSurface     = MidnightSurface
val DarkSurfaceVar  = Color(0xFF1E2A38)
val DarkOnBg        = SlateWhite
val DarkOnSurface   = Slate200
val DarkOutline     = Color(0xFF334155)
