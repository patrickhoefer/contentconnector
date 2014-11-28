package com.gentics.cr.lucene.search.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;

import com.gentics.cr.CRConfig;
import com.gentics.cr.CRConfigUtil;
import com.gentics.cr.CRRequest;
import com.gentics.cr.configuration.GenericConfiguration;
import com.gentics.cr.lucene.indexaccessor.IndexAccessor;
import com.gentics.cr.lucene.indexer.index.LuceneIndexLocation;

/**
 * The SynonymQueryParser change the users Query, if there are any synonyms for
 * his searchTerm.
 * 
 * @author Patrick Höfer
 *         <p.hoefer@gentics.com>
 */
public class SynonymQueryParser extends CRQueryParser {

	/**
	 * maximum count of fetched Synonyms.
	 */
	private static final int MAX_SYNONYMS = 20;

	/**
	 * 
	 * GenericConfiguration object of the factory.
	 */
	private GenericConfiguration config;

	/**
	 * static log4j {@link Logger} to log errors and debug.
	 */
	private static Logger log = Logger.getLogger(SynonymQueryParser.class);

	/**
	 * sub Query Parser, which is used as "super QueryParser".
	 */
	private QueryParser childQueryParser;

	private static ConcurrentHashMap<String, CRConfig> indexmap;

	/**
	 * Constructor.
	 * 
	 * @param pconfig
	 *            Generic Configuration object
	 * @param version
	 *            Version
	 * @param searchedAttributes
	 *            All searched Attributes
	 * @param analyzer
	 *            Analyzer
	 * @param crRequest
	 *            CRRequest
	 */
	public SynonymQueryParser(final GenericConfiguration pconfig,
			Version version, final String[] searchedAttributes,
			Analyzer analyzer, CRRequest crRequest) {
		super(version, searchedAttributes, analyzer, crRequest);
		this.config = pconfig;
		// get SubqueryParser if available
		this.childQueryParser = CRQueryParserFactory.getConfiguredParser(
				searchedAttributes, analyzer, crRequest, new CRConfigUtil(
						pconfig, "Subconfig"));
	}

	/**
	 * parse the query for lucene.
	 * 
	 * @param query
	 *            as {@link String}
	 * @return parsed lucene query
	 * @throws ParseException
	 *             when the query cannot be successfully parsed
	 */
	public final Query parse(final String query) throws ParseException {
		String crQuery = query;

		crQuery = replaceBooleanMnoGoSearchQuery(crQuery);
		if (getAttributesToSearchIn().size() > getOne()) {
			crQuery = addMultipleSearchedAttributes(crQuery);
		}
		crQuery = addWildcardsForWordmatchParameter(crQuery);
		crQuery = replaceSpecialCharactersFromQuery(crQuery);

		try {
			Query resultQuery = childQueryParser.parse(crQuery);
		
			resultQuery = childQueryParser.parse(includeSynonyms(super.parse(
					crQuery).toString()));
			
			return resultQuery;
		} catch (IOException e) {
			log.debug("Error while adding synonyms to query.", e);
		} catch (ParseException e){
			log.debug("Error while parsing query");
		}
		
		return super.parse(crQuery);
	}

	public void fetchSynonymsFromIndex(IndexSearcher synonymSearcher,
			IndexReader synonymReader,
			HashMap<String, HashSet<String>> synonymlookup, String queryString,
			String searchTerm, String synonym) throws Exception {
		Query querySynonym = super.parse(queryString);
		log.debug("Synonym Query String: " + querySynonym.toString());
		TopDocs docs = synonymSearcher.search(querySynonym, MAX_SYNONYMS);
		log.debug("total found synonyms: " + docs.totalHits);
		HashSet<String> synonyms = null;
		if(synonymlookup.get(searchTerm) != null){
			synonyms = synonymlookup.get(searchTerm);
		}
		else{
			synonyms = new HashSet<String>();
		}
		for (ScoreDoc doc : docs.scoreDocs) {
			Document d = synonymReader.document(doc.doc);
			synonyms.add(d.get(synonym));
		}
		log.debug("put "+searchTerm+" length: "+synonyms.size());
		
		synonymlookup.put(searchTerm, synonyms);
	}

