# 🗝️ KamiKeys
### Sistema Premium de Keys Ativáveis para Servidores Minecraft

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Spigot 1.20+](https://img.shields.io/badge/Minecraft-1.16%2B-orange)]()
[![KamiPlugins](https://img.shields.io/badge/By-KamiPlugins-purple)]()

> **Segurança** • **Exclusividade** • **Auditoria Total** • **Experiência Premium**

---

## 🌟 Por que escolher o KamiKeys?

KamiKeys não é só um gerador de códigos — é um **sistema de recompensas profissional**, projetado para:

- ✅ **Vender itens reais** (VIP, coins, ranks) via Discord + Pix
- ✅ **Distribuir recompensas exclusivas** em eventos
- ✅ **Controlar acesso com segurança total** (UUID, logs, anti-fraude)
- ✅ **Manter auditoria completa** de todas as ações
- ✅ **Impressionar seus jogadores** com uma experiência visual premium

---

## 🚀 Recursos-Chave

| Categoria | Recurso |
|---------|--------|
| 🔑 **Geração de Keys** | Suporte a múltiplos tipos (`basica`, `comum`, `rara`), com cores, comandos e mensagens personalizáveis |
| 👤 **Exclusividade** | Keys vinculadas à **UUID do jogador** — impossível de transferir |
| 📋 **Gestão Inteligente** | Apagar por **origem**, **tipo**, **jogador** ou **tudo** (com confirmação segura) |
| 💾 **Backup Visual** | Comando `/kamikeys exportar` gera YAML organizado para arquivamento |
| 📊 **Log Completo** | Cada key gerada, ativada ou apagada é registrada com detalhes |
| 🎨 **Experiência Premium** | Mensagens coloridas, keys clicáveis, TabComplete inteligente, GUI em desenvolvimento |
| 🔐 **Segurança** | Validação rigorosa, proteção contra uso indevido, sistema pronto para vouchers |

---

## 📦 Instalação

1. **Baixe** o arquivo `.jar` da [última versão](https://github.com/seu-usuario/KamiKeys/releases)
2. **Coloque** em `plugins/` do seu servidor Spigot/Paper 1.20+
3. **Reinicie** o servidor
4. **Configure** `plugins/KamiKeys/config.yml` conforme sua necessidade

> 💡 **Recomendado**: use com [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.6343/) para recompensas em coins.

---

## ⚙️ Exemplo de Configuração (`config.yml`)

```yaml
Types:
  basica:
    Length: 15
    PrefixColor: "&8"
    Commands:
      - "playerpoints give {player} 100"
    SuccessMessage: "&aVocê resgatou &e100 coins&a!"

  especial:
    Length: 30
    PrefixColor: "&6"
    Commands:
      - "playerpoints give {player} 5000"
      - "lp user {player} parent add vip"
    SuccessMessage: "&aVIP ativado! Bem-vindo ao clube exclusivo!"