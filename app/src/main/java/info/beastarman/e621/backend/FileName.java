package info.beastarman.e621.backend;

import java.util.Arrays;
import java.util.List;

public class FileName
{
	private static List<String> notAllowedSequences = Arrays.asList(new String[]{
																						"\\",
																						"/",
																						"?",
																						"%",
																						"*",
																						":",
																						"|",
																						"\"",
																						"<",
																						">",
																						"'",
	});
	
	private static String safeCharacter = "$";
	private static char baseCharacter = 'a';

	public static String encodeFileName(String str)
	{
		String safeString = str.replace(safeCharacter, safeCharacter + Character.toString((char) (baseCharacter)));

		for(int i = 0; i < notAllowedSequences.size(); i++)
		{
			String sequence = notAllowedSequences.get(i);

			safeString = safeString.replace(sequence, safeCharacter + Character.toString((char) (baseCharacter + i + 1)));
		}

		return safeString;
	}

	public static String decodeFileName(String str)
	{
		String safeString = str;

		for(int i = 0; i < notAllowedSequences.size(); i++)
		{
			String sequence = notAllowedSequences.get(i);

			safeString = safeString.replace(safeCharacter + Character.toString((char) (baseCharacter + i + 1)), sequence);
		}

		safeString = safeString.replace(safeCharacter + Character.toString((char) (baseCharacter)), safeCharacter);

		return safeString;
	}
}
