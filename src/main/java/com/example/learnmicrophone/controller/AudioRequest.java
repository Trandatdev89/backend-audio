package com.example.learnmicrophone.controller;

import org.springframework.web.multipart.MultipartFile;

public record AudioRequest(MultipartFile audioFile) {
}
