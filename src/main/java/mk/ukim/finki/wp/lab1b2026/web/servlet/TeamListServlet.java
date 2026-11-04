package mk.ukim.finki.wp.lab1b2026.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mk.ukim.finki.wp.lab1b2026.repository.TeamRepository;
import mk.ukim.finki.wp.lab1b2026.service.TeamService;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;

@WebServlet(name="TeamListServlet", urlPatterns = "/teams")
public class TeamListServlet extends HttpServlet {
    TeamService teamService;
    SpringTemplateEngine  templateEngine;

    public TeamListServlet(TeamService teamService, SpringTemplateEngine templateEngine) {
        this.teamService = teamService;
        this.templateEngine = templateEngine;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        IWebExchange webExchange = JakartaServletWebApplication.buildApplication(getServletContext())
                .buildExchange(req, resp);

        WebContext webContext = new WebContext(webExchange);

        webContext.setVariable("teams", teamService.listTeams());
        templateEngine.process("listTeams.html", webContext, resp.getWriter());
    }
}
