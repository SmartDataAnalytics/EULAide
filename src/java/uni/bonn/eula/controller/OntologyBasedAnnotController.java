package uni.bonn.eula.controller;
import java.util.*;
import java.util.function.ToIntBiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;


import org.apache.lucene.index.CorruptIndexException;
import org.json.simple.JSONObject;

import uni.bonn.eula.clustering.AverageLinkageStrategy;
import uni.bonn.eula.clustering.Cluster;
import uni.bonn.eula.disco.CorruptConfigFileException;
import uni.bonn.eula.disco.DISCO;
import uni.bonn.eula.disco.TextSimilarity;
import uni.bonn.eula.disco.DISCO.SimilarityMeasure;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.ling.CoreLabel;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.text.DecimalFormat;

import gate.*;
import gate.util.GateException;
import gate.util.Out;
import uni.bonn.eula.model.*;
import uni.bonn.eula.clustering.ClusteringAlgorithm;
import uni.bonn.eula.clustering.DefaultClusteringAlgorithm;
import uni.bonn.eula.lib.*;
import uni.bonn.eula.lib.GateResources.SortedAnnotationList;

public class OntologyBasedAnnotController {	
	ArrayList<HashMap<String, String>> paragraphsCorefInfo; 
	private GateResources gr;
	private StanfordController sCtrl;
	private StanfordResources sr;	
	private String discoWordSpace;
	private DISCO disco;
	private static final double threshold = 0.3;
	
