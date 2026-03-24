package com.tasty.android.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.tasty.android.R


// Declare Google Font Provider
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Declare fontName: Roboto
val fontName = GoogleFont("Roboto")

// Declare fontFamily
val fontFamily = FontFamily(
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Light), // W300
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Normal), // W400
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Medium), // W500
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.SemiBold), // W600
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Bold), // W700
)