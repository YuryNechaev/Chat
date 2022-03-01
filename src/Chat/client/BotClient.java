package Chat.client;

import Chat.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BotClient extends Client{

    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }
    protected boolean shouldSendTextFromConsole(){ // Бот не должен отправлять текст введенный с консоли
        return false;
    }

    protected String getUserName(){ // Каждый раз генерируем новое имя бота на случай, если к серверу подключится несколько ботов
        int x = (int) (Math.random()*100);
        return "date_bot_"+x;
    }

    public static void main(String[] args) { //создаем и запускаем бот клиента
        BotClient botClient = new BotClient();
        botClient.run();
    }

    public class BotSocketThread extends SocketThread {

        protected void clientMainLoop() throws IOException, ClassNotFoundException {

            String hello = "Привет чатику я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.";
            BotClient.this.sendTextMessage(hello);

            super.clientMainLoop();
        }
         protected void processIncomingMessage(String message){
             // Выводим текст сообщения в консоль
             ConsoleHelper.writeMessage(message);
             // Отделяем отправителя от текста сообщения
             if((message.split(": ").length)!=2)
                 return;

             String senderName = message.split(": ")[0];
             String messageData = message.split(": ")[1];

             // Подготавливаем формат для отправки даты согласно запросу
             String format = null;
              switch (messageData){
                  case "дата": format = "d.MM.YYYY";
                  break;
                  case "день": format = "d";
                  break;
                  case "месяц": format = "MMMM";
                  break;
                  case "год": format = "YYYY";
                  break;
                  case "время": format = "H:mm:ss";
                  break;
                  case "час": format = "H";
                  break;
                  case "минуты": format = "m";
                  break;
                  case "секунды": format = "s";
                  break;
              }
              if(format!=null){
                  String answer = new SimpleDateFormat(format).format(Calendar.getInstance().getTime());

                 BotClient.this.sendTextMessage("Информация для "+ senderName+ ": "+ answer);
              }

         }


    }


}
