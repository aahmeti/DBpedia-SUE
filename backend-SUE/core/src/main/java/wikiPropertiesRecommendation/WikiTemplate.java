package wikiPropertiesRecommendation;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

public class WikiTemplate {

	public int count;
	public int numProperties;
	public String name;
	public TreeMap<String,Integer> properties;
	
	public WikiTemplate(){
		properties = new TreeMap<>();
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getNumProperties() {
		return numProperties;
	}

	public void setNumProperties(int numProperties) {
		this.numProperties = numProperties;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TreeMap<String, Integer> getProperties() {
		return properties;
	}

	public void setProperties(TreeMap<String, Integer> properties) {
		this.properties = properties;
	}
	
	public void addProperty(String property, int count){
		this.properties.put(property,count);
	}
	public int getProperty(String property){
		if (this.properties.get(property)!=null){
			return this.properties.get(property);
		}
		else
			return -1;
	}
	
	
	/**
	 * Recommend the most frequent property from a set of alternatives
	 * @param alternatives
	 * @return name of the most frequent alternative
	 */
	public String recommendTopAlternative(Set<String> alternatives){
		
		TreeMap<Integer,String> alternativeProperties = new TreeMap<Integer,String>(Collections.reverseOrder());
		
		Iterator<String> propIt = alternatives.iterator();
		while (propIt.hasNext()){
			String prop = propIt.next();
			if (this.properties.get(prop)==null){
				alternativeProperties.put(-1, prop);
			}
			else{
				alternativeProperties.put(this.properties.get(prop), prop);
				System.out.println("       trace, "+prop+":"+this.properties.get(prop));
			}
		}
		return alternativeProperties.firstEntry().getValue();
		
	}
	
}
