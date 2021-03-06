package applab.search.feeds;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import applab.net.HttpGet;
import applab.net.HttpResponse;
import applab.search.server.SearchSalesforceProxy;
import applab.server.DatabaseHelpers;
import applab.server.DatabaseId;
import applab.server.DatabaseTable;
import applab.server.WebAppId;
import applab.server.XmlHelpers;

import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;

public class ParseFitFeedXml {

    private static final String attribution = "Information provided by FIT Uganda"; 
    private static final String keywordBase = "Market_Prices";

    private String fitFeedUrl;
    private Integer categoryId;
    private String manualDate;
    private HashMap<String, String> regionMap;
    
    private ArrayList<String> missingMarkets;
    
    private ArrayList<KeywordEntry> keywords;
    
    private Connection connection;
    private PreparedStatement insertStatement;
    private PreparedStatement updateStatement;

    public ParseFitFeedXml(Integer categoryId, String fitFeedUrl, String manualDate)
            throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {
        this.categoryId = categoryId;
        this.fitFeedUrl = fitFeedUrl;
        this.manualDate = manualDate;
        initFitFeed();
    }
    
    public ArrayList<KeywordEntry> parseXml()
            throws IOException, SAXException, ParserConfigurationException, ParseException, ServiceException {

        String xml = getFitFeedXml();
        
        if (xml == null) {
            return null;
        }

        if(parseFitFeedXml(xml)) {
            return this.keywords;
        }

        return null;
    }
    
    private String getFitFeedXml() 
            throws IOException, ServiceException {

        HttpGet request = new HttpGet(this.fitFeedUrl);
        HttpResponse response = request.getResponse();
        return response.getBodyAsString();
    }

    private boolean parseFitFeedXml(String xml)
            throws SAXException, IOException, ParserConfigurationException, ParseException {

        // Normalize the xml
        Document xmlDocument = XmlHelpers.parseXml(xml);
        xmlDocument.normalizeDocument();
        Element rootNode = xmlDocument.getDocumentElement();

        parseRss(rootNode);
        
        return true;
    }

