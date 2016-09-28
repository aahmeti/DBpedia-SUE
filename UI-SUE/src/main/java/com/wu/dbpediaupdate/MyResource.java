package com.wu.dbpediaupdate;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.dbpedia.extraction.InfoboxSandboxCustom;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("/")
public class MyResource {
	
	@Context
	ServletContext context;
	
	private static final String PATH_ONTOLOGY = "/WEB-INF/classes/ontology.xml";
	private static final String PATH_DOWNLOADS = "/WEB-INF/downloads";
	private static final String PATH_MAPPINGS = "/WEB-INF/mappings";

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     * @throws IOException 
     * @throws JSONException 
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
    	String localPath = context.getRealPath(PATH_DOWNLOADS);
		String localPathOntology = context.getRealPath(PATH_ONTOLOGY);
		String localPathMappings = context.getRealPath(PATH_MAPPINGS);
	
    
    	//wrp.ContentRDFFormat_Nquads()
    	String testQuery = "prefix owl: <http://www.w3.org/2002/07/owl#> prefix xsd: <http://www.w3.org/2001/XMLSchema#>"+
    			"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+
    			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+
    			"prefix foaf: <http://xmlns.com/foaf/0.1/>"+
    			"prefix dc: <http://purl.org/dc/elements/1.1/>"+
    			"prefix : <http://dbpedia.org/resource/>"+
    			"prefix dbpedia2: <http://en.dbpedia.org/property/>"+
    			"prefix dbpedia: <http://dbpedia.org/>"+
    			"prefix skos: <http://www.w3.org/2004/02/skos/core#>"+
    			"prefix dbo: <http://dbpedia.org/ontology/>"+
    			"DELETE { :Santi_Cazorla dbpedia2:position :Midfielder .}"+
    			"INSERT { :Santi_Cazorla dbpedia2:position :Attacker .}"+
    			"WHERE"+
    			"{"+
    			"}";
    	 //InfoboxSandboxCustom info = new InfoboxSandboxCustom(null,"");
    	InfoboxSandboxCustom info = new InfoboxSandboxCustom(null,"_test.xml");
    	 info.setConfiguration(localPath, localPathOntology,localPathMappings);
    	 info.checkConsistencyFromUpdate(testQuery);
    
    	// info.checkConsistencyFromUpdate(testQuery);
    	 
    	//TestInfoboxSandboxCustomMappings test = new TestInfoboxSandboxCustomMappings();
    	 
    	
        return "Got it!!";
    }
  
}
