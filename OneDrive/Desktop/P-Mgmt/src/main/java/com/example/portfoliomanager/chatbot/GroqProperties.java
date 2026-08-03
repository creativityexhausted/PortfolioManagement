package com.example.portfoliomanager.chatbot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chatbot")
public class GroqProperties {

    private final Groq groq = new Groq();
    private int maxHoldingsInContext = 30;

    public Groq getGroq() {
        return groq;
    }

    public int getMaxHoldingsInContext() {
        return maxHoldingsInContext;
    }

    public void setMaxHoldingsInContext(int maxHoldingsInContext) {
        this.maxHoldingsInContext = maxHoldingsInContext;
    }

    public static class Groq {
        private String baseUrl = "https://api.groq.com/openai/v1";
        private String apiKey;
        private String model = "llama-3.1-8b-instant";
        private String newsModel = "llama-3.3-70b-versatile";
        private double temperature = 0.2;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getNewsModel() {
            return newsModel;
        }

        public void setNewsModel(String newsModel) {
            this.newsModel = newsModel;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
}
