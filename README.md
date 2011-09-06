Spider
=======
Simple Web crawler written in Java.

Building
---------
`ant`

Running
--------
`java -cp spider.jar org.spektom.spider.SpiderTool`

Usage
------
`java Spider [options] URL`
 
Where options are:

<pre>
-r &lt;true|false&gt;  Follow robots.txt and META robot tag rules (default: true) 
-t &lt;number&gt;      Number of concurrent downloads (default: 5)
-f &lt;true|false&gt;  Follow other domains (default: false)
-c &lt;timeout&gt;     Connect/read timeout in milliseconds (default: 5000)
-u &lt;string&gt;      String that will be sent in User-Agent header (default: none)
-p &lt;pattern&gt;     Follow only URLs that match pattern
-v &lt;true|false&gt;  Verbose output (default: false)
</pre>
