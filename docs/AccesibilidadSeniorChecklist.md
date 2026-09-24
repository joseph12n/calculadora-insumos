# Checklist de Accesibilidad Sénior — Calculadora de Insumos

> ⚠️ **PENDIENTE DE RE-VERIFICACIÓN (rediseño del 2026-09-23):** este checklist se
> redactó antes del rediseño de la captura. Desde entonces: "¿Qué cuento?" y
> "¿Cuántos?" se fusionaron en una sola pantalla tipo calculadora
> (`ui/CalculatorScreen.kt`), el escaneo en vivo con CameraX se sustituyó por
> **cámara del sistema + lectura de la foto** (sin permiso de cámara) y la
> cabecera "Paso X de 3" desapareció. Las referencias de línea, los nombres de
> archivos y el conteo 27/27 están **desactualizados**; las reglas sénior que
> contiene (≥64dp, ≥20sp, colores, sin gestos) siguen siendo el estándar del
> proyecto y se aplicaron al código nuevo.

- **Fecha de revisión original:** 2026-09-23 (actualización **FASE 3**: lote, imagen de la
  cuenta, totales enteros, control de tamaño de letra A−/A+). **Re-verificación
  QA del mismo día (post H-1):** §1 re-verificada al 100 % con `grep`, H-1
  resuelto en código y añadidos los objetivos de los botones del DatePicker y
  de los círculos 📅/✕ del lote (27/27).
- **Método:** revisión ESTÁTICA del código fuente (`app/src/main/java/…`), cada
  ítem citado como `archivo:línea` (verificado con `grep -n`), más cálculo de
  contraste WCAG 2.1 sobre los valores hex literales de `ui/theme/Color.kt`.
- **Alcance:** las 7 pantallas (`StartScreen`, `PickProductScreen`,
  `EnterQuantityScreen`, `ConfirmScreen`, `SuccessScreen`, `HistoryScreen`,
  `DayDetailScreen`), `CameraScanScreen`, el **overlay "Leyendo tu imagen…"**
  (`MainActivity.kt`), los componentes (`SeniorButton`, `Keypad`, `ProductCard`,
  `MoneyText`, `StepHeader`, **`CandidatosNumeros`**) y el **control de tamaño
  de letra** (`ui/texto/AjustesTexto.kt`).
- Este documento NO es un test automatizado: es la verificación manual de la
  regla. Los tests de UI asociados viven en `app/src/androidTest/java/.../ui/`
  (FASE 3: `FlujoBasicoTest` actualizado al contrato de lotes/enteros y el nuevo
  **`LoteBasicoTest`**; 15 tests instrumentados en total).

---

## 1. Botones y objetivos táctiles ≥ 64 dp — ✅ CUMPLE (27/27; H-1 RESUELTO)

