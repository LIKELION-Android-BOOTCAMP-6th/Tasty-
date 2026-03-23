package com.tasty.android.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.tasty.android.R

// 폰트 프로바이더 정의
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage =  "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Roboto 폰트
val fontName = GoogleFont("Roboto")

// 폰트 패밀리 정의
val fontFamily = FontFamily(
    Font(googleFont = fontName, fontProvider = provider, style = FontStyle.Normal, weight = FontWeight.Light),
    Font(googleFont = fontName, fontProvider = provider, style = FontStyle.Normal, weight = FontWeight.Normal),
    Font(googleFont = fontName, fontProvider = provider, style = FontStyle.Normal, weight = FontWeight.SemiBold),
    Font(googleFont = fontName, fontProvider = provider, style = FontStyle.Normal, weight = FontWeight.Bold),
)

// 타이포그래피 정의
val Typography = Typography(

    // Hero
    displayMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 45.sp,
        letterSpacing = 0.4.sp
    ),
    // Header
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.5.sp
    ),
    //Title
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.5.sp
    ),
    // Body
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Label Medium
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.5.sp
    ),
    // Label Small
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    )
)