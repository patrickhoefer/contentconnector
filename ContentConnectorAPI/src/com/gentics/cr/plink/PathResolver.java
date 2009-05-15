package com.gentics.cr.plink;

import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.cr.CRRequest;

/**
 * @author haymo
 * 
 * This class is used to resolve URLs to objects an vice versa.
 * 
 */
public class PathResolver {

	private Datasource ds = null;

	private Expression expression = null;

	private Logger log;

	/**
	 * Initialize the expression needed to resolve Objects from passed URLs. As
	 * this uses a lot of time initalization in the constructor improves
	 * performance. Initialize PathResolver once on Server startup.
	 * 
	 * @param ds
	 */
	public PathResolver(Datasource ds, String appRule) {

		this.ds = ds;

		this.log = Logger.getLogger(this.getClass().getName());

		// define the rule for finding pages or files with path/filename
		String rule = "object.filename == data.filename && (object.folder_id.pub_dir == data.path || object.folder_id.pub_dir == concat(data.path, '/'))";
		
		
		//Apply AppRule
		if (appRule!=null && !appRule.equals(""))
		{
			rule = "("+rule+") AND "+appRule;
		}
		// parse the rule into an expression object (check for syntax
		// errors)
		try {
			this.expression = ExpressionParser.getInstance().parse(rule);
		} catch (Exception e) {
			this.log.error("Could create expression path rule.");
		}
	}

	/**
	 * The Method looks in the repository with the expression initialized in the
	 * constructor and tries to find a corrisponding object. This only works
	 * correctly when only one node is in the repository, otherwise there may
	 * bee more object with the same URL in the repository.
	 * 
	 * @param url
	 * @return a Resolvalbe Object based on the passed URL.
	 */
	@SuppressWarnings("unchecked")
	public Resolvable getObject(CRRequest request) {
		Resolvable contentObject = null;
		String url = request.getUrl();
		if (url != null) {
			try {
				
				// prepare the filter
				DatasourceFilter filter = ds.createDatasourceFilter(expression);
				
				//Deploy base objects
				Iterator<String> it = request.getObjectsToDeploy().keySet().iterator();
				while(it.hasNext())
				{
					String key = it.next();
					filter.addBaseResolvable(key,request.getObjectsToDeploy().get(key));
				}
				
				// add the data to the filter
				filter.addBaseResolvable("data", new PathBean(url));

				// use the filter to get matching objects
				Collection<Resolvable> objects = ds.getResult(filter,
						request.getAttributeArray());

				Iterator<Resolvable> i = objects.iterator();
				if (i.hasNext()) {
					// we found at least one object, take the first one
					contentObject = i.next();
				}

			} catch (FilterGeneratorException e) {
				this.log.error("Could not create filter with path expression.");
			} catch (ExpressionParserException e) {
				this.log.error("Error while parsing path expression.");
			} catch (DatasourceException e) {
				this.log.error("Datasource error while getting object for url "
						+ url);
			}
		}
		return contentObject;
	}

	/**
	 * initializes a Resolvable Object an calls getPath(Resolvable linkedObject)
	 * 
	 * @param contentid
	 * @return
	 * @see getPath(Resolvable linkedObject)
	 */
	public String getPath(String contentid) {

		Resolvable linkedObject = null;
		try {

			// initialize linked Object
			linkedObject = PortalConnectorFactory.getContentObject(contentid,
					ds);
			return getPath(linkedObject);

		} catch (DatasourceNotAvailableException e) {
			this.log.error("Datasource error generating url for " + contentid);
		}

		// if the linked Object cannot be initialized return a dynamic URL
//		this.log.info("Use dynamic url for " + contentid);
//		return getDynamicUrl(contentid);
		return(null);
	}

	/**
	 * @param linkedObject
	 *            must be a Resolvable
	 * @return a the URL from Gentics Content.Node. This expects the attributes
	 *         filename and folder_id to be set for the passed object and the
	 *         attribute pub_dir to be set for folders.
	 */
	public String getPath(Resolvable linkedObject) {

		String filename = "";
		String pub_dir = "";

		if (linkedObject != null) {

			// get filename from linkedObject
			filename = (String) linkedObject.get("filename");

			// Get folder Object from attribute folder_id and attribute pub_dir
			// from it
			Resolvable folder = (Resolvable) linkedObject.get("folder_id");
			if (folder != null) {
				pub_dir = (String) folder.get("pub_dir");
			}
		}

		// If filename is empty or not set, no need to return an path
		if (filename != null && !"".equals(filename)) {

			return pub_dir + filename;
		} else {

			// return a dynamic URL instead
			this.log.warn("Object " + linkedObject.get("contentid")
					+ " has no filename.");
			//this.log.info("Use dynamic url for "
			//		+ linkedObject.get("contentid"));

			//return getDynamicUrl((String) linkedObject.get("contentid"));
			return(null);
		}
	}

	/**
	 * @param contentid
	 * @return a dynamic URL to suitable for CRServlet.
	 */
	public String getDynamicUrl(String contentid) {

			return "?contentid=" + contentid;
		
	}
}