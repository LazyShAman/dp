package dp.scsa;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Класс, представляющий набор вспомогательных инструментов.
 */
public class Tools {
    /**
     * Выполняет openssl команду в системной оболочке.
     *
     * @param processBuilder объект ProcessBuilder для выполнения команды
     * @throws IOException если возникают проблемы при выполнении команды
     */
    public static void executeCommand(ProcessBuilder processBuilder) throws IOException {
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Выполняет openssl команду в системной оболочке
     * и выводит содержимое консоли при выполнении.
     *
     * @param processBuilder объект ProcessBuilder для выполнения команды
     * @throws IOException если возникают проблемы при выполнении команды
     */
    public static void executeReadableCommand(ProcessBuilder processBuilder) throws IOException {
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Создаем буфер для чтения вывода из консоли OpenSSL
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        // Читаем и выводим каждую строку из консоли OpenSSL в консоль IntelliJ IDEA
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Преобразует содержимое файла PEM в строку.
     *
     * @param filePath путь к файлу PEM
     * @return содержимое файла PEM в виде строки
     */
    public static String convertPEMFileToString(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            return String.join("\n", lines);
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла PEM: " + e.getMessage());
        }
        return "";
    }

    /**
     * Записывает строку в файл PEM.
     *
     * @param pemString содержимое в формате PEM
     * @param filePath  путь к файлу PEM
     */
    public static void convertStringToPEMFile(String pemString, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(pemString);
        } catch (IOException e) {
            System.err.println("Ошибка при записи в файл PEM: " + e.getMessage());
        }
    }

    /**
     * Создает папку с указанным именем.
     *
     * @param folderName имя папки
     */
    public static void createFolder(String folderName) {
        File folder = new File(folderName);
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    /**
     * Выбирает файл с помощью диалогового окна JFileChooser.
     * Позволяет пользователю выбрать текстовые файлы с расширениями: txt, pdf, docx, rtf, html, xml, json, csv.
     *
     * @return Путь к выбранному файлу в виде строки. Возвращает null, если файл не был выбран.
     */
    public static String chooseFile() {
        System.out.println("\nВыберите файл для отправки");

        // Создаем экземпляр JFileChooser
        JFileChooser fileChooser = new JFileChooser();

        // Определяем фильтр файлов, если нужно
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Текстовые файлы",
                "txt", "pdf", "docx", "rtf", "html", "xml", "json", "csv");
        fileChooser.setFileFilter(filter);

        // Задаем изначальную директорию
        String currentDirectory = System.getProperty("user.dir"); // Директория текущего проекта
        fileChooser.setCurrentDirectory(new java.io.File(currentDirectory));

        // Открываем проводник для выбора файла
        int result = fileChooser.showOpenDialog(null);

        // Проверяем, был ли выбран файл
        if (result == JFileChooser.APPROVE_OPTION) {
            // Получаем выбранный файл
            java.io.File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getPath();
            System.out.println("Выбранный файл: " + filePath);

            return filePath;
        } else {
            System.out.println("Файл не выбран");
        }
        return null;
    }
}