	public OntologyBasedAnnotController() throws Exception{
		gr = GateResources.getInstance("");
		sr = StanfordResources.getInstance();
		sCtrl = new StanfordController();
		discoWordSpace = gr.getDiscoWordSpace();
		
	}
	/**
	 * Builds Thread and pulls phrases from the thread
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public void buildCorpusExecutePipeline(URL url) throws Exception{
		 
		gr.buildCorpusWithDoc(url);
		gr.execute();
			
	}

	
	private PolicyStructure buildRelatedPermStructure(gate.Annotation annotation, AnnotEnum annotationType)  throws Exception{
		PolicyStructure policyStructure = new PolicyStructure();
		policyStructure.setPolicyType(annotationType);
		String action = annotation.getFeatures().get("action") != null ? annotation.getFeatures().get("action").toString() : "";
		String sentence = annotation.getFeatures().get("sentence") != null ? annotation.getFeatures().get("sentence").toString() : "";		
		String annotationString = annotation.getFeatures().get("annotation") != null ? annotation.getFeatures().get("annotation").toString() : "";
		String condition = annotation.getFeatures().get("condition") != null ? annotation.getFeatures().get("condition").toString() : "";
		policyStructure.setActions(action);
		policyStructure.setSentence(sentence);
		policyStructure.setObject("");
		policyStructure.setCondition(condition);
		//policyStructure.setAnnotation(annotationString);
		policyStructure.setGrantType("");
		policyStructure.setRelatedPermission(null);
		policyStructure.setRelatedDuty(null);
		policyStructure.setRemainder(gr.getContentFromAnnot(annotation).replaceAll(action, ""));

		
		return policyStructure;
	}
	/**
	 * Builds annotations with the "kind" feature in GATE's featuremap of an annotation
	 * @param annot
	 * @param kind
	 * @return
	 * @throws Exception
	 */
	public   ArrayList<PolicyStructure> createKnowlegeBase(AnnotEnum annotationType) throws Exception {
		ArrayList<PolicyStructure> list = new ArrayList<PolicyStructure>();
		String annotType = annotationType.name();		
		//HashSet<Annotation> annots = gr.getAnnotations(annotType);
		AnnotationSet annots = gr.getAnnotations(annotType); 
		
		SortedAnnotationList sortedAnnots = new SortedAnnotationList();		
		for(gate.Annotation an: annots){
			sortedAnnots.addSortedExclusive(an);
		}		
		for (int i = 0; i < sortedAnnots.size(); ++i) {
			String policy;
			HashMap<String, String> annotation =  new HashMap<String, String>();
			gate.Annotation a = (gate.Annotation) sortedAnnots.get(i);			

			//annotation.put("action", action);
			String str = gr.getContentFromAnnot(a);
			annotation.put("annotation", str);
			
			String condition = "";
			if (a.getFeatures().get("condition") != null){
				condition = a.getFeatures().get("condition").toString(); 
			}
			
			
			//Out.prln(str);
			if ( condition.equals("") && (str.contains("it") || str.contains("such") || str.contains("they")|| str.contains("them"))) {
				for(Iterator itr = paragraphsCorefInfo.iterator(); itr.hasNext();) {
					HashMap<String, String> paragraph = (HashMap<String, String>)itr.next();
					int paragraphStartOffset = Integer.parseInt(paragraph.get("paragraphStartOffset"));
					int paragraphEndOffset = Integer.parseInt(paragraph.get("paragraphEndOffset"));
					if(paragraph.get("corefResolutionInfo") != null && a.getStartNode().getOffset().longValue() >= paragraphStartOffset 
							&& a.getEndNode().getOffset().longValue() <= paragraphEndOffset) {
						String corefResolutionInfo = paragraph.get("corefResolutionInfo");
						String[] eachCoref;
						if(corefResolutionInfo.contains(";")){
							eachCoref = corefResolutionInfo.split(";");
						}else{
							eachCoref = new String[1];
							eachCoref[0] = corefResolutionInfo;
						}						
						ArrayList<HashMap<String, String>> allCorefsIncurrAnnotation = new ArrayList<HashMap<String, String>>();
						for(int j = 0; j < eachCoref.length; j++){							
							String[] coref = eachCoref[j].split(",");
							int corefStartIndex = Integer.parseInt(coref[0]);
							int corefEndIndex = Integer.parseInt(coref[1]);
							String mention = coref[2];							
							if(corefStartIndex >= a.getStartNode().getOffset().longValue() && corefEndIndex <= a.getEndNode().getOffset().longValue()){
								HashMap<String, String> coReference = new HashMap<String, String>(); 
								coReference.put("corefStartIndex", corefStartIndex + "");
								coReference.put("corefEndIndex", corefEndIndex + "");
								coReference.put("mention", mention);
								if ( !mention.contains(".")) {
									if(allCorefsIncurrAnnotation.isEmpty()) {
										allCorefsIncurrAnnotation.add(coReference);
									} else {
										boolean add = false;
										for(int count = 0; count < allCorefsIncurrAnnotation.size(); count++) {
											HashMap<String, String> cr = (HashMap<String, String>)allCorefsIncurrAnnotation.get(count);
											int start = Integer.parseInt(cr.get("corefStartIndex").toString());
											int end = Integer.parseInt(cr.get("corefEndIndex").toString());
											if(corefStartIndex >= start && corefEndIndex <= end){
												add = false;
												break;
											}else if(start >= corefStartIndex && end <= corefEndIndex){
												allCorefsIncurrAnnotation.remove(count);
												add = true;
											}else{
												add = true;
											}
										}
										if(add){
											allCorefsIncurrAnnotation.add(coReference);
										}	
									}
								}
							}
						}
						String annotationWithCoref = addCorefToAnnotation(a,allCorefsIncurrAnnotation);
						annotation.put("CorefResolvedAnnotation", annotationWithCoref);
						//System.out.println("annotationWithCoref = " + annotationWithCoref);
												
						break;
					}
				}
			}
			
			if(annotation.get("CorefResolvedAnnotation") != null){
				policy = annotation.get("CorefResolvedAnnotation").toString();
			}else{
				policy = annotation.get("annotation").toString();
			}
			
			policy = " " + policy;			
			PolicyStructure relatedPermission = new PolicyStructure();
			
			if (a.getFeatures().get("relatedPermission") != null){
				gate.Annotation relatedPerm = (gate.Annotation)a.getFeatures().get("relatedPermission");
				relatedPermission = buildRelatedPermStructure(relatedPerm, annotationType);
			} else {
				relatedPermission = null;
			}
			
			
			String sentence = a.getFeatures().get("sentence") != null ? a.getFeatures().get("sentence").toString() : "";	
			if (sentence.equals("") && relatedPermission != null) {
				sentence = relatedPermission.getSentence();
			}
			
			
			String enumerators = "((\\s\\(?[a-h][).]\\s)|(\\s-\\s))";
			String greekOrNums = "(\\s\\(?)(([i]*(i|v|x)[v]*[i]*[v]*)|[1-9])[).]\\s";
			if (policy.split(enumerators).length > 1 || policy.contains(";")) {
				ArrayList<HashMap<String, String>> firstPhaseSplitted =	firstPhaseSentenceSplit(policy);
				for (Iterator itr2 = firstPhaseSplitted.iterator(); itr2.hasNext();){
					HashMap<String, String> eachSplit = (HashMap<String, String>)itr2.next();
	        		String typeOfGrant = eachSplit.get("typeOfGrant")!= null? eachSplit.get("typeOfGrant"): "" ;
					String mainGrant = eachSplit.get("mainGrant");				        
			        if (mainGrant.split(greekOrNums).length > 1){				        	
						ArrayList<HashMap<String, String>> secondPhaseSplitted = secondPhaseSentenceSplit(mainGrant , typeOfGrant);
						for(Iterator itr3 = secondPhaseSplitted.iterator(); itr3.hasNext();){
							HashMap<String, String> eachSplit2 = (HashMap<String, String>)itr3.next();
			        		String typeOfGrant2 = eachSplit2.get("typeOfGrant")!= null? eachSplit2.get("typeOfGrant"): "" ;
			        		typeOfGrant2 = cleanGrantType(typeOfGrant2);
							String mainGrant2 = eachSplit2.get("mainGrant");
							String GrantWithoutParentheses = mainGrant2;
							if (mainGrant2.contains("(") && mainGrant2.contains(")")) { // remove texts in parentheses, they are for more explanation
								GrantWithoutParentheses = mainGrant2.substring(0, mainGrant2.indexOf('(')) + mainGrant2.substring(mainGrant2.indexOf(')')+1, mainGrant2.length());
							}												
							edu.stanford.nlp.pipeline.Annotation annotatedPolicy2 = sCtrl.getTokensInfo(GrantWithoutParentheses);
							String[] actionAndRemainder = extractActions ( annotatedPolicy2 , mainGrant2, annotationType, typeOfGrant2).split(";");
							String action = actionAndRemainder[0];
							if (!typeOfGrant2.trim().equals("")) {
								annotatedPolicy2 = sCtrl.getTokensInfo(typeOfGrant2 + " " + GrantWithoutParentheses);
							}
							String actionAndobject = extractObject( annotatedPolicy2 , actionAndRemainder[0], annotationType);
							String object = actionAndobject; 
							if (actionAndobject.contains(";")) {
								String[] temp = actionAndobject.split(";");
								action = temp[0];
								object = temp[1];
							}
							PolicyStructure policyStructure = new PolicyStructure(mainGrant2, annotationType, actionAndRemainder[1], condition, typeOfGrant2 , action , object ,sentence ,relatedPermission, null);
							list.add(policyStructure);
						}					        	
			        } else {
						typeOfGrant = cleanGrantType(typeOfGrant);
						String GrantWithoutParentheses = mainGrant;
						if (mainGrant.contains("(") && mainGrant.contains(")")) { // remove texts in parentheses, they are for more explanation
							GrantWithoutParentheses = mainGrant.substring(0, mainGrant.indexOf('(')) + mainGrant.substring(mainGrant.indexOf(')')+1, mainGrant.length());
						}												
						
						edu.stanford.nlp.pipeline.Annotation annotatedPolicy = sCtrl.getTokensInfo(GrantWithoutParentheses);
						String[] actionAndRemainder = extractActions(annotatedPolicy, mainGrant,annotationType, typeOfGrant).split(";");
						String action = actionAndRemainder[0];
						if (!typeOfGrant.trim().equals("")) {
							annotatedPolicy = sCtrl.getTokensInfo(typeOfGrant + " " + GrantWithoutParentheses);							
						}
						String actionAndobject = extractObject(annotatedPolicy , actionAndRemainder[0], annotationType);
						String object = actionAndobject; 
						if (actionAndobject.contains(";")) {
							String[] temp = actionAndobject.split(";");
							action = temp[0];
							object = temp[1];
						}						
						PolicyStructure policyStructure = new PolicyStructure(mainGrant, annotationType, actionAndRemainder[1], condition, typeOfGrant , action, object, sentence , relatedPermission, null);
						list.add(policyStructure);			        	
			        }
				}
			} else if(policy.split(greekOrNums).length > 1) {					
				ArrayList<HashMap<String, String>> secondPhaseSplitted = secondPhaseSentenceSplit(policy , "");
				for(Iterator itr2 = secondPhaseSplitted.iterator(); itr2.hasNext();){
					HashMap<String, String> eachSplit = (HashMap<String, String>)itr2.next();
	        		String typeOfGrant = eachSplit.get("typeOfGrant")!= null? eachSplit.get("typeOfGrant"): "" ;
	        		typeOfGrant = cleanGrantType(typeOfGrant);
					String mainGrant = eachSplit.get("mainGrant");
					String GrantWithoutParentheses = mainGrant;
					if (mainGrant.contains("(") && mainGrant.contains(")")) { // remove texts in parentheses, they are for more explanation
						GrantWithoutParentheses = mainGrant.substring(0, mainGrant.indexOf('(')) + mainGrant.substring(mainGrant.indexOf(')')+1, mainGrant.length());
					}												
					
					edu.stanford.nlp.pipeline.Annotation annotatedPolicy = sCtrl.getTokensInfo(GrantWithoutParentheses);
					String[] actionAndRemainder = extractActions(annotatedPolicy, mainGrant, annotationType , typeOfGrant).split(";");
					String action = actionAndRemainder[0];
					if (!typeOfGrant.trim().equals("")) {
						annotatedPolicy = sCtrl.getTokensInfo( typeOfGrant + " " + GrantWithoutParentheses);						
					}
					String actionAndobject = extractObject( annotatedPolicy , actionAndRemainder[0], annotationType);
					String object = actionAndobject; 
					if (actionAndobject.contains(";")) {
						String[] temp = actionAndobject.split(";");
						action = temp[0];
						object = temp[1];
					}						
					PolicyStructure policyStructure = new PolicyStructure(mainGrant, annotationType, actionAndRemainder[1], condition, typeOfGrant ,action , object , sentence, relatedPermission, null);
					list.add(policyStructure);
				}	
			} else if (!policy.contains("http://") && policy.contains(":")) {
				String typeOfGrant = policy.substring(0, policy.indexOf(":"));
				typeOfGrant = cleanGrantType(typeOfGrant);
				String mainGrant = policy.substring(policy.indexOf(":")+1, policy.length());
				String actions = a.getFeatures().get("action") != null ? a.getFeatures().get("action").toString() : "";
				String remainder = mainGrant;
				if (!actions.equals("")){
					remainder = remainder.replaceAll("\\(","");
					remainder = remainder.replaceAll("\\)","");
					actions = actions.replaceAll("\\(","");
					actions = actions.replaceAll("\\)","");										
					remainder = remainder.replaceAll(actions, "");
				}
			
				
				String GrantWithoutParentheses = mainGrant;
				if (mainGrant.contains("(") && mainGrant.contains(")")) { // remove texts in parentheses, they are for more explanation
					GrantWithoutParentheses = mainGrant.substring(0, mainGrant.indexOf('(')) + mainGrant.substring(mainGrant.indexOf(')')+1, mainGrant.length());
				}												
				
				String actionAndobject = "";
				if (!typeOfGrant.trim().equals("")) {
					actionAndobject = extractObject( sCtrl.getTokensInfo(typeOfGrant + " " + GrantWithoutParentheses) , actions, annotationType);
				} else {
					actionAndobject = extractObject( sCtrl.getTokensInfo(GrantWithoutParentheses) , actions, annotationType);
				}
				String object = actionAndobject; 
				if (actionAndobject.contains(";")) {
					String[] temp = actionAndobject.split(";");
					actions = temp[0];
					object = temp[1];
				}				
				PolicyStructure policyStructure = new PolicyStructure(mainGrant, annotationType, remainder, condition, typeOfGrant , actions, object, sentence,  relatedPermission, null);
				list.add(policyStructure);
				
			} else {				
				String actions = a.getFeatures().get("action") != null ? a.getFeatures().get("action").toString() : "";	
				String remainder = policy;
				if (!actions.equals("")){
					remainder = remainder.replaceAll("\\(","");
					remainder = remainder.replaceAll("\\)","");
					actions = actions.replaceAll("\\(","");
					actions = actions.replaceAll("\\)","");															
					remainder = remainder.replaceAll(actions, "");					
				}
			
				String GrantWithoutParentheses = policy;
				if (policy.contains("(") && policy.contains(")")) { // remove texts in parentheses, they are for more explanation
					GrantWithoutParentheses = policy.substring(0, policy.indexOf('(')) + policy.substring(policy.indexOf(')')+1, policy.length());
				}					
				String actionAndobject = extractObject( sCtrl.getTokensInfo(GrantWithoutParentheses) , actions, annotationType);
				String object = actionAndobject; 
				if (actionAndobject.contains(";")) {
					String[] temp = actionAndobject.split(";");
					actions = temp[0];
					object = temp[1];
				}					
				PolicyStructure policyStructure = new PolicyStructure(policy, annotationType, remainder, condition, "" , actions, object , sentence, relatedPermission, null);
				list.add(policyStructure);
			}
		}	

		return list;
	}
	
