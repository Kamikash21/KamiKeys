package com.kamiplugins.kamikeys.models.enums;

public enum KeyState {

    // ===== CRIAÇÃO =====
    ATIVA,        // Key interna ou dada manualmente
    VENDA,        // Key criada para monetização

    // ===== FLUXO DE VENDA =====
    RESERVADA,    // Bot reservou (pagamento pendente)
    VENDIDA,      // Pagamento confirmado, aguardando ativação

    // ===== FLUXO DE VOUCHER =====
    VOUCHER,      // Convertida em voucher
    EXPIRADA,     // Voucher expirado

    // ===== FINAL =====
    USADA,        // Ativada pelo player
    BLOQUEADA,    // Key inválida / corrompida
    EXCLUIDA      // Key removida logicamente
}

