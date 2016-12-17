package com.wu.dbpediaupdate;

import at.tuwien.dbai.rewriter.Stopwatch;

import org.dbpedia.extraction.WikiDML;
import org.dbpedia.extraction.destinations.Quad;
import org.dbpedia.updateresolution.RDFUpdateResolver;
import org.dbpedia.updateresolution.Update;
import org.json.JSONException;
import org.json.JSONObject;
import scala.collection.*;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

/**
 * This is the address of the resource (exposed in the "webapi/" path, specified in web.xml)
 */
@Path("/ajaxupdateSingletonRecode")
public class ajaxupdateSingletonRecode {

	@Context
	ServletContext context;

	/*
	 * The MAX_SAMPLING for the statistics
	 */
	private static final Integer MAX_SAMPLING = 300;
	private static final Integer DEFAULT_SAMPLING = 100;
	
	private static final Boolean DEBUG = false;

	/**
	 * @param query
	 *            The SPARQL query
	 * @param stats
	 *            FORM value indicating if the stats should be shown
	 * @param sample
	 *            The number of sample subject to compute the stats
	 * @param req
	 *            Common HTTP req
	 * @param servletResponse
	 *            Common response
	 * @return a String with the HTML code of the ajax
	 * @throws IOException
	 */
	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String getItHTMLFORM(@FormParam("query") String query, @FormParam("stats") String stats, @FormParam("sample") String sample,
			@Context HttpServletRequest req, @Context HttpServletResponse servletResponse) throws IOException {

		Stopwatch watch = new Stopwatch(); // Count time
		watch.start();
		
		String ret = ""; // Returned String
		String divjsoninfoboxes = "";
		String originalWikiInfobox = ""; //store the original Wikipedia infobox
		String title_wiki = ""; // title of wikipedia page (e.g.Cristiano_Ronaldo)
		String jscript = "";
		int numTabsVertical = 1;
		String insertPropertyDbpedia = null;
		ArrayList<String> titles = new ArrayList<String>();
		int numSubject = 0; //counts the number of subjects
		int numAlternative = 0; //counts the number of alternatives

		// Check "WHERE" clause, and include it if it is not present
		if (!query.contains("WHERE")) {
			query = query + "\nWHERE{}";
		}

		if (DEBUG)
			System.out.println("Query:" + query);

		try {
			// Create main object RDFUpdateResolver
			RDFUpdateResolver resolver = Init.getRDFUpdateResolver();

			// Call resolve method with the provided user query
			// Returns: Subject->Original Triple->Set of Alternative Updates (each update can have several affected properties)
			Map<String, Map<Quad, Set<Update>>> update = resolver.factorUpdateFromUpdateQuery(query);

			if (DEBUG) {
				System.out.println("Update:" + update);
				// Iterate results
				showUpdate(update);
			}

			ret = "<div id=\"alternativesSubjects\">"; // everything is wrapped under a div "alternativesSubjects"

			// iterate on Subjects
			for (String subject : update.keySet()) {
				ret += "<div id=\"subjectstab-" + (numSubject + 1) + "\">"; // each subject is inside a div subjectstab-1, 2, 3...

				title_wiki = subject; //get title of wikipedia page (e.g.Cristiano_Ronaldo)
				// Get rid of the prefix URI if necessary:
				int bar = title_wiki.lastIndexOf('/');
				if (bar != -1) {
					title_wiki = title_wiki.substring(bar + 1);
				}
				titles.add(title_wiki); // add current title to the set of all titles involved in the update
				if (DEBUG)
					System.out.println("title_wiki:" + title_wiki);
				
				// Get map: Original Triple->Set of Alternative Updates (each update can have several affected properties)
				Map<Quad, Set<Update>> wikidmls = update.get(subject);

				if (wikidmls != null) {

					originalWikiInfobox = getOriginalInfobox(title_wiki); //Get the original Infobox of the subject
					
					// Now we iterate on each original triple of the update
					Iterator<Entry<Quad, Set<Update>>> tps = wikidmls.entrySet().iterator();
					ret += "<div id=\"tpsSubject\">"; //each triple is inside a div tpsSubject
					String retTemp = "";
					boolean firstTP = true;
					while (tps.hasNext()) {
						Entry<Quad, Set<Update>> tpWithOriginalText = tps.next();
						Quad qd = tpWithOriginalText.getKey();
						String originalUpdate = qd.subject() + " " + qd.predicate() + " " + qd.value(); //get the original triple pattern update
						Set<Update> tp = tpWithOriginalText.getValue();

						//insert the header where we will list all original triple patterns
						ret += "<a id=\"showtp-" + numTabsVertical + "\" style=\"cursor:pointer;";
						if (firstTP)
							ret += "font-weight:700"; //the first one is highlighted
						ret += "\"> <img src=\"img/click.jpg\" width=5% style=\"vertical-align: middle;\">" + escapeHtml(originalUpdate)
								+ "</a><br/>";
						if (!firstTP)
							ret += "<br/>";
						
						retTemp += "<div id=\"tpHide-" + numTabsVertical + "\""; //each TP is inside a numbered tpHide div
						if (firstTP)
							firstTP = false;
						else
							retTemp += " style=\"display:none\""; //hide all TPs except for the first one
						retTemp += ">";

						retTemp += "<div id=\"alternativesDML\">"; //inside each tpHide, we build a alternativesDML and tabsvertical div 
						retTemp += "<div id=\"tabsvertical-"
						// + (w + 1)
								+ numTabsVertical + "\" class=\"ui-tabs-verticalsmall ui-helper-clearfix\">";

						numTabsVertical++;
						
						String tempRetOptions = "<div id=\"tabsOptionsContent\">"; //these are the options (alternative updates)
						int initialIndex = numAlternative;
						int numoption = 0;

						java.util.Iterator<Update> it = tp.iterator();
						while (it.hasNext()) { // iterate on all the alternative updates
							numoption++;
							ArrayList<String> tempInsertProperties = new ArrayList<String>(); // list of inserted properties, used to provide visualization in the original infobox
							ArrayList<String> tempInsertValues = new ArrayList<String>(); // list of inserted values, used to provide visualization in the original infobox
							ArrayList<String> tempDelProperties = new ArrayList<String>(); // list of deleted properties, used to provide visualization in the original infobox
							ArrayList<String> tempDelValues = new ArrayList<String>(); // list of deleted values, used to provide visualization in the original infobox

							Update seq = it.next();
						
							// Split by adds and deletes
							String addsWikipedia = seq.toString("WikiInsert");
							String delsWikipedia = seq.toString("WikiDelete");

							// FIXME I have to parse the adds and deleted and update the tempInsertProperties, tempInsertValues, tempDelProperties and tempDelValues
							//change the following to allow for insert
							// and delete properties
							String exportDMLs = "";
							/*
							 * while (it2.hasNext()) { // iterate on various // wikidml WikiDML dml = it2.next(); String dmlString = dml.toString();
							 * if (dmlString.contains("INSERT")) { addsWikipedia += dmlString + "<br/>";
							 * 
							 * tempInsertProperties .add(getWikiPropertyFromWikiDML(dmlString)); tempInsertValues
							 * .add(getWikiValueFromWikiDML(dmlString)); } else { delsWikipedia += dmlString + "<br/>"; tempDelProperties
							 * .add(getWikiPropertyFromWikiDML(dmlString)); tempDelValues .add(getWikiValueFromWikiDML(dmlString)); } //EXPORT WIKIDML
							 * TO BE USE IN CHECK CONSISTENCY
							 * 
							 * exportDMLs += dml.exportString() + "###";
							 * 
							 * }
							 */

							// print the div including the content of the alternative
							tempRetOptions += "<div id=\"tabs-" + (numAlternative + 1) + "\">";
							tempRetOptions += wikiResult(); // header of the Wikipedia results
							tempRetOptions += startTable(); 
							tempRetOptions += printRow(addsWikipedia, "green"); //print adds
							tempRetOptions += printRow(delsWikipedia, "red"); //print deletes
							tempRetOptions += closeTable();

							String addsDbpedia = "";
							String delsDbpedia = "";

							// FIXME In the following, we should read the resultant Dbpdia triples, they should be in the update already
							/*
							 * if (seq.size() > 0) { Tuple2 tuple = info.getDiffFromInfoboxUpdate( JavaConversions.asScalaBuffer(seq) .toSeq(), null);
							 * 
							 * if (tuple._1() != null && !tuple._1().toString() .equalsIgnoreCase("List()")) { Seq<Quad> quads = (Seq<Quad>)
							 * tuple._1(); Iterator<Quad> itQuad = quads.iterator(); while (itQuad.hasNext()) { Quad quad = itQuad.next(); delsDbpedia
							 * += parseQuad(quad) + "<br/>";
							 * 
							 * } } if (tuple._2() != null && !tuple._2().toString() .equalsIgnoreCase("List()")) { Seq<Quad> quads = (Seq<Quad>)
							 * tuple._2(); Iterator<Quad> itQuad = quads.iterator(); while (itQuad.hasNext()) { Quad quad = itQuad.next(); addsDbpedia
							 * += parseQuad(quad) + "<br/>";
							 * 
							 * if (insertPropertyDbpedia == null) { insertPropertyDbpedia = quad .predicate(); }
							 * 
							 * } } }
							 */

							tempRetOptions += DBpediaResult(); // header of the Dbpedia results
							tempRetOptions += startTable();
							tempRetOptions += printRow(addsDbpedia, "green"); //print adds
							tempRetOptions += printRow(delsDbpedia, "red"); //print deletes
							tempRetOptions += closeTable();
							
							// print footer in which we ask for feedback
							tempRetOptions += "<div style=\"padding-top:10px;width:100%; text-align: left\"><a id=\"mailFeedback\" href=\"mailto:dbpediaupdate@ai.wu.ac.at?subject=feedback on query&body=Hi, please check the query:\n\n"
									+ escapeHtml(query)
									+ "\" title=\"Report on query results\"><img src=\"img/mail.png\" style=\"padding-right:10px;\" width=50px>Report on query results</a></div>";

							// print footer in which we can check the consistency of the selected alternatives
							tempRetOptions += "<div id=\"consistencywikiDMLs-"
									+ (numAlternative + 1)
									+ "\" style=\"text-align:right\"> Mark to check consistency <input type=\"checkbox\" name=\"wikidml\" value=\"wikiDMLs-"
									+ (numAlternative + 1) + "\"";
							
							if (numoption == 1) {
								tempRetOptions += " checked"; // activate the first one by default
							}

							// we keep and export of the alternatives ina hidden div, which will be send with the request of checking the consistency
							tempRetOptions += "><div style=\"display:none\" id=\"wikiDMLs-" + (numAlternative + 1) + "\">" + exportDMLs + "</div></div>";
							

							tempRetOptions += "</div>"; // close tabs-"+i

							/*
							 * Apply changes to the original infobox
							 */
							String currentWikiPage = applyChangesInfobox(originalWikiInfobox, tempInsertProperties, tempInsertValues,
									tempDelProperties, tempDelValues);
							
							if (DEBUG)
									System.out.println("currentWikiPage after:" + currentWikiPage);

							//divjsoninfoboxes stores the jsoninfobox div with the currentWikipedia Pages
							if (numAlternative == 0) {
								divjsoninfoboxes += "<div id=\"jsoninfobox-" + (numAlternative + 1) + "\">";
							} else {
								divjsoninfoboxes += "<div id=\"jsoninfobox-" + (numAlternative + 1) + "\" style=\"display:none\">";
							}
							divjsoninfoboxes += "<img style=\"cursor:pointer;\" title=\"Copy text to your clipboard\" width=30px align=\"middle\" src=\"img/copy.png\" id=\"copyButton-"
									+ (numAlternative + 1) + "\"><span id=\"msg-" + (numAlternative + 1) + "\"></span><br/>";
							divjsoninfoboxes += currentWikiPage + "</div>";

							numAlternative++; // increment number of alternatives
						}

						tempRetOptions += "</div>"; // close tabsOptionsContent

						/*
						 * COMPUTE STATISTICS IF IT IS REQUIRED
						 */
						// FIXME In the next IF, it should also be: insertPropertyDbpedia!= null but I removed it until we have the Dbpedia output integrated in the patterns
						if (stats != null && stats.equalsIgnoreCase("true")) {
							retTemp += getStatistics(sample, title_wiki, resolver, tp);
						}
						/*
						 * Write the name of the tabs
						 */
						retTemp += "<div id=\"tabsOptionsmenu\">";
						retTemp += "<ul id=\"tabsnum\">";

						for (int n = initialIndex; n < numAlternative; n++) {
							retTemp += "<li><a id=\"tabselect-" + (n + 1) + "\" href=\"#tabs-" + (n + 1) + "\">OPT#" + (n + 1) + "</a></li>\n";
						}
						retTemp += "</ul>"; // close tabsNum

						retTemp += "</div>"; // close tabsOptionsmenu

						retTemp += tempRetOptions;
						retTemp += "</div>"; // close tabsvertical

						retTemp += "</div>"; // close alternativesDML
						retTemp += "</div>"; // close tpHide

					}
					ret += retTemp + "</div>"; // close tpsSubject;

				}

				ret += "</div>"; // close subjectstab

				numSubject += 1;
			} // END OF ITERATION TRIPLES

			
			ret += "<div style=\"text-align:right\"><button id=\"checkconsistency\">Check consitency of selected results</button><div id=\"resultConsistency\" style=\"margin-top:10px\"></div></div>";

			ret += "</div>"; // close alternativesTriples
			/*
			 * Write the name of the tabs
			 */
			ret += "<ul id=\"tabsTriples\">";

			for (int n = 1; n <= numSubject; n++) {
				ret += "<li><a id=\"tabtriplesselect-" + n + "\" href=\"#subjectstab-" + n + "\">" + titles.get(n - 1) + "</a></li>\n";
			}
			ret += "</ul>"; // close tabsNum

			/*
			 * print infoboxes
			 */
			ret += "<div id=\"jsoninfoboxes\">" + divjsoninfoboxes + "</div>";

			ret += "<div id=\"js2\" style=\"display:none;\" >"
			// + "<script id=\"js\">\n"
					+ "$(document).ready(function() {\n";
			// ret+="alert(\"test\");\n";

			// for (int p = 1; p <= w; p++) {
			for (int p = 1; p <= numTabsVertical; p++) {
				jscript += "$(\"#tabsvertical-" + p + "\" ).tabs();\n";
				System.out.println("\n*****************Doing tabs: " + p + "\n");

				// add show/hide functionality
				jscript += "document.getElementById(\"showtp-" + p + "\").addEventListener(\"click\", function() {\n";
				jscript += "$('#showtp-" + p + "').css( 'font-weight', '700' );";
				for (int k = 1; k <= numTabsVertical; k++) {
					if (k != p) {
						jscript += "$(\"#tpHide-" + k + "\").hide();\n";
						jscript += "$('#showtp-" + k + "').css( 'font-weight', '400' );";
					}
				}
				jscript += "$(\"#tpHide-" + p + "\").show();\n" + "});\n";
			}
			System.out.println("jscript so far:" + jscript);

			/*
			 * UPDATE SCRIPT
			 */
			for (int n = 1; n <= numAlternative; n++) {
				jscript += "document.getElementById(\"copyButton-" + n + "\").addEventListener(\"click\", function() {"
						+ "copyToClipboardMsg(document.getElementById(\"jsoninfobox-" + n + "\"), \"msg-" + n + "\");});\n";

				jscript += "$(\"#tabselect-" + n + "\").click(function(){\n";
				// jscript+="alert(\"visible!\");\n";
				for (int k = 1; k <= numAlternative; k++) {
					if (k != n)
						jscript += "$(\"#jsoninfobox-" + k + "\").hide()\n";
				}
				jscript += "$(\"#jsoninfobox-" + n + "\").show()\n" + "});\n";
				/*
				 * jscript+="$('#infobox_text').html('tabs-"+(i+1)+"');\n" + "});\n";
				 */

				// for the visibility of the stats
				// jscript+="$( \"#showstats\" ).click(function() {\n"+
				// "$( \"#statsTable\").toggle();\n"+
				// "});\n";
			}

			ret += jscript;
			ret += "});\n"
			// ret+="</script>comon"
					+ "</div>";

			// return title of wikipage (to get the wikipedia infobox in
			// javascript
			ret += "<div id=\"title_wiki\">" + title_wiki + "</div>";
			System.out.println("ret:" + ret);

		} catch (Exception e) {
			System.out.println("except:" + e);
			e.printStackTrace(System.out);
		}

		watch.stop();
		System.out.println("Time:" + watch.getTime());

		return ret;
	}

