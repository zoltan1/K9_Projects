package restoreManager;

import java.io.File;

import intradoc.common.ExecutionContext;
import intradoc.common.LocaleUtils;
import intradoc.common.ServiceException;
import intradoc.common.SystemUtils;
import intradoc.data.DataBinder;
import intradoc.data.DataException;
import intradoc.data.ResultSet;
import intradoc.data.Workspace;
import intradoc.provider.Provider;
import intradoc.provider.Providers;
import intradoc.server.Service;
import intradoc.server.ServiceData;
import intradoc.server.ServiceManager;
import intradoc.server.UserStorage;
import intradoc.shared.SharedObjects;
import intradoc.shared.UserData;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;






import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * FLD COMPONENTE RESTORE DELETED FOLDERS AND FILES.
 * 
 * @author Zoltan Molnar
 *
 */
public class RestoreManager extends Service{
	
	
	
	public RestoreManager(){}
	
	public void restoreFoldersAndFiles() throws DataException, ServiceException {
		
		DataBinder serviceBinder = new DataBinder(SharedObjects.getSafeEnvironment());
		DataBinder serviceBinderExtraMetadata = new DataBinder(SharedObjects.getSafeEnvironment());
		
		String startFolder = intradoc.shared.SharedObjects.getEnvironmentValue("RestoreLocation");
		
		
		
		String did = m_binder.getLocal("dID");
		String dDocName = m_binder.getLocal("dDocName");
		String selectedFolderGUID = m_binder.getLocal("selectedFolderGUID");
		
		serviceBinder.putLocal("IdcService", "GET_FILE");
		serviceBinder.putLocal("dID", did);
		serviceBinder.putLocal("dDocName", dDocName);
	    executeService(serviceBinder, "sysadmin", false);
	    
	    String filePath = serviceBinder.getLocal("FilePath");
	    serviceBinder.clearResultSets();
	    serviceBinder.getLocalData().clear();
	    String filesThatCantBeRestored = "<table style='margin-left:4px;margin-right:4px;' border=1><tr style='background-color:LightSteelBlue;'><td><b>&nbsp;DDOCNAME&nbsp;</b></td><td><b>&nbsp;DDOCTITLE&nbsp;</b></td><td><b>&nbsp;FOLDER&nbsp;</b></td></tr>";
	    
	    try {
	    	 
	    	File fXmlFile = new File(filePath);
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(fXmlFile);
	     
	    	NodeList nListFolders = doc.getElementsByTagName("folder");
	    	// find starting point(selected folder to restore)
	    	
	    	Element startElement = null; 
	    	
	    	
	    	int startingPosition = 0;
	    	for (int tempX = 0; tempX < nListFolders.getLength(); tempX++) {
	    		
	    		Node nNode = nListFolders.item(tempX);
	    		
	    		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	    			
	    			Element eElement = (Element) nNode;
	    			if(eElement.getAttribute("id").equals(selectedFolderGUID)) {
	    				startingPosition = tempX;
	    				startElement = eElement;
	    				
	    				break;
	    			}
	    		}
	    	}
	    	
	    	
	    	doc.getDocumentElement().normalize();
	     
	    	
	    	//
	    	HashMap<String, String> newAndOldFolderGUIDS = new HashMap<String, String>();
	    	//
	    	
	    	
	    	NodeList nListFoldersFromStartPosition = startElement.getElementsByTagName("*");
	    	
	    	
	     
	    	
	    	String newfFolderGUID = "";
	    	
	    	//
	    	serviceBinder.clearResultSets();
		    serviceBinder.getLocalData().clear();
		
			serviceBinder.putLocal("IdcService", "FLD_RETRIEVE_CHILD_FOLDERS");
			serviceBinder.putLocal("fFolderGUID", "FLD_ROOT");
		    executeService(serviceBinder,"sysadmin", false);
		    
		    //trying to find if folder from cfg file is already created
		    ResultSet rootFolders = serviceBinder.getResultSet("ChildFolders");
		    
		    String startFolderGUID = "FLD_ROOT";
		    for (rootFolders.first(); rootFolders.isRowPresent(); )
			{
			    		
		    	String fFolderName = rootFolders.getStringValueByName("fFolderName"); 
				
		    	if(fFolderName.equals(startFolder.substring(1, startFolder.length()))) {
		    		
		    		startFolderGUID = rootFolders.getStringValueByName("fFolderGUID");
		    	}
				  	
				rootFolders.next();
			}
		    
		    serviceBinder.clearResultSets();
		    serviceBinder.getLocalData().clear();
	    	//
	    	
	    	//create start folder which is set in cfg file of the component
	    	//startFolder
	    	
	    	if(startFolder.length() > 1) {
		    	
	    		if(startFolderGUID.equals("FLD_ROOT")) {
	    			
		    		serviceBinder.putLocal("IdcService", "FLD_CREATE_FOLDER");
		    		serviceBinder.putLocal("fInhibitPropagation", "1");
			    	serviceBinder.putLocal("fParentGUID", startFolderGUID);
			    	serviceBinder.putLocal("fFolderName", startFolder.substring(1, startFolder.length()));
			    	executeService(serviceBinder, "sysadmin", false);
			    	
			    	startFolderGUID = serviceBinder.getLocal("fFolderGUID");
			    	
			    	
			    	
			    	serviceBinder.clearResultSets();
				    serviceBinder.getLocalData().clear();
	    		}
	    	}
	    	//
	    	
	    	//create restore folder, everything goes in there
	    	String restoredfFolderGUID = "";
	    	serviceBinder.putLocal("IdcService", "FLD_CREATE_FOLDER");
	    	serviceBinder.putLocal("fInhibitPropagation", "1");
	    	serviceBinder.putLocal("fParentGUID", startFolderGUID);
	    	
	    	//Folder name contains illegal characters, so use new date format instead of system default date format.
	    	//Frank Zhao
	    	//Jul 11, 2014
	    	//serviceBinder.putLocal("fFolderName", "_Recovered_folders_" + m_binder.getLocal("dUser") + "_" + new Date());
	    	SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmSS");
			String date = formatter.format(new Date());
	    	String recoverFolderName = "_Recovered_folders_" + m_binder.getLocal("dUser") + "_" + date;
	    	serviceBinder.putLocal("fFolderName", recoverFolderName);
	    	SystemUtils.outln("Recover folder name:" + recoverFolderName);
	    	
	    	executeService(serviceBinder, "sysadmin", false);
	    	
	    	restoredfFolderGUID = serviceBinder.getLocal("fFolderGUID");
	    	serviceBinder.clearResultSets();
		    serviceBinder.getLocalData().clear();
	    	//create restore folder
		    
		    // create selected folder
		    
		    serviceBinder.putLocal("IdcService", "FLD_CREATE_FOLDER");
		    serviceBinder.putLocal("fInhibitPropagation", "1");
	    	serviceBinder.putLocal("fParentGUID", restoredfFolderGUID);
	    	serviceBinder.putLocal("fFolderName", startElement.getAttribute("name"));
	    	
	    		
	    		
	    		NodeList listFNodes = startElement.getElementsByTagName("f");
	    		
	    		
	    		
	    		int cntRecapitulate = 0;
	    		for(int cntFNodes=0;cntFNodes<listFNodes.getLength();++cntFNodes) {
	    			
	    			Node nNodeF0 = listFNodes.item(cntFNodes);
	    		
	    		
	    			Element eElement0 = (Element) nNodeF0;
	    			
	    			Integer numId = new Integer(eElement0.getAttribute("id"));
	    			
	    			
	    			if(numId < cntRecapitulate) {
	    				// do nothing
	    				
	    			} else {
	    				
	    				String metaName = eElement0.getAttribute("name");
	    				String metaValue = nNodeF0.getTextContent();
	    				
	    				String type = eElement0.getAttribute("type");
	    				
	    				
	    				
	    				
	    				if(!metaName.equals("fFolderGUID") && !metaName.equals("fParentGUID") && !metaName.equals("fFolderName") && type.equals("nonmeta") ) {
	    				
	    					serviceBinder.putLocal(metaName, metaValue);
	    					
	    				}
	    				
	    				
	    				if(type.equals("meta") && !metaName.equals("folderPath")  && !metaName.equals("path") && !metaName.equals("folderPathLocalized")) {
	    				    
	    					
	    					serviceBinderExtraMetadata.putLocal(metaName, metaValue);
	    				}
	    				
	    			}
	    			
	    			++cntRecapitulate;
	    		}
	    	
	    		
	    		
	    	
	    	executeService(serviceBinder, "sysadmin", false);
	    	String folderGUIDSelected = serviceBinder.getLocal("fFolderGUID");
	    	
	    	
	    	serviceBinderExtraMetadata.putLocal("IdcService", "FLD_EDIT_METADATA_RULES");
	    	serviceBinderExtraMetadata.putLocal("fFolderGUID", folderGUIDSelected);
	    	executeService(serviceBinderExtraMetadata, "sysadmin", false);
	    	serviceBinderExtraMetadata.clearResultSets();
	    	serviceBinderExtraMetadata.getLocalData().clear();
	    	serviceBinderExtraMetadata.m_localData.clear();
	    	
	    	newAndOldFolderGUIDS.put(startElement.getAttribute("id"), serviceBinder.getLocal("fFolderGUID"));
	    	serviceBinder.clearResultSets();
		    serviceBinder.getLocalData().clear();
	    	NodeList nListFilesOfStartPosition = startElement.getElementsByTagName("*");
	    	
	    	for (int temp3 = 0; temp3 < nListFilesOfStartPosition.getLength(); temp3++) {
	    		
	    		Node nNode3 = nListFilesOfStartPosition.item(temp3);
	     
	    		
	    		
	    		if (nNode3.getNodeType() == Node.ELEMENT_NODE) {
	    			
		    			Element eElement3 = (Element) nNode3;
		    			
		    			
		    			if(eElement3.getTagName().equals("d")) {
		    				
		    				if(eElement3.getAttribute("parent_id").equals(startElement.getAttribute("id"))) {
		    				
		    					String fParentPathForFile = isFileInExistingFolder(eElement3.getAttribute("dID"),serviceBinder);
	    	    				if(fParentPathForFile.equals("")) {
		    					
				    				
				    				
				    				serviceBinder.putLocal("IdcService", "UPDATE_DOCINFO_BYFORM");
		    		    		    serviceBinder.putLocal("dID", eElement3.getAttribute("dID"));
		    		    			serviceBinder.putLocal("dDocName", eElement3.getAttribute("dDocName"));
		    		    			serviceBinder.putLocal("dOutDate", "");
		    		    			serviceBinder.putLocal("dStatus", "RELEASED");
		    		    			executeService(serviceBinder,"sysadmin", false);
		    		    			serviceBinder.clearResultSets();
		    		    		    serviceBinder.getLocalData().clear();
		    		    			
		    		    			serviceBinder.putLocal("IdcService", "FLD_CREATE_FILE");
		    		    		        	
		    		    				
		    		    			serviceBinder.putLocal("fParentGUID", folderGUIDSelected);
		    		    		
		    		    	
		    		    			serviceBinder.putLocal("dDocName", eElement3.getAttribute("dDocName"));
		    		    			serviceBinder.putLocal("fFileType", "owner");
		    		    			serviceBinder.putLocal("fApplication", "framework");
		    		    	
		    		    			executeService(serviceBinder, "sysadmin", false);
		    		    	
		    		    		    serviceBinder.clearResultSets();
		    		    		    serviceBinder.getLocalData().clear();
	    	    				} else {
	    	    					
	    	    					filesThatCantBeRestored += "<tr><td>&nbsp;" + eElement3.getAttribute("dDocName") + "</td><td>&nbsp;" + 
	    	    							eElement3.getAttribute("dDocTitle") + "</td><td>&nbsp;" + fParentPathForFile + "&nbsp;</td></tr>";
	    	    				}
		    				}
		    			}
	    		}
	    	}
	    	
	    	serviceBinder.clearResultSets();
		    serviceBinder.getLocalData().clear();
		    //
		    
		    
	    	
	    	//for (int tempF = startingPosition; tempF < nListFoldersFromStartPosition.getLength(); tempF++) {
		    for (int tempF = 0; tempF < nListFoldersFromStartPosition.getLength(); tempF++) {
	    		
	    		Node nNode = nListFoldersFromStartPosition.item(tempF);
	     
	    		
	    		
	    		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	    			
		    			Element eElement = (Element) nNode;
		    			
		    			
		    			if(eElement.getTagName().equals("folder")) {
		    				
		    				
		    				
		    				if(eElement.getAttribute("name").equals("MAX_FOLDERS_REACHED") || eElement.getAttribute("name").equals("MAX_FILES_REACHED")) {
		    					
		    					serviceBinder.putLocal("IdcService", "FLD_CREATE_FOLDER");
		    					serviceBinder.putLocal("fInhibitPropagation", "1");
				    			serviceBinder.putLocal("fFolderName", eElement.getAttribute("name") );
				    			serviceBinder.putLocal("fParentGUID", restoredfFolderGUID);
				    			serviceBinder.putLocal("fApplication", "framework");
				    			serviceBinder.putLocal("fFolderType", "owner");
				    			executeService(serviceBinder, "sysadmin", false);
				    			serviceBinder.clearResultSets();
				    		    serviceBinder.getLocalData().clear();

		    				}
		    				if(!eElement.getAttribute("name").equals("MAX_FOLDERS_REACHED") && !eElement.getAttribute("name").equals("MAX_FILES_REACHED")) {
		    			
			    			serviceBinder.putLocal("IdcService", "FLD_CREATE_FOLDER");
			    			serviceBinder.putLocal("fInhibitPropagation", "1");
			    			serviceBinder.putLocal("fFolderName", eElement.getAttribute("name"));
			    			serviceBinder.putLocal("fApplication", "framework");
			    			serviceBinder.putLocal("fFolderType", "owner");
			    			
			    			NodeList listFNodes1 = eElement.getElementsByTagName("f");
				    		
			    			int cntRecapitulate1 = 0;
				    		for(int cntFNodes1=0;cntFNodes1<listFNodes1.getLength();++cntFNodes1) {
				    			
				    			Node nNodeF00 = listFNodes1.item(cntFNodes1);
				    		
				    		
				    			Element eElement00 = (Element) nNodeF00;
				    			
				    			Integer numId1 = new Integer(eElement00.getAttribute("id"));
				    			
				    			if(numId1 < cntRecapitulate1) {
				    				// do nothing
				    				
				    			} else {
				    				
				    				String metaName1 = eElement00.getAttribute("name");
				    				String metaValue1 = nNodeF00.getTextContent();
				    				String type1 = eElement00.getAttribute("type");
				    				
				    				if(!metaName1.equals("fFolderGUID") && !metaName1.equals("fParentGUID") && !metaName1.equals("fFolderName") && type1.equals("nonmeta") ) {
				    				
				    					serviceBinder.putLocal(metaName1, metaValue1);
				    					
				    				
				    				}
				    				if(type1.equals("meta") && !metaName1.equals("folderPath")  && !metaName1.equals("path") && !metaName1.equals("folderPathLocalized")) {
				    				    
				    					
				    					serviceBinderExtraMetadata.putLocal(metaName1, metaValue1);
				    				}
				    			}
				    			
				    			++cntRecapitulate1;
				    		}
			    			
			    		
			    			
			    			String oldFolderGUID =  eElement.getAttribute("id");
			    					
			    			
			    			String str = "";
			    		
			    					Iterator it3 = newAndOldFolderGUIDS.entrySet().iterator();
			    			        while (it3.hasNext()) {
			    			            Map.Entry pairs3 = (Map.Entry)it3.next();
			    			            
			    			            if(pairs3.getKey().equals(eElement.getAttribute("parent_id"))) {
			    			            	
			    			            	// put new value of folder GUID
			    			            	serviceBinder.putLocal("fParentGUID", pairs3.getValue().toString());
			    			            }
			    		
			    			        }
	    			
	    			// on create folder , folder gets new fFolderGUID, so remembering it on delete is maybe unnesecary
	    			
	    			
	    			// problem is that every node that has parent_id that is unknown should have parent_id=FLD_ROOT
		    		
	    			
	    			executeService(serviceBinder, "sysadmin", false);
	    			
	    			newfFolderGUID = serviceBinder.getLocal("fFolderGUID");
	    			
	    			serviceBinderExtraMetadata.putLocal("IdcService", "FLD_EDIT_METADATA_RULES");
	    	    	serviceBinderExtraMetadata.putLocal("fFolderGUID", newfFolderGUID);
	    	    	executeService(serviceBinderExtraMetadata, "sysadmin", false);
	    	    	serviceBinderExtraMetadata.clearResultSets();
	    	    	serviceBinderExtraMetadata.getLocalData().clear();
	    	    	serviceBinderExtraMetadata.m_localData.clear();
	    			
	    			
	    			// remember old and new GUIDS so we can insert files in folders
	    			newAndOldFolderGUIDS.put(oldFolderGUID, newfFolderGUID);
		    			
	    			
	    			serviceBinder.clearResultSets();
	    		    serviceBinder.getLocalData().clear();
	    			//////
	    			
	    			NodeList nListFiles = doc.getElementsByTagName("d");
	   		     
	    	    	
	    	    	for (int tempFile1 = 0; tempFile1 < nListFiles.getLength(); tempFile1++) {
	    	   	     
	    	    		Node nNode1 = nListFiles.item(tempFile1);
	    	     
	    	    		
	    	     
	    	    		if (nNode1.getNodeType() == Node.ELEMENT_NODE) {
	    	     
	    	    			Element eElement1 = (Element) nNode1;
	    	    			
	    	    			//eElement1.getChildNodes()
	    	    			
	    	    			 
	    	    			if(eElement1.getAttribute("parent_id").equals(oldFolderGUID)) {
	    	    				
	    	    				
	    	    				String fParentPathForFile = isFileInExistingFolder(eElement1.getAttribute("dID"),serviceBinder);
	    	    				if(fParentPathForFile.equals("")) {
	    	    				
	    	    				
		    	    				
		    	    				
		    	    				serviceBinder.putLocal("IdcService", "UPDATE_DOCINFO_BYFORM");
		    		    			
		    		    		    serviceBinder.putLocal("dID", eElement1.getAttribute("dID"));
		    		    			
		    		    			
		    		    			serviceBinder.putLocal("dDocName", eElement1.getAttribute("dDocName"));
		    		    			
		    		    			
		    		    			
		    		    			serviceBinder.putLocal("dOutDate", "");
		    		    			
		    		    			serviceBinder.putLocal("dStatus", "RELEASED");
		    		    			
		    		    			executeService(serviceBinder,"sysadmin", false);
		    		    			serviceBinder.clearResultSets();
		    		    		    serviceBinder.getLocalData().clear();
		    		    			
		    		    			
		    		    			
		    		    			serviceBinder.putLocal("IdcService", "FLD_CREATE_FILE");
		    		    			
		    		    			
		    		    			
		    		    			Iterator it2 = newAndOldFolderGUIDS.entrySet().iterator();
		    		    			
		    		    			 while (it2.hasNext()) {
		    		    			        Map.Entry pairs2 = (Map.Entry)it2.next();
		    		    			       
		    		    			        if(pairs2.getKey().equals(eElement1.getAttribute("parent_id"))) {
		    		    			        	
		    		    			        	
		    		    			        	serviceBinder.putLocal("fFolderGUID", pairs2.getValue().toString());
		    		    			        	serviceBinder.putLocal("fParentGUID", pairs2.getValue().toString());
		    		    			        	
		    		    			        	
		    		    			        }
		    		    			        
		    		    			       
		    		    			  }
		    		    	
		    		    			serviceBinder.putLocal("dDocName", eElement1.getAttribute("dDocName"));
		    		    			serviceBinder.putLocal("fFileType", "owner");
		    		    			serviceBinder.putLocal("fApplication", "framework");
		    		    	
		    		    			executeService(serviceBinder, "sysadmin", false);
		    		    	
		    		    		    serviceBinder.clearResultSets();
		    		    		    serviceBinder.getLocalData().clear();
	    	    				
	    	    				} else {
	    	    					
	    	    				
	    	    					filesThatCantBeRestored += "<tr><td>&nbsp;" + eElement1.getAttribute("dDocName") + "</td><td>&nbsp;" + 
	    	    							eElement1.getAttribute("dDocTitle") + "</td><td>&nbsp;" + fParentPathForFile + "&nbsp;</td></tr>";
	    	    					
	    	    				}
	    	    			}
	    	    		}
	    	    	}
	    			
		    		}
	    			//////
	    			
	    		    serviceBinder.clearResultSets();
	    		    serviceBinder.getLocalData().clear();
	    		}
	    	}
	        	
	    		
		    }
		    	
		    	
	    	} catch (Exception e) {
	        	e.printStackTrace();
	        }
	    
	    	
	    filesThatCantBeRestored += "</table>";
    	m_binder.putLocal("filesThatCantBeRestored", filesThatCantBeRestored);
		
	}
	
	
	
	public String isFileInExistingFolder(String did,DataBinder serviceBinder) throws DataException, ServiceException {
		
		serviceBinder.clearResultSets();
	    serviceBinder.getLocalData().clear();
	    serviceBinder.putLocal("IdcService", "DOC_INFO");
	    serviceBinder.putLocal("dID", did);
	    executeService(serviceBinder, "sysadmin", false);
	    String fParentPath = serviceBinder.getLocal("fParentPath");
	    
	    
	    
	    if(fParentPath == null) {
	        // file isn't in folder
	    	return "";
	    } else {
	    	
	    	return fParentPath;
	    }
	
	}
	
	
	
	
	 // methods for calling services
	  public Workspace getSystemWorkspace()
	  {
	    Workspace workspace = null;
	    Provider wsProvider = Providers.getProvider("SystemDatabase");
	    if (wsProvider != null)
	      workspace = (Workspace)wsProvider.getProvider();
	    return workspace;
	  }

	  public UserData getFullUserData(String userName, ExecutionContext cxt, Workspace ws) throws DataException, ServiceException
	  {
	    if (ws == null)
	      ws = getSystemWorkspace();
	    UserData userData = UserStorage.retrieveUserDatabaseProfileDataFull(userName, ws, null, cxt, true, true);
	    ws.releaseConnection();
	    return userData;
	  }

	  public void executeService(DataBinder binder, String userName, boolean suppressServiceError) throws DataException, ServiceException
	  {
	    Workspace workspace = getSystemWorkspace();

	    String cmd = binder.getLocal("IdcService");
	    if (cmd == null) {
	      throw new DataException("!csIdcServiceMissing");
	    }
	    ServiceData serviceData = ServiceManager.getFullService(cmd);
	    if (serviceData == null) {
	      throw new DataException(LocaleUtils.encodeMessage("!csNoServiceDefined", null, cmd));
	    }
	    Service service = ServiceManager.createService(serviceData.m_classID, workspace, null, binder, serviceData);

	    UserData fullUserData = getFullUserData(userName, service, workspace);
	    service.setUserData(fullUserData);
	    binder.m_environment.put("REMOTE_USER", userName);

	    ServiceException error = null;
	    try
	    {
	      service.setSendFlags(true, true);
	      service.initDelegatedObjects();
	      service.globalSecurityCheck();
	      service.preActions();
	      service.doActions();
	      service.postActions();
	      service.updateSubjectInformation(true);
	      service.updateTopicInformation(binder);
	    }
	    catch (ServiceException e)
	    {
	      error = e;
	    }
	    finally
	    {
	      service.cleanUp(true);
	      workspace.releaseConnection();
	    }

	    if (error == null)
	      return;
	    if (suppressServiceError)
	    {
	      error.printStackTrace();
	      if (binder.getLocal("StatusCode") != null)
	        return;
	      binder.putLocal("StatusCode", String.valueOf(error.m_errorCode));
	      binder.putLocal("StatusMessage", error.getMessage());
	    }
	    else
	    {
	      throw new ServiceException(error.m_errorCode, error.getMessage());
	    }
	
}
	  
}
