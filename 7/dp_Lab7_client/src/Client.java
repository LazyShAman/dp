import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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
    byte[] message;

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
     * Отправляет запрос на сертификат (CSR) для листового сертификата на сервер.
     *
     * @param socket объект Socket для обмена данными с сервером
     * @throws IOException если возникают проблемы при отправке данных
     */
    public void sendLeafCSR(Socket socket) throws IOException {
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
    public void receiveCertPack(Socket socket) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        String[] certs = (String[]) inputStream.readObject();
        rootCert = certs[0];
        intermediateCert = certs[1];
        leafCert = certs[2];
    }

    /**
     * Генерирует пару ключей для листового сертификата.
     *
     * @throws IOException если возникают проблемы при генерации пары ключей
     */
    private static void generateLeafKeyPair() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "genpkey", "-algorithm", "ED448", "-out", "leaf_keypair.pem");
        executeCommand(builder);
    }

    /**
     * Генерирует запрос на сертификат (CSR) для листового сертификата.
     *
     * @throws IOException если возникают проблемы при создании CSR
     */
    private static void generateLeafCSR() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "req", "-new", "-subj", "\"/CN=Leaf\"", "-addext",
                "\"basicConstraints=critical,CA:FALSE\"", "-key",
                "leaf_keypair.pem", "-out", "leaf_csr.pem");
        executeCommand(builder);

        leafCSR = convertPEMFileToString("leaf_csr.pem");
    }

    /**
     * Проверяет листовой сертификат.
     *
     * @throws IOException если возникают проблемы при проверке сертификата
     */
    protected void verifyLeafCert() throws IOException {
        convertStringToPEMFile(rootCert, "root_cert.pem");
        convertStringToPEMFile(intermediateCert, "intermediate_cert.pem");
        convertStringToPEMFile(leafCert, "leaf_cert.pem");

        // Проверка подписи листового сертификата
        // с использованием цепочки корневого и промежуточного сертификатов
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "verify", "-verbose", "-show_chain", "-trusted", "root_cert.pem",
                "-untrusted", "intermediate_cert.pem", "leaf_cert.pem");
        executeCommand(builder);
    }

    /**
     * Выполняет openssl команду в системной оболочке.
     *
     * @param processBuilder объект ProcessBuilder для выполнения команды
     * @throws IOException если возникают проблемы при выполнении команды
     */
    private static void executeCommand(ProcessBuilder processBuilder) throws IOException {
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
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
    private static String convertPEMFileToString(String filePath) {
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
    private static void convertStringToPEMFile(String pemString, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(pemString);
        } catch (IOException e) {
            System.err.println("Ошибка при записи в файл PEM: " + e.getMessage());
        }
    }
}
