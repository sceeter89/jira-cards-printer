package eu.ganymede.jira.cardsPrinter.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;


public abstract class ServletBase extends HttpServlet{
    private final UserManager userManager;
    private final LoginUriProvider loginUriProvider;
    
    public ServletBase(
	UserManager userManager,
	LoginUriProvider loginUriProvider) {
	
	this.userManager = userManager;
	this.loginUriProvider = loginUriProvider;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	String username = userManager.getRemoteUsername(request);
	if (username == null)
	{
	    redirectToLogin(request, response);
	    return;
	}
	
	processRequest(request, response);
    }
    
    	
    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
	response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }
    
    private URI getUri(HttpServletRequest request)
    {
	StringBuffer builder = request.getRequestURL();
	if (request.getQueryString() != null)
	{
	    builder.append("?");
	    builder.append(request.getQueryString());
	}
	return URI.create(builder.toString());
    } 
    
    protected abstract void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
}