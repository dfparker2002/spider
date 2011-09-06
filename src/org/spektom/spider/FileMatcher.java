package org.spektom.spider;

import java.util.regex.Pattern;

public class FileMatcher {

	private static boolean isPerl5MetaCharacter(char ch) {
		return ("'*?+[]()|^$.{}\\".indexOf(ch) >= 0); //$NON-NLS-1$
	}

	private static boolean isGlobMetaCharacter(char ch) {
		return ("*?[]".indexOf(ch) >= 0); //$NON-NLS-1$
	}

	/**
	 * This method converts Glob pattern into Perl-compatible regular expression.
	 * <p>
	 * @param pattern  A character array representation of a Glob pattern.
	 * @return A String representation of a Perl5 pattern equivalent to the 
	 *         Glob pattern.
	 */
	private static String globToPerl5(char[] pattern) {
		boolean inCharSet;
		int ch;
		StringBuffer buffer;

		buffer = new StringBuffer(2 * pattern.length);
		inCharSet = false;

		for (ch = 0; ch < pattern.length; ch++) {
			switch (pattern[ch]) {
				case '*':
					if (inCharSet)
						buffer.append('*');
					else {
						buffer.append(".*"); //$NON-NLS-1$
					}
					break;
				case '?':
					if (inCharSet)
						buffer.append('?');
					else {
						buffer.append('.');
					}
					break;
				case '[':
					inCharSet = true;
					buffer.append(pattern[ch]);

					if (ch + 1 < pattern.length) {
						switch (pattern[ch + 1]) {
							case '!':
							case '^':
								buffer.append('^');
								++ch;
								continue;
							case ']':
								buffer.append(']');
								++ch;
								continue;
						}
					}
					break;
				case ']':
					inCharSet = false;
					buffer.append(pattern[ch]);
					break;
				case '\\':
					buffer.append('\\');
					if (ch == pattern.length - 1) {
						buffer.append('\\');
					} else if (isGlobMetaCharacter(pattern[ch + 1]))
						buffer.append(pattern[++ch]);
					else
						buffer.append('\\');
					break;
				default:
					if (!inCharSet && isPerl5MetaCharacter(pattern[ch]))
						buffer.append('\\');
					buffer.append(pattern[ch]);
					break;
			}
		}
		return buffer.toString();
	}
	
	/**
	 * This function matches the string against Glob pattern.
	 * <p>
	 * Glob patterns are defined as follows:
	 * <ul>
	 * <li>'*' - Matches zero or more instances of any character.</li>
	 * <li>'?' - Matches one instance of any character. </li>
	 * <li>[...] - Matches any of characters enclosed by the brackets. '*' and '?' lose their
	 * special meanings within a character class. Additionaly if the first character following
	 * the opening bracket is a '!' or a '^', then any character not in the character class is
	 * matched. A '-' between two characters can be used to denote a range. A '-' at the beginning
	 * or end of the character class matches itself rather than referring to a range.
	 * A ']' immediately following the opening '[' matches itself rather than indicating the end
	 * of the character class, otherwise it must be escaped with a backslash to refer to itself.</li> 
	 * <li>'\' - A backslash matches itself in most situations. But when a special character such
	 * as a '*' follows it, a backslash escapes the character, indicating that the special character
	 * should be interpreted as a normal character instead of its special meaning.</li> 
	 * <li>All other characters match themselves.</li>
	 * <p>
	 * @param pattern Glob-style pattern
	 * @param string String to match
	 * @return <code>true</code> if matches, <code>false</code> otherwise.
	 */
	public static boolean matches(String pattern, String string) {
		Pattern p = Pattern.compile(globToPerl5(pattern.toCharArray()));
		return p.matcher(string).matches();
	}
}
