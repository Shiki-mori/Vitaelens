package com.phrolova.vitaelensbackend.service;

public interface CacheService {
    void setAnalysisResult(String inputHash, Object result);
    Object getAnalysisResult(String inputHash);
    void deleteAnalysisResult(String inputHash);
}
