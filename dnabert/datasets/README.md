# DNABERT-2 Datasets

## Sample Data (Included in Repo)

The `samples/` folder contains 100 DNA sequences per crop for quick testing:

| File | Crop | Sequences |
|------|------|-----------|
| `rice_sample.csv` | Rice (*Oryza sativa*) | 100 |
| `wheat_sample.csv` | Wheat (*Triticum aestivum*) | 100 |
| `ragi_sample.csv` | Ragi (*Eleusine coracana*) | 100 |
| `tomato_sample.csv` | Tomato (*Solanum lycopersicum*) | 100 |
| `rose_sample.csv` | Rose (*Rosa chinensis*) | 100 |

## Full Dataset (Download Required)

The full training dataset contains **732,641 DNA sequences** across 5 crops:

| Crop | Train | Dev | Test |
|------|-------|-----|------|
| Rice | 148,364 | 18,483 | 18,514 |
| Wheat | 329,461 | 41,103 | 41,159 |
| Ragi | 60,970 | 7,585 | 7,594 |
| Tomato | 80,686 | 10,050 | 10,053 |
| Rose | 113,160 | 14,110 | 14,132 |

### How to Generate the Full Dataset

```bash
# Step 1: Download raw sequences from NCBI
python download_sequences.py

# Step 2: Process into train/dev/test splits
python process_for_dnabert2.py
```

This will create the full dataset in the `datasets/` folder (~750 MB).

## Data Format

Each CSV file has two columns:

```csv
sequence,label
ATCGATCGATCGATCG...,0
GCTAGCTAGCTAGCTA...,1
```

### Label Mapping

| Label | Crop |
|-------|------|
| 0 | Rice |
| 1 | Wheat |
| 2 | Ragi |
| 3 | Tomato |
| 4 | Rose |
