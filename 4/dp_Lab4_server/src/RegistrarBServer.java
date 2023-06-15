import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RegistrarBServer {
    private static final int PORT = 8888;
    private static BigInteger publicModulus;
    private static BigInteger publicExponent;
    private static BigInteger privateExponent;
    private static BigInteger blindedSignature;
    private static BigInteger blindedMessage;

    public RegistrarBServer() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Генерация закрытого и открытого ключей
        generateKeys();
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, ClassNotFoundException {
        RegistrarBServer server = new RegistrarBServer();
        Socket socket = server.start(PORT);
        server.sendComponents(socket);
        server.receiveBlindedMessage(socket);
        blindedSignature = server.createBlindedSignature(privateExponent, publicModulus, blindedMessage);
        server.sendBlindedSignature(socket);
    }

    public Socket start(int serverPort) {
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Сервер запущен. Ожидание подключения клиента...");

            Socket socket = serverSocket.accept();
            System.out.println("Подключено клиент: " + socket.getInetAddress().getHostAddress());

            return socket;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendComponents(Socket socket) throws IOException {
        // Отправка открытого ключа клиенту
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        BigInteger[] components = new BigInteger[2];
        components[0] = publicModulus;
        components[1] = publicExponent;
        outputStream.writeObject(components);
    }

    private void receiveBlindedMessage(Socket socket) throws IOException, ClassNotFoundException {
        // Получение затемненного сообщения клиента
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        blindedMessage = (BigInteger) inputStream.readObject();
    }

    private void sendBlindedSignature(Socket socket) throws IOException {
        // Отправка подписанного затемненного сообщения клиенту
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.writeObject(blindedSignature);
    }

    private static void generateKeys() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Генерация закрытого ключа с помощью OpenSSL
        ProcessBuilder privateKeyBuilder = new ProcessBuilder("openssl", "genpkey",
                "-algorithm", "RSA", "-out", "privatekey.pem", "-pkeyopt", "rsa_keygen_bits:1024");
        executeCommand(privateKeyBuilder);

        // Извлечение открытого ключа из закрытого ключа с помощью OpenSSL
        ProcessBuilder publicKeyBuilder = new ProcessBuilder("openssl", "rsa",
                "-pubout", "-in", "privatekey.pem", "-out", "publickey.pem");
        executeCommand(publicKeyBuilder);

        PrivateKey privateKey = getFormattedPrivateKey(Files.readAllBytes(Paths.get("privatekey.pem")));
        PublicKey publicKey = getFormattedPublicKey(Files.readAllBytes(Paths.get("publickey.pem")));
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;

        privateExponent = rsaPrivateKey.getPrivateExponent();
        publicModulus = rsaPublicKey.getModulus();
        publicExponent = rsaPublicKey.getPublicExponent();
    }

    private static PrivateKey getFormattedPrivateKey(byte[] privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        String privateKeyString = new String(privateKey, StandardCharsets.UTF_8);
        String privateKeyContent = privateKeyString
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);
    }

    private static PublicKey getFormattedPublicKey(byte[] publicKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        String publicKeyString = new String(publicKey, StandardCharsets.UTF_8);
        String publicKeyContent = publicKeyString
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
    }

    private BigInteger createBlindedSignature(BigInteger privateExp, BigInteger privateMod, BigInteger blindMessage) {
        return blindMessage.modPow(privateExp, privateMod);
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
}