	/**
	 * look for synonyms in specified Synonymlocation. add the synonyms to
	 * search query
	 * 
	 * @param query
	 *            the search query, before the synonyms are added
	 * @return searchQuery as String, with added synonyms
	 * @throws IOException
	 *             when theres a problem with accessing the Index
	 */
	public final String includeSynonyms(String query) throws IOException {
		IndexAccessor ia;
		IndexSearcher synonymSearcher;
		IndexReader synonymReader;

		log.debug("Synonym QueryParser Start Query: " + query);
		query = " "+query;

		GenericConfiguration autoConf = (GenericConfiguration) config
				.get("synonymlocation");
		boolean useSynonymAsDeskriptorToo = false;
		useSynonymAsDeskriptorToo = Boolean.parseBoolean((String) autoConf
				.get("useSynonymAsDeskriptorToo"));
		log.debug((String) autoConf.get("useSynonymAsDeskriptorToo"));

		CRConfig crconfig = null;
		String name = (String) autoConf.get("indexExtensionName");
		if (name == null) {
			name = "synonym";
		}
		log.debug("extensionname: " + name);
		if (indexmap == null) {
			indexmap = new ConcurrentHashMap<String, CRConfig>();
			log.debug("indexmap was null");
		}
		crconfig = (CRConfig) indexmap.get(name);
		if (crconfig == null) {
			log.debug("crconfig was null");
			CRConfig newConf = new CRConfigUtil(autoConf, "synonymlocation");
			crconfig = (CRConfig) indexmap.put(name, newConf);
			crconfig = newConf;
		}


		LuceneIndexLocation synonymLocation = LuceneIndexLocation
				.getIndexLocation(crconfig);
		ia = synonymLocation.getAccessor();
		synonymSearcher = (IndexSearcher) ia.getPrioritizedSearcher();
		synonymReader = ia.getReader();

		try {
			String subquery = query;
			ArrayList<QueryElement> searchedTerms = new ArrayList<QueryElement>();
			while (true) {
				String searchAttribute = "";

				int pos1 = subquery.indexOf(":");
				if (pos1 == -1) {
					break;
				}
				searchAttribute = subquery.substring(0, pos1);

				if (searchAttribute.lastIndexOf(" ") != -1) {
					searchAttribute = searchAttribute.substring(searchAttribute
							.lastIndexOf(" ") + 1);
				}
				searchAttribute = searchAttribute.replaceAll("\\(", "")
						.replaceAll("\\)", "").replaceAll("\\+", "");

				subquery = subquery.substring(pos1);

				int pos2 = subquery.indexOf(" ");
				if (pos2 == -1) {
					pos2 = subquery.indexOf(")");
				}
				String searchTerm = subquery.substring(1, pos2);
				while(searchTerm.lastIndexOf(")") == searchTerm.length()-1){
					searchTerm = searchTerm.substring(0,searchTerm.length()-1);
				}

				subquery = subquery.substring(pos2);
				searchedTerms
						.add(new QueryElement(searchTerm, searchAttribute));
			}

			HashMap<String, HashSet<String>> synonymlookup = new HashMap<String, HashSet<String>>();

			Iterator<QueryElement> it = searchedTerms.iterator();
			String queryString = "";
			boolean proceed = true;
			while (it.hasNext()) {
				String searchTerm = it.next().getSearchTerm();
				try {
					queryString = "Deskriptor:("
							+ searchTerm.replaceAll("\\*", "")
									.replaceAll("\\(", "")
									.replaceAll("\\)", "") + ")";

					fetchSynonymsFromIndex(synonymSearcher, synonymReader,
							synonymlookup, queryString, searchTerm, "Synonym");
					log.debug("useSynonymAsDeskriptorToo "
							+ useSynonymAsDeskriptorToo);
					if (true) {
						queryString = "Synonym:"
								+ searchTerm.replaceAll("\\*", "")
										.replaceAll("\\(", "")
										.replaceAll("\\)", "");
						fetchSynonymsFromIndex(synonymSearcher, synonymReader,
								synonymlookup, queryString, searchTerm,
								"Deskriptor");
					}

				} catch (ParseException e) {
					log.debug(
							"Error while parsing query for accessing the synonym Index.",
							e);
					proceed = false;
				}
			}

			if(proceed){
				Iterator<QueryElement> it2 = searchedTerms.iterator();
	
				log.debug(query);
				while (it2.hasNext()) {
					QueryElement queryElement = it2.next();
					log.debug(queryElement.toString());
					String searchAttribute = queryElement.getSearchAttribute();
					String searchTerm = queryElement.getSearchTerm();
					log.debug("search for: "+searchTerm+"---");
					HashSet<String> synonyms = synonymlookup.get(searchTerm);
					log.debug(synonyms.size());
					if (synonyms != null && synonyms.size() > 0) {
						String queryadd = "";
						Iterator<String> it3 = synonyms.iterator();
						while (it3.hasNext()) {
							queryadd = " OR " + searchAttribute + ":" + it3.next()
									+ "";
						}
						String regex = "([+( ])"+Pattern.quote(queryElement.toString());
										
						log.debug("regex -- "+regex);
						log.debug("befor -- "+query);
						query = query.replaceAll(regex, "$1("
								+ queryElement.toString() + queryadd + ")");
						log.debug("after -- "+query);
						
					}
	
				}
	
				log.debug("Synonym QueryParser Result Query: " + query);
			}
		} catch (Exception e) {
			return query;
		} finally {
			ia.release(synonymSearcher);
			ia.release(synonymReader);
		}

		return query;
	}

	public class QueryElement implements Comparable<Object> {
		String searchTerm;
		String searchAttribute;

		public QueryElement(String searchTerm, String searchAttribute) {
			this.searchTerm = searchTerm;
			this.searchAttribute = searchAttribute;
		}

		public String getSearchTerm() {
			return searchTerm;
		}

		public String getSearchAttribute() {
			return searchAttribute;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof QueryElement)) {
				return false;
			}

			if (this.getSearchTerm().equals(((QueryElement) o).getSearchTerm())
					&& this.getSearchAttribute().equals(
							((QueryElement) o).getSearchAttribute())) {
				return true;
			}
			return false;
		}

		@Override
		public int compareTo(Object o) {
			return 0;
		}

		public String toString() {
			return getSearchAttribute() + ":" + getSearchTerm() + "";
		}

	}

}
