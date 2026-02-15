package com.kamiplugins.kamikeys.utils;

public class KeyBatchDeleteResult {

    private final int totalEncontradas;
    private final int apagadas;
    private final int ignoradasPorEstado;

    public KeyBatchDeleteResult(int totalEncontradas, int apagadas, int ignoradasPorEstado) {
        this.totalEncontradas = totalEncontradas;
        this.apagadas = apagadas;
        this.ignoradasPorEstado = ignoradasPorEstado;
    }

    public int getTotalEncontradas() {
        return totalEncontradas;
    }

    public int getApagadas() {
        return apagadas;
    }

    public int getIgnoradasPorEstado() {
        return ignoradasPorEstado;
    }
}