	/**
	 * @param sample
	 * @param title_wiki
	 * @param resolver
	 * @param retTemp
	 * @param tp
	 * @return
	 */
	private String getStatistics(String sample, String title_wiki, RDFUpdateResolver resolver, Set<Update> tp) {
		String retTemp="";
		String insertPropertyDbpedia;
		// && insertPropertyDbpedia != null) {

		// prepare statistics 
		int sampling = DEFAULT_SAMPLING;
		if (sample != null) {
			try {
				sampling = Integer.parseInt(sample);
				if (sampling > MAX_SAMPLING) {
					sampling = MAX_SAMPLING;
				}
			} catch (NumberFormatException e) {
				sampling = DEFAULT_SAMPLING;
			}
		}
		scala.collection.mutable.HashMap<String, Object> statOutput = new scala.collection.mutable.HashMap<>();
		// FIXME I need the insertPropertyDbpedia SO far I use insertPropertyDbpedia= ? meaning everything
		// until we have the Dbpedia output integrated in the patterns
		insertPropertyDbpedia = "?";
		statOutput = resolver.getStatResultsAlternativesfromUpdate("<http://dbpedia.org/resource/" + title_wiki + ">",
				insertPropertyDbpedia, sampling, tp);

		retTemp += "<div style=\"margin-bottom:10px\">";
		
		if (statOutput.size() > 0) {
			scala.collection.Iterator<String> itprops = statOutput.keysIterator();

			retTemp += "<table id=\"statsTable\" style=\"margin-left:150px;\"><tr><td colspan=2 bgcolor=lightgrey> Presence of alternative wikipedia properties producing '"
					+ insertPropertyDbpedia + "'</td><tr>";

			while (itprops.hasNext()) {
				String props = itprops.next();
				Integer value = Integer.parseInt(statOutput.get(props).toString().replace("(", "").replace(")", "")
						.replace("Some", ""));
				// erase the infobox info
				props = props.substring(props.indexOf(':') + 1);
				int length = 0;
				String per = "0 %";
				if (value != 0) {
					length = 400 * (sampling / value);
					int percentage = (sampling / value) * 100;
					per = percentage + " %";
				}

				retTemp += "<tr><td>" + props + ":</td><td><table><tr><td bgcolor=\"" + getRandomColor()
						+ "\" height=\"10px\" width=\"" + length + "px\" title=\"" + per + "\"></td></tr></table></td></tr>";
			}
			retTemp += "</table>";
		}

		// ret+=statOutput.toString();
		retTemp += "</div>";
		return retTemp;
	}

