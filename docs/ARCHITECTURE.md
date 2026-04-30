# 🏗 System Architecture — Vrikshaayush

## Overview

Vrikshaayush is a **fully offline, edge-AI mobile application**. Every computation, storage, and logic operation happens on the farmer's device. There is no server.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────┐
│              FARMER'S PHONE                 │
│                                             │
│  ┌─────────────┐    ┌─────────────────────┐ │
│  │   Camera /  │───▶│  Image Preprocessor │ │
│  │   Gallery   │    │  (resize 224x224,   │ │
│  └─────────────┘    │   normalize 0-1)    │ │
│                     └──────────┬──────────┘ │
│                                │            │
│                     ┌──────────▼──────────┐ │
│                     │   TFLite Engine     │ │
│                     │  plant_disease.     │ │
│                     │  tflite (~9MB)      │ │
│                     │  MobileNetV2 base   │ │
│                     │  38 disease classes │ │
│                     └──────────┬──────────┘ │
│                                │            │
│                     ┌──────────▼──────────┐ │
│                     │  Result Processor   │ │
│                     │  - Disease name     │ │
│                     │  - Confidence %     │ │
│                     │  - Severity level   │ │
│                     └──────────┬──────────┘ │
│                                │            │
│  ┌─────────────────────────────▼──────────┐ │
│  │          Local SQLite Database         │ │
│  │  ┌─────────────┐  ┌────────────────┐  │ │
│  │  │ ScanRecords │  │  DiseaseInfo   │  │ │
│  │  │ (history)   │  │  (treatments)  │  │ │
│  │  └─────────────┘  └────────────────┘  │ │
│  │  ┌─────────────┐                      │ │
│  │  │  UserPrefs  │                      │ │
│  │  │ (language,  │                      │ │
│  │  │  settings)  │                      │ │
│  │  └─────────────┘                      │ │
│  └────────────────────────────────────────┘ │
│                                             │
│  ┌─────────────────────────────────────────┐│
│  │         Flutter UI Layer                ││
│  │  Splash→Home→Scan→Result→Detail→        ││
│  │  History→Library→Settings               ││
│  │  (Hindi | Kannada | English)            ││
│  └─────────────────────────────────────────┘│
└─────────────────────────────────────────────┘

⚡ Zero internet required at any step
🔒 Farmer's data never leaves the device
```

---

## Technology Stack

| Layer | Technology | Reason |
|-------|-----------|--------|
| UI Framework | Flutter 3.x | Cross-platform, single codebase |
| AI Inference | TensorFlow Lite | On-device, no internet |
| AI Model | MobileNetV2 (PlantVillage) | 95% accuracy, 9MB size |
| Local Database | SQLite (sqflite) | Offline-first, reliable |
| Camera | flutter camera plugin | Native camera access |
| Internationalization | Flutter l10n | EN, HI, KN support |
| Image processing | dart image package | Resize + normalize |

---

## Data Flow

### Happy Path (Offline Diagnosis)
1. Farmer opens app
2. Taps "Scan Plant"
3. Takes photo or selects from gallery
4. `ImagePreprocessor` resizes to 224×224, normalizes to 0.0–1.0
5. `DiseaseClassifier.classify()` runs TFLite model (150ms avg)
6. Result decoded: disease name + confidence + severity
7. `DiseaseRepository.getInfo(disease)` loads from local JSON
8. Result screen shown with treatment recommendations
9. Scan auto-saved to SQLite with timestamp
10. Farmer sees diagnosis — **total time: under 2 seconds**

### No-Internet Assumption
- Model embedded in APK (no download needed after install)
- Disease database bundled as JSON assets
- Scan history in SQLite on device
- Settings in SharedPreferences

---

## Module Breakdown

### `lib/screens/`
| Screen | File | Purpose |
|--------|------|---------|
| Splash | `splash_screen.dart` | App entry, language select |
| Home | `home_screen.dart` | Dashboard, stats, scan button |
| Scanner | `scanner_screen.dart` | Camera + gallery capture |
| Diagnosis Result | `result_screen.dart` | Show AI diagnosis |
| Disease Detail | `disease_detail_screen.dart` | Full disease info |
| Scan History | `history_screen.dart` | Audit log of past scans |
| Crop Library | `library_screen.dart` | Offline disease database |
| Settings | `settings_screen.dart` | Language, notifications, data |

### `lib/utils/`
| File | Purpose |
|------|---------|
| `disease_classifier.dart` | TFLite model loader + inference |
| `image_preprocessor.dart` | Image resize + normalization |
| `database_helper.dart` | SQLite CRUD operations |
| `disease_repository.dart` | Load disease info from JSON |

### `lib/l10n/`
| File | Purpose |
|------|---------|
| `app_en.arb` | English strings |
| `app_hi.arb` | Hindi strings |
| `app_kn.arb` | Kannada strings |

---

## Key Design Decisions

### Why TFLite over cloud API?
- Rural areas have poor connectivity
- Faster response (no network round-trip)
- Privacy-preserving
- Zero operating cost

### Why MobileNetV2?
- Designed for mobile devices
- 9MB model size (fits in APK)
- 150ms inference time on mid-range phones
- 95% accuracy on PlantVillage benchmark

### Why Flutter?
- Single codebase for Android (primary) + iOS (future)
- Good offline support
- Strong Indic language support
- Large community

### Why SQLite over Hive/other NoSQL?
- SQLite is mature and reliable for structured scan records
- Easy to query by date, crop type, severity
- Native support on Android

---

## Future Scope (Post-MVP)

1. **Optional cloud sync** — when WiFi available, backup scan history
2. **Federated learning** — improve model with anonymized data from farmers
3. **Weather integration** — alert when conditions favor disease outbreaks
4. **Voice input** — farmer describes symptoms in Hindi/Kannada
5. **iOS version** — using same Flutter codebase
6. **Government scheme integration** — link diagnosis to crop insurance claims
