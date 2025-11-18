package com.microservices.log430.marketdataservice.adapters.web.dto;

import java.util.List;

public class SymbolsResponse {
    private List<String> symbols;

    public SymbolsResponse(List<String> symbols) {
        this.symbols = symbols;
    }

    public List<String> getSymbols() {
        return symbols;
    }
    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }
}