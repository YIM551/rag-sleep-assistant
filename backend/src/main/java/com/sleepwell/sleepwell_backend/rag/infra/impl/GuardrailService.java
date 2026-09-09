package com.sleepwell.sleepwell_backend.rag.infra.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class GuardrailService {
    private final int yearMin;
    private final List<String> redFlags;

    public GuardrailService(
        @Value("${rag.guard.yearMin:2015}") int yearMin,
        @Value("#{'${rag.guard.redFlags:자살,극심한 호흡곤란,흉통,임신 합병증,아동 학대}'.split(',')}") List<String> redFlags
    ){
        this.yearMin = yearMin; this.redFlags = redFlags;
    }

    public boolean hasRedFlag(String input){
        if (input == null || input.isBlank()) return false;
        String lc = input.toLowerCase();
        return redFlags.stream().anyMatch(f -> lc.contains(f));
    }
}
