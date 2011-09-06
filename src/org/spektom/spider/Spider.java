package org.spektom.spider;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class Spider implements Runnable {

	private URL startURL;
	private LinkedList<URL> urlQueue;
	private Set<String> processedURLs;
	private Map<String, String[]> disallowedEntries;
	private ISpiderHandler handler;
	private int timeout;
	private boolean followOtherDomains;
	private int threadsNumber;
	private boolean followRobots;
	private String userAgent;
	private String pattern;
	private boolean verbose;

	public Spider() {
		this(null);
	}

	public Spider(URL startURL) {
		this.startURL = startURL;
		urlQueue = new LinkedList<URL>();
		processedURLs = new HashSet<String>();
		disallowedEntries = new HashMap<String, String[]>();
		timeout = 5000; // default timeout is 5 seconds
		threadsNumber = 5;
		followRobots = true;
	}

	/**
	 * Gets URL to start retreival from
	 * @return start URL
	 */
	public URL getStartURL() {
		return startURL;
	}

	/**
	 * Sets URL to start retreival from
	 * @param startURL
	 */
	public void setStartURL(URL startURL) {
		this.startURL = startURL;
	}

	/**
	 * @return Handler attached to this spider
	 */
	public ISpiderHandler getHandler() {
		return handler;
	}

	/**
	 * Attach handler to this spider
	 * @param handler
	 */
	public void setHandler(ISpiderHandler handler) {
		this.handler = handler;
	}

	/**
	 * Connection timeout in milliseconds
	 * @return timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Sets connection timeout in milliseconds
	 * @param timeout
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Whether to follow other domains
	 * @return followOtherDomains
	 */
	public boolean isFollowOtherDomains() {
		return followOtherDomains;
	}

	/**
	 * Set whether to follow other domains
	 * @param followOtherDomains
	 */
	public void setFollowOtherDomains(boolean followOtherDomains) {
		this.followOtherDomains = followOtherDomains;
	}

	/**
	 * Gets number of concurrent worker threads
	 * @return
	 */
	public int getThreadsNumber() {
		return threadsNumber;
	}

	/**
	 * Sets number of concurrent worker threads
	 * @param threadsNumber
	 */
	public void setThreadsNumber(int threadsNumber) {
		this.threadsNumber = threadsNumber;
	}

	/**
	 * Gets whether to follow robots.txt rules, and META robots tag
	 * @param followRobots
	 */
	public boolean isFollowRobots() {
		return followRobots;
	}

	/**
	 * Sets whether to follow robots.txt rules, and META robots tag
	 * @param followRobots
	 */
	public void setFollowRobots(boolean followRobots) {
		this.followRobots = followRobots;
	}

	/**
	 * Returns user agent string, that will be used in User-Agent header
	 * @return
	 */
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * Sets user agent string, that will be used in User-Agent header
	 * @param userAgent
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * Sets pattern that only URL matches it will be retrieved
	 * @param pattern
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Returns pattern that only URL matches it will be retrieved
	 * @return pattern
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * If set to true verbose information will be printed to user
	 * @param verbose
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * Returns whether verbose information will be printed to user
	 * @return verbose
	 */
	public boolean isVerbose() {
		return verbose;
	}
	

	/**
	 * Add this URL to the queue of URLs to be processed
	 * @param url
	 */
	private void queueURL(URL url) {
		if (!followOtherDomains && !url.getHost().equals(getStartURL().getHost())) {
			if (verbose) {
				System.out.format("Refusing to put URL %s into queue - this URL is from other domain\n", url.toString());
			}
			return;
		}
		String urlStr = url.toString();

		synchronized (urlQueue) {
			if (!processedURLs.contains(urlStr)) {
				if (verbose) {
					System.out.format("Putting URL %s into queue\n", urlStr);
				}
				urlQueue.add(url);
				processedURLs.add(urlStr);

				urlQueue.notifyAll();
			}
		}
	}

	/**
	 * Checks whether this URL is disallowed in file 'robots.txt' placed on the site root
	 * @param url
	 * @return <code>true</code> if this URL is disallowed for processing by this robot, <code>false</code> otherwise
	 */
	private boolean isAllowed(URL url) {

		String[] disallowedEntries;

		synchronized (this.disallowedEntries) {
			if (!this.disallowedEntries.containsKey(url.getHost())) {
				try {
					// File robot.txt must be placed on the site root, see: http://www.robotstxt.org/wc/exclusion.html#robotstxt 
					URL robotsURL = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/robots.txt");
					URLConnection urlConnection = robotsURL.openConnection();
					urlConnection.setConnectTimeout(timeout);
					BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

					boolean userAgentMatches = true;
					List<String> disallowed = new ArrayList<String>();

					String line;
					while ((line = r.readLine()) != null) {

						// See: http://www.robotstxt.org/wc/norobots.html#format
						if (line.startsWith("User-agent:")) {
							// Check whether the restriction is valid only for specific robot:
							if (!"*".equals(line.substring(11).trim())) {
								userAgentMatches = false;
							}
						} else if (line.startsWith("Disallow:") && userAgentMatches) {
							disallowed.add(line.substring(9).trim());
						}
					}
					r.close();

					this.disallowedEntries.put(url.getHost(), disallowed.toArray(new String[disallowed.size()]));
				} catch (MalformedURLException e) {
					// cannot happen, since we are checking the URL before adding it to the queue
				} catch (IOException e) {
					this.disallowedEntries.put(url.getHost(), new String[] {});
				}
			}

			disallowedEntries = this.disallowedEntries.get(url.getHost());
		}

		String path = url.getPath();

		for (int i = 0; i < disallowedEntries.length; ++i) {
			// URL path must not start with one of entries in "Disallow:", see: http://www.robotstxt.org/wc/norobots.html#format
			if (path.startsWith(disallowedEntries[i])) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Starts retreival of Web pages
	 * @see Runnable#run()
	 */
	public void run() {
		queueURL(getStartURL());

		Semaphore waitForQueueSemaphore = new Semaphore(threadsNumber);

		if (verbose) {
			System.out.format("Starting %d working threads\n", threadsNumber);
		}

		Worker[] workers = new Worker[threadsNumber];
		for (int i = 0; i < threadsNumber; ++i) {
			workers[i] = new Worker(waitForQueueSemaphore);
			new Thread(workers[i], "Spider Worker #" + i).start();
		}

		do {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			// Check whether all workers are waiting for the queue,
			// that usually means, that there are no links any more:
			if (waitForQueueSemaphore.availablePermits() == 0) {
				if (verbose) {
					System.out.println("Stopping working threads");
				}
				for (int i = 0; i < threadsNumber; ++i) {
					workers[i].stop();
				}
				// Free them:
				synchronized (urlQueue) {
					urlQueue.notifyAll();
				}
				break;
			}
		} while (true);
	}

	class Worker implements Runnable {

		private boolean isStopped;
		private Semaphore waitForQueueSemaphore;

		public Worker(Semaphore waitForQueueSemaphore) {
			this.waitForQueueSemaphore = waitForQueueSemaphore;
		}

		public void stop() {
			isStopped = true;
		}

		public void run() {
			ISpiderHandler handler = getHandler();
			if (handler == null) {
				handler = new DefaultSpiderHandler();
			}

			while (!isStopped) {
				URL url;
				synchronized (urlQueue) {
					if (urlQueue.isEmpty()) {
						try {
							waitForQueueSemaphore.acquire();
							urlQueue.wait();
						} catch (InterruptedException e) {
						} finally {
							waitForQueueSemaphore.release();
						}
						continue;
					}
					url = urlQueue.removeFirst();
				}

				if (pattern != null && !FileMatcher.matches(pattern, url.toString())) {
					if (verbose) {
						System.out.format("Refusing to load URL %s - it doesn't match pattern '%s'\n", url.toString(), pattern);
					}
					continue;
				}

				// We only work with HTTP protocol:
				if (!"http".equals(url.getProtocol())) {
					if (verbose) {
						System.out.format("Refusing to load URL %s - protocol is not HTTP\n", url.toString());
					}
					continue;
				}

				// Check whether this URL is allowed for processing by this robot
				if (followRobots && !isAllowed(url)) {
					if (verbose) {
						System.out.format("Refusing to load URL %s - not allowed in robots.txt\n", url.toString());
					}
					continue;
				}
				try {
					URLConnection urlConnection = url.openConnection();
					urlConnection.setConnectTimeout(timeout);

					if (userAgent != null) {
						urlConnection.addRequestProperty("User-Agent", userAgent);
					}

					urlConnection.getHeaderFields();
					// We process only HTML pages:
					String contentType = urlConnection.getContentType();
					if (contentType == null || !contentType.startsWith("text/html")) {
						if (verbose) {
							System.out.format("Stopping processing URL %s - unknown content type (%s)\n", url.toString(), contentType);
						}
						continue;
					}

					HTMLPageProcessor htmlPageProcessor = new HTMLPageProcessor(urlConnection);
					htmlPageProcessor.process();

					if (followRobots && htmlPageProcessor.shouldFollow()) {
						Collection<URL> links = htmlPageProcessor.getLinks();
						Iterator<URL> i = links.iterator();
						while (i.hasNext()) {
							queueURL(i.next());
						}
					}

					if (followRobots && htmlPageProcessor.shouldIndex()) {
						handler.handleContent(url, urlConnection.getLastModified(), htmlPageProcessor.getContents());
					}
				} catch (FileNotFoundException e) {
					if (verbose) {
						System.out.println("Resource doesn't exist: " + url);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
