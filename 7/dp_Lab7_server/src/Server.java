import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class Server {
    private static final int PORT = 8888;
    private final HashMap<String, Socket> connectedClients;
    private static String rootCert;
    private static String intermediateCert;

    public Server() throws IOException {
        connectedClients = new HashMap<>();
        generateRootKeyPair();
        generateRootCSR();
        releaseSignedRootCert();
    }

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


    private String receiveLeafCSR(Socket socket) throws IOException, ClassNotFoundException {
        // Получение сертификата (CSR) для листового сертификата
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        return (String) inputStream.readObject();
    }

    private void sendCertPack(Socket socket, String leafCert) throws IOException {
        // Отправка набора сертификатов клиенту для верификации
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        String[] certs = new String[3];
        certs[0] = rootCert;
        certs[1] = intermediateCert;
        certs[2] = leafCert;
        outputStream.writeObject(certs);
    }

    private static void generateRootKeyPair() throws IOException {
        // Генерация ключевой пары корневого УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "genpkey", "-algorithm", "ED448", "-out", "root_keypair.pem");
        executeCommand(builder);
    }

    private static void generateRootCSR() throws IOException {
        // Создание запроса на сертификат (CSR) для корневого УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "req", "-new", "-subj", "\"/CN=Root CA\"", "-addext",
                "\"basicConstraints=critical,CA:TRUE\"", "-key",
                "root_keypair.pem", "-out", "root_csr.pem");
        executeCommand(builder);
    }

    private static void releaseSignedRootCert() throws IOException {
        // Подписание сертификата корневым УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "x509", "-req", "-in", "root_csr.pem", "-copy_extensions",
                "copyall", "-key", "root_keypair.pem", "-days",
                "3650", "-out", "root_cert.pem");
        executeCommand(builder);

        rootCert = convertPEMFileToString("root_cert.pem");
    }

    private static void generateIntermediateKeyPair() throws IOException {
        // Генерация ключевой пары промежуточного УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "genpkey", "-algorithm", "ED448", "-out", "intermediate_keypair.pem");
        executeCommand(builder);
    }

    private static void generateIntermediateCSR() throws IOException {
        // Создание запроса на сертификат (CSR) для промежуточного УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "req", "-new", "-subj", "\"/CN=Intermediate CA\"", "-addext",
                "\"basicConstraints=critical,CA:TRUE\"", "-key",
                "intermediate_keypair.pem", "-out", "intermediate_csr.pem");
        executeCommand(builder);
    }

    private static void releaseSignedIntermediateCert() throws IOException {
        // Подписание промежуточного сертификата корневым УЦ
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "x509", "-req", "-in", "intermediate_csr.pem", "-copy_extensions",
                "copyall", "-CA", "root_cert.pem", "-CAkey", "root_keypair.pem",
                "-days", "3650", "-out", "intermediate_cert.pem");
        executeCommand(builder);

        intermediateCert = convertPEMFileToString("intermediate_cert.pem");
    }

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

    private static void executeCommand(ProcessBuilder processBuilder) throws IOException {
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String convertPEMFileToString(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            return String.join("\n", lines);
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла PEM: " + e.getMessage());
        }
        return "";
    }

    private static void convertStringToPEMFile(String pemString, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(pemString);
        } catch (IOException e) {
            System.err.println("Ошибка при записи в файл PEM: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start(PORT);
    }
}