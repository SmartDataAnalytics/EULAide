package uni.bonn.eula.controller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import gate.CorpusController;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ANNIEConstants;
import gate.creole.SerialAnalyserController;
import gate.creole.gazetteer.FlexibleGazetteer;
import gate.creole.gazetteer.Gazetteer;
import gate.creole.ontology.DataType;
import gate.creole.ontology.DatatypeProperty;
import gate.creole.ontology.InvalidValueException;
import gate.creole.ontology.Literal;
import gate.creole.ontology.OClass;
import gate.creole.ontology.OConstants;
import gate.creole.ontology.OInstance;
import gate.creole.ontology.OURI;
import gate.creole.ontology.Ontology;
import gate.gui.ontology.OntologyEditor;
import gate.jape.JapeException;
import gate.util.GateException;

public class TermsConditionsApp {
	private CorpusController termsConditions = null;
	

    private FlexibleGazetteer createFlexibleGazetteer(CorpusController rootFinder, Ontology ontforgaz) throws GateException{
 	   
 	   //load the Gazetteer_Ontology_Based
 	   
 	   try {
 	    
 	    Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "Gazetteer_Ontology_Based").toURI().toURL());
 	       
 	   //need Tools plugin for flexible Gazetteer
 	   Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "Tools").toURI().toURL());
 	    
 	   } catch (GateException e) {
 	    // TODO Auto-generated catch block
 	    e.printStackTrace();
 	   } catch (MalformedURLException e) {
 	    // TODO Auto-generated catch block
 	    e.printStackTrace(); 
 	   } 

 	  
 	   FeatureMap params = Factory.newFeatureMap();
 	   params.put("ontology", ontforgaz);
 	   params.put("rootFinderApplication", rootFinder);
 	   System.out.println("Done.....");
 	   Gazetteer ontoRootGazetteer = (Gazetteer)Factory.createResource("gate.clone.ql.OntoRootGaz",params); 
 	   
 	  
 
 	  FeatureMap params2 = Factory.newFeatureMap();
 	  ArrayList<String> inputFeature = new ArrayList<String>();
 	  inputFeature.add("Token.root");
 	  params2.put("gazetteerInst", ontoRootGazetteer);
 	  params2.put("inputFeatureNames",inputFeature);
 	  
 	 FlexibleGazetteer flexibleGazetteer = (FlexibleGazetteer)Factory.createResource("gate.creole.gazetteer.FlexibleGazetteer",params2); 

 	   	  
 	  return flexibleGazetteer;
 
 }
	
	
	
    public  CorpusController createResources (CorpusController rootFinder) throws GateException {
  	  

  	  try {
  	    	  
  	   //Load ANNIE for the Tokeniser and POS Tagger , ...
  	   Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), ANNIEConstants.PLUGIN_DIR).toURL());
	   
  	   //need Tools plugin for the Morphological Analyser 
	   Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "Tools").toURL());

  	   //load Ontology plugin
  	   File pluginHome = new File (new File(Gate.getGateHome(),"plugins"), "Ontology");
  	   Gate.getCreoleRegister().registerDirectories(pluginHome.toURI().toURL());
  	  
  	   
  	  } catch (GateException e) {
  	   // TODO Auto-generated catch block
  	   e.printStackTrace();
  	  } catch (MalformedURLException e) {
  	   // TODO Auto-generated catch block
  	   e.printStackTrace(); 
  	  } 
  	  
  	  String[] processingResources = {
  			"gate.creole.annotdelete.AnnotationDeletePR",  
  	        "gate.creole.tokeniser.DefaultTokeniser",
  			"gate.creole.splitter.SentenceSplitter",   			  
  	        "gate.creole.POSTagger"};
  	  
  	  termsConditions = (CorpusController)Factory.createResource("gate.creole.SerialAnalyserController");
  	   
  	  for(int pr = 0; pr < processingResources.length; pr++) {
  	         System.out.print("\t* Loading " + processingResources[pr] + " ... ");
  	         ((SerialAnalyserController)termsConditions).add((gate.LanguageAnalyser)Factory.createResource(processingResources[pr]));
  	         System.out.println("done");
  	  }

  	  //transfer original markup to default annotation set
		FeatureMap param = Factory.newFeatureMap();
		param.put("copyAnnotations", "true");
		param.put("inputASName", "Original markups");
		param.put("tagASName", "Original markups");
		param.put("textTagName", "paragraph");
		ProcessingResource annotTransferSet =(ProcessingResource) Factory.createResource("gate.creole.annotransfer.AnnotationSetTransfer", param);
	   ((SerialAnalyserController)termsConditions).add(annotTransferSet);
  	  
	   //add my defined Gazetteer
		param = Factory.newFeatureMap();