	/**
	 * Apply changes to the originalInfobox
	 * @param originalWikiInfobox original text of the infobox
	 * @param tempInsertProperties //inserted wikipedia properties
	 * @param tempInsertValues //inserted wikipedia values
	 * @param tempDelProperties //deleted wikipedia properties
	 * @param tempDelValues //deleted wikipedia values
	 * @return text with the changes applied
	 */
	private String applyChangesInfobox(String originalWikiInfobox, ArrayList<String> tempInsertProperties, ArrayList<String> tempInsertValues,
			ArrayList<String> tempDelProperties, ArrayList<String> tempDelValues) {
		String currentWikiPage = originalWikiInfobox;

		// insert new properties
		int posInfobox = currentWikiPage.indexOf("{{Infobox");
		String preInsert = currentWikiPage.substring(0, currentWikiPage.indexOf("|", posInfobox));
		String postInsert = currentWikiPage.substring(currentWikiPage.indexOf("|", posInfobox));
		String insertlines = "";

		

		for (int j = 0; j < tempInsertProperties.size(); j++) {

			insertlines += "<span style=\"background-color: green;\">" + "| " + tempInsertProperties.get(j) + " = "
					+ tempInsertValues.get(j).replace("\"", "") + "</span>" + "<br>";
		}
		currentWikiPage = preInsert + insertlines + postInsert;


		// delete properties
		//FIXME not sure this is working	
		for (int j = 0; j < tempDelProperties.size(); j++) {
			String valueNoQuotes = tempDelValues.get(j).replace("\"", "");
			// System.out.println("searching:"+"\\| "+tempDelProperties.get(j)+
			// " = " + valueNoQuotes+"<br\\/>"); //escape /
			// of
			// <br/>
			// System.out.println("matches:"+currentWikiPage.matches("\\| "+tempDelProperties.get(j)+
			// " = " + valueNoQuotes+"<br\\/>")); //escape /
			// of
			// <br/>
			/*
			 * currentWikiPage = currentWikiPage.replaceAll("\\| " + tempDelProperties.get(j) + " = " + valueNoQuotes + "<br\\/>",
			 * ""); // escape
			 */// / of
				// <br/>
			String tooltip = "<span style=\"background-color: red;\" title=\"" + tempDelProperties.get(j) + " = " + valueNoQuotes
					+ "\">&nbsp;&nbsp;&nbsp;</span><br\\/>";
			currentWikiPage = currentWikiPage.replaceAll("\\| " + tempDelProperties.get(j) + " += " + valueNoQuotes + "<br\\/>",
					tooltip); // +=
								// escape one or more spaces escape / of <br/>

		}
		return currentWikiPage;
	}

