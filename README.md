# Crop Disease Detector — AgroScan AI 🌾

An advanced, AI-powered **Crop Disease Detection & Botanical Diagnostic Android Application** built with modern Android development standards, Jetpack Compose, Room Database, and the Google Gemini 3.5 Flash Multimodal Cognitive API.

This application translates agricultural computer vision and leaf pathology into a responsive, local-first Android experience, allowing farmers and researchers to capture or upload images of crop leaves (such as Tomato, Potato, Corn, and Pepper) to instantly detect infections, determine confidence, and obtain ecological treatments.

---

## 🎨 Visual Preview & Design Philosophy

AgroScan AI utilizes a custom **Botanical Forest Theme** focusing on earthy moss-greens, crisp white canvases, and rich crimson alerts. 
*   **Asymmetrical Hero Header**: A minimalist, high-contrast banner of sunlit modern farms to set a professional tone.
*   **Circular Progress Metrics**: A custom Canvas-drawn ring indicating the percentage of healthy vs. infected scans.
*   **Swipe-to-Details Archives**: Comprehensive historical records of all diagnostics, allowing instant review of recommendations.

---

## 🏗️ Architecture Design & Flow

```
+--------------------------------------------------------------------------+
|                            USER INTERFACE                                |
|   [Home Screen] -> [Scan Leaf Screen] -> [Archive History] -> [About]   |
+--------------------------------------------------------------------------+
                                     | (Jetpack Compose View States)
                                     v
+--------------------------------------------------------------------------+
|                             VIEW MODEL                                   |
|                          [CropViewModel]                                 |
+--------------------------------------------------------------------------+
                    |                                 |
                    | (Repository Pattern)            | (Retrofit REST Client)
                    v                                 v
+--------------------------------------+   +-------------------------------+
|             DATABASE LAYER           |   |           AI COGNITIVE        |
|            (Room SQLite)             |   |       (Gemini 3.5 Flash)      |
|  [CropPrediction] -> [PredictionDao] |   |  Multi-Modal Vision Analysis  |
+--------------------------------------+   +-------------------------------+
```

### 1. Unified Diagnostic Lifecycle Flow
1.  **Selection**: The user selects or captures a leaf image (`Camera` or `Gallery`).
2.  **Compression**: The image is automatically scaled and downsampled to a `1024px` safe boundary before base64 packaging to minimize bandwidth.
3.  **Analysis**: The base64 payload is securely sent to the **Gemini 3.5 Flash** endpoint using specialized botanical pathologists' diagnostic system instructions.
4.  **Parsing**: Gemini's structured JSON is compiled using Moshi's type-safe adapters into `CropAnalysisResult`.
5.  **Persistence**: The diagnosis is saved locally in the SQLite database via a **Room Repository**.
6.  **Presentation**: The user interacts with tabbed recommendations (Symptoms, Causes, Treatment, Prevention) and can share reports natively.

---

## 📁 Folder Structure

```
AgroScanAI/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt               # Central Navigation Router
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/
│   │   │   │   │   │   ├── CropAnalysisResult.kt # API Data Models
│   │   │   │   │   │   └── GeminiClient.kt        # Retrofit & OkHttp REST Config
│   │   │   │   │   └── database/
│   │   │   │   │       ├── AppDatabase.kt        # Room Database Singleton
│   │   │   │   │       ├── CropPrediction.kt     # Room Local Database Entity
│   │   │   │   │       └── CropPredictionDao.kt  # Local Query Definitions
│   │   │   │   │   └── repository/
│   │   │   │   │       └── CropPredictionRepository.kt # Repository Abstraction
│   │   │   │   └── ui/
│   │   │   │       ├── screens/
│   │   │   │       │   ├── AboutScreen.kt        # Technical Info Page
│   │   │   │       │   ├── ContactScreen.kt      # Support Ticket & Share Page
│   │   │   │       │   ├── DiseaseDetectionScreen.kt # Camera/Gallery Analysis
│   │   │   │       │   └── HomeScreen.kt         # Custom Canvas Analytics Dashboard
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt              # Custom Botanical Green Tokens
│   │   │   │           ├── Theme.kt              # Material 3 Custom Theme Setup
│   │   │   │           └── Type.kt               # Typographic Scalings
│   │   │   │
│   │   │   └── res/
│   │   │       ├── drawable/
│   │   │       │   ├── img_hero_banner.jpg       # Beautiful generated banner
│   │   │       │   └── img_app_icon_fg.jpg       # App logo foreground
│   │   │       └── xml/
│   │   │           └── file_paths.xml            # Camera file provider rules
│   │   │
│   │   └── test/                                 # Robolectric & Roborazzi Visual Tests
│   └── build.gradle.kts                          # Module dependencies config
│
├── .env.example                                  # API key placeholders
├── metadata.json                                 # AI Studio Metadata
└── README.md                                     # Documentation
```