/*		
  		URL url= getClass().getResource("/gazetteer/lists.def"); 
  		String listsURL = url.getPath();	  		
*/  		
		param.put("listsURL", getClass().getClassLoader().getResource("gazetteer/lists.def").toExternalForm());		
		ProcessingResource annieGaz =(ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer", param);
	   ((SerialAnalyserController)termsConditions).add(annieGaz);
		
		param = Factory.newFeatureMap();
		param.put("rulesFile", getClass().getClassLoader().getResource("improved_plugins/Tools/resources/morph/default.rul").toExternalForm());		
		ProcessingResource morphAnalyzer =(ProcessingResource) Factory.createResource("gate.creole.morph.Morph", param);
		((SerialAnalyserController)termsConditions).add(morphAnalyzer);	   
	   
		
	   // load the Ontology 
		FeatureMap fm = Factory.newFeatureMap ();
		try {
		 fm.put("rdfXmlURL", getClass().getClassLoader().getResource("ODRL21.rdf").toExternalForm());
		 
		} catch (Exception e) {
		 // TODO Auto-generated catch block
		 e.printStackTrace();
		} 		
	    Ontology odrl = (Ontology)Factory.createResource("gate.creole.ontology.impl.sesame.OWLIMOntology",fm);  
	    odrl.setName("ODRL");

/*	    
	    OURI actionURI = odrl.createOURI("http://www.w3.org/ns/odrl/2/Action");
	    OClass actionClass = odrl.getOClass(actionURI);	   
	    DataType aDatatype = new DataType("http://www.w3.org/2001/XMLSchema#string");
	    OURI dataTypeURI = odrl.createOURI("http://www.w3.org/ns/odrl/2/imagePath");
	    Set<OClass> domain = new HashSet<OClass>(); 
	    domain.add(actionClass); 	   
	    odrl.addDatatypeProperty(dataTypeURI, domain, aDatatype);
		addImagePropertyValue(odrl, dataTypeURI);
	    
		File savedOntology = new File("enhancedODRL.rdf");
	    FileOutputStream os;
		try {
			os = new FileOutputStream(savedOntology);
		    odrl.writeOntologyData(os, OConstants.OntologyFormat.RDFXML, false);
		    os.close();	   			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/

	   
	   // add an Ontology Enhancement Transducer
		FeatureMap transducerParam = Factory.newFeatureMap();
		transducerParam.put("ontology",odrl);
		transducerParam.put("grammarURL", getClass().getClassLoader().getResource("ontologyEnhancement.jape").toExternalForm());
		ProcessingResource ontologyEnhancementTransducer = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerParam);
		System.out.print("\t* Loading gate.creole.Transducer" + " ... ");		
	   ((SerialAnalyserController)termsConditions).add(ontologyEnhancementTransducer);
	   System.out.println("done");

	   
	   //add flexible Gazetteer	
	   System.out.print("\t* Loading gate.creole.gazetteer.FlexibleGazetteer" + " ... ");
	   ((SerialAnalyserController)termsConditions).add(createFlexibleGazetteer(rootFinder,odrl));	   
	   System.out.println("done");
	   
	   
	   
	   // add my JAPE rules
		transducerParam = Factory.newFeatureMap();
		transducerParam.put("grammarURL", getClass().getClassLoader().getResource("JAPE/main.jape").toExternalForm()); 		
		ProcessingResource japeTransducer =(ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerParam);
	   ((SerialAnalyserController)termsConditions).add(japeTransducer);
	   
      
  	  termsConditions.setName("terms and Conditions");
  	  return termsConditions;
  }	
    
    private void addImagePropertyValue(Ontology odrl, OURI dataTypeURI) throws JapeException{
		OURI instanceURI = odrl.createOURI("http://www.w3.org/ns/odrl/2/pay");
		OInstance payInstance = odrl.getOInstance(instanceURI);
		DatatypeProperty imagePath = odrl.getDatatypeProperty(dataTypeURI);
		try {
			payInstance.addDatatypePropertyValue(imagePath,  new Literal("https://goo.gl/71iLHj", "en"));
		}
		catch(InvalidValueException e) {
		  throw new JapeException(e);
		}		

    }
	

}
