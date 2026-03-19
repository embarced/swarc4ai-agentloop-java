package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class AgentLoop implements CommandLineRunner {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final ChatClient chatClient;

    public AgentLoop(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    public static String runShell(String command) {
        System.out.println("Führe Shell aus: " + command);
        try {
            ProcessBuilder builder = new ProcessBuilder();
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            if (isWindows) {
                builder.command("cmd.exe", "/c", command);
            } else {
                builder.command("sh", "-c", command);
            }

            Process process = builder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            return output.toString().trim().isEmpty() ? "Befehl ausgeführt (kein Output)" : output.toString();
        } catch (Exception e) {
            return "Fehler: " + e.getMessage();
        }
    }

    public static String readFile(String path) {
        System.out.println("Lese Datei: " + path);
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            return "Fehler: " + e.getMessage();
        }
    }

    public static String writeFile(String path, String content) {
        System.out.println("Schreibe Datei: " + path);
        try {
            Files.writeString(Path.of(path), content);
            return "Erfolg: Datei gespeichert.";
        } catch (IOException e) {
            return "Fehler: " + e.getMessage();
        }
    }

    static class AgentAction {
        public String thought;
        public String action;
        public Map<String, String> args;
    }

    private static final String SYSTEM_PROMPT = """
            Du bist ein autonomer Java Coding-Agent.
            Antworte IMMER im rohen JSON-Format. Kein Markdown!

            Struktur:
            {
              "thought": "Überlegung...",
              "action": "run_shell" | "read_file" | "write_file" | "finish",
              "args": { "command": "...", "path": "...", "content": "..." }
            }

            Tools:
            1. run_shell(command): Führt Befehle aus.
            2. write_file(path, content): Erstellt Dateien.
            3. read_file(path): Liest Dateien.
            4. finish(): Wenn Aufgabe erledigt.
            """;

    public static void main(String[] args) {
        SpringApplication.run(AgentLoop.class, args);
    }

    @Override
    public void run(String... args) {
        String aufgabe = "Erstelle eine Datei 'Main.java', die 'Hallo Welt aus Java' ausgibt. Kompiliere sie und führe sie aus.";

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        messages.add(new UserMessage("Aufgabe: " + aufgabe));

        System.out.println("Starte Agent...\n");

        int steps = 0;
        while (steps < 15) {
            steps++;

            // TODO: ÜBUNG 1 - Erstelle ein Prompt-Objekt mit der bisherigen Message-History und den OpenAiChatOptions.
            // TODO: ÜBUNG 1 - Konfiguriere das Modell gpt-4o, temperature 0.1 und JSON_OBJECT als ResponseFormat.
            // TODO: ÜBUNG 1 - Rufe das LLM über den ChatClient auf und speichere die Antwort als AssistantMessage.
            // TODO: ÜBUNG 1 - Lies den Content aus der LLM-Antwort aus.
            // TODO: ÜBUNG 1 - Parse den JSON-String mit Jackson in ein AgentAction-Objekt.
            // TODO: ÜBUNG 1 - Beende den Loop, wenn die Action 'finish' ist.
            // TODO: ÜBUNG 1 - Füge die Assistant-Antwort und das Tool-Ergebnis wieder zur Message-History hinzu.
            // SO PRÜFST DU ES: Starte das Programm. Der Agent sollte mehrere Schritte ausführen, eine Datei
            // 'Main.java' erzeugen, sie kompilieren und am Ende 'Hallo Welt aus Java' ausgeben.
            throw new UnsupportedOperationException("ÜBUNG 1: Implementiere den Spring-AI-Aufruf und das JSON-Parsing.");
        }
    }
}
