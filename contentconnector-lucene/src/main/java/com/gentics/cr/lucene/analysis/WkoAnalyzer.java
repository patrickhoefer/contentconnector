package com.gentics.cr.lucene.analysis;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Properties;
import java.io.FileReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import com.gentics.cr.configuration.GenericConfiguration;
import java.io.BufferedReader;
import com.gentics.cr.lucene.LuceneVersion;
import org.apache.lucene.util.Version;
import com.gentics.cr.lucene.indexer.IndexerUtil;

import org.apache.log4j.Logger;

public class WkoAnalyzer extends StopwordAnalyzerBase {
	
	private static final String STOP_WORD_FILE_KEY = "STOPWORDFILE";
	private static final Logger LOGGER = Logger.getLogger(WkoAnalyzer.class);
	
	public WkoAnalyzer(GenericConfiguration conf) throws IOException{
		super(LuceneVersion.getVersion(),loadStopwordSet(new BufferedReader(new FileReader(IndexerUtil.getFileFromPath((String) conf.get(STOP_WORD_FILE_KEY)))), LuceneVersion.getVersion()));
		LOGGER.error("Charrarrayset: "+stopwords);
		LOGGER.error("Stopwordsfile: "+((String) conf.get(STOP_WORD_FILE_KEY)));
		/*LOGGER.error("INTERVAL: "+((String) conf.get("interval")));
		Properties p = conf.getProperties();
		StringWriter sw = new StringWriter();
		p.list(new PrintWriter(sw));
		LOGGER.error("HMMM" + sw.toString());*/
	}
	public WkoAnalyzer(Version matchVersion, CharArraySet stopWords) {
		    super(matchVersion, stopWords);
	}
	 
	/*public WkoAnalyzer(LuceneVersion version){
		//super(conf);
		// File stopWordFile = IndexerUtil.getFileFromPath((String) config.get(STOP_WORD_FILE_KEY));
		super(version,stopwords);
		LOGGER.info()
	}*/
	public WkoAnalyzer(){
		super(LuceneVersion.getVersion());
	}
	
	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		final Tokenizer source = new StandardTokenizer(LuceneVersion.getVersion(), reader);
		TokenStream ts = new StandardFilter(LuceneVersion.getVersion(), source);
		ts = new LowerCaseFilter(LuceneVersion.getVersion(), ts);
		ts = new GermanNormalizationFilterAdapted(ts);
		ts = new StopFilter(LuceneVersion.getVersion(), ts, stopwords);
		return new TokenStreamComponents(source,ts);
	}

	public final class GermanNormalizationFilterAdapted extends TokenFilter {
		private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

		public GermanNormalizationFilterAdapted(TokenStream input) {
			super(input);
		}

		@Override
		public boolean incrementToken() throws IOException {
			
			
			if (input.incrementToken()) {
				char buffer[] = termAtt.buffer();
				int length = termAtt.length();
				for (int i = 0; i < length; i++) {
					final char c = buffer[i];
					switch(c) {
					case 'ß':
						// LOGGER.error("found scharfes s");
						buffer[i++] = 's';
						buffer = termAtt.resizeBuffer(1 + length);
						if (i < length) {
							System.arraycopy(buffer, i, buffer, i + 1, (length - i));
						} 
						buffer[i] = 's';
						length++;
						break;
					case '@':
						buffer[i++] = 't';
						buffer = termAtt.resizeBuffer(1 + length);
						if (i < length) {
							System.arraycopy(buffer, i, buffer, i + 1, (length - i));
						} 
						buffer[i] = 'a';
						length++;
						break;
					/*case '\'':
						LOGGER.error("found einfaches hochkomma");
						//<><
						buffer = termAtt.resizeBuffer(2 + length);
						buffer[i+1] = '>';						
						if (i < length) {
							System.arraycopy(buffer, i, buffer, i + 2, (length - i));
						}
						buffer[i+2] = '<';
						buffer[i] = '<';
						length=length+2;
						break;*/
					default:
					}
				}
				termAtt.setLength(length);
				return true;
			} else {
				return false;
			}
		}
	}
}