package org.spektom.spider;

import java.net.MalformedURLException;
import java.net.URL;

public class SpiderTool {

	class WrongUsageException extends Exception {
		
		public WrongUsageException() {
		}
		
		public WrongUsageException(String message) {
			super(message);
		}

		public String getMessage() {
			StringBuilder buf = new StringBuilder();
			if (super.getMessage() != null) {
				buf.append("ERROR: " + super.getMessage() + "\n\n");
			}
			buf.append("USAGE: java Spider [options] URL\n\n");
			buf.append("Where options are:\n\n");
			buf.append("-r <true|false>         Follow robots.txt and META robot tag rules (default: true)\n");
			buf.append("-t <number>             Number of concurrent downloads (default: 5)\n");
			buf.append("-f <true|false>         Follow other domains (default: false)\n");
			buf.append("-c <timeout>            Connect/read timeout in milliseconds (default: 5000)\n");	
			buf.append("-u <string>             String that will be sent in User-Agent header (default: none)\n");
			buf.append("-p <pattern>            Follow only URLs that match pattern\n");
			buf.append("-v <true|false>         Verbose output (default: false)\n");
			return buf.toString();
		}
	}
	
	private boolean parseBoolean(String str) throws WrongUsageException {
		if ("true".equals(str)) {
			return true;
		}
		if ("false".equals(str)) {
			return false;
		}
		throw new WrongUsageException("Illegal boolean value: " + str);
	}
	
	private int parseInt(String str) throws WrongUsageException {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e)  {
			throw new WrongUsageException("Illegal integer value: " + str);
		}
	}
	
	public void runTool(String[] args) throws WrongUsageException {
		if (args.length < 1) {
			throw new WrongUsageException();
		}

		Spider spider = null;
		try {
			spider = new Spider(new URL(args[args.length - 1]));
		} catch (MalformedURLException e) {
			throw new WrongUsageException("Wrong URL: " + args[args.length - 1]);
		}

		for (int i = 0; i < args.length - 1; ++i) {
			if ("-r".equals(args[i]) && i < args.length - 2) {
				spider.setFollowRobots(parseBoolean(args[i + 1]));
				++i;
			} else if ("-t".equals(args[i]) && i < args.length - 2) {
				spider.setThreadsNumber(parseInt(args[i + 1]));
				++i;
			} else if ("-f".equals(args[i]) && i < args.length - 2) {
				spider.setFollowOtherDomains(parseBoolean(args[i + 1]));
				++i;
			} else if ("-c".equals(args[i]) && i < args.length - 2) {
				spider.setTimeout(parseInt(args[i + 1]));
				++i;
			} else if ("-u".equals(args[i]) && i < args.length - 2) {
				spider.setUserAgent(args[i + 1]);
				++i;
			} else if ("-p".equals(args[i]) && i < args.length - 2) {
				spider.setPattern(args[i + 1]);
				++i;
			} else if ("-v".equals(args[i]) && i < args.length - 2) {
				spider.setVerbose(parseBoolean(args[i + 1]));
				++i;
			} else {
				throw new WrongUsageException("Illegal option: " + args[i]);
			}
		}
		spider.run();
	}
	
	public static void main(String[] args) {
		try {
			new SpiderTool().runTool(args);
		} catch (WrongUsageException e) {
			System.out.println(e.getMessage());
		}
	}
}
