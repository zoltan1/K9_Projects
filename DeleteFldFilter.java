package deleteFldFilter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import intradoc.common.ExecutionContext;
import intradoc.common.LocaleUtils;
import intradoc.common.ServiceException;
import intradoc.data.DataBinder;
import intradoc.data.DataException;
import intradoc.data.ResultSet;
import intradoc.data.Workspace;
import intradoc.provider.Provider;
import intradoc.provider.Providers;
import intradoc.server.Service;
import intradoc.server.ServiceData;
import intradoc.server.ServiceHandler;
import intradoc.server.ServiceManager;
import intradoc.server.UserStorage;
import intradoc.shared.SharedObjects;
import intradoc.shared.UserData;

/**
 * This ServiceHandler will be called on FLD_MOVE (component FrameworkFolders)
 * It needs to remember folder, his children and documents on delete.
 * 
 * @author Zoltan Molnar
 *
 */
public class DeleteFldFilter extends ServiceHandler
{
  
	private String xml = "";	
	
	private int maxFolders,maxFiles;
	private int cntFolders,cntFiles;
	private String xmlLocation;
	
	public void checkinBeforeDelete() throws DataException, ServiceException, UnsupportedEncodingException
	{
	  

	  DataBinder serviceBinder = new DataBinder(SharedObjects.getSafeEnvironment());
	  DataBinder serviceBinderFiles = new DataBinder(SharedObjects.getSafeEnvironment());
	  DataBinder serviceBinderExtraMetadata = new DataBinder(SharedObjects.getSafeEnvironment());
	  
	  maxFolders = new Integer(intradoc.shared.SharedObjects.getEnvironmentValue("MaxFoldersToDelete"));
	  maxFiles = new Integer(intradoc.shared.SharedObjects.getEnvironmentValue("MaxFilesToDelete"));
	  
	  xmlLocation = SharedObjects.getEnvironmentValue("XmlLocation");
	  
	 
	  
	  // user can select more folders to delete
	  ArrayList listOfDeletedFolders = new ArrayList();
	  int u = 1;
	  while(m_binder.getLocal("item" + u) != null) {
		  
		  listOfDeletedFolders.add(m_binder.getLocal("item"+u).substring(m_binder.getLocal("item"+u).indexOf(":")+1, (m_binder.getLocal("item"+u).length())));
		  
		  ++u;
	  }
	  
	  
	  Iterator<String> it = listOfDeletedFolders.iterator();
	  
	  // we need to go thru all selecteed folders to be deleted
	  while(it.hasNext())
	  {
	      	  
		      String fFolderGUID = it.next();
	      

			  serviceBinder.putLocal("IdcService", "FLD_INFO");
			  serviceBinder.putLocal("fFolderGUID", fFolderGUID);      
			  executeService(serviceBinder, "sysadmin", false);
		      
		      
		      ResultSet folderInfo = serviceBinder.getResultSet("FolderInfo");
		      
		      String fParentGUID = folderInfo.getStringValueByName("fParentGUID");
		      String fFolderNameParent = folderInfo.getStringValueByName("fFolderName");
		      String fSecurityGroup = folderInfo.getStringValueByName("fSecurityGroup");
		      String fCreator = folderInfo.getStringValueByName("fCreator");
		      
		      String fFolderDescription = folderInfo.getStringValueByName("fFolderDescription");
		      String fFolderSize = folderInfo.getStringValueByName("fFolderSize");
		      String fFolderType = folderInfo.getStringValueByName("fFolderType");
		      String fLastModifiedDate = folderInfo.getStringValueByName("fLastModifiedDate");
		      String fChildFoldersCount = folderInfo.getStringValueByName("fChildFoldersCount");
		      
		      
		      
		      //do not make xml for directories that are restored
		      if(fFolderNameParent.indexOf("_Recovered_folders") < 0) {
		      
				      serviceBinder.clearResultSets();
				      serviceBinder.getLocalData().clear();
				
					  serviceBinder.putLocal("IdcService", "FLD_RETRIEVE_CHILD_FOLDERS");
					  serviceBinder.putLocal("fFolderGUID", fFolderGUID);
				      executeService(serviceBinder,"sysadmin", false);
				      
				      
				      
				      String folderDeleted = serviceBinder.getLocal("path").substring(serviceBinder.getLocal("path").lastIndexOf("/"), serviceBinder.getLocal("path").length());

				      
				      // NEED TO FIND FROM WHICH FOLDER IS FOLDER DELETED
				      String[] tokens = serviceBinder.getLocal("path").split("/");
				      
				      String folderFromWhichFolderIsDeleted = tokens[tokens.length-2]; 
				      
				    
				      if(folderFromWhichFolderIsDeleted == null) {
				    	  
				    	  folderFromWhichFolderIsDeleted = "FLD_ROOT";
				      } else {
				    	  if(folderFromWhichFolderIsDeleted.equals("")) {
				    		  
				    		  folderFromWhichFolderIsDeleted = "FLD_ROOT";
				    	  } else {
				    		  
				    		  folderFromWhichFolderIsDeleted = serviceBinder.getLocal("path").substring(1, serviceBinder.getLocal("path").lastIndexOf("/"));
				    	  }
				      }
				      // NEED TO FIND FROM WHICH FOLDER IS FOLDER DELETED
				      
				      String nameOfDeletedFolder = folderDeleted.substring(folderDeleted.indexOf("/") + 1, folderDeleted.length());
				      
				      xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +  
				    		" <folder deletedFrom=\"" + folderFromWhichFolderIsDeleted + "\" id=\"" + fFolderGUID + "\" parent_id=\"" + fParentGUID + "\" name=\"" + nameOfDeletedFolder + "\">";
				    		
				      int cntCurr0 = folderInfo.getNumFields();
				      int innerCnt0 = 0;
			  		  for(int jCnt0=0;jCnt0<cntCurr0;++jCnt0) {
			  				
			  				String cntFieldName0 = folderInfo.getFieldName(jCnt0);
			  				String cntFieldValue0 = folderInfo.getStringValue(jCnt0);
			  				
			  				if(cntFieldValue0 != null) {
			  				
			  					if(!cntFieldValue0.equals("")) {
			  						
			  						xml += " <f id=\"" + innerCnt0 + "\" name=\"" + cntFieldName0 + "\" type=\"" + "nonmeta" + "\"  >" + cntFieldValue0 + "</f>";
			  						innerCnt0++;
			  					}
			  				}
			  		  }
				      
			  		  // i need to get custom metadata for folder
			  		  serviceBinderExtraMetadata.clearResultSets();
					  serviceBinderExtraMetadata.getLocalData().clear();  
			  		  serviceBinderExtraMetadata.putLocal("IdcService", "FLD_EDIT_METADATA_RULES_FORM");
			  		  serviceBinderExtraMetadata.putLocal("fFolderGUID", fFolderGUID);
					  executeService(serviceBinderExtraMetadata, "sysadmin", false);
					  
					  Properties localData = serviceBinderExtraMetadata.m_localData;
					  Iterator iterator = localData.entrySet().iterator();
					  while(iterator.hasNext()) {
						  
						  // 
						  Object obj = iterator.next();
						  String nameLocale = obj.toString().substring(0, obj.toString().indexOf("="));
						  String valueLocale = obj.toString().substring(obj.toString().indexOf("=") + 1, obj.toString().length());
						  
						  
						  
						  if(!valueLocale.equals("") && !nameLocale.equals("IdcService") && !nameLocale.equals("fFolderGUID") && !nameLocale.equals("folderPath") && !nameLocale.equals("path") && !nameLocale.equals("folderPathLocalized")) {
						  
						  
							  
							  xml += " <f id=\"" + innerCnt0  + "\" name=\"" + nameLocale + "\" type=\"" + "meta" + "\"  >" + valueLocale + "</f>";
							  innerCnt0++;
						  }
						  
					  }
					  //
					  
					  serviceBinderExtraMetadata.clearResultSets();
					  serviceBinderExtraMetadata.getLocalData().clear();  
				      //
					  
				      cntFolders = cntFolders + 1; 
				      
				      
				      // for deleted folder we need to get files
					  serviceBinderFiles.putLocal("IdcService", "FLD_RETRIEVE_CHILD_FILES");
					  serviceBinderFiles.putLocal("fFolderGUID", fFolderGUID);
					  executeService(serviceBinderFiles, "sysadmin", false);
					  ResultSet childFilesDeletedFolder = serviceBinderFiles.getResultSet("ChildFiles");
					  serviceBinderFiles.clearResultSets();
					  serviceBinderFiles.getLocalData().clear();    
					      
					  for (childFilesDeletedFolder.first(); childFilesDeletedFolder.isRowPresent(); )
					  {
					    		
						    
						  	cntFiles = cntFiles + 1;
						  	
						  	if(cntFiles < maxFiles) {
					    		
					    		xml += " <d id=\"" + childFilesDeletedFolder.getStringValueByName("fFileGUID") + "\" parent_id=\"" + fFolderGUID + "\" dDocName=\"" + childFilesDeletedFolder.getStringValueByName("dDocName") + "\" name=\"" + childFilesDeletedFolder.getStringValueByName("fFileName") + "\" dID=\"" +  childFilesDeletedFolder.getStringValueByName("dID") + "\" dDocTitle=\"" +  childFilesDeletedFolder.getStringValueByName("dDocTitle")  + "\"/>";
					    	}
						  	if(cntFiles == maxFiles) {
					    		
						  		xml += " <d id=\"" + childFilesDeletedFolder.getStringValueByName("fFileGUID") + "\" parent_id=\"" + fFolderGUID + "\" dDocName=\"" + childFilesDeletedFolder.getStringValueByName("dDocName") + "\" name=\"" + childFilesDeletedFolder.getStringValueByName("fFileName") + "\" dID=\"" +  childFilesDeletedFolder.getStringValueByName("dID") + "\" dDocTitle=\"" +  childFilesDeletedFolder.getStringValueByName("dDocTitle")  + "\"/>";
					    		xml += " <folder id=\"" + "MAX_FILES_REACHED" + "\" parent_id=\"" + "" + "\" name=\"" + "MAX_FILES_REACHED" + "\">";
					  			xml += " </folder> ";
					  			//return;
					    	} 
						  	
						  	
						  	childFilesDeletedFolder.next();
					  }
				      
				      
					  
						  
					  GRA(fFolderGUID,serviceBinder,serviceBinderFiles,serviceBinderExtraMetadata);
					 
					  
				      serviceBinder.clearResultSets();
				      serviceBinder.getLocalData().clear();
				     
				      xml += "</folder>";
				      
				      //check is structure of xml ok?
				      StringTokenizer st2 = new StringTokenizer(xml, " ");
				      
				      int cntFolderOpened=0,cntFolderClosed=0;
					  while (st2.hasMoreElements()) {
							
						  String t1 = (String)st2.nextElement();
						  if(t1.equals("<folder")) {
							  
							  cntFolderOpened = cntFolderOpened + 1;
						  }
						  
						  if(t1.equals("</folder>")) {
							  
							  cntFolderClosed = cntFolderClosed + 1;
						  }
					  }
					  if(cntFolderClosed < cntFolderOpened) {
						  
						  if(cntFolderOpened > 1) {
							  
							  for(int t=0;t<cntFolderOpened-cntFolderClosed;++t) {
								  
								  xml += " </folder> ";
							  }
						  }
					  }
					  //
				      
				      String tempPath = xmlLocation;
				      
				      
				      
				      File file;
				      
				      try {
				    	  
				    	  String fileName = m_binder.getLocal("dUser") + "_deletedContent_" + new Date().getTime() + ".xml";
				    	  String filePath = tempPath + fileName;
				          file = new File(filePath);
				          BufferedWriter output = new BufferedWriter(new FileWriter(file));
				          
				          byte ptext[] = xml.getBytes("ISO-8859-1"); 
				          String value = new String(ptext, "UTF-8"); 
				          
				          output.write(value);
				          output.close();
				          
				          
				
					      serviceBinder.putLocal ("IdcService", "CHECKIN_NEW");
					      serviceBinder.putLocal ("dDocType", "deletedFolderRecord");
					      serviceBinder.putLocal ("dDocTitle", "Deleted folder recovery record, folder: " + nameOfDeletedFolder);	
					      serviceBinder.putLocal ("dSecurityGroup", "Public");
					      serviceBinder.putLocal ("dDocAuthor", "sysadmin");
					      serviceBinder.putLocal ("xDeletedBy", m_binder.getLocal("dUser"));
					      serviceBinder.putLocal ("xDeletedFolder", nameOfDeletedFolder);
					      serviceBinder.putLocalDate("xDeletedDate", new Date());
					      serviceBinder.putLocal ("dRevLabel", "1");
					      
					      //Set primary file like:
					      //Oracle Support 
					      //primaryFile=df_rev4.txt
					      //primaryFile:path=/home/brehl/samplefiles/df_rev4.txt
					      //Also refer to
					      //Oracle® WebCenter Content System Administrator's Guide for Content Server
					      //11g Release 1 (11.1.1)
					      //Part Number E10792-04
					      //Section, 3.6.3.4.1 Batch Load Command Files
					      //
					      //Frank Zhao, Jul 09, 2014
					      //
					      serviceBinder.putLocal("primaryFile", fileName);
					      serviceBinder.putLocal("primaryFile:path", filePath);
					      
					      executeService(serviceBinder, "sysadmin", false);
					      serviceBinder.clearResultSets();
					      serviceBinder.getLocalData().clear();
					      
					      //file.delete();
				      
				      } catch ( IOException e ) {
				          e.printStackTrace();
				      }
	  
		      } 
	  }
  }
  
	
   
	
  // method for going thru folders
  public void GRA(String  fFolderGUID,DataBinder serviceBinderFolders,DataBinder serviceBinderFiles,DataBinder serviceBinderExtraMetadata) throws DataException, ServiceException{		
	  	 
	 
	  
	  	  serviceBinderFolders.putLocal("IdcService", "FLD_RETRIEVE_CHILD_FOLDERS");
	  	  serviceBinderFolders.putLocal("fFolderGUID", fFolderGUID);
	      executeService(serviceBinderFolders, "sysadmin", false);
		  ResultSet childFolders = serviceBinderFolders.getResultSet("ChildFolders");

		  serviceBinderFolders.clearResultSets();
		  serviceBinderFolders.getLocalData().clear();
		  
		 
		  
		  if(childFolders != null) {
			  
			  
			  for (childFolders.first(); childFolders.isRowPresent(); )
		      {
				  	
				 
				  		cntFolders = cntFolders + 1;
				  		
				  		
				  		if(cntFolders < maxFolders && cntFiles < maxFiles) {
					  		
				  			xml += " <folder id=\"" + childFolders.getStringValueByName("fFolderGUID") + "\" parent_id=\"" + childFolders.getStringValueByName("fParentGUID") + "\" name=\"" + childFolders.getStringValueByName("fFolderName") + "\">";  
						    
				  			
				  			int cntCurr = childFolders.getNumFields();
				  			int innerCnt = 0;
				  			for(int jCnt=0;jCnt<cntCurr;++jCnt) {
				  				
				  				String cntFieldName1 = childFolders.getFieldName(jCnt);
				  				String cntFieldValue1 = childFolders.getStringValue(jCnt);
				  				
				  				if(cntFieldValue1 != null) {
					  				
				  					if(!cntFieldValue1.equals("")) {
				  				
				  						
				  						xml += " <f id=\"" + innerCnt + "\" name=\"" + cntFieldName1 + "\" type=\"" + "nonmeta" + "\"  >" + cntFieldValue1 + "</f>";
				  						innerCnt++;
				  					}
				  				}
				  				
				  			}
				  			
				  			
				  			  // i need to get custom metadata for folder
				  			  serviceBinderExtraMetadata.clearResultSets();
							  serviceBinderExtraMetadata.getLocalData().clear();  
					  		  serviceBinderExtraMetadata.putLocal("IdcService", "FLD_EDIT_METADATA_RULES_FORM");
					  		  serviceBinderExtraMetadata.putLocal("fFolderGUID", childFolders.getStringValueByName("fFolderGUID"));
							  executeService(serviceBinderExtraMetadata, "sysadmin", false);
							  
							  Properties localData1 = serviceBinderExtraMetadata.m_localData;
							  Iterator iterator1 = localData1.entrySet().iterator();
							  while(iterator1.hasNext()) {
								  
								  // 
								  Object obj1 = iterator1.next();
								  String nameLocale1 = obj1.toString().substring(0, obj1.toString().indexOf("="));
								  String valueLocale1 = obj1.toString().substring(obj1.toString().indexOf("=") + 1, obj1.toString().length());
								  
								  
								  
								  if(!valueLocale1.equals("") && !nameLocale1.equals("IdcService") && !nameLocale1.equals("fFolderGUID")  && !nameLocale1.equals("folderPath") && !nameLocale1.equals("path") && !nameLocale1.equals("folderPathLocalized")) {
								  
								  
								  
									  
									  xml += " <f id=\"" + innerCnt + "\" name=\"" + nameLocale1 + "\" type=\"" + "meta" + "\"  >" + valueLocale1 + "</f>";
									  innerCnt++;
								  }
								  
							  }
							  //
				  		
						    
				  		}
				  		if(cntFolders == maxFolders) {
				  			
				  			xml += " <folder id=\"" + childFolders.getStringValueByName("fFolderGUID") + "\" parent_id=\"" + childFolders.getStringValueByName("fParentGUID") + "\" name=\"" + childFolders.getStringValueByName("fFolderName") + "\">";
				  			
				  			
				  			
				  			int cntCurr1 = childFolders.getNumFields();
				  			int innerCnt1 = 0;
				  			for(int jCnt1=0;jCnt1<cntCurr1;++jCnt1) {
				  				
				  				String cntFieldName2 = childFolders.getFieldName(jCnt1);
				  				String cntFieldValue2 = childFolders.getStringValue(jCnt1);
				  				
				  				
				  				if(cntFieldValue2 != null) {
					  				
				  					if(!cntFieldValue2.equals("")) {
				  				
				  						
				  						xml += " <f id=\"" + innerCnt1 + "\" name=\"" + cntFieldName2 + "\"  >" + cntFieldValue2 + "</f>";
				  						innerCnt1++;
				  					}
				  				}
				  				
				  			}
				  			
				  			  // i need to get custom metadata for folder
				  			  serviceBinderExtraMetadata.clearResultSets();
							  serviceBinderExtraMetadata.getLocalData().clear();  
					  		  serviceBinderExtraMetadata.putLocal("IdcService", "FLD_EDIT_METADATA_RULES_FORM");
					  		  serviceBinderExtraMetadata.putLocal("fFolderGUID", childFolders.getStringValueByName("fFolderGUID"));
							  executeService(serviceBinderExtraMetadata, "sysadmin", false);
							  
							  Properties localData2 = serviceBinderExtraMetadata.m_localData;
							  Iterator iterator2 = localData2.entrySet().iterator();
							  while(iterator2.hasNext()) {
								  
								  // 
								  Object obj2 = iterator2.next();
								  String nameLocale2 = obj2.toString().substring(0, obj2.toString().indexOf("="));
								  String valueLocale2 = obj2.toString().substring(obj2.toString().indexOf("=") + 1, obj2.toString().length());
								  
								  
								  
								  if(!valueLocale2.equals("") && !nameLocale2.equals("IdcService") && !nameLocale2.equals("fFolderGUID") && !nameLocale2.equals("folderPath") && !nameLocale2.equals("path") && !nameLocale2.equals("folderPathLocalized") ) {
								  
								  
								  
									  
									  xml += " <f id=\"" + innerCnt1 + "\" name=\"" + nameLocale2  + "\" type=\"" + "meta" + "\"  >" + valueLocale2 + "</f>";
									  innerCnt1++;
								  }
								  
							  }
							  //
				  			
						    
				  			xml += " <folder id=\"" + "MAX_FOLDERS_REACHED" + "\" parent_id=\"" + "" + "\" name=\"" + "MAX_FOLDERS_REACHED" + "\">";
				  			xml += " </folder> ";
				  			
				  		} 
				  		
				  		
					  	
					  	
					  	
				        String childoFoldero = childFolders.getStringValueByName("fFolderGUID");

					    //za svaki folder treba i dohvatiti listu fajlova
					  	serviceBinderFiles.putLocal("IdcService", "FLD_RETRIEVE_CHILD_FILES");

					  	serviceBinderFiles.putLocal("fFolderGUID", childoFoldero);
					    executeService(serviceBinderFiles, "sysadmin", false);
					    ResultSet childFiles = serviceBinderFiles.getResultSet("ChildFiles");
					    childFolders.next();  
					      
					    for (childFiles.first(); childFiles.isRowPresent(); )
					    {
					    	
					    	
				    			
					    	
					    	cntFiles = cntFiles + 1;
					    	
					    	if(cntFolders < maxFolders && cntFiles < maxFiles) {
					    		
					    		xml += " <d id=\"" + childFiles.getStringValueByName("fFileGUID") + "\" parent_id=\"" + childoFoldero + "\" dDocName=\"" + childFiles.getStringValueByName("dDocName") + "\" name=\"" +  childFiles.getStringValueByName("fFileName") + "\" dID=\"" +  childFiles.getStringValueByName("dID") + "\" dDocTitle=\"" +  childFiles.getStringValueByName("dDocTitle") + "\"/>";
					    	}
					    	if(cntFiles == maxFiles) {
					    		
					    		
					    		xml += " <d id=\"" + childFiles.getStringValueByName("fFileGUID") + "\" parent_id=\"" + childoFoldero + "\" dDocName=\"" + childFiles.getStringValueByName("dDocName") + "\" name=\"" +  childFiles.getStringValueByName("fFileName") + "\" dID=\"" +  childFiles.getStringValueByName("dID") + "\" dDocTitle=\"" +  childFiles.getStringValueByName("dDocTitle") + "\"/>";
					    		xml += " <folder id=\"" + "MAX_FILES_REACHED" + "\" parent_id=\"" + "" + "\" name=\"" + "MAX_FILES_REACHED" + "\">";
					  			xml += " </folder> ";
					  			
					    	} 
					    	
					    	
					    	childFiles.next();
					    }
					    
					    
					    serviceBinderFiles.clearResultSets();
					    serviceBinderFiles.getLocalData().clear();
					    
					    
					  
					    	
					    GRA(childoFoldero,serviceBinderFolders,serviceBinderFiles,serviceBinderExtraMetadata);
					    
					    if(cntFolders < maxFolders && cntFiles < maxFiles) {
					    	
					    	xml += " </folder> ";
					    }
			  
			  
				  }  
		  
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