| Componente | Ubicación | Valor |
|---|---|---|
| Botón base de toda la app (`SeniorButton`) | `ui/components/SeniorButton.kt:71` | `heightIn(min = 64.dp)` |
| Teclas del teclado propio (`KeyCap`, dígitos y ⌫ Borrar) | `ui/components/Keypad.kt:164` | `heightIn(min = 64.dp)` |
| Tarjeta de producto (toda la tarjeta es clicable) | `ui/components/ProductCard.kt:56` | `heightIn(min = 80.dp)` |
| CONTINUAR LISTA (Inicio, **FASE 3**) | `ui/StartScreen.kt:119` | `heightIn(min = 72.dp)` |
| CONTAR INSUMOS (Inicio) | `ui/StartScreen.kt:131` | `heightIn(min = 88.dp)` |
| VER REGISTROS (Inicio) | `ui/StartScreen.kt:141` | `heightIn(min = 72.dp)` |
| **A−** más pequeña (**FASE 3**) | `ui/StartScreen.kt:258` | `heightIn(min = 64.dp)` |
| **A+** más grande (**FASE 3**) | `ui/StartScreen.kt:268` | `heightIn(min = 64.dp)` |
| Botón de reintento "Intentar de nuevo" | `ui/StartScreen.kt:330` | `heightIn(min = 72.dp)` |
| "📷 Escanear con la cámara" (Paso 2) | `ui/EnterQuantityScreen.kt:179` | `heightIn(min = 64.dp)` |
| "🖼 Cargar imagen de la cuenta" (Paso 2, **FASE 3**) | `ui/EnterQuantityScreen.kt:193` | `heightIn(min = 64.dp)` |
| "SEGUIR →" (Paso 2) | `ui/EnterQuantityScreen.kt:214` | `heightIn(min = 72.dp)` |
| "📅 Cambiar fecha" (Paso 3, item actual) | `ui/ConfirmScreen.kt:180-186` | 64 dp (hereda `SeniorButton`) |
| "➕ Agregar otro" (Paso 3, **FASE 3**) | `ui/ConfirmScreen.kt:193-200` | 64 dp (hereda `SeniorButton`) |
| "✔ GUARDAR TODO" (Paso 3) | `ui/ConfirmScreen.kt:258` | `heightIn(min = 80.dp)` |
| "← Corregir" (Paso 3) | `ui/ConfirmScreen.kt:264-270` | 64 dp (hereda `SeniorButton`) |
| Chips de candidato "10/25/…" (**FASE 3**) | `ui/components/CandidatosNumero.kt:112` (caja `:125`) | `heightIn(min = 72.dp)` |
| "✍️ Escribir a mano" (candidatos, **FASE 3**) | `ui/components/CandidatosNumero.kt:87` | `heightIn(min = 64.dp)` |
| "↩ DESHACER" (Éxito) | `ui/SuccessScreen.kt:129` | `heightIn(min = 72.dp)` |
| "LISTO" (Éxito) | `ui/SuccessScreen.kt:143` | `heightIn(min = 80.dp)` |
| "← Atrás" del Historial | `ui/HistoryScreen.kt:65` | `heightIn(min = 64.dp)` |
| "← Atrás" del Detalle de semana | `ui/DayDetailScreen.kt:90-93` | `heightIn(min = 64.dp)` |
| "🗑 Borrar" (fila del detalle) | `ui/DayDetailScreen.kt:238-244` | 64 dp (hereda `SeniorButton`) |
| Botones del diálogo "¿Borrar este registro?" ("Sí, borrar" y "Cancelar", `BotonDeDialogo`) | `ui/DayDetailScreen.kt:170-185` (usos), composable `:259-289` | `heightIn(min = 64.dp)` (`:270`) |
| "Volver" de la cámara (con/sin permiso) | `ui/CameraScanScreen.kt:284`, `:321` | `heightIn(min = 64.dp)` |
| **Botones circulares 📅 y ✕ de las filas del lote (H-1, RESUELTO)** | `ui/ConfirmScreen.kt:403` (`Modifier.cuadradoMinimo64()`), def. `:421-447` | `defaultMinSize(64.dp, 64.dp)` (`:423`) + cuadrado por `layout` — **VEREDICTO: CUMPLE ≥ 64 dp** |
| **Botones "Listo" y "Cancelar" del DatePicker (`BotonDeFecha`, nuevo)** | `ui/ConfirmScreen.kt:490-511` (usos), composable `:523-555` | `heightIn(min = 64.dp)` (`:534`) — sustituyen a los `TextButton` de M3 (~48 dp) |

- El "← Atrás" de los Pasos 1–3 hereda el mínimo del `SeniorButton`
  (`ui/components/StepHeader.kt:43-49` → `SeniorButton.kt:71`).
- Grep literal de `heightIn(min` sobre `app/src/main/java`: **24 usos** (todos
  ≥ 64 dp; era 23 antes de añadir `BotonDeFecha`). Con los que heredan el
  mínimo base, la tabla suma **27 objetivos y los 27 cumplen**: 26 vía
  `heightIn` y los círculos 📅/✕ vía `defaultMinSize(64.dp, 64.dp)`.
- **NUEVO — botones del DatePicker ≥ 64 dp (verificado con grep):**
  `BotonDeFecha` (`ui/ConfirmScreen.kt:523-555`) usa
  `heightIn(min = 64.dp)` (`:534`), texto 22 sp bold (`:550`) y código de color
  sénior: verde "Listo" (`:490-503`) y gris "Cancelar" (`:505-511`). Sustituye
  a los `TextButton` de Material 3 que quedaban en ~48 dp (comentario
  "hallazgo 4" en `:488`).
- **NUEVO — círculos 📅/✕ con `defaultMinSize(64)`:** `Modifier.cuadradoMinimo64()`
  (`ui/ConfirmScreen.kt:403`, def. `:421-447`) = `defaultMinSize(minWidth =
  64.dp, minHeight = 64.dp)` (`:423`) + un `layout` propio que fija el lado
  MAYOR ≥ 64 dp y la forma cuadrada. NOTA verificada: **NO** se usó
  `aspectRatio(1f)`; el KDoc `:366-378` documenta el motivo (dentro de una
  `Row`, `AspectRatioNode` de foundation-layout 1.7.6 estiraría el botón al
  ancho restante de la fila) — el requisito ≥ 64 dp y el cuadrado se cumplen
  igual. *"pendiente de confirmación"* resuelto: el cambio YA está en `src/main`.
