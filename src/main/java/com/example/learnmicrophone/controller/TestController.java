package com.example.learnmicrophone.controller;


import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin(value = "http://localhost:5173")
public class TestController {
    private final DeepgramService deepgramService;

    public TestController(DeepgramService deepgramService) {
        this.deepgramService = deepgramService;
    }

    @PostMapping("/transcribe")
    public String transcribeAudio(@ModelAttribute AudioRequest audioRequest) throws IOException {
        return deepgramService.transcribe(audioRequest.audioFile());
    }
}
