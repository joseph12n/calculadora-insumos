package com.bioplast.insumos.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de alto contraste con fondo claro.
 * Reglas sénior:
 *  - Verde  = avanzar / guardar
 *  - Gris   = volver / corregir
 *  - Rojo   = borrar (únicamente)
 */

// --- Acciones -------------------------------------------------------------
/** Verde de acción: guardar / avanzar. */
val GreenAction: Color = Color(0xFF2E7D32)

/** Contenedor verde claro (superficies de avance, éxito). */
val GreenActionContainer: Color = Color(0xFFE8F5E9)

/** Gris de acción: volver / corregir. */
val GreyAction: Color = Color(0xFF5F6368)

/** Contenedor gris claro (botones secundarios, superficies). */
val GreyActionContainer: Color = Color(0xFFE8EAED)

/** Rojo de acción: borrar (nunca para otra cosa). */
val RedAction: Color = Color(0xFFC62828)

/** Contenedor rojo suave (tecla Borrar, avisos de borrado). */
val RedActionContainer: Color = Color(0xFFFDECEA)

/** Texto/iconos sobre fondos de acción (verde, gris, rojo). */
val OnAction: Color = Color(0xFFFFFFFF)

// --- Neutros --------------------------------------------------------------
/** Texto principal, casi negro. */
val TextPrimary: Color = Color(0xFF1A1A1A)

/** Fondo de la app: blanco puro. */
val AppBackground: Color = Color(0xFFFFFFFF)

/** Superficie suave (teclas del teclado, tarjetas neutras). */
val SurfaceSoft: Color = Color(0xFFF1F3F4)

/** Acento para tarjetas de producto. */
val ProductAccent: Color = GreenActionContainer

/** Contorno suave para variantes OUTLINE. */
val OutlineSoft: Color = Color(0xFFDADCE0)
