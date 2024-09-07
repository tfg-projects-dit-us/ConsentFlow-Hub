package us.dit.gestorconsentimientos.service.conf;

import java.util.ArrayList;
import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Practitioner;
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

import us.dit.gestorconsentimientos.service.model.FhirDAO;
import us.dit.gestorconsentimientos.service.model.FhirDTO;

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

	private static FhirDAO fhirDAO = new FhirDAO();

	@Value("${fhirserver.location}")
	private String server;
	
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

		// codifico las password en https://bcrypt-generator.com/, uso nombre como password

		ArrayList<UserDetails> userList = new ArrayList<UserDetails>();

		// $2a$12$Pa3IIDS5JhAJpiLt5/lT4O5KVw1pyU.dVGpz/q7kEGUAH.JL85tRC
		UserDetails user = User.withUsername("user").password(encoder.encode("user")).roles("kie-server", "FACULTATIVO", "PACIENTE").build();
		userList.add(user);

		// $2a$12$irR0VcP4SdtvAn7cbnXXQ.Cnfk/NlLWZa4mnx0J8EeXFum8Pt1pfm
		UserDetails wbadmin = User.withUsername("wbadmin").password(encoder.encode("wbadmin")).roles("admin").build();
		userList.add(wbadmin);

		//Este usuario se va a utilizar para el acceso al servidor
		UserDetails consentimientos = User.withUsername(USER).password(encoder.encode(PASSWORD)).roles("kie-server").build();
		userList.add(consentimientos);

		// $2a$12$1T7IYm0PmxpWyJFjqTSlm.489.s65TvHJbW4R7d1SG0giNHb5bqAm
		UserDetails kieserver = User.withUsername("kieserver").password(encoder.encode("kieserver")).roles("kie-server").build();
		userList.add(kieserver);

		UserDetails facultativo = User.withUsername("medico").password(encoder.encode("medico")).roles("kie-server", "FACULTATIVO").build();
		userList.add(facultativo);

		UserDetails paciente = User.withUsername("paciente").password(encoder.encode("paciente")).roles("kie-server", "PACIENTE").build();
		userList.add(paciente);

		UserDetails paciente2 = User.withUsername("paciente2").password(encoder.encode("paciente")).roles("kie-server", "PACIENTE").build();
		userList.add(paciente2);

		UserDetails paciente3 = User.withUsername("paciente3").password(encoder.encode("paciente")).roles("kie-server", "PACIENTE").build();
		userList.add(paciente3);

		UserDetails paciente4 = User.withUsername("paciente4").password(encoder.encode("paciente")).roles("kie-server", "PACIENTE").build();
		userList.add(paciente4);

		
		for(UserDetails userDetail: userList){
			
			if (userDetail.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTATIVO"))){
				Practitioner practitioner = new Practitioner();
				practitioner.addName(new HumanName().setText(userDetail.getUsername())); 
				
				if (fhirDAO.searchPatientOrPractitionerIdByName(server,userDetail.getUsername(),"Practitioner") != null){
					fhirDAO.save(new FhirDTO(server,practitioner));
				}

			}
			
			if (userDetail.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"))){
				Patient patient = new Patient();
				patient.addName(new HumanName().setText(userDetail.getUsername())); 

				if (fhirDAO.searchPatientOrPractitionerIdByName(server,userDetail.getUsername(),"Patient") != null){
					fhirDAO.save(new FhirDTO(server,patient));
				}
			}			

		} 


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
