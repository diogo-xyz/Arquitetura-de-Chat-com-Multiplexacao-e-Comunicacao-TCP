import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer
{
  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();
  
  static Set<String> commands = new TreeSet<>();

  static TreeMap<String,SocketChannel> names = new TreeMap<>();
  static TreeMap<String,HashSet<SocketChannel>> rooms = new TreeMap<>();

  static private class Client {
	  String name;
	  String room;
	  ByteBuffer buffer;
	  ByteBuffer lineBuffer;
	  Client() {
		  name = null;
		  room = null;
		  buffer = ByteBuffer.allocate( 16384 );
		  lineBuffer = ByteBuffer.allocate( 16384 );
	  }
  }



  static public void main( String args[] ) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt( args[0] );

    commands.add("/nick");
    commands.add("/join");
    commands.add("/leave");
    commands.add("/bye");
    commands.add("/priv");
    
    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );

      // Create a new Selector for selecting
      Selector selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );
  
      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if (key.isAcceptable()) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println( "Got connection from "+s );

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );

            // Register it with the selector, for reading
            SelectionKey clientKey = sc.register( selector, SelectionKey.OP_READ );
	        clientKey.attach(new Client());
          } else if (key.isReadable()) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
              boolean ok = processInput(key,sc);

              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                key.cancel();
				
				Client client = (Client)key.attachment();
				if (client.name != null && client.room != null) {
					String message = "LEFT " + client.name + "\n";
					broadcast_room(message,rooms.get(client.room),sc);
				}
				if (client.name != null) {
					names.remove(client.name);
				}
				if (client.room != null) {
					rooms.get(client.room).remove(sc);
				}
                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
              }

            } catch( IOException ie ) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch( IOException ie2 ) { System.out.println( ie2 ); }

              System.out.println( "Closed "+sc );
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }


    // Just read the message from the socket and send it to stdout
    static private boolean processInput(SelectionKey key, SocketChannel sc) throws IOException {
		// Read the message to the buffer
		Client client = (Client)key.attachment();
		ByteBuffer buffer = client.buffer;
		ByteBuffer lineBuffer = client.lineBuffer;

		int numRead = sc.read( buffer );

        // If no data, close the connection
		if (numRead == -1) {
			return false;
		}

		if (numRead == 0) {
			return true;
		}


        // Decode and print the message to stdout
		buffer.flip();
		String message = "";
        while (buffer.hasRemaining()) {
			byte b = buffer.get();
			if (b == '\n') {
				// Fim de uma mensagem completa
				lineBuffer.flip();
				message = message + StandardCharsets.UTF_8.decode(lineBuffer).toString();
				System.out.println("GOT: " + message);   
				processMessage(key, sc, message);

			    lineBuffer.clear();
			    message = "";  // Reset para próxima mensagem
			} else {
				lineBuffer.put(b);
			}
		}
		buffer.compact();
		return true;
    }

    static private void processMessage(SelectionKey key, SocketChannel sc, String message) throws IOException {
		Client client = (Client)key.attachment();

        //not supported command
        if (message.equals("")) return;
		if (message.charAt(0) == '/' && (message.length() == 1 || message.charAt(1) != '/') && !commands.contains(message.split(" ")[0])) {
			reply_error(sc);
			return;
		}

        if (client.name == null) {  // init state
		String[] tokens = message.split(" ");
			if (tokens.length == 2 && tokens[0].equals("/nick")) {
				if (names.containsKey(tokens[1])) reply_error(sc);
				else {
					names.put(tokens[1],sc);
					client.name = tokens[1];
					reply_ok(sc);
				}
			}
			else if (tokens.length == 1 && tokens[0].equals("/bye")) {
				reply_bye(sc);
				key.cancel();
				Socket s = null;
                try {
					s = sc.socket();
                    System.out.println( "Closing connection to "+s );
					s.close();
                } catch( IOException ie ) {
					System.err.println( "Error closing socket "+s+": "+ie );
				}
			}
			else {
				reply_error(sc);
			}
		}
		else if (client.name != null && client.room == null) { // outside state
		    String[] tokens = message.split(" ");
			if (tokens.length == 2) {
				if (tokens[0].equals("/join")) {
					reply_ok(sc);
			        rooms.putIfAbsent(tokens[1], new HashSet<>());
					rooms.get(tokens[1]).add(sc);
					client.room = tokens[1];
					String reply = "JOINED " + client.name + "\n";
					broadcast_room(reply,rooms.get(tokens[1]),sc);
				}
				else if (tokens[0].equals("/nick")) {
					if (names.containsKey(tokens[1])) reply_error(sc);
				    else {
						names.put(tokens[1],sc);
						names.remove(client.name);
				        client.name = tokens[1];
				        reply_ok(sc);
				    }
				}
			}
			else if (tokens.length == 1 && tokens[0].equals("/bye")) {
				reply_bye(sc);
		        key.cancel();
		        names.remove(client.name);
		        Socket s = null;
                try {
					s = sc.socket();
                    System.out.println( "Closing connection to "+s );
					s.close();
				} catch( IOException ie ) {
					System.err.println( "Error closing socket "+s+": "+ie );
				}
			}
			else if (tokens.length >= 2 && tokens[0].equals("/priv")) {
				if (!names.containsKey(tokens[1])) reply_error(sc);
				else {
					reply_ok(sc);
			        String temp = message.replaceFirst(tokens[0]+" "+tokens[1]+" ","");
			        String msg = "PRIVATE " + client.name + " " + temp + "\n";
			        send_priv_message(msg,names.get(tokens[1]));
				}
			}
			else {
				reply_error(sc);
			}
		}
		else if (client.name != null && client.room != null) { //inside state
	        String[] tokens = message.split(" ");
			if (tokens.length == 2 && tokens[0].equals("/nick")) {
				if (names.containsKey(tokens[1])) reply_error(sc);
		        else {
					names.put(tokens[1],sc);
			        String msg_users = "NEWNICK " + client.name + " " + tokens[1] + "\n";
					names.remove(client.name);
			        client.name = tokens[1];
		            reply_ok(sc);
			        broadcast_room(msg_users,rooms.get(client.room),sc);
				}
			}
	        else if (tokens.length == 2 && tokens[0].equals("/join")) {
				reply_ok(sc);
		        rooms.putIfAbsent(tokens[1], new HashSet<>());
		        rooms.get(tokens[1]).add(sc);
		        String msg_last_room = "LEFT " + client.name + "\n";
				broadcast_room(msg_last_room,rooms.get(client.room),sc);
		        rooms.get(client.room).remove(sc);
		        client.room = tokens[1];
		        String msg_new_room = "JOINED " + client.name + "\n";
				broadcast_room(msg_new_room,rooms.get(client.room),sc);
			}
			else if (tokens.length == 1 && tokens[0].equals("/leave")) {
				reply_ok(sc);
		        String msg_room = "LEFT " + client.name + "\n";
		        broadcast_room(msg_room,rooms.get(client.room),sc);
		        rooms.get(client.room).remove(sc);
		        client.room = null;
			}
			else if (tokens.length == 1 && tokens[0].equals("/bye")) {
				reply_bye(sc);
		        String msg_room = "LEFT " + client.name + "\n";
				broadcast_room(msg_room,rooms.get(client.room),sc);        
		        key.cancel();
		        names.remove(client.name);
		        rooms.get(client.room).remove(sc);
		        Socket s = null;
                try {
					s = sc.socket();
                    System.out.println( "Closing connection to "+s );
				    s.close();
                } catch( IOException ie ) {
					System.err.println( "Error closing socket "+s+": "+ie );
		        }
			}
			else if (tokens.length >= 2 && tokens[0].equals("/priv")) {
				if (!names.containsKey(tokens[1])) reply_error(sc);
		        else {
					reply_ok(sc);
			        String temp = message.replaceFirst(tokens[0]+" "+tokens[1]+" ","");
			        String msg = "PRIVATE " + client.name + " " + temp + "\n";
					send_priv_message(msg,names.get(tokens[1]));
				}
			}
			else  {
				if (message.charAt(0) == '/') message = message.replaceFirst("/","");
				String msg = "MESSAGE " + client.name + " " + message + "\n";
				broadcast_room(msg,rooms.get(client.room),null);
			}
		}
		else { // not supported state
		    reply_error(sc);
		}
    }

    static private void reply_error(SocketChannel sc) {
		try {
			String reply = "ERROR\n";
			sc.write(ByteBuffer.wrap(reply.getBytes()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    static private void reply_ok(SocketChannel sc) {
		try {
			String reply = "OK\n";
			sc.write(ByteBuffer.wrap(reply.getBytes()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    static private void reply_bye(SocketChannel sc) {
		try {
			String reply = "BYE\n";
			sc.write(ByteBuffer.wrap(reply.getBytes()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    static private void broadcast_room(String message, Set<SocketChannel> room, SocketChannel sc_user) {
		try {
			for (SocketChannel sc : room) {
				if (sc != sc_user) {
					sc.write(ByteBuffer.wrap(message.getBytes()));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    static private void send_priv_message(String message, SocketChannel sc_dest) {
		try {
			sc_dest.write(ByteBuffer.wrap(message.getBytes()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