- Los `Modifier.height(…)` fijos que existen son solo `Spacer` decorativos
  (`ui/SuccessScreen.kt:91`, `:147`; `ui/PickProductScreen.kt:67`;
  `ui/CameraScanScreen.kt:262`, `:315`) y el `CircularProgressIndicator`
  (`ui/StartScreen.kt:291-292`) — ninguno contiene texto ni acciones.

### Hallazgo H-1 — Círculos 📅/✕ de las filas del lote a 56 dp — ✅ RESUELTO

- **Dónde (histórico):** `ui/ConfirmScreen.kt`, composable `BotonCircularDeFila`
  — usaba `Modifier.size(56.dp)`; los dos usos eran la fila del lote: **📅**
  (cambiar fecha) y **✕** rojo (quitar de la lista).
- **Contexto:** diseño los justificó citando una spec de "objetivo pequeño
  ≥ 48 dp" (guía genérica de Android/Material y WCAG 2.2 ≈ 44–48 dp).
- **VEREDICTO QA (ronda FASE 3): NO CUMPLE la regla de ESTE proyecto.** El
  checklist sénior que adopta el equipo exige **≥ 64 dp para TODO botón** (§1:
  es la regla con la que se revisaron los demás objetivos de la app). 56 dp:
  1. rompe la **consistencia** (todos los demás botones son 64–88 dp);
  2. deja a los dos únicos botones de **borrar/cambiar fecha por fila** como los
     objetivos **MÁS PEQUEÑOS** de la app, justo los que una persona con temblor
     o visión reducida necesita más grandes;
  3. la spec "≥ 48 dp" es un **mínimo** genérico, no el estándar propio
     sénior adoptado (regla más estricta gana).
- **CORRECCIÓN APLICADA** (coordinador/diseño, 2026-09-23 19:08) y
  **re-verificada por QA con grep**: hoy el círculo usa
  `Modifier.cuadradoMinimo64()` sobre el CONTENIDO
  (`ui/ConfirmScreen.kt:403`), definido en `:421-447` como
  `defaultMinSize(minWidth = 64.dp, minHeight = 64.dp)` (`:423`) + un `layout`
  propio que toma el lado MAYOR entre contenido y mínimo → cuadrado de ≥ 64 dp
  que CRECE con el glifo/fuente. Sobre la Surface quedan `escalaAlTocar`
  (`:394`) y el `contentDescription` (`:395`), sin tocar la regla de color.
- **Detalle del método (verificado en el código actual):** el plan citado en su
  día era `defaultMinSize(64) + aspectRatio(1f)`, pero **NO** se usó
  `aspectRatio(1f)`: el KDoc `:366-378` explica que, dentro de una `Row` sin
  peso, el `AspectRatioNode` de foundation-layout 1.7.6 intenta `tryMaxWidth`
  antes que el contenido y convertiría el botón en un cuadrado del ancho
  COMPLETO de la fila. Se sustituyó por el `layout` cuadrado manual con el
  mismo requisito cumplido (≥ 64 dp + cuadrado) y sin ese efecto secundario.
- **VEREDICTO FINAL QA: CUMPLE ≥ 64 dp** (además: un solo toque y el rojo SOLO
  para borrar). **H-1 CERRADO**; §1 re-verificada al 100 % con `grep`:
  **27/27 objetivos cumplen**.

## 2. Texto ≥ 20 sp — ✅ CUMPLE

- La tipografía de la app fija el mínimo de 20 sp para TODOS los estilos:
  `ui/theme/Type.kt:11` (KDoc de la regla) y estilos elevados en
  `Type.kt:49-90` (`headlineSmall` 24 sp `:52`, `titleMedium` 20 sp `:58`,
  `titleSmall` 20 sp `:64`, `bodyMedium` 20 sp `:70`, `bodySmall` 20 sp `:76`,
  `labelMedium` 20 sp `:82`, `labelSmall` 20 sp `:88`).
  Los obligatorios: `headlineLarge` 40 sp (`Type.kt:20`), `headlineMedium`
  32 sp (`:26`), `titleLarge` 26 sp (`:32`), `bodyLarge` 22 sp (`:38`),
  `labelLarge` 24 sp (`:44`); los defaults de Material 3 quedan por encima
  (`Type.kt:91-92`).
