import java.io.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Scanner;

public class Main {
    static int keyLength = 20;
    static int imageOffset = 54; // BMP header size is 54 bytes

    public static void main(String[] args) throws IOException {
        int[] key = generateKey();
        saveKey(key, "key.txt");

        String hexString = generateSHA1();
        // Создает из строки хэш-кода файла leasing.txt последовательность байтов
        assert hexString != null;
        ByteBuffer buffer = ByteBuffer.allocate(hexString.length() / 2);
        for (int i = 0; i < hexString.length(); i += 2) {
            buffer.put((byte) Integer.parseInt(hexString.substring(i, i + 2), 16));
        }
        byte[] bytes = buffer.array();


        encode(bytes, key);
        key = loadKey("key.txt");
        decode(key);
    }

    public static void encode(byte[] message, int[] key) {
        File bmpFile = new File("input.bmp");
        byte[] imageBytes = new byte[(int) bmpFile.length()];

        try {
            FileInputStream fis = new FileInputStream(bmpFile);
            fis.read(imageBytes);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        byte[] messageBytes = new byte[keyLength];

        for (int i = 0; i < message.length && i < keyLength; i++) {
            messageBytes[i] = message[i];
        }

        // Кодирует сообщение в массив байтов изображения, используя LSB стеганографию
        int messageOffset = 0;
        while (messageOffset < messageBytes.length) {
            byte messageByte = messageBytes[messageOffset];
            encodeByte(key[messageOffset], imageBytes, messageByte);
            messageOffset++;
        }

        // Сохраняет модифицированное BMP-изображение в новый файл
        try {
            FileOutputStream fos = new FileOutputStream("image_with_secret.bmp");
            fos.write(imageBytes);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("Secret message successfully hidden in BMP image!");
    }

    public static void decode(int[] key) {
        File bmpFile = new File("image_with_secret.bmp");
        byte[] imageBytes = new byte[(int) bmpFile.length()];

        try {
            FileInputStream fis = new FileInputStream(bmpFile);
            fis.read(imageBytes);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Декодирует ключ из изображения, используя LSB стеганографию
        int messageOffset = 0;
        byte[] messageBytes = new byte[keyLength];

        while (messageOffset < messageBytes.length) {
            byte messageByte = decodeByte(key[messageOffset], imageBytes);
            messageBytes[messageOffset] = messageByte;
            messageOffset++;
        }

        // Переводит из последовательности байтов в строку
        ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
        StringBuilder hexString = new StringBuilder();
        while (buffer.hasRemaining()) {
            hexString.append(String.format("%02x", buffer.get() & 0xff));
        }

        System.out.println("Secret message extracted from BMP image: " + hexString);
    }

    public static boolean contains(int[] arr, int val) {
        for (int j : arr) {
            if (j == val) {
                return true;
            }
        }
        return false;
    }

    public static void saveKey(int[] arr, String filename) {
        // Сохраняет полученный ключ в файл
        try (PrintWriter writer = new PrintWriter(new File(filename))) {
            for (int i = 0; i < keyLength; i++) {
                writer.println(arr[i]);
            }
            System.out.println("Array saved to " + filename);
        } catch (FileNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static int[] loadKey(String filename) {
        try (Scanner scanner = new Scanner(new File(filename))) {
            int[] loadedArr = new int[keyLength];
            int i = 0;
            while (scanner.hasNextInt()) {
                loadedArr[i++] = scanner.nextInt();
            }
            System.out.println("Array loaded from " + filename);
            return loadedArr;
        } catch (FileNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    public static int[] generateKey() {
        int max = 400;
        int size = keyLength;
        int[] key = new int[size];
        Random rand = new Random();

        // Генерирует последовательность уникальных случайных чисел
        for (int i = 0; i < size; i++) {
            int randInt = rand.nextInt(max) + 1;
            while (contains(key, randInt)) {
                randInt = rand.nextInt(max) + 1;
            }
            key[i] = randInt;
        }
        return key;
    }

    public static void encodeByte(int offset, byte[] imageBytes, byte messageByte) {
        for (int i = 0; i < 8; i++) {
            int bit = (messageByte >>> i) & 1;
            int imageByteIndex = imageOffset + (offset * 8) + i;
            imageBytes[imageByteIndex] = (byte) ((imageBytes[imageByteIndex] & 0xFE) | bit);
        }
    }

    public static byte decodeByte(int offset, byte[] imageBytes) {
        byte messageByte = 0;
        for (int i = 0; i < 8; i++) {
            int imageByteIndex = imageOffset + (offset * 8) + i;
            int bit = imageBytes[imageByteIndex] & 1;
            messageByte |= bit << i;
        }
        return messageByte;
    }

    protected static String generateSHA1() {
        try {
            ProcessBuilder builder = new ProcessBuilder("openssl", "dgst", "-sha1", "leasing.txt");

            // Сообщает процессу объединять стандартный вывод и стандартный поток ошибок в один поток,
            // который мы можем прочитать в Java
            builder.redirectErrorStream(true);
            Process process = builder.start();

            // Для чтения вывода процесса
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            // Читает каждую строку вывода процесса
            while ((line = reader.readLine()) != null) {
                return line.substring(line.indexOf("= ") + 2);
            }

            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
