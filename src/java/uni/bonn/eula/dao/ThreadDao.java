package uni.bonn.eula.dao;

import java.net.URL;
import java.util.ArrayList;

import uni.bonn.eula.model.*;
import uni.bonn.eula.controller.*;
public class ThreadDao {

	
	
	public Summary getSummaryForDoc(URL u) throws Exception{
		OntologyBasedAnnotController OntBaseAnnot = new OntologyBasedAnnotController();
		OntBaseAnnot.buildCorpusExecutePipeline(u);
		Summary summary = OntBaseAnnot.getSummary();
		return summary;
		
	}
	
}
