package g3pjt.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;

@Configuration
public class EncodingConfig {

    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        // 1. 요청(Request) 인코딩을 UTF-8로 강제 설정
        filter.setEncoding("UTF-8");
        // 2. 응답(Response) 인코딩도 UTF-8로 강제 설정
        filter.setForceEncoding(true);
        return filter;
    }
}
