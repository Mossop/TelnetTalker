/*
 * $Author$
 * $RCSfile$
 * $Date$
 * $Revision$
 */
package com.blueprintit.telnettalker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.speech.Central;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.Voice;

/**
 * @author Dave
 */
public class TelnetTalker implements Runnable
{
	//private BaseGUI gui;
	private int localport = 6666;
	private int port = 6715;
	private String server = "mud.atrocity.org";
	private Selector selector;
	private Map<SocketChannel,SocketChannel> channelMap;
	private Map<SocketChannel,TextListener> listenerMap;
	
	public TelnetTalker()
	{
		//gui = new BaseGUI();
		channelMap = new HashMap<SocketChannel,SocketChannel>();
		listenerMap = new HashMap<SocketChannel,TextListener>();
		(new Thread(this)).start();
	}
	
	public void run()
	{
		try
		{
			String voiceName="kevin16";
			
	    SynthesizerModeDesc desc = new SynthesizerModeDesc(
          null,          // engine name
          "general",     // mode name
          Locale.US,     // locale
          null,          // running
          null);         // voice
	    Synthesizer synthesizer = Central.createSynthesizer(desc);
	    if (synthesizer==null)
	    {
	    	throw new Exception("Could not create synthesizer");
	    }
	    synthesizer.allocate();
	    synthesizer.resume();

      desc = (SynthesizerModeDesc) synthesizer.getEngineModeDesc();
      Voice[] voices = desc.getVoices();
      Voice voice = null;
      for (int i = 0; i < voices.length; i++) {
          if (voices[i].getName().equals(voiceName)) {
              voice = voices[i];
              break;
          }
      }
      if (voice == null) {
          System.err.println(
              "Synthesizer does not have a voice named "
              + voiceName + ".");
          System.exit(1);
      }
      synthesizer.getSynthesizerProperties().setVoice(voice);
      
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			selector = Selector.open();
			ServerSocketChannel listener = ServerSocketChannel.open();
			listener.configureBlocking(false);
			listener.socket().bind(new InetSocketAddress("localhost",localport));
			listener.register(selector,SelectionKey.OP_ACCEPT);
			while (true)
			{
				if (selector.select()>0)
				{
					Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
					while (keys.hasNext())
					{
						SelectionKey key = keys.next();
						if (key.isAcceptable())
						{
							try
							{
								assert key.channel()==listener;
								ServerSocketChannel channel = (ServerSocketChannel)key.channel();
								SocketChannel socket = channel.accept();
								if (socket!=null)
								{
									SocketChannel osocket = SocketChannel.open(new InetSocketAddress(server,port));
									channelMap.put(socket,osocket);
									channelMap.put(osocket,socket);
									socket.configureBlocking(false);
									osocket.configureBlocking(false);
									
									TextLineListener tl = new TextLineListener();
									listenerMap.put(osocket,tl);

									tl.addLineListener(new RegexLineTalker(synthesizer,"\\*WIZ\\*.*?: (.*)",1));
									tl.addLineListener(new RegexLineTalker(synthesizer,"chats, \"(.*)\"",1));
									tl.addLineListener(new RegexLineTalker(synthesizer,"\\*FIEND\\*.*?: (.*)",1));
									
									socket.register(selector,SelectionKey.OP_READ);
									osocket.register(selector,SelectionKey.OP_READ);
									System.out.println("Connected");
								}
								else
								{
									System.out.println("Problem connecting");
								}
								keys.remove();
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
						else if (key.isReadable())
						{
							try
							{
								assert key.channel() instanceof SocketChannel;
								SocketChannel channel = (SocketChannel)key.channel();
								SocketChannel ochannel = channelMap.get(channel);
								buffer.clear();
								int count = channel.read(buffer);
								while (count!=0)
								{
									//System.out.println("Read "+count+" bytes.");
									if (count>0)
									{
										buffer.flip();
										ochannel.write(buffer);
										if (listenerMap.containsKey(channel))
										{
											buffer.rewind();
											listenerMap.get(channel).write(buffer);
										}
									}
									else if (count<0)
									{
										channelMap.remove(channel);
										channelMap.remove(ochannel);
										key.cancel();
										for (SelectionKey okey : selector.keys())
										{
											if (okey.channel()==ochannel)
											{
												okey.cancel();
											}
										}
										listenerMap.remove(channel);
										listenerMap.remove(ochannel);
										ochannel.close();
										System.out.println("Disconnected");
										break;
									}
									count = channel.read(buffer);
								}
								keys.remove();
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("Error creating selector or setting up synthesizer");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		new TelnetTalker();
	}
}
