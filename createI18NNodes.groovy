import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.spi.commons.value.QValueValue;
import au.com.bytecode.opencsv.*;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

/************************************************************************
* 				MAIN					*
************************************************************************/

//Get filename argument
def filename = this.args[0]

//Call createI18NNodes to read csv file and create nodes in CRX 
createI18NNodes(filename)

/************************************************************************
*                                END                                    *
************************************************************************/


/************************************************************************
*                             FUNCTIONS                                 *
************************************************************************/

/**
* createNodes function handles reading csv file and creating nodes in CRX *
**/
@Grab(group='javax.jcr', module='jcr', version='2.0')
@Grab(group='org.apache.jackrabbit', module='jackrabbit-jcr2dav', version='2.5.0')
@Grab(group='org.apache.sling', module='org.apache.sling.api', version='2.2.4')
@Grab(group='org.apache.sling', module='org.apache.sling.jcr.api', version='2.1.0')
@Grab(group='org.apache.sling', module='org.apache.sling.commons.testing', version='2.0.10')
//@GrabExclude('org.slf4j:slf4j-simple')
@Grab(group='org.slf4j', module='slf4j-api', version='1.6.6')
@Grab(group='net.sf.opencsv', module='opencsv', version='2.3')
def createI18NNodes(String filename){

	//Initialize session
	def Session session
	try {
		def repository = JcrUtils.getRepository("http://localhost:4502/crx/server")
		def credentials = new SimpleCredentials("admin", "admin".toCharArray())
		session = repository.login(credentials)
	} 
	catch (RepositoryException e) {
		e.printStackTrace()
	}	

	//Get parent node for all French nodes
	def frNode = session.getRootNode().getNode("apps/bombardier/i18n/fr/")

	def tagsCSVFile = new File(filename)
	def reader = new CSVReader(new FileReader(tagsCSVFile))
	def nextLine
	while ((nextLine = reader.readNext()) != null) {
		def english = nextLine[0]
		def french = nextLine[1]
		def nodeName = english.replaceAll(" ", "")
		printf("%s --> %s\n", english, french)
			
		def propertiesMap = ["sling:key" : english, "sling:message" : french]
		def mixins = ["sling:Message"]
		def node = createNodeIfNotExists(frNode, nodeName, "nt:folder", propertiesMap, mixins)
	
	}
	
	//Commit changes
	session.save()

	//Close session
	session.logout()
}


/**
* If a child node already exists with the given name, just return existing node.
* Otherwise create a new node and initialize with properties from propertiesMap
**/
def createNodeIfNotExists(Node parentNode, String name, String type, Map<String, String> propertiesMap, List<String> mixins) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, ValueFormatException, RepositoryException{

	def node = null
	if(!parentNode.hasNode(name)){
		node = parentNode.addNode(name, type)
			
		//Add mixins
		if(mixins != null){
			mixins.each{ mixin ->
				node.addMixin(mixin)
			}
		}
			
		//Set properties on new node
		if(propertiesMap != null){
			propertiesMap.each {key, value ->
				node.setProperty(key, value)
			}
		}
	}

	printf("\tNODE %s ALREADY EXISTS, NO NODE CREATED\n", name)
	node = parentNode.getNode(name)

	return node
}