- Textos con `fontSize` explícito (grep literal, NUNCA < 20 sp):
  - botones 24 sp (`SeniorButton.kt:91`), teclas 32 sp (`Keypad.kt:124`) y
    "Borrar" 22 sp (`Keypad.kt:140`),
  - cantidad gigante 72 sp (`EnterQuantityScreen.kt:118`), emoji del producto
    40 sp (`:131`), avisos "Primero escribe cuántos" y `ocrMessage`/`savingError`
    22 sp (`:201`, `:241`),
  - tarjetas de producto 26/22 sp (`ProductCard.kt:92`, `:99`; emoji 34 sp `:82`),
  - Paso 3: cálculo 40 sp (`ConfirmScreen.kt:164`), fila del lote 24 sp nombre /
    20 sp fecha (**FASE 3**, `:331`, `:337`), glifo ✕/📅 del círculo 26 sp
    (**FASE 3**, `:407`), error de guardado 22 sp (**FASE 3**, `:462`), botones
    del DatePicker 22 sp (`BotonDeFecha`, `:550`),
  - Éxito "¡Guardado!" 40 sp (`SuccessScreen.kt:102`),
  - Inicio: 📋 40 sp (`StartScreen.kt:208`), "Tienes N productos en tu lista"
    24 sp (**FASE 3**, `:210-211`), etiqueta "Tamaño de letra" 20 sp (**FASE 3**,
    `:243-244`),
  - candidato de la foto 36 sp (**FASE 3**, `CandidatosNumero.kt:129`) y
    pregunta "¿Cuál número ves en tu cuenta?" `titleLarge` 26 sp (`:69-70`),
  - Historial vacío 28 sp (`HistoryScreen.kt:85`), Detalle vacío/diálogo
    26/22 sp (`DayDetailScreen.kt:127-128`, `:155-156`, `:163-165`; fila
    26 sp nombre / 22 sp fecha `:221-222`, `:214-216`; botones del diálogo
    22 sp `:283-287`; recuadro de error 22 sp `:303-307`), "Apunta al número" 22 sp
    (`CameraScanScreen.kt:265`),
  - overlay de imagen "Leyendo tu imagen…" 30 sp (**FASE 3**,
    `MainActivity.kt:375`), "Cargando…" `titleLarge` 26 sp (`MainActivity.kt:334`,
    `StartScreen.kt:297`).

## 3. Cifras 40–80 sp — ✅ CUMPLE

| Cifra | Ubicación | Valor |
|---|---|---|
| Número gigante de cantidad (Paso 2) | `ui/EnterQuantityScreen.kt:118` | 72 sp |
| Total del ITEM ACTUAL (Paso 3, FASE 3) | `ui/ConfirmScreen.kt:172` | 56 sp |
| Total del LOTE (Paso 3, FASE 3) | `ui/ConfirmScreen.kt:245` | 56 sp |
| Total en ÉXITO (FASE 3) | `ui/SuccessScreen.kt:120` | 56 sp |
| Resumen semanal (Inicio) | `ui/StartScreen.kt:178` | 56 sp |
| Total de la semana (Detalle) | `ui/DayDetailScreen.kt:104` | 52 sp |
| Total por semana (Historial) | `ui/HistoryScreen.kt:138` | 48 sp |
| Cálculo "N cantidad producto" (Paso 3) | `ui/ConfirmScreen.kt:164` | 40 sp |
| `MoneyText` por defecto (rango documentado 40–80) | `ui/components/MoneyText.kt:20`, `:31` | 48 sp |

Todas las cifras PRINCIPALES están dentro del rango sénior 40–80 sp (KDoc en
`MoneyText.kt:14` y `EnterQuantityScreen.kt:115-116`).

**Excepciones documentadas de FILA (texto pequeño, no cifra principal):**
- total de un pendiente del lote: 26 sp SIN decimales (`ConfirmScreen.kt:342`);
- total por fila del detalle semanal: 22 sp CON decimales
  (`DayDetailScreen.kt:232-236`);
- precio unitario del producto: 22 sp CON decimales (`ProductCard.kt:99`).
La regla FASE 3 (`MoneyText.kt:23-25`) es: **decimales SOLO en texto pequeño
(20–26 sp)**; las cifras grandes usan `CurrencyFormat.formatEntero` (pesos
enteros, p. ej. 10 × 7.18 → "…72"). El número candidato de 36 sp
(`CandidatosNumero.kt:129`) es etiqueta de botón (§2), no una cifra de dinero.

## 4. Color con significado fijo (verde = avanzar, gris = volver, rojo = borrar) — ✅ CUMPLE

Regla declarada en `ui/theme/Color.kt:8-10` y `SeniorButton.kt:27-31`
(4ª variante `OUTLINE` = acción opcional neutra, `SeniorButton.kt:31`).

