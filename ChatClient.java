import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.ByteBuffer;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

    static Set<String> commands = new TreeSet<>();

    private Socket socket;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;

    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    
    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

        commands.add("/nick");
        commands.add("/join");
        commands.add("/leave");
        commands.add("/bye");
        commands.add("/priv");
        
        socket = new Socket(server,port);
        outToServer = new DataOutputStream(socket.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
	    //escape
        if (message.charAt(0) == '/' && !commands.contains(message.split(" ")[0])) {
            message = "/" + message;
        }
        outToServer.writeBytes(message + '\n');
    }

    
    // Método principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
        Thread receiver = new Thread(new Runnable () {
            String message;
            @Override
            public void run() {
                try {
                    message = inFromServer.readLine();
                    while (message != null) {
                        // formato mais amigavel
                        String formattedMessage = formatMessage(message);;
                        printMessage(formattedMessage+'\n');
					    message = inFromServer.readLine();
				    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
			    }
            }
        });
        receiver.start();
    }

    private String formatMessage(String serverMessage) {
	    String[] tokens = serverMessage.split(" ", 3);
	    
	    if (tokens.length == 0) {
		    return serverMessage;
	    }
	    
	    String messageType = tokens[0];
	    
	    switch (messageType) {
		    case "MESSAGE":
			    // Formato servidor: MESSAGE nome mensagem
			    // Formato amigável: nome: mensagem
			    if (tokens.length >= 3) {
				    return tokens[1] + ": " + tokens[2];
			    }
			    break;
			    
		    case "NEWNICK":
			    // Formato servidor: NEWNICK nome_antigo nome_novo
			    // Formato amigável: * nome_antigo mudou de nome para nome_novo
			    if (tokens.length >= 3) {
				    return "* " + tokens[1] + " mudou de nome para " + tokens[2];
			    }
			    break;
			    
		    case "JOINED":
			    // Formato servidor: JOINED nome
			    // Formato amigável: * nome entrou na sala
			    if (tokens.length >= 2) {
				    return "* " + tokens[1] + " entrou na sala";
			    }
			    break;
			    
		    case "LEFT":
			    // Formato servidor: LEFT nome
			    // Formato amigável: * nome saiu da sala
			    if (tokens.length >= 2) {
				    return "* " + tokens[1] + " saiu da sala";
			    }
			    break;
			    
		    case "PRIVATE":
			    // Formato servidor: PRIVATE emissor mensagem
			    // Formato amigável: [Privado de emissor]: mensagem
			    if (tokens.length >= 3) {
				    return "[Privado de " + tokens[1] + "]: " + tokens[2];
			    }
			    break;
			    
		    case "OK":
			    // Formato servidor: OK
			    // Formato amigável: * OK
			    return "* OK";
			    
		    case "ERROR":
			    // Formato servidor: ERROR
			    // Formato amigável: * ERRO: comando inválido ou não permitido
			    // Nota: O protocolo não permite erros mais específicos.
			    return "* ERRO: comando inválido ou não permitido";
			    
		    case "BYE":
			    // Formato servidor: BYE
			    // Formato amigável: * Adeus! Conexão encerrada.
			    return "* Adeus! Conexão encerrada.";
			    
		    default:
			    // Se não reconhecer o tipo, mostrar mensagem original
			    return serverMessage;
	    }
	    
	    // Fallback: retornar mensagem original se algo correr mal
	    return serverMessage;
    }
    

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}
