# 🌿 Vrikshaayush — AI Plant Health Diagnostic App

> *Vrikshaayush* (वृक्षायुष) — "Life of the Plant" in Sanskrit

An **offline-first, edge-AI mobile app** for rural Indian farmers to diagnose plant diseases instantly — no internet required.

---

## 📱 What It Does

- 📸 Farmer takes a photo of their plant
- 🤖 AI diagnoses the disease **on-device** (zero internet needed)
- 💊 Shows treatment recommendations instantly
- 📚 Offline crop & disease library
- 📊 Scan history & audit logs
- 🌐 Supports **Hindi | Kannada | English**

---

## 🏗 Architecture

```
Farmer's Phone
│
├── Camera / Gallery Input
│
├── TensorFlow Lite Model (on-device)
│   └── plant_disease_model.tflite
│       └── Trained on PlantVillage Dataset (50,000+ images, 38 classes)
│
├── Local SQLite Database
│   └── Scan history, disease library, user preferences
│
└── UI (Flutter)
    └── 8 screens, multilingual, offline-ready
```

---

## 📂 Folder Structure

```
vrikshaayush/
├── assets/
│   ├── images/          # Disease reference images
│   └── icons/           # App icons
├── model/
│   ├── plant_disease_model.tflite   # TFLite model (download separately)
│   └── labels.txt                   # 38 disease class labels
├── lib/
│   ├── screens/         # All 8 UI screens
│   ├── widgets/         # Reusable UI components
│   ├── data/            # Disease database (JSON)
│   ├── utils/           # TFLite inference helper, image preprocessing
│   └── l10n/            # Translations (EN, HI, KN)
├── database/
│   └── diseases.json    # Full offline disease database
├── docs/
│   ├── TFLITE_GUIDE.md          # TFLite integration guide
│   ├── ARCHITECTURE.md          # System architecture
│   ├── STRESS_TEST.md           # Architecture stress-test analysis
│   └── UI_PROMPTS.md            # Stitch UI prompts for all 8 screens
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- Flutter SDK (>=3.0.0)
- Android Studio or VS Code
- Android device or emulator (API 24+)

### Setup
```bash
git clone https://github.com/t23050288-cmyk/vrikshaayush.git
cd vrikshaayush
flutter pub get
```

### Download the TFLite Model
See `docs/TFLITE_GUIDE.md` for step-by-step model download and integration.

---

## 🤖 AI Model

- **Base model:** MobileNetV2 trained on PlantVillage dataset
- **Format:** TensorFlow Lite (.tflite)
- **Input:** 224×224 RGB image
- **Output:** 38 disease classes + confidence score
- **Size:** ~9MB (runs on any Android phone from 2018+)
- **Inference time:** ~150ms on mid-range device

---

## 🌾 Supported Crops & Diseases

| Crop | Diseases Covered |
|------|-----------------|
| Tomato | Early Blight, Late Blight, Leaf Mold, Mosaic Virus, etc. |
| Rice | Blast, Brown Spot, Neck Rot |
| Wheat | Leaf Rust, Yellow Rust, Powdery Mildew |
| Cotton | Boll Rot, Bacterial Blight, Leaf Curl |
| Maize | Common Rust, Gray Leaf Spot, Northern Blight |
| Apple | Scab, Black Rot, Cedar Apple Rust |
| Potato | Early Blight, Late Blight, Healthy |

---

## 🎨 Design System

| Color | Hex | Usage |
|-------|-----|-------|
| Forest Green | `#2E7D32` | Primary, buttons, headers |
| Warm Amber | `#F9A825` | Accent, gallery button, highlights |
| Off-White/Cream | `#F5F5F0` | Background |
| Near Black | `#1C1C1C` | Body text |
| Deep Red | `#C62828` | High severity alerts |

---

## 📚 College Project Context

**Course:** Interdisciplinary Project
**Problem:** Karnataka farmers lost ₹X crores due to plant diseases & unpredictable weather (2023)
**Solution:** Edge-AI diagnosis tool that works in zero-connectivity rural areas
**Disciplines:** Agriculture + Computer Science (AI/ML) + Design Thinking

---

## 🔒 Privacy

All data stays on the device. No photos, location, or farmer data is ever sent to a server. The TFLite model runs entirely offline.

---

*Built with ❤️ for Indian farmers*