    private void parseRss(Element rootNode) throws ParseException {
        
        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if(childNode.getLocalName().equals("channel")) {
                    Node grandChildNode = childNode.getFirstChild();
                    if (grandChildNode.getLocalName().equals("title")) {
                        Channel channel = Channel.valueOf(parseCharacterData((Element) grandChildNode));
                        switch (channel) {
                            case Prices:
                                parsePrices((Element) childNode);
                                break;
                        }
                    }
                }
            }
        }
    }

    private void parsePrices(Element channelNode) throws ParseException {
        
        for (Node childNode = channelNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if(childNode.getLocalName().equals("item")) {
                    parseItem((Element) childNode);
                }
            }
        }
    }

    private void parseItem(Element itemNode) throws ParseException {

        KeywordEntry keywordEntry = new KeywordEntry(attribution, keywordBase, this.categoryId);

        for (Node childNode = itemNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                ItemElement itemElement = ItemElement.valueOf(childNode.getLocalName());
                
                switch (itemElement) {
                    case market:
                        keywordEntry.setMarket(parseCharacterData((Element) childNode));
                        break;
                    case product:
                        keywordEntry.setProduct(parseCharacterData((Element) childNode));
                        break;
                    case unit:
                        keywordEntry.setUnit(parseCharacterData((Element) childNode));
                        break;
                    case date:
                        
                        // Parse the date
                        if (this.manualDate == null) {
                            keywordEntry.setDate(parseCharacterData((Element) childNode) + " 23:59:59");
                        }
                        else {
                            keywordEntry.setDate(manualDate);
                        }
                        break;
                    case retailprice:
                        keywordEntry.setRetailPrice(parseCharacterData((Element) childNode));
                        break;
                    case wholesaleprice:
                        keywordEntry.setWholesalePrice(parseCharacterData((Element) childNode));
                        break;
                }
            }
        }
        
        // Add the region from the map. This needs to be done as it won't come in on the feed
        if (this.regionMap.containsKey(keywordEntry.getMarket()) && keywordEntry.getDate() != null) {
            keywordEntry.setRegion(this.regionMap.get(keywordEntry.getMarket()));

            // Only add the keyword if the region exists
            this.keywords.add(keywordEntry);
        }
        else {

            // Create a list of markets that we don't have a region for. TODO create a method of emailing IS to tell them of this
            if (!this.missingMarkets.contains(keywordEntry.getMarket())) {
                this.missingMarkets.add(keywordEntry.getMarket());
            }
        }
    }

    private String parseCharacterData(Element element) {
        Node child = element.getFirstChild();
        if (child instanceof CharacterData) {
            return ((CharacterData)child).getData();
        }
        return null;
    }

    public boolean saveToDatabase() 
            throws ClassNotFoundException, SQLException {

        boolean success = true;
        
        try {
            initDatabaseConnection();
            for (KeywordEntry keyword : this.keywords) {
                if (keyword.getWholesalePrice() != null || keyword.getRetailPrice() != null) {
                    
                    String keywordString = keyword.generateKeyword();
                    String contentString = keyword.getContent();
            
                    if (keywordString != null && contentString != null) {
                        if (!updateKeyword(keywordString, contentString, keyword)) {
                            if (!insertNewKeyword(keyword, keywordString, contentString)) {
                                success = false;
                            }
                        }
                    }
                    else {
                        success =  false;
                    }
                }
                else {
                    success = false;
                }
            }
        }
        catch (Exception e) {
            success = false;
        }
        finally {
            success = cleanOldContent();
            closeDatabaseConnection();
        }
       
        return success;
    }

    public boolean cleanOldContent() throws SQLException {

        boolean success = true;
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, -1);
        now.add(Calendar.WEEK_OF_YEAR, -2);

        StringBuilder queryText = new StringBuilder();
        queryText.append("UPDATE ");
        queryText.append(DatabaseTable.Keyword.getTableName());
        queryText.append(" SET ");
        queryText.append(" isDeleted = 1,");
        queryText.append(" updated = '");
        queryText.append(DatabaseHelpers.getTimestamp(new java.util.Date()));
        queryText.append("'");
        queryText.append(" WHERE");
        queryText.append(" keyword LIKE '%" + keywordBase +"%'");
        queryText.append(" AND updated < '");
        queryText.append(DatabaseHelpers.getTimestamp(now.getTime()));
        queryText.append("'");

        PreparedStatement statement = this.connection.prepareStatement(queryText.toString());

        try {
            statement.executeUpdate();
        }
        catch (Exception e){

            // Do nothing as the finally that calls this will close the connections
            success = false;
        }
        finally {
            statement.close();
        }
        
        return success;
    }

    private void initDatabaseConnection()
            throws ClassNotFoundException, SQLException {

        this.connection = DatabaseHelpers.createConnection(WebAppId.search);

        // Prepare the insert statement
        StringBuilder insertText = new StringBuilder();
        insertText.append("INSERT INTO ");
        insertText.append(DatabaseTable.Keyword.getTableName());
        insertText.append(" (keyword, categoryId, createDate, content, updated, attribution, otrigger, quizAction_action, quizAction_quizId) ");
        insertText.append("VALUES ");
        insertText.append("(?, ?, ?, ?, ?, ?, ?, ?, ?)");
        this.insertStatement = this.connection.prepareStatement(insertText.toString());

        // Prepare the update statement
        StringBuilder updateText = new StringBuilder();
        updateText.append("UPDATE ");
        updateText.append(DatabaseTable.Keyword.getTableName());
        updateText.append(" SET ");
        updateText.append("content = ?, ");
        updateText.append("updated = ?, ");
        updateText.append("isDeleted = ? ");
        updateText.append("WHERE ");
        updateText.append("keyword = ? ");
        this.updateStatement = this.connection.prepareStatement(updateText.toString());
    }

    private void closeDatabaseConnection()
            throws SQLException {

        if (this.connection != null) {
            this.connection.close();
        }
        if (this.insertStatement != null) {
            this.insertStatement.close();
        }
        if (this.updateStatement != null) {
            this.updateStatement.close();
        }
    }

    private boolean updateKeyword(String keyword, String content, KeywordEntry entry)
            throws SQLException, ParseException {

        this.updateStatement.clearParameters();
        this.updateStatement.setString(1, content);
        this.updateStatement.setTimestamp(2, DatabaseHelpers.getTimestamp(DatabaseHelpers.getJavaDateFromString(entry.getDate(), 0)));
        this.updateStatement.setInt(3, 0);
        this.updateStatement.setString(4, keyword);

        if (this.updateStatement.executeUpdate() == 0) {
            return false;
        }
        return true;
    }

    private boolean insertNewKeyword(KeywordEntry entry, String keyword, String content)
            throws ClassNotFoundException, SQLException, ParseException {

        this.insertStatement.clearParameters();

        this.insertStatement.setString(1, keyword);
        this.insertStatement.setInt(2, entry.getCategoryId());
        this.insertStatement.setDate(3, new java.sql.Date(new java.util.Date().getTime()));
        this.insertStatement.setString(4, content);
        this.insertStatement.setTimestamp(5, DatabaseHelpers.getTimestamp(DatabaseHelpers.getJavaDateFromString(entry.getDate(), 0)));
        this.insertStatement.setString(6, entry.getAttribution());
        this.insertStatement.setInt(7, 0);
        this.insertStatement.setString(8, "");
        this.insertStatement.setInt(9, 0);

        if (this.insertStatement.executeUpdate() == 0) {
            return false;
        }
        return true;
    }

    /**
     * Get the required parameters from the config file
     * 
     */
    private void initFitFeed()
            throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {

        this.keywords = new ArrayList<KeywordEntry>();
        this.missingMarkets = new ArrayList<String>();
        initRegionMap();
    }

    private void initRegionMap()
            throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {

        SearchSalesforceProxy salesforceProxy = new SearchSalesforceProxy();
        this.regionMap = salesforceProxy.getRegionMap();
    }

    private enum Channel {
        Prices;
    }
    
    private enum ItemElement {
        market,
        product,
        unit,
        date,
        retailprice,
        wholesaleprice;
    }
}
