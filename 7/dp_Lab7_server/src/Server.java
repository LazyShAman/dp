import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

/**
 * Класс, представляющий серверную часть приложения.
 * Сервер принимает подключение клиентов и обрабатывает их запросы.
 * Также сервер генерирует ключевые пары и сертификаты для корневого и промежуточного Удостоверяющих Центров (УЦ),
 * а также подписывает листовые сертификаты клиентов.
 */
public class Server {
    private static final int PORT = 8888;
    private final HashMap<String, Socket> connectedClients;
    private static String rootCert;
    private static String intermediateCert;

    /**
     * Конструктор класса Server.
     *
     * @throws IOException если возникают проблемы при генерации ключевой пары и сертификатов
     */
    public Server() throws IOException {
        connectedClients = new HashMap<>();
        generateRootKeyPair();
        generateRootCSR();
        releaseSignedRootCert();
    }

    /**
     * Запускает сервер на указанном порту.
     *
     * @param serverPort порт сервера
     */
    public void start(int serverPort) {
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Сервер запущен. Ожидание подключения клиентов...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Подключено клиент: " + socket.getInetAddress().getHostAddress());

                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                String clientName = (String) inputStream.readObject();

                connectedClients.put(clientName, socket);

                // Создание и запуск нового потока для обработки клиента
                Thread clientThread = new Thread(new ClientHandler(socket, clientName));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Внутренний класс для обработки клиентов в отдельных потоках.
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final String clientName;

        public ClientHandler(Socket socket, String clientName) {
            this.socket = socket;
            this.clientName = clientName;
        }

        @Override
        public void run() {
            // Обработка подключенного клиента
            try {
                handleClient(socket, clientName);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Обрабатывает клиента и выполняет необходимые операции с сертификатами.
     *
     * @param socket     объект Socket для обмена данными с клиентом
     * @param clientName имя клиента
     * @throws IOException            если возникают проблемы при обмене данными
     * @throws ClassNotFoundException если класс сертификатов не найден
     */
    private void handleClient(Socket socket, String clientName) throws IOException, ClassNotFoundException {
        int clientHash = clientName.hashCode();

        generateIntermediateKeyPair();
        generateIntermediateCSR();
        releaseSignedIntermediateCert();

        String leafCSR = receiveLeafCSR(socket);
        String leafCert = releaseSignedLeafCert(leafCSR, clientHash);
        sendCertPack(socket, leafCert);

        // Закрытие соединения после завершения обработки клиента
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает сертификат (CSR) для листового сертификата от клиента.
     *
     * @param socket объект Socket для обмена данными с клиентом
     * @return сертификат (CSR) в виде строки
     * @throws IOException            если возникают проблемы при обмене данными
     * @throws ClassNotFoundException если класс сертификата не найден
     */
    private String receiveLeafCSR(Socket socket) throws IOException, ClassNotFoundException {
        // Получение сертификата (CSR) для листового сертификата
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        return (String) inputStream.readObject();
    }

    /**
     * Отправляет набор сертификатов клиенту для верификации.
     *
     * @param socket    объект Socket для обмена данными с клиентом
     * @param leafCert  сертификат листового узла
     * @throws IOException если возникают проблемы при обмене данными
     */
    private void sendCertPack(Socket socket, String leafCert) throws IOException {
        // Отправка набора сертификатов клиенту для верификации
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        String[] certs = new String[3];
        certs[0] = rootCert;
        certs[1] = intermediateCert;
        certs[2] = leafCert;
        outputStream.writeObject(certs);
    }

    /**
     * Генерирует пару ключей для корневого УЦ.
     *
     * @throws IOException если возникают проблемы при выполнении команды
     */
    private static void generateRootKeyPair() throws IOException {
        // Генерация ключевой пары корневого УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "genpkey", "-algorithm", "ED448", "-out", "root_keypair.pem");
        executeCommand(builder);
    }

    /**
     * Создает запрос на сертификат (CSR) для корневого УЦ.
     *
     * @throws IOException если возникают проблемы при выполнении команды
     */
    private static void generateRootCSR() throws IOException {
        // Создание запроса на сертификат (CSR) для корневого УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "req", "-new", "-subj", "\"/CN=Root CA\"", "-addext",
                "\"basicConstraints=critical,CA:TRUE\"", "-key",
                "root_keypair.pem", "-out", "root_csr.pem");
        executeCommand(builder);
    }

    /**
     * Подписывает сертификат корневым УЦ.
     *
     * @throws IOException если возникают проблемы при выполнении команды
     */
    private static void releaseSignedRootCert() throws IOException {
        // Подписание сертификата корневым УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "x509", "-req", "-in", "root_csr.pem", "-copy_extensions",
                "copyall", "-key", "root_keypair.pem", "-days",
                "3650", "-out", "root_cert.pem");
        executeCommand(builder);

        rootCert = convertPEMFileToString("root_cert.pem");
    }

    /**
     * Генерирует пару ключей для промежуточного УЦ.
     *
     * @throws IOException если возникают проблемы при выполнении команды
     */
    private static void generateIntermediateKeyPair() throws IOException {
        // Генерация ключевой пары промежуточного УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "genpkey", "-algorithm", "ED448", "-out", "intermediate_keypair.pem");
        executeCommand(builder);
    }

    /**
     * Создает запрос на сертификат (CSR) для промежуточного УЦ.
     *
     * @throws IOException если возникают проблемы при выполнении команды
     */
    private static void generateIntermediateCSR() throws IOException {
        // Создание запроса на сертификат (CSR) для промежуточного УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "req", "-new", "-subj", "\"/CN=Intermediate CA\"", "-addext",
                "\"basicConstraints=critical,CA:TRUE\"", "-key",
                "intermediate_keypair.pem", "-out", "intermediate_csr.pem");
        executeCommand(builder);
    }

    /**
     * Подписывает промежуточный сертификат корневым УЦ.
     *
     * @throws IOException если возникают проблемы при выполнении команды
     */
    private static void releaseSignedIntermediateCert() throws IOException {
        // Подписание промежуточного сертификата корневым УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "x509", "-req", "-in", "intermediate_csr.pem", "-copy_extensions",
                "copyall", "-CA", "root_cert.pem", "-CAkey", "root_keypair.pem",
                "-days", "3650", "-out", "intermediate_cert.pem");
        executeCommand(builder);

        intermediateCert = convertPEMFileToString("intermediate_cert.pem");
    }

    /**
     * Подписывает листовой сертификат промежуточным УЦ.
     *
     * @param leafCSR CSR (Certificate Signing Request) листового сертификата
     * @param clientHash хеш клиента
     * @return подписанный листовой сертификат в виде строки
     * @throws IOException если возникают проблемы при выполнении команды
     */
    private static String releaseSignedLeafCert(String leafCSR, int clientHash) throws IOException {
        String leafCSRFile = "leaf_csr_" + clientHash + ".pem";
        convertStringToPEMFile(leafCSR, leafCSRFile);
        String leafCertFile = "leaf_cert_" + clientHash + ".pem";

        // Подписание листового сертификата промежуточным УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "x509", "-req", "-in", leafCSRFile, "-copy_extensions",
                "copyall", "-CA", "intermediate_cert.pem", "-CAkey",
                "intermediate_keypair.pem", "-days", "3650",
                "-out", leafCertFile);
        executeCommand(builder);

        return convertPEMFileToString(leafCertFile);
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

    /**
     * Точка входа в приложение сервера.
     *
     * @param args аргументы командной строки (не используются)
     * @throws IOException если возникают проблемы при запуске сервера
     */
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start(PORT);
    }
}