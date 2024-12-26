package ma.ensa.full_backend.config;

import ma.ensa.full_backend.service.ReservationService;
import ma.ensa.full_backend.ws.ReservationSoapService;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CxfConfig {

    @Bean
    public ServletRegistrationBean<CXFServlet> cxfServlet() {
        return new ServletRegistrationBean<>(new CXFServlet(), "/services/*");
    }

    @Bean(name = Bus.DEFAULT_BUS_ID)
    public SpringBus springBus() {
        return new SpringBus();
    }
    @Bean
    public EndpointImpl reservationServiceEndpoint(ReservationSoapService reservationSoapService) {
        EndpointImpl endpoint = new EndpointImpl(springBus(), reservationSoapService);
        endpoint.publish("/reservation");
        return endpoint;
    }

}