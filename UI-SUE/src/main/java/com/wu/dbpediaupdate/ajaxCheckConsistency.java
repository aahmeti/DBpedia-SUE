package com.wu.dbpediaupdate;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.dbpedia.extraction.InfoboxSandboxCustom;
import org.dbpedia.extraction.WikiDML;

import org.dbpedia.updateresolution.RDFUpdateResolver;

import scala.collection.mutable.ArrayBuffer;


/**
 * Root resource (exposed at "myresource" path)
 */
@Path("/ajaxcheckconsistency")
public class ajaxCheckConsistency {

	@Context
	ServletContext context;

	private static final String PATH_ONTOLOGY = "/WEB-INF/classes/ontology.xml";
	private static final String PATH_DOWNLOADS = "/WEB-INF/downloads";
	private static final String PATH_MAPPINGS = "/WEB-INF/mappings";

	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String getItHTMLFORMsimple(
			@FormParam("postvar[]") List<String> vars,
			@Context HttpServletRequest req,
			@Context HttpServletResponse servletResponse) throws IOException {
		 System.out.println("our vars");
		 System.out.println(vars);

		// retrieve dmls
		Iterator<String> it = vars.iterator();
		
		ArrayBuffer<WikiDML> wikiupdates = new ArrayBuffer<WikiDML>();
		
		while (it.hasNext()) {

			String dmlString = it.next();
			System.out.println("dmlString:"+dmlString);
			String[] severalwikidmls = dmlString.split("###");
			for (int i=0;i<severalwikidmls.length;i++){
				String[] parts = dmlString.split("\\$\\$");
				String wikiPage=null,infobox=null,property=null,oldValue=null,newValue=null,operation=null;
				if (parts.length>0)
					wikiPage = parts[0];
				if (parts.length>1)
					infobox = parts[1];
				if (parts.length>2)
					property = parts[2];
				if (parts.length>3)
					oldValue = parts[3];
				if (parts.length>4)
					newValue = parts[4];
				if (parts.length>5)
					operation = parts[5];
				
				WikiDML wikidml = new WikiDML(wikiPage, infobox, property,
						oldValue, newValue, operation);
				System.out.println("wikidml:"+wikidml.toString());
				
				wikiupdates.$plus$eq(wikidml); //append wikidml
			}
			
		}
		
		String localPath = context.getRealPath(PATH_DOWNLOADS);
		String localPathOntology = context.getRealPath(PATH_ONTOLOGY);
		String localPathMappings = context.getRealPath(PATH_MAPPINGS);
		
		//InfoboxSandboxCustom info = Init.getInfoboxSandboxCustom();
		RDFUpdateResolver resolver = Init.getRDFUpdateResolver();
				//new InfoboxSandboxCustom(null,
				//"_ambig.xml",null,null,null); // this
								// worked
//info.setConfiguration(localPath, localPathOntology,
		//localPathMappings);
//System.out.println("wel set config"); // inserts
		// for
		System.out.println("well created! new InfoboxSandboxCustom"); // inserts
		

		Boolean ret=false;
		try{
			System.out.println("wikiupdates:"+wikiupdates);
			//info.checkConsistency(wikiupdates,null);
			resolver.checkConsistency(wikiupdates,null);
		}
		catch (Exception e){
		  e.printStackTrace(System.err);
		}
		return ret.toString();
	}

}