**Verde = avanzar / guardar** (`GreenAction`, `Color.kt:15`):
- CONTAR INSUMOS `StartScreen.kt:132`; reintento `:331`;
  **CONTINUAR LISTA `:120` (FASE 3)** — abre el Paso 3 = avanzar;
- SEGUIR → `EnterQuantityScreen.kt:215`;
- **✔ GUARDAR TODO `ConfirmScreen.kt:259`** (el único verde del Paso 3);
- LISTO `SuccessScreen.kt:144`; **"Listo" del DatePicker `ConfirmScreen.kt:492` (FASE 3/ajuste)**.

**Gris = volver / corregir / secundario** (`GreyAction`, `Color.kt:21`):
- ← Atrás de los pasos `StepHeader.kt:46`; Historial `HistoryScreen.kt:66`;
  Detalle `DayDetailScreen.kt:94`; cámara `CameraScanScreen.kt:285`, `:322`;
- ← Corregir `ConfirmScreen.kt:268`; VER REGISTROS `StartScreen.kt:142`;
  "Cancelar" del diálogo `DayDetailScreen.kt:180-185` (`BotonDeDialogo`);
  **"Cancelar" del DatePicker `ConfirmScreen.kt:505-511` (ajuste ≥64 dp)**;
- **FASE 3:** **A+ `StartScreen.kt:269`** y **✍️ Escribir a mano
  `CandidatosNumero.kt:88`** (ambos acciones secundarias/neutras, mismo
  precedente que VER REGISTROS: ni avanzan guardan ni borran);
  **📅 circular de fila `ConfirmScreen.kt:349-350`** (contenedor
  `GreyActionContainer`).

**Rojo = borrar** (`RedAction`, `Color.kt:27`) — verificación por grep:
- "↩ DESHACER" (borrar lo recién guardado) `SuccessScreen.kt:130`;
- "🗑 Borrar" (borrar fila) `DayDetailScreen.kt:241`;
- **✕ circular de la fila del lote (FASE 3) `ConfirmScreen.kt:358`**
  (`containerColor = RedAction`) — quitar de la lista = borrar. ✅ cumple;
- más allá de los botones: "⌫ Borrar" (`Keypad.kt:134`), "Sí, borrar"
  (`DayDetailScreen.kt:171-172`) y los TEXTOS de avisos de error (matiz ya
  documentado: nunca son acciones).
**Ninguna acción de avance/vuelta usa rojo.**

**OUTLINE (contorno gris, fondo transparente) = opcional neutro**
(`SeniorButton.kt:82-86`, borde `GreyAction`): 📷 cámara
(`EnterQuantityScreen.kt:180`), **🖼 imagen (`:194`, FASE 3)**, 📅 Cambiar fecha
(`ConfirmScreen.kt:184`), **➕ Agregar otro (`:197`, FASE 3)**,
**A− (`StartScreen.kt:259`, FASE 3)**.

## 5. Contraste alto (WCAG 2.1, calculado sobre los hex de Color.kt) — ✅ CUMPLE

| Par (texto sobre fondo) | Hex | Ratio | Nivel WCAG |
|---|---|---|---|
| Texto principal sobre fondo app | `#1A1A1A` / `#FFFFFF` (`Color.kt:37`, `:40`) | 17.40:1 | **AAA** |
| Blanco sobre verde de acción | `#FFFFFF` / `#2E7D32` (`Color.kt:33`, `:15`) | 5.13:1 | **AA** |
| Blanco sobre gris de acción | `#FFFFFF` / `#5F6368` (`Color.kt:33`, `:21`) | 6.05:1 | **AA** |
| Blanco sobre rojo de acción | `#FFFFFF` / `#C62828` (`Color.kt:33`, `:27`) | 5.62:1 | **AA** |
| Gris sobre fondo (precios, "N insumos") | `#5F6368` / `#FFFFFF` | 6.05:1 | **AA** |
| Rojo sobre fondo (avisos de error) | `#C62828` / `#FFFFFF` | 5.62:1 | **AA** |
| Texto sobre verde claro (tarjeta resumen) | `#1A1A1A` / `#E8F5E9` (`Color.kt:18`) | 15.47:1 | **AAA** |
| Rojo sobre rojo suave (recuadros de error) | `#C62828` / `#FDECEA` (`Color.kt:30`) | 4.92:1 | **AA** |
| Texto sobre teclas del keypad | `#1A1A1A` / `#F1F3F4` (`Color.kt:43`) | 15.64:1 | **AAA** |
| Texto sobre gris claro (superficies) | `#1A1A1A` / `#E8EAED` (`Color.kt:24`) | 14.44:1 | **AAA** |
| Gris sobre verde claro (tarjeta de producto) | `#5F6368` / `#E8F5E9` | 5.38:1 | **AA** |
| **Glifo 📅 gris sobre círculo gris (FASE 3)** | `#5F6368` / `#E8EAED` (`ConfirmScreen.kt:349-350`) | **5.02:1** | **AA** |
| **Blanco sobre círculo ✕ rojo (FASE 3)** | `#FFFFFF` / `#C62828` (`ConfirmScreen.kt:358-359`) | **5.62:1** | **AA** |
| **Nº candidato sobre chip blanco (FASE 3)** | `#1A1A1A` / `#FFFFFF` (`CandidatosNumero.kt:116-131`) | **17.40:1** | **AAA** |
| **Texto del panel de candidatos (FASE 3)** | `#1A1A1A` / `#F1F3F4` (`CandidatosNumero.kt:60-72`) | **15.64:1** | **AAA** |
| **Fecha gris de fila sobre superficie (FASE 3)** | `#5F6368` / `#F1F3F4` (`ConfirmScreen.kt:336-339`) | **5.44:1** | **AA** |
| **Overlay "Leyendo tu imagen…" (FASE 3)** | `#1A1A1A` / blanco 88 % sobre app blanca (`MainActivity.kt:351-383`) | **≈15:1** | **AAA** |

