package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentLoop {

    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    private static final ObjectMapper mapper = new ObjectMapper();

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
        if (API_KEY == null) {
            System.err.println("Bitte setze die Umgebungsvariable OPENAI_API_KEY!");
            return;
        }

        OpenAiService service = new OpenAiService(API_KEY, Duration.ofSeconds(60));

        String aufgabe = "Erstelle eine Datei 'Main.java', die 'Hallo Welt aus Java' ausgibt. Kompiliere sie und führe sie aus.";

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), SYSTEM_PROMPT));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), "Aufgabe: " + aufgabe));

        System.out.println("Starte Agent...\n");

        int steps = 0;
        while (steps < 15) {
            steps++;

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-4o")
                    .messages(messages)
                    .responseFormat(new ChatCompletionRequest.ChatCompletionRequestResponseFormat("json_object"))
                    .build();

            ChatMessage responseMessage = service.createChatCompletion(request).getChoices().get(0).getMessage();
            String content = responseMessage.getContent();

            try {
                AgentAction actionData = mapper.readValue(content, AgentAction.class);

                System.out.println("\n[Gedanke]: " + actionData.thought);

                if ("finish".equals(actionData.action)) {
                    System.out.println("Fertig!");
                    break;
                }

                String toolResult;
                switch (actionData.action) {
                    case "run_shell" -> toolResult = runShell(actionData.args.get("command"));
                    case "read_file" -> toolResult = readFile(actionData.args.get("path"));
                    case "write_file" -> toolResult = writeFile(actionData.args.get("path"), actionData.args.get("content"));
                    default -> toolResult = "Fehler: Unbekanntes Tool " + actionData.action;
                }

                String displayResult = toolResult.length() > 100 ? toolResult.substring(0, 100) + "..." : toolResult;
                System.out.println("   [Ergebnis]: " + displayResult);

                messages.add(responseMessage);
                messages.add(new ChatMessage(ChatMessageRole.USER.value(), "Tool Output: " + toolResult));
            } catch (Exception e) {
                System.err.println("JSON Parsing Fehler oder Ausnahmefehler: " + e.getMessage());
                System.err.println("Raw Content: " + content);
            }
        }

        service.shutdownExecutor();
    }
}
