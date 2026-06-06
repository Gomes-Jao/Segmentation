# Segmentation

Treinamento de segmentação semântica (Oxford-IIIT Pet + UNet) e implantação no Android com TFLite.

## Estrutura

| Pasta | Descrição |
|-------|-----------|
| `Colab/Segmentacao.ipynb` | Pipeline completo no Colab (treino, métricas, export TFLite) |
| `Android/` | App Android com overlay da máscara sobre a imagem |

## Parâmetros principais

| Parâmetro | Valor |
|-----------|-------|
| Dataset | Oxford-IIIT Pet (subset 1000) |
| Modelo | UNet + ResNet18 |
| Tamanho da imagem | 128×128 |
| Classes | 2 (fundo / pet) |
| Épocas | 10 |
