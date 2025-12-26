package g3pjt.service.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class TomcatUriEncodingConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatUriEncodingCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setURIEncoding(StandardCharsets.UTF_8.name());
            // GET querystring 등 URI 디코딩에도 request encoding을 적용
            connector.setUseBodyEncodingForURI(true);
        });
    }
}
