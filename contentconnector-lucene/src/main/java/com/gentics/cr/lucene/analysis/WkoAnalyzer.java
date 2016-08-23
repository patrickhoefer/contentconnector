package com.gentics.cr.lucene.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.gentics.cr.lucene.LuceneVersion;

public class WkoAnalyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		final Tokenizer source = new StandardTokenizer(LuceneVersion.getVersion(), reader);
		TokenStream ts = new StandardFilter(LuceneVersion.getVersion(), source);
		ts = new LowerCaseFilter(LuceneVersion.getVersion(), ts);
		ts = new GermanNormalizationFilterAdapted(ts);
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
						System.out.println("found scharfes s");
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
					case '\'':
						System.out.println("found einfaches hochkomma");
						//<><
						buffer = termAtt.resizeBuffer(2 + length);
						buffer[i+1] = '>';
						buffer[i+2] = '<';						
						if (i < length) {
							System.arraycopy(buffer, i, buffer, i + 2, (length - i));
						} 
						buffer[i] = '<';
						length=length+2;
						break;
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