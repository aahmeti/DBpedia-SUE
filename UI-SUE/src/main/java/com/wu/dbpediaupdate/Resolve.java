package com.wu.dbpediaupdate;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.apache.commons.lang.StringEscapeUtils.unescapeHtml;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.dbpedia.extraction.destinations.Quad;
import org.dbpedia.extraction.InfoboxSandboxCustom;
import org.dbpedia.extraction.WikiDML;

import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.Seq;
import scala.collection.mutable.ArrayBuffer;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("/get")
@Api(value = "get")
public class Resolve {

	@Context
	ServletContext context;

	private static final String PATH_ONTOLOGY = "/WEB-INF/classes/ontology.xml";
	private static final String PATH_DOWNLOADS = "/WEB-INF/downloads";
	private static final String PATH_MAPPINGS = "/WEB-INF/mappings";

	
	
	@GET
	@Path("/working")
	@Produces("application/json")
	public WikiUpdateResponse simplegetInJSON(
			@QueryParam("query") String query) {
		WikiUpdateResponse cust = new WikiUpdateResponse();
		cust.setQuery(query);
		
		WikiUpdateResponse.WikiUpdate wikiUpdate = cust.new WikiUpdate();
		wikiUpdate.setWikiTitle("Cristiano_Ronaldo");
		WikiUpdateResponse.WikiUpdate.WikiAccomodation wikiAccommodation = wikiUpdate.new WikiAccomodation();
		wikiAccommodation.setAddsDBpedia("<http://dbpedia.org/page/Cristiano_Ronaldo> <http://xmlns.com/foaf/0.1/name> \"El Bicho\"@en . ");
		wikiAccommodation.setDelsDBpedia("<http://dbpedia.org/page/Cristiano_Ronaldo> <http://xmlns.com/foaf/0.1/name> \"Cristiano Ronaldo\"@en . ");
		wikiAccommodation.setAddsWikipedia("ON wikiPage = Cristiano_Ronaldo INSERT InfoboxTemplate(infobox football biography).name = \"El Bicho\"; ");
		
		wikiUpdate.addWikiAccommodation(wikiAccommodation);
		cust.addWikiUpdate(wikiUpdate);
		
		return cust;
	}

