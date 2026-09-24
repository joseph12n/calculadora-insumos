# 📱 Calculadora de Insumos

Aplicación Android **100% local** para el conteo y cálculo de costos de insumos de
laboratorio, diseñada específicamente para **personas mayores**: flujo corto, botones
grandes, alto contraste y lenguaje cotidiano.

## Características

- **Captura en UNA pantalla tipo calculadora**: Inicio → Calculadora → Revisar → ¡Guardado!.
  En la calculadora se elige el insumo (diálogo "¿Qué vas a contar?"), se escribe la
  cantidad con el **teclado propio** (nunca el del sistema) y se ve el **total en vivo**
  en el visor ("10 × $7,18 = $72"). El botón **SEGUIR →** está siempre visible: no hay
  que desplazar la pantalla para avanzar.
- **Leer el número de una foto**: un solo botón **📷 FOTO** abre la cámara del sistema
  (**sin pedir permiso de cámara**). La lectura es **recursiva** (foto original → foto en
  blanco y negro con umbral adaptativo → recorte ampliado de cada número) y ML Kit corre
  on-device. Como la letra manuscrita confunde 1/7/4/8, **siempre se confirma**: el
  diálogo muestra el **recorte de tu propia letra** junto al número ("¿Es este el número
  de tu hoja?" / "¿Cuál número ves en tu hoja?"). Si no se lee nada, se avisa sin borrar
  lo escrito.
- **Lote de productos**: acumular varios insumos en una sesión ("➕ Agregar otro") y
  guardar todo junto (transacción atómica).
- **Memoria semanal**: resumen de la semana actual, historial por semanas y desglose
  por semana (Room + `WeekFields` ISO).
- **Pensada para personas mayores**: botones ≥64dp, texto ≥20sp, contraste AA/AAA,
  tamaño de fuente ajustable (A−/A+), sin gestos, sin sonidos, verde=avanzar /
  gris=volver / rojo=borrar.
- **Sin internet**: cero llamadas de red, cero permiso de red; los datos viven solo en
  el dispositivo.

## Stack

Kotlin 2.1 · Jetpack Compose + Material 3 · MVVM + UDF (StateFlow) · Room 2.6 (KSP,
esquema exportado) · Google ML Kit Text Recognition (on-device) para leer la foto ·
FileProvider para la foto temporal · Coroutines/Flow · minSdk 26 / target 35.

> La captura usa la **cámara del sistema** (`ActivityResultContracts.TakePicture`): no
> hay CameraX ni permiso `CAMERA` en la app.

## Compilar

```bash
./gradlew testDebugUnitTest     # 75 tests unitarios
./gradlew assembleDebug         # APK debug → app/build/outputs/apk/debug/
./gradlew assembleRelease       # APK release firmada → app/build/outputs/apk/release/
./gradlew connectedAndroidTest  # 21 tests instrumentados (requiere celular/emulador)
```

Requisitos: JDK 17+ y el SDK de Android (platform 35, build-tools 35.0.0).

## Distribución

- **Descargar desde GitHub (lo más fácil para otra persona)**: cada tag `v*` publica la
  APK release como asset del release:
  `https://github.com/joseph12n/calculadora-insumos/releases/latest`
  → abrir en el celular y tocar `app-release.apk` → habilitar "instalar apps de orígenes
  desconocidos" si lo pide.
- **Instalación directa**: comparte el `.apk` por WhatsApp/Drive/correo y se instala igual.
- **GitHub Actions** (`.github/workflows/android-ci.yml`):
  - Cada *push* a `main` → corre los unit tests, compila la APK debug y la sube como **artifact**.
  - Cada *tag* `v*` (ej. `git tag v1.1.0 && git push origin v1.1.0`) → además compila la **release** y la publica como asset del **GitHub Release** generado.
  - Firma de release: configura los secretos `KEYSTORE_B64` (keystore en base64), `KEYSTORE_PASSWORD` y `KEY_PASSWORD`. Sin ellos, la release se firma con la clave de debug (sigue siendo instalable manualmente).

## Estructura

```
app/src/main/java/com/bioplast/insumos/
├── model/          # ProductType (precios COP), InventoryRecord, CurrencyFormat, Screen
├── data/           # Room: DAO, AppDatabase (v1, esquema exportado), Repository
├── camera/         # OCR de la foto (ML Kit) + foto temporal (FileProvider)
├── ui/             # theme + components + pantallas (Inicio, Calculadora, Revisar,
│                   # Éxito, Historial, Detalle) + ajustes de texto
├── InventoryViewModel.kt   # Estado único (InventoryUiState) que gobierna la navegación
└── MainActivity.kt         # when(screen) como única fuente de verdad + flujo de la foto
```

Los agentes de desarrollo (arquitecto, backend, base de datos, diseño, QA) están definidos
localmente en `.opencode/agents/` (carpeta ignorada por git: configuración privada).

> Nota: `docs/AccesibilidadSeniorChecklist.md` se redactó **antes** del rediseño de la
> captura (calculadora de una pantalla + foto con cámara del sistema); sus referencias de
> línea y nombres de pantalla están pendientes de re-verificación.
