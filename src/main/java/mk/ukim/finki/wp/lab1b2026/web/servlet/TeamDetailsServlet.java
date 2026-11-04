package mk.ukim.finki.wp.lab1b2026.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mk.ukim.finki.wp.lab1b2026.model.Team;
import mk.ukim.finki.wp.lab1b2026.service.TeamService;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "TeamDetailsServlet", urlPatterns = "/teamDetails")
public class TeamDetailsServlet extends HttpServlet {
    TeamService teamService;
    SpringTemplateEngine templateEngine;

    public TeamDetailsServlet(TeamService teamService, SpringTemplateEngine templateEngine) {
        this.teamService = teamService;
        this.templateEngine = templateEngine;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Team team;
        try{
            Long teamId = Long.valueOf(req.getParameter("teamId"));
            team = teamService.findById(teamId);
        }
        catch (IllegalArgumentException e){
            resp.sendRedirect("/teams");
            return;
        }

        IWebExchange webExchange = JakartaServletWebApplication.buildApplication(getServletContext())
                .buildExchange(req, resp);

        WebContext webContext = new WebContext(webExchange);

        webContext.setVariable("team", team);
        templateEngine.process("teamDetails.html", webContext, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String teamIdParam = req.getParameter("teamId");
        String playerIdParam = req.getParameter("playerId");

        try{
            Long teamId = Long.valueOf(teamIdParam);
            teamService.addPlayerToTeam(teamId, playerIdParam);
            resp.sendRedirect("/teamDetails?teamId=" + teamId);
        }
        catch (IllegalArgumentException e){
            String message = e.getMessage() != null ? e.getMessage() : "Invalid input";
            resp.sendRedirect("/players?teamId=" + teamIdParam
                    + "&errorMessage=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
        }
    }
}
