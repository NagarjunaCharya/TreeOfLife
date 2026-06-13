"""
Fine-tune DNABERT-2 - EPOCH 4 ONLY
Loads checkpoint from epoch 3
"""

import io
import sys
if sys.stdout.encoding and sys.stdout.encoding.lower() != 'utf-8':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

import os
import csv
import json
import time
import random
import numpy as np
from datetime import datetime

import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader, WeightedRandomSampler
from torch.amp import autocast, GradScaler

from transformers import AutoTokenizer, AutoModelForSequenceClassification
from sklearn.metrics import accuracy_score, f1_score, matthews_corrcoef

CROP_LABELS = {0: 'rice', 1: 'wheat', 2: 'ragi', 3: 'tomato', 4: 'rose'}
NUM_LABELS = 5

MODEL_PATH = r'c:\Users\Nagarjuna\OneDrive\Documents\SIP\DNABERT-2-117M'
DATASET_DIR = r'c:\Users\Nagarjuna\OneDrive\Documents\SIP\datasets'
OUTPUT_DIR = r'c:\Users\Nagarjuna\OneDrive\Documents\SIP\finetuned_model'

class CropDNADataset(Dataset):
    def __init__(self, split, dataset_dir, tokenizer, max_length=512, max_samples_per_crop=None):
        self.tokenizer = tokenizer
        self.max_length = max_length
        self.sequences = []
        self.labels = []

        crops = ['rice', 'wheat', 'ragi', 'tomato', 'rose']
        for crop in crops:
            csv_path = os.path.join(dataset_dir, crop, f'{split}.csv')
            if not os.path.exists(csv_path):
                print(f'  WARNING: {csv_path} not found, skipping')
                continue

            crop_seqs = []
            crop_labels = []
            with open(csv_path, 'r') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    crop_seqs.append(row['sequence'])
                    crop_labels.append(int(row['label']))

            if max_samples_per_crop and len(crop_seqs) > max_samples_per_crop:
                indices = random.sample(range(len(crop_seqs)), max_samples_per_crop)
                crop_seqs = [crop_seqs[i] for i in indices]
                crop_labels = [crop_labels[i] for i in indices]

            self.sequences.extend(crop_seqs)
            self.labels.extend(crop_labels)
            print(f'  {crop:8s} ({split}): {len(crop_seqs):>7,} sequences')

        print(f'  {"TOTAL":8s} ({split}): {len(self.sequences):>7,} sequences')

    def __len__(self):
        return len(self.sequences)

    def __getitem__(self, idx):
        encoding = self.tokenizer(
            self.sequences[idx],
            max_length=self.max_length,
            padding='max_length',
            truncation=True,
            return_tensors='pt',
        )
        return {
            'input_ids': encoding['input_ids'].squeeze(0),
            'attention_mask': encoding['attention_mask'].squeeze(0),
            'label': torch.tensor(self.labels[idx], dtype=torch.long),
        }

    def get_class_weights(self):
        from collections import Counter
        counts = Counter(self.labels)
        total = len(self.labels)
        weights = {cls: total / count for cls, count in counts.items()}
        sample_weights = [weights[label] for label in self.labels]
        return sample_weights


def train_epoch(model, loader, optimizer, scaler, device, grad_accum_steps=1):
    model.train()
    total_loss = 0.0
    correct = 0
    total = 0
    optimizer.zero_grad()

    for step, batch in enumerate(loader):
        input_ids = batch['input_ids'].to(device)
        attention_mask = batch['attention_mask'].to(device)
        labels = batch['label'].to(device)

        with autocast(device_type='cuda', dtype=torch.float16):
            outputs = model(
                input_ids=input_ids,
                attention_mask=attention_mask,
                labels=labels,
            )
            loss = outputs.loss / grad_accum_steps

        scaler.scale(loss).backward()

        if (step + 1) % grad_accum_steps == 0:
            scaler.unscale_(optimizer)
            torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
            scaler.step(optimizer)
            scaler.update()
            optimizer.zero_grad()

        total_loss += outputs.loss.item()
        preds = outputs.logits.argmax(dim=-1)
        correct += (preds == labels).sum().item()
        total += labels.size(0)

        if (step + 1) % 50 == 0:
            avg_loss = total_loss / (step + 1)
            acc = correct / total * 100
            print(f'    Step {step+1:>5d}/{len(loader)} | Loss: {avg_loss:.4f} | Acc: {acc:.1f}%', end='\r')

    avg_loss = total_loss / len(loader)
    accuracy = correct / total * 100
    return avg_loss, accuracy


@torch.no_grad()
def evaluate(model, loader, device):
    model.eval()
    total_loss = 0.0
    all_preds = []
    all_labels = []

    for batch in loader:
        input_ids = batch['input_ids'].to(device)
        attention_mask = batch['attention_mask'].to(device)
        labels = batch['label'].to(device)

        with autocast(device_type='cuda', dtype=torch.float16):
            outputs = model(
                input_ids=input_ids,
                attention_mask=attention_mask,
                labels=labels,
            )

        total_loss += outputs.loss.item()
        preds = outputs.logits.argmax(dim=-1)
        all_preds.extend(preds.cpu().numpy())
        all_labels.extend(labels.cpu().numpy())

    avg_loss = total_loss / len(loader)
    accuracy = accuracy_score(all_labels, all_preds) * 100
    f1_macro = f1_score(all_labels, all_preds, average='macro') * 100
    f1_weighted = f1_score(all_labels, all_preds, average='weighted') * 100
    mcc = matthews_corrcoef(all_labels, all_preds)

    return {
        'loss': avg_loss,
        'accuracy': accuracy,
        'f1_macro': f1_macro,
        'f1_weighted': f1_weighted,
        'mcc': mcc,
        'preds': all_preds,
        'labels': all_labels,
    }


