package back.domain.prompt.search.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class QueryTypeRuleProvider {

    private static final Set<String> TECH_QUERY_WHITELIST = Set.of(
            // Backend Framework
            "spring boot", "spring_boot",
            "django", "flask",
            "fastapi", "fast_api",
            "rails",
            "gin", "echo",

            // Frontend Framework
            "react", "vue", "nuxt", "angular", "svelte",
            "next js", "next_js",

            // Language
            "java", "kotlin", "scala",
            "python", "ruby",
            "javascript", "typescript",
            "node", "nodejs",
            "go", "golang",
            "rust",
            "graphql", "grpc",

            // Database
            "mysql", "mariadb",
            "postgres", "postgresql",
            "mongodb",
            "elasticsearch",
            "redis",
            "kafka", "rabbitmq", "rabbit_mq",
            "sqlite",

            // DevOps / Infra
            "docker",
            "kubernetes", "k8s",
            "terraform",
            "nginx",
            "prometheus", "grafana",
            "ci cd", "ci_cd",
            "github actions", "github_actions",

            // Auth / API
            "jwt", "oauth", "oauth2",
            "swagger", "openapi",

            // ORM / Query
            "querydsl", "query_dsl",

            // Build / Tool
            "gradle", "maven",
            "webpack", "vite"
    );

    private static final Set<String> FUNCTION_HINT_WORDS = Set.of(
            "기능", "관리", "결제", "주문", "리뷰", "조회", "작성", "검색", "로그인", "회원",
            "function", "feature", "management", "processing", "creation",
            "payment", "order", "review", "search", "login", "user", "checkout",
            "功能", "管理", "支付", "订单", "评论", "搜索", "创建", "登录", "用户"
    );

    public Set<String> getTechQueryWhitelist() {
        return TECH_QUERY_WHITELIST;
    }

    // 쿼리 텍스트 → 문서에서 매칭을 시도할 alias 변형 목록
    // 동일 기술의 여러 표기(쿼리 형태)가 같은 alias 목록을 가리킬 수 있음
    private static final Map<String, List<String>> ALIAS_GROUPS = Map.ofEntries(
            // Spring
            Map.entry("spring boot",      List.of("spring boot", "springboot", "spring_boot")),
            Map.entry("spring_boot",      List.of("spring boot", "springboot", "spring_boot")),

            // ORM
            Map.entry("querydsl",         List.of("querydsl", "query dsl", "query_dsl")),
            Map.entry("query_dsl",        List.of("querydsl", "query dsl", "query_dsl")),

            // Frontend
            Map.entry("next js",          List.of("next.js", "nextjs", "next_js", "next js")),
            Map.entry("next_js",          List.of("next.js", "nextjs", "next_js", "next js")),
            Map.entry("vue",              List.of("vue", "vuejs", "vue.js")),
            Map.entry("nuxt",             List.of("nuxt", "nuxtjs", "nuxt.js")),
            Map.entry("react",            List.of("react", "reactjs", "react.js")),
            Map.entry("angular",          List.of("angular", "angularjs")),

            // Node
            Map.entry("node",             List.of("node", "nodejs", "node.js", "node_js")),
            Map.entry("nodejs",           List.of("node", "nodejs", "node.js", "node_js")),

            // FastAPI
            Map.entry("fastapi",          List.of("fastapi", "fast api", "fast_api")),
            Map.entry("fast_api",         List.of("fastapi", "fast api", "fast_api")),

            // Go
            Map.entry("go",               List.of("go", "golang")),
            Map.entry("golang",           List.of("go", "golang")),

            // Kubernetes
            Map.entry("kubernetes",       List.of("kubernetes", "k8s")),
            Map.entry("k8s",              List.of("kubernetes", "k8s")),

            // Database
            Map.entry("postgres",         List.of("postgres", "postgresql")),
            Map.entry("postgresql",       List.of("postgres", "postgresql")),
            Map.entry("mongodb",          List.of("mongodb", "mongo", "mongo db")),
            Map.entry("elasticsearch",    List.of("elasticsearch", "elastic", "elastic search")),
            Map.entry("rabbitmq",         List.of("rabbitmq", "rabbit mq", "rabbit_mq")),
            Map.entry("rabbit_mq",        List.of("rabbitmq", "rabbit mq", "rabbit_mq")),

            // CI/CD
            Map.entry("github actions",   List.of("github actions", "github_actions", "githubactions")),
            Map.entry("github_actions",   List.of("github actions", "github_actions", "githubactions")),
            Map.entry("ci cd",            List.of("ci cd", "ci_cd", "ci/cd")),
            Map.entry("ci_cd",            List.of("ci cd", "ci_cd", "ci/cd")),

            // Language
            Map.entry("typescript",       List.of("typescript", "ts")),
            Map.entry("javascript",       List.of("javascript", "js")),

            // Auth
            Map.entry("oauth",            List.of("oauth", "oauth2")),
            Map.entry("oauth2",           List.of("oauth", "oauth2"))
    );

    public Set<String> getFunctionHintWords() {
        return FUNCTION_HINT_WORDS;
    }

    public Map<String, List<String>> getAliasGroups() {
        return ALIAS_GROUPS.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
    }
}