---

## 🗄️ Database Schema (Room SQLite)

### Table: `crop_predictions`
| Column Name | Data Type | Key / Attribute | Purpose |
|---|---|---|---|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique identifier for each record. |
| `crop` | `TEXT` | `NOT NULL` | Name of the scanned crop (e.g., Tomato). |
| `disease` | `TEXT` | `NOT NULL` | Name of identified illness (e.g., Early Blight). |
| `confidence` | `REAL` | `NOT NULL` | Confidence percentage matching the disease. |
| `description` | `TEXT` | `NOT NULL` | Broad summary of the plant's health status. |
| `symptomsJson` | `TEXT` | `NOT NULL` | String-delimited (`||`) list of key symptoms. |
| `causesJson` | `TEXT` | `NOT NULL` | String-delimited (`||`) list of environmental causes. |
| `treatmentJson` | `TEXT` | `NOT NULL` | String-delimited (`||`) list of bio/chemical remedies. |
| `preventionJson`| `TEXT` | `NOT NULL` | String-delimited (`||`) list of preventive measures. |
| `imageUri` | `TEXT` | `NULLABLE` | Local FileProvider path to the captured image. |
| `timestamp` | `INTEGER` | `NOT NULL` | Epoch millisecond timestamp of the diagnostic. |

---

## ⚙️ Setup & Installation Guide

Follow these steps to run the application locally or in the Android Studio IDE:

### Prerequisite: Gemini API Key Configuration
1.  Obtain an API key from the [Google AI Studio Console](https://aistudio.google.com/).
2.  In the AI Studio user interface, navigate to the **Secrets panel** on the bottom-left sidebar.
3.  Add a new secret key:
    -   **Name**: `GEMINI_API_KEY`
    -   **Value**: *Your actual API key string*
4.  The system automatically compiles this value into `BuildConfig.GEMINI_API_KEY` using the **Secrets Gradle Plugin** configured in `.env` / `build.gradle.kts`.

### Option A: Running inside AI Studio
*   The system includes an incremental Android compilation container. Simply review changes and allow the **Streaming Android Emulator** on the right side of your browser viewport to boot and refresh!

### Option B: Exporting to Android Studio
1.  Click **Export ZIP** or push the repository to **GitHub** via the project settings menu.
2.  Unpack the ZIP and open the project directory in **Android Studio (Ladybug or newer)**.
3.  Ensure your Android Studio SDK is configured with Android 14+ (API 34/35/36).
4.  Create a file named `.env` in your root directory and write your API key:
    ```properties
    GEMINI_API_KEY=your_actual_api_key_here
    ```
5.  Sync Gradle dependencies.
6.  Connect a physical Android device or launch the Android Emulator, and press **Run (Shift + F10)**.

---

## 🛡️ Coding Standards & Security Best Practices

1.  **Decompilation Protection**: Secrets are managed via `.env` properties parsed by the **Secrets Gradle Plugin**, avoiding hardcoding sensitive API keys inside Java/Kotlin files.
2.  **Edge-to-Edge Compliance**: Employs `enableEdgeToEdge()` with strict bottom-bar navigation insets, preventing gesture overlapping across device dimensions.
3.  **Low-Footprint Performance**: Images are downsampled to `1024px` and heavily compressed using high-quality JPEG encoders before API transmission, avoiding memory thrashing.
4.  **Local Isolation**: Room operations are scheduled asynchronously within a custom `viewModelScope` on the `Dispatchers.IO` coroutine threadpool, ensuring the UI rendering thread maintains 60fps animations.
