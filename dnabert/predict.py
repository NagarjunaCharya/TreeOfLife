"""
DNABERT-2 Crop DNA Classifier — Inference Script
Predict the crop species from a raw DNA sequence.

Usage:
    python predict.py --sequence "ATCGATCGATCG..."
    python predict.py --fasta input.fasta
"""

import argparse
import json
import os
import sys

import torch
from torch.amp import autocast
from transformers import AutoTokenizer, AutoModelForSequenceClassification

CROP_LABELS = {0: 'Rice', 1: 'Wheat', 2: 'Ragi', 3: 'Tomato', 4: 'Rose'}
MODEL_DIR = os.path.join(os.path.dirname(__file__), 'finetuned_model')


def load_model(model_dir):
    """Load the fine-tuned DNABERT-2 model and tokenizer."""
    if not os.path.exists(model_dir):
        print(f"ERROR: Model directory not found: {model_dir}")
        print("Please train the model first using train_epoch_1.py through train_epoch_5.py")
        sys.exit(1)

    tokenizer = AutoTokenizer.from_pretrained(model_dir, trust_remote_code=True)
    model = AutoModelForSequenceClassification.from_pretrained(
        model_dir, num_labels=5, trust_remote_code=True
    )

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model.to(device)
    model.eval()

    return model, tokenizer, device


def predict_sequence(model, tokenizer, device, sequence, max_length=512):
    """Predict crop species from a DNA sequence."""
    # Clean sequence
    sequence = sequence.upper().strip()
    valid_chars = set('ATCGN')
    sequence = ''.join(c for c in sequence if c in valid_chars)

    if len(sequence) < 10:
        return None, None, "Sequence too short (need at least 10 bases)"

    # Tokenize
    encoding = tokenizer(
        sequence,
        max_length=max_length,
        padding='max_length',
        truncation=True,
        return_tensors='pt',
    )

    input_ids = encoding['input_ids'].to(device)
    attention_mask = encoding['attention_mask'].to(device)

    # Predict
    with torch.no_grad():
        with autocast(device_type=str(device)):
            outputs = model(input_ids=input_ids, attention_mask=attention_mask)

    probs = torch.softmax(outputs.logits, dim=-1)[0]
    pred_idx = probs.argmax().item()
    confidence = probs[pred_idx].item() * 100

    return CROP_LABELS[pred_idx], confidence, None


def parse_fasta(filepath):
    """Parse a FASTA file and return list of (header, sequence) tuples."""
    sequences = []
    header = None
    seq_parts = []

    with open(filepath, 'r') as f:
        for line in f:
            line = line.strip()
            if line.startswith('>'):
                if header is not None:
                    sequences.append((header, ''.join(seq_parts)))
                header = line[1:]
                seq_parts = []
            elif line:
                seq_parts.append(line)

    if header is not None:
        sequences.append((header, ''.join(seq_parts)))

    return sequences


def main():
    parser = argparse.ArgumentParser(description='DNABERT-2 Crop DNA Classifier')
    parser.add_argument('--sequence', type=str, help='Raw DNA sequence (A/T/C/G)')
    parser.add_argument('--fasta', type=str, help='Path to FASTA file')
    parser.add_argument('--model-dir', type=str, default=MODEL_DIR, help='Path to fine-tuned model')
    args = parser.parse_args()

    if not args.sequence and not args.fasta:
        parser.error("Please provide --sequence or --fasta")

    print("Loading DNABERT-2 model...")
    model, tokenizer, device = load_model(args.model_dir)
    print(f"Model loaded on {device}\n")

    if args.sequence:
        crop, confidence, error = predict_sequence(model, tokenizer, device, args.sequence)
        if error:
            print(f"Error: {error}")
        else:
            print(f"{'='*50}")
            print(f"  Prediction: {crop}")
            print(f"  Confidence: {confidence:.1f}%")
            print(f"  Sequence:   {args.sequence[:50]}...")
            print(f"{'='*50}")

    elif args.fasta:
        sequences = parse_fasta(args.fasta)
        print(f"Found {len(sequences)} sequences in {args.fasta}\n")

        results = []
        for header, seq in sequences:
            crop, confidence, error = predict_sequence(model, tokenizer, device, seq)
            if error:
                print(f"  {header[:40]:40s} | ERROR: {error}")
            else:
                print(f"  {header[:40]:40s} | {crop:8s} | {confidence:.1f}%")
                results.append({'header': header, 'crop': crop, 'confidence': confidence})

        # Save results
        out_path = args.fasta.replace('.fasta', '_predictions.json')
        with open(out_path, 'w') as f:
            json.dump(results, f, indent=2)
        print(f"\nResults saved to {out_path}")


if __name__ == '__main__':
    main()
