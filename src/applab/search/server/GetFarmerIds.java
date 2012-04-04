package applab.search.server;

import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Servlet implementation class GetFarmerIds
 */
public class GetFarmerIds extends ApplabServlet {
	private static final long serialVersionUID = 1L;
	private static final String IMEI = "x-Imei";
	private static final String CURRENT_FARMER_ID_COUNT = "currentFarmerIdCount";

    /**
     * Default constructor. 
     */
    public GetFarmerIds() {
        // TODO Auto-generated constructor stub
    }

    @Override
	protected void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context)
            throws ServletException, IOException, ServiceException {
        
        log("Reached get method for Get Farmer Ids");
        doApplabPost(request, response, context);
	}

    @Override
	protected void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context)
            throws ServletException, IOException, ServiceException {
        
        try {
            log("Reached post method for Get Farmer Ids");
            String imei = request.getHeader(IMEI);
            log("x-Imei: " + imei);
            Document requestXml = context.getRequestBodyAsXml();
            NodeList nodeList = requestXml.getElementsByTagName(CURRENT_FARMER_ID_COUNT);
            String farmerIdString = nodeList.item(0).getTextContent();
            log("Farmer Id Count : " + farmerIdString);
            int farmerIdCount = Integer.parseInt(farmerIdString);
            log("Current Farmer Id Count: " + farmerIdCount);
            
            // make Salesforce call
            String jsonResult = getFarmerIdsFromSalesforce(imei, farmerIdCount);
            
            PrintWriter out = response.getWriter();
            out.println(jsonResult);
            log("Finished sending new Farmer Ids");
        }
        catch (SAXException e) {
           
            e.printStackTrace();
        }
        catch (ParserConfigurationException e) {
            
            e.printStackTrace();
        }        
	}
    
    /**
     * Stub for Saleforce method
     * @param imei
     * @param currentFarmerIdCount
     * @return
     */
    private String getFarmerIdsFromSalesforce(String imei, int currentFarmerIdCount) {
        Random rand = new Random();
        long randomValue1 = Math.round(rand.nextDouble() * 10000);
        long randomValue2 = Math.round(rand.nextDouble() * 10000);
        return String.format("{\"FarmerIds\" : [ {\"Id\" : \"223AB%d\", \"FId\" : \"DF%d\"}, {\"Id\" : \"234CD%d\", \"FId\" : \"CQ%d\"}]}",randomValue1, randomValue1,randomValue2, randomValue2);
    }
    

}
