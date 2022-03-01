package Chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message){
        for(Connection connection:connectionMap.values()){
            try {
                connection.send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Сообщение не отправлено");
            }
        }
    }
    public static void main(String[] args) {

        int port = ConsoleHelper.readInt();

        try(ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Сервер запущен");
            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        }catch (Exception e){
            ConsoleHelper.writeMessage("Ошибка подключения.");
        }
        
    }
//    Класс Handler должен реализовывать протокол общения с клиентом.

    private static class Handler extends Thread {

        private Socket socket;

        public Handler(Socket socket){
            this.socket = socket;
        }
        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message message = connection.receive();
                if (message.getType() != MessageType.USER_NAME) {
                    ConsoleHelper.writeMessage("Получено сообщение от" + socket.getRemoteSocketAddress()+" Тип информации не соответствует типу имени пользователя.");
                    continue;
                }
                String userName = message.getData();
                if(userName.isEmpty()){
                    ConsoleHelper.writeMessage("Получено сообщение от "+ socket.getRemoteSocketAddress()+ "Имя пользователя не может быть пустым.");
                    continue;
                }
                if(connectionMap.containsKey(userName)){
                    ConsoleHelper.writeMessage("Получено сообщение от "+ socket.getRemoteSocketAddress()+ "Такое имя пользователя уже есть.");
                    continue;
                }

                    connectionMap.put(userName, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                    return userName;

                }
        }
        private void notifyUsers(Connection connection, String userName) throws IOException{
            for(String key: connectionMap.keySet()) {
                if (!key.equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, key));
                }
            }
        }
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while(true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    Message outMessage = new Message(MessageType.TEXT, userName+": "+message.getData());
                    sendBroadcastMessage(outMessage);
                }else{
                    ConsoleHelper.writeMessage("Некорректный тип сообщения.");
                }
            }
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Установлено новое соединение с удаленным адресом "+ socket.getRemoteSocketAddress());
            String userName = null;
            try (Connection connection = new Connection(socket)){
                userName = serverHandshake(connection);
                // Сообщаем всем участникам, что присоединился новый участник
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                // Сообщаем новому участнику о существующих участниках
                notifyUsers(connection, userName);
                // Обрабатываем сообщения пользователей
                serverMainLoop(connection, userName);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с уделенным адресом "+ socket.getRemoteSocketAddress());
            } catch (ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с уделенным адресом "+ socket.getRemoteSocketAddress());
            }
            if(userName!=null){
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }
            ConsoleHelper.writeMessage("Соединение с "+ socket.getRemoteSocketAddress()+ " закрыто.");
        }
    }
}
