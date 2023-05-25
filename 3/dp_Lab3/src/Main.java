import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    /**
     * Выполняет чтение изображения, извлечение заголовка, сохранение
     * заголовка и беззаголовочных данных, а также шифрование и сохранение
     * данных с использованием различных режимов шифрования.
     *
     * @param args Аргументы командной строки.
     */
    public static void main(String[] args) {
        // Имя входного файла изображения
        String inputFileName = "tux.bmp";
        byte[] imageData;
        byte[] header;
        int headerLength = 110;
        try {
            // Чтение всех байтов изображения
            imageData = Files.readAllBytes(Path.of(inputFileName));
            // Извлечение заголовка
            header = new byte[headerLength];
            System.arraycopy(imageData, 0, header, 0, headerLength);

            // Извлечение данных без заголовка
            byte[] imageDataWithoutHeader = new byte[imageData.length - headerLength];
            System.arraycopy(imageData, headerLength, imageDataWithoutHeader,
                    0, imageData.length - headerLength);

            // Сохранение заголовка в отдельный файл
            FileOutputStream outputStream = new FileOutputStream("tux_header.bin");
            outputStream.write(header);
            outputStream.close();

            // Сохранение данных без заголовка в отдельный файл
            FileOutputStream outputStreamWithoutHeader = new FileOutputStream("tux_without_header.bmp");
            outputStreamWithoutHeader.write(imageDataWithoutHeader);
            outputStreamWithoutHeader.close();

            // Шифрование и сохранение данных с использованием различных режимов шифрования
            encryptAndSaveData(header, "ecb");
            encryptAndSaveData(header, "cbc");
            encryptAndSaveData(header, "cfb");
            encryptAndSaveData(header, "ofb");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод encryptAndSaveData выполняет шифрование и сохранение данных.
     *
     * @param header         Заголовок данных.
     * @param encryptionMode Режим шифрования.
     * @throws IOException Если возникают проблемы с вводом-выводом.
     */
    public static void encryptAndSaveData(byte[] header, String encryptionMode) throws IOException {
        // Путь к файлу ключа шифрования
        String keyPath = "key.bin";
        // Путь к исходному файлу изображения без заголовка
        String tuxWithoutHeaderPath = "tux_without_header.bmp";
        // Путь к зашифрованному файлу изображения
        String tuxEncryptedPath = "tux_" + encryptionMode + ".bmp";

        // Генерация ключа шифрования
        ProcessBuilder keyGenProcessBuilder = new ProcessBuilder("openssl", "rand", "-hex", "16");
        keyGenProcessBuilder.redirectOutput(ProcessBuilder.Redirect.to(new File(keyPath)));
        Process keyGenProcess = keyGenProcessBuilder.start();
        try {
            keyGenProcess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Шифрование данных изображения без заголовка
        ProcessBuilder encryptionProcessBuilder = new ProcessBuilder("openssl", "enc",
                "-aes-256-" + encryptionMode,
                "-in", tuxWithoutHeaderPath,
                "-out", tuxEncryptedPath,
                "-pass", "file:key.bin");

        encryptionProcessBuilder.redirectErrorStream(true);
        Process encryptionProcess = encryptionProcessBuilder.start();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(encryptionProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            encryptionProcess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Чтение зашифрованных данных
        byte[] encryptedImageData = Files.readAllBytes(Path.of(tuxEncryptedPath));

        // Создание выходного файла и запись заголовка + зашифрованных данных
        FileOutputStream outputStream = new FileOutputStream(tuxEncryptedPath);
        outputStream.write(header);
        outputStream.write(encryptedImageData);
        outputStream.close();
    }
}