	/**
	 * Get the original Infobox of a wikipedia page
	 * @param title_wiki
	 * @return
	 */
	private String getOriginalInfobox(String title_wiki) {
		String originalWikiInfobox;
		/*
		 * GET the real and actual infobox page
		 */
		try {

			// Uncomment just to debug and see the JSON, change format to jsonfm for a pretty version
			// JSONObject json =
			// readJsonFromUrl("https://en.wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&format=json&titles=Santi_Cazorla&rvsection=0");
			JSONObject json = readJsonFromUrl("https://en.wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&format=json&titles="
					+ title_wiki + "&rvsection=0");

			JSONObject jsonPage = json.getJSONObject("query").getJSONObject("pages");
			String page = jsonPage.toString();
			String[] names = JSONObject.getNames(jsonPage);
			if (names.length > 0) {
				String nameAtt = names[0];
				page = jsonPage.getJSONObject(nameAtt).getJSONArray("revisions").getJSONObject(0).get("*").toString();
			}
			//store the original wikipedia infobox
			originalWikiInfobox = page.replace("\n", "<br/>");
			
		} catch (Exception e) {
			originalWikiInfobox = "<span style=\"background-color: #FFFF00\">Content is temporarily unavailable</span>";
		}
		return originalWikiInfobox;
	}

