# 🧬 DNABERT-2 — Crop DNA Classifier

Genotype-based crop species classification using [DNABERT-2](https://github.com/MAGICS-LAB/DNABERT_2), a pre-trained DNA language model.

This module is part of the **TreeOfLife** dual-verification system:
- **Vision AI** (Android app) → identifies crop diseases from leaf photos (phenotype)
- **DNABERT-2** (this module) → classifies crops from DNA sequences (genotype)

## Architecture

```
Input: Raw DNA sequence (A/T/C/G)
  ↓
DNABERT-2 (117M parameters, BPE tokenizer)
  ↓
Classification Head (5 classes)
  ↓
Output: Rice | Wheat | Ragi | Tomato | Rose
```

## Supported Crops

| Crop | Scientific Name | NCBI Taxonomy |
|------|----------------|---------------|
| 🌾 Rice | *Oryza sativa* | 4530 |
| 🌿 Wheat | *Triticum aestivum* | 4565 |
| 🌱 Ragi | *Eleusine coracana* | 4509 |
| 🍅 Tomato | *Solanum lycopersicum* | 4081 |
| 🌹 Rose | *Rosa chinensis* | 74649 |

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Download DNABERT-2 Model

```bash
# Option A: Using git lfs
git lfs install
git clone https://huggingface.co/zhihan1996/DNABERT-2-117M

# Option B: Using huggingface_hub
pip install huggingface_hub
python -c "from huggingface_hub import snapshot_download; snapshot_download('zhihan1996/DNABERT-2-117M')"
```

### 3. Generate Full Dataset

```bash
python download_sequences.py      # Downloads from NCBI
python process_for_dnabert2.py     # Creates train/dev/test splits
```

### 4. Train (5 epochs)

```bash
python train_epoch_1.py    # ~1-2 hours on RTX 4050
python train_epoch_2.py
python train_epoch_3.py
python train_epoch_4.py
python train_epoch_5.py    # Final evaluation + test results
```

### 5. Predict

```bash
# Single sequence
python predict.py --sequence "ATCGATCGATCGATCG..."

# FASTA file
python predict.py --fasta input.fasta
```

## Training Details

| Parameter | Value |
|-----------|-------|
| Base Model | DNABERT-2-117M (MosaicBERT) |
| Parameters | 117,072,389 |
| Tokenizer | BPE (vocab size: 4,096) |
| Max Sequence Length | 512 tokens |
| Batch Size | 16 × 2 (gradient accumulation) = 32 effective |
| Learning Rate | 3e-5 |
| Optimizer | AdamW (weight decay: 0.01) |
| Mixed Precision | FP16 (autocast) |
| GPU | NVIDIA RTX 4050 (6 GB VRAM) |

## File Structure

```
dnabert/
├── README.md                  ← You are here
├── requirements.txt           ← Python dependencies
├── predict.py                 ← Inference script
├── train_epoch_1.py           ← Epoch 1 training
├── train_epoch_2.py           ← Epoch 2 training
├── train_epoch_3.py           ← Epoch 3 training
├── train_epoch_4.py           ← Epoch 4 training
├── train_epoch_5.py           ← Epoch 5 + test evaluation
├── datasets/
│   ├── README.md              ← Dataset documentation
│   └── samples/               ← 100 sequences per crop (demo)
│       ├── rice_sample.csv
│       ├── wheat_sample.csv
│       ├── ragi_sample.csv
│       ├── tomato_sample.csv
│       └── rose_sample.csv
├── DNABERT-2-117M/            ← (not in repo — download separately)
└── finetuned_model/           ← (generated after training)
```

## References

- [DNABERT-2: Efficient Foundation Model and Benchmark for Multi-Species Genome](https://arxiv.org/abs/2306.15006)
- [DNABERT-2 on HuggingFace](https://huggingface.co/zhihan1996/DNABERT-2-117M)
- [MAGICS-LAB/DNABERT_2 GitHub](https://github.com/MAGICS-LAB/DNABERT_2)
