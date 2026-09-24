# 📱 Calculadora de Insumos

Aplicación Android **100% local** para el conteo y cálculo de costos de insumos de
laboratorio, diseñada específicamente para **personas mayores**: flujo guiado paso a paso,
botones grandes, alto contraste y lenguaje cotidiano.

## Características

- **Flujo de 7 pantallas**: Inicio → ¿Qué cuento? → ¿Cuántos? → Revisar → ¡Guardado! → Historial → Detalle.
- **Lote de productos**: acumular varios insumos en una sesión ("➕ Agregar otro") y guardar todo junto (transacción atómica).
- **Captura flexible de cantidades**: teclado numérico propio (nunca el del sistema), OCR opcional con cámara en vivo y **carga de imagen de la cuenta** con validación automática (ML Kit, excluye precios y fechas).
- **Memoria semanal**: resumen de la semana actual, historial por semanas y desglose por día (Room + `WeekFields` ISO).
- **Pensada para personas mayores**: botones ≥64dp, texto ≥20sp, contraste AA/AAA, tamaño de fuente ajustable (A−/A+), sin gestos, sin sonidos, verde=avanzar / gris=volver / rojo=borrar.
- **Sin internet**: cero llamadas de red, cero permiso de red; los datos viven solo en el dispositivo.

## Stack

Kotlin 2.1 · Jetpack Compose + Material 3 · MVVM + UDF (StateFlow) · Room 2.6 (KSP, esquema exportado) · CameraX + Google ML Kit Text Recognition · Coroutines/Flow · minSdk 26 / target 35.

## Compilar

```bash
./gradlew testDebugUnitTest     # 81 tests unitarios
./gradlew assembleDebug         # APK debug → app/build/outputs/apk/debug/
./gradlew assembleRelease       # APK release firmada → app/build/outputs/apk/release/
./gradlew connectedAndroidTest  # 15 tests instrumentados (requiere celular/emulador)
```

Requisitos: JDK 17+ y el SDK de Android (platform 35, build-tools 35.0.0).

## Distribución

- **Instalación directa**: comparte el `.apk` y habilita "instalar apps de orígenes desconocidos".
- **GitHub Actions** (`.github/workflows/android-ci.yml`):
  - Cada *push* a `main` → corre los unit tests, compilar la APK debug y la sube como **artifact**.
  - Cada *tag* `v*` (ej. `git tag v1.0.0 && git push --tags`) → además compila la **release** y la publica como asset del **GitHub Release** generado.
  - Firma de release: configura los secretos `KEYSTORE_B64` (keystore en base64), `KEYSTORE_PASSWORD` y `KEY_PASSWORD`. Sin ellos, la release se firma con la clave de debug (sigue siendo instalable manualmente).

## Estructura

```
app/src/main/java/com/bioplast/insumos/
├── model/          # ProductType (precios COP), InventoryRecord, CurrencyFormat, Screen
├── data/           # Room: DAO, AppDatabase (v1, esquema exportado), Repository
├── camera/         # OCR: analyzer en vivo, OCR de imagen, permisos
├── ui/             # theme + components + las 7 pantallas + ajustes de texto
├── InventoryViewModel.kt   # Estado único (InventoryUiState) que gobierna la navegación
└── MainActivity.kt         # when(screen) como única fuente de verdad
```

Los agentes de desarrollo (arquitecto, backend, base de datos, diseño, QA) están definidos
localmente en `.opencode/agents/` (carpeta ignorada por git: configuración privada).
