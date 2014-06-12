package com.gentics.cr.lucene.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.gentics.cr.lucene.LuceneVersion;

public class WkoAnalyzer extends Analyzer {

	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		TokenStream ts = new StandardTokenizer(LuceneVersion.getVersion(), reader);
		ts = new LowerCaseFilter(LuceneVersion.getVersion(), ts);
		ts = new GermanNormalizationFilterAdapted(ts);
		return ts;
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
						buffer[i++] = 's';
						buffer = termAtt.resizeBuffer(1 + length);
						if (i < length) {
							System.arraycopy(buffer, i, buffer, i + 1, (length - i));
						} 
						buffer[i] = 's';
						length++;
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