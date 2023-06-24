package dp.scsa;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import static dp.scsa.Tools.*;

/**
 * Класс, представляющий клиентскую часть приложения.
 * Клиент подключается к серверу, обменивается сертификатами и сообщениями.
 * Генерирует ключевую пару, запрос на сертификат (CSR) и отправляет их серверу.
 * Также верифицирует полученные сертификаты.
 */
public class Client {
    private static String rootCert;
    private static String intermediateCert;
    private static String leafCert;
    private static String leafCSR;
    private static String clientLogin;
    private static String clientMessage;

    /**
     * Конструктор класса Client.
     *
     * @param name имя клиента
     * @throws IOException если возникают проблемы при генерации ключевой пары и запроса на сертификат
     */
    public Client(String name) throws IOException {
        clientLogin = name;
        generateLeafKeyPair();
        generateLeafCSR();
    }

    /**
     * Отправляет запрос на сертификат (CSR) для листового сертификата на сервер.
     *
     * @param socket объект Socket для обмена данными с сервером
     * @throws IOException если возникают проблемы при отправке данных
     */
    private static void sendLeafCSR(Socket socket) throws IOException {
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.writeObject(leafCSR);
    }

    /**
     * Получает набор сертификатов для верификации от сервера.
     *
     * @param socket объект Socket для обмена данными с сервером
     * @throws IOException            если возникают проблемы при получении данных
     * @throws ClassNotFoundException если класс сертификатов не найден
     */
    private static void receiveCertPack(Socket socket) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        String[] certs = (String[]) inputStream.readObject();
        rootCert = certs[0];
        intermediateCert = certs[1];
        leafCert = certs[2];
    }

    public String getClientLogin() {
        return clientLogin;
    }

    /**
     * Устанавливает сообщение и хэш-значение для клиентского сообщения.
     *
     * @param filePath путь к файлу сообщения
     */
    public void setMessage(String filePath) {
        clientMessage = filePath;
    }

    /**
     * Создает листовой сертификат и верифицирует его.
     *
     * @param socket сокет для обмена данными
     * @throws IOException            если возникают ошибки ввода-вывода при взаимодействии с сокетом
     * @throws ClassNotFoundException если класс не найден при десериализации
     */
    public void createCertificate(Socket socket) throws IOException, ClassNotFoundException {
        sendLeafCSR(socket);
        receiveCertPack(socket);
        verifyLeafCert();
        socket.close();
    }

    /**
     * Устанавливает соединение с сервером.
     *
     * @param serverAddress IP-адрес сервера
     * @param serverPort    порт сервера
     * @return объект Socket для обмена данными с сервером
     */
    public Socket connectToServer(String serverAddress, int serverPort) {
        try {
            Socket socket = new Socket(serverAddress, serverPort);
            System.out.println("Подключено к серверу: " + socket.getInetAddress().getHostAddress());

            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(clientLogin);
            return socket;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Отправляет файлы клиенту Б, включая сообщение,
     * цифровую подпись и листовой сертификат.
     *
     * @param socket объект Socket для обмена данными с клиентом Б
     */
    public void sendFiles(Socket socket) {
        try {
            // Подготовка файлов для отправки
            File[] filesToSend = {new File(clientMessage), new File("leaf_cert.pem"), new File("signature.bin")};

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream()); // Инициализируем outputStream

            // Отправка количества файлов
            outputStream.writeInt(filesToSend.length);
            outputStream.flush();

            // Отправка файлов
            for (File file : filesToSend) {
                sendFile(socket, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет файл клиенту Б.
     *
     * @param socket объект Socket для обмена данными с клиентом Б
     * @param file   отправляемый файл
     */
    private void sendFile(Socket socket, File file) throws IOException {
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        // Отправка имени файла и размера
        outputStream.writeUTF(file.getName());
        outputStream.writeLong(file.length());
        outputStream.flush();

        // Отправка содержимого файла
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.flush();
        System.out.println("Файл успешно отправлен: " + file.getName());

        fileInputStream.close();
    }

    /**
     * Получает файлы клиента Б, включая сообщение,
     * цифровую подпись и листовой сертификат.
     *
     * @param socket     объект Socket для обмена данными с клиентом Б
     * @param clientName имя клиента Б
     */
    public void receiveFiles(Socket socket, String clientName) {
        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            // Получение количества файлов от клиента
            int fileCount = inputStream.readInt();
            System.out.println("Количество файлов для получения: " + fileCount);

            // Создание папки для сохранения файлов, если она не существует
            String folderName = "received_files_" + clientName;
            createFolder(folderName);

            // Получение файлов от клиента
            String[] temp;
            String sign = "", cert = "", file = "";
            for (int i = 0; i < fileCount; i++) {
                temp = receiveFile(inputStream, folderName);
                switch (temp[0]) {
                    case ("s") -> sign = temp[1];
                    case ("c") -> cert = temp[1];
                    default -> file = temp[1];
                }
            }

            verifySignature(sign, cert, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получает файл клиента Б.
     *
     * @param inputStream входной поток данных для чтения файла от клиента Б
     * @param folderName  имя папки, в которую будет сохранен файл
     * @throws IOException если возникают проблемы ввода-вывода при чтении или записи файла
     */
    private String[] receiveFile(DataInputStream inputStream, String folderName) throws IOException {
        // Создание объекта для форматирования даты и времени
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

        // Получение информации о файле
        String fileRole = inputStream.readUTF();
        String fileName = dateFormat.format(new Date()) + "_" + fileRole;
        long fileSize = inputStream.readLong();
        System.out.println("Получение файла: " + fileName + " (" + fileSize + " байт)");

        // Создание файлового потока для записи файла
        String filePath = folderName + "/" + fileName;
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);

        // Чтение и запись содержимого файла
        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalBytesRead = 0;

        while (totalBytesRead < fileSize) {
            int bytesToRead = (int) Math.min(buffer.length, fileSize - totalBytesRead);
            bytesRead = inputStream.read(buffer, 0, bytesToRead);
            if (bytesRead == -1) {
                break;
            }
            fileOutputStream.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
        }

        System.out.println("Файл успешно получен: " + fileName);

        fileOutputStream.close();

        return switch (fileRole) {
            case ("signature.bin") -> new String[]{"s", "\"" + filePath + "\""};
            case ("leaf_cert.pem") -> new String[]{"c", "\"" + filePath + "\""};
            default -> new String[]{"f", "\"" + filePath + "\""};
        };
    }

    /**
     * Генерирует пару ключей для листового сертификата.
     *
     * @throws IOException если возникают проблемы при генерации пары ключей
     */
    private void generateLeafKeyPair() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("openssl", "genpkey", "-algorithm", "RSA", "-out", "leaf_keypair.pem");
        executeCommand(builder);
    }

    /**
     * Генерирует запрос на сертификат (CSR) для листового сертификата.
     *
     * @throws IOException если возникают проблемы при создании CSR
     */
    private void generateLeafCSR() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("openssl", "req", "-new", "-subj", "\"/CN=Leaf\"", "-addext", "\"basicConstraints=critical,CA:FALSE\"", "-key", "leaf_keypair.pem", "-out", "leaf_csr.pem");
        executeCommand(builder);

        leafCSR = convertPEMFileToString("leaf_csr.pem");
    }

    /**
     * Проверяет листовой сертификат.
     *
     * @throws IOException если возникают проблемы при проверке сертификата
     */
    private void verifyLeafCert() throws IOException {
        convertStringToPEMFile(rootCert, "root_cert.pem");
        convertStringToPEMFile(intermediateCert, "intermediate_cert.pem");
        convertStringToPEMFile(leafCert, "leaf_cert.pem");

        // Проверка подписи листового сертификата
        // с использованием цепочки корневого и промежуточного сертификатов
        ProcessBuilder builder = new ProcessBuilder("openssl", "verify", "-verbose", "-show_chain", "-trusted", "root_cert.pem", "-untrusted", "intermediate_cert.pem", "leaf_cert.pem");
        executeReadableCommand(builder);
    }

    /**
     * Получает цифровую подпись документа.
     *
     * @throws IOException если возникают проблемы при получении цифровой подписи
     */
    public void createSignature() throws IOException {
        // Получение цифровой подписи
        ProcessBuilder builder = new ProcessBuilder("openssl", "dgst", "-sha512", "-sign", "leaf_keypair.pem", "-out", "signature.bin", "\"" + clientMessage + "\"");
        executeCommand(builder);
    }

    /**
     * Проверяет цифровую подпись полученного документа.
     *
     * @throws IOException если возникают проблемы при проверке цифровой подписи
     */
    private void verifySignature(String sign, String cert, String file) throws IOException {
        // Получение подписанного публичного ключа из листового сертификата
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "x509", "-in", cert, "-pubkey", "-noout", "-out", "public_key.pem");
        executeCommand(builder);

        // Получение цифровой подписи
        builder = new ProcessBuilder("openssl",
                "dgst", "-sha512", "-verify", "public_key.pem", "-signature", sign, file);
        executeReadableCommand(builder);

        (new File("public_key.pem")).delete();
    }
}
