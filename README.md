# Documento de Definición de Proyecto: QuantTrack (Fase 1)

## 1. Objetivo del Sistema
Desarrollar una aplicación nativa en Android (Java) orientada al análisis cuantitativo de carteras de inversión. El sistema descargará datos históricos de mercado (series temporales OHLCV), calculará el Valor Liquidativo (NAV) mediante el balance de transacciones, y aplicará algoritmos matemáticos de filtrado (medias móviles, momentum) para extraer señales de tendencia.

Para garantizar la mantenibilidad y el trabajo en paralelo, el proyecto se construirá bajo una estricta arquitectura modular (basada en los principios de separación de responsabilidades y MVVM), desacoplando completamente las matemáticas puras del framework de Android.

## 2. Desglose Modular y Asignación de Roles

El sistema se divide en cuatro módulos independientes. Cada módulo define un "contrato" (entradas y salidas esperadas), lo que permite que el equipo trabaje en paralelo.

### Módulo A: Capa de Ingesta de Datos (Data Layer)
* **Responsabilidad:** Conexión a la red, consumo de la API REST financiera y deserialización de JSON a objetos inmutables en Java.
* **Entrada:** Petición de un ticker (ej. "AAPL") y un intervalo de tiempo.
* **Salida:** Estructura de datos temporal (OHLCV).
* **Herramientas tecnológicas:** Librería Retrofit2 para peticiones asíncronas y Gson para el mapeo.

### Módulo B: Motor Cuantitativo (Domain Layer - Math Engine)
* **Responsabilidad:** Procesamiento algorítmico de las series temporales. Este módulo será Java puro, permitiendo su testeo unitario absoluto.
* **Entrada:** Arrays de primitivos (double[]) con los precios de cierre y los parámetros del modelo.
* **Salida:** Arrays de primitivos (double[]) con los datos suavizados o evaluados.
* **Requisitos técnicos:** * Implementar la Media Móvil Simple (SMA) utilizando un algoritmo de ventana deslizante para garantizar una complejidad O(N).
    * Implementar Osciladores de Momentum.

### Módulo C: Gestión de Cartera (Domain Layer - Portfolio)
* **Responsabilidad:** Mantener el estado financiero del usuario aplicando diseño orientado a objetos.
* **Estructura:**
    * Transaction: Registro inmutable.
    * Position: Agregación de transacciones.
    * Portfolio: Contenedor principal.
* **Lógica matemática:** Cálculo iterativo del NAV mediante el producto escalar entre el vector de posiciones activas y el vector de precios de mercado actuales.

### Módulo D: Capa de Presentación (UI Layer)
* **Responsabilidad:** La interfaz gráfica (Actividades y Fragmentos en Android). Se alimenta exclusivamente de los datos ya procesados.
* **Entrada:** Vectores de resultados (double[]) y el valor escalar del NAV total.
* **Salida:** Pantallas interactivas renderizadas.
* **Requisitos técnicos:** * Implementación de la librería MPAndroidChart.
    * Renderizado de un Candlestick Chart para el precio.
    * Superposición de un gráfico de líneas para la SMA/EMA.
    * Gráfico circular que represente el peso porcentual de la cartera.

## 3. Infraestructura y Control de Versiones (Reglas de Git)

Para evitar conflictos críticos, el equipo operará bajo el modelo Feature Branch Workflow:

1. **Configuración Inicial:** Un único miembro inicializa el proyecto, configura el .gitignore y sube el esqueleto a la rama main.
2. **Clonación:** El resto del equipo descarga este esqueleto funcional.
3. **Desarrollo:** Nadie programa en la rama main. Cada miembro crea una rama específica (ej. feature/data-api).
4. **Integración:** Cuando un módulo está terminado, se fusiona hacia la rama común develop para comprobar que interactúa correctamente con el resto del sistema.