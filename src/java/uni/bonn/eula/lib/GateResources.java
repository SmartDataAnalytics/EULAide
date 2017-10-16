package uni.bonn.eula.lib;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.*;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;

import uni.bonn.eula.controller.OntoRootApp;
import uni.bonn.eula.controller.TermsConditionsApp;
import uni.bonn.eula.model.*;

import gate.*;
import gate.creole.*;
import gate.gui.MainFrame;
import gate.util.*;
import gate.util.persistence.PersistenceManager;

/**
 * Class to get all data from the document using GATE modules. 
 *
 */
public class GateResources {

	//private SerialAnalyserController serAnCtrlr= null;
	private CorpusController termsConditionPipeLine = null;
	private CorpusController rootFinderPipeLine = null;
    String discoWordSpace;
    

	Corpus corpus = null;
	private static GateResources instance = null;

	private HashSet <String> reqAnnots = new HashSet<String>();
	
	
	
	/**
	 * Singleton constructor to initialize GATE. This takes a couple of seconds,
	 * and we do not want to do this for each instance
	 * @throws GateException
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	protected GateResources(String gateHome) throws GateException, IOException, URISyntaxException{
		//Gate.setGateHome(new File(this.getClass().getResource("/gate/").getPath()));	
		//Gate.setGateHome(new File(getClass().getClassLoader().getResource("gate").getPath()).ex	;
		//URI path = getClass().getClassLoader().getResource("gate").toURI();
		//Gate.setGateHome(new File(path));			
				
		//Gate.setGateHome(new File("C:/gate"));		
/*
		//My GATE resources are in the "/gate" folder of the JAR
		URI url = getClass().getResource("/gate").toURI();
		File gateHome;
		if (url.isOpaque()) {
		    String tempDirectoryPath = FileUtils.getTempDirectoryPath();
		    String gateResource = "gate";
		    //Delete any existing temporary directory
		    FileUtils.deleteDirectory(new File(FilenameUtils.concat(tempDirectoryPath, gateResource)));
		    String gateHomePath = extractDirectoryFromClasspathJAR(getClass(), gateResource, tempDirectoryPath);
		    gateHome = new File(gateHomePath);

		} else {
		    gateHome = new File(url);
		}
*/		
		Gate.setGateHome(new File(gateHome));
		
		
		File GateHome = Gate.getGateHome();
		Gate.setUserConfigFile(new File(GateHome,"gate.xml"));
		File pluginsHome = new File(GateHome,"plugins");
		Gate.setPluginsHome(pluginsHome);
		Gate.init();
		//MainFrame.getInstance().setVisible(true);		
		rootFinderPipeLine = new SerialAnalyserController();			
		termsConditionPipeLine = new SerialAnalyserController();	
		
	}
	
	/**
	 * Generates a singleton instance
	 * @return
	 * @throws GateException
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	

	public static GateResources getInstance(String gateHome) throws GateException, IOException, URISyntaxException{
		if(instance == null){
			instance = new GateResources(gateHome);
		}

		return instance;
	}
	
	
	
	/**
	 * Initialize ANNIE and other processing resources here
	 */
	public void initializePipeLines(String discoWordSpace)
	{
		try {
			
	        OntoRootApp rootFinder = new OntoRootApp();
	        rootFinderPipeLine = rootFinder.createResources();

			TermsConditionsApp mainPipeLine = new TermsConditionsApp();
	        termsConditionPipeLine = mainPipeLine.createResources(rootFinderPipeLine);
	        setDiscoWordSpace(discoWordSpace);
			Out.prln("Processing resources are loaded");
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	private void setDiscoWordSpace(String wordSpace) {
		discoWordSpace = wordSpace;
	}
	
	public String getDiscoWordSpace(){
		return discoWordSpace;
	}

	
	private void setCorpus() {
		termsConditionPipeLine.setCorpus(corpus);             
	}
	public void execute() throws GateException {
		
		termsConditionPipeLine.execute();
		Out.prln("Main Pipe Line Executed.");
	}
	
	/**
	 * Builds corpus with URL
	 * @param u
	 * @throws ResourceInstantiationException
	 */
	public void buildCorpusWithDoc(URL u) throws ResourceInstantiationException{
		corpus = (Corpus) Factory.createResource("gate.corpora.CorpusImpl");
		//corpus.clear();
		FeatureMap params = Factory.newFeatureMap();
		params.put("sourceUrl", u);
		params.put("preserveOriginalContent", new Boolean(false));
		params.put("collectRepositioningInfo", new Boolean(false));
	    params.put("markupAware", new Boolean(true));
	    params.put("encoding", "windows-1252");
		Out.prln("Creating Gate document for " + u);
		Document temp = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);	
		corpus.add(temp);
		setCorpus();
	}
	
	/**
	 * Builds corpus with file
	 * @param file
	 * @throws ResourceInstantiationException
	 * @throws MalformedURLException
	 */
	public void buildCorpusWithDoc(File file) throws ResourceInstantiationException, MalformedURLException{
		corpus = (Corpus) Factory.createResource("gate.corpora.CorpusImpl");
		corpus.clear();
		FeatureMap params = Factory.newFeatureMap();
		URL u = file.toURI().toURL();
		params.put("sourceUrl", u);
		params.put("preserveOriginalContent", new Boolean(true));
		params.put("collectRepositioningInfo", new Boolean(true));
		Out.prln("Creating doc for " + u);
		Document temp = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
		corpus.add(temp);
		setCorpus();
	}
	
	public void setRequiredAnnots(HashSet<String> list){
		reqAnnots.clear();
		reqAnnots.addAll(list);
	}
	/**
	 * Default setter
	 * @return
	 */
	public HashSet<Annotation> getAnnotations(){
		if(corpus.iterator().hasNext()){
			Document doc = corpus.iterator().next();
			return new HashSet<Annotation>(doc.getAnnotations().get(reqAnnots));
		}
		else
			return null;
	}
	/**
	 * Gets specific annotations
	 * @param type
	 * @return
	 */
	public AnnotationSet getAnnotations(String type){
		if(corpus.iterator().hasNext()){
			Document doc = corpus.iterator().next();
			
			return doc.getAnnotations().get(type);
		}
		else
			return null;
	}

	
	/**
	 * Gets string from annotation
	 * @param annot
	 * @return
	 * @throws Exception
	 */
	public String getContentFromAnnot(Annotation annot) throws Exception{	
		//return (String)annot.getFeatures().get(Constants.STRING);
		return corpus.get(0).getContent().getContent(annot.getStartNode().getOffset().longValue(),annot.getEndNode().getOffset().longValue()).toString();
	}
	
	public String getContentByBoundary(long startOffset, long endOffset) throws Exception{	
		//return (String)annot.getFeatures().get(Constants.STRING);
		return corpus.get(0).getContent().getContent(startOffset,endOffset).toString();
	}	

	public void freeResources(){
		corpus.cleanup();
		Factory.deleteResource(corpus);
		
	}
	
	public static class SortedAnnotationList extends Vector {
		public SortedAnnotationList() {
			super();
		} // SortedAnnotationList

		public boolean addSortedExclusive(Annotation annot) {
			Annotation currAnot = null;
			// overlapping check
			for (int i = 0; i < size(); ++i) {
				currAnot = (Annotation) get(i);
				if (annot.overlaps(currAnot)) {
					return false;
				} // if
			} // for
			long annotStart = annot.getStartNode().getOffset().longValue();
			long currStart;
			// insert
			for (int i = 0; i < size(); ++i) {
				currAnot = (Annotation) get(i);
				currStart = currAnot.getStartNode().getOffset().longValue();
				if (annotStart < currStart) {
					insertElementAt(annot, i);
					/*
					 * Out.prln("Insert start: "+annotStart+" at position: "+i+
					 * " size="+size()); Out.prln("Current start: "+currStart);
					 */
					return true;
				} // if
			} // for
			int size = size();
			insertElementAt(annot, size);
			// Out.prln("Insert start: "+annotStart+" at size position: "+size);
			return true;
		} // addSorted
	} // SortedAnnotationList
} // class StandAloneAnnie