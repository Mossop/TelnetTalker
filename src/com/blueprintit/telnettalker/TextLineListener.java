/*
 * $Author$
 * $RCSfile$
 * $Date$
 * $Revision$
 */
package com.blueprintit.telnettalker;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Dave
 */
public class TextLineListener implements TextListener
{
	private StringBuilder linebuffer;
	private Charset charset;
	private List<LineListener> listeners;
	private Pattern ansistrip = Pattern.compile("\u001B\\[.*?[a-zA-Z]");
	
	public TextLineListener()
	{
		charset = Charset.forName("ISO-8859-1");
		linebuffer = new StringBuilder();
		listeners = new LinkedList<LineListener>();
	}
	
	public void addLineListener(LineListener listener)
	{
		listeners.add(listener);
	}
	
	private void lineComplete()
	{
		String line = linebuffer.toString();
		line = ansistrip.matcher(line).replaceAll("");
		//System.out.println("Received and stripped: "+line);
		for (LineListener listener : listeners)
		{
			listener.lineReceived(line);
		}
		linebuffer.delete(0,linebuffer.length());
	}
	
	public void write(ByteBuffer buffer)
	{
		CharBuffer cbuffer = charset.decode(buffer);
		while (cbuffer.remaining()>0)
		{
			char ch = cbuffer.get();
			if (ch=='\n')
			{
				lineComplete();
			}
			else if (ch!='\r')
			{
				linebuffer.append(ch);
			}
		}
	}
}
