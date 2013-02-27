package applab.search.server;

import applab.server.ApplabConfiguration;
import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;
import applab.server.WebAppId;
import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.schemas._class.UpdateKeywords.JsonRequest;
import com.sforce.soap.schemas._class.UpdateKeywords.UpdateKeywordsBindingStub;
import com.sforce.soap.schemas._class.UpdateKeywords.UpdateKeywordsServiceLocator;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GetSearchKeywords extends ApplabServlet
{
  private static final long serialVersionUID = 1L;
  private static final String IMEI = "x-Imei";
  private static final String KEYWORDS_LAST_UPDATE_DATE = "localKeywordsVersion";
  private static final String IMAGES_LAST_UPDATE_DATE = "localImagesVersion";
  private static final String MENU_IDS = "menuIds";

  protected void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context)
    throws ServletException, IOException, ServiceException
  {
    log("Reached get method");
    doApplabPost(request, response, context);
  }

  protected void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context)
    throws ServletException, IOException, ServiceException
  {
    try
    {
      log("Reached post method");

      SimpleDateFormat dateFormat = 
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = new Date();
      String currentVersion = dateFormat.format(date);

      UpdateKeywordsBindingStub serviceStub = setupSalesforceAuthentication();

      String imei = request.getHeader("x-Imei");
      log("x-Imei: " + imei);

      Document requestXml = context.getRequestBodyAsXml();
      NodeList keywordsNodeList = requestXml.getElementsByTagName("localKeywordsVersion");
      String keywordsDateString = keywordsNodeList.item(0).getTextContent();

      String imagesDateString = keywordsDateString;
      log("Keywords update String: " + keywordsDateString);

      NodeList imagesNodeList = requestXml.getElementsByTagName("localImagesVersion");

      if ((imagesNodeList != null) && (imagesNodeList.getLength() != 0)) {
        imagesDateString = imagesNodeList.item(0).getTextContent();
        log("Keywords update String: " + keywordsDateString);
      }

      NodeList menuList = requestXml.getElementsByTagName("menuIds");
      String[] menuIds = menuList.item(0).getTextContent().split(",");

      if ((menuIds != null) && (menuIds.length != 0)) {
        log("menu Ids: " + menuIds[0]);
      }
      else {
        menuIds = new String[0];
        log("No previous menu Ids");
      }

      JsonRequest req = new JsonRequest();
      req.setImei(imei);
      req.setKeywordsLastUpdatedDate(keywordsDateString);
      req.setImagesLastUpdatedDate(imagesDateString);
      req.setMenuIds(menuIds);

      String[] jsonResults = serviceStub.getKeywords(req);

      String json = buildJsonResponse(jsonResults, currentVersion);

      PrintWriter out = response.getWriter();
      out.println(json);
      log("Finished sending keywords");
    }
    catch (DOMException e)
    {
      e.printStackTrace();
      log(e.getMessage());
    }
    catch (SAXException e)
    {
      e.printStackTrace();
      log(e.getMessage());
    }
    catch (ParserConfigurationException e)
    {
      e.printStackTrace();
      log(e.getMessage());
    }
  }

  private String buildJsonResponse(String[] jsonResults, String currentVersion)
  {
    StringBuilder str = new StringBuilder("{\"Total\":");
    str.append(jsonResults[0]);
    str.append(",\"Version\":\"");
    str.append(currentVersion);
    str.append("\", \"Menus\":");
    str.append(jsonResults[1]);
    str.append(", \"MenuItems\":");
    str.append(jsonResults[2]);
    str.append(", \"DeletedMenuItems\":");
    str.append(jsonResults[3]);
    str.append(", \"Images\":");
    str.append(jsonResults[4]);
    str.append(", \"DeletedImages\":");
    str.append(jsonResults[5]);
    str.append("}");
    return str.toString();
  }

  private UpdateKeywordsBindingStub setupSalesforceAuthentication()
    throws ServiceException, RemoteException, InvalidIdFault, UnexpectedErrorFault, LoginFault
  {
    UpdateKeywordsServiceLocator updateKeywordsServiceLocator = new UpdateKeywordsServiceLocator();
    UpdateKeywordsBindingStub serviceStub = (UpdateKeywordsBindingStub)updateKeywordsServiceLocator.getUpdateKeywords();

    SforceServiceLocator soapServiceLocator = new SforceServiceLocator();
    soapServiceLocator.setSoapEndpointAddress((String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceAddress", ""));
    SoapBindingStub binding = (SoapBindingStub)soapServiceLocator.getSoap();
    LoginResult loginResult = binding.login((String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceUsername", ""), 
      (String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforcePassword", "") + 
      (String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceToken", ""));
    SessionHeader sessionHeader = new SessionHeader(loginResult.getSessionId());

    serviceStub.setHeader("http://soap.sforce.com/schemas/class/UpdateKeywords", "SessionHeader", sessionHeader);
    return serviceStub;
  }
}