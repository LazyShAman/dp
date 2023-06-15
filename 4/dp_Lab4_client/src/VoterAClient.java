import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;

public class VoterAClient {
    private static final int SERVER_PORT = 8888;
    private static final String IP_ADDRESS = "localhost";
    static BigInteger publicModulus;
    static BigInteger publicExponent;
    static BigInteger blindedSignature;
    byte[] message;
    BigInteger unblindedSignature;

    public static void main(String[] args) {
        VoterAClient client = new VoterAClient();
        client.message = "This is my secret vote. Shh!".getBytes();
        client.connectToServer(IP_ADDRESS, SERVER_PORT);
    }

    public void connectToServer(String serverAddress, int serverPort) {
        try {
            Socket socket = new Socket(serverAddress, serverPort);
            System.out.println("Подключено к серверу: " + socket.getInetAddress().getHostAddress());

            // Получение компонентов открытого ключа сервера
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            BigInteger[] components = (BigInteger[]) inputStream.readObject();

            publicModulus = components[0];
            publicExponent = components[1];

            // Blind the message
            BigInteger[] blindedMessage = blindMessage(message, publicExponent, publicModulus);
            sendBlindedMessage(socket, blindedMessage[0]);

            receiveBlindedSignature(socket);
            unblindedSignature = unblindSignature(blindedSignature, blindedMessage[1], publicModulus);

            verifySignature();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendBlindedMessage(Socket socket, BigInteger message) throws IOException {
        // Отправка затемненного сообщения серверу
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.writeObject(message);
    }

    private void receiveBlindedSignature(Socket socket) throws IOException, ClassNotFoundException {
        // Получение затемненной цифровой подписи с сервера
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        blindedSignature = (BigInteger) inputStream.readObject();
    }

    private BigInteger[] blindMessage(byte[] message, BigInteger exp, BigInteger mod) {
        BigInteger messageInt = new BigInteger(1, message);

        // Генерирует затемняющий фактор (случайное число)
        BigInteger r = generateRandomBlindingFactor(mod);
        // Blinded message = (message * r^e) mod n
        BigInteger blindedMessageInt = messageInt.multiply(r.modPow(exp, mod)).mod(mod);

        // Возвращает затемненное сообщение и затемняющий фактор
        return new BigInteger[]{blindedMessageInt, r};
    }

    private BigInteger generateRandomBlindingFactor(BigInteger mod) {
        BigInteger rand;
        do {
            rand = new BigInteger(mod.bitLength(), new java.security.SecureRandom());
        } while (rand.compareTo(BigInteger.ZERO) <= 0 || rand.compareTo(mod) >= 0 || !rand.gcd(mod).equals(BigInteger.ONE));
        return rand;
    }

    private BigInteger unblindSignature(BigInteger blindedSignature, BigInteger r, BigInteger modulus) {
        // Unblinded message = (blinded message * r^-e) mod n
        return blindedSignature.multiply(r.modPow(new BigInteger("-1"), modulus)).mod(modulus);
    }

    private void verifySignature() {
        BigInteger messageInt = new BigInteger(1, message);
        System.out.println("Первоначальный  голос избирателя (в байтах) " + messageInt);
        System.out.println("Восстановленный голос избирателя (в байтах) " + unblindedSignature.modPow(publicExponent, publicModulus).mod(publicModulus));

        if (unblindedSignature.modPow(publicExponent, publicModulus).mod(publicModulus).equals(messageInt))
            System.out.println("Голос будет зачтен счетчиком голосов.");
        else
            System.out.println("Голос отклонен. Подпись неверна.");
    }
}