	@GET
	@Produces("application/json")
	 @ApiOperation(value = "Resolve SPARQL update and get Wikipedia Updates",
	    notes = "getWikiUpdatesfromQuery",
	    response = WikiUpdateResponse.class,
	    responseContainer = "List")
	public WikiUpdateResponse getWikiUpdatesfromQuery(
			@QueryParam("query") String query,@Context HttpServletRequest req,
			@Context HttpServletResponse servletResponse) throws IOException {
		
		
		/*
		 * Local variables initialization
		 */
		WikiUpdateResponse cust = new WikiUpdateResponse(); // final response
		cust.setQuery(query); // save user query
		String localPath = context.getRealPath(PATH_DOWNLOADS);
		String localPathOntology = context.getRealPath(PATH_ONTOLOGY);
		String localPathMappings = context.getRealPath(PATH_MAPPINGS);
		String ret = "";

		String divjsoninfoboxes = "";
		String originalWikiPage = "";
		String title_wiki = "";
		String initial_title_wiki = "";
		
		
		
		
		/*
		WikiUpdateResponse.WikiUpdate wikiUpdate = cust.new WikiUpdate();
		wikiUpdate.setWikiTitle("Cristiano_Ronaldo");
		WikiUpdateResponse.WikiUpdate.WikiAccomodation wikiAccommodation = wikiUpdate.new WikiAccomodation();
		wikiAccommodation.setAddsDBpedia("<http://dbpedia.org/page/Cristiano_Ronaldo> <http://xmlns.com/foaf/0.1/name> \"El Bicho\"@en . ");
		wikiAccommodation.setDelsDBpedia("<http://dbpedia.org/page/Cristiano_Ronaldo> <http://xmlns.com/foaf/0.1/name> \"Cristiano Ronaldo\"@en . ");
		wikiAccommodation.setAddsWikipedia("ON wikiPage = Cristiano_Ronaldo INSERT InfoboxTemplate(infobox football biography).name = \"El Bicho\"; ");
		
		wikiUpdate.addWikiAccommodation(wikiAccommodation);
		cust.addWikiUpdate(wikiUpdate);
		*/
		
	
	
			
		//System.out.println("let's create InfoboxSandboxCustom");
		try {
			// retrieve a potential previous infobox sandbox from the session
			HttpSession session = req.getSession(true);
			InfoboxSandboxCustom info = (InfoboxSandboxCustom) session
					.getAttribute("InfoboxSandboxCustom");
			if (info == null) {
				// Uncomment to try with a reduced version of mappings
				// InfoboxSandboxCustom info = new InfoboxSandboxCustom(null,
				// "_ambig.xml");
				info = new InfoboxSandboxCustom(null, ".xml");
				info.setConfiguration(localPath, localPathOntology,
						localPathMappings);
				session.setAttribute("InfoboxSandboxCustom", info); //save in session
			}	
			// Uncomment to debug session
			//else { System.out.println("read from Cache!");}
			
			/*
			 * Minor check query
			 */
			if (!query.contains("WHERE")) {
				query = query + "\nWHERE{}";
			}
			
			// Uncomment to debug query
			//System.out.println("query:" + query);



			Tuple2<scala.collection.mutable.ArrayBuffer<scala.collection.mutable.ArrayBuffer<scala.collection.Seq<WikiDML>>>, scala.collection.mutable.ArrayBuffer<String>> update = info
					.updateFromUpdateQuery(query);
			String jscript = "";

			
			scala.collection.mutable.ArrayBuffer<scala.collection.mutable.ArrayBuffer<scala.collection.Seq<WikiDML>>> testSeveralPages = update._1;

			scala.collection.mutable.ArrayBuffer<String> titles = update._2;

			int w = 0;

			ret += "<div id=\"alternativesTriples\">";
			int i = 0;
			Iterator<scala.collection.mutable.ArrayBuffer<scala.collection.Seq<WikiDML>>> itWikiPages = testSeveralPages
					.iterator();
			while (itWikiPages.hasNext()) { // iterate on pages
				
				scala.collection.mutable.ArrayBuffer<scala.collection.Seq<WikiDML>> wikidmls = itWikiPages
						.next();

				// new update
				WikiUpdateResponse.WikiUpdate wikiUpdate = cust.new WikiUpdate();
				
				ret += "<div id=\"triplestab-" + (w + 1) + "\">";
				if (wikidmls != null) {

					/*
					 * GET infobox page for the text comparison
					 */
					try {

						title_wiki = titles.head();
						wikiUpdate.setWikiTitle(title_wiki);
						titles = (ArrayBuffer<String>) titles.tail();

						// get JSON
						// in the Web browser, change format to jsonfm for a
						// pretty version
						// JSONObject json =
						// readJsonFromUrl("https://en.wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&format=json&titles=Santi_Cazorla&rvsection=0");
						JSONObject json = readJsonFromUrl("https://en.wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&format=json&titles="
								+ title_wiki + "&rvsection=0");

						JSONObject jsonPage = json.getJSONObject("query")
								.getJSONObject("pages");
						String page = jsonPage.toString();
						String[] names = JSONObject.getNames(jsonPage);
						if (names.length > 0) {
							String nameAtt = names[0];
							page = jsonPage.getJSONObject(nameAtt)
									.getJSONArray("revisions").getJSONObject(0)
									.get("*").toString();
						}

						originalWikiPage = page.replace("\n", "<br/>");
						
						wikiUpdate.setOriginalInfobox(originalWikiPage);
						/*
						 * .replace("| position ",
						 * "<span style=\"background-color: #FFFF00\">| position </span>"
						 * );
						 */
						// getJSONArray("revisions").getJSONObject(0).get("*").toString().replace("\n",
						// "<br><br/>"));
					} catch (Exception e) {
						originalWikiPage = "<span style=\"background-color: #FFFF00\">Content is temporarily unavailable</span>";
					}
					// divjsoninfoboxes+=originalWikiPage;

					Iterator<Seq<WikiDML>> it = wikidmls.iterator();
					ret += "<div id=\"alternativesDML\">";
					ret += "<div id=\"tabsvertical-"
							+ (w + 1)
							+ "\" class=\"ui-tabs-verticalsmall ui-helper-clearfix\">";

					String tempRetOptions = "<div id=\"tabsOptionsContent\">";
					int initialIndex = i;
					int numoption = 0;
					while (it.hasNext()) { // iterate on options
						numoption++;
						
						
						ArrayList<String> tempInsertProperties = new ArrayList<String>();
						ArrayList<String> tempInsertValues = new ArrayList<String>();
						ArrayList<String> tempDelProperties = new ArrayList<String>();
						ArrayList<String> tempDelValues = new ArrayList<String>();

						Seq<WikiDML> seq = it.next();
						String addsWikipedia = "";
						String delsWikipedia = "";
						scala.collection.Iterator<WikiDML> it2 = seq.iterator();
						// System.out.println("iterate it2");
						String exportDMLs = "";
						while (it2.hasNext()) { // iterate on various wikidml
							WikiDML dml = it2.next();
							String dmlString = dml.toString();
							if (dmlString.contains("INSERT")) { // One could
																// also use the
																// operation
																// property of
																// WikiDML
								//addsWikipedia += dmlString + "<br/>";
								addsWikipedia += dmlString + "\n";
								// System.out.println("dmlString:" + dmlString);
								tempInsertProperties
										.add(getWikiPropertyFromWikiDML(dmlString));
								tempInsertValues
										.add(getWikiValueFromWikiDML(dmlString));
							} else {
								//delsWikipedia += dmlString + "<br/>";
								delsWikipedia += dmlString + "\n";
								tempDelProperties
										.add(getWikiPropertyFromWikiDML(dmlString));
								tempDelValues
										.add(getWikiValueFromWikiDML(dmlString));
							}
							/*
							 * EXPORT WIKIDML TO BE USE IN CHECK CONSISTENCY
							 */
							exportDMLs += dml.exportString() + "###";

						}
						// testonly, add deletion to test it
						/*
						 * String testonly =
						 * "ON wikiPage = Santi_Cazorla\nINSERT InfoboxTemplate(infobox football biography).name = \"Santi Cazorla\";"
						 * ; tempDelProperties.add(getWikiProperty(testonly));
						 * tempDelValues.add(getWikiValue(testonly));
						 */

						// System.out.println("end iterate");
						tempRetOptions += "<div id=\"tabs-" + (i + 1) + "\">";
						// ret += "<div id=\"test-" + (i + 1) + "\">Test</div>";

						// System.out.println("call wikiResult:");
						tempRetOptions += wikiResult();
						// System.out.println("call startTable:");
						tempRetOptions += startTable();
						// System.out.println("call printRow gree:");
						tempRetOptions += printRow(addsWikipedia, "green");
						// System.out.println("call printRow red:");
						tempRetOptions += printRow(delsWikipedia, "red");
						// System.out.println("call closeTable:");
						tempRetOptions += closeTable();

						// System.out.println("call the diff:");
						// System.out.println("seq:"+seq);
						String addsDbpedia = "";
						String delsDbpedia = "";

						if (seq.size() > 0) {
							// Tuple2 tuple =
							// info.getDiffFromInfoboxUpdate_fromUI(seq);
							// getDiffFromInfoboxUpdate_fromUI not needed
							Tuple2 tuple = info.getDiffFromInfoboxUpdate(seq,
									null);

							// System.out.println("tuple is:" +
							// tuple.toString());

							if (tuple._1() != null
									&& !tuple._1().toString()
											.equalsIgnoreCase("List()")) {
								Seq<Quad> quads = (Seq<Quad>) tuple._1();
								Iterator<Quad> itQuad = quads.iterator();
								while (itQuad.hasNext()) {
									Quad quad = itQuad.next();
									//delsDbpedia += parseQuad(quad) + "<br/>";
									delsDbpedia += parseQuad(quad) + "\n";

								}
							}
							if (tuple._2() != null
									&& !tuple._2().toString()
											.equalsIgnoreCase("List()")) {
								Seq<Quad> quads = (Seq<Quad>) tuple._2();
								Iterator<Quad> itQuad = quads.iterator();
								while (itQuad.hasNext()) {
									Quad quad = itQuad.next();
									//addsDbpedia += parseQuad(quad) + "<br/>";
									addsDbpedia += parseQuad(quad)+"\n";
								}
							}
						}

						tempRetOptions += DBpediaResult();
						tempRetOptions += startTable();
						tempRetOptions += printRow(addsDbpedia, "green");
						tempRetOptions += printRow(delsDbpedia, "red");
						tempRetOptions += closeTable();
						tempRetOptions += "<div style=\"padding-top:10px;width:100%; text-align: left\"><a id=\"mailFeedback\" href=\"mailto:albin.ahmeti@wu.ac.at?subject=feedback on query&body=Hi, please check the query:\n\n"
								+ escapeHtml(query)
								+ "\" title=\"Report on query results\"><img src=\"img/mail.png\" style=\"padding-right:10px;\" width=50px>Report on query results</a></div>";

						tempRetOptions += "<div id=\"consistencywikiDMLs-"
								+ (i + 1)
								+ "\" style=\"text-align:right\"> Mark to check consistency <input type=\"checkbox\" name=\"wikidml\" value=\"wikiDMLs-"
								+ (i + 1)
								+ "\"><div style=\"display:none\" id=\"wikiDMLs-"
								+ (i + 1) + "\">" + exportDMLs + "</div></div>";
						/*
						 * UPDATE SCRIPT
						 */
						/*
						 * jscript+="$(\"#tabselect-"+(i +
						 * 1)+"\").click(function(){\n";
						 * jscript+="alert(\"visible!\");\n";
						 * jscript+="$(\"#jsoninfobox-"+(i + 1)+"\").show()\n";
						 * jscript
						 * +="$('#infobox_text').html('tabs-"+(i+1)+"');\n" +
						 * "});\n";
						 */

						tempRetOptions += "</div>"; // close tabs-"+i

						
						
						// create and add new accommodation
						WikiUpdateResponse.WikiUpdate.WikiAccomodation wikiAccommodation = wikiUpdate.new WikiAccomodation(unescapeHtml(addsWikipedia),unescapeHtml(delsWikipedia),unescapeHtml(addsDbpedia),unescapeHtml(delsDbpedia));
						wikiUpdate.addWikiAccommodation(wikiAccommodation);
						
						/*
						 * Apply wiki page changes
						 */
						String currentWikiPage = originalWikiPage;

						// insert new properties
						int posInfobox = currentWikiPage.indexOf("{{Infobox");
						String preInsert = currentWikiPage.substring(0,
								currentWikiPage.indexOf("|", posInfobox));
						// System.out.println("preInsert:" + preInsert);
						String postInsert = currentWikiPage
								.substring(currentWikiPage.indexOf("|",
										posInfobox));
						// System.out.println("postInsert:" + postInsert);
						String insertlines = "";

						/*
						 * TEST
						 */
						/*
						 * for (int j = 0; j < tempInsertProperties.size(); j++)
						 * { System.out.println("tempInsertProperties.get(" + j
						 * + "):" + tempInsertProperties.get(j)); } for (int j =
						 * 0; j < tempInsertValues.size(); j++) {
						 * System.out.println("tempInsertValues.get(" + j + "):"
						 * + tempInsertValues.get(j)); }
						 */

						for (int j = 0; j < tempInsertProperties.size(); j++) {

							insertlines += "<span style=\"background-color: green;\">"
									+ "| "
									+ tempInsertProperties.get(j)
									+ " = "
									+ tempInsertValues.get(j).replace("\"", "")
									+ "</span>" + "<br>";
						}
						currentWikiPage = preInsert + insertlines + postInsert;
						// System.out.println("currentWikiPage:"+currentWikiPage);

						// TODO DEBUG
						// tempDelProperties.add("name");
						// delete properties

						for (int j = 0; j < tempDelProperties.size(); j++) {
							String valueNoQuotes = tempDelValues.get(j)
									.replace("\"", "");
							// System.out.println("searching:"+"\\| "+tempDelProperties.get(j)+
							// " = " + valueNoQuotes+"<br\\/>"); //escape / of
							// <br/>
							// System.out.println("matches:"+currentWikiPage.matches("\\| "+tempDelProperties.get(j)+
							// " = " + valueNoQuotes+"<br\\/>")); //escape / of
							// <br/>
							/*
							 * currentWikiPage =
							 * currentWikiPage.replaceAll("\\| " +
							 * tempDelProperties.get(j) + " = " + valueNoQuotes
							 * + "<br\\/>", ""); // escape
							 */// / of
								// <br/>
							String tooltip = "<span style=\"background-color: red;\" title=\""
									+ tempDelProperties.get(j)
									+ " = "
									+ valueNoQuotes
									+ "\">&nbsp;&nbsp;&nbsp;</span><br\\/>";
							currentWikiPage = currentWikiPage.replaceAll("\\| "
									+ tempDelProperties.get(j) + " += "
									+ valueNoQuotes + "<br\\/>", tooltip); // +=
																			// escape
																			// one
																			// or
																			// more
																			// spaces
																			// escape
																			// /
																			// of
																			// <br/>

						}
						System.out.println("currentWikiPage after:"
								+ currentWikiPage);

						if (i == 0) {
							divjsoninfoboxes += "<div id=\"jsoninfobox-"
									+ (i + 1) + "\">";
						} else {
							divjsoninfoboxes += "<div id=\"jsoninfobox-"
									+ (i + 1) + "\" style=\"display:none\">";
						}
						divjsoninfoboxes += "<img style=\"cursor:pointer;\" title=\"Copy text to your clipboard\" width=30px align=\"middle\" src=\"img/copy.png\" id=\"copyButton-"
								+ (i + 1)
								+ "\"><span id=\"msg-"
								+ (i + 1)
								+ "\"></span><br/>";
						divjsoninfoboxes += currentWikiPage + "</div>";

						i++;
					}
					tempRetOptions += "</div>"; // close tabsOptionsContent

					/*
					 * Write the name of the tabs
					 */
					ret += "<div id=\"tabsOptionsmenu\">";
					ret += "<ul id=\"tabsnum\">";

					for (int n = initialIndex; n < i; n++) {
						ret += "<li><a id=\"tabselect-" + (n + 1)
								+ "\" href=\"#tabs-" + (n + 1) + "\">OPT#"
								+ (n + 1) + "</a></li>\n";
					}
					ret += "</ul>"; // close tabsNum

					ret += "</div>"; // close tabsOptionsmenu

					ret += tempRetOptions;
					ret += "</div>"; // close tabsvertical

					ret += "</div>"; // close alternativesDML

				}
				cust.addWikiUpdate(wikiUpdate);

				ret += "</div>"; // close triplestab

				w += 1;
			}

			/*
			 * END OF ITERATION TRIPLES
			 */
			ret += "<div style=\"text-align:right\"><button id=\"checkconsistency\">Check consitency of selected results</button></div>";

			ret += "</div>"; // close alternativesTriples
			/*
			 * Write the name of the tabs
			 */
			ret += "<ul id=\"tabsTriples\">";

			for (int n = 1; n <= w; n++) {
				ret += "<li><a id=\"tabtriplesselect-" + n
						+ "\" href=\"#triplestab-" + n + "\">TP#" + n
						+ "</a></li>\n";
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

			for (int p = 1; p <= w; p++) {
				jscript += "$(\"#tabsvertical-" + p + "\" ).tabs();\n";

			}

			/*
			 * UPDATE SCRIPT
			 */
			for (int n = 1; n <= i; n++) {
				jscript += "document.getElementById(\"copyButton-"
						+ n
						+ "\").addEventListener(\"click\", function() {"
						+ "copyToClipboardMsg(document.getElementById(\"jsoninfobox-"
						+ n + "\"), \"msg-" + n + "\");});\n";

				jscript += "$(\"#tabselect-" + n + "\").click(function(){\n";
				// jscript+="alert(\"visible!\");\n";
				for (int k = 1; k <= i; k++) {
					if (k != n)
						jscript += "$(\"#jsoninfobox-" + k + "\").hide()\n";
				}
				jscript += "$(\"#jsoninfobox-" + n + "\").show()\n" + "});\n";
				/*
				 * jscript+="$('#infobox_text').html('tabs-"+(i+1)+"');\n" +
				 * "});\n";
				 */
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

	 return cust;
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException,
			JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
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
		String ret = "<table><tr>" + "<td><h3>ADDs</h3></td>"
				+ "<td><h3>DELETEs</h3></td>" + "</tr><tr>";
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
		String ret = "<td valign=\"top\"><div style=\"border-radius: 25px; border: 2px solid "
				+ color
				+ "; padding: 20px; width: 400px; min-height: 150px; word-break: break-all; white-space: normal;\">";
		ret += values;
		ret += "</div></td>";
		return ret;
	}

	public String parseQuad(Quad quad) {
		// System.out.println("my quad is:" + quad);
		String ret = "<" + quad.subject() + "> <" + quad.predicate() + "> ";
		// add quotes if needed
		if (((quad.language() != null) || (quad.datatype() != null))
				&& (!quad.value().startsWith("\""))) {
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
		return line
				.substring(line.indexOf("= ", posPar) + 2, line.length() - 2); // -2
																				// to
																				// get
																				// rid
																				// of
																				// last
																				// ';'
	}
}
