package com.gentics.cr.lucene.indexaccessor;

/**
 * Derived from code by subshell GmbH
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.Similarity;

/**
 * An IndexAccessor coordinates access to Writers, Readers, and Searchers in a
 * way that allows multiple threads to share the same access objects. Also,
 * Readers and Searchers are refreshed after a Writer is released.
 * 
 * Unless you are batch loading documents, you should get the object, use it,
 * and then return it.
 * 
 * <pre>
 * IndexWriter writer = indexAccessor.getWriter(false, false); &lt;br/&gt;
 * try {&lt;br/&gt;
 *   writer.addDocument(doc);&lt;br/&gt;
 * } finally {&lt;br/&gt;
 *   indexAccessor.release(writer);&lt;br/&gt;
 * }
 * </pre>
 * 
 */
public interface IndexAccessor {
	/**
	 * @return number of threads holding a Searcher
	 */
	int searcherUseCount();

	/**
	 * Releases any resources held by this IndexAccessor.
	 */
	void close();

	/**
	 * Returns an IndexReader. 
	 * 
	 * @throws IOException
	 */
	IndexReader getReader() throws IOException;

	/**
	 * Fetches a double checked Searcher that has been checked for the presence of a reopen file
	 * Note that it may occure that a prioritized Searcher may be reopened twice.
	 * @throws IOException
	 */
	IndexSearcher getPrioritizedSearcher() throws IOException;

	/**
	 * Returns a Searcher.
	 * 
	 * @return new or cached Searcher
	 * @throws IOException
	 */
	IndexSearcher getSearcher() throws IOException;

	/**
	 * Returns a Searcher that uses a supplied IndexReader.
	 * 
	 * @param indexReader to create Searcher with
	 * @return new or cached Searcher
	 * @throws IOException
	 */
	@Deprecated
	IndexSearcher getSearcher(IndexReader indexReader) throws IOException;

	/**
	 * Returns a Searcher that uses a supplied IndexReader and Similarity.
	 * 
	 * @param similarity to create Searcher with
	 * @param indexReader to create Searcher with
	 * @return new or cached Searcher
	 * @throws IOException
	 */
	@Deprecated
	IndexSearcher getSearcher(Similarity similarity, IndexReader indexReader) throws IOException;
	
	/**
	 * Returns a Searcher that uses a Similarity.
	 * 
	 * @param similarity to create Searcher with
	 * @return new or cached Searcher
	 * @throws IOException
	 */
	IndexSearcher getSearcher(Similarity similarity) throws IOException;

	/**
	 * @throws IOException
	 */
	public IndexWriter getWriter() throws IOException;

	boolean isOpen();

	boolean isLocked();

	void open();

	/**
	 * @throws IOException 
	 */
	void reopen() throws IOException;

	int readingReadersOut();

	/**
	 * Release an IndexReader instance if it is not used any longer.
	 * @param reader the Reader that should be released.
	 */
	void release(IndexReader reader);

	/**
	 * @param writer
	 */
	void release(IndexWriter writer);

	/**
	 * @param searcher
	 */
	void release(IndexSearcher searcher);

	int writerUseCount();

	int writingReadersUseCount();

}
