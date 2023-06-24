import dp.scsa.Client;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Bob {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Scanner scanner = new Scanner(System.in);

        Client client = new Client("Bob");
        client.setMessage("Dear Alice.pdf");
        Socket socket = client.connectToServer("localhost", 8888);
        client.createCertificate(socket);
        socket.close();

        try {
            // Создаем серверный сокет, чтобы ожидать подключения Алисы
            ServerSocket serverSocket = new ServerSocket(1234);
            // Ожидаем подключения Алисы
            Socket socketP2P = serverSocket.accept();

            // Получаем потоки ввода/вывода для обмена данными с Бобом
            ObjectOutputStream outputStream = new ObjectOutputStream(socketP2P.getOutputStream());
            outputStream.writeObject(client.getClientLogin());
            ObjectInputStream inputStream = new ObjectInputStream(socketP2P.getInputStream());
            String penFriend = (String) inputStream.readObject();

            // Цикл для отправки и получения файлов
            while (true) {
                System.out.println();

                // Получаем документ от Алисы
                if (!socketP2P.isClosed())
                    client.receiveFiles(socketP2P, penFriend);
                else
                    break;

                System.out.print("\nВведите путь к файлу для отправки: ");
                String filePath = scanner.nextLine();

                // Проверяем, был ли введен путь к файлу
                if (!filePath.isEmpty() && (new File(filePath)).exists()) {
                    System.out.println("Выбранный файл: " + filePath);
                } else {
                    System.out.println("Файл не выбран");
                    socketP2P.close();
                    break;
                }

                client.setMessage(filePath);
                client.createSignature();

                // Отправляем документ Алисе
                client.sendFiles(socketP2P);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