	/**
	 * Auxiliary method to show the current update
	 * 
	 * @param update
	 */
	private void showUpdate(Map<String, Map<Quad, Set<Update>>> update) {
		Set<Entry<String, Map<Quad, Set<Update>>>> it1 = update.entrySet();
		Iterator<Entry<String, Map<Quad, Set<Update>>>> itEntry = it1.iterator();
		while (itEntry.hasNext()) {
			Entry<String, Map<Quad, Set<Update>>> entry = itEntry.next();
			System.out.println("Subject:" + entry.getKey());
			Map<Quad, Set<Update>> updates = entry.getValue();
			Iterator<Entry<Quad, Set<Update>>> ups = updates.entrySet().iterator();
			while (ups.hasNext()) {
				Entry<Quad, Set<Update>> u = ups.next();
				System.out.println("The original quad: " + u.getKey().toString());
				Iterator<Update> options = u.getValue().iterator();
				while (options.hasNext()) {
					Update upd = options.next();
					System.out.println("The updates: " + upd.toString());
					System.out.println("The pattern: " + upd.pattern().toString());
				}
			}
		}
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			// System.out.println("jsontext>" + jsonText);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	private String startTable() {
		String ret = "<table><tr>" + "<td><h3>ADDs</h3></td>" + "<td><h3>DELETEs</h3></td>" + "</tr><tr>";
		return ret;
	}

