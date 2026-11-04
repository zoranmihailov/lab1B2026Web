# 📘 Веб програмирање: целосна лекција (Servlets, Thymeleaf, слоеви, Session)

Како се гради проект од нула, како работат **GET и POST** во servlet-ите, и на крај решени барањата од оваа недела.

## Содржина

1. [Теорија: што се случува кога ќе отвориш страница](#1-теорија-што-се-случува-кога-ќе-отвориш-страница)
2. [Servlet во детали: GET, POST, барање и одговор](#2-servlet-во-детали-get-post-барање-и-одговор) ⭐
3. [Градење на проектот чекор по чекор](#3-градење-на-проектот-чекор-по-чекор)
4. [Exercise 3: Session, Filter, Listener](#4-exercise-3-session-filter-listener)
5. [Референца: Thymeleaf и анотации](#5-референца-thymeleaf-и-анотации)
6. [Грешки и дебагирање](#6-грешки-и-дебагирање)
7. [Барањата од оваа недела (решени)](#7-барањата-од-оваа-недела-решени)
8. [Рецепти за други барања и чеклиста](#8-рецепти-за-други-барања-и-чеклиста)

---

# 1. Теорија: што се случува кога ќе отвориш страница

Веб е разговор од две страни: **прелистувач (клиент)** праша, **сервер** одговара.

```
Прелистувач ──── HTTP БАРАЊЕ ────▶ Сервер (Tomcat + твојата Spring Boot апликација)
Прелистувач ◀─── HTTP ОДГОВОР ──── Сервер
```

**HTTP барање** содржи:

| Дел | Пример | Значење |
|---|---|---|
| Метод | `GET` или `POST` | што сакаш да направиш |
| URL | `/players?teamId=1` | патека (`/players`) + параметри (`?teamId=1`) |
| Заглавија (headers) | `User-Agent`, `Cookie` | податоци за прелистувачот |
| Тело (body) | `playerId=P1&teamId=1` | податоците од формата (само кај POST) |

**HTTP одговор** содржи: **статус** (200 = ОК, 302 = пренасочи, 404 = нема, 500 = грешка во серверот), **заглавија** (на пример `Location`) и **тело** (HTML страната).

### GET наспроти POST

| | **GET** | **POST** |
|---|---|---|
| Намена | **прикажи / побарај** страница | **испрати / промени** податоци |
| Каде се параметрите | во URL: `?name=Ana&age=20` | во телото (не се гледаат во URL) |
| Како се прави | внесуваш адреса, кликнуваш **линк** `<a>`, или форма со `method="GET"` | **само** форма со `method="POST"` |
| F5 (refresh) | безбедно, само ја прикажува пак | прелистувачот пак праќа (опасност од дупликат!) |
| Менува податоци? | **не** | **да** (додавање, бришење, менување) |

> 💡 **Правило за паметење:** Линк (`<a href>`) е **секогаш GET**. Затоа „Delete“ никогаш не е линк, туку **форма со POST**.

### Stateless (HTTP не памти)

Секое барање е независно: серверот не знае дека претходно си бил на друга страна. Ако сакаме да памтиме (кој е најавен, кои тимови си ги посетил), користиме **Session** (Дел 4).

---

# 2. Servlet во детали: GET, POST, барање и одговор

> Ова е најважниот дел. Ако го разбереш овој дел, сè друго е повторување.

## 2.1 Што е servlet

**Servlet = Java класа што Tomcat ја вика кога ќе стигне барање до нејзиниот URL.**

```java
@WebServlet(name = "TeamListServlet", urlPatterns = "/teams")   // 1. на кој URL слуша
public class TeamListServlet extends HttpServlet {              // 2. наследува HttpServlet

    // 3. зависности (Spring ги дава преку конструкторот)
    private final TeamService teamService;
    private final SpringTemplateEngine springTemplateEngine;

    public TeamListServlet(TeamService teamService, SpringTemplateEngine springTemplateEngine) {
        this.teamService = teamService;
        this.springTemplateEngine = springTemplateEngine;
    }

    // 4. се вика за GET барања до /teams
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // ...
    }

    // 5. се вика за POST барања до /teams
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // ...
    }
}
```

- `@WebServlet(urlPatterns = ...)`: мапира URL → класа. Два servlet-и **не смеат** да имаат ист URL (ниту исто `name`).
- Работи само ако има `@ServletComponentScan` на главната класа.
- Tomcat **сам** одлучува кој метод да повика: GET → `doGet`, POST → `doPost`. Ако пратиш POST, а немаш `doPost` → **405 Method Not Allowed**.
- Постои само **една инстанца** на servlet-от за сите корисници, затоа **не чувај податоци на корисник во полиња на servlet-от** (чувај во session).

## 2.2 Двата параметри: `req` и `resp`

Tomcat ти дава два објекта при секој повик:

### `req` (HttpServletRequest) = „што ми прати прелистувачот“ (**читаш од него**)

| Што сакаш | Код |
|---|---|
| Параметар (од URL **или** форма) | `req.getParameter("teamId")` → `String` или **`null`** |
| Заглавие | `req.getHeader("User-Agent")` |
| IP на клиентот | `req.getRemoteAddr()` |
| Метод / патека | `req.getMethod()`, `req.getRequestURI()`, `req.getServletPath()` |
| Session | `req.getSession()` |
| Application (споделено) | `getServletContext()` |

### `resp` (HttpServletResponse) = „што му враќам“ (**пишуваш во него**)

Имаш **два** начина да одговориш. Во едно барање користиш **точно еден**.

| | **A) Прикажи страница (render)** | **B) Пренасочи (redirect)** |
|---|---|---|
| Код | `springTemplateEngine.process("x.html", context, resp.getWriter());` | `resp.sendRedirect("/teams");` |
| Што се случува | серверот го испишува HTML-от директно | серверот вика „оди на `/teams`“ (статус **302**), а **прелистувачот прави ново GET барање** |
| Кога | во **`doGet`** (прикажување страница) | по успешен **`doPost`**, или кога треба да се префрли на друга страна |
| Параметри | достапни од тоа барање | **се губат**: ново барање е! Што сакаш да пренесеш, ставаш во URL: `?errorMessage=...` |

> ⚠️ После `sendRedirect(...)` секогаш стави `return;` ако има код под него, и **никогаш** не прави и redirect и render во исто барање (грешка: `Cannot call sendRedirect() after the response has been committed`).

## 2.3 Како работи render (прикажување страница)

```java
IWebExchange webExchange = JakartaServletWebApplication
        .buildApplication(getServletContext())
        .buildExchange(req, resp);                 // (1)
WebContext context = new WebContext(webExchange); // (2)

context.setVariable("teams", teamService.listTeams());   // (3)

springTemplateEngine.process("listTeams.html", context, resp.getWriter());   // (4)
```

1. Го врзува Thymeleaf со тековното барање/одговор (така шаблонот има пристап до `session`).
2. `WebContext` = „кутија“ во која ставаш променливи за шаблонот.
3. `setVariable("teams", ...)`: во шаблонот ќе е `${teams}`. Првиот аргумент (стринг) е името што го пишуваш во HTML-от.
4. `process(...)`: „земи го `listTeams.html`, пополни ги `${...}` со вредностите од кутијата и испиши го резултатот во одговорот“. Шаблонот се бара во `src/main/resources/templates/`.

Овие 4 линии се исти во секој servlet што прикажува страница. Копирај ги.

## 2.4 Како стигнуваат параметрите (HTML → Java)

Сите се читаат со **истиот** метод `req.getParameter("име")`. Разликата е само **како** стигнале:

| Од каде | HTML | Како изгледа барањето | Читање |
|---|---|---|---|
| GET форма | `<form action="/players" method="GET"><input name="teamId" ...>` | `GET /players?teamId=1` | `getParameter("teamId")` |
| POST форма | `<form action="/teamDetails" method="POST"><input name="playerId" ...>` | `POST /teamDetails`, тело: `playerId=P1&teamId=1` | `getParameter("playerId")` |
| Скриено поле | `<input type="hidden" name="teamId" th:value="${team.id}">` | оди заедно со формата | `getParameter("teamId")` |
| Линк | `<a th:href="@{/players(teamId=${t.id})}">` | `GET /players?teamId=3` | `getParameter("teamId")` |
| Redirect | `resp.sendRedirect("/teams?errorMessage=Bad")` | `GET /teams?errorMessage=Bad` | `getParameter("errorMessage")` |

**Правило:** стрингот во `name="..."` на HTML-от мора буквално да е ист со стрингот во `getParameter("...")`. Најчеста причина за `null`.

### `null` наспроти празен стринг (честа збунетост)

| Ситуација | Што враќа `getParameter` |
|---|---|
| Параметарот воопшто не е испратен (нема таков `name`, или радио не е избрано) | **`null`** |
| Текст поле испратено празно | **`""`** (празен стринг, не `null`!) |

Затоа валидацијата во service-от секогаш проверува **и двете**: `x == null || x.isEmpty()`.

### Претворање од `String` во број

`getParameter` секогаш враќа `String`. Броевите се претвораат:

```java
Long id    = Long.valueOf(req.getParameter("teamId"));     // null или "abc" -> NumberFormatException
int  seats = Integer.parseInt(req.getParameter("numSeats"));
Double max = Double.valueOf(req.getParameter("maxPrice")); // "" -> NumberFormatException!
```

`NumberFormatException` е подкласа на `IllegalArgumentException`, па ист `catch (IllegalArgumentException e)` ги фаќа и лошите броеви и грешките од service-от.

## 2.5 Рецепт за `doGet` (прикажи страница)

```java
@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // 1. прочитај параметри (ако ги има) и најди ги податоците преку SERVICE
    Team team;
    try {
        Long teamId = Long.valueOf(req.getParameter("teamId"));
        team = teamService.findById(teamId);
    } catch (IllegalArgumentException e) {
        resp.sendRedirect("/teams");     // 2. лош/нема параметар -> префрли на друга страна
        return;                          //    (return! за да не продолжи кодот)
    }

    // 3. подготви ја кутијата со променливи
    IWebExchange webExchange = JakartaServletWebApplication
            .buildApplication(getServletContext()).buildExchange(req, resp);
    WebContext context = new WebContext(webExchange);
    context.setVariable("team", team);
    context.setVariable("errorMessage", req.getParameter("errorMessage"));  // порака од redirect

    // 4. прикажи го шаблонот
    springTemplateEngine.process("playersList.html", context, resp.getWriter());
}
```

**Формула:** прочитај → најди преку service → (грешка: redirect + return) → `setVariable` → `process`.

## 2.6 Рецепт за `doPost` (прими форма, промени податоци)

```java
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // 1. прочитај од формата
    String teamIdParam = req.getParameter("teamId");
    String playerId    = req.getParameter("playerId");

    try {
        // 2. повикај SERVICE (тој ги проверува правилата и фрла грешка ако не е ок)
        Long teamId = Long.valueOf(teamIdParam);
        teamService.addPlayerToTeam(teamId, playerId);

        // 3a. УСПЕХ: redirect кон GET страница (POST-Redirect-GET)
        resp.sendRedirect("/teamDetails?teamId=" + teamId);

    } catch (IllegalArgumentException e) {
        // 3b. ГРЕШКА: redirect назад со порака во URL
        String msg = e.getMessage() != null ? e.getMessage() : "Invalid input";
        resp.sendRedirect("/players?teamId=" + teamIdParam
                + "&errorMessage=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
    }
}
```

**Формула:** прочитај → `try { service } ` → успех: `redirect` на GET страна → `catch`: `redirect` назад со `errorMessage`.

## 2.7 Зошто POST завршува со redirect (POST-Redirect-GET)

Ако POST директно прикаже страница, прелистувачот го памти „последното барање = POST“. Кога ќе притиснеш **F5**, праќа **пак POST** → играчот се додава двапати / тимот се брише двапати.
Со redirect, последното барање е безопасен **GET**, па F5 само ја освежува страната.

## 2.8 Две можности за прикажување грешка

| Начин | Код | Кога |
|---|---|---|
| **Redirect со порака** (лаб) | `sendRedirect("/players?teamId=1&errorMessage=" + URLEncoder.encode(msg, UTF_8))` | кога POST може да менува податоци (за да F5 е безбеден) |
| **Повторно прикажување** (Exercise 3, логин) | `context.setVariable("errorMessage", e.getMessage()); process("login.html", ...); return;` | кога POST не променил ништо (на пример погрешна лозинка) |

Пораката во URL **мора** да се енкодира со `URLEncoder.encode(...)` (празни места, специјални знаци), и **не смее да е `null`**.

## 2.9 Како изгледа еден цел тек (проследи ги чекорите)

**Сценарио: корисникот избира тим и додава играч**

```
1. Прелистувачот: GET /teams
   TeamListServlet.doGet  → service.listTeams()  → setVariable("teams") → process("listTeams.html")
   ◀ HTML со радио копчиња за тимови

2. Корисникот избира тим 1, клик Submit (GET форма)
   Прелистувачот: GET /players?teamId=1
   PlayerServlet.doGet → getParameter("teamId")="1" → service.findById(1) → process("playersList.html")
   ◀ HTML со играчи (формата има hidden teamId=1)

3. Корисникот избира играч P1, клик Add (POST форма)
   Прелистувачот: POST /teamDetails   тело: teamId=1&playerId=P1
   TeamDetailsServlet.doPost → service.addPlayerToTeam(1,"P1")

   3a. УСПЕХ:  sendRedirect("/teamDetails?teamId=1")   ◀ 302
       Прелистувачот автоматски: GET /teamDetails?teamId=1
       TeamDetailsServlet.doGet → process("teamDetails.html")   ◀ HTML (F5 е безбеден)

   3b. ГРЕШКА (service фрла IllegalArgumentException("Jersey number 10 is already taken in this team")):
       sendRedirect("/players?teamId=1&errorMessage=Jersey+number+10+...")   ◀ 302
       Прелистувачот: GET /players?teamId=1&errorMessage=...
       PlayerServlet.doGet → setVariable("errorMessage", ...) → страната покажува црвена порака
```

## 2.10 Најчести грешки во servlet-и

| Грешка | Причина |
|---|---|
| 404 | нема `@ServletComponentScan`, погрешен `urlPatterns`, или нерестартирана апликација |
| `null` / `Cannot parse null string` | `name` во HTML ≠ стрингот во `getParameter` |
| 500 + NPE во `URLEncoder.encode` | `e.getMessage()` е `null` (service фрлил исклучок без порака) |
| 405 Method Not Allowed | форма праќа POST, а нема `doPost` (или обратно) |
| `Cannot call sendRedirect() after the response has been committed` | redirect + render во исто барање, или нема `return` по redirect |
| Празна страница / `IllegalStateException` | `process(...)` повикан два пати |
| Страната покажува стари податоци | не си рестартирал, или си кеширал со F5 на POST |

---

# 3. Градење на проектот чекор по чекор

Главен пример: **Team/Player** (лабот, група Б). Во аудиториските истото го прави `Category` (вежба 1 и 2): само замени `Team → Category`.

**Редослед (секогаш долу → горе):** проект → пакети → Model → DataHolder → Repository → Service → Servlet → Шаблон.
Зошто така? Секој слој користи слој под него, па долниот мора да постои прв.

## Чекор 1: Креирање на проектот

IntelliJ → **New Project → Spring Boot**.

| Поле | Вредност |
|---|---|
| Language / Type | Java / Maven |
| Group | `mk.ukim.finki.wp` |
| Artifact | `lab` |
| Package name | `mk.ukim.finki.wp.lab` |
| Dependencies | **Spring Web, Thymeleaf, Lombok** |

Отвори ја главната класа (`src/main/java/mk/ukim/finki/wp/lab/LabApplication.java`) и додај:

```java
@SpringBootApplication
@ServletComponentScan          // ← ЗАДОЛЖИТЕЛНО (import: org.springframework.boot.web.servlet.ServletComponentScan)
public class LabApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabApplication.class, args);
    }
}
```

> Без `@ServletComponentScan` сите servlet-и враќаат 404.

Во `src/main/resources/application.properties` (по желба): `server.port=8080` (аудиториските користат 9090).

## Чекор 2: Пакети

Десен клик на `mk.ukim.finki.wp.lab` → **New → Package**. Направи:

```
model            bootstrap
repository       repository.impl
service          service.impl
web.servlet
```

Класи: десен клик на пакетот → **New → Java Class** (или **Interface**). Сите пакети мора да се **под** основниот, инаку Spring не ги гледа.

## Чекор 3: Model (пакет `model`)

```java
// Player.java
@Data
@AllArgsConstructor
public class Player {
    private String playerId;
    private String firstName;
    private String lastName;
    private String position;
    private int jerseyNumber;
}

// Team.java
@Data
@AllArgsConstructor
public class Team {
    private Long id;
    private String name;
    private String city;
    private List<Player> players;
}
```

Lombok: `@Data` = getters + setters + `toString` + `equals` + `hashCode`; `@AllArgsConstructor` = конструктор со сите полиња (по редот). Thymeleaf `${team.name}` вика `getName()`, па getters мора да постојат.

## Чекор 4: DataHolder (пакет `bootstrap`)

„Лажна база“: статични листи што се полнат на почеток.

```java
@Component
public class DataHolder {
    public static List<Team> teams = null;
    public static List<Player> players = null;

    @PostConstruct
    public void init() {
        teams = new ArrayList<>();
        teams.add(new Team(1L, "Skopje Eagles", "Skopje", new ArrayList<>()));   // new ArrayList<>() = изменлива!
        teams.add(new Team(2L, "Ohrid Waves", "Ohrid", new ArrayList<>()));
        teams.add(new Team(3L, "Bitola Bears", "Bitola", new ArrayList<>()));
        teams.add(new Team(4L, "Tetovo Wolves", "Tetovo", new ArrayList<>()));
        teams.add(new Team(5L, "Kumanovo Lions", "Kumanovo", new ArrayList<>()));

        players = new ArrayList<>();
        players.add(new Player("P1", "Marko", "Ilievski", "Midfielder", 10));
        players.add(new Player("P2", "Stefan", "Trajkov", "Goalkeeper", 1));
        players.add(new Player("P3", "Ana", "Petreska", "Forward", 9));
        players.add(new Player("P4", "Nikola", "Stojanov", "Defender", 5));
        players.add(new Player("P5", "Elena", "Dimitrova", "Midfielder", 7));
        players.add(new Player("P6", "Filip", "Angelov", "Forward", 10));   // исто #10 како P1 (за тест)
        players.add(new Player("P7", "Viktor", "Kostov", "Defender", 3));
        players.add(new Player("P8", "Sara", "Jovanova", "Forward", 11));
        players.add(new Player("P9", "David", "Naumov", "Defender", 4));
        players.add(new Player("P10", "Maja", "Ristova", "Midfielder", 8));
    }
}
```

- `@Component` = Spring прави еден објект (**bean**).
- `@PostConstruct` = се извршува еднаш, откако Spring ќе го создаде bean-от.
- Податоците живеат во меморија → при секој рестарт почнуваат одново.

## Чекор 5: Repository (пакети `repository` и `repository.impl`)

Repository = **само** чување и барање податоци. Нема правила.

```java
// repository/TeamRepository.java  (Interface)
public interface TeamRepository {
    List<Team> findAll();
    Optional<Team> findById(Long id);
    Team save(Team team);
}

// repository/PlayerRepository.java  (Interface)
public interface PlayerRepository {
    List<Player> findAll();
    Optional<Player> findByPlayerId(String playerId);
}
```

```java
// repository/impl/InMemoryTeamRepository.java
@Repository
public class InMemoryTeamRepository implements TeamRepository {

    @Override
    public List<Team> findAll() {
        return DataHolder.teams;
    }

    @Override
    public Optional<Team> findById(Long id) {
        return DataHolder.teams.stream()
                .filter(t -> t.getId().equals(id))       // Long се споредува со .equals, НИКОГАШ ==
                .findFirst();
    }

    @Override
    public Team save(Team team) {
        Optional<Team> existing = findById(team.getId());
        if (existing.isPresent()) {
            DataHolder.teams.set(DataHolder.teams.indexOf(existing.get()), team);   // замени
        } else {
            DataHolder.teams.add(team);                                              // додај
        }
        return team;
    }
}

// repository/impl/InMemoryPlayerRepository.java
@Repository
public class InMemoryPlayerRepository implements PlayerRepository {

    @Override
    public List<Player> findAll() {
        return DataHolder.players;
    }

    @Override
    public Optional<Player> findByPlayerId(String playerId) {
        return DataHolder.players.stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst();
    }
}
```

- **`Optional<T>`** = „може да нема резултат“. Наместо `null` враќаш `Optional.empty()`, па повикувачот мора да го реши тоа: `.orElseThrow(...)`.
- **Stream** = `lista.stream().filter(услов).findFirst()` → „најди го првиот што го задоволува условот“. `.toList()` → „собери ги сите што го задоволуваат“.
- `@Repository` = Spring bean за пристап до податоци.

## Чекор 6: Service (пакети `service` и `service.impl`)

Service = **правилата и проверките**. Грешка = `throw new IllegalArgumentException("порака")`.

```java
// service/TeamService.java  (Interface)
public interface TeamService {
    List<Team> listTeams();
    Team findById(Long id);
    Team addPlayerToTeam(Long teamId, String playerId);
}

// service/PlayerService.java  (Interface)
public interface PlayerService {
    List<Player> listPlayers();
    Player findByPlayerId(String playerId);
}
```

```java
// service/impl/PlayerServiceImpl.java
@Service
public class PlayerServiceImpl implements PlayerService {
    private final PlayerRepository playerRepository;

    public PlayerServiceImpl(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public List<Player> listPlayers() {
        return playerRepository.findAll();
    }

    @Override
    public Player findByPlayerId(String playerId) {
        return playerRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }
}

// service/impl/TeamServiceImpl.java
@Service
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public TeamServiceImpl(TeamRepository teamRepository, PlayerRepository playerRepository) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public List<Team> listTeams() {
        return teamRepository.findAll();
    }

    @Override
    public Team findById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
    }

    @Override
    public Team addPlayerToTeam(Long teamId, String playerId) {
        // 1. најди тим и играч
        Team team = findById(teamId);
        Player player = playerRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        // 2. дали играчот веќе игра за некој тим?
        Optional<Team> currentTeam = teamRepository.findAll().stream()
                .filter(t -> t.getPlayers().stream()
                        .anyMatch(p -> p.getPlayerId().equals(playerId)))
                .findFirst();
        if (currentTeam.isPresent()) {
            throw new IllegalArgumentException("Player already plays for " + currentTeam.get().getName());
        }

        // 3. дали бројот на дрес е зафатен во овој тим?
        boolean jerseyTaken = team.getPlayers().stream()
                .anyMatch(p -> p.getJerseyNumber() == player.getJerseyNumber());   // int -> ==
        if (jerseyTaken) {
            throw new IllegalArgumentException(
                    "Jersey number " + player.getJerseyNumber() + " is already taken in this team");
        }

        // 4. додај, зачувај, врати
        team.getPlayers().add(player);
        return teamRepository.save(team);
    }
}
```

- **Constructor injection:** не пишуваш `new ...Repository()`. Бараш во конструкторот и Spring го дава.
- Service зависи од **интерфејс** (`TeamRepository`), не од `InMemory...`. Spring ја наоѓа единствената имплементација.
- Редоследот на проверките е важен (како во задачата).
- Секој `throw` има **порака**.

## Чекор 7: Servlet-и (пакет `web.servlet`)

Потребни импорти (за секој servlet што рендерира):

```java
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
```

### 7.1 `TeamListServlet` → `/teams` (само GET)

```java
@WebServlet(name = "TeamListServlet", urlPatterns = "/teams")
public class TeamListServlet extends HttpServlet {
    private final TeamService teamService;
    private final SpringTemplateEngine springTemplateEngine;

    public TeamListServlet(TeamService teamService, SpringTemplateEngine springTemplateEngine) {
        this.teamService = teamService;
        this.springTemplateEngine = springTemplateEngine;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        IWebExchange webExchange = JakartaServletWebApplication
                .buildApplication(getServletContext()).buildExchange(req, resp);
        WebContext context = new WebContext(webExchange);

        context.setVariable("teams", teamService.listTeams());
        context.setVariable("errorMessage", req.getParameter("errorMessage"));

        springTemplateEngine.process("listTeams.html", context, resp.getWriter());
    }
}
```

### 7.2 `PlayerServlet` → `/players` (само GET)

```java
@WebServlet(name = "PlayerServlet", urlPatterns = "/players")
public class PlayerServlet extends HttpServlet {
    private final PlayerService playerService;
    private final TeamService teamService;
    private final SpringTemplateEngine springTemplateEngine;

    public PlayerServlet(PlayerService playerService, TeamService teamService,
                         SpringTemplateEngine springTemplateEngine) {
        this.playerService = playerService;
        this.teamService = teamService;
        this.springTemplateEngine = springTemplateEngine;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Team team;
        try {
            Long teamId = Long.valueOf(req.getParameter("teamId"));
            team = teamService.findById(teamId);
        } catch (IllegalArgumentException e) {
            resp.sendRedirect("/teams");
            return;
        }

        IWebExchange webExchange = JakartaServletWebApplication
                .buildApplication(getServletContext()).buildExchange(req, resp);
        WebContext context = new WebContext(webExchange);

        context.setVariable("team", team);
        context.setVariable("players", playerService.listPlayers());
        context.setVariable("errorMessage", req.getParameter("errorMessage"));

        springTemplateEngine.process("playersList.html", context, resp.getWriter());
    }
}
```

### 7.3 `TeamDetailsServlet` → `/teamDetails` (GET и POST)

```java
@WebServlet(name = "TeamDetailsServlet", urlPatterns = "/teamDetails")
public class TeamDetailsServlet extends HttpServlet {
    private final TeamService teamService;
    private final SpringTemplateEngine springTemplateEngine;

    public TeamDetailsServlet(TeamService teamService, SpringTemplateEngine springTemplateEngine) {
        this.teamService = teamService;
        this.springTemplateEngine = springTemplateEngine;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Team team;
        try {
            Long teamId = Long.valueOf(req.getParameter("teamId"));
            team = teamService.findById(teamId);
        } catch (IllegalArgumentException e) {
            resp.sendRedirect("/teams");
            return;
        }

        IWebExchange webExchange = JakartaServletWebApplication
                .buildApplication(getServletContext()).buildExchange(req, resp);
        WebContext context = new WebContext(webExchange);
        context.setVariable("team", team);

        springTemplateEngine.process("teamDetails.html", context, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String teamIdParam = req.getParameter("teamId");
        String playerId = req.getParameter("playerId");

        try {
            Long teamId = Long.valueOf(teamIdParam);
            teamService.addPlayerToTeam(teamId, playerId);
            resp.sendRedirect("/teamDetails?teamId=" + teamId);          // успех -> GET
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Invalid input";
            resp.sendRedirect("/players?teamId=" + teamIdParam
                    + "&errorMessage=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
        }
    }
}
```

## Чекор 8: Шаблони (`src/main/resources/templates/`)

Десен клик на `templates` → **New → File** → `listTeams.html`. CSS-от го копираш од задачата (овде го изоставам за краткост). Секој шаблон почнува со:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
```

**`listTeams.html`** (GET форма: избор на тим)
```html
<form action="/players" method="GET">
    <div th:each="team : ${teams}">
        <label>
            <input type="radio" name="teamId" th:value="${team.id}" required>
            <span th:text="'Name: ' + ${team.name} + ', City: ' + ${team.city}
                           + ', Players: ' + ${team.players.size()}"></span>
        </label>
    </div>
    <input type="submit" value="Submit">
</form>
```

**`playersList.html`** (POST форма: избор на играч)
```html
<p class="error" th:if="${errorMessage != null}" th:text="${errorMessage}"></p>

<section>
    <form action="/teamDetails" method="POST">
        <input type="hidden" name="teamId" th:value="${team.id}">
        <div th:each="player : ${players}">
            <label>
                <input type="radio" name="playerId" th:value="${player.playerId}">
                <span th:text="${player.firstName} + ' ' + ${player.lastName}
                               + ' (#' + ${player.jerseyNumber} + ', ' + ${player.position} + ')'"></span>
            </label>
        </div>
        <input type="submit" value="Add player">
    </form>
</section>
<aside>
    <table>
        <tr><td><b>Team ID</b></td><td th:text="${team.id}"></td></tr>
        <tr><td><b>Team Name</b></td><td th:text="${team.name}"></td></tr>
        <tr><td><b>City</b></td><td th:text="${team.city}"></td></tr>
    </table>
</aside>
```
Скриеното поле го носи `teamId` до следниот servlet; корисникот не го гледа.

**`teamDetails.html`**
```html
<h1 th:text="'Team: ' + ${team.name}"></h1>
<h2 th:text="'City: ' + ${team.city}"></h2>
<h2>Players in this team:</h2>

<ul th:unless="${team.players.isEmpty()}">
    <li th:each="player : ${team.players}"
        th:text="${player.firstName} + ' ' + ${player.lastName}
                 + ' (#' + ${player.jerseyNumber} + ', ' + ${player.position} + ')'"></li>
</ul>
<p th:if="${team.players.isEmpty()}">No players yet</p>

<p>
    <a th:href="@{/players(teamId=${team.id})}">Add another player</a> |
    <a th:href="@{/teams}">Back to teams</a>
</p>
```

## Чекор 9: Тестирање

Стартувај и отвори `http://localhost:8080/teams`.

1. 5 тимови со `Players: 0`.
2. Избери тим → `/players?teamId=1` (10 играчи, табела со тимот).
3. Додај **Marko (#10)** → `/teamDetails?teamId=1` (F5 не дуплира).
4. Додај **Filip (#10)** во истиот тим → *Jersey number 10 is already taken in this team*.
5. Марко во друг тим → *Player already plays for Skopje Eagles*.
6. Add без избран играч → *Player not found*.
7. `/players` без `teamId`, или `?teamId=99` → враќа на `/teams`.

---

# 4. Exercise 3: Session, Filter, Listener

## 4.1 Три „scope“-а: каде живее податокот

| Scope | Живее | Споделено меѓу | Код |
|---|---|---|---|
| **Request** | едно барање | никој | `req.getParameter(...)`, `context.setVariable(...)` |
| **Session** | една посета на еден корисник (до logout/timeout) | сите барања на **тој** корисник | `req.getSession().setAttribute("k", v)` / `getAttribute("k")` |
| **Application** (`ServletContext`) | додека работи апликацијата | **сите** корисници | `getServletContext().setAttribute("k", v)` |

**Како серверот знае која е твојата session?** При првото барање Tomcat прави session и му праќа на прелистувачот cookie `JSESSIONID`. Прелистувачот го праќа со секое барање и Tomcat по него ја наоѓа твојата session.

- `req.getSession()` → ја враќа сесијата, или **прави нова** ако нема.
- `req.getSession(false)` → ја враќа сесијата, или **`null`** ако нема (не прави нова).
- `session.invalidate()` → ја брише сесијата.
- `getAttribute` враќа `Object` (мора cast) и **`null`** ако нема.

**Во шаблон** `${session}` е достапна секогаш: `${session.user.name}`, без `setVariable`.

## 4.2 „Најавен“ = во сесијата има `user`

**Model, repository, service (ист шаблон како категории)**

```java
@Data @AllArgsConstructor
public class User {
    private String username, password, name, surname;
}

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndPassword(String username, String password);
    User save(User user);
    void deleteByUsername(String username);
}
```

Тест корисници во `DataHolder` (од вежбата): `elena.atanasoska/ea`, `darko.sasanski/ds`, `bojana.marojevikj/bm`.

```java
@Service
public class AuthServiceImpl implements AuthService {
    // constructor injection на UserRepository ...

    @Override
    public User login(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new InvalidArgumentsException();
        }
        return userRepository.findByUsernameAndPassword(username, password)
                .orElseThrow(InvalidUserCredentialsException::new);
    }
}
```

**Свои исклучоци** (во пакет `model.exceptions`):

```java
public class InvalidUserCredentialsException extends RuntimeException {
    public InvalidUserCredentialsException() {
        super("Invalid username or password.");
    }
}
```

| Исклучок | Кога | Порака |
|---|---|---|
| `InvalidArgumentsException` | празно поле | *Please fill in all required fields.* |
| `InvalidUserCredentialsException` | погрешно ime/лозинка | *Invalid username or password.* |
| `PasswordsDoNotMatchException` | лозинките не се исти | *Passwords do not match.* |

Зошто свои наместо `IllegalArgumentException`? Името кажува што не е во ред, servlet-от фаќа **точно** што очекува (не маскира вистински bug), а пораката е веќе за корисникот. **Ако лабот бара `IllegalArgumentException`, користи него.**

## 4.3 LoginServlet (GET: форма, POST: најава)

```java
@WebServlet(name = "loginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {
    private final AuthService authService;
    private final SpringTemplateEngine templateEngine;

    // constructor ...

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // само прикажи ја формата
        IWebExchange webExchange = JakartaServletWebApplication
                .buildApplication(getServletContext()).buildExchange(req, resp);
        WebContext context = new WebContext(webExchange);
        templateEngine.process("login.html", context, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        IWebExchange webExchange = JakartaServletWebApplication
                .buildApplication(getServletContext()).buildExchange(req, resp);
        WebContext context = new WebContext(webExchange);

        User user;
        try {
            user = authService.login(req.getParameter("username"), req.getParameter("password"));
        } catch (InvalidArgumentsException | InvalidUserCredentialsException e) {   // multi-catch
            context.setVariable("errorMessage", e.getMessage());
            templateEngine.process("login.html", context, resp.getWriter());        // повторно прикажи
            return;
        }

        req.getSession().setAttribute("user", user);     // ← „најавен“
        resp.sendRedirect("/servlet/category");           // redirect (PRG)
    }
}
```

> Во репото servlet-от е на `/servlet/login` (бидејќи `/login` подоцна го зема controller). Важно е **сите места** (servlet, filter, logout-redirect, `th:action` во шаблонот) да користат иста патека.

`login.html`: форма `th:action="@{/login}"`, `th:method="POST"`, полиња `name="username"` и `name="password"`, и `th:if="${errorMessage != null}"` за грешка.

Фаќаме **само** исклучоците што ги очекуваме (не `RuntimeException`), за вистински bug (на пример NPE) да не изгледа како „погрешна лозинка“.

## 4.4 LogoutServlet

```java
@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.getSession().invalidate();     // ја брише сесијата со се што има во неа
        resp.sendRedirect("/login");
    }
}
```

## 4.5 Session во шаблон

```html
<th:block th:if="${session.user}">
    <div th:text="${session.user.username}"></div>
    <div th:text="${session.user.name} + ' ' + ${session.user.surname}"></div>
    <a th:href="@{/logout}">Logout</a>
</th:block>
```

## 4.6 Filter: проверка пред секој servlet

Без filter би морал во **секој** servlet да проверуваш „дали е најавен“. Filter се извршува **пред** servlet-от, на едно место.

```
Прелистувач ──▶ LoginFilter ──▶ Servlet
                   │
                   ├─ има user во сесија?         → продолжи (filterChain.doFilter)
                   ├─ патеката е игнорирана?      → продолжи
                   └─ инаку                        → redirect на /login
```

```java
@WebFilter(filterName = "auth-filter", urlPatterns = "/*",
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD},
        initParams = @WebInitParam(name = "ignore-paths", value = "/login, /register"))
public class LoginFilter implements Filter {
    private List<String> ignorePaths;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // "/login, /register" -> ["/login", "/register"]
        this.ignorePaths = Arrays.stream(filterConfig.getInitParameter("ignore-paths").split(","))
                .map(String::trim).toList();
    }

    private boolean isIgnored(String path) {
        return ignorePaths.stream().anyMatch(p -> path.equals(p) || path.startsWith(p + "/"));
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;

        HttpSession session = req.getSession(false);
        User loggedInUser = session != null ? (User) session.getAttribute("user") : null;
        String path = req.getServletPath();

        if (loggedInUser == null && !isIgnored(path)) {
            resp.sendRedirect("/login");
        } else {
            filterChain.doFilter(req, resp);     // пушти го барањето до servlet-от
        }
    }
}
```

- Ако **не** повикаш `filterChain.doFilter(...)`, барањето **запира** тука.
- `/login` **мора** да е во листата на игнорирани, инаку: `/login → /login → /login ...` („too many redirects“).
- Внимавај на насоката: `path.startsWith(ignorePath)`, не обратно. Грешна насока ги пушта страните без најава (нема crash, па тестирај и страни што **треба** да се блокирани).

## 4.7 Listener + ServletContext: глобален бројач

**Listener** реагира на настани. `ServletContextListener` се вика кога апликацијата **стартува** и **запира**.

```java
@WebListener
public class ApplicationContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute("userViews", 0);        // апликацијата стартува
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().removeAttribute("userViews");        // апликацијата запира
    }
}
```

Во `doGet` на servlet-от:

```java
Integer userViews = (Integer) getServletContext().getAttribute("userViews");
userViews = userViews + 1;
getServletContext().setAttribute("userViews", userViews);
context.setVariable("userViews", userViews);
```

Во шаблон: `<div th:text="${userViews?:0}"></div>` (`?:` = „ако е `null`, земи `0`“).

Бројачот е **споделен за сите** (два прелистувачи, ист број). Ако `userViews + 1` фрли NPE → listener-от не работел (`@WebListener`, пакет под основниот).

## 4.8 Што да знаеш за колоквиум (Exercise 3)

1. HTTP е stateless; session (cookie `JSESSIONID`) памти корисник.
2. Scope: request / session / application.
3. „Најавен“ = `user` во сесијата; logout = `invalidate()`.
4. Filter = проверка пред servlet; Listener = реакција на старт/стоп.

---

# 5. Референца: Thymeleaf и анотации

## Thymeleaf

| Што сакаш | Код |
|---|---|
| променлива | `th:text="${team.name}"` |
| текст + променливи | `th:text="'Name: ' + ${t.name} + ', City: ' + ${t.city}"` |
| јамка | `th:each="t : ${teams}"` |
| само ако | `th:if="${errorMessage != null}"` / `th:unless="..."` |
| вредност на input | `th:value="${t.id}"` |
| оневозможено | `th:disabled="${w.capacity == 0}"` |
| големина / празна листа | `${team.players.size()}` / `${team.players.isEmpty()}` |
| линк | `th:href="@{/teams}"` |
| линк со параметар | `th:href="@{/players(teamId=${team.id})}"` |
| форма | `th:action="@{/x}" th:method="POST"` (или обичен `action` и `method`) |
| session | `${session.user.name}` |
| „ако е null, земи“ | `${userViews?:0}` |
| wrapper без таг | `<th:block th:if="...">...</th:block>` |

`th:text` автоматски ги ескејпира HTML знаците (заштита од XSS).

## Анотации

| Анотација | Каде | Значење |
|---|---|---|
| `@SpringBootApplication` | главна класа | стартува Spring Boot |
| `@ServletComponentScan` | главна класа | регистрира `@WebServlet`, `@WebFilter`, `@WebListener` |
| `@WebServlet(urlPatterns=...)` | servlet | URL на кој слуша |
| `@WebFilter`, `@WebListener` | filter / listener | како servlet, но за филтер/слушател |
| `@Component` | DataHolder | Spring bean |
| `@Repository` | InMemory… | bean за податоци |
| `@Service` | …ServiceImpl | bean за логика |
| `@PostConstruct` | метод | се извршува еднаш по создавањето |
| `@Data`, `@AllArgsConstructor` | model | Lombok: getters/setters/конструктор |

---

# 6. Грешки и дебагирање

**Прво отвори ја конзолата (IntelliJ → Run) и најди го последниот `Caused by:`. Сè пишува таму.**

| Симптом | Причина → поправка |
|---|---|
| **404** на сè | нема `@ServletComponentScan`, или servlet-от не е под основниот пакет |
| **404** на една патека | погрешен `urlPatterns`; рестартирај (нов servlet не се гледа без рестарт) |
| Чудно однесување со два servlet-и | исти `urlPatterns` или исто `name` |
| **500** + NPE во `URLEncoder.encode` | `e.getMessage()` е `null` → секој `throw` со порака |
| `Cannot parse null string` | `name` во HTML ≠ `getParameter("...")` |
| `NumberFormatException` за празно поле | `Double.valueOf("")`: прво `""` → `null` |
| `TemplateInputException` | погрешно име на шаблон во `process(...)` или не е во `templates/` |
| `TemplateProcessingException` / `cannot find property` | погрешно име на променлива или нема getter |
| `UnsupportedOperationException` | листата не е `new ArrayList<>()` (на пример `List.of`) |
| `No qualifying bean of type` | нема `@Repository`/`@Service`/`@Component`, или е надвор од основниот пакет |
| `expected single matching bean but found 2` | две имплементации на ист интерфејс |
| NPE во repository | `DataHolder` нема `@Component` (листата е `null`) |
| `cannot find symbol getName()` | Lombok: *Settings → Compiler → Annotation Processors → Enable* |
| `too many redirects` | `/login` не е во `ignore-paths` на filter-от |
| NPE на `userViews + 1` | listener не работи (`@WebListener`, пакет) |
| `Port already in use` | запри го претходниот run или смени `server.port` |
| `PKIX path building failed` (Maven) | антивирус го скенира HTTPS (исклучи за `repo.maven.apache.org`) |
| `JVM target 5` | Maven → **Reload All Maven Projects**, провери JDK |

---

# 7. Барањата од оваа недела (решени)

## 7.1 Барање 1: Delete копче што го брише тимот

**Идеја:** repository брише, service проверува, **нов servlet** (POST) прима, redirect назад.

### ① Repository
`TeamRepository` (интерфејс): додај
```java
void deleteById(Long id);
```
`InMemoryTeamRepository`: додај
```java
@Override
public void deleteById(Long id) {
    DataHolder.teams.removeIf(t -> t.getId().equals(id));
}
```

### ② Service
`TeamService` (интерфејс): додај
```java
void delete(Long id);
```
`TeamServiceImpl`: додај
```java
@Override
public void delete(Long id) {
    findById(id);                      // ако не постои, фрла IllegalArgumentException("Team not found")
    teamRepository.deleteById(id);
}
```

### ③ Нов servlet `TeamDeleteServlet` (пакет `web.servlet`)
```java
@WebServlet(name = "TeamDeleteServlet", urlPatterns = "/teams/delete")
public class TeamDeleteServlet extends HttpServlet {
    private final TeamService teamService;

    public TeamDeleteServlet(TeamService teamService) {
        this.teamService = teamService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id;
        try {
            id = Long.valueOf(req.getParameter("teamId"));
            teamService.delete(id);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Invalid input";
            resp.sendRedirect("/teams?errorMessage=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
            return;
        }

        // (чистење на листата „последно посетени“: види 7.2 ④)

        resp.sendRedirect("/teams");       // POST-Redirect-GET
    }
}
```
Само `doPost` (бришењето менува податоци, не смее да е GET). Нема `process(...)`: нема своја страница, само пренасочува.

### ④ Копче во `listTeams.html`

**Опција А (наједноставно):** копче во истата форма со радио копчињата:
```html
<input type="submit" value="Submit">
<button type="submit" formaction="/teams/delete" formmethod="POST">Delete</button>
```
`formaction` и `formmethod` ги заменуваат `action`/`method` на формата **само за тоа копче**. `teamId` доаѓа од избраното радио (`required` блокира ако ништо не е избрано).

**Опција Б:** посебна форма за секој тим (надвор од другата форма; форми не се вгнездуваат):
```html
<form th:each="team : ${teams}" th:action="@{/teams/delete}" method="POST">
    <input type="hidden" name="teamId" th:value="${team.id}">
    <span th:text="${team.name}"></span>
    <button type="submit">Delete</button>
</form>
```

Играчите не се бришат (тие се во `DataHolder.players`), па по бришење на тимот стануваат слободни. Грешката од `errorMessage` ја прикажува `listTeams.html` со `<p class="error" th:if="${errorMessage != null}" th:text="${errorMessage}"></p>` (`TeamListServlet` веќе го праќа).

## 7.2 Барање 2: Листа на 3 последно посетени тимови

**Идеја:** во **сесијата** (Exercise 3) чуваме листа од тимови. При посета на тим: тргни дупликат → стави на **врв** → остани со 3. Прикажи ја на `/teams`.

### ① Каде се „посетува“ тим
Во servlet-от што го отвора тимот, на пример `TeamDetailsServlet.doGet` (или `PlayerServlet.doGet`, што го бара задачата). Целиот `doGet`:

```java
@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Team team;
    try {
        Long teamId = Long.valueOf(req.getParameter("teamId"));
        team = teamService.findById(teamId);
    } catch (IllegalArgumentException e) {
        resp.sendRedirect("/teams");
        return;
    }

    // --- последно посетени (SESSION) ---
    @SuppressWarnings("unchecked")
    List<Team> visited = (List<Team>) req.getSession().getAttribute("visitedTeams");
    if (visited == null) {                                       // прв пат во оваа сесија
        visited = new ArrayList<>();
        req.getSession().setAttribute("visitedTeams", visited);
    }
    visited.removeIf(t -> t.getId().equals(team.getId()));       // без дупликати
    visited.add(0, team);                                        // најново прво
    if (visited.size() > 3) {
        visited.remove(visited.size() - 1);                      // остани со 3
    }
    // ------------------------------------

    IWebExchange webExchange = JakartaServletWebApplication
            .buildApplication(getServletContext()).buildExchange(req, resp);
    WebContext context = new WebContext(webExchange);
    context.setVariable("team", team);

    springTemplateEngine.process("teamDetails.html", context, resp.getWriter());
}
```
Имаш `import java.util.ArrayList; import java.util.List;`.

### ② Прикажување во `listTeams.html`
Нема промена во `TeamListServlet` (`${session...}` е достапно директно):

```html
<h3>Last visited teams</h3>
<ul th:if="${session.visitedTeams != null and !session.visitedTeams.isEmpty()}">
    <li th:each="t : ${session.visitedTeams}">
        <a th:href="@{/teamDetails(teamId=${t.id})}" th:text="${t.name}"></a>
    </li>
</ul>
<p th:if="${session.visitedTeams == null or session.visitedTeams.isEmpty()}">No visited teams yet</p>
```

### ③ Тест
Отвори `/teamDetails?teamId=1`, `2`, `3`, `4` (по ред), па `/teams` → прикажани 4, 3, 2 (најновиот прв). Отвори пак 2 → ред 2, 4, 3 (без дупликат).

### ④ Поврзано со Delete (чистење)
Во `TeamDeleteServlet.doPost`, по `teamService.delete(id)`:
```java
@SuppressWarnings("unchecked")
List<Team> visited = (List<Team>) req.getSession().getAttribute("visitedTeams");
if (visited != null) {
    visited.removeIf(t -> t.getId().equals(id));
}
```
(Нужни импорти: `java.util.List`, `mk.ukim.finki.wp.lab.model.Team`.)

### ⑤ Варијанта БЕЗ session (само Exercise 1 и 2: слоеви)
Ако не е дозволена session: листата живее во `DataHolder` и е **споделена за сите корисници**.

```java
// DataHolder: додај
public static List<Team> lastVisited = new ArrayList<>();

// TeamRepository + InMemoryTeamRepository
List<Team> findLastVisited();                       // return DataHolder.lastVisited;

// TeamService
void markVisited(Long teamId);
List<Team> listLastVisited();

// TeamServiceImpl
@Override
public void markVisited(Long teamId) {
    Team team = findById(teamId);
    List<Team> visited = teamRepository.findLastVisited();
    visited.removeIf(t -> t.getId().equals(teamId));
    visited.add(0, team);
    if (visited.size() > 3) visited.remove(visited.size() - 1);
}

@Override
public List<Team> listLastVisited() {
    return teamRepository.findLastVisited();
}
```
Во servlet-от каде што се посетува тим: `teamService.markVisited(teamId);`.
Во `TeamListServlet.doGet`: `context.setVariable("lastVisited", teamService.listLastVisited());`.
Во шаблонот: `th:each="t : ${lastVisited}"` (и `${lastVisited.isEmpty()}` за празна порака).
При бришење (во `TeamServiceImpl.delete`): `teamRepository.findLastVisited().removeIf(t -> t.getId().equals(id));`.

**Разлика:** со session секој корисник има **своја** листа; со `DataHolder` листата е **заедничка**.

## 7.3 Барање 3: Бришење играч од тим (+ скриј го од радио листата)

**Што се бара:** (1) да може да се избрише играч од тим; (2) ако играчот е веќе во тимот, да **не се покажува** во радио копчињата на `/players`; (3) на страната за тимот (`/teamDetails`) да има **Delete** копче до секој играч.

**Идеја:** service метод што го тргнува играчот од листата на тимот, нов servlet (POST), и две промени на екранот. **Repository не се менува:** тимот ја држи листата на играчи, па е `removeIf` и потоа `save`.

### ① Service
`TeamService` (интерфејс): додај
```java
Team removePlayerFromTeam(Long teamId, String playerId);
```
`TeamServiceImpl`: додај
```java
@Override
public Team removePlayerFromTeam(Long teamId, String playerId) {
    Team team = findById(teamId);     // нема тим -> IllegalArgumentException
    boolean removed = team.getPlayers().removeIf(p -> p.getPlayerId().equals(playerId));
    if (!removed) {
        throw new IllegalArgumentException("Player is not in this team");
    }
    return teamRepository.save(team);
}
```
`removeIf` враќа `true` ако нешто е избришано, па така го фаќаш случајот кога играчот не е во тимот.

### ② Нов servlet `TeamRemovePlayerServlet` (пакет `web.servlet`)
```java
@WebServlet(name = "TeamRemovePlayerServlet", urlPatterns = "/teamDetails/remove")
public class TeamRemovePlayerServlet extends HttpServlet {
    private final TeamService teamService;

    public TeamRemovePlayerServlet(TeamService teamService) {
        this.teamService = teamService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String teamIdParam = req.getParameter("teamId");
        String playerId = req.getParameter("playerId");

        try {
            Long teamId = Long.valueOf(teamIdParam);
            teamService.removePlayerFromTeam(teamId, playerId);
            resp.sendRedirect("/teamDetails?teamId=" + teamId);          // назад на тимот
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Invalid input";
            resp.sendRedirect("/teamDetails?teamId=" + teamIdParam
                    + "&errorMessage=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
        }
    }
}
```
Само `doPost` (менува податоци). Нема `process(...)`, само redirect.

### ③ `PlayerServlet`: не ги покажувај играчите што се веќе во тимот
Во `doGet`, наместо `context.setVariable("players", playerService.listPlayers());` стави:
```java
List<Player> availablePlayers = playerService.listPlayers().stream()
        .filter(p -> team.getPlayers().stream()
                .noneMatch(tp -> tp.getPlayerId().equals(p.getPlayerId())))
        .toList();
context.setVariable("players", availablePlayers);
```
Импорти: `mk.ukim.finki.wp.lab.model.Player`, `java.util.List`. Шаблонот користи иста променлива `players`, па нема промена.

Ова ги крие играчите од **избраниот** тим. Правилото „Player already plays for <team>“ останува за играчи од други тимови. Ако бараат да се кријат играчите од **кој било** тим:
```java
.filter(p -> teamService.listTeams().stream()
        .noneMatch(t -> t.getPlayers().stream()
                .anyMatch(tp -> tp.getPlayerId().equals(p.getPlayerId()))))
```

### ④ `TeamDetailsServlet.doGet`: прати ја пораката за грешка
Додај до `context.setVariable("team", team);`:
```java
context.setVariable("errorMessage", req.getParameter("errorMessage"));
```

### ⑤ `teamDetails.html`: Delete до секој играч
Додај ја пораката над листата и замени го `<ul>`:
```html
<p class="error" th:if="${errorMessage != null}" th:text="${errorMessage}"></p>

<ul th:unless="${team.players.isEmpty()}">
    <li th:each="player : ${team.players}">
        <span th:text="${player.firstName} + ' ' + ${player.lastName}
                       + ' (#' + ${player.jerseyNumber} + ', ' + ${player.position} + ')'"></span>

        <form th:action="@{/teamDetails/remove}" method="POST" style="display:inline">
            <input type="hidden" name="teamId" th:value="${team.id}">
            <input type="hidden" name="playerId" th:value="${player.playerId}">
            <button type="submit">Delete</button>
        </form>
    </li>
</ul>
```
Додај `.error { color: red; }` во `<style>`.

> ⚠️ Текстот мора да е во `<span th:text>`, **не** на `<li th:text>`. `th:text` на `<li>` го заменува целиот внатрешен дел, па формата со копчето исчезнува.

### ⑥ Тест
1. Додај Marko и Ana во Skopje Eagles.
2. На `/players?teamId=1` Marko и Ana **ги нема** во радио листата.
3. На `/teamDetails?teamId=1` секој има Delete. Избриши го Marko: исчезнува, F5 не прави ништо повторно.
4. Врати се на `/players?teamId=1`: Marko е пак на листата. Сега може да се додаде Filip (#10), бидејќи бројот е слободен.

---

## 7.4 Барање 4: Rating на играчот

**Што се бара:** играчот добива **rating** (оценка) што се прикажува. Претпоставка: rating е `double` (на пример 8.5). Ако задачата бара `int`, смени го типот на тоа место.

**Правило за паметење:** кога додаваш ново поле во model, менуваш **три места**: класата, секој `new ...(...)` што ја креира (DataHolder), и шаблоните што го прикажуваат.

### ① Model: ново поле на крај (`Player.java`)
```java
@Data
@AllArgsConstructor
public class Player {
    private String playerId;
    private String firstName;
    private String lastName;
    private String position;
    private int jerseyNumber;
    private double rating;          // ← ново (на крај, па конструкторот го има како последен аргумент)
}
```

### ② DataHolder: секој играч добива rating
`@AllArgsConstructor` го менува конструкторот, па **секој** `new Player(...)` мора да добие нов аргумент, инаку не се компајлира:
```java
players.add(new Player("P1", "Marko", "Ilievski", "Midfielder", 10, 8.5));
players.add(new Player("P2", "Stefan", "Trajkov", "Goalkeeper", 1, 7.8));
players.add(new Player("P3", "Ana", "Petreska", "Forward", 9, 9.1));
players.add(new Player("P4", "Nikola", "Stojanov", "Defender", 5, 7.2));
players.add(new Player("P5", "Elena", "Dimitrova", "Midfielder", 7, 8.0));
players.add(new Player("P6", "Filip", "Angelov", "Forward", 10, 6.9));
players.add(new Player("P7", "Viktor", "Kostov", "Defender", 3, 7.5));
players.add(new Player("P8", "Sara", "Jovanova", "Forward", 11, 8.8));
players.add(new Player("P9", "David", "Naumov", "Defender", 4, 7.0));
players.add(new Player("P10", "Maja", "Ristova", "Midfielder", 8, 8.3));
```

### ③ Прикажи го во шаблоните
`playersList.html` (радио копче):
```html
<span th:text="${player.firstName} + ' ' + ${player.lastName}
               + ' (#' + ${player.jerseyNumber} + ', ' + ${player.position}
               + ', rating: ' + ${player.rating} + ')'"></span>
```
`teamDetails.html` (во `<span>` до Delete копчето):
```html
<span th:text="${player.firstName} + ' ' + ${player.lastName}
               + ' (#' + ${player.jerseyNumber} + ', ' + ${player.position}
               + ', rating: ' + ${player.rating} + ')'"></span>
```
Repository и service **не се менуваат**: тие само ги носат `Player` објектите.

### ④ (ако барат) корисникот да го внесува/менува ratingот
Нова акција = нов service метод + нов servlet + форма.

`PlayerService` (интерфејс): `Player ratePlayer(String playerId, double rating);`

`PlayerServiceImpl`:
```java
@Override
public Player ratePlayer(String playerId, double rating) {
    if (rating < 1 || rating > 10) {
        throw new IllegalArgumentException("Rating must be between 1 and 10");
    }
    Player player = findByPlayerId(playerId);    // фрла ако нема играч
    player.setRating(rating);
    return player;
}
```
Не треба `save`: објектот е **истиот** во `DataHolder.players` и во листите на тимовите (референца), па промената се гледа насекаде. (Со вистинска база тука би повикал `playerRepository.save(player)`.)

Нов servlet `PlayerRateServlet`:
```java
@WebServlet(name = "PlayerRateServlet", urlPatterns = "/players/rate")
public class PlayerRateServlet extends HttpServlet {
    private final PlayerService playerService;

    public PlayerRateServlet(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String teamIdParam = req.getParameter("teamId");
        try {
            double rating = Double.parseDouble(req.getParameter("rating"));   // "" или null -> NumberFormatException
            playerService.ratePlayer(req.getParameter("playerId"), rating);
            resp.sendRedirect("/teamDetails?teamId=" + teamIdParam);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Invalid input";
            resp.sendRedirect("/teamDetails?teamId=" + teamIdParam
                    + "&errorMessage=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
        }
    }
}
```
Форма во `teamDetails.html`, во `<li>`, до Delete формата:
```html
<form th:action="@{/players/rate}" method="POST" style="display:inline">
    <input type="hidden" name="teamId" th:value="${team.id}">
    <input type="hidden" name="playerId" th:value="${player.playerId}">
    <input type="number" name="rating" min="1" max="10" step="0.1"
           th:value="${player.rating}" required style="width:60px">
    <button type="submit">Rate</button>
</form>
```

### ⑤ (ако барат) просечен rating на тимот и сортирање
`TeamService`: `double averageRating(Long teamId);`
```java
@Override
public double averageRating(Long teamId) {
    return findById(teamId).getPlayers().stream()
            .mapToDouble(Player::getRating)
            .average()
            .orElse(0.0);                  // празен тим -> 0
}
```
`TeamDetailsServlet.doGet`: `context.setVariable("avgRating", teamService.averageRating(team.getId()));`

Шаблон: `<p th:text="'Average rating: ' + ${#numbers.formatDecimal(avgRating, 1, 2)}"></p>` (2 децимали).

Сортирање на играчите според rating (најдобар прв), во `PlayerServlet` пред `.toList()`:
```java
.sorted(Comparator.comparingDouble(Player::getRating).reversed())      // import java.util.Comparator
```

### ⑥ Тест
1. Во листата на `/players` секој играч покажува `rating: ...`.
2. Додај играч во тим: на `/teamDetails` rating е прикажан.
3. (⑤) Менувај rating на 11 → *Rating must be between 1 and 10*; празно поле → грешка од `parseDouble`.
4. (⑤) Просекот се менува кога додаваш или бришеш играч.

---

## 7.5 Конечни шаблони и структура

### `listTeams.html` (Delete тим + последно посетени)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="utf-8">
    <title>Team Manager - Choose a Team</title>
    <style type="text/css">
        body { width: 800px; margin: auto; font-family: Arial, sans-serif; }
        h1 { color: #333; }
        .error { color: red; }
    </style>
</head>
<body>
<header>
    <h1>Welcome to the Team Manager</h1>
</header>
<main>
    <p class="error" th:if="${errorMessage != null}" th:text="${errorMessage}"></p>

    <h2>Choose a team:</h2>
    <form action="/players" method="GET">
        <div th:each="team : ${teams}">
            <label>
                <input type="radio" name="teamId" th:value="${team.id}" required>
                <span th:text="'Name: ' + ${team.name} + ', City: ' + ${team.city}
                               + ', Players: ' + ${team.players.size()}"></span>
            </label>
        </div>
        <br/>
        <input type="submit" value="Submit">
        <button type="submit" formaction="/teams/delete" formmethod="POST">Delete</button>
    </form>

    <h3>Last visited teams</h3>
    <ul th:if="${session.visitedTeams != null and !session.visitedTeams.isEmpty()}">
        <li th:each="t : ${session.visitedTeams}">
            <a th:href="@{/teamDetails(teamId=${t.id})}" th:text="${t.name}"></a>
        </li>
    </ul>
    <p th:if="${session.visitedTeams == null or session.visitedTeams.isEmpty()}">No visited teams yet</p>
</main>
</body>
</html>
```

> Ако друг корисник (друга сесија) го избрише тимот, во твојата сесија може да остане избришан тим. За лаб е прифатливо; „почисто“ решение е да чуваш `id`-а наместо `Team` објекти.

### `playersList.html` (финална, со rating и без играчи што се веќе во тимот)

```html
<p class="error" th:if="${errorMessage != null}" th:text="${errorMessage}"></p>

<section>
    <h2>Select player:</h2>
    <form action="/teamDetails" method="POST">
        <input type="hidden" name="teamId" th:value="${team.id}">

        <div th:each="player : ${players}">
            <label>
                <input type="radio" name="playerId" th:value="${player.playerId}">
                <span th:text="${player.firstName} + ' ' + ${player.lastName}
                               + ' (#' + ${player.jerseyNumber} + ', ' + ${player.position}
                               + ', rating: ' + ${player.rating} + ')'"></span>
            </label>
        </div>
        <p th:if="${players.isEmpty()}">No available players</p>

        <br/>
        <input type="submit" value="Add player">
    </form>
</section>
<aside>
    <table>
        <tr><td><b>Team ID</b></td><td th:text="${team.id}"></td></tr>
        <tr><td><b>Team Name</b></td><td th:text="${team.name}"></td></tr>
        <tr><td><b>City</b></td><td th:text="${team.city}"></td></tr>
    </table>
</aside>
```

### `teamDetails.html` (финална, со Delete и rating)

```html
<p class="error" th:if="${errorMessage != null}" th:text="${errorMessage}"></p>

<h1 th:text="'Team: ' + ${team.name}"></h1>
<h2 th:text="'City: ' + ${team.city}"></h2>
<h2>Players in this team:</h2>

<ul th:unless="${team.players.isEmpty()}">
    <li th:each="player : ${team.players}">
        <span th:text="${player.firstName} + ' ' + ${player.lastName}
                       + ' (#' + ${player.jerseyNumber} + ', ' + ${player.position}
                       + ', rating: ' + ${player.rating} + ')'"></span>

        <!-- (по желба) форма за Rate од 7.4 ④ оди тука -->

        <form th:action="@{/teamDetails/remove}" method="POST" style="display:inline">
            <input type="hidden" name="teamId" th:value="${team.id}">
            <input type="hidden" name="playerId" th:value="${player.playerId}">
            <button type="submit">Delete</button>
        </form>
    </li>
</ul>
<p th:if="${team.players.isEmpty()}">No players yet</p>

<p>
    <a th:href="@{/players(teamId=${team.id})}">Add another player</a> |
    <a th:href="@{/teams}">Back to teams</a>
</p>
```

### Финална структура (сè заедно)

```
model/         Team, Player (+ rating)
bootstrap/     DataHolder (играчи со rating)
repository/    TeamRepository (+ deleteById), PlayerRepository
service/       TeamService (+ delete, + removePlayerFromTeam, + averageRating*),
               PlayerService (+ ratePlayer*)
web/servlet/   TeamListServlet, PlayerServlet (филтрира играчи во тимот),
               TeamDetailsServlet (+ visited, + errorMessage),
               TeamDeleteServlet            /teams/delete          POST
               TeamRemovePlayerServlet      /teamDetails/remove    POST
               PlayerRateServlet*           /players/rate          POST
templates/     listTeams.html, playersList.html, teamDetails.html
(* = само ако е побарано)
```

### Сите URL-а на едно место

| URL | Метод | Servlet | Што прави |
|---|---|---|---|
| `/teams` | GET | TeamListServlet | листа на тимови + последно посетени |
| `/teams/delete` | POST | TeamDeleteServlet | брише тим |
| `/players?teamId=` | GET | PlayerServlet | играчи (без оние во тимот) |
| `/teamDetails?teamId=` | GET | TeamDetailsServlet | детали за тим |
| `/teamDetails` | POST | TeamDetailsServlet | додава играч во тим |
| `/teamDetails/remove` | POST | TeamRemovePlayerServlet | брише играч од тим |
| `/players/rate` | POST | PlayerRateServlet | менува rating |


---

# 8. Рецепти за други барања и чеклиста

## Каде се менува, според барањето

| Ако барањето е… | Менуваш |
|---|---|
| нова операција врз податоци | **Repository** (интерфејс + InMemory) |
| правило / проверка / грешка | **Service**: `throw new IllegalArgumentException("порака")` |
| нова страница или копче | нов **Servlet** (уникатен URL) + шаблон |
| прикажување / форма | **Шаблон** |
| памти меѓу барања | **Session**; за сите корисници: **ServletContext** |

## Брзи рецепти

| Барање | Како |
|---|---|
| **Пребарување** | GET форма (`name="text"`); во repository: `.filter(x -> text == null \|\| text.isEmpty() \|\| x.getName().toLowerCase().contains(text.toLowerCase()))` |
| **Филтер по цена** | `maxPrice` стигнува како `String`: `""`→`null`, инаку `Double.valueOf`; filter: `maxPrice == null \|\| w.getPrice() <= maxPrice` |
| **Сортирање** | `.sorted(Comparator.comparing(Team::getName))` / `Comparator.comparingInt(Player::getJerseyNumber)` |
| **Ново правило** | во service: `if (услов) throw new IllegalArgumentException("порака");` (на пр. `team.getPlayers().size() >= 11`) |
| **Отстрани играч од тим** | service: `team.getPlayers().removeIf(p -> p.getPlayerId().equals(id)); teamRepository.save(team);` + servlet POST + redirect |
| **Откажи регистрација (група А)** | врати места: `w.setCapacity(w.getCapacity() + r.getNumberOfSeats())`, отстрани ја регистрацијата |
| **Измени (edit)** | форма со `th:value="${team.name}"`, `doPost` → service `update` → `save()` |
| **Додади нов** | форма POST → service `create` (валидација) → `save()`; id = најголемиот + 1 |
| **Листа на сите (регистрации)** | service `list…()` → нов servlet `doGet` → `th:each` во `<table>` |
| **Бројач по корисник** | `Integer n = (Integer) session.getAttribute("visits"); session.setAttribute("visits", n == null ? 1 : n + 1);` |
| **Глобален бројач** | `ServletContext` + listener (Дел 4.7) |
| **IP и прелистувач** | `req.getRemoteAddr()`, `req.getHeader("User-Agent")` |
| **Ново поле на model (rating, price...)** | класа (`private double rating;`) → секој `new ...(...)` во DataHolder (нов аргумент) → шаблони. Repository/service не се менуваат |
| **Избриши играч од тим + скриј од листа** | service `removeIf` + `save`, servlet POST + redirect; `PlayerServlet` филтрира со `noneMatch` (Дел 7.3) |
| **Дупликат име** | `repository.findByName(x).isPresent()` → `throw` |

## Како да размислуваш кога ќе добиеш ново барање

1. **Што се менува на екранот?** → шаблон (копче, листа, порака).
2. **Дали копчето менува податоци?** Да → **POST** форма + `doPost` + redirect. Не → GET (линк или GET форма).
3. **Дали треба нов URL?** → нов servlet (`@WebServlet`, уникатен), добива само она што му треба од service-от.
4. **Што треба од податоците?** → repository метод (само чување/барање).
5. **Има ли правило?** → service, со `IllegalArgumentException("порака")`.
6. **Дали треба да се памти меѓу барања?** → session (по корисник) или ServletContext (за сите).

## ✅ Чеклиста пред да кажеш „готово“

- Секој слој зборува само со слојот под него; нема `new` на друг слој.
- Секој servlet има **уникатен** `urlPatterns` и `name`; има `@ServletComponentScan`.
- Секој POST завршува со `sendRedirect`; по `sendRedirect` има `return` каде треба.
- Секој `throw new IllegalArgumentException(...)` има порака.
- `name` во HTML = `getParameter` во servlet.
- `Long` со `.equals`, `int` со `==`.
- Листите во `DataHolder` се `new ArrayList<>()`.
- Тестирано: нормален тек → секоја грешка → F5 после POST → празна листа → рестарт.