Todos los pares ≥ 4.5:1 (AA) y la mayoría AAA. Tema SIEMPRE claro y sin
dynamic color (`ui/theme/Theme.kt:11-15`), contraste predecible.

## 6. Un solo toque por acción, sin gestos, sin doble toque — ✅ CUMPLE

- **Sin gestos:** grep sobre `app/src/main/java` de
  `draggable|transformable|detectDragGestures|detectTapGestures|swipeable|
  doubleTap|DraggableState|detectTransformGestures` → **0 coincidencias**.
  **Actualización FASE 3:** `pointerInput`/`awaitPointerEvent` AHORA SÍ aparecen
  (`MainActivity.kt:35` import, `:355-359`) — **exclusivamente en el overlay
  "Leyendo tu imagen…"** para CONSUMIR los punteros e impedir toques
  accidentales mientras se lee la foto (`MainActivity.kt:351-383`); no detectan
  ningún gesto ni lanzan acciones. El resto de la app sigue sin gestos.
- **Sin doble toque (bloqueo de guardado doble):**
  - `InventoryViewModel.kt:646` — `if (estado.isSaving) return` +
    `:666` valida que solo se persista desde `Screen.CONFIRM`;
  - `InventoryViewModel.kt:507` (Atrás), `:556` (Cancelar), `:565` (Listo)
    tampoco actúan con `isSaving`;
  - guard de lote: `onAgregarOtro` SOLO desde CONFIRM (`:427`);
    `onQuitarPendiente` posición fuera de rango = no-op (`:450-451`);
    `onContinuarLote` solo desde START con lista (`:485-493`);
  - la UI deshabilita con `enabled = !isSaving`: GUARDAR TODO
    `ConfirmScreen.kt:260`, ← Corregir `:269`, ➕ Agregar otro `:198`, SEGUIR
    `EnterQuantityScreen.kt:216`, ← Atrás `EnterQuantityScreen.kt:107` y
    `ConfirmScreen.kt:135`;
  - "↩ DESHACER" cierra la ventana en el primer toque
    (`InventoryViewModel.kt:723`: `canUndo = false` inmediato) y la ventana de
    ~5 s caduca sola (`InventoryViewModel.kt:792-801`);
  - estado deshabilitado visible con opacidad 0.4 (`SeniorButton.kt:78-79`).
- **Nuevos controles FASE 3, todos de un toque simple** (ninguno usa
  `toggleable`/gesto): chips de candidato (`CandidatosNumero.kt:108-119`),
  "✍️ Escribir a mano" (`:82-90`), A−/A+ (`StartScreen.kt:253-272`),
  círculos 📅/✕ (`ConfirmScreen.kt:380-413`), CONTINUAR LISTA
  (`StartScreen.kt:114-123`). Todos usan `escalaAlTocar` (`Tacto.kt:30-46`),
  que SOLO pinta la respuesta del toque (scale 0.98), sin crear gestos.
- **Sin menús ocultos ni dobles funciones:** todo es `Button`/`Card(onClick)` a
  nivel de pantalla; la única acción con confirmación es BORRAR, con diálogo de
  un toque (`DayDetailScreen.kt:150-187`) — intencional.

## 7. Soporte de escalado de fuente al 200 % — ✅ CUMPLE

