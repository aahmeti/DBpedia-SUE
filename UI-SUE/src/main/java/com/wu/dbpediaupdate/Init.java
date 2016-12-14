package com.wu.dbpediaupdate;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.dbpedia.updateresolution.RDFUpdateResolver;
import org.dbpedia.extraction.InfoboxSandboxCustom;

@WebListener
public class Init implements ServletContextListener {

	// @Context
	ServletContext context;
	private static final String PATH_ONTOLOGY = "/WEB-INF/classes/ontology.xml";
	private static final String PATH_DOWNLOADS = "/WEB-INF/downloads";
	private static final String PATH_MAPPINGS = "/WEB-INF/mappings";

	@Inject
	static String startMessage;
	static InfoboxSandboxCustom info;
	static RDFUpdateResolver resolver;

	public Init() {

	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		context = sce.getServletContext();
		System.out.println("let's init");

		String localPath = context.getRealPath(PATH_DOWNLOADS);
		System.out.println("localPath:" + localPath);

		String localPathOntology = context.getRealPath(PATH_ONTOLOGY);
		System.out.println("localPathOntology:" + localPath);
		String localPathMappings = context.getRealPath(PATH_MAPPINGS);
		System.out.println("localPathMappings:" + localPath);

		
		
		info = new InfoboxSandboxCustom(null, ".xml", localPath,
				localPathOntology, localPathMappings);

		resolver = new RDFUpdateResolver(null,".xml", localPath,
				localPathOntology, localPathMappings);

		startMessage = "infoboxSandbox up!";
		// ApplicationContext appCtx =
		// WebApplicationContextUtils.getWebApplicationContext(event.getServletContext());
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub

	}

	public static String getStartMessage() {
		return startMessage;
	}
	public static InfoboxSandboxCustom getInfoboxSandboxCustom() {
		return info;
	}
	public static RDFUpdateResolver getRDFUpdateResolver() {
		return resolver;
	}

}
