/*
 * $Author$
 * $RCSfile$
 * $Date$
 * $Revision$
 */
package com.blueprintit.telnettalker;

import java.nio.ByteBuffer;

/**
 * @author Dave
 */
public interface TextListener
{
	public void write(ByteBuffer buffer);
}
