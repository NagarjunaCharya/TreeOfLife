# 🌳 TreeOfLife — AI-Powered Crop Disease Diagnosis

TreeOfLife is a **dual-verification crop disease detection system** that combines **Vision AI** (phenotype analysis) with **DNABERT-2** (genotype analysis) for accurate, AI-powered agricultural diagnostics.

## Architecture

```
Farmer → Selects Crop → Uploads Photo / DNA Sequence
                              │
               ┌──────────────┴──────────────┐
               ▼                              ▼
         VISION AI                       DNABERT-2
        (Phenotype)                     (Genotype)
     Leaf photo analysis           DNA sequence analysis
               │                              │
     "Leaf spots detected →         "Pathogen DNA found →
      looks like Blast"              confirms Blast"
               │                              │
               └──────────────┬──────────────┘
                              ▼
                    ✅ DOUBLE CONFIRMED
                   "Rice Blast — 98%"
```

## Modules

### 📱 Vision AI (Android App)
> **Path:** [`app/`](app/)

AI-powered Android application for plant disease identification using the Gemini API.

- **AI Plant Analysis** — Analyze plant photos for disease detection
- **Local Plant Database** — Save plants locally using Room Database
- **Modern UI** — Built with Jetpack Compose

**Tech Stack:** Kotlin, Jetpack Compose, Room Database, Gemini API

### 🧬 DNABERT-2 (Genotype Classifier)
> **Path:** [`dnabert/`](dnabert/)

Crop species classification from raw DNA sequences using the DNABERT-2 foundation model.

- **5 Crop Species** — Rice, Wheat, Ragi, Tomato, Rose
- **117M Parameter Model** — Fine-tuned DNABERT-2
- **732K+ Training Sequences** — From NCBI GenBank
- **Sample Datasets Included** — 100 sequences per crop for quick testing

**Tech Stack:** Python, PyTorch, Transformers, DNABERT-2

## Quick Start

### Vision AI (Android)
```bash
# Open in Android Studio
# Add your Gemini API key to .env
# Build and run
```

### DNABERT-2 (Python)
```bash
cd dnabert/
pip install -r requirements.txt

# Download model from HuggingFace
# Train: python train_epoch_1.py (through train_epoch_5.py)
# Predict: python predict.py --sequence "ATCGATCG..."
```

See [`dnabert/README.md`](dnabert/README.md) for detailed instructions.

## Supported Crops

| Crop | Vision AI | DNABERT-2 |
|------|-----------|-----------|
| 🌾 Rice | ✅ Disease detection | ✅ DNA classification |
| 🌿 Wheat | ✅ Disease detection | ✅ DNA classification |
| 🌱 Ragi | ✅ Disease detection | ✅ DNA classification |
| 🍅 Tomato | ✅ Disease detection | ✅ DNA classification |
| 🌹 Rose | ✅ Disease detection | ✅ DNA classification |

## Project Structure

```
TreeOfLife/
├── README.md                  ← You are here
├── app/                       ← Android Vision AI app
│   └── src/                   ← Kotlin source code
├── dnabert/                   ← DNABERT-2 genotype module
│   ├── README.md              ← DNABERT documentation
│   ├── train_epoch_*.py       ← Training scripts (5 epochs)
│   ├── predict.py             ← Inference script
│   ├── requirements.txt       ← Python dependencies
│   └── datasets/              ← Sample + full datasets
├── build.gradle.kts           ← Android build config
├── settings.gradle.kts        ← Android settings
└── gradle/                    ← Gradle wrapper
```

## License

This project is developed as part of a Student Internship Program (SIP).
