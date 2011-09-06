Spider
=======

Simple Web crawler.

Building
---------

Simply run `ant`

Running
--------

`java -cp spider.jar org.spektom.spider.SpiderTool`

Usage
------

 java Spider [options] URL
 
 Where options are:

 -r <true|false>         Follow robots.txt and META robot tag rules (default: true)
 -t <number>             Number of concurrent downloads (default: 5)
 -f <true|false>         Follow other domains (default: false)
 -c <timeout>            Connect/read timeout in milliseconds (default: 5000)
 -u <string>             String that will be sent in User-Agent header (default: none)
 -p <pattern>            Follow only URLs that match pattern
 -v <true|false>         Verbose output (default: false)

