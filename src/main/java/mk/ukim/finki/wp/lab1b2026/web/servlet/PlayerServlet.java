package mk.ukim.finki.wp.lab1b2026.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mk.ukim.finki.wp.lab1b2026.model.Team;
import mk.ukim.finki.wp.lab1b2026.repository.PlayerRepository;
import mk.ukim.finki.wp.lab1b2026.repository.TeamRepository;
import mk.ukim.finki.wp.lab1b2026.service.PlayerService;
import mk.ukim.finki.wp.lab1b2026.service.TeamService;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;

@WebServlet(name = "PlayerServlet", urlPatterns = "/players")
public class PlayerServlet extends HttpServlet {
    TeamService teamService ;
    PlayerService playerService;
    SpringTemplateEngine templateEngine;

    public PlayerServlet(TeamService teamService, PlayerService playerService, SpringTemplateEngine templateEngine) {
        this.teamService = teamService;
        this.playerService = playerService;
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
        webContext.setVariable("players", playerService.listPlayers());
        webContext.setVariable("errorMessage", req.getParameter("errorMessage"));

        templateEngine.process("playersList.html", webContext, resp.getWriter());
    }
}
