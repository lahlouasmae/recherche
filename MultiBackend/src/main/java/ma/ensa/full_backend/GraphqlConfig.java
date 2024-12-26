package ma.ensa.full_backend;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import ma.ensa.full_backend.utils.DateScalar;
import ma.ensa.full_backend.utils.LongScalar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDate;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

@Configuration
public class GraphqlConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(DateScalar.DATE)
                .scalar(LongScalar.LONG);
    }
}