	private String cleanGrantType (String str){
		String cleanedStr = str;
		if(str.trim().toLowerCase().startsWith("if") || str.trim().toLowerCase().equals("you") || str.trim().toLowerCase().equals("provided") || 
				str.trim().toLowerCase().contains("contributor must:") || str.trim().toLowerCase().contains("you must:") || str.trim().toLowerCase().contains("you must not:") || str.trim().toLowerCase().contains("you will") ){
			cleanedStr = "";
		}else if(str.toLowerCase().contains("you") && str.toLowerCase().contains("meet") && str.toLowerCase().contains("conditions")){
			cleanedStr = "";
		}
			
		return cleanedStr;		
	}
	
	private String extractActions(edu.stanford.nlp.pipeline.Annotation annotatedPolicy, String policy, AnnotEnum annotationType, String typeOfGrant) {
		String action = " ";
		String remainder = policy.equals("") ? " " : policy;
		boolean isBreak = false;
		
		
        if (annotationType == AnnotEnum.Duty) {
        	if (!typeOfGrant.trim().equals("")) {
        		edu.stanford.nlp.pipeline.Annotation grantType = sCtrl.getTokensInfo(typeOfGrant);
                for (CoreMap sentence : grantType.get(CoreAnnotations.SentencesAnnotation.class)) {
                    if (sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class) != null) {
                      List<SemanticGraphEdge> semanticGraphEdges = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class).edgeListSorted();
                      for (SemanticGraphEdge edge : semanticGraphEdges) {
                    	  if (edge.getTarget().lemma().equals("must") || edge.getTarget().lemma().equals("should") || edge.getTarget().lemma().equals("will")){
                    		  action = edge.getSource().lemma(); 
                      		  remainder = remainder.replace(edge.getSource().word(), "");
                      		  isBreak = true;
                        	  break;
                    	  }
                    		  
                      }              
                    }
                    if (isBreak) {
                    	break;
                    }
                }
        		
        	}
        	
