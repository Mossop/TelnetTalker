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
import java.util.Map;

/**
 * @author Dave
 */
public class TelnetTalker implements Runnable
{
	private BaseGUI gui;
	private int localport = 6666;
	private int port = 23;
	private String server = "192.168.0.254";
	private Selector selector;
	private Map<SocketChannel,SocketChannel> channelMap;
	
	public TelnetTalker()
	{
		//gui = new BaseGUI();
		channelMap = new HashMap<SocketChannel,SocketChannel>();
		(new Thread(this)).start();
	}
	
	public void run()
	{
		try
		{
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
					for (SelectionKey key : selector.selectedKeys())
					{
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
									socket.register(selector,SelectionKey.OP_READ);
									osocket.register(selector,SelectionKey.OP_READ);
									System.out.println("Connected");
								}
								else
								{
									System.out.println("Problem connecting");
								}
								selector.selectedKeys().remove(key);
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
										ochannel.close();
										System.out.println("Disconnected");
										break;
									}
									count = channel.read(buffer);
								}
								selector.selectedKeys().remove(key);
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
		catch (IOException e)
		{
			System.err.println("Error creating selector");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		new TelnetTalker();
	}
}
