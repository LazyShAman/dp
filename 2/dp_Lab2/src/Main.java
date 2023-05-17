import java.io.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Scanner;

public class Main {
    static int keyLength = 20; // Длина ключа шифрования
    static int imageOffset = 54; // Размер хедера BMP-изображения составляет 54 байта

    public static void main(String[] args) {
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

    /**
     * Метод encode внедряет в BMP-изображение хэшкод файла leasing.txt
     * с помощью реализации LSB Replacement
     * @param message сообщение, которое необходимо закодировать
     * @param key ключ шифрования
     */
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

    /**
     * Метод decode извлекает из BMP-изображения хэшкод файла leasing.txt
     * с помощью реализации LSB Replacement
     * @param key ключ шифрования
     */
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

    /**
     * Метод contains проверяет наличие передаваемого числа в указанном массиве
     * @param arr массив чисел
     * @param val искомое число
     * @return true/false в зависимости от результатов поиска
     */
    public static boolean contains(int[] arr, int val) {
        for (int j : arr) {
            if (j == val) {
                return true;
            }
        }
        return false;
    }

    /**
     * Метод generateKey генерирует рандомные числа на отрезке [1; max]
     * в количестве keyLength для формирования ключа шифрования
     * @return ключ шифрования в виде массива чисел
     */
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

    /**
     * Метод saveKey сохраняет полученный ключ шифрования в файл
     * @param arr ключ шифрования
     * @param filename имя создаваемого файла, в который будет сохранен ключ
     */
    public static void saveKey(int[] arr, String filename) {
        try (PrintWriter writer = new PrintWriter(filename)) {
            for (int i = 0; i < keyLength; i++) {
                writer.println(arr[i]);
            }
            System.out.println("Array saved to " + filename);
        } catch (FileNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Метод loadKey извлекает из файла записанный ключ шифрования
     * @param filename имя файла, в котором был сохранен ключ
     * @return ключ шифрования
     */
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

    /**
     * Метод encodeByte внедряет в байтовое представление BMP-изображения байт
     * шифруемого сообщения, меняя наименьший значащий бит каждого байта пикселей
     * @param offset позиция шифрования
     * @param imageBytes массив байтов BMP-изображения
     * @param messageByte байт шифруемого сообщения
     */
    public static void encodeByte(int offset, byte[] imageBytes, byte messageByte) {
        for (int i = 0; i < 8; i++) {
            int bit = (messageByte >>> i) & 1;
            int imageByteIndex = imageOffset + (offset * 8) + i;
            imageBytes[imageByteIndex] = (byte) ((imageBytes[imageByteIndex] & 0xFE) | bit);
        }
    }

    /**
     * Метод decodeByte извлекает из байтового представления BMP-изображения байт
     * зашифрованного сообщения
     * @param offset позиция шифрования
     * @param imageBytes массив байтов BMP-изображения
     * @return байт шифруемого сообщения
     */
    public static byte decodeByte(int offset, byte[] imageBytes) {
        byte messageByte = 0;
        for (int i = 0; i < 8; i++) {
            int imageByteIndex = imageOffset + (offset * 8) + i;
            int bit = imageBytes[imageByteIndex] & 1;
            messageByte |= bit << i;
        }
        return messageByte;
    }

    /**
     * Метод generateSHA1 генерирует хэшкод SHA1 (в данном случае)
     * для файла leasing.txt
     * @return строку, содержащую хэшкод указанного файла
     */
    protected static String generateSHA1() {
        try {
            ProcessBuilder builder = new ProcessBuilder("openssl", "dgst", "-sha1", "leasing.txt");

            // Сообщает процессу объединять стандартный вывод и стандартный поток ошибок в один поток,
            // который мы можем прочитать в Java
            builder.redirectErrorStream(true);
            Process process = builder.start();

            // Для чтения вывода процесса
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Читает строку вывода процесса
            String line = reader.readLine();
            if (line != null) {
                return line.substring(line.indexOf("= ") + 2);
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
