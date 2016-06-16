package wikiPropertiesRecommendation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class WikiTemplateManager {

	private HashMap<String, WikiTemplate> templates;

	public WikiTemplateManager() {
		/*
		 * LOAD templates
		 */
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader("mappingstats_en.txt"));

			String line;
			// skip until templates
			while (((line = br.readLine()) != null)
					&& (!line.startsWith("templates"))) {
				;
			}
			; // line 127116

			// templates count
			String[] sublines = line.split("\\|");
			String totalTemplates = sublines[1];
			System.out.println("totalTemplates: " + totalTemplates);

			templates = new HashMap<String, WikiTemplate>(); // Structure with
																// statistics of
																// templates

			WikiTemplate currentTemplate = null;
			String nameTemplate = "";
			while ((line = br.readLine()) != null) {
				if (line.startsWith("template")) {
					// save previous template
					if (currentTemplate != null) {
						templates.put(nameTemplate, currentTemplate);
					}
					// start new template
					sublines = line.split("\\|");
					nameTemplate = sublines[1]; // e.g. Template:Infobox
												// basketball club
					currentTemplate = new WikiTemplate();
					currentTemplate.setName(nameTemplate);
				} else if (line.startsWith("count")) {
					sublines = line.split("\\|");
					currentTemplate.setCount(Integer.parseInt(sublines[1]));
				} else if (line.startsWith("properties")) {
					sublines = line.split("\\|");
					currentTemplate.setNumProperties(Integer
							.parseInt(sublines[1]));
				} else if (line.startsWith("p")) {
					sublines = line.split("\\|");
					currentTemplate.addProperty(sublines[1],
							Integer.parseInt(sublines[2]));
				}
			}

			// save last
			if (currentTemplate != null) {
				templates.put(nameTemplate, currentTemplate);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public String recommendTopAlternative(String infobox, Set<String> alternatives){
		WikiTemplate template = templates.get(infobox);
		return template.recommendTopAlternative(alternatives);
	}
	
	WikiTemplate getTemplate (String infobox){
		return templates.get(infobox);
	}
}
