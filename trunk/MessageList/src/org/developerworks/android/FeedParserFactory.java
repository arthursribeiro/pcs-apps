package org.developerworks.android;
public abstract class FeedParserFactory {
	static String feedUrl = "http://globoesporte.globo.com/dynamo/semantica/editorias/plantao/futebol/times/campinense/feed.rss";
	
	public static FeedParser getParser(){
		return new SaxFeedParser(feedUrl);
	}
}