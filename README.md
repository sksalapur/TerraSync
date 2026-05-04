<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher.png" alt="TerraSync Logo" width="120" />
</p>

<h1 align="center">🌍 TerraSync</h1>

<p align="center">
  <strong>Geotechnical Engineering & Soil Permeability Tracking</strong>
</p>

<p align="center">
  <em>An enterprise-grade mobile application for tracking soil nodes, calculating complex permeability (k-values), and running real-time Gemini AI risk inference on geotechnical sites.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-Jetpack_Compose-3DDC84?logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-Coroutines-7F52FF?logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Firebase-Auth_%7C_Firestore-FFCA28?logo=firebase&logoColor=black" />
  <img src="https://img.shields.io/badge/Google_Gemini-Generative_AI-4285F4?logo=google&logoColor=white" />
  <img src="https://img.shields.io/badge/Architecture-MVVM_%7C_Hilt-000000" />
</p>

---

## 🤔 What Is This?

In large-scale construction and geotechnical engineering, engineers collect dozens of soil samples (nodes) across a site. Calculating the soil's Coefficient of Permeability ($k$) and assessing collapse risks traditionally requires manual spreadsheet entry back at the office. 

**TerraSync** digitizes this entirely. It provides a heavily validated, real-time edge-computing environment where engineers can input raw granular data (D10, D30, D60, Void Ratios) right on the field, instantly calculate the permeability, and use an LLM (Gemini) to evaluate site safety.

---

## 🧠 Core Engineering Features

### 📐 Edge-Computed Geotechnical Mathematics
- **Instant K-Value Computation**: Calculates the Coefficient of Permeability ($k$) dynamically based on real-time inputs of $D_{10}$, $D_{30}$, $D_{60}$, and Void Ratio ($e$).
- **Strict Data Validation**: Real-time form validation preventing physically impossible engineering states (e.g., $D_{60}$ must be strictly $\ge D_{30}$, Void ratios must be bounded between 0 and 10).

### 🤖 Generative AI Risk Inference
Instead of static threshold alerts, TerraSync uses the **Google Generative AI SDK (Gemini)**.
- Sends the fully computed soil node metrics to Gemini.
- Returns a structured `GeminiInference` containing a distinct **Risk Level** (Low/Moderate/High) and specific **Reinforcement Suggestions** for the civil engineers.
- Auto-caches the AI result in Firestore to prevent redundant API calls.

### ⚡ Reactive Compose Architecture
- Built 100% in **Jetpack Compose** with Material 3.
- Utilizes `MutableStateFlow` and Coroutines for deep state-management across complex forms.
- Form states are meticulously tracked so that if an engineer modifies a previously evaluated node, the cached AI inference is immediately invalidated and re-computed.

---

## 🏗️ Architecture

```text
┌─────────────────────────┐            ┌─────────────────────────────────┐
│   Field Engineer        │            │   TerraSync Architecture        │
│   (Android App)         │ ────────►  │                                 │
│                         │            │  ┌──────────┐  ┌─────────────┐  │
│  • Input Soil Params    │            │  │ MVVM     │  │ UseCases    │  │
│  • Create Sites         │            │  │ StateFlow│  │ Math Engine │  │
│  • View Risk Reports    │            │  └────┬─────┘  └──────┬──────┘  │
│                         │            │       │               │         │
│                         │ ◄────────  │  ┌────▼───────────────▼──────┐  │
│                         │            │  │ Gemini Generative AI SDK  │  │
│                         │            │  │ (Real-time Site Analysis) │  │
│                         │            │  └─────────────┬─────────────┘  │
└─────────────────────────┘            │                │                │
                                       │  ┌─────────────▼─────────────┐  │
                                       │  │ Firebase Firestore        │  │
                                       │  │ (Offline-first Cloud DB)  │  │
                                       │  └───────────────────────────┘  │
                                       └─────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **UI** | Jetpack Compose, Material 3 | Fully declarative, reactive frontend |
| **State** | Kotlin Coroutines, StateFlow | Asynchronous state management & validation |
| **Architecture** | MVVM, Clean Architecture | Separating calculation usecases from the UI |
| **Dependency Injection**| Dagger Hilt + KSP | Fast, compile-time dependency provisioning |
| **Cloud** | Firebase Auth + Firestore | Secure identity and NoSQL cloud syncing |
| **AI** | Google Generative AI SDK | Live soil risk analysis and reporting |
| **Navigation** | Navigation Compose | Type-safe declarative routing |

---

<p align="center">
  <strong>Engineering precision. Artificial Intelligence. Real-time synchronization.</strong><br/>
</p>

<p align="center">
  Made with ❤️ by <a href="https://github.com/sksalapur">Shashank Salapur</a>
</p>
