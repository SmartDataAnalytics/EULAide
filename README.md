# EULAide: 
EULAide is an attempt to aid the end-users to understand an EULA (End-User License Agreement) better. It uses semantic technologies to reach this goal. 
For the complete overview of the approach please take a look at the published papers: 
1. https://www.researchgate.net/publication/304781565_EULAide_Interpretation_of_End-User_License_Agreements_using_Ontology-Based_Information_Extraction
2. https://www.researchgate.net/publication/318361089_Semantic_Similarity_based_Clustering_of_License_Excerpts_for_Improved_End-User_Interpretation

# Deployment: 
There are two applications: a back-end and a front-end; The EULAideClient folder in this repo is the front-end and all other files belong to the back-end. It is better to create two seperate folders for apps to avoid the confusion. 
The front-end and back-end are independent. You can impelement your own front-end, as long as the front-end sends a file or URL to the server, the back-end will reply with a JSON object and everything will run smoothly.

To deploy the back-end, run Main.java as a java-application. Make sure to allocate at least 1 GB memory to avoid the OutOfMemory error. For running Main.java in the back-end, two arguments are necessary: GATE home and DISCO WORD SPACE. If the arguments are null the program will use a default path in the root.

The client is a lightweight app with a bunch of HTML, CSS and angular js files. Use any application server to deploy this web app (e.g., tomcat). If the client side is deployed successfully, you should see something like [this image](https://github.com/SmartDataAnalytics/EULAide/blob/master/EULAideClient.png) in your browser. In this web page you can choose a license file (text, pdf, doc, ...) or directly enter the URL of the license agreement into the URL box.

If both sides are deployed successfully, the client sends a request to the back-end and receives a JSON object when the request is valid.
Finally you can see the results like [this image](https://github.com/SmartDataAnalytics/EULAide/blob/master/screenshot.png).