            if (action.equals(" ")) {
	            for (CoreMap sentence : annotatedPolicy.get(CoreAnnotations.SentencesAnnotation.class)) {
	                if (sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class) != null) {
	                  List<SemanticGraphEdge> semanticGraphEdges = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class).edgeListSorted();
	                  for (SemanticGraphEdge edge : semanticGraphEdges) {
	                	  if (edge.getTarget().lemma().equals("must") || edge.getTarget().lemma().equals("should") || edge.getTarget().lemma().equals("will")){
	                		  action = edge.getSource().lemma(); 
	                  		  remainder = remainder.replace(edge.getSource().word(), "");
	                  		  isBreak = true;
	                    	  break;
	                	  }
	                		  
	                  }              
	                }
	                if (isBreak) {
	                	break;
	                }
	            }
            }
            if (action.equals(" ")) {
                for (CoreMap sentence : annotatedPolicy.get(CoreAnnotations.SentencesAnnotation.class)) {        
                    List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
                    for (int i = 0; i < tokens.size(); i++) {
                    	CoreLabel token = tokens.get(i); 
                    	if ( !token.lemma().equals("be") && !token.lemma().equals("have") && !token.lemma().equals("do")  && (token.tag().equals("VB") || token.tag().equals("VBD") || token.tag().equals("VBG") || 
                    			token.tag().equals("VBN") || token.tag().equals("VBP") || token.tag().equals("VBZ"))) {
                    		action = token.lemma(); 
                    		remainder = remainder.replace(token.word(), "");
                    		break;
                    	}
                    }
                    if (isBreak) {
                    	break;
                    }
                }                	
            }
        } else {
        	if (!typeOfGrant.trim().equals("") && !typeOfGrant.toLowerCase().contains("the following")) { // to do the following:
        		edu.stanford.nlp.pipeline.Annotation grantType = sCtrl.getTokensInfo(typeOfGrant);        		
	            for (CoreMap sentence : grantType.get(CoreAnnotations.SentencesAnnotation.class)) {        
	                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
	                for (int i = 0; i < tokens.size(); i++) {
	                	CoreLabel token = tokens.get(i); 
	                	if ( !token.lemma().equals("do") && !token.lemma().equals("be") && (token.tag().equals("VB") || token.tag().equals("VBD") || token.tag().equals("VBG") || 
	                			token.tag().equals("VBN") || token.tag().equals("VBP") || token.tag().equals("VBZ"))) {
	                		action += token.lemma() + " "; 
	                		remainder = remainder.replace(token.word(), "");
	                		if ( i+1 < tokens.size()) {
	                			if (tokens.get(i+1).tag().equals(",")) {                				                		
	                				action += ", "; 
	                				i++;
	                			} else if (tokens.get(i+1).lemma().equals("and")) {                				                		
	                				action += "and "; 
	                				i++;
	                			} else if (tokens.get(i+1).lemma().equals("or")) {                				                		
	                				action += "or "; 
	                				i++;
	                			}  
	                		}	
	                	}
	                }             
	            }
        	}
        	if (action.equals(" ")) {        	
	            for (CoreMap sentence : annotatedPolicy.get(CoreAnnotations.SentencesAnnotation.class)) {        
	                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
	                for (int i = 0; i < tokens.size(); i++) {
	                	CoreLabel token = tokens.get(i); 
	                	if ( !token.lemma().equals("do") && !token.lemma().equals("be") && (token.tag().equals("VB") || token.tag().equals("VBD") || token.tag().equals("VBG") || 
	                			token.tag().equals("VBN") || token.tag().equals("VBP") || token.tag().equals("VBZ"))) {
	                		action += token.lemma() + " "; 
	                		remainder = remainder.replace(token.word(), "");
	                		if ( i+1 < tokens.size() && !tokens.get(i+1).tag().equals(",") && !tokens.get(i+1).lemma().equals("and") && !tokens.get(i+1).lemma().equals("or")) {
	                			isBreak = true;
	                			break;
	                		}
	                			
	                		if ( i+1 < tokens.size()) {
	                			if (tokens.get(i+1).tag().equals(",")) {                				                		
	                				action += ", "; 
	                				i++;
	                			} else if (tokens.get(i+1).lemma().equals("and")) {                				                		
	                				action += "and "; 
	                				i++;
	                			} else if (tokens.get(i+1).lemma().equals("or")) {                				                		
	                				action += "or "; 
	                				i++;
	                			}  
	                		}	
	                	}
	                }  
	                if (isBreak) break;
	            }
            }
        	
        	if (action.equals(" ")) {
    			isBreak = false;
	            for (CoreMap sentence : annotatedPolicy.get(CoreAnnotations.SentencesAnnotation.class)) {        
	                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
	                for (int i = 0; i < tokens.size(); i++) {
	                	CoreLabel token = tokens.get(i); 
	                	if ( !token.lemma().equals("be") && (token.tag().equals("NN") || token.tag().equals("NNS") || token.tag().equals("NNP") || token.tag().equals("NNPS"))) {
	                		action += token.lemma(); 
                			isBreak = true;
                			break;
	                	}
	                }
	                
	                if (isBreak) break;
	            }    
        		
        	}
        }		

		return action + ";" + remainder;
	}
	
	
	private String extractObject (edu.stanford.nlp.pipeline.Annotation annotatedText, String action, AnnotEnum annotationType) { 
		
		String object = "";
        String updatedAction = "";

        for (CoreMap sentence : annotatedText.get(CoreAnnotations.SentencesAnnotation.class)) {

	      if (sentence.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class) != null) {
	          Collection<SemanticGraphEdge> edges = sentence.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class).edgeListSorted();
	          for (SemanticGraphEdge edge : edges) {
	              String relation = edge.getRelation().toString();
	              IndexedWord governor = edge.getGovernor();
	          	  IndexedWord dependent = edge.getDependent(); 
	          	  if ( (governor.word().equals(action.trim().toLowerCase()) || governor.lemma().equals(action.trim().toLowerCase()) ) && relation.equals("dobj") && !object.contains(dependent.word())) {

	          		  object += dependent.word() + " ";
	          	            	  
		          	  if (dependent.tag().equals("DT")) { // like (use, the)
		  	            for (SemanticGraphEdge edge2 : edges) {
		  	            	if ( (edge2.getRelation().toString().equals("root") || edge2.getRelation().toString().equals("amod")) && edge2.getGovernor().equals(dependent)) {
		  	            		object += ( !object.contains(edge2.getDependent().word()) ? edge2.getDependent().word() + " " : "" );
		  	            		if (edge2.getDependent().tag().equals("DT")) {
		  	            			for (SemanticGraphEdge edge3 : edges) {
		  	            				String relationship = edge3.getRelation().toString();
		  	            				if (relationship.contains(":")){
		  	            					relationship = relationship.substring(0, relationship.indexOf(':'));
		  	            				}
		  	            				
		  	            				if (relationship.equals("nmod") && edge3.getGovernor().equals(edge2.getDependent()) && !object.contains(edge3.getDependent().word())) {
		  			  	            		object += ( !object.contains(edge3.getDependent().word()) ? edge3.getDependent().word() + " " : "" );
		  	            					break;
		  	            				}
		  	            			}
		  	            		}
		  	            	
		  	            	} 
		  	            	
		  	          }
		            }
		          	if (dependent.tag().equals("NN")) { // like: post kind of publicity, provide false information
		          		IndexedWord dependent2 = new IndexedWord();
		  	            for (SemanticGraphEdge edge2 : edges) {
		  	            	String relationship = edge2.getRelation().toString();
		  	            	String modifier = "";
		  	            	if (relationship.contains(":")) {
		  	            		String[] tmp = relationship.split(":");
		  	            		relationship = tmp[0];
		  	            		modifier = tmp[1];			  	            		
		  	            	}
		  	            	if ( relationship.equals("nmod") && modifier.equals("of") && edge2.getGovernor().equals(dependent)) {
		  	            		object += ( !object.contains("of " + edge2.getDependent().word()) ?  "of "  + edge2.getDependent().word() + " " : "" );
		  	            		break;
		  	            	} else if ( relationship.equals("amod") && edge2.getGovernor().equals(dependent) ){
		  	            		
		  	            		if (edge2.getDependent().tag().equals("JJ")) { //adjective
		  	            			object = ( !object.contains(edge2.getDependent().word()) ? edge2.getDependent().word() + " " : "" ) + object;
		  	            		} else {
		  	            			object += ( !object.contains(edge2.getDependent().word()) ? edge2.getDependent().word() + " " : "" );
		  	            		}		  	            		
		  	            		break;		  	            		
		  	            	} else if (relationship.equals("nmod") && (edge2.getGovernor().equals(governor) || edge2.getGovernor().equals(dependent2)) ) {
		  	            		object += ( !object.contains(modifier + " " + edge2.getDependent().word()) ?  modifier  + " "  + edge2.getDependent().word() + " " : "" );		  	            		
		  	            		//object += modifier  + " " + edge2.getDependent().word() + " ";			  	  
			  	          	    dependent2 = edge2.getDependent();  
		  	            	}
		  	            }
		          	}		          	  
	      	      break;      
	            }
	        	  
	          }
	          
	          if (object.equals("")) {
		          for (SemanticGraphEdge edge : edges) {
		              String relation = edge.getRelation().toString();
		              IndexedWord governor = edge.getGovernor();
		          	  IndexedWord dependent = edge.getDependent(); 
		          	  if ( ( action.toLowerCase().contains(governor.word()) || action.toLowerCase().contains(governor.lemma()) ) && 
		          			  dependent.tag().equals("NN") && governor.tag().contains("VB") && (relation.equals("nmod:for") || relation.equals("nmod:in"))) {
	
		          		  object =  relation.substring(5, relation.length()) + " " + dependent.word() + " ";
	          	  
		      	      break;      
		            }
		        	  
		          }
	          }
	          
	          if (object.equals("")) {
		          for (SemanticGraphEdge edge: edges) {
		              String relation = edge.getRelation().toString();
		              IndexedWord governor = edge.getGovernor();
		          	  IndexedWord dependent = edge.getDependent();         	  
			      	  if (relation.equals("dobj") && !object.contains(dependent.word()) && !hasNegativeAuxiliary( governor, edges , annotationType)) {
			      		
			      		  object += dependent.word() + " ";
			      		  updatedAction = governor.word();
			      	            	  
			          	  if (dependent.tag().equals("DT")) { // like (use, the)
			  	            for (SemanticGraphEdge edge2 : edges) {
			  	            	if ( (edge2.getRelation().toString().equals("root") || edge2.getRelation().toString().equals("amod")) && edge2.getGovernor().equals(dependent)) {
			  	            		object += ( !object.contains(edge2.getDependent().word()) ? edge2.getDependent().word() + " " : "" );
			  	            		if (edge2.getDependent().tag().equals("DT")) {
			  	            			for (SemanticGraphEdge edge3 : edges) {
			  	            				String relationship = edge3.getRelation().toString();
			  	            				if (relationship.contains(":")){
			  	            					relationship = relationship.substring(0, relationship.indexOf(':'));
			  	            				}
			  	            				
			  	            				if (relationship.equals("nmod") && edge3.getGovernor().equals(edge2.getDependent()) && !object.contains(edge3.getDependent().word())) {
						  	            		object += ( !object.contains(edge3.getDependent().word()) ? edge3.getDependent().word() + " " : "" );
			  	            					break;
			  	            				}
			  	            			}
			  	            		}
			  	            	
			  	            	} 
			  	            	
			  	          }
			            }
			          	if (dependent.tag().equals("NN")) { // like (post kind of publicity)
			          		IndexedWord dependent2 = new IndexedWord();
			  	            for (SemanticGraphEdge edge2 : edges) {
			  	            	String relationship = edge2.getRelation().toString();
			  	          	      			  	            	
			  	            	String modifier = "";
			  	            	if (relationship.contains(":")) {
			  	            		String[] tmp = relationship.split(":");
			  	            		relationship = tmp[0];
			  	            		modifier = tmp[1];			  	            		
			  	            	}
			  	            	if ( relationship.equals("nmod") && modifier.equals("of") && edge2.getGovernor().equals(dependent)) {
			  	            		object += ( !object.contains("of " + edge2.getDependent().word()) ?  "of "  + edge2.getDependent().word() + " " : "" );			  	            		
			  	            		break;
			  	            	} else if ( relationship.equals("amod") && edge2.getGovernor().equals(dependent) ){
			  	            		if (edge2.getDependent().tag().equals("JJ")) { //adjective
			  	            			object = ( !object.contains(edge2.getDependent().word()) ? edge2.getDependent().word() + " " : "" ) + object;
			  	            		} else {
			  	            			object += ( !object.contains(edge2.getDependent().word()) ? edge2.getDependent().word() + " " : "" );
			  	            		}
			  	            		break;		  	            		
			  	            	} else if (relationship.equals("nmod") && (edge2.getGovernor().equals(governor) || edge2.getGovernor().equals(dependent2))) {
			  	            		object += ( !object.contains(modifier + " " + edge2.getDependent().word()) ?  modifier  + " "  + edge2.getDependent().word() + " " : "" );		  	            					  	            		
			  	            		dependent2 = edge2.getDependent();
			  	            	}
			  	            }
			          	}
			          	  
			  	      break;      
			        }          	  
		          }
		          
	        }  
	      }  		
        }
        
        if (!updatedAction.equals("")) {
        	object = updatedAction + ';' +  object;
        }
        
        
	    return object;
	}
	
	private boolean hasNegativeAuxiliary (IndexedWord action, Collection<SemanticGraphEdge> edges, AnnotEnum annotationType ) {
		boolean negativeModifier = false;
        if (annotationType != AnnotEnum.Prohibition) {
	          for (SemanticGraphEdge edge: edges) {
	              String relation = edge.getRelation().toString();
	              IndexedWord governor = edge.getGovernor();
	          	  if ( relation.equals("neg") && governor.equals(action)){
	          		negativeModifier = true;
	          	  }
	          }	  
        }
		return negativeModifier;
	}
	
	private double calcStringsSimilarity(String s1, String s2) throws CorruptConfigFileException, IOException, URISyntaxException{
	    TextSimilarity ts = new TextSimilarity();

		double similarity = 0;		
		if (!s1.equals("") && !s2.equals("") && s1.trim().toLowerCase().equals(s2.trim().toLowerCase())){
			return threshold*2; //instead of returning 1
		} else if (s1.equals("") || s2.equals("")){
			return 0;
		}
		edu.stanford.nlp.pipeline.Annotation text1 = sCtrl.getTokensLemma(s1);
		edu.stanford.nlp.pipeline.Annotation text2 = sCtrl.getTokensLemma(s2);
	
    	try {
    		similarity = ts.textSimilarity(text1, text2, disco, discoWordSpace+"/disco.config",SimilarityMeasure.KOLB);

    	} catch (Exception e){
    		e.printStackTrace();
    	}

		return similarity;
	}

	
	private String addCorefToAnnotation (gate.Annotation annot, ArrayList<HashMap<String, String>> allCorefsIncurrAnnotation) throws Exception{
		String corefResolvedAnnot = "";
		Collections.sort(allCorefsIncurrAnnotation, new MapComparator("corefStartIndex"));
		long fromIndex = annot.getStartNode().getOffset();
		for(Iterator itr = allCorefsIncurrAnnotation.iterator(); itr.hasNext();){
			HashMap<String, String> eachCoref = (HashMap<String, String>) itr.next();
			long corefStartIndex = Long.parseLong(eachCoref.get("corefStartIndex").toString());
			long corefEndIndex = Long.parseLong(eachCoref.get("corefEndIndex").toString());
			long toIndex = corefEndIndex;
			String mention = eachCoref.get("mention");
			corefResolvedAnnot += gr.getContentByBoundary(fromIndex, toIndex);
			String token = gr.getContentByBoundary(corefStartIndex, corefEndIndex).toLowerCase();
			if (token.equals("it") || token.equals("such") || token.equals("they") || token.equals("them")) {
				corefResolvedAnnot +=  " [" + mention + "] ";
			}
			fromIndex = toIndex + 1;
		}
		if(fromIndex <= annot.getEndNode().getOffset()){
			corefResolvedAnnot += gr.getContentByBoundary(fromIndex, annot.getEndNode().getOffset());
		}
		return corefResolvedAnnot;
	}
	
	private double calcPoliciesSimilarity (double similarity, PolicyStructure p1, PolicyStructure p2) throws
				FileNotFoundException, CorruptIndexException, IOException, CorruptConfigFileException, URISyntaxException{
		
        similarity += calcStringsSimilarity(p1.getRemainder(), p2.getRemainder());            		        

        similarity += calcStringsSimilarity(p1.getActions(), p2.getActions());            			
        
        similarity += calcStringsSimilarity(p1.getCondition(), p2.getCondition());

        similarity += calcStringsSimilarity(p1.getGrantType(), p2.getGrantType());     
               		

/*	
	    DISCO disco = null;
	    try{
	    	disco = new DISCO(discoWordSpace, false);
	    } catch (Exception e){
	    	e.printStackTrace();
	    }
	    TextSimilarity ts = new TextSimilarity();
	    similarity = ts.textSimilarity(p1.getAnnotation(true, true), p2.getAnnotation(true, true), disco, SimilarityMeasure.KOLB);
*/
        
		
		return similarity;
	}
	

	
	

	
	public Summary getSummary() throws Exception{


		double absoluteStartTime = System.currentTimeMillis();
		Summary summary = new Summary();
	    try{
	    	disco = new DISCO(discoWordSpace, false);
	    } catch (Exception e){
	    	e.printStackTrace();
	    }
				
		paragraphsCorefInfo = new ArrayList<HashMap<String, String>>(); 
		AnnotationSet importantParagraphs = gr.getAnnotations("importantParagraphs");
		System.out.println("importantParagraphs size: " + importantParagraphs.size());
		double startTime = System.currentTimeMillis();

		for (Iterator itr = importantParagraphs.iterator(); itr.hasNext();) {
			HashMap<String, String> currentParagraphCoref = new HashMap<String, String>(); 
			gate.Annotation importantParagraph = (gate.Annotation)itr.next();
			currentParagraphCoref.put("paragraphStartOffset", importantParagraph.getStartNode().getOffset().longValue() + "");
			currentParagraphCoref.put("paragraphEndOffset", importantParagraph.getEndNode().getOffset().longValue() + "");			
			String paragraph = gr.getContentFromAnnot(importantParagraph);
			String corefResolutionInfo = sCtrl.getAnnotations(paragraph, importantParagraph.getStartNode().getOffset().longValue());
			if(!corefResolutionInfo.equals("")){
				currentParagraphCoref.put("corefResolutionInfo", corefResolutionInfo);
			}
			paragraphsCorefInfo.add(currentParagraphCoref);
		}
		System.out.println("coref resolution finished in " + (System.currentTimeMillis() - startTime));
		startTime = System.currentTimeMillis();
		
		ArrayList<PolicyStructure> permissions = createKnowlegeBase(AnnotEnum.Permission); 
		ArrayList<PolicyStructure> prohibitions = createKnowlegeBase(AnnotEnum.Prohibition); 
		ArrayList<PolicyStructure> duties = createKnowlegeBase(AnnotEnum.Duty);
		
		
		System.out.println("creating knowledgeBase finished in " + (System.currentTimeMillis() - startTime));

        ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
        List<Cluster> clusters;
        List<Set<Object>> clusteredPermissions = new ArrayList<Set<Object>>(); 
        List<Set<Object>> clusteredDuties = new ArrayList<Set<Object>>();
        List<Set<Object>> clusteredProhibitions = new ArrayList<Set<Object>>();
        
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        
		startTime = System.currentTimeMillis();
        if( permissions.size() > 0) {
            System.out.println("permSimilarityMatrix:");        	
	        double[][] permSimilarityMatrix = new double[permissions.size()][permissions.size()];
	        for (int col = 0; col < permissions.size(); col++) {
				PolicyStructure p1 = permissions.get(col);
	            for (int row = col + 1; row < permissions.size(); row++) {
					PolicyStructure p2 = permissions.get(row);
	                permSimilarityMatrix[col][row] = Double.valueOf(twoDForm.format(calcPoliciesSimilarity(0, p1, p2)));
	                System.out.print(Double.valueOf(twoDForm.format(permSimilarityMatrix[col][row]))+",");
	            }
	            System.out.println();
	        }
	        String[] permissionIndices = new String[permissions.size()];
	        for(int count = 0; count < permissions.size(); count++){
	        	permissionIndices[count] = Integer.toString(count+1);
	        }                
	        clusters = alg.performFlatClustering(permSimilarityMatrix, permissionIndices, new AverageLinkageStrategy(), threshold);
	        
	        ArrayList<HashMap<String, String>> permissionClusters = new ArrayList<HashMap<String, String>>();	        
	        for(java.util.Iterator<Cluster> itr = clusters.iterator(); itr.hasNext();) {
	        	Cluster cluster = itr.next();
	        	String[] leaves = cluster.returnLeaves().split(",");
	        	String membersAnnotation = "";
	        	String annotationSentence = "";
	        	String actionAndObject = "";
				HashMap<String, String> perm = new HashMap<String, String>();
				JSONObject jsonAnnot = new JSONObject();
        		PolicyStructure firstMember = permissions.get(Integer.parseInt(leaves[0])-1);
				String firstMemberCondition = firstMember.getCondition().trim();
				String firstMemberGrantType = firstMember.getGrantType().trim();
				boolean allHaveSameCondition = false; 
	        	for(int count = 1; count < leaves.length; count++) {
	        		PolicyStructure p = permissions.get(Integer.parseInt(leaves[count])-1);
	        		if (!firstMemberCondition.equals("") && firstMemberCondition.equals(p.getCondition().trim())) {
	        			allHaveSameCondition = true;
	        		} else {
	        			allHaveSameCondition = false;
	        			break;
	        		}
	        	}
					
				boolean allHaveSameGrantType = false; 
	        	for(int count = 1; count < leaves.length; count++) {
	        		PolicyStructure p = permissions.get(Integer.parseInt(leaves[count])-1);
	        		if (!firstMemberGrantType.equals("") && firstMemberGrantType.equals(p.getGrantType().trim())) {
	        			allHaveSameGrantType = true;
	        		} else {
	        			allHaveSameGrantType = false;
	        			break;
	        		}
	        	}

	        	for(int count = 0; count < leaves.length; count++) {
	        		String newLine = "";
	        		if ( count != (leaves.length-1)){
	        			newLine = "\n\n";
	        		}
	        		PolicyStructure p = permissions.get(Integer.parseInt(leaves[count])-1);														
					membersAnnotation +=  p.getAnnotation(!allHaveSameCondition, !allHaveSameGrantType) + newLine;
					annotationSentence += (!annotationSentence.contains(p.getSentence()) ? p.getSentence() + newLine : "");
					if (!actionAndObject.contains(p.getActions().trim() + " " + p.getObject().trim())) {
						actionAndObject += p.getActions().trim() + " ";
						if (!p.getActions().contains(p.getObject().trim())){
							actionAndObject += p.getObject().trim() + " , ";
						}
					}
	        		System.out.print(leaves[count] + ",");
	        	}
	        	if (leaves.length > 1) {
	        		if (allHaveSameCondition && allHaveSameGrantType) {
	        			membersAnnotation = firstMemberCondition + " " + firstMemberGrantType + "\n\n\n\n" + membersAnnotation;
	        		} else if (allHaveSameGrantType) {
	        			membersAnnotation = firstMemberGrantType + "\n\n\n\n" + membersAnnotation;
	        		} else if (allHaveSameCondition) {
	        			membersAnnotation = firstMemberCondition + "\n\n\n\n" + membersAnnotation;
	        		}
	        	}
				perm.put("annotation", membersAnnotation);
				perm.put("annotationSentence", annotationSentence);
				if (actionAndObject.contains(",")) {
					perm.put("action", actionAndObject.substring(0, actionAndObject.lastIndexOf(",")));
				} else {
					perm.put("action", actionAndObject);
				}
				jsonAnnot.putAll(perm);
				permissionClusters.add(jsonAnnot);	        	
	    		Out.prln();

	        	Set<Object> set = Arrays.asList(leaves).stream().collect(Collectors.toSet()); 
	        	clusteredPermissions.add(set);            		            	
	        }
			summary.setPermissions(permissionClusters);			
			
	    } else { // permission is empty
	        ArrayList<HashMap<String, String>> perms = new ArrayList<HashMap<String, String>>();
				HashMap<String, String> perm = new HashMap<String, String>();
				JSONObject jsonAnnot = new JSONObject();
				jsonAnnot.putAll(perm);
				perms.add(jsonAnnot);	        		        	
	        //}
			summary.setPermissions(perms);
			
	    }
        

		System.out.println("clustering Permissions took " + (System.currentTimeMillis() - startTime));

		startTime = System.currentTimeMillis();
        if( prohibitions.size() > 0) {
            System.out.println("prohibitionSimMatrix:");        	
	        double[][] prohibitionSimMatrix = new double[prohibitions.size()][prohibitions.size()];
	        for (int col = 0; col < prohibitions.size(); col++) {
				PolicyStructure p1 = prohibitions.get(col);
	            for (int row = col + 1; row < prohibitions.size(); row++) {
					PolicyStructure p2 = prohibitions.get(row);
					prohibitionSimMatrix[col][row] = Double.valueOf(twoDForm.format(calcPoliciesSimilarity(0, p1, p2)));
	                System.out.print(Double.valueOf(twoDForm.format(prohibitionSimMatrix[col][row]))+",");
	            }
	            System.out.println();
	        }
	        String[] prohibitionIndices = new String[prohibitions.size()];
	        for(int count = 0; count < prohibitions.size(); count++){
	        	prohibitionIndices[count] = Integer.toString(count+1);
	        }                
	        clusters = alg.performFlatClustering(prohibitionSimMatrix, prohibitionIndices, new AverageLinkageStrategy(), threshold);
	        
	        ArrayList<HashMap<String, String>> prohibitionClusters = new ArrayList<HashMap<String, String>>();	        
	        for(java.util.Iterator<Cluster> itr = clusters.iterator(); itr.hasNext();){
	        	Cluster cluster = itr.next();
	        	String[] leaves = cluster.returnLeaves().split(",");
	        	String membersAnnotation = "";
	        	String annotationSentence = ""; 
	        	String actionAndObject = "";
	        	
        		PolicyStructure firstMember = prohibitions.get(Integer.parseInt(leaves[0])-1);
				String firstMemberCondition = firstMember.getCondition().trim();
				String firstMemberGrantType = firstMember.getGrantType().trim();
				boolean allHaveSameCondition = false; 
	        	for(int count = 1; count < leaves.length; count++){
	        		PolicyStructure p = prohibitions.get(Integer.parseInt(leaves[count])-1);
	        		if (!firstMemberCondition.equals("") && firstMemberCondition.equals(p.getCondition().trim())) {
	        			allHaveSameCondition = true;
	        		} else {
	        			allHaveSameCondition = false;
	        			break;
	        		}
	        	}
					
				boolean allHaveSameGrantType = false; 
	        	for(int count = 1; count < leaves.length; count++){
	        		PolicyStructure p = prohibitions.get(Integer.parseInt(leaves[count])-1);
	        		if (!firstMemberGrantType.equals("") && firstMemberGrantType.equals(p.getGrantType().trim())) {
	        			allHaveSameGrantType = true;
	        		} else {
	        			allHaveSameGrantType = false;
	        			break;
	        		}
	        	}
	        	
	        	for(int count = 0; count < leaves.length; count++){
	        		String newLine = "";
	        		if ( count != (leaves.length-1)){
	        			newLine = "\n\n";
	        		}
					PolicyStructure p = prohibitions.get(Integer.parseInt(leaves[count])-1);
					membersAnnotation += p.getAnnotation(!allHaveSameCondition, !allHaveSameGrantType) + newLine;
					annotationSentence += (!annotationSentence.contains(p.getSentence()) ? p.getSentence() + newLine : "");
					if (!actionAndObject.contains(p.getActions().trim() + " " + p.getObject().trim())) {
						actionAndObject += p.getActions().trim() + " ";
						if (!p.getActions().contains(p.getObject().trim())){
							actionAndObject += p.getObject().trim() + " , ";
						}
					}
	        		System.out.print(leaves[count] + ",");
	        	}
	        	
	        	if (leaves.length > 1) {
	        		if (allHaveSameCondition && allHaveSameGrantType) {
	        			membersAnnotation = firstMemberCondition + " " + firstMemberGrantType + "\n\n\n\n" + membersAnnotation;
	        		} else if (allHaveSameGrantType) {
	        			membersAnnotation = firstMemberGrantType + "\n\n\n\n" + membersAnnotation;
	        		} else if (allHaveSameCondition) {
	        			membersAnnotation = firstMemberCondition + "\n\n\n\n" + membersAnnotation;
	        		}

	        	}
	        	
				HashMap<String, String> prob = new HashMap<String, String>();
				JSONObject jsonAnnot = new JSONObject();
				prob.put("annotation", membersAnnotation);
				prob.put("annotationSentence", annotationSentence);
				if (actionAndObject.contains(",")) {
					prob.put("action", actionAndObject.substring(0,actionAndObject.lastIndexOf(",")));
				} else {
					prob.put("action", actionAndObject);
				}
				jsonAnnot.putAll(prob);
				prohibitionClusters.add(jsonAnnot);	        	
	    		Out.prln();
	        	Set<Object> set = Arrays.asList(leaves).stream().collect(Collectors.toSet()); 
	        	clusteredProhibitions.add(set);            		            	
	        }
			summary.setProhibitions(prohibitionClusters);
	    } else { // empty prohibition
	        ArrayList<HashMap<String, String>> prohbs = new ArrayList<HashMap<String, String>>();
				HashMap<String, String> proh = new HashMap<String, String>();
				JSONObject jsonAnnot = new JSONObject();
				jsonAnnot.putAll(proh);
				prohbs.add(jsonAnnot);	        		        	
	      //  }
			summary.setProhibitions(prohbs);
	    }        
		System.out.println("clustering Prohibitions took " + (System.currentTimeMillis() - startTime));
		startTime = System.currentTimeMillis();
        
       
        if( duties.size() > 0){
	        System.out.println("dutySimilarityMatrix:");
	        double[][] dutySimilarityMatrix = new double[duties.size()][duties.size()];
	        for (int col = 0; col < duties.size(); col++) {
				PolicyStructure p1 = duties.get(col);
	            for (int row = col + 1; row < duties.size(); row++) {
					PolicyStructure p2 = duties.get(row);
	                dutySimilarityMatrix[col][row] = Double.valueOf(twoDForm.format(calcPoliciesSimilarity(0, p1, p2)));
	                System.out.print(Double.valueOf(twoDForm.format(dutySimilarityMatrix[col][row]))+",");
	            }
	            System.out.println();
	        }
	        String[] dutyIndices = new String[duties.size()];
	        for(int count = 0; count < duties.size(); count++){
	        	dutyIndices[count] = Integer.toString(count+1);
	        }                
	        
	        clusters = alg.performFlatClustering(dutySimilarityMatrix, dutyIndices, new AverageLinkageStrategy(), threshold);
	        
	        ArrayList<HashMap<String, String>> dutyClusters = new ArrayList<HashMap<String, String>>();	        
	        for(java.util.Iterator<Cluster> itr = clusters.iterator(); itr.hasNext();){
	        	Cluster cluster = itr.next();
	        	String[] leaves = cluster.returnLeaves().split(",");
	        	String membersAnnotation = "";
	        	String actionAndObject = "";
	        	String annotationSentence = "";
	        	
        		PolicyStructure firstMember = duties.get(Integer.parseInt(leaves[0])-1);
				String firstMemberCondition = firstMember.getCondition().trim();
				String firstMemberGrantType = firstMember.getGrantType().trim();
				boolean allHaveSameCondition = false; 
	        	for(int count = 1; count < leaves.length; count++){
	        		PolicyStructure p = duties.get(Integer.parseInt(leaves[count])-1);
	        		if (!firstMemberCondition.equals("") && firstMemberCondition.equals(p.getCondition().trim())) {
	        			allHaveSameCondition = true;
	        		} else {
	        			allHaveSameCondition = false;
	        			break;
	        		}
	        	}
					
				boolean allHaveSameGrantType = false; 
	        	for(int count = 1; count < leaves.length; count++){
	        		PolicyStructure p = duties.get(Integer.parseInt(leaves[count])-1);
	        		if (!firstMemberGrantType.equals("") && firstMemberGrantType.equals(p.getGrantType().trim())) {
	        			allHaveSameGrantType = true;
	        		} else {
	        			allHaveSameGrantType = false;
	        			break;
	        		}
	        	}	        	
	        	
	        	for(int count = 0; count < leaves.length; count++){
	        		String newLine = "";
	        		if ( count != (leaves.length-1)){
	        			newLine = "\n\n";
	        		}
					PolicyStructure p = duties.get(Integer.parseInt(leaves[count])-1);
					
					annotationSentence += (!annotationSentence.contains(p.getSentence()) ? p.getSentence() + newLine : "");
					membersAnnotation += p.getAnnotation(!allHaveSameCondition, !allHaveSameGrantType) + newLine;
					if (!actionAndObject.contains(p.getActions().trim() + " " + p.getObject().trim())) {
						actionAndObject += p.getActions().trim() + " ";
						if (!p.getActions().contains(p.getObject().trim())){
							actionAndObject += p.getObject().trim() + " , ";
						}
					}
	        		System.out.print(leaves[count] + ",");
	        	}
	        	
	        	if (leaves.length > 1) {
	        		if (allHaveSameCondition && allHaveSameGrantType) {
	        			membersAnnotation = firstMemberCondition + " " + firstMemberGrantType + "\n\n\n\n" + membersAnnotation;
	        		} else if (allHaveSameGrantType) {
	        			membersAnnotation = firstMemberGrantType + "\n\n\n\n" + membersAnnotation;
	        		} else if (allHaveSameCondition) {
	        			membersAnnotation = firstMemberCondition + "\n\n\n\n" + membersAnnotation;
	        		}

	        	}	     
	        	
				HashMap<String, String> duty = new HashMap<String, String>();
				JSONObject jsonAnnot = new JSONObject();
				duty.put("annotation", membersAnnotation);
				duty.put("annotationSentence", annotationSentence);
				if (actionAndObject.contains(",")) {
					duty.put("action", actionAndObject.substring(0, actionAndObject.lastIndexOf(",")));
				} else {
					duty.put("action", actionAndObject);
				}
				jsonAnnot.putAll(duty);
				dutyClusters.add(jsonAnnot);	        	
	    		Out.prln();	        	
	        	Set<Object> set = Arrays.asList(leaves).stream().collect(Collectors.toSet()); 
	        	clusteredDuties.add(set);            		            	
	        }
			summary.setDuties(dutyClusters);
        } else { // empty Duties
	        ArrayList<HashMap<String, String>> dutys = new ArrayList<HashMap<String, String>>();
				HashMap<String, String> duty = new HashMap<String, String>();
				JSONObject jsonAnnot = new JSONObject();
				jsonAnnot.putAll(duty);
				dutys.add(jsonAnnot);	        		        	
	        //}
	        summary.setDuties(dutys);
	    } 
		System.out.println("clustering duties took " + (System.currentTimeMillis() - startTime));
		System.out.println("the whole procedure took " + (System.currentTimeMillis() - absoluteStartTime));


      // Evaluation Eval = new Evaluation (clusteredPermissions,clusteredDuties,clusteredProhibitions,permissions.size(), duties.size(),prohibitions.size(),"adobe");
		String permActions = "";
		String prohActions = "";
		String dutiesAction = "";
		
		return summary;
	}

	
	public List<Set<Object>> createSetsFromString (String sequence){
		List<Set<Object>> goldstandardClass = new ArrayList<Set<Object>>();
		if(sequence.contains(";")){
			String[] sets = sequence.split(";");
			for (int i = 0; i < sets.length; i++){			
				if(sets[i].contains(",")){
					String[] setMembers = sets[i].split(",");
					goldstandardClass.add(Arrays.asList(setMembers).stream().collect(Collectors.toSet()));				
				}else{
					goldstandardClass.add(Arrays.asList(sets[i]).stream().collect(Collectors.toSet()));				
				}
				 
			}
		}else{ // only one cluster
			if(sequence.contains(",")){
				String[] setMembers = sequence.split(",");
				goldstandardClass.add(Arrays.asList(setMembers).stream().collect(Collectors.toSet()));				
			}else{
				goldstandardClass.add(Arrays.asList(sequence).stream().collect(Collectors.toSet()));				
			}			
		}
		return goldstandardClass;
		
	}	

	
	
	public double calculateRandIndex( List<Set<Object>> goldstandard, List<Set<Object>> EULAide, int count) {
        
        int[][] evaluationMatrix = new int[EULAide.size()][goldstandard.size()];        
     
        for(int col = 0; col < goldstandard.size(); col++) {
        	Set<Object> set1 = goldstandard.get(col);
        	for(int row = 0; row < EULAide.size(); row++){
            	Set<Object> set2 = EULAide.get(row);
            	Set<Object> intersection = new HashSet<Object>(set1); 
            	intersection.retainAll(set2);
            	evaluationMatrix[row][col] = intersection.size();                	
        	}
        }
        int numOfRows = EULAide.size();
        int numOfColumns = goldstandard.size();
        
		int truePositive = 0;
    	for(int row = 0; row < EULAide.size(); row++){
            for(int col = 0; col < goldstandard.size(); col++){
            	int value = evaluationMatrix[row][col];            	
            	truePositive += value*(value-1)/2;
            }
    	}


    	int falsePositive = 0;
    	for(int row = 0; row < EULAide.size(); row++){
    		falsePositive += calcRowInnerProduct(evaluationMatrix, row, numOfColumns);
    	}		
    	
    	
    	int falseNegative = 0;
    	for(int col = 0; col < numOfColumns; col++){
    		falseNegative += calcColumnInnerProduct(evaluationMatrix, col, numOfRows);
    	}		
    	    
    	
    	int trueNegative = ( count*(count-1)/2 ) - (truePositive+falsePositive+falseNegative);
    	
    	
		double RandIndex = (double)(truePositive+trueNegative)/(truePositive+trueNegative+falsePositive+falseNegative);
    	        
    	     
    	return RandIndex;
	}
	
	
	public int calcRowInnerProduct(int[][] similarityMatrix, int currentRow, int numOfColumns){
		int eachRowInnerProduct = 0;
        for(int col = 0; col < numOfColumns; col++){
        	int firstEdge = similarityMatrix[currentRow][col];
        	for(int col2 = col+1; col2 < numOfColumns; col2++){
        		int secondEdge = similarityMatrix[currentRow][col2];
        		eachRowInnerProduct += firstEdge*secondEdge;
        	}
        }
		return eachRowInnerProduct;
	}
	
	public int calcColumnInnerProduct(int[][] similarityMatrix, int currentCol, int numOfRows){
		int eachColInnerProduct = 0;
        for(int row = 0; row < numOfRows; row++){
        	int firstEdge = similarityMatrix[row][currentCol];
        	for(int row2 = row+1; row2 < numOfRows; row2++){
        		int secondEdge = similarityMatrix[row2][currentCol];
        		eachColInnerProduct += firstEdge*secondEdge;
        	}
        }
		return eachColInnerProduct;
	}	
	public ArrayList<HashMap<String, String>> getAnnotatedText2(String annotType) throws Exception {
		
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		AnnotationSet annots = gr.getAnnotations(annotType); 
		
		SortedAnnotationList sortedAnnots = new SortedAnnotationList(); 
		for(gate.Annotation an: annots){
			sortedAnnots.addSortedExclusive(an);
		} 
		
		for (int i = 0; i < sortedAnnots.size(); ++i) {
			
			HashMap<String, String> annotation =  new HashMap<String, String>();
			JSONObject jsonAnnot = new JSONObject();
			gate.Annotation a = (gate.Annotation) sortedAnnots.get(i);
			
			if(a.getFeatures().get("action") != null) {
				
				String id = Integer.toString(a.getId());
				annotation.put("id", id);
				
				String action = a.getFeatures().get("action").toString();				
				annotation.put("action", action);					
				
				String str = gr.getContentFromAnnot(a);				
				annotation.put("annotation", str);
				
				if(a.getFeatures().get("relatedPermission") != null){				
					
					String ra = a.getFeatures().get("relatedPermission").toString();
					
					String[] temp1 = ra.split("start=");
					String[] temp2 = temp1[0].split(":");
					String[] temp3 = temp2[1].split(";");
					String[] temp4 = temp3[0].split("=");
					annotation.put("relatedAnnot"+temp4[0].replaceAll("\\s+", ""), temp4[1]);
					
					String[] temp5 = temp3[1].split("=");
					annotation.put("relatedAnnot"+temp5[0].replaceAll("\\s+", ""), temp5[1]);					
				}
				
				jsonAnnot.putAll(annotation);				
				list.add(jsonAnnot); 
			}
			
		} 

		return list;
	}	
	
	


	
	private  ArrayList<HashMap<String, String>> firstPhaseSentenceSplit(String policy){
		ArrayList<HashMap<String, String>> splittedSentences = new ArrayList<HashMap<String, String>>();
		String enumerators = "((\\s\\(?[a-h][).]\\s)|(\\s-\\s))";//i may denote Greek letters	, \s is new line, space, etc. 
		//"Copyright (c) 2001, 2002, c is recognized wrongly
		String[] temp = policy.toLowerCase().split(enumerators);
		if (temp.length > 1) { 
			String[] seperatedByEnum = temp;
			for(int count = 0; count < seperatedByEnum.length; count++){
				HashMap<String, String> eachPart = new HashMap<String, String>();								
				eachPart.put("mainGrant", seperatedByEnum[count]);
				splittedSentences.add(eachPart);			
			}
			if (seperatedByEnum[0].contains(enumerators)){				
				// return seperatedByEnum in the form of ArrayList
			} else { // seperatedByEnum[0] is some kind of grant type and should be applied to all
				String typeOfGrant = seperatedByEnum[0];
				splittedSentences = new ArrayList<HashMap<String, String>>();
				for(int j = 1; j < seperatedByEnum.length; j++){
					HashMap<String, String> eachPart = new HashMap<String, String>();					
					eachPart.put("typeOfGrant", typeOfGrant);
					eachPart.put("mainGrant", seperatedByEnum[j]);
					splittedSentences.add(eachPart);
				}				
			}
		} else if(policy.contains(";")) {
			String typeOfGrant = "";
			String[] seperatedBySemicolon = policy.split(";");
			splittedSentences = new ArrayList<HashMap<String, String>>();
			for(int count = 0; count < seperatedBySemicolon.length; count++){
				HashMap<String, String> eachPart = new HashMap<String, String>();								
				eachPart.put("mainGrant", seperatedBySemicolon[count]);
				splittedSentences.add(eachPart);			
			}			
			if(seperatedBySemicolon[0].contains(":")) {
				String beforeColon = seperatedBySemicolon[0].substring(0, seperatedBySemicolon[0].lastIndexOf(":"));
				policy = policy.substring(policy.indexOf(";")+1, policy.length());	
				typeOfGrant = beforeColon;
				splittedSentences = new ArrayList<HashMap<String, String>>();					
				for(int j = 0; j < seperatedBySemicolon.length; j++){
					HashMap<String, String> eachPart = new HashMap<String, String>();					
					eachPart.put("typeOfGrant", typeOfGrant);
					if (j == 0){
						eachPart.put("mainGrant",seperatedBySemicolon[0].substring(seperatedBySemicolon[0].lastIndexOf(":")+1,seperatedBySemicolon[0].length()));
					}else{
						eachPart.put("mainGrant", seperatedBySemicolon[j]);
					}					
					splittedSentences.add(eachPart);
				}								
			} else {	
				return splittedSentences;
			}
			boolean moreColon = false;
			int count = 1;
			while(count < seperatedBySemicolon.length) {
				if(seperatedBySemicolon[count].contains(":")) {					
					moreColon = true;
					break;
				}else{
					policy = policy.substring(policy.indexOf(";")+1, policy.length());	
				}
				count++;
			}				
			if( moreColon ){
				ArrayList<HashMap<String, String>> restructuredList = new ArrayList<HashMap<String,String>>();
				for(int i = 0; i < count; i++){
					restructuredList.add(splittedSentences.get(i));
				}
				HashMap<String, String> lastElement = new HashMap<String, String>();
				lastElement.put("typeOfGrant", typeOfGrant);
				lastElement.put("mainGrant", policy);
				restructuredList.add(lastElement);
				return restructuredList;
			}				
		}

		return splittedSentences;
	}
	

	private  ArrayList<HashMap<String, String>> secondPhaseSentenceSplit(String policy, String grantType) {
		ArrayList<HashMap<String, String>> secondPhaseSplitted = new ArrayList<HashMap<String, String>>();
        String greekOrNums = "(\\s\\(?)(([i]*(i|v|x)[v]*[i]*[v]*)|[1-9])[).]\\s";        
        String[] seperatedByGreekOrNum = policy.split(greekOrNums);
        if ( seperatedByGreekOrNum.length > 1){						
        	secondPhaseSplitted = new ArrayList<HashMap<String, String>>();
			grantType = grantType + " " + seperatedByGreekOrNum[0];
			for(int count = 1; count < seperatedByGreekOrNum.length; count++){
				HashMap<String, String> eachPart = new HashMap<String, String>();
				eachPart.put("typeOfGrant", grantType);
				eachPart.put("mainGrant",  seperatedByGreekOrNum[count]);
				secondPhaseSplitted.add(eachPart);			
			}						        	
        	
        }
        return secondPhaseSplitted;
	}

	public ArrayList<String> getAnnotatedTextXML(String annotType) throws Exception {
		
		ArrayList<String> list = new ArrayList<String>();		
		gate.AnnotationSet annots = gr.getAnnotations(annotType);
		
		SortedAnnotationList sortedAnnots = new SortedAnnotationList();		
		for(gate.Annotation an: annots){
			sortedAnnots.addSortedExclusive(an);
		}

		for (int i = 0; i < sortedAnnots.size(); ++i) {
									
			gate.Annotation a = (gate.Annotation) sortedAnnots.get(i);		
			
			String str = gr.getContentFromAnnot(a);
			
			if(str != null){
				list.add(str);
			}				
		}
		return list;
	}
	

	
}

class MapComparator implements Comparator<Map<String, String>>{
    private final String key;

    public MapComparator(String key)
    {
        this.key = key;
    }

    public int compare(Map<String, String> first,
                       Map<String, String> second)
    {
        // TODO: Null checking, both for maps and values
        String firstValue = first.get(key);
        String secondValue = second.get(key);
        return firstValue.compareTo(secondValue);
    }
}