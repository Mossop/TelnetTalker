/*
 * $Author$
 * $RCSfile$
 * $Date$
 * $Revision$
 */
package com.blueprintit.telnettalker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.speech.synthesis.Synthesizer;

/**
 * @author Dave
 */
public class RegexLineTalker implements LineListener
{
	private Pattern pattern;
	private Synthesizer synthesizer;
	private int group;
	
	public RegexLineTalker(Synthesizer synthesizer, String regex, int group)
	{
		this(synthesizer,Pattern.compile(regex),group);
	}
	
	public RegexLineTalker(Synthesizer synthesizer, Pattern regex, int group)
	{
		this.pattern=regex;
		this.synthesizer=synthesizer;
		this.group=group;
	}
	
	public void lineReceived(String line)
	{
		Matcher matcher = pattern.matcher(line);
		while (matcher.find())
		{
			synthesizer.speakPlainText(matcher.group(group),null);
		}
	}
}
