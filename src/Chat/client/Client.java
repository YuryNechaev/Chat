package Chat.client;

import Chat.Connection;
import Chat.ConsoleHelper;
import Chat.Message;
import Chat.MessageType;

import java.io.IOException;
import java.net.Socket;


public class Client {

    protected Connection connection;
    private volatile boolean clientConnected = false;

    protected String getServerAddress() {
        System.out.println("Введите адрес сервера");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        System.out.println("Введите номер порта");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        System.out.println("Введите имя пользователя");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Сообщение не отправлено");
            clientConnected = false;
        }

    }

    public class SocketThread extends Thread {

        // Выводим текст сообщения в консоль
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        // Выводим информацию о добавлении участника
        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник с именем " + userName + " присоединился к чату.");
        }

        // Выводим информацию о выходе участника
        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник с именем " + userName + " покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        // представляем клиента серверу
        protected void clientHandshake() throws IOException, ClassNotFoundException {
            Message message = null;
            while (true) {
                message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST) {// Сервер запросил имя пользователя
                    // Запрашиваем ввод имени с консоли
                    String userName = getUserName();
                    // Отправляем имя на сервер
                    Message newMessage = new Message(MessageType.USER_NAME, userName);
                    connection.send(newMessage);
                } else if (message.getType() == MessageType.NAME_ACCEPTED) {// Сервер принял имя пользователя
                    // Сообщаем главному потоку, что он может продолжить работу
                    notifyConnectionStatusChanged(true);
                    return;
                } else
                    throw new IOException("Unexpected MessageType");

            }

        }
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            // Цикл обработки сообщений сервера
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) // Сервер прислал сообщение с текстом
                    processIncomingMessage(message.getData());
                else if (message.getType() == MessageType.USER_ADDED)
                    informAboutAddingNewUser(message.getData());
                else if (message.getType() == MessageType.USER_REMOVED)
                    informAboutDeletingNewUser(message.getData());
                else
                    throw new IOException("Unexpected MessageType");
            }
        }
        public void run(){

            try {// Создаем соединение с сервером
                connection = new Connection(new Socket(getServerAddress(), getServerPort()));
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);

            }
        }
    }


        public void run() {
            SocketThread additionalSocketThread = getSocketThread();
            // Помечаем поток как daemon
            additionalSocketThread.setDaemon(true);
            additionalSocketThread.start();

            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Произошла ошибка во время подключения");
                return;
            }
            if (clientConnected)
                ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
            else
                ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");

            // Пока не будет введена команда exit, считываем сообщения с консоли и отправляем их на сервер
            while (clientConnected) {
                String text = ConsoleHelper.readString();
                if (text.equalsIgnoreCase("exit"))
                    break;
                if (shouldSendTextFromConsole())
                    sendTextMessage(text);

            }
        }

        public static void main(String[] args) {
            Client client = new Client();
            client.run();
        }
    }
