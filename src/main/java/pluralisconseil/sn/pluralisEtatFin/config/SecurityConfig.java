package pluralisconseil.sn.pluralisEtatFin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private  final UserDetailsService userDetails;
    private final PasswordEncoder passwordEncoder;
    private final JwtFilter jwtFilter;


    //Authentication
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider=new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetails);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return  http.csrf(AbstractHttpConfigurer::disable)
                // .formLogin(AbstractAuthenticationFilterConfigurer::permitAll)
                .authorizeHttpRequests(auth-> auth
                        .requestMatchers(
//                                "/users/login",
                                "/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                ).authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }


}
