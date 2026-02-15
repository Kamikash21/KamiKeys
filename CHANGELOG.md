# 📚 Changelog – KamiKeys v1.5

## v1.5.0 (08/01/2026)
### ✨ Novos Recursos
- GUI do Admin completa com 4 categorias organizadas
- Submenus com filtragem inteligente e navegação premium
- Lore premium com informações completas (gerado por, data, origem, dono)
- Cópia segura de keys para admins com fechamento automático
- Exclusão com confirmação de segurança
- Sons e feedbacks premium totalmente configuráveis
- Background personalizável via config.yml
- Mensagens educativas e de responsabilidade para admins

### 🛡️ Segurança
- Separação clara de responsabilidades (jogadores vs admins)
- Proteção contra cliques acidentais
- Console limpo (sem logs de debug em produção)

### ⚙️ Configuração
- Totalmente configurável via config.yml
- Suporte a múltiplos tipos de keys
- Sistema de origens organizado (venda, interna, exclusiva)

### 🎯 Público-alvo
- Servidores em produção que buscam qualidade premium
- Redes profissionais como a KamiCraftMC
- Administradores que valorizam segurança e organização

# KamiKeys v2.1.0

## 🔐 Auditoria Profissional
- Sistema completo de logs estruturados
- Registro de IP
- Registro de ator + UUID
- Registro de transições de estado

## 🎟 Voucher System
- Correção de fluxo de expiração
- Log de tentativa de uso de voucher expirado
- Remoção física correta após uso

## 💾 Backup System
- Backup em ZIP
- Inclusão de:
  - keys.yml
  - vouchers.yml
  - pasta logs completa
- Execução assíncrona

## 🛠 Correções
- Correção de NPE em KeyService
- Correção de transição duplicada de estado
- Melhor validação de estados inválidos

---

Autor: Kamikash21