def main():
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print('=' * 70)
    print('  DNABERT-2 Fine-Tuning - EPOCH 4/5')
    print('=' * 70)
    
    if torch.cuda.is_available():
        gpu_name = torch.cuda.get_device_name(0)
        gpu_mem = torch.cuda.get_device_properties(0).total_memory / 1024**3
        print(f'  Device: {gpu_name} ({gpu_mem:.1f} GB VRAM)')
    else:
        print('  Device: CPU (WARNING: Training will be very slow)')

    batch_size = 16
    grad_accum = 2
    lr = 3e-5
    max_length = 512
    max_samples = 5000
    eval_samples = 1000

    print(f'  Batch size: {batch_size} x {grad_accum} accumulation = {batch_size * grad_accum} effective')
    print(f'  Learning rate: {lr}')
    print('=' * 70)

    # Load tokenizer
    print('\n[1/4] Loading DNABERT-2 tokenizer...')
    tokenizer = AutoTokenizer.from_pretrained(OUTPUT_DIR, trust_remote_code=True)
    print(f'  Vocab size: {tokenizer.vocab_size}')

    # Load datasets
    print('\n[2/4] Loading datasets...')
    print('\n  --- Training Set ---')
    train_dataset = CropDNADataset('train', DATASET_DIR, tokenizer, max_length=max_length, max_samples_per_crop=max_samples)
    print('\n  --- Validation Set ---')
    dev_dataset = CropDNADataset('dev', DATASET_DIR, tokenizer, max_length=max_length, max_samples_per_crop=eval_samples)

    sample_weights = train_dataset.get_class_weights()
    sampler = WeightedRandomSampler(sample_weights, len(train_dataset), replacement=True)

    train_loader = DataLoader(train_dataset, batch_size=batch_size, sampler=sampler, num_workers=0, pin_memory=True, drop_last=True)
    dev_loader = DataLoader(dev_dataset, batch_size=batch_size * 2, shuffle=False, num_workers=0, pin_memory=True)

    # Load model from checkpoint
    print('\n[3/4] Loading model from epoch 3 checkpoint...')
    model = AutoModelForSequenceClassification.from_pretrained(OUTPUT_DIR, num_labels=NUM_LABELS, trust_remote_code=True)
    model.to(device)

    checkpoint_path = os.path.join(OUTPUT_DIR, 'checkpoint_epoch3.pt')
    if os.path.exists(checkpoint_path):
        checkpoint = torch.load(checkpoint_path, map_location=device)
        model.load_state_dict(checkpoint['model_state'])
        print('  ✓ Loaded model state from epoch 3')

    total_params = sum(p.numel() for p in model.parameters())
    trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
    print(f'  Total parameters: {total_params:,}')
    print(f'  Trainable parameters: {trainable_params:,}')

    # Optimizer & Scheduler
    optimizer = torch.optim.AdamW(model.parameters(), lr=lr, weight_decay=0.01)
    total_steps = len(train_loader) // grad_accum
    warmup_steps = int(total_steps * 0.1)

    from transformers import get_linear_schedule_with_warmup
    scheduler = get_linear_schedule_with_warmup(optimizer, num_warmup_steps=warmup_steps, num_training_steps=total_steps)

    if os.path.exists(checkpoint_path):
        optimizer.load_state_dict(checkpoint['optimizer_state'])
        scheduler.load_state_dict(checkpoint['scheduler_state'])
        print('  ✓ Loaded optimizer and scheduler state')

    scaler = GradScaler()

    # Training EPOCH 4
    print(f'\n[4/4] Training Epoch 4/5...')
    print(f'  Warmup steps: {warmup_steps} | Total steps: {total_steps}')

    epoch_start = time.time()

    print(f'\n  --- Epoch 4/5 ---')
    train_loss, train_acc = train_epoch(model, train_loader, optimizer, scaler, device, grad_accum_steps=grad_accum)
    scheduler.step()

    dev_metrics = evaluate(model, dev_loader, device)
    epoch_time = time.time() - epoch_start

    print(f'\n  Epoch 4 Summary:')
    print(f'    Train Loss: {train_loss:.4f} | Train Acc: {train_acc:.1f}%')
    print(f'    Val   Loss: {dev_metrics["loss"]:.4f} | Val   Acc: {dev_metrics["accuracy"]:.1f}%')
    print(f'    Val F1 (macro): {dev_metrics["f1_macro"]:.1f}% | MCC: {dev_metrics["mcc"]:.4f}')
    print(f'    Time: {epoch_time:.1f}s')

    # Save model after epoch 4
    print(f'    Saving model after epoch 4...')
    model.save_pretrained(OUTPUT_DIR)

    checkpoint = {
        'epoch': 4,
        'model_state': model.state_dict(),
        'optimizer_state': optimizer.state_dict(),
        'scheduler_state': scheduler.state_dict(),
        'train_loss': train_loss,
        'val_f1_macro': dev_metrics['f1_macro'],
    }
    torch.save(checkpoint, os.path.join(OUTPUT_DIR, 'checkpoint_epoch4.pt'))

    print(f'\n{"=" * 70}')
    print(f'  Epoch 4 Complete! Model and checkpoint saved.')
    print(f'  Next: Run train_epoch_5.py')
    print(f'{"=" * 70}')


if __name__ == '__main__':
    main()
