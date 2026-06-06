# Segmentation

Treinamento de segmentação semântica (Oxford-IIIT Pet + UNet) e implantação no Android com TFLite.

## Estrutura

| Pasta | Descrição |
|-------|-----------|
| `Colab/Segmentacao.ipynb` | Pipeline completo no Colab (treino, métricas, export TFLite) |
| `Android/` | App Android com overlay da máscara sobre a imagem |

## 1. Treinar no Colab

1. Abra `Colab/Segmentacao.ipynb` no [Google Colab](https://colab.research.google.com) (File → Upload notebook).
2. Ative **Runtime → Change runtime type → GPU (T4)**.
3. Execute todas as células em ordem (**Runtime → Run all**).

O script irá:

- Baixar o **Oxford-IIIT Pet** (`torchvision`)
- Treinar uma **UNet** (ResNet18) em um subset de 1000 imagens
- Calcular **IoU** e **acurácia** com `torchmetrics`
- Gerar visualizações com overlay
- Exportar `model.tflite` e `model_int8.tflite`

## 2. Implantar no Android

1. Copie `model.tflite` para `Android/app/src/main/assets/model.tflite`.
2. Abra a pasta `Android/` no Android Studio.
3. Conecte o smartphone e execute o app (**Run**).
4. Toque em **Escolher imagem** e selecione uma foto de pet.
5. O app exibe a imagem com a **máscara sobreposta** em vermelho.

## 3. Entrega (Classroom)

- Print da inferência no celular
- Link do repositório GitHub

## Parâmetros principais

| Parâmetro | Valor |
|-----------|-------|
| Dataset | Oxford-IIIT Pet (subset 1000) |
| Modelo | UNet + ResNet18 |
| Tamanho da imagem | 128×128 |
| Classes | 2 (fundo / pet) |
| Épocas | 10 |
