import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;

public class Main {
    public static void main(String[] args) throws Exception {
        // Загрузка изображения
        byte[] imageBytes = loadImage("tux.png");

        // Генерация ключа
        SecretKey key = generateKey();

        // Шифрование и сохранение изображения в режиме ECB
        byte[] encryptedECB = encryptECB(imageBytes, key);
        saveImage(encryptedECB, "tux_ecb.png");

        // Шифрование и сохранение изображения в режиме CBC
        byte[] encryptedCBC = encryptInMode(imageBytes, key, "CBC");
        saveImage(encryptedCBC, "tux_cbc.png");

        // Шифрование и сохранение изображения в режиме CFB
        byte[] encryptedCFB = encryptInMode(imageBytes, key, "CFB");
        saveImage(encryptedCFB, "tux_cfb.png");

        // Шифрование и сохранение изображения в режиме OFB
        byte[] encryptedOFB = encryptInMode(imageBytes, key, "OFB");
        saveImage(encryptedOFB, "tux_ofb.png");
    }

    /**
     * Генерирует ключ для использования в алгоритме AES.
     *
     * @return сгенерированный секретный ключ
     * @throws Exception если произошла ошибка при генерации ключа
     */
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128); // Используем ключ длиной 128 бит
        return keyGenerator.generateKey();
    }

    /**
     * Загружает изображение из файла и возвращает его в виде массива байтов.
     *
     * @param imagePath путь к файлу изображения
     * @return массив байтов, представляющих изображение
     * @throws Exception если произошла ошибка при загрузке изображения
     */
    public static byte[] loadImage(String imagePath) throws Exception {
        FileInputStream fis = new FileInputStream(imagePath);
        byte[] imageBytes = new byte[fis.available()];
        fis.read(imageBytes);
        fis.close();
        return imageBytes;
    }

    /**
     * Сохраняет массив байтов в виде изображения в указанный файл.
     *
     * @param imageBytes массив байтов, представляющих изображение
     * @param imagePath  путь к файлу для сохранения изображения
     * @throws Exception если произошла ошибка при сохранении изображения
     */
    public static void saveImage(byte[] imageBytes, String imagePath) throws Exception {
        FileOutputStream fos = new FileOutputStream(imagePath);
        fos.write(imageBytes);
        fos.close();
    }

    /**
     * Шифрует изображение в режиме ECB (Electronic Codebook) с использованием указанного ключа.
     * В режиме ECB не используется вектор инициализации (IV).
     *
     * @param imageBytes массив байтов, представляющих изображение
     * @param key        секретный ключ для шифрования
     * @return зашифрованный массив байтов
     * @throws Exception если произошла ошибка при шифровании
     */
    public static byte[] encryptECB(byte[] imageBytes, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(imageBytes);
    }

    /**
     * Шифрует изображение в выбранном режиме шифрования (CBC/CFB/OFB) с использованием указанного ключа.
     * В режимах CBC, CFB и OFB используется вектор инициализации (IV).
     *
     * @param imageBytes массив байтов, представляющих изображение
     * @param key        секретный ключ для шифрования
     * @param mode       режим шифрования (CBC/CFB/OFB)
     * @return зашифрованный массив байтов
     * @throws Exception если произошла ошибка при шифровании
     */
    public static byte[] encryptInMode(byte[] imageBytes, SecretKey key, String mode) throws Exception {
        if (mode.equalsIgnoreCase("ECB")) {
            return encryptECB(imageBytes, key);
        }

        Cipher cipher = Cipher.getInstance("AES/" + mode + "/PKCS5Padding");
        byte[] iv = generateIV(cipher.getBlockSize());
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);
        return cipher.doFinal(imageBytes);
    }

    /**
     * Генерирует случайный вектор инициализации (IV) заданного размера.
     *
     * @param blockSize размер блока шифрования
     * @return сгенерированный вектор инициализации
     */
    public static byte[] generateIV(int blockSize) {
        byte[] iv = new byte[blockSize];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
