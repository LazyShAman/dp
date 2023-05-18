import javax.crypto.*;
import java.io.*;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Программа, предназначенная для шифрования изображения
 * с использованием различных режимов шифрования AES.
 */
public class Main {
    public static void main(String[] args) {
        String inputFileName = "tux.bmp";
        byte[] imageData;
        byte[] header;
        int headerLength = 110;

        try {
            // Чтение входного файла изображения
            imageData = Files.readAllBytes(new File(inputFileName).toPath());
            header = new byte[headerLength];
            System.arraycopy(imageData, 0, header, 0, headerLength);

            // Разделение данных заголовка и изображения
            byte[] imageDataWithoutHeader = new byte[imageData.length - headerLength];
            System.arraycopy(imageData, headerLength, imageDataWithoutHeader, 0,
                        imageData.length - headerLength);

            // Сохранение изображения без заголовка в новый файл
            FileOutputStream outputStream = new FileOutputStream("tux_without_header.bmp");
            outputStream.write(imageDataWithoutHeader);
            outputStream.close();

            // Шифрование и сохранение данных изображения
            // с помощью различных режимов шифрования AES
            encryptAndSaveData(imageDataWithoutHeader, header, "ECB");
            encryptAndSaveData(imageDataWithoutHeader, header, "CBC");
            encryptAndSaveData(imageDataWithoutHeader, header, "CFB");
            encryptAndSaveData(imageDataWithoutHeader, header, "OFB");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод encryptAndSaveData шифрует и сохраняет данные изображения
     * с использованием указанного режима шифрования.
     *
     * @param imageData данные изображения без заголовка
     * @param header    заголовок изображения
     * @param encryptionMode режим шифрования (ECB, CBC, CFB или OFB)
     */
    public static void encryptAndSaveData(byte[] imageData, byte[] header, String encryptionMode) {
        try {
            // Генерация AES ключа
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            SecretKey secretKey = keyGenerator.generateKey();

            // Инициализация шифра с ключом AES и режимом шифрования
            Cipher cipher = Cipher.getInstance("AES/" + encryptionMode + "/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Шифрование данных изображения
            byte[] encryptedData = cipher.doFinal(imageData);

            // Объединение заголовка и зашифрованных данных изображения
            byte[] encryptedImage = new byte[header.length + encryptedData.length];
            System.arraycopy(header, 0, encryptedImage, 0, header.length);
            System.arraycopy(encryptedData, 0, encryptedImage, header.length, encryptedData.length);

            // Сохранение зашифрованного изображения в новый файл
            String outputFileName = "tux" + encryptionMode + "_encrypted.bmp";
            FileOutputStream outputStream = new FileOutputStream(outputFileName);
            outputStream.write(encryptedImage);
            outputStream.close();

            System.out.println("Image encrypted and saved: " + outputFileName);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException | IOException e) {
            e.printStackTrace();
        }
    }
}