- **Alturas mínimas, no fijas:** 24 usos de `heightIn(min = …)` (grep literal;
  listados en §1) — los botones crecen con la fuente. No existe ningún
  `Modifier.height(…)` sobre texto o botón (solo `Spacer` e indicadores,
  citados en §1).
- **Scroll donde puede faltar espacio:** `verticalScroll` en Inicio
  (`StartScreen.kt:85`), Paso 1 (`PickProductScreen.kt:44`), Paso 2
  (`EnterQuantityScreen.kt:103`), Paso 3 (`ConfirmScreen.kt:130`), Éxito
  (`SuccessScreen.kt:86`); `LazyColumn` en Historial (`HistoryScreen.kt:92`) y
  Detalle (`DayDetailScreen.kt:135`). El scroll nunca sustituye a un botón.
- **Tamaños en `sp`** (escalan con el fontScale del sistema): todos los
  `fontSize` de §2–§3; grep de `fontSize … dp` → **0 coincidencias**.
- **Control de tamaño de letra (FASE 3):** `AjustesTexto.kt` multiplica el
  `fontScale` del sistema por 0.85–1.6 (paso 0.15, `:35-44`) vía
  `LocalDensity provides Density(density, fontScale × escala)`
  (`AjustesTexto.kt:131-135`): los TEXTOS crecen hasta 1.6 × 200 % del sistema
  mientras los botones conservan sus dp (`ConEscalaDeTexto` solo cambia
  `fontScale`, `AjustesTexto.kt:96-105`), persistido en `SharedPreferences`
  (`:55-61`).
- `Keypad` crece con `heightIn(min = 64.dp)` + `Box(fillMaxSize)`
  (`Keypad.kt:164-…`); los chips de candidato igual (`CandidatosNumero.kt:112`,
  `:125`).
- Marco decorativo de la cámara `MARCO_ALTO = 240.dp`
  (`CameraScanScreen.kt:67`) y el overlay de imagen (fondo semitransparente a
  pantalla completa, `MainActivity.kt:351-383`) no contienen acciones.

## 8. `contentDescription` donde aplica (TalkBack) — ✅ CUMPLE

- `SeniorButton` acepta `contentDescription` y lo aplica con `Modifier.semantics`
  (`SeniorButton.kt:58-63`); si es `null` se lee el `text`.
- ← Atrás de los pasos: "Atrás, volver al paso anterior" (`StepHeader.kt:48`);
  "Atrás, volver al inicio" (`HistoryScreen.kt:67`); "Atrás, volver al
  historial" (`DayDetailScreen.kt:89`).
- Inicio: "Contar insumos, empezar un registro nuevo" (`StartScreen.kt:133`),
  "Ver los registros de semanas anteriores" (`:143`).
  **FASE 3:** "Continuar tu lista, N productos pendientes" (`:121-122`),
  "Letra más pequeña" (`:261`), "Letra más grande" (`:271`).
- **FASE 3 — imagen/candidatos:** "Cargar una imagen de tu cuenta para leer los
  números" (`EnterQuantityScreen.kt:195`), "Elegir el número N"
  (`CandidatosNumero.kt:114`), "Escribir el número a mano con el teclado"
  (`:89`); el overlay anuncia "Leyendo tu imagen..." como `liveRegion` polite
  (`MainActivity.kt:382`).
- **FASE 3 — lote:** "Agregar otro producto a tu lista" (`ConfirmScreen.kt:199`),
  "Guardar los N productos de tu lista" (`:261`), "Cambiar la fecha del registro
  actual" (`:185`), "Cambiar la fecha de N producto" (`:348`),
  "Quitar de tu lista N producto" (`:357`).
- Éxito: "Deshacer, quitar los N productos que acabo de guardar"
  (`SuccessScreen.kt:131-132`); el ícono es `contentDescription = null`
  (`:95`).
- Detalle: "Borrar el registro de N producto" (`DayDetailScreen.kt:242-243`).
- Teclado propio: "Tecla 0"…"Tecla 9" (`Keypad.kt:119`) y "Borrar el último
  número" (`:135`).
- Los tests usan estas etiquetas como selectores (`SinTecladoSistemaTest`,
  `AtrasConservaCantidadTest`, **`LoteBasicoTest` (FASE 3)**), lo que además
  impide que se rompan sin que un test lo note.

## 9. El teclado del sistema nunca se invoca — ✅ CUMPLE

- `Keypad.kt:34`: "TECLADO NUMÉRICO PROPIO. Nunca usa BasicTextField ni el
  teclado del sistema".
- Grep de `BasicTextField|TextField|EditText|keyboardOptions|keyboardActions|
  focusRequester|LocalSoftwareKeyboard` sobre `app/src/main/java` → **la única
  línea es el propio comentario de `Keypad.kt:34`** (0 usos de código).
