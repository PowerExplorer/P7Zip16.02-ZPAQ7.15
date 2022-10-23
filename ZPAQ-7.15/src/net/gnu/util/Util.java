package net.gnu.util;

import java.util.*;
import java.io.*;
import java.util.Map.Entry;
import java.net.*;
import java.text.*;
import java.lang.reflect.*;
import android.util.*;
import java.util.regex.Pattern;

public class Util {

	public static final String UTF8 = "UTF-8";
	public static final NumberFormat nf = NumberFormat.getInstance();

	public static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}
	
	public static final String SPECIAL_CHAR_PATTERNSTR = "([{}^$.\\[\\]|*+?()\\\\])";
	public static final String replaceRegexAll(String fileContent, String from, String to, boolean isRegex, boolean caseSensitive) {
		if (!isRegex) {
			//Log.d(from, to);
			from = from.replaceAll(SPECIAL_CHAR_PATTERNSTR, "\\\\$1");
			to = to.replaceAll(SPECIAL_CHAR_PATTERNSTR, "\\\\$1");
			//Log.d(from, to);
		}
		//System.out.println(fileContent);
		Pattern p = null;
		if (!caseSensitive) {
			p = Pattern.compile(from, Pattern.CASE_INSENSITIVE);
			//fileContent = fileContent.replaceAll("(?i)"+from, to);
		} else {
			p = Pattern.compile(from, Pattern.UNICODE_CASE);
			//fileContent = fileContent.replaceAll(from, to);
		}
		fileContent = p.matcher(fileContent).replaceAll(to);
		//System.out.println(fileContent);
		return fileContent;
	}

	public static String collectionToString(Collection<?> list, boolean number, String sep) {
		if (list == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		int len = list.size() - 1;
		int c = 0;
		if (!number) {
			for (Object obj : list) {
				sb.append(obj);
				if (c++ < len) {
					sb.append(sep);
				}
			}
		} else {
			int counter = 0;
			for (Object obj : list) {
				sb.append(++counter + ": ").append(obj);
				if (c++ < len) {
					sb.append(sep);
				}
			}
		}
		return sb.toString();
	}
	
	public static String arrayToString(Object[] list, boolean number, String sep) {
		if (list == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		int len = list.length - 1;
		int c = 0;
		if (!number) {
			for (Object obj : list) {
				sb.append(obj);
				if (c++ < len) {
					sb.append(sep);
				}
			}
		} else {
			int counter = 0;
			for (Object obj : list) {
				sb.append(++counter + ": ").append(obj);
				if (c++ < len) {
					sb.append(sep);
				}
			}
		}
		return sb.toString();
	}

	public static String[] stringToArray(String s, String sep) {
		sep = sep.replaceAll(SPECIAL_CHAR_PATTERNSTR, "\\\\$1");
		String[] split = s.split(sep);
		return split;
	}

	public static List<String> stringToList(String s, String sep) {
		sep = sep.replaceAll(Util.SPECIAL_CHAR_PATTERNSTR, "\\\\$1");
		String[] split = s.split(sep);
		ArrayList<String> l = new ArrayList<String>(split.length);
		for (String st : split) {
			l.add(st);
		}
		return l;
	}
	
}
