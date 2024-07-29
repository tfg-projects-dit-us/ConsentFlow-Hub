package us.dit.gestorconsentimientos.service.conf;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración de la seguridad mediante el objeto HttpSecurity
 * 
 * @author Isabel, Javier 
 */
@Configuration("kieServerSecurity")
@EnableWebSecurity
public class WebSecurityConfig {

	@Value("${kie.user}")
	private String USER;

	@Value("${kie.pwd}")
	private String PASSWORD;
	
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
			.authorizeHttpRequests( (authorize) -> authorize
				.antMatchers("/").permitAll()
				.antMatchers("/form-styles.css").permitAll()
				.antMatchers("/bussiness-application/**").permitAll()
				.antMatchers("/paciente/**").hasRole("PACIENTE")
				.antMatchers("/facultativo/**").hasRole("FACULTATIVO")
				.anyRequest().authenticated())
			.exceptionHandling((exceptionHandling) -> 
				exceptionHandling
					.accessDeniedPage("/access-denied.html"))
			.csrf((csrf) -> 
				csrf
					.disable())
			.httpBasic(withDefaults())
			.cors(withDefaults())
			.formLogin(withDefaults());

		return http.build();
	}

	@Bean
	UserDetailsService userDetailsService(BCryptPasswordEncoder encoder) {

		// codifico las password en https://bcrypt-generator.com/, uso nombre como
		// password
		// $2a$12$Pa3IIDS5JhAJpiLt5/lT4O5KVw1pyU.dVGpz/q7kEGUAH.JL85tRC
		UserDetails user = User.withUsername("user").password(encoder.encode("user")).roles("kie-server", "FACULTATIVO", "PACIENTE").build();
		// $2a$12$irR0VcP4SdtvAn7cbnXXQ.Cnfk/NlLWZa4mnx0J8EeXFum8Pt1pfm
		UserDetails wbadmin = User.withUsername("wbadmin").password(encoder.encode("wbadmin")).roles("admin").build();
		//Este usuario se va a utilizar para el acceso al servidor
		UserDetails consentimientos = User.withUsername(USER).password(encoder.encode(PASSWORD)).roles("kie-server").build();
		// $2a$12$1T7IYm0PmxpWyJFjqTSlm.489.s65TvHJbW4R7d1SG0giNHb5bqAm
		UserDetails kieserver = User.withUsername("kieserver").password(encoder.encode("kieserver")).roles("kie-server").build();

		UserDetails facultativo = User.withUsername("medico").password(encoder.encode("medico")).roles("kie-server", "FACULTATIVO").build();
		UserDetails paciente = User.withUsername("paciente").password(encoder.encode("paciente")).roles("kie-server", "PACIENTE").build();
		UserDetails paciente2 = User.withUsername("paciente2").password(encoder.encode("paciente")).roles("kie-server", "PACIENTE").build();
		UserDetails paciente3 = User.withUsername("paciente3").password(encoder.encode("paciente")).roles("kie-server", "PACIENTE").build();
		UserDetails paciente4 = User.withUsername("paciente4").password(encoder.encode("paciente")).roles("kie-server", "PACIENTE").build();

		return new InMemoryUserDetailsManager(wbadmin, user, kieserver, consentimientos, facultativo, paciente, paciente2, paciente3, paciente4);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		 UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	        CorsConfiguration corsConfiguration = new CorsConfiguration();
	        corsConfiguration.setAllowedOrigins(Arrays.asList("*"));
	        corsConfiguration.setAllowCredentials(true);
	        corsConfiguration.setAllowedMethods(Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name(),
	                                                          HttpMethod.POST.name(), HttpMethod.DELETE.name(), HttpMethod.PUT.name()));
	        corsConfiguration.applyPermitDefaultValues();
	        source.registerCorsConfiguration("/**", corsConfiguration);
	        return source;
	}


	@Bean
	BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
