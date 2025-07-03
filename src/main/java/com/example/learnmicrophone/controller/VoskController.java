package com.example.learnmicrophone.controller;

import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;

@RestController
@CrossOrigin(value = "*")
public class VoskController {

    @Value("${vosk.model.path}")
    private String voskModelPath;

    private static Model model;


    @PostConstruct
    public void init(){
        LibVosk.setLogLevel(LogLevel.DEBUG); // Set log level for detailed output
        try {
            // Load the Vosk model
            model = new Model(voskModelPath);
        } catch (IOException e) {
            System.err.println("Failed to load Vosk model: " + e.getMessage());
        }
    }

    @PostMapping("/audio-to-text")
    public String convertAudioToText(@ModelAttribute AudioRequest audioRequest) throws UnsupportedAudioFileException {
        var audioFile = audioRequest.audioFile();

        if (audioFile.isEmpty()) {
            return "{\"text\": \"No audio file uploaded.\"}";
        }


        // Save the file temporarily to check if it is received correctly
        File tempFile;
        try {
            tempFile = new File("temp_" + audioFile.getOriginalFilename());
            audioFile.transferTo(tempFile);
            System.out.println("Audio file saved to: " + tempFile.getAbsolutePath());
        } catch (IOException e) {
            return "{\"text\": \"Error saving audio file: " + e.getMessage() + "\"}";
        }

        File convertedFile;
        try {
            convertedFile = convertTo16kHzWav(tempFile);
        } catch (IOException | InterruptedException e) {
            return "{\"text\": \"Error converting audio file: " + e.getMessage() + "\"}";
        }

        // Convert audio to text using Vosk
        String transcription = transcribeAudio(convertedFile);

        return transcription;
    }

    private String transcribeAudio(File audioFile) throws UnsupportedAudioFileException {
        StringBuilder result = new StringBuilder();

        try (InputStream audioStream = new BufferedInputStream(new FileInputStream(audioFile))) {
            // Get audio input stream
            AudioInputStream ais = AudioSystem.getAudioInputStream(audioStream);

            // Check if the audio format is supported
            if (!ais.getFormat().getEncoding().toString().equalsIgnoreCase("PCM_SIGNED") ||
                    ais.getFormat().getSampleRate() != 16000 ||
                    ais.getFormat().getChannels() != 1) {
                return "Unsupported audio format. Please upload a WAV file with PCM encoding, 16 kHz sample rate, and mono channel.";
            }

            // Create recognizer and process audio
            try (Recognizer recognizer = new Recognizer(model, 16000)) {
                int nbytes;
                byte[] buffer = new byte[4096];
                String lastPartial = "";

                while ((nbytes = ais.read(buffer)) >= 0) {
                    if (recognizer.acceptWaveForm(buffer, nbytes)) {
                        // Khi có kết quả chính thức, reset partial
                        lastPartial = "";
                        String jsonResult = recognizer.getResult();
                        String text = extractTextFromJson(jsonResult);
                        result.append(text);
                    } else {
                        String partialJson = recognizer.getPartialResult();
                        String partialText = extractTextFromJson(partialJson);
                        if (!partialText.isEmpty()) {
                            lastPartial = partialText;
                        }
                    }
                }

                // Nếu có partial cuối chưa được flush, thêm vào
                if (!lastPartial.isEmpty()) {
                    System.out.println("Appending last partial: " + lastPartial);
                    result.append(lastPartial).append(" ");
                }

                // Final result
                String finalJson = recognizer.getFinalResult();
                String finalText = extractTextFromJson(finalJson);
                result.append(finalText);
            }


        } catch (IOException | UnsupportedAudioFileException e) {
            return "Error processing audio file.";
        }

        return result.toString().trim(); // Return the transcription result
    }

    private File convertTo16kHzWav(File inputFile) throws IOException, InterruptedException {
        String baseName = inputFile.getName().replaceAll("\\.[^.]+$", ""); // bỏ phần mở rộng
        File outputFile = new File(inputFile.getParent(), "converted_" + baseName + ".wav");

        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-y",           // -y để ghi đè file output nếu tồn tại
                "-i", inputFile.getAbsolutePath(),
                "-ac", "1",
                "-ar", "16000",           // Sample rate 16kHz
                outputFile.getAbsolutePath()
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg failed with exit code " + exitCode);
        }

        return outputFile;
    }

    private String extractTextFromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.optString("text", "").trim();
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            return "";
        }
    }

}
