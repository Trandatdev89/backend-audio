package com.example.learnmicrophone.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

@Service
public class DeepgramService {

    private final WebClient webClient;

    // Thay YOUR_API_KEY bằng API Key thực tế của bạn từ Deepgram
    private static final String API_KEY = "b4608e6fa6c60d864c7bd0ac9066da2682d376b8";

    public DeepgramService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.deepgram.com/v1/listen?model=nova-3&smart_format=true")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Token " + API_KEY)
                .build();
    }

    public String transcribe(MultipartFile file) throws IOException {
        byte[] audioData = file.getBytes();

        String response = webClient.post()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(audioData)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Trích xuất văn bản từ JSON nếu cần (tùy vào bạn muốn xử lý kiểu gì)
        return extractTranscript(response);
    }

    private String extractTranscript(String jsonResponse) {
        try {
            // Sử dụng Jackson để đọc JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonResponse);
            return node.at("/results/channels/0/alternatives/0/transcript").asText();
        } catch (Exception e) {
            return "[Không thể phân tích kết quả]";
        }
    }
}