- La FASE 4 (foto de la cuenta) tampoco lo invoca: usa el **Photo Picker** del
  sistema (`MainActivity.kt:175-180`, sin permisos ni campo editable) y los
  candidatos se eligen con botones (`CandidatosNumero.kt`).
- Verificación automatizada en dispositivo:
  `androidTest/.../SinTecladoSistemaTest.kt` (Espresso `EditText` →
  `doesNotExist` + Compose `hasSetTextAction()` → `assertDoesNotExist`).

---

## Resumen

| # | Regla | Estado |
|---|---|---|
| 1 | Botones ≥ 64 dp | ✅ **27/27 objetivos cumplen** (§1 re-verificada con `grep`; **H-1 RESUELTO**: círculos 📅/✕ con `defaultMinSize(64)` `ConfirmScreen.kt:423` + `BotonDeFecha` del DatePicker `:534`) |
| 2 | Texto ≥ 20 sp | ✅ Cumple (tipografía + todos los fontSize explícitos, incl. FASE 3) |
| 3 | Cifras 40–80 sp | ✅ Cumple (40–72 sp; excepciones de fila 22–26 sp documentadas) |
| 4 | Verde avanzar / gris volver / rojo borrar | ✅ Cumple (matiz documentado en §4; ✕ rojo = borrar ✅) |
| 5 | Contraste alto | ✅ Cumple (mínimo 4.92:1; nuevos pares FASE 3 ≥ 5.02:1) |
| 6 | Un solo toque, sin gestos, sin doble toque | ✅ Cumple (0 gestos; `pointerInput` SOLO como bloqueo del overlay) |
| 7 | Escalado 200 % sin recortes | ✅ Cumple (`heightIn`, scroll, sp; A−/A+ hasta 1.6×) |
| 8 | `contentDescription` | ✅ Cumple (etiquetas FASE 3/4 añadidas) |
| 9 | Sin teclado del sistema | ✅ Cumple (grep + test Espresso/Compose) |

**Observación para el equipo de diseño/backend (FASE 3 + re-verificación QA):**

1. **HALLAZGO H-1 (diseño/UI): RESUELTO y CERRADO.** Los botones circulares
   **📅** y **✕** subieron de 56 dp a `defaultMinSize(64.dp, 64.dp)` + cuadrado
   por `layout` (`ui/ConfirmScreen.kt:403`, def. `:421-447`, mínimo `:423`), y
   los botones del DatePicker ("Listo"/"Cancelar") pasaron de `TextButton`
   (~48 dp) a `BotonDeFecha` con `heightIn(min = 64.dp)`
   (`ui/ConfirmScreen.kt:523-555`, `:534`). §1 re-verificada al 100 % con
   `grep`: **27/27 objetivos ≥ 64 dp**. Sin deudas abiertas en la regla 1.
2. Ningún ítem de los 9 del checklist sénior queda sin satisfacer según la
   revisión estática del código al 2026-09-23 (re-verificación post H-1;
   control: `heightIn(min` → 24 usos, gestos → 0, `fontSize … dp` → 0,
   `BasicTextField` solo en el comentario de `Keypad.kt:34`).
3. Companion tests (dispositivo REAL: Xiaomi Android 16):
   - `./gradlew :app:testDebugUnitTest` → **81 tests, 0 fallos, 7 omitidos**
     (los 7 omitidos son `InventoryDaoTest` en JVM — Room necesita
     instrumentado; sus 7 gemelos corren y PASAN en `androidTest`).
   - `./gradlew :app:connectedAndroidTest` → **15 tests: 14 PASAN, 1 FALLA**.
     La única falla NO es de accesibilidad ni del test:
     `androidTest/.../ui/AtrasConservaCantidadTest.kt:110` destapa un bug de
     navegación: desde INICIO, "➕ CONTAR INSUMOS" con `selectedProduct` +
     `quantityInput` preservados (cadena de "Atrás" hasta el inicio) cae en el
     **PASO 3 (`CONFIRM`)** porque `InventoryViewModel.onSiguiente`
     (`:379-396`) avanza a `CONFIRM` cuando ya hay producto y cantidad ≥ 1 —
     rompe el contrato documentado en `MainActivity.kt` ("con
     `selectedProduct == null` … camino al Paso 1") y el `contentDescription`
     "empezar un registro nuevo". Verificado con diagnóstico en dispositivo:
     `confirm=true paso1=false itemConfirm=true`. **REPORTADO al agente
     backend**; QA NO parchea el test ni toca `src/main`.
