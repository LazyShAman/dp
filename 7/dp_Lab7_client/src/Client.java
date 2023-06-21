import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Client {
    private static String rootCert;
    private static String intermediateCert;
    private static String leafCert;
    private static String leafCSR;
    private static String clientLogin;
    byte[] message;

    public Client(String name) throws IOException {
        clientLogin = name;
        generateLeafKeyPair();
        generateLeafCSR();
    }

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

    public void sendLeafCSR(Socket socket) throws IOException {
        // Отправка сертификата (CSR) для листового сертификата
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.writeObject(leafCSR);
    }

    public void receiveCertPack(Socket socket) throws IOException, ClassNotFoundException {
        // Получение набора сертификатов для верификации
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        String[] certs = (String[]) inputStream.readObject();
        rootCert = certs[0];
        intermediateCert = certs[1];
        leafCert = certs[2];
    }

    private static void generateLeafKeyPair() throws IOException {
        // Генерация ключевой пары для листового сертификата
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "genpkey", "-algorithm", "ED448", "-out", "leaf_keypair.pem");
        executeCommand(builder);
    }

    private static void generateLeafCSR() throws IOException {
        // Создание запроса на сертификат (CSR) для листового сертификата
        ProcessBuilder builder = new ProcessBuilder("openssl",
                "req", "-new", "-subj", "\"/CN=Leaf\"", "-addext",
                "\"basicConstraints=critical,CA:FALSE\"", "-key",
                "leaf_keypair.pem", "-out", "leaf_csr.pem");
        executeCommand(builder);

        leafCSR = convertPEMFileToString("leaf_csr.pem");
    }

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
}