	private String closeTable() {
		return "</tr></table>";
	}

	private String wikiResult() {
		String ret = "";
		ret += "<h2><img src=\"img/wikipedia_logo_detail.gif\" width=\"75px\" align=\"middle\" style=\"padding-right: 10px\"></img>WIKIPEDIA RESULTS</h2>";
		return ret;
	}

	private String DBpediaResult() {
		String ret = "";
		// <div style=\"margin-top: 50px;\">
		ret += "<h2><img src=\"img/dbpedia.png\" width=\"75px\" align=\"middle\" style=\"padding-right: 10px\"></img>DBPEDIA RESULTS</h2>";

		return ret;
	}

	private String printRow(String values, String color) {
		// #73AD21
		String ret = "<td valign=\"top\"><div style=\"border-radius: 25px; border: 2px solid " + color
				+ "; padding: 20px; width: 350px; min-height: 150px; word-break: break-all; white-space: normal;\">";
		ret += values;
		ret += "</div></td>";
		return ret;
	}

	public String parseQuad(Quad quad) {
		// System.out.println("my quad is:" + quad);
		String ret = "<" + quad.subject() + "> <" + quad.predicate() + "> ";
		// add quotes if needed
		if (((quad.language() != null) || (quad.datatype() != null)) && (!quad.value().startsWith("\""))) {
			ret += "\"" + quad.value() + "\"";
		} else
			ret += quad.value();
		if (quad.language() != null) {
			ret += "@" + quad.language();
		} else if (quad.datatype() != null) {
			ret += "^^<" + quad.datatype() + ">";
		} // according to
			// https://www.w3.org/TR/turtle/#grammar-production-RDFLiteral,
			// language and datatype are mutually exclusive.
		ret += " .\n";
		return escapeHtml(ret);

	}

	public String getWikiPropertyFromWikiDML(String line) {
		int posPar = line.indexOf(").");
		return line.substring(posPar + 2, line.indexOf(" =", posPar));
	}

	public String getWikiValueFromWikiDML(String line) {
		int posPar = line.indexOf(").");
		return line.substring(line.indexOf("= ", posPar) + 2, line.length() - 2); // -2
																					// to
																					// get
																					// rid
																					// of
																					// last
																					// ';'
	}

	public String getRandomColor() {
		Random rand = new Random();
		float r = rand.nextFloat();
		float g = rand.nextFloat();
		float b = rand.nextFloat();
		// Color randomColor = new Color(r, g, b);

		return "rgb(" + r + "," + g + "," + b + ")";
	}